package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;

@ModuleInfo(
        name = "Notification",
        cnName = "通知",
        description = "",
        category = Category.RENDER
)
public class Notification extends Module {
    public DragValue dragValue = ValueBuilder.create(this, "Position")
            .setDefaultX(10f)
            .setDefaultY(50f)
            .build()
            .getDragValue();

    @EventTarget
    public void onRenderSkia(EventRenderSkia event) {
        Naven.getInstance().getNotificationManager().onRender(event, dragValue);
    }
}
