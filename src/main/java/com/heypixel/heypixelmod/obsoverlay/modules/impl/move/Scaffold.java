package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RayCastUtil;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
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
import net.minecraft.world.phys.AABB;
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
    BooleanValue safeWalk = ValueBuilder.create(this, "SafeWalk").setDefaultBooleanValue(true)
            .setVisibility(() -> !telly.getCurrentValue())
            .build()
            .getBooleanValue();
    BooleanValue esp = ValueBuilder.create(this, "ESP").setDefaultBooleanValue(true).build().getBooleanValue();
    private int airTick;
    private int yLevel;
    private BlockPos blockPos;
    private Direction enumFacing;
    private int oldSlot = -1;

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
        if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) {
            return false;
        } else if (!InventoryUtils.isItemValid(stack)) {
            return false;
        } else {
            String string = stack.getDisplayName().getString();
            if (string.contains("Click") || string.contains("点击")) {
                return false;
            } else if (stack.getItem() instanceof ItemNameBlockItem) {
                return false;
            } else {
                Block block = ((BlockItem) stack.getItem()).getBlock();
                if (block instanceof FlowerBlock) {
                    return false;
                } else if (block instanceof BushBlock) {
                    return false;
                } else if (block instanceof FungusBlock) {
                    return false;
                } else if (block instanceof CropBlock) {
                    return false;
                } else {
                    return !(block instanceof SlabBlock) && !blacklistedBlocks.contains(block);
                }
            }
        }
    }

    @EventTarget
    public void onPreTick(EventRunTicks event) {
        if (mc.player == null || mc.level == null || event.type() != EventType.PRE) return;

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
        if (mc.player.onGround()) yLevel = (int) Math.floor(mc.player.getY()) - 1;
        getBlockInfo();
        if (telly.getCurrentValue()) {
            if (mc.player.onGround()) {
                airTick = 0;
                blockPos = null;
                enumFacing = null;
                Rotation rotation = new Rotation(mc.player.getYRot(), mc.player.getXRot());
                RotationManager.setRotations(rotation, rotateBackSpeed.getCurrentValue());
            } else {
                if (airTick >= tellyTick.getCurrentValue()) {
                    Rotation rotation = getRotation(blockPos, enumFacing);
                    RotationManager.setRotations(rotation, rotateSpeed.getCurrentValue());
                    place();
                }
                airTick++;
            }
            this.setSuffix("Telly");
        } else {
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

    public void place() {
        if (!onAir()) return;
        boolean hasRotated = RayCastUtil.overBlock(RotationManager.getRotation(), blockPos);
//      boolean hasRotated = true;
        if (hasRotated) {
            InteractionResult interactionResult = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(getVec3(blockPos, enumFacing), enumFacing, blockPos, false));
            if (interactionResult == InteractionResult.SUCCESS) mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.player.onGround() && !mc.options.keyJump.isDown() && MoveUtils.isMoving() && telly.getCurrentValue())
            event.setJump(true);
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

    public BlockPos getIntBlockPos(double x, double y, double z) {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE && safeWalk.getCurrentValue() && !telly.getCurrentValue()) {
            mc.options.keyShift.setDown(mc.player.onGround() && SafeWalk.isOnBlockEdge(0.3F));
        }
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (blockPos != null && esp.getCurrentValue()) {
            PoseStack stack = event.getPMatrixStack();
            stack.pushPose();
            Vec3 cameraPos = RenderUtils.getCameraPos();
            stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F);

            AABB box = new AABB(blockPos);
            RenderUtils.drawSolidBox(box, stack);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            stack.popPose();
        }
    }

    @Override
    public void onEnable() {
        if (mc.player != null) oldSlot = mc.player.getInventory().selected;
        airTick = 0;
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
