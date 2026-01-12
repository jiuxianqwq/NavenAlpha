package com.heypixel.heypixelmod.mixin.O.accessors;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface AccessorEntity {
    @Accessor("yRotO")
    float getPreviousYRot();

    @Accessor("xRotO")
    float getPreviousXRot();
}
