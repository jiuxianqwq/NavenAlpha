package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventStrafe;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.stream.StreamSupport;

@ModuleInfo(
        name = "Speed", cnName = "嘻嘻哈哈", description = "Movement speed adjustments with Grim modes", category = Category.MOVEMENT)
public class Speed extends Module {


    private final FloatValue grimHorizontal = ValueBuilder.create(this, "Bounding Box Size")
            .setDefaultFloatValue(0.4F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    private final FloatValue grimVertical = ValueBuilder.create(this, "In Player Speed")
            .setDefaultFloatValue(0.08F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(0.08F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();
    private final FloatValue grimMoveFlying = ValueBuilder.create(this, "Move Flying Increase")
            .setDefaultFloatValue(0.1f)
            .setMinFloatValue(-1F)
            .setMaxFloatValue(1F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();
    private final BooleanValue grimFastFall = ValueBuilder.create(this, "Fast Fall")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();


    private int grimAirTicks;
    private int grimGroundState;

    @Override
    public void onEnable() {
        grimAirTicks = 0;
        grimGroundState = 0;

    }

    @Override
    public void onDisable() {
        if (mc.options != null) {
            mc.options.keyShift.setDown(false);
            mc.options.keyJump.setDown(false);
        }
        grimAirTicks = 0;
        grimGroundState = 0;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        grimAirTicks = !mc.player.onGround() ? grimAirTicks + 1 : 0;

        if (!isInWeb()
                && !mc.player.onGround()
                && !(mc.options.keyUp.isDown() && (mc.options.keyRight.isDown() || mc.options.keyLeft.isDown()))
                && !grimFastFall.getCurrentValue()) {
            float yaw = (float) (mc.player.getYRot() + 45.0F + Math.random());
            RotationManager.setRotations(new Rotation(yaw, mc.player.getXRot()), 10.0);
        }
    }


    @EventTarget
    public void onMotion(EventMotion event) {
        if (mc.player == null) {
            return;
        }

        if (!grimFastFall.getCurrentValue()) {
            return;
        }
        if (event.getType() == EventType.PRE) {
            grimGroundState = grimGroundState == 0 ? (event.isOnGround() ? 1 : 0) : 0;
            event.setOnGround(grimGroundState != 0);
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
        } else if (event.getType() == EventType.POST) {
            if (!mc.player.onGround() && grimGroundState >= 0) {
                if (grimAirTicks < 1 || grimAirTicks > 5) {
                    return;
                }
                sendStartFallFlying();
                mc.options.keyShift.setDown(false);
                if (grimAirTicks == 2) {
                    mc.player.jumpFromGround();
                }
            }
        }
    }

    @EventTarget
    public void onStrafe(EventStrafe event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        AABB expanded = mc.player.getBoundingBox().inflate(grimHorizontal.getCurrentValue());
        StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> entity != mc.player && expanded.intersects(entity.getBoundingBox()))
                .forEach(entity -> moveFlying(grimVertical.getCurrentValue()));

        moveFlying(grimMoveFlying.getCurrentValue() / 1000D);
    }


    private void sendStartFallFlying() {
        if (mc.getConnection() == null || mc.player == null) {
            return;
        }
        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
    }


    private void moveFlying(double speed) {
        if (!MoveUtils.isMoving()) {
            return;
        }
        double direction = getDirection();
        Vec3 motion = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(
                motion.x + -Math.sin(direction) * speed,
                motion.y,
                motion.z + Math.cos(direction) * speed
        );
    }

    private double getDirection() {
        float forward = mc.player.input.forwardImpulse;
        float strafe = mc.player.input.leftImpulse;
        float yaw = mc.player.getYRot();
        if (forward < 0.0F) {
            yaw += 180.0F;
        }
        float factor = 1.0F;
        if (forward < 0.0F) {
            factor = -0.5F;
        } else if (forward > 0.0F) {
            factor = 0.5F;
        }
        if (strafe > 0.0F) {
            yaw -= 90.0F * factor;
        }
        if (strafe < 0.0F) {
            yaw += 90.0F * factor;
        }
        return Math.toRadians(yaw);
    }

    private boolean isInWeb() {
        return mc.level.getBlockState(mc.player.blockPosition()).getBlock() instanceof WebBlock;
    }
}
