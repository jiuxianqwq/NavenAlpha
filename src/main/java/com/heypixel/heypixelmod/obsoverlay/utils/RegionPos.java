package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public class RegionPos {
    private final int x;
    private final int z;

    public RegionPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public static RegionPos of(BlockPos pos) {
        return new RegionPos(pos.getX() >> 9 << 9, pos.getZ() >> 9 << 9);
    }

    public static RegionPos of(ChunkPos pos) {
        return new RegionPos(pos.x >> 5 << 9, pos.z >> 5 << 9);
    }

    public RegionPos negate() {
        return new RegionPos(-this.x, -this.z);
    }

    public Vec3 toVec3() {
        return new Vec3(this.x, 0.0, this.z);
    }

    public BlockPos toBlockPos() {
        return new BlockPos(this.x, 0, this.z);
    }
}
