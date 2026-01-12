package com.heypixel.heypixelmod.obsoverlay.utils.rotation;

import com.heypixel.heypixelmod.mixin.O.accessors.AccessorEntity;
import com.heypixel.heypixelmod.obsoverlay.utils.MathConst;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RotationUtils {
    private static final Minecraft mc = Minecraft.getInstance();


    public static Rotation calculate(final Vec3 from, final Vec3 to) {
        final Vec3 diff = to.subtract(from);
        final double distance = Math.hypot(diff.x, diff.z);
        final float yaw = (float) (Mth.atan2(diff.z, diff.x) * MathConst.TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(Mth.atan2(diff.y, distance) * MathConst.TO_DEGREES));
        return new Rotation(yaw, pitch);
    }

    public static Rotation calculate(final Entity entity) {
        return calculate(entity.position().add(0, Math.max(0, Math.min(mc.player.getY() - entity.getY() +
                mc.player.getEyeHeight(), (entity.getBoundingBox().maxY - entity.getBoundingBox().minY) * 0.9)), 0));
    }

    public static Rotation calculate(final Entity entity, final boolean adaptive, final double range) {
        Rotation normalRotations = RotationUtils.calculate(entity);

        HitResult result = RayCastUtil.rayCast(normalRotations, range, 0.0f);

        if (!adaptive || (result != null && result.getType() == HitResult.Type.ENTITY)) {
            return normalRotations;
        }

        AABB bb = entity.getBoundingBox();
        double minX = bb.minX;
        double maxX = bb.maxX;
        double minY = bb.minY;
        double maxY = bb.maxY;
        double minZ = bb.minZ;
        double maxZ = bb.maxZ;

        Vec3 basePos = entity.position();

        for (double yPercent = 1; yPercent >= 0; yPercent -= 0.25 + Math.random() * 0.1) {
            for (double xPercent = 1; xPercent >= -0.5; xPercent -= 0.5) {
                for (double zPercent = 1; zPercent >= -0.5; zPercent -= 0.5) {

                    double offsetX = (maxX - minX) * xPercent;
                    double offsetY = (maxY - minY) * yPercent;
                    double offsetZ = (maxZ - minZ) * zPercent;

                    Vec3 targetPoint = basePos.add(offsetX, offsetY, offsetZ);

                    Rotation adaptiveRotations = RotationUtils.calculate(targetPoint);

                    HitResult rayCastResult = RayCastUtil.rayCast(adaptiveRotations, range, 0.0f);

                    if (rayCastResult != null && rayCastResult.getType() == HitResult.Type.ENTITY) {
                        return adaptiveRotations;
                    }
                }
            }
        }

        return normalRotations;
    }


    public static Rotation calculate(final BlockPos to) {
        return calculate(mc.player.getEyePosition(), new Vec3(to.getX(), to.getY(), to.getZ()).add(0.5, 0.5, 0.5));
    }

    public static Rotation calculate(final Vec3 to) {
        return calculate(mc.player.getEyePosition(), to);
    }

    public static Rotation calculate(final Vec3 position, final Direction enumFacing) {
        double x = position.x + 0.5D;
        double y = position.y + 0.5D;
        double z = position.z + 0.5D;

        x += (double) enumFacing.getNormal().getX() * 0.5D;
        y += (double) enumFacing.getNormal().getY() * 0.5D;
        z += (double) enumFacing.getNormal().getZ() * 0.5D;
        return calculate(new Vec3(x, y, z));
    }


    public static Rotation calculate(final BlockPos position, final Direction enumFacing) {
        double x = position.getX() + 0.5D;
        double y = position.getY() + 0.5D;
        double z = position.getZ() + 0.5D;

        x += (double) enumFacing.getNormal().getX() * 0.5D;
        y += (double) enumFacing.getNormal().getY() * 0.5D;
        z += (double) enumFacing.getNormal().getZ() * 0.5D;
        return calculate(new Vec3(x, y, z));
    }

    public static Rotation applySensitivityPatch(final Rotation rotation) {
        AccessorEntity accessorEntity = (AccessorEntity) mc.player;
        final Rotation previousRotation = new Rotation(accessorEntity.getPreviousYRot(), accessorEntity.getPreviousXRot());
        final float mouseSensitivity = (float) (mc.options.sensitivity().get() * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        final double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        final float yaw = previousRotation.getYaw() + (float) (Math.round((rotation.getYaw() - previousRotation.getYaw()) / multiplier) * multiplier);
        final float pitch = previousRotation.getPitch() + (float) (Math.round((rotation.getPitch() - previousRotation.getPitch()) / multiplier) * multiplier);
        return new Rotation(yaw, Mth.clamp(pitch, -90, 90));
    }

    public static Rotation applySensitivityPatch(final Rotation rotation, final Rotation previousRotation) {
        final float mouseSensitivity = (float) (mc.options.sensitivity().get() * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        final double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        final float yaw = previousRotation.getYaw() + (float) (Math.round((rotation.getYaw() - previousRotation.getYaw()) / multiplier) * multiplier);
        final float pitch = previousRotation.getPitch() + (float) (Math.round((rotation.getPitch() - previousRotation.getPitch()) / multiplier) * multiplier);
        return new Rotation(yaw, Mth.clamp(pitch, -90, 90));
    }

    public static Rotation relateToPlayerRotation(final Rotation rotation) {
        AccessorEntity accessorEntity = (AccessorEntity) mc.player;
        final Rotation previousRotation = new Rotation(accessorEntity.getPreviousYRot(), accessorEntity.getPreviousXRot());
        final float yaw = previousRotation.getYaw() + Mth.wrapDegrees(rotation.getYaw() - previousRotation.getYaw());
        final float pitch = Mth.clamp(rotation.getPitch(), -90, 90);
        return new Rotation(yaw, pitch);
    }

    public static Rotation resetRotation(final Rotation rotation) {
        if (rotation == null) {
            return null;
        }

        final float yaw = rotation.getYaw() + Mth.wrapDegrees(mc.player.getYRot() - rotation.getYaw());
        final float pitch = mc.player.getXRot();
        return new Rotation(yaw, pitch);
    }

    public static Rotation move(final Rotation targetRotation, final double speed) {
        return move(RotationManager.lastRotations, targetRotation, speed);
    }

    public static Rotation move(final Rotation lastRotation, final Rotation targetRotation, double speed) {
        if (speed != 0) {

            double deltaYaw = Mth.wrapDegrees(targetRotation.getYaw() - lastRotation.getYaw());
            final double deltaPitch = (targetRotation.getPitch() - lastRotation.getPitch());

            final double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            final double distributionYaw = Math.abs(deltaYaw / distance);
            final double distributionPitch = Math.abs(deltaPitch / distance);

            final double maxYaw = speed * distributionYaw;
            final double maxPitch = speed * distributionPitch;

            final float moveYaw = (float) Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
            final float movePitch = (float) Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);

            return new Rotation(moveYaw, movePitch);
        }

        return new Rotation(0, 0);
    }

    public static Rotation smooth(final Rotation targetRotation, final double speed) {
        return smooth(RotationManager.lastRotations, targetRotation, speed);
    }

    public static Rotation smooth(final Rotation lastRotation, final Rotation targetRotation, final double speed) {
        float yaw = targetRotation.getYaw();
        float pitch = targetRotation.getPitch();
        final float lastYaw = lastRotation.getYaw();
        final float lastPitch = lastRotation.getPitch();

        if (speed != 0) {
            Rotation move = move(targetRotation, speed);

            yaw = lastYaw + move.getYaw();
            pitch = lastPitch + move.getPitch();

            for (int i = 1; i <= (int) (Minecraft.getInstance().getFps() / 20f + Math.random() * 10); ++i) {

                if (Math.abs(move.getYaw()) + Math.abs(move.getPitch()) > 0.0001) {
                    yaw += (Math.random() - 0.5) / 1000;
                    pitch -= Math.random() / 200;
                }

                /*
                 * Fixing GCD
                 */
                final Rotation rotations = new Rotation(yaw, pitch);
                final Rotation fixedRotations = applySensitivityPatch(rotations);

                /*
                 * Setting rotations
                 */
                yaw = shortestYaw(lastYaw, fixedRotations.getYaw());
                pitch = Math.max(-90, Math.min(90, fixedRotations.getPitch()));
            }
        }

        return new Rotation(yaw, pitch);
    }

    public static double getDistance(final Entity entity) {
        Vec3 eyes = mc.player.getEyePosition();
        AABB aabb = entity.getBoundingBox();

        double x = Mth.clamp(eyes.x, aabb.minX, aabb.maxX);
        double y = Mth.clamp(eyes.y, aabb.minY, aabb.maxY);
        double z = Mth.clamp(eyes.z, aabb.minZ, aabb.maxZ);

        return Math.sqrt(eyes.distanceToSqr(x, y, z));
    }

    public static double getDistance(Player player, final Entity entity) {
        Vec3 eyes = player.getEyePosition();
        AABB aabb = entity.getBoundingBox();

        double x = Mth.clamp(eyes.x, aabb.minX, aabb.maxX);
        double y = Mth.clamp(eyes.y, aabb.minY, aabb.maxY);
        double z = Mth.clamp(eyes.z, aabb.minZ, aabb.maxZ);

        return Math.sqrt(eyes.distanceToSqr(x, y, z));
    }

    public static float shortestYaw(float from, float to) {
        return from + Mth.wrapDegrees(to - from);
    }
}
