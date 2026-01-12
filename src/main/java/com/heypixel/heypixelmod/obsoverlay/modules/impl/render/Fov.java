package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateFoV;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;

@ModuleInfo(
        name = "Fov",
        cnName = "视场角",
        description = "Change fov.",
        category = Category.RENDER
)
public class Fov extends Module {
    public FloatValue fov = ValueBuilder.create(this, "Fov")
            .setDefaultFloatValue(120.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(150.0F)
            .build()
            .getFloatValue();

    @EventTarget
    public void onFovUpdate(EventUpdateFoV event) {
        event.setFov(fov.getCurrentValue() / 100.0F);
    }
}
