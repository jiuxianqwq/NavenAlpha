package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.KillAura;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.StreamSupport;

/**
 * @Author：jiuxian_baka
 * @Date：2026/1/10 02:49
 * @Filename：GrimSpeed
 */

@ModuleInfo(
        name = "GrimSpeed", cnName = "严峻的嘻嘻哈哈", description = "Movement speed adjustments with Grim modes", category = Category.MOVEMENT)
public class GrimSpeed extends Module {

    private final BooleanValue logging = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();

    private final BooleanValue grimFastFall = ValueBuilder.create(this, "Fast Fall")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final BooleanValue tick1 = ValueBuilder.create(this, "Fast Fall On 1 Tick")
            .setDefaultBooleanValue(false)
            .setVisibility(grimFastFall::getCurrentValue)
            .build()
            .getBooleanValue();
    private final BooleanValue tick2 = ValueBuilder.create(this, "Fast Fall On 2 Tick")
            .setDefaultBooleanValue(false)
            .setVisibility(grimFastFall::getCurrentValue)
            .build()
            .getBooleanValue();
    private final BooleanValue tick3 = ValueBuilder.create(this, "Fast Fall On 3 Tick")
            .setDefaultBooleanValue(false)
            .setVisibility(grimFastFall::getCurrentValue)
            .build()
            .getBooleanValue();
    private final BooleanValue tick4 = ValueBuilder.create(this, "Fast Fall On 4 Tick")
            .setDefaultBooleanValue(false)
            .setVisibility(grimFastFall::getCurrentValue)
            .build()
            .getBooleanValue();
    private final BooleanValue tick5 = ValueBuilder.create(this, "Fast Fall On 5 Tick")
            .setDefaultBooleanValue(false)
            .setVisibility(grimFastFall::getCurrentValue)
            .build()
            .getBooleanValue();
    private final BooleanValue rotation = ValueBuilder.create(this, "Rotation")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final FloatValue grimMoveFlying = ValueBuilder.create(this, "Speed")
            .setDefaultFloatValue(0.1f)
            .setMinFloatValue(0F)
            .setMaxFloatValue(10F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();
    private final FloatValue startFall = ValueBuilder.create(this, "Start Fast Fall")
            .setDefaultFloatValue(2f)
            .setMinFloatValue(0F)
            .setMaxFloatValue(10F)
            .setFloatStep(1F)
            .setVisibility(grimFastFall::getCurrentValue)
            .build()
            .getFloatValue();
    private final FloatValue packetCount = ValueBuilder.create(this, "Packet Count")
            .setDefaultFloatValue(1f)
            .setMinFloatValue(1F)
            .setMaxFloatValue(10F)
            .setFloatStep(1F)
            .setVisibility(grimFastFall::getCurrentValue)
            .build()
            .getFloatValue();
    private final FloatValue skipTicks = ValueBuilder.create(this, "Skip Ticks")
            .setDefaultFloatValue(2f)
            .setMinFloatValue(1F)
            .setMaxFloatValue(10F)
            .setFloatStep(1F)
            .setVisibility(grimFastFall::getCurrentValue)
            .build()
            .getFloatValue();
    private final FloatValue ticks = ValueBuilder.create(this, "Ticks")
            .setDefaultFloatValue(3f)
            .setMinFloatValue(1F)
            .setMaxFloatValue(10F)
            .setFloatStep(1F)
            .setVisibility(grimFastFall::getCurrentValue)
            .build()
            .getFloatValue();

    private void log(String message) {
        if (this.logging.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    private int airTicks;

    @Override
    public void onEnable() {
        airTicks = 0;
    }

    @Override
    public void onDisable() {
        airTicks = 0;
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

    @EventTarget
    public void onMotion(EventMotion event) {
        if (mc.player == null) {
            return;
        }

        if (!grimFastFall.getCurrentValue()) {
            return;
        }
        if (event.getType() == EventType.PRE) {
            if (mc.player.onGround()) airTicks = 0; else airTicks++;
        } else if (event.getType() == EventType.POST) {
            if (!mc.player.onGround() && airTicks > startFall.getCurrentValue()) {
                log("Air ticks: " + airTicks);
                switch (airTicks - (int) (startFall.getCurrentValue())) {
                    case 1 -> {
                        if (tick1.getCurrentValue()) {
                            sendPacket();
                        }
                    }
                    case 2 -> {
                        if (tick2.getCurrentValue()) {
                            sendPacket();
                        }
                    }
                    case 3 -> {
                        if (tick3.getCurrentValue()) {
                            sendPacket();
                        }
                    }
                    case 4 -> {
                        if (tick4.getCurrentValue()) {
                            sendPacket();
                        }
                    }
                    case 5 -> {
                        if (tick5.getCurrentValue()) {
                        }
                    }
                }
            }
        }
    }

    private void sendPacket() {
        for (int i = 0; i < (int) packetCount.getCurrentValue(); i++) {
            NetworkUtils.sendPacketNoEvent(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
        mc.options.keyShift.setDown(false);
        mc.player.jumpFromGround();
        moveFlying(grimMoveFlying.getCurrentValue() / 10D);
        for (int i = 0; i < (int) ticks.getCurrentValue(); i++) {
            mc.player.tick();
        }
        Naven.skipTicks += skipTicks.getCurrentValue();
        log("fall");
    }

    @EventTarget
    public void onPreTick(EventRunTicks event) {
        if (event.type() != EventType.PRE || !rotation.getCurrentValue()) return;

        Scaffold scaffold = Naven.getInstance().getModuleManager().getModule(Scaffold.class);
        KillAura killAura = Naven.getInstance().getModuleManager().getModule(KillAura.class);
        float randomFloatInRange = MathUtils.getRandomFloatInRange(-1f, 1f);
        if (!mc.player.onGround() && !scaffold.isEnabled() && killAura.target == null) RotationManager.setRotations(new Rotation(Mth.wrapDegrees(mc.player.getYRot() - 45.0F) + randomFloatInRange, mc.player.getXRot() + randomFloatInRange), 360);
    }

//    @EventTarget
//    public void onStrafe(EventStrafe event) {
//        if (mc.player == null || mc.level == null) {
//            return;
//        }
//        Scaffold scaffold = Naven.getInstance().getModuleManager().getModule(Scaffold.class);
//        KillAura killAura = Naven.getInstance().getModuleManager().getModule(KillAura.class);
//        if (!scaffold.isEnabled() && killAura.target == null) moveFlying(grimMoveFlying.getCurrentValue() / 1000D);
//    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        event.setJump(true);
    }


}
