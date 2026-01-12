package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ModuleInfo(
        name = "NoSlow",
        cnName = "无减速",
        description = "NoSlowDown",
        category = Category.MOVEMENT
)
public class NoSlow extends Module {
    public ModeValue mode = ValueBuilder.create(this, "Mode").setDefaultModeIndex(0).setModes("None", "Heypixel 2/3", "Grim50%", "Grim 1/3", "Jump").build().getModeValue();
    public BooleanValue food = ValueBuilder.create(this, "Food").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue bow = ValueBuilder.create(this, "Bow").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue crossbow = ValueBuilder.create(this, "Crossbow").setDefaultBooleanValue(true).build().getBooleanValue();
    private int onGroundTick = 0;

    @EventTarget
    public void onSlow(EventSlowdown eventSlowdown) {
        if (mc.player == null || (checkFood() && mc.player.getUseItemRemainingTicks() > 30)) return;

        if (!food.getCurrentValue() && checkFood()) return;
        if (!bow.getCurrentValue() && checkItem(Items.BOW)) return;
        if (!crossbow.getCurrentValue() && checkItem(Items.CROSSBOW)) return;

        switch (mode.getCurrentMode()) {
            case "Jump" -> GrimJump(eventSlowdown);
            case "Grim50%" -> Grim50(eventSlowdown);
            case "None" -> none(eventSlowdown);
            case "Grim 1/3" -> grim1_3(eventSlowdown);
            case "Heypixel 2/3" -> heypixel2_3(eventSlowdown);
        }

    }

    private void GrimJump(EventSlowdown eventSlowdown) {
        if (onGroundTick == 1 && mc.player.getUseItemRemainingTicks() <= 30) {
            eventSlowdown.setSlowdown(false);
            if (!mc.player.isSprinting()) mc.player.setSprinting(true);
        }
    }

    private void Grim50(EventSlowdown eventSlowdown) {
        if (mc.player.getUseItemRemainingTicks() % 2 == 0 && mc.player.getUseItemRemainingTicks() <= 30) {
            eventSlowdown.setSlowdown(false);
            if (!mc.player.isSprinting()) mc.player.setSprinting(true);
        }
    }

    private void none(EventSlowdown eventSlowdown) {
        eventSlowdown.setSlowdown(false);
        if (!mc.player.isSprinting()) mc.player.setSprinting(true);
    }

    private void grim1_3(EventSlowdown eventSlowdown) {
        if (mc.player.getUseItemRemainingTicks() % 3 == 0 && (!checkFood() || mc.player.getUseItemRemainingTicks() <= 30)) {
            eventSlowdown.setSlowdown(false);
            if (!mc.player.isSprinting()) mc.player.setSprinting(true);
        }
    }

    private void heypixel2_3(EventSlowdown eventSlowdown) {
        if (mc.player.getUseItemRemainingTicks() % 3 != 0 && (!checkFood() || mc.player.getUseItemRemainingTicks() <= 30)) {
            eventSlowdown.setSlowdown(false);
            if (!mc.player.isSprinting()) mc.player.setSprinting(true);
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        this.setSuffix(mode.getCurrentMode());
        if (mc.player.onGround()) {
            onGroundTick++;
        } else {
            onGroundTick = 0;
        }
    }

    @Override
    public void onEnable() {
        onGroundTick = 0;
    }

    @Override
    public void onDisable() {
        onGroundTick = 0;
    }

    private boolean checkFood() {
        ItemStack mainHandItem = mc.player.getMainHandItem();
        ItemStack offhandItem = mc.player.getOffhandItem();
        return mainHandItem.is(Items.GOLDEN_APPLE)
                || offhandItem.is(Items.GOLDEN_APPLE)
                || mainHandItem.is(Items.ENCHANTED_GOLDEN_APPLE)
                || offhandItem.is(Items.ENCHANTED_GOLDEN_APPLE)
                || mainHandItem.is(Items.POTION)
                || offhandItem.is(Items.POTION);
    }

    private boolean checkItem(Item item) {
        ItemStack mainHandItem = mc.player.getMainHandItem();
        ItemStack offhandItem = mc.player.getOffhandItem();
        return mainHandItem.is(item) || offhandItem.is(item);
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.player.onGround() && mc.player.isUsingItem() && (event.getForward() != 0 || event.getStrafe() != 0) && mode.isCurrentMode("Jump"))
            event.setJump(true);
    }
}
