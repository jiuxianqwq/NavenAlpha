package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;

import java.awt.*;

@ModuleInfo(
        name = "Test",
        cnName = "测试",
        description = "",
        category = Category.RENDER
)
public class Test extends Module {
    public DragValue dragValue = ValueBuilder.create(this, "Position")
            .setDefaultX(10f)
            .setDefaultY(10f)
            .build()
            .getDragValue();

    @EventTarget
    public void onRenderSkia(EventRenderSkia event) {
//        PoseStack poseStack = event.stack();
//        poseStack.pushPose();
//        RenderUtils.drawRoundedRect(poseStack, dragValue.getX(), dragValue.getY(), 50f, 50f, 5f, new Color(0, 0, 0, 90).getRGB());
//        poseStack.popPose();
        Skia.drawShadow(dragValue.getX(), dragValue.getY(), 50, 50, 5);
        Skia.drawRoundedBlur(dragValue.getX(), dragValue.getY(), 50, 50, 5);
        Skia.drawRoundedRect(dragValue.getX(), dragValue.getY(), 50, 50, 5, new Color(0, 0, 0, 90));
        dragValue.setWidth(50f);
        dragValue.setHeight(50f);
    }
}
