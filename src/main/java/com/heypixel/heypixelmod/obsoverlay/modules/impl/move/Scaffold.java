package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RayCastUtil;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

@ModuleInfo(
        name = "Scaffold",
        cnName = "自动搭路",
        description = "Automatically places blocks under you",
        category = Category.MOVEMENT
)
public class Scaffold extends Module {

    public static final List<Block> blacklistedBlocks = Arrays.asList(
            Blocks.AIR,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.ENCHANTING_TABLE,
            Blocks.GLASS_PANE,
            Blocks.GLASS_PANE,
            Blocks.IRON_BARS,
            Blocks.SNOW,
            Blocks.COAL_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.TORCH,
            Blocks.ANVIL,
            Blocks.TRAPPED_CHEST,
            Blocks.NOTE_BLOCK,
            Blocks.JUKEBOX,
            Blocks.TNT,
            Blocks.GOLD_ORE,
            Blocks.IRON_ORE,
            Blocks.LAPIS_ORE,
            Blocks.STONE_PRESSURE_PLATE,
            Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Blocks.STONE_BUTTON,
            Blocks.LEVER,
            Blocks.TALL_GRASS,
            Blocks.TRIPWIRE,
            Blocks.TRIPWIRE_HOOK,
            Blocks.RAIL,
            Blocks.CORNFLOWER,
            Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM,
            Blocks.VINE,
            Blocks.SUNFLOWER,
            Blocks.LADDER,
            Blocks.FURNACE,
            Blocks.SAND,
            Blocks.CACTUS,
            Blocks.DISPENSER,
            Blocks.DROPPER,
            Blocks.CRAFTING_TABLE,
            Blocks.COBWEB,
            Blocks.PUMPKIN,
            Blocks.COBBLESTONE_WALL,
            Blocks.OAK_FENCE,
            Blocks.REDSTONE_TORCH,
            Blocks.FLOWER_POT
    );

