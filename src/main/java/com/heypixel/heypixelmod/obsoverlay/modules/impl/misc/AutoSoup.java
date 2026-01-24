package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

@ModuleInfo(
        name = "AutoSoup",
        cnName = "自动汤",
        description = "Automatically uses mushroom stew when health is low",
        category = Category.MISC
)
public class AutoSoup extends Module {
    public static Integer back;

    @Override
    public void onEnable() {
        back = null;
    }

    @Override
    public void onDisable() {
        back = null;
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.type() != EventType.PRE) return;
        Minecraft mc = Minecraft.getInstance();
        if (back == null) {
            if (mc.player.tickCount % 10 != 0) return;
            if (mc.player.getHealth() < mc.player.getMaxHealth() / 2) {
                Integer soup = InventoryUtils.findItemHotbar(Items.MUSHROOM_STEW);
                if (soup != null) {
                    back = mc.player.getInventory().selected;
                    mc.player.getInventory().selected = soup;
                    KeyMapping.click(mc.options.keyUse.getKey());
                }
            }
        } else {
            mc.player.getInventory().selected = back;
            back = null;
        }
    }
}
