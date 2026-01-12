package com.heypixel.heypixelmod.obsoverlay.utils.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.util.List;
import java.util.Optional;

public final class RayCastUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    public static HitResult rayCast(final Rotation rotation, final double range) {
        return rayCast(rotation, range, 0);
    }

    public static boolean inView(final Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == null) return false;

        double renderDistance = mc.options.renderDistance().get() * 16.0;

        Rotation rotations = RotationUtils.calculate(entity);

        if (Math.abs(Mth.wrapDegrees(mc.player.getYRot() - rotations.getYaw())) > mc.options.fov().get()) {
            return false;
        }

        double dist = mc.player.distanceTo(entity);

        if (dist > 100 || !(entity instanceof Player)) {
            HitResult result = RayCastUtil.rayCast(rotations, renderDistance, 0.2f);
            return result != null && result.getType() == HitResult.Type.ENTITY;
        } else {
            AABB bb = entity.getBoundingBox();
            double width = bb.getXsize();
            double height = bb.getYsize();
            double depth = bb.getZsize();
            Vec3 basePos = entity.position();

            for (double yPercent = 1; yPercent >= -1; yPercent -= 0.5) {
                for (double xPercent = 1; xPercent >= -1; xPercent -= 1.0) {
                    for (double zPercent = 1; zPercent >= -1; zPercent -= 1.0) {

                        Vec3 targetPoint = basePos.add(width * xPercent, height * yPercent, depth * zPercent);
                        Rotation subRotations = RotationUtils.calculate(targetPoint);

                        HitResult result = RayCastUtil.rayCast(subRotations, renderDistance, 0.2f);
                        if (result != null && result.getType() == HitResult.Type.ENTITY) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static HitResult rayCast(final Rotation rotation, final double range, final float expand) {
        return rayCast(rotation, range, expand, mc.player);
    }

    public static HitResult rayCast(final Rotation rotation, final double range, final float expand, Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || entity == null) return null;

        float partialTicks = mc.getFrameTime();

        Vec3 eyePos = entity.getEyePosition(partialTicks);
        Vec3 lookVec = Vec3.directionFromRotation(rotation.getPitch(), rotation.getYaw());
        Vec3 endVec = eyePos.add(lookVec.scale(range));

        HitResult objectMouseOver = mc.level.clip(new ClipContext(
                eyePos,
                endVec,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                entity
        ));

        double distToBlock = range;
        if (objectMouseOver.getType() != HitResult.Type.MISS) {
            distToBlock = objectMouseOver.getLocation().distanceTo(eyePos);
        }

        Vec3 entitySearchEndVec = eyePos.add(lookVec.scale(range));

        Entity pointedEntity = null;
        Vec3 hitVec = null;
        double currentDist = distToBlock;

        AABB searchBox = entity.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D);

        List<Entity> list = mc.level.getEntities(entity, searchBox, e -> !e.isSpectator() && e.isPickable());

        for (Entity candidate : list) {
            float collisionSize = candidate.getPickRadius() + expand;
            AABB entityBox = candidate.getBoundingBox().inflate(collisionSize);

            Optional<Vec3> intercept = entityBox.clip(eyePos, entitySearchEndVec);

            if (entityBox.contains(eyePos)) {
                if (currentDist >= 0.0D) {
                    pointedEntity = candidate;
                    hitVec = intercept.orElse(eyePos);
                    currentDist = 0.0D;
                }
            } else if (intercept.isPresent()) {
                Vec3 interceptVec = intercept.get();
                double d3 = eyePos.distanceTo(interceptVec);

                if (d3 < currentDist || currentDist == 0.0D) {
                    if (candidate.getRootVehicle() == entity.getRootVehicle() && !candidate.canRiderInteract()) {
                        if (currentDist == 0.0D) {
                            pointedEntity = candidate;
                            hitVec = interceptVec;
                        }
                    } else {
                        pointedEntity = candidate;
                        hitVec = interceptVec;
                        currentDist = d3;
                    }
                }
            }
        }

        if (pointedEntity != null && (currentDist < distToBlock || objectMouseOver.getType() == HitResult.Type.MISS)) {
            return new EntityHitResult(pointedEntity, hitVec);
        }

        return objectMouseOver;
    }

    public static boolean overBlock(final Rotation rotation, final Direction enumFacing, final BlockPos pos, final boolean strict) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) return false;

        Vec3 lookVec = Vec3.directionFromRotation(rotation.getPitch(), rotation.getYaw());

        Vec3 eyePos = mc.player.getEyePosition(1.0F);
        double reach = 4.5D;
        Vec3 endVec = eyePos.add(lookVec.scale(reach));

        BlockHitResult result = mc.level.clip(new ClipContext(
                eyePos,
                endVec,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        if (result.getType() == HitResult.Type.MISS) return false;

        return result.getBlockPos().equals(pos)
                && (!strict || result.getDirection() == enumFacing);
    }

    public static boolean overBlock(final Direction enumFacing, final BlockPos pos, final boolean strict) {
        return overBlock(RotationManager.getRotation(), Direction.UP, pos, false);
    }

    public static Boolean overBlock(final Rotation rotation, final BlockPos pos) {
        return overBlock(rotation, Direction.UP, pos, false);
    }

    public static Boolean overBlock(final Rotation rotation, final BlockPos pos, final Direction enumFacing) {
        return overBlock(rotation, enumFacing, pos, true);
    }


}
