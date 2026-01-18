package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.BlockUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ChunkUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/26 10:55
 * @Filename：GhostHand
 */

@ModuleInfo(
        name = "GhostHand",
        cnName = "鬼头",
        description = "Allows interacting with blocks through walls",
        category = Category.MISC
)
public class GhostHand extends Module {

    private static final Set<Block> BLOCKS = new HashSet<>();

    static {
        BLOCKS.add(Blocks.CHEST);
        BLOCKS.add(Blocks.ENDER_CHEST);
        BLOCKS.add(Blocks.TRAPPED_CHEST);
        BLOCKS.add(Blocks.SHULKER_BOX);
    }

    private final Minecraft mc = Minecraft.getInstance();

    @EventTarget
    public void onClick(EventClick event) {
        if (mc.options.keyUse.isDown()) {
            if (ghostInteractWithChest()) {
                event.setCancelled(true);
            }
        }
    }

    public boolean ghostInteractWithChest() {
        if (mc.player == null || mc.level == null) {
            return false;
        }
        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookVec = mc.player.getViewVector(1.0f);
        Vec3 reachEnd = eyePos.add(lookVec.scale(4.5));
        ChestBlockEntity targetChest = null;
        BlockHitResult fakeHit = null;
        double closestDist = Double.MAX_VALUE;
        ArrayList<BlockEntity> blockEntities = ChunkUtils.getLoadedBlockEntities().collect(Collectors.toCollection(ArrayList::new));
        for (BlockEntity be : blockEntities) {
            double dist;
            Optional<Vec3> hit;
            ChestBlockEntity chest;
            AABB box;
            if (!(be instanceof ChestBlockEntity) || (box = this.getChestBox(chest = (ChestBlockEntity) be)) == null || !(hit = box.clip(eyePos, reachEnd)).isPresent() || !((dist = hit.get().distanceTo(eyePos)) < closestDist))
                continue;
            closestDist = dist;
            targetChest = chest;
            fakeHit = new BlockHitResult(hit.get(), Direction.UP, chest.getBlockPos(), false);
        }
        if (targetChest != null && fakeHit != null) {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, fakeHit);
            mc.player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private AABB getChestBox(ChestBlockEntity chestBE) {
        BlockPos pos2;
        BlockState state = chestBE.getBlockState();
        if (!state.hasProperty(ChestBlock.TYPE)) {
            return null;
        }
        ChestType chestType = state.getValue(ChestBlock.TYPE);
        if (chestType == ChestType.LEFT) {
            return null;
        }
        BlockPos pos = chestBE.getBlockPos();
        AABB box = BlockUtils.getBoundingBox(pos);
        if (chestType != ChestType.SINGLE && BlockUtils.canBeClicked(pos2 = pos.relative(ChestBlock.getConnectedDirection(state)))) {
            AABB box2 = BlockUtils.getBoundingBox(pos2);
            box = box.minmax(box2);
        }
        return box;
    }

    private void triggerBlockInteraction(BlockPos pos, Direction direction) {
        if (mc.gameMode != null) {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), direction, pos, false)
            );
        }
        if (mc.player != null) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}