    BooleanValue telly = ValueBuilder.create(this, "Telly").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue snap = ValueBuilder.create(this, "Snap").setDefaultBooleanValue(false)
            .setVisibility(() -> !telly.getCurrentValue())
            .build()
            .getBooleanValue();
    FloatValue rotateSpeed = ValueBuilder.create(this, "Rotation Speed")
            .setDefaultFloatValue(180.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(180.0F)
            .build()
            .getFloatValue();
    FloatValue rotateBackSpeed = ValueBuilder.create(this, "Rotation Back Speed")
            .setDefaultFloatValue(180.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(180.0F)
            .setVisibility(telly::getCurrentValue)
            .build()
            .getFloatValue();
    FloatValue tellyTick = ValueBuilder.create(this, "Telly Ticks")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(6.0F)
            .setVisibility(telly::getCurrentValue)
            .build()
            .getFloatValue();
    BooleanValue extra = ValueBuilder.create(this, "EXTRA")
            .setDefaultBooleanValue(true)
            .setVisibility(telly::getCurrentValue)
            .build()
            .getBooleanValue();
    BooleanValue safeWalk = ValueBuilder.create(this, "SafeWalk").setDefaultBooleanValue(true)
            .setVisibility(() -> !telly.getCurrentValue())
            .build()
            .getBooleanValue();
    private int airTick;
    private int yLevel;
    private BlockPos blockPos;
    private Direction enumFacing;
    private int oldSlot = -1;
    private int tellyDist = 0;
    private boolean tellyExtraPending = false;
    private int tellyStartY = 0;
    private int tellyJumps = 0;
    private BlockPos lastPlacedPos;
    private float lastYaw = 0;
    private static final double TELLY_JUMP_MOTION_Y = -0.15233518685055708;
    private static final float MAX_TURN_ANGLE = 20.0f;
    private static final int STACK_INTERVAL = 4;

    public static Vec3 getVec3(BlockPos pos, Direction face) {
        double x = (double) pos.getX() + 0.5;
        double y = (double) pos.getY() + 0.5;
        double z = (double) pos.getZ() + 0.5;
        if (face != Direction.UP && face != Direction.DOWN) {
            y += 0.08;
        } else {
            x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
            z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }

        if (face == Direction.WEST || face == Direction.EAST) {
            z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }

        if (face == Direction.SOUTH || face == Direction.NORTH) {
            x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }

        return new Vec3(x, y, z);
    }

    public static boolean isValidStack(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) return false;
        if (!InventoryUtils.isItemValid(stack)) return false;

        String string = stack.getDisplayName().getString();
        if (string.contains("Click") || string.contains("点击")) return false;
        if (stack.getItem() instanceof ItemNameBlockItem) return false;

        Block block = ((BlockItem) stack.getItem()).getBlock();
        if (block instanceof FlowerBlock || block instanceof BushBlock || block instanceof FungusBlock || block instanceof CropBlock) return false;
        return !(block instanceof SlabBlock) && !blacklistedBlocks.contains(block);
    }

    private void updateSlot() {
        int slotID = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isValidStack(stack)) {
                slotID = i;
                break;
            }
        }
        if (slotID != -1 && mc.player.getInventory().selected != slotID) {
            mc.player.getInventory().selected = slotID;
        }
    }

    @EventTarget
    public void onPreTick(EventRunTicks event) {
        if (mc.player == null || mc.level == null || event.type() != EventType.PRE) return;

        updateSlot();

        if (telly.getCurrentValue()) {
            if (mc.player.onGround()) {
                yLevel = (int) Math.floor(mc.player.getY()) - 1;
                tellyStartY = (int) Math.floor(mc.player.getY());
                // tellyDist = 0; // Removed to persist distance across jumps
                tellyJumps = 0;
                tellyExtraPending = false;
                airTick = 0;
                blockPos = null;
                enumFacing = null;
                lastPlacedPos = null;
                Rotation rotation = new Rotation(mc.player.getYRot(), mc.player.getXRot());
                RotationManager.setRotations(rotation, rotateBackSpeed.getCurrentValue());
            } else {
                // Determine if we are moving diagonally
                boolean isDiagonal = (Math.abs(mc.player.input.forwardImpulse) > 0 && Math.abs(mc.player.input.leftImpulse) > 0);

                // Check for direction change (Turning)
                // If we turn significantly, abort extra placement and reset counter to ensure we place structural blocks
                if (lastPlacedPos != null && Math.abs(Mth.wrapDegrees(mc.player.getYRot() - lastYaw)) > MAX_TURN_ANGLE) {
                    tellyExtraPending = false;
                    tellyDist = 0;
                }

                // Priority: Handle Extra Placement (Stacking)
                // Disable stacking when diagonal to prevent VL (RotationPlace) and Falling
                if (tellyExtraPending && lastPlacedPos != null && !isDiagonal) {
                    BlockPos targetPos = lastPlacedPos.above();
                    // Check if we can place on top of the last block
                    // We need to find a valid face to place on. Since it's stacking, we want to place on the UP face of lastPlacedPos.
                    // But we need to use getBlockInfo-like logic to ensure rotation and range.
                    
                    // Manually setup blockPos/enumFacing for the extra placement
                    if (mc.level.getBlockState(lastPlacedPos).getBlock() instanceof AirBlock) {
                        // If the block below is still air (client lag), we can't legitimately place on it.
                        // We skip this extra placement to avoid VL/Fall.
                        tellyExtraPending = false;
                    } else {
                        blockPos = lastPlacedPos;
                        enumFacing = Direction.UP;
                        
                        // Verify range and rotation
                        Rotation rotation = getRotation(blockPos, enumFacing);
                        RotationManager.setRotations(rotation, rotateSpeed.getCurrentValue());
                        
                        if (airTick >= tellyTick.getCurrentValue()) {
                             place();
                             tellyExtraPending = false;
                             // We consumed this tick for extra placement. Return to avoid normal placement.
                             this.setSuffix("Telly Extra");
                             airTick++;
                             return; 
                        }
                    }
                }

                // Normal Placement Logic
                int targetY = tellyStartY - 1; 
                yLevel = targetY;

                getBlockInfo();

                if (airTick >= tellyTick.getCurrentValue()) {
                    Rotation rotation = getRotation(blockPos, enumFacing);
                    RotationManager.setRotations(rotation, rotateSpeed.getCurrentValue());
                    place();

                    if (blockPos != null) {
                        lastPlacedPos = blockPos.relative(enumFacing); // Record where we just placed
                        lastYaw = mc.player.getYRot(); // Update yaw to current direction
                        
                        // Check trigger conditions for NEXT tick
                        tellyDist++;
                        boolean shouldStack = false;
                        
                        // Condition 1: Every 4 blocks
                        if (tellyDist >= STACK_INTERVAL) {
                            shouldStack = true;
                        }

                        // Condition 2: Fixed Y logic
                        if (getTellyFixedY() == tellyStartY) {
                            shouldStack = true;
                        }
                        
                        // Only stack if we are at the base level AND not holding jump (towering) AND not diagonal
                        if (extra.getCurrentValue() && shouldStack && lastPlacedPos.getY() == tellyStartY - 1 && !mc.options.keyJump.isDown() && !isDiagonal) {
                            tellyExtraPending = true;
                            tellyDist = 0;
                        }
                    }
                }
                airTick++;
            }
            this.setSuffix("Telly");
        } else {
            if (mc.player.onGround()) yLevel = (int) Math.floor(mc.player.getY()) - 1;
            getBlockInfo();
            
            if (blockPos == null) {
                RotationManager.setRotations(new Rotation(Mth.wrapDegrees(mc.player.getYRot() - 180), 89.64F), rotateSpeed.getCurrentValue());
            }
            if (onAir() || !snap.getCurrentValue()) {
                Rotation rotation = getRotation(blockPos, enumFacing);
                RotationManager.setRotations(rotation, rotateSpeed.getCurrentValue());
            }
            place();

            this.setSuffix(snap.getCurrentValue() ? "Snap" : "Normal");
        }
    }

    private int getTellyFixedY() {
        double nextY = Math.floor(mc.player.getY() + mc.player.getDeltaMovement().y);
        if (nextY <= tellyStartY && mc.player.getY() > (double) (tellyStartY + 1)) {
            return tellyStartY;
        }
        if (mc.player.getDeltaMovement().y == TELLY_JUMP_MOTION_Y && tellyJumps >= 2) {
            tellyJumps = 0;
            return tellyStartY;
        }
        return tellyStartY - 1;
    }

    public void place() {
        if (!onAir()) return;
        boolean hasRotated = RayCastUtil.overBlock(RotationManager.getRotation(), blockPos);
        if (hasRotated) {
            InteractionResult interactionResult = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(getVec3(blockPos, enumFacing), enumFacing, blockPos, false));
            if (interactionResult == InteractionResult.SUCCESS) mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.player.onGround() && !mc.options.keyJump.isDown() && MoveUtils.isMoving() && telly.getCurrentValue()) {
            event.setJump(true);
            tellyJumps++;
        }
    }

    public int getYLevel() {
        if (!mc.options.keyJump.isDown() && MoveUtils.isMoving() && mc.player.fallDistance <= 0.25 && telly.getCurrentValue()) {
            return yLevel;
        } else {
            return (int) Math.floor(mc.player.getY()) - 1;
        }
    }

    public void getBlockInfo() {
        Vec3 baseVec = mc.player.getEyePosition();
        BlockPos base = BlockPos.containing(baseVec.x, getYLevel(), baseVec.z);
        int baseX = base.getX();
        int baseZ = base.getZ();
        if (isSolidAndNonInteractive(mc.level.getBlockState(base), mc.level, base)) return;
        if (checkBlock(baseVec, base)) {
            return;
        }
        for (int d = 1; d <= 6; d++) {
            if (checkBlock(baseVec, new BlockPos(
                    baseX,
                    getYLevel() - d,
                    baseZ
            ))) {
                return;
            }
            for (int x = 0; x <= d; x++) {
                for (int z = 0; z <= d - x; z++) {
                    int y = d - x - z;
                    for (int rev1 = 0; rev1 <= 1; rev1++) {
                        for (int rev2 = 0; rev2 <= 1; rev2++) {
                            if (checkBlock(baseVec, new BlockPos(baseX + (rev1 == 0 ? x : -x), getYLevel() - y, baseZ + (rev2 == 0 ? z : -z))))
                                return;
                        }
                    }
                }
            }
        }
    }

    public boolean isSolidAndNonInteractive(BlockState state, Level level, BlockPos pos) {
        boolean hasCollision = !state.getCollisionShape(level, pos).isEmpty();

        boolean hasNoMenu = state.getMenuProvider(level, pos) == null;

        return hasCollision && hasNoMenu;
    }

    private boolean checkBlock(Vec3 baseVec, BlockPos pos) {
        if (!(mc.level.getBlockState(pos).getBlock() instanceof AirBlock) && !(mc.level.getBlockState(pos).getBlock() instanceof WaterlilyBlock)) {
            return false;
        }

        if (pos.getY() > getYLevel()) return false;

        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        for (Direction dir : Direction.values()) {
            Vec3 hit = center.add(new Vec3(dir.getNormal().getX(), dir.getNormal().getY(), dir.getNormal().getZ()).scale(0.5));
            Vec3i baseBlock = pos.offset(dir.getNormal());
            BlockPos baseBlockPos = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());

            if (!isSolidAndNonInteractive(mc.level.getBlockState(baseBlockPos), mc.level, baseBlockPos)) continue;

            Vec3 relevant = hit.subtract(baseVec);
            if (relevant.lengthSqr() <= 4.5 * 4.5 && relevant.dot(new Vec3(dir.getNormal().getX(), dir.getNormal().getY(), dir.getNormal().getZ())) >= 0) {
                if (dir.getOpposite() == Direction.UP && MoveUtils.isMoving() && !mc.options.keyJump.isDown())
                    continue;
                blockPos = new BlockPos(baseBlock);
                enumFacing = dir.getOpposite();
                return true;
            }
        }
        return false;
    }

    @EventTarget
    public void onRender(com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender e) {
        if (blockPos != null) {
            com.mojang.blaze3d.vertex.PoseStack stack = e.getPMatrixStack();
            stack.pushPose();
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionShader);
            com.mojang.blaze3d.vertex.Tesselator tessellator = com.mojang.blaze3d.systems.RenderSystem.renderThreadTesselator();
            com.mojang.blaze3d.vertex.BufferBuilder bufferBuilder = tessellator.getBuilder();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F);

            com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils.装女人(bufferBuilder, stack.last().pose(), com.heypixel.heypixelmod.obsoverlay.utils.BlockUtils.getBoundingBox(blockPos));

            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            stack.popPose();
        }
    }

    public BlockPos getIntBlockPos(double x, double y, double z) {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE && safeWalk.getCurrentValue() && !telly.getCurrentValue()) {
            mc.options.keyShift.setDown(mc.player.onGround() && SafeWalk.isOnBlockEdge(0.3F));
        }
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            oldSlot = mc.player.getInventory().selected;
            lastYaw = mc.player.getYRot();
        }
        airTick = 0;
        tellyDist = 0;
        blockPos = null;
        enumFacing = null;
    }

    @Override
    public void onDisable() {
        boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
        mc.options.keyShift.setDown(isHoldingShift);
        if (mc.player != null && oldSlot != -1) {
            mc.player.getInventory().selected = oldSlot;
        }
    }

    public Rotation getRotation(BlockPos pos, Direction direction) {
        Rotation rotations = onAir() ? RotationUtils.calculate(pos, direction) : RotationUtils.calculate(pos.getCenter());
        Rotation reverseYaw = new Rotation(Mth.wrapDegrees(mc.player.getYRot() - 180), rotations.getPitch());
        boolean hasRotated = RayCastUtil.overBlock(reverseYaw, pos);
        if (hasRotated/* || !onAir()*/) return reverseYaw;
        else return rotations;
    }

    private boolean onAir() {
        Vec3 baseVec = mc.player.getEyePosition();
        BlockPos base = BlockPos.containing(baseVec.x, getYLevel(), baseVec.z);
        return mc.level.getBlockState(base).getBlock() instanceof AirBlock || mc.level.getBlockState(base).getBlock() instanceof WaterlilyBlock;
    }
}
