package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record EventDestroyBlock(BlockPos pos, Direction face) implements Event {
}
