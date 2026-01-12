package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class RayTraceUtils {
    private static final Minecraft mc = Minecraft.getInstance();

    public static Vec3 calculateViewVector(float pXRot, float pYRot) {
        float f = pXRot * (float) (Math.PI / 180.0);
        float f1 = -pYRot * (float) (Math.PI / 180.0);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3(f3 * f4, -f5, f2 * f4);
    }

    public static HitResult pick(double pHitDistance, boolean pHitFluids, float pYRot, float pXRot) {
        Vec3 vec3 = new Vec3(mc.player.getX(), mc.player.getY() + 1.62, mc.player.getZ());
        Vec3 vec31 = calculateViewVector(pXRot, pYRot);
        Vec3 vec32 = vec3.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
        return mc.player.level().clip(new ClipContext(vec3, vec32, Block.OUTLINE, pHitFluids ? Fluid.ANY : Fluid.NONE, mc.player));
    }

    public static HitResult rayTraceBlocks(Vec3 start, Vec3 end, boolean includeLiquids, boolean colliderOnly, boolean useVisualShape, Entity entity) {
        Block var7;
        if (colliderOnly) {
            var7 = Block.COLLIDER;
        } else {
            var7 = useVisualShape ? Block.VISUAL : Block.OUTLINE;
        }

        Fluid var8 = includeLiquids ? Fluid.ANY : Fluid.NONE;
        ClipContext var9 = new ClipContext(start, end, var7, var8, entity);
        return mc.level.clip(var9);
    }

    public static EntityHitResult calculateIntercept(AABB instance, Vec3 var1, Vec3 var2) {
        Optional<Vec3> e = instance.clip(var1, var2);
        return e.map(vec3 -> new EntityHitResult(null, vec3)).orElse(null);
    }
}
