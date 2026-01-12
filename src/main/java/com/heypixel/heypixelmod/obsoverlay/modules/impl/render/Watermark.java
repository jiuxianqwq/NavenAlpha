package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;
import io.github.humbleui.skija.Font;

import java.awt.*;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/29 01:43
 * @Filename：Watermark
 */
@ModuleInfo(name = "Watermark", cnName = "客户端水印", description = "", category = Category.RENDER)
public class Watermark extends Module {
    private final DragValue dragValue = ValueBuilder.create(this, "Position")
            .setDefaultX(10f)
            .setDefaultY(10f)
            .build()
            .getDragValue();

    @EventTarget
    public void onRenderSkia(EventRenderSkia event) {
        Font navenFont = Fonts.getUrbanistVariable(20.0f);
        Font alphaFont = Fonts.getUrbanistVariable(13.5f);
        float navenWidth = navenFont.measureTextWidth("Naven");
        float alphaWidth = alphaFont.measureTextWidth("Alpha");
        float x = dragValue.getX();
        float y = dragValue.getY();
        Skia.drawText("Naven", x, y, new Color(255, 255, 255, 125), navenFont);
        Skia.drawText("Alpha", x + navenWidth + 0.5f, y + 3.25f, new Color(255, 255, 255, 125), alphaFont);
        dragValue.setHeight(20.0f);
        dragValue.setWidth(navenWidth + 0.5f + alphaWidth);
    }
}
