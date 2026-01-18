package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderAfterWorld;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.ClientFriend;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Target;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.utils.RayTraceUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(
        name = "AimAssist",
        cnName = "瞄准辅助",
        description = "Automatically aims at targets",
        category = Category.COMBAT
)
public class AimAssist extends Module {
    private final FloatValue speed = ValueBuilder.create(this, "Speed")
            .setDefaultFloatValue(2.0f)
            .setMinFloatValue(1.0f)
            .setMaxFloatValue(10.0f)
            .setFloatStep(0.1f)
            .build()
            .getFloatValue();

    private final BooleanValue aimPitch = ValueBuilder.create(this, "Aim Pitch")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue click = ValueBuilder.create(this, "Require Swinging")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue sticky = ValueBuilder.create(this, "Sticky")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue mouseMovement = ValueBuilder.create(this, "Require Mouse Movement")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue limitItems = ValueBuilder.create(this, "Limit Items")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue aimWhilstOnTarget = ValueBuilder.create(this, "Aim Whilst on Target")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final FloatValue onTargetSpeed = ValueBuilder.create(this, "On Target Speed")
            .setDefaultFloatValue(1.0f)
            .setMinFloatValue(1.0f)
            .setMaxFloatValue(10.0f)
            .setFloatStep(0.1f)
            .setVisibility(aimWhilstOnTarget::getCurrentValue)
            .build()
            .getFloatValue();

    private final FloatValue fov = ValueBuilder.create(this, "FOV")
            .setDefaultFloatValue(90.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(180.0f)
            .setFloatStep(1.0f)
            .build()
            .getFloatValue();


    private float moveX;
    private float moveY;
    private LivingEntity target;
    private double lastMouseX;
    private double lastMouseY;

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE) return;

        double range = 4.0;
        moveX = 0;
        moveY = 0;
        target = null;

        List<LivingEntity> targets = getTargets(range);

        if (targets.isEmpty()) {
            return;
        }

        target = targets.get(0);

        Rotation neededRot = RotationUtils.calculate(target);
        float yawDiff = Math.abs(Mth.wrapDegrees(neededRot.getYaw() - mc.player.getYRot()));
        if (yawDiff > fov.getCurrentValue()) {
            target = null;
            return;
        }

        if (limitItems.getCurrentValue() && (mc.player.getMainHandItem().isEmpty() || !(mc.player.getMainHandItem().getItem() instanceof SwordItem))) {
            target = null;
            return;
        }

        float speedVal = speed.getCurrentValue();
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) speedVal = onTargetSpeed.getCurrentValue();
        if (sticky.getCurrentValue()) {
            speedVal *= 10;
        }
        
        float adjustedSpeed = (speedVal / Minecraft.getInstance().getFps()) * 100;

        Rotation currentRot = new Rotation(mc.player.getYRot(), mc.player.getXRot());
        Rotation nextRot = RotationUtils.move(currentRot, neededRot, adjustedSpeed);
        
        moveX = nextRot.getYaw();
        moveY = nextRot.getPitch();
    }

    @EventTarget
    public void onRender3D(EventRenderAfterWorld event) {
        if (target == null || (moveX == 0 && moveY == 0)) return;

        double curMouseX = mc.mouseHandler.xpos();
        double curMouseY = mc.mouseHandler.ypos();
        boolean isMouseMoving = curMouseX != lastMouseX || curMouseY != lastMouseY;
        lastMouseX = curMouseX;
        lastMouseY = curMouseY;

        if (mouseMovement.getCurrentValue() && !isMouseMoving) {
             return;
        }

        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY && !aimWhilstOnTarget.getCurrentValue()) {
            return;
        }
        
        if (click.getCurrentValue() && !mc.options.keyAttack.isDown()) {
            return;
        }

        double sensitivity = mc.options.sensitivity().get() * 0.6 + 0.2;
        double gcd = sensitivity * sensitivity * sensitivity * 8.0;
        double sensitivityStep = gcd * 0.15;

        float fixedMoveX = (float) (Math.round(moveX / sensitivityStep) * sensitivityStep);
        float fixedMoveY = (float) (Math.round(moveY / sensitivityStep) * sensitivityStep);

        boolean applyYaw = Math.abs(fixedMoveX) >= sensitivityStep;
        boolean applyPitch = aimPitch.getCurrentValue() && Math.abs(fixedMoveY) >= sensitivityStep;

        if (!applyYaw && !applyPitch) return;

        mc.player.turn(applyYaw ? fixedMoveX : 0, applyPitch ? fixedMoveY : 0);
    }

    private List<LivingEntity> getTargets(double range) {
        List<LivingEntity> targets = new ArrayList<>();
        if (mc.level == null) return targets;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && isValiadTarget(living)) {
                if (mc.player.distanceTo(living) <= range) {
                    Rotation neededRot = RotationUtils.calculate(living);
                    float yawDiff = Math.abs(Mth.wrapDegrees(neededRot.getYaw() - mc.player.getYRot()));
                    if (yawDiff <= fov.getCurrentValue()) {
                        targets.add(living);
                    }
                }
            }
        }
        
        targets.sort(Comparator.comparingDouble(e -> {
            Rotation rot = RotationUtils.calculate(e);
            float yawDiff = Math.abs(Mth.wrapDegrees(rot.getYaw() - mc.player.getYRot()));
            float pitchDiff = Math.abs(rot.getPitch() - mc.player.getXRot());
            return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        }));
        return targets;
    }

    private boolean isValiadTarget(LivingEntity entity) {
        if (entity == mc.player) return false;
        if (!entity.isAlive()) return false;
        
        if (!mc.player.hasLineOfSight(entity)) return false;
        
        // AntiBot
        if (AntiBots.isBot(entity)) return false;
        
        // Teams
        if (Teams.isSameTeam(entity)) return false;
        
        // ClientFriend
        if (ClientFriend.isUser(entity)) return false;
        
        // Target
        Target targetModule = Naven.getInstance().getModuleManager().getModule(Target.class);
        if (targetModule != null && targetModule.isEnabled()) {
             if (!targetModule.isTarget(entity)) return false;
        } else {
             if (entity instanceof Player) return true;
             return false; 
        }

        return true;
    }
}
