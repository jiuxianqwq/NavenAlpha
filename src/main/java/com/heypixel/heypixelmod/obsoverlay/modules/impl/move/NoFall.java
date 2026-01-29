package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RayCastUtil;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.heypixel.heypixelmod.mixin.O.accessors.MultiPlayerGameModeAccessor;

@ModuleInfo(
        name = "NoFall",
        cnName = "免摔伤害",
        description = "Prevents fall damage",
        category = Category.MOVEMENT
)
public class NoFall extends Module {
    private final ModeValue fallDistanceMode = ValueBuilder.create(this, "FallDistance Mode").setModes("Calc", "Custom").build().getModeValue();
    private final FloatValue fallDistanceValue = ValueBuilder.create(this, "FallDistance")
            .setDefaultFloatValue(10.0F)
            .setMinFloatValue(2.0F)
            .setMaxFloatValue(100.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> "Custom".equals(fallDistanceMode.getCurrentMode()))
            .build()
            .getFloatValue();
    private final FloatValue rotationSpeed = ValueBuilder.create(this, "Rotation Speed")
            .setDefaultFloatValue(180.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(180.0F)
            .build()
            .getFloatValue();
    private final BooleanValue retrieve = ValueBuilder.create(this, "Retrieve Water").setDefaultBooleanValue(true).build().getBooleanValue();
    private static final long PLACE_DELAY_MS = 50L;
    private boolean retrieveFlag = false;
    private boolean retrievePending = false;
    private boolean rotation = false;
    private boolean placeWater = false;
    private int timeout = 0;
    private int originalSlot = -1;
    private long lastPlaceAttempt = 0L;
    private BlockPos above = null;
    private Rotation targetRotation = null;

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (mc.player == null || mc.level == null) {
                return;
            }

            if (retrieveFlag && mc.player.onGround()) {
                retrievePending = true;
                if (above != null) {
                    targetRotation = RotationUtils.calculate(above, Direction.UP);
                }
            }

            if (rotation || placeWater || retrievePending) {
                Rotation current = targetRotation == null ? new Rotation(mc.player.getYRot(), 90.0F) : targetRotation;
                RotationManager.setRotations(current, rotationSpeed.getCurrentValue());
            }

            if (rotation) {
                if (shouldPlaceNow()) {
                    placeWater = true;
                }
                if (timeout > 0) {
                    timeout--;
                } else {
                    resetState(true);
                }
                return;
            }

            if (isFalling()) {
                tryPrepareWater();
            }
        }
    }

    @Override
    public void onEnable() {
        resetState(true);
        lastPlaceAttempt = 0L;
    }

    @Override
    public void onDisable() {
        resetState(true);
    }

    @EventTarget
    public void onClick(EventClick e) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (placeWater) {
            long now = System.currentTimeMillis();
            if (now - lastPlaceAttempt < PLACE_DELAY_MS) {
                return;
            }
            lastPlaceAttempt = now;
            placeWater = false;

            HitResult hitResult = getRaycastResult();
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                Direction dir = ((net.minecraft.world.phys.BlockHitResult) hitResult).getDirection();
                boolean isLowHeight = mc.player.fallDistance >= 2.0F && mc.player.fallDistance < 4.0F;
                BlockPos hitPos = ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos();
                if (dir == Direction.UP || (isLowHeight && dir != Direction.DOWN)) {
                    above = hitPos.above();
                    useItem(InteractionHand.MAIN_HAND);
                    retrieveFlag = retrieve.getCurrentValue();
                    rotation = false;
                    return;
                }
            }
            rotation = false;
            return;
        }

        if (retrievePending && above != null) {
            HitResult hitResult = getRaycastResult();
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos hitAbove = ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos().above();
                if (hitAbove.equals(above)) {
                    useItem(InteractionHand.MAIN_HAND);
                    retrievePending = false;
                    retrieveFlag = false;
                    above = null;
                    rotation = false;
                    if (originalSlot != -1) {
                        mc.player.getInventory().selected = originalSlot;
                    }
                    originalSlot = -1;
                }
            }
        }
    }

    private boolean isFalling() {
        if (mc == null || mc.player == null || mc.level == null) {
            return false;
        }

        if (mc.player.isFallFlying()) {
            return false;
        }

        String mode = fallDistanceMode.getCurrentMode();
        if ("Custom".equals(mode)) {
            return mc.player.fallDistance > fallDistanceValue.getCurrentValue();
        } else {
            return (((mc.player.fallDistance - 3) / 2F) + 3.5F) > mc.player.getHealth() / 3f;
        }
    }

    private void tryPrepareWater() {
        Integer waterSlot = InventoryUtils.findItemHotbar(Items.WATER_BUCKET);
        if (waterSlot == null) {
            return;
        }

        float fallDistance = mc.player.fallDistance;
        boolean isLowHeight = fallDistance >= 2.0F && fallDistance < 4.0F;
        double groundCheckMultiplier = isLowHeight ? 1.5 : 2.0;
        double motionY = mc.player.getDeltaMovement().y;
        if (!isOnGround(motionY * groundCheckMultiplier) && !(isLowHeight && isOnGround(motionY * 1.2))) {
            return;
        }

        if (originalSlot == -1) {
            originalSlot = mc.player.getInventory().selected;
        }
        mc.player.getInventory().selected = waterSlot;
        BlockPos below = mc.player.blockPosition().below();
        targetRotation = RotationUtils.calculate(below, Direction.UP);
        rotation = true;
        timeout = isLowHeight ? 8 : 5;
    }

    private boolean shouldPlaceNow() {
        float fallDistance = mc.player.fallDistance;
        boolean isLowHeight = fallDistance >= 2.0F && fallDistance < 4.0F;
        double motionY = mc.player.getDeltaMovement().y;
        return isOnGround(motionY) || (isLowHeight && isOnGround(motionY * 0.8));
    }

    private static boolean isOnGround(double height) {
        Iterable<VoxelShape> collisions = mc.level.getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, height, 0.0));
        return collisions.iterator().hasNext();
    }

    private void resetState(boolean switchBack) {
        rotation = false;
        placeWater = false;
        timeout = 0;
        retrieveFlag = false;
        retrievePending = false;
        above = null;
        targetRotation = null;
        if (switchBack && originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selected = originalSlot;
        }
        originalSlot = -1;
    }

    private void useItem(InteractionHand hand) {
        MultiPlayerGameModeAccessor gm = (MultiPlayerGameModeAccessor) mc.gameMode;
        if (gm != null) gm.invokeEnsureHasSentCarriedItem();
        PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(hand, id));
    }

    private HitResult getRaycastResult() {
        if (targetRotation != null) {
            HitResult result = RayCastUtil.rayCast(targetRotation, 4.5D);
            if (result != null) {
                return result;
            }
        }
        return mc.hitResult;
    }
}
