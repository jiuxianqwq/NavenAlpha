package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.shader.impl.KawaseBlur;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.platform.Window;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.List;

/**
 * @Author：jiuxian_baka
 * @Date：2026/1/1 18:49
 * @Filename：ArrayList
 */

@ModuleInfo(name = "ArrayList", cnName = "模块列表", description = "显示你打开的模块", category = Category.RENDER)
public class ArrayListModule extends Module {
    private final DragValue dragValue = ValueBuilder.create(this, "Position")
            .setDefaultX(800f)
            .setDefaultY(5f)
            .build()
            .getDragValue();
    public BooleanValue prettyModuleName = ValueBuilder.create(this, "Pretty Module Name")
            .setOnUpdate(value -> Module.update = true)
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue hideRenderModules = ValueBuilder.create(this, "Hide Render Modules")
            .setOnUpdate(value -> Module.update = true)
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue rainbow = ValueBuilder.create(this, "Rainbow")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public FloatValue rainbowSpeed = ValueBuilder.create(this, "Rainbow Speed")
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(17.5F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public FloatValue rainbowOffset = ValueBuilder.create(this, "Rainbow Offset")
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(10.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public FloatValue arrayListSize = ValueBuilder.create(this, "ArrayList Size")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(32F)
            .build()
            .getFloatValue();
    List<Module> renderModules;
    List<Vector4f> blurMatrices = new java.util.ArrayList<>();
    private final Paint listPaint = new Paint();
    private final Path blurPath = new Path();
    private final Paint blurPaint = new Paint();


    @EventTarget
    public void onSkia(EventRenderSkia event) {
        Font miSans = Fonts.getMiSans(arrayListSize.getCurrentValue());
        Font icon = Fonts.getNavenIcon(arrayListSize.getCurrentValue());
        ModuleManager moduleManager = Naven.getInstance().getModuleManager();
        if (Module.update || this.renderModules == null || this.renderModules.isEmpty()) {
            this.renderModules = new java.util.ArrayList<>(moduleManager.getModules());
            if (this.hideRenderModules.getCurrentValue()) {
                this.renderModules.removeIf(modulex -> modulex.getCategory() == Category.RENDER);
            }

            this.renderModules.sort((o1, o2) -> {
                float o1Width = Skia.getStringWidth(getModuleDisplayName(o1), miSans);
                float o2Width = Skia.getStringWidth(getModuleDisplayName(o2), miSans);
                return Float.compare(o2Width, o1Width);
            });

            update = false;
        }


        boolean right = dragValue.getX() >= (float) mc.getWindow().getGuiScaledWidth() / 2;
        float height = arrayListSize.getCurrentValue() * 1.5f;
        float iconWidth = height;
        float margen = arrayListSize.getCurrentValue() / 4.0f;

        float maxWidth = this.renderModules.isEmpty()
                ? 0.0F
                : Skia.getStringWidth(this.getModuleDisplayName(this.renderModules.get(0)), miSans) + margen * 3 + iconWidth;

        float startY = dragValue.getY();
            for (Module m : renderModules) {
                SmoothAnimationTimer animation = m.getAnimation();
                if (m.isEnabled()) {
                    animation.target = 100.0F;
                } else {
                    animation.target = 0.0F;
                }
                animation.update(true);

                if (animation.value == 0.0f) continue;
                if (m.getCategory() == Category.RENDER && hideRenderModules.getCurrentValue()) continue;


                String text = getModuleDisplayName(m);
                float stringWidth = Skia.getStringWidth(text, miSans);
                float width = stringWidth + margen * 2;
                float fullWidth = width + margen + iconWidth;
                float startX;
                float iconStartX;
                if (right) {
                    startX = dragValue.getX() + maxWidth - fullWidth + (fullWidth * ((100 - animation.value) / 100.0f));
                    iconStartX = startX + width + margen;
                } else {
                    startX = dragValue.getX() - (fullWidth * ((100 - animation.value) / 100.0f));
                    iconStartX = startX + width + margen;
                }

                Color color = Color.white;
                if (this.rainbow.getCurrentValue()) {
                    color = RenderUtils.getRainbow(
                            (int) (-startY * this.rainbowOffset.getCurrentValue()), 1.0F, 1.0F, (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F
                    );
                }

                listPaint.reset();
                listPaint.setAlpha((int) (255 * (animation.value / 100.0f)));
                Skia.getCanvas().saveLayer(Rect.makeXYWH(startX, startY, fullWidth, height), listPaint);
                if (animation.value >= 75) blurMatrices.add(new Vector4f(startX, startY, width, height));
                Skia.drawRoundedRect(startX, startY, width, height, margen, new Color(0, 0, 0, 100));
                Skia.drawText(text, startX + margen, startY + margen, color, miSans);

                if (animation.value >= 75) blurMatrices.add(new Vector4f(iconStartX, startY, iconWidth, height));
                Skia.drawRoundedRect(iconStartX, startY, iconWidth, height, margen, new Color(0, 0, 0, 100));
                Skia.drawText(m.getCategory().getIcon(), iconStartX + margen, startY + margen, Color.WHITE, icon);

                Skia.restore();
                startY += (height + margen) * (animation.value / 100.0f);
            }

        dragValue.setWidth(maxWidth);
        dragValue.setHeight(startY);

    }

    @EventTarget(1)
    public void onShader(EventRenderSkia event) {
        float margen = arrayListSize.getCurrentValue() / 4.0f;
        blurPath.reset();
        for (Vector4f blurMatrix : blurMatrices) {
            blurPath.addRRect(RRect.makeXYWH(blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), margen));
        }
        blurPaint.reset();
        blurPaint.setImageFilter(ImageFilter.makeBlur(2.5F, 2.5F, FilterTileMode.DECAL));

        Skia.save();

        Skia.clipPath(blurPath, ClipMode.DIFFERENCE, true);

        for (Vector4f blurMatrix : this.blurMatrices) {

            Color color = Color.white;
            if (this.rainbow.getCurrentValue()) {
                color = RenderUtils.getRainbow(
                        (int) (-blurMatrix.y * this.rainbowOffset.getCurrentValue()), 1.0F, 1.0F, (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F
                );
            }

            blurPaint.setColor(color.getRGB());

            Skia.getCanvas().drawRRect(
                    RRect.makeXYWH(blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), margen),
                    blurPaint
            );

        }

        Skia.restore();
//
        Skia.save();
        Skia.getCanvas().clipPath(blurPath, ClipMode.INTERSECT, true);
        Window window = Minecraft.getInstance().getWindow();
        Skia.drawImage(KawaseBlur.INGAME_BLUR.getTexture(), 0, 0, window.getWidth() / (float) window.getGuiScale(), window.getHeight() / (float) window.getGuiScale(), 1F,
                SurfaceOrigin.BOTTOM_LEFT);
        Skia.restore();
        blurMatrices.clear();
    }

    public String getModuleDisplayName(Module module) {
        String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
        return name + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
    }
}
