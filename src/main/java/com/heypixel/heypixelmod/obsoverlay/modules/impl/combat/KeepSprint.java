package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttackSlowdown;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;

@ModuleInfo(
        name = "KeepSprint",
        cnName = "保持疾跑",
        description = "Maintain a sprinting state while attacking.",
        category = Category.COMBAT
)
public class KeepSprint extends Module {

    @EventTarget
    public void onAttackSlowdown(EventAttackSlowdown event) {
        event.setCancelled(true);
    }
}
