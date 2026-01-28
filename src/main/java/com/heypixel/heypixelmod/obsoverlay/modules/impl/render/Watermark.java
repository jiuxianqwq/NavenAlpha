package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.ColorUtil;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.Rect;

import java.awt.*;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/29 01:43
 * @Filename：Watermark
 */
@ModuleInfo(name = "Watermark", cnName = "客户端水印", description = "", category = Category.RENDER)
public class Watermark extends Module {
    public BooleanValue rainbow = ValueBuilder.create(this, "Sync Color")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public FloatValue rainbowSpeed = ValueBuilder.create(this, "Speed")
            .setMinFloatValue(2.0F)
            .setMaxFloatValue(30.0F)
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> rainbow.getCurrentValue())
            .build()
            .getFloatValue();

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

        if (rainbow.getCurrentValue()) {
            Color color1 = ColorUtil.interpolateColorsBackAndForth((int)this.rainbowSpeed.getCurrentValue(), (int)-x, HUD.getColor1(), HUD.getColor2(), false);
            Color color2 = ColorUtil.interpolateColorsBackAndForth((int)this.rainbowSpeed.getCurrentValue(), (int)-(x + navenWidth + 0.5f + alphaWidth), HUD.getColor1(), HUD.getColor2(), false);
            float totalWidth = navenWidth + 0.5f + alphaWidth;

            try (Shader gradient = Shader.makeLinearGradient(
                    new Point(x, y),
                    new Point(x + totalWidth, y),

                    new int[]{

                 io.github.humbleui.skija.Color.makeARGB(color1.getAlpha(), color1.getRed(), color1.getGreen(), color1.getBlue()),
                            io.github.humbleui.skija.Color.makeARGB(color2.getAlpha(), color2.getRed(), color2.getGreen(), color2.getBlue())
                    },
                    new float[]{0, 1}
            )) {
                drawGradientText("Naven", x, y, navenFont, gradient);
                drawGradientText("Alpha", x + navenWidth + 0.5f, y + 3.25f, alphaFont, gradient);
            }
        } else {
            Skia.drawText("Naven", x, y, new Color(255, 255, 255, 125), navenFont);
            Skia.drawText("Alpha", x + navenWidth + 0.5f, y + 3.25f, new Color(255, 255, 255, 125), alphaFont);
        }

        dragValue.setHeight(20.0f);
        dragValue.setWidth(navenWidth + 0.5f + alphaWidth);
    }

    private void drawGradientText(String text, float x, float y, Font font, Shader shader) {
        try (Paint paint = new Paint().setShader(shader);
             Paint blurPaint = paint.makeClone().setImageFilter(ImageFilter.makeBlur(2.5F, 2.5F, FilterTileMode.DECAL))) {
            Rect bounds = font.measureText(text);
            float bx = x - bounds.getLeft();
            float by = y - bounds.getTop();

            Skia.getCanvas().drawString(text, bx, by, font, blurPaint);
            Skia.getCanvas().drawString(text, bx, by, font, paint);
        }
    }
}
