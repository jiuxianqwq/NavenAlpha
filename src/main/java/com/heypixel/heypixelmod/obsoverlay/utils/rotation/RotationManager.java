package com.heypixel.heypixelmod.obsoverlay.utils.rotation;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.Priority;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.util.Mth;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.function.Function;

public class RotationManager {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Rotation offset = new Rotation(0, 0);
    public static Rotation rotations, lastRotations = new Rotation(0, 0), targetRotations, animationRotation, lastAnimationRotation;
    @Setter
    @Getter
    private static boolean active, smoothed;
    private static double rotationSpeed;
    private static Function<Rotation, Boolean> raycast;
    private static float randomAngle;

    public static void setRotations(final Rotation rotations, final double rotationSpeed) {
        setRotations(rotations, rotationSpeed, null);

        if (AuthUtils.transport == null || AuthUtils.authed.get().length() != 32) {
            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
        }
    }

    public static void setRotations(final Rotation rotations, final double rotationSpeed, final Function<Rotation, Boolean> raycast) {
        RotationManager.targetRotations = rotations;
        RotationManager.rotationSpeed = rotationSpeed;
        RotationManager.raycast = raycast;
        active = true;

        smooth();
    }

    public static void smooth() {
        if (!smoothed) {
            float targetYaw = targetRotations.getYaw();
            float targetPitch = targetRotations.getPitch();

            if (raycast != null && (Math.abs(targetYaw - rotations.getYaw()) > 5 || Math.abs(targetPitch - rotations.getPitch()) > 5)) {
                final Rotation trueTargetRotations = new Rotation(targetRotations.getYaw(), targetRotations.getPitch());

                double speed = /*Math.min(*/(Math.random() * Math.random() * Math.random()) * 20/*, MoveUtil.speed() * 30)*/;
                randomAngle += (float) ((20 + (float) (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360)) * (mc.player.tickCount / 10 % 2 == 0 ? -1 : 1));

                offset.setYaw((float) (offset.getYaw() + -Mth.sin((float) Math.toRadians(randomAngle)) * speed));
                offset.setPitch((float) (offset.getPitch() + Mth.cos((float) Math.toRadians(randomAngle)) * speed));

                targetYaw += offset.getYaw();
                targetPitch += offset.getPitch();

                if (!raycast.apply(new Rotation(targetYaw, targetPitch))) {
                    randomAngle = (float) Math.toDegrees(Math.atan2(trueTargetRotations.getYaw() - targetYaw, targetPitch - trueTargetRotations.getPitch())) - 180;

                    targetYaw -= offset.getYaw();
                    targetPitch -= offset.getPitch();

                    offset.setYaw((float) (offset.getYaw() + -Mth.sin((float) Math.toRadians(randomAngle)) * speed));
                    offset.setPitch((float) (offset.getPitch() + Mth.cos((float) Math.toRadians(randomAngle)) * speed));

                    targetYaw = targetYaw + offset.getYaw();
                    targetPitch = targetPitch + offset.getPitch();
                }

                if (!raycast.apply(new Rotation(targetYaw, targetPitch))) {
                    offset.setYaw(0);
                    offset.setPitch(0);

                    targetYaw = (float) (targetRotations.getYaw() + Math.random() * 2);
                    targetPitch = (float) (targetRotations.getPitch() + Math.random() * 2);
                }
            }

            rotations = RotationUtils.smooth(new Rotation(targetYaw, targetPitch),
                    rotationSpeed + Math.random());

        }

        smoothed = true;

        mc.gameRenderer.pick(1.0F);
    }

    public static Rotation getRotation() {
        return active ? rotations : new Rotation(mc.player.getYRot(), mc.player.getXRot());
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        lastRotations = null;
        rotations = null;
    }

    @EventTarget(4)
    public void updateGlobalYaw(EventRunTicks e) {
        if (e.type() == EventType.PRE && mc.player != null) {

            if (!active || rotations == null || lastRotations == null || targetRotations == null) {
                rotations = lastRotations = targetRotations = new Rotation(mc.player.getYRot(), mc.player.getXRot());
            }

            if (active) {
                smooth();
            }
        }
    }

    @EventTarget
    public void onAnimation(EventRotationAnimation e) {
        if (active && animationRotation != null && lastAnimationRotation != null) {
            e.setYaw(animationRotation.getYaw());
            e.setLastYaw(lastAnimationRotation.getYaw());
            e.setPitch(animationRotation.getPitch());
            e.setLastPitch(lastAnimationRotation.getPitch());
        }
    }

    @EventTarget(4)
    public void onPre(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (active && rotations != null) {

                float yaw = rotations.getYaw();
                float pitch = rotations.getPitch();
                if (!Float.isNaN(yaw) && !Float.isNaN(pitch) && active) {
                    e.setYaw(yaw);
                    e.setPitch(pitch);
                }

                if (Math.abs((rotations.getYaw() - mc.player.getYRot()) % 360) < 1 && Math.abs((rotations.getPitch() - mc.player.getXRot())) < 1) {
                    active = false;

                    this.correctDisabledRotations();
                }

                lastRotations = rotations;
            } else {
                lastRotations = new Rotation(mc.player.getYRot(), mc.player.getXRot());
            }

            lastAnimationRotation = animationRotation;
            animationRotation = new Rotation(e.getYaw(), e.getPitch());
            targetRotations = new Rotation(mc.player.getYRot(), mc.player.getXRot());
            smoothed = false;
        }
    }

    @EventTarget(Priority.HIGH)
    public void onMove(EventMoveInput event) {
        if (active && rotations != null) {
            float yaw = rotations.getYaw();
            MoveUtils.fixMovement(event, yaw);
        }
    }

    @EventTarget
    public void onMove(EventRayTrace event) {
        if (rotations != null && event.entity == mc.player && active) {
            event.setYaw(rotations.getYaw());
            event.setPitch(rotations.getPitch());
        }
    }

    @EventTarget
    public void onItemRayTrace(EventUseItemRayTrace event) {
        if (rotations != null && active) {
            event.setYaw(rotations.getYaw());
            event.setPitch(rotations.getPitch());
        }
    }

    @EventTarget
    public void onStrafe(EventStrafe event) {
        if (active && rotations != null) {
            event.setYaw(rotations.getYaw());
        }
    }

    @EventTarget
    public void onJump(EventJump event) {
        if (active && rotations != null) {
            event.setYaw(rotations.getYaw());
        }
    }

    @EventTarget(0)
    public void onPositionItem(EventPositionItem e) {
        if (active && rotations != null) {
            PosRot packet = (PosRot) e.getPacket();
            PosRot newPacket = new PosRot(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), rotations.getYaw(), rotations.getYaw(), packet.isOnGround());
            e.setPacket(newPacket);
        }
    }

    @EventTarget
    public void onFallFlying(EventFallFlying e) {
        if (rotations != null) {
            e.setPitch(rotations.getPitch());
        }
    }

    @EventTarget
    public void onAttack(EventAttackYaw e) {
        if (rotations != null) {
            e.setYaw(rotations.getYaw());
        }
    }

    private void correctDisabledRotations() {
        final Rotation rotations = new Rotation(mc.player.getYRot(), mc.player.getXRot());
        final Rotation fixedRotations = RotationUtils.resetRotation(RotationUtils.applySensitivityPatch(rotations, lastRotations));

        mc.player.setYRot(fixedRotations.getYaw());
        mc.player.setXRot(fixedRotations.getPitch());
    }
}
