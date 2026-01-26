package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.ColorUtil;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import io.github.humbleui.skija.Font;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public BooleanValue rainbow = ValueBuilder.create(this, "Sync Color")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public FloatValue rainbowSpeed = ValueBuilder.create(this, "Speed")
            .setMinFloatValue(2.0F)
            .setMaxFloatValue(30.0F)
            .setDefaultFloatValue(25.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> rainbow.getCurrentValue())
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
    private boolean lastTopHalf = true;
    private final Map<Module, SmoothAnimationTimer> xTimers = new HashMap<>();
    private final Map<Module, SmoothAnimationTimer> yTimers = new HashMap<>();
    private final SmoothAnimationTimer maxWidthTimer = new SmoothAnimationTimer(0.0F, 0.0F, 0.2F);

    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 100);

    @EventTarget
    public void onSkia(EventRenderSkia event) {
        Font miSans = Fonts.getMiSans(arrayListSize.getCurrentValue());
        Font icon = Fonts.getNavenIcon(arrayListSize.getCurrentValue());
        ModuleManager moduleManager = Naven.getInstance().getModuleManager();
        boolean topHalf = dragValue.getY() < mc.getWindow().getGuiScaledHeight() / 2.0f;
        if (Module.update || this.renderModules == null || this.renderModules.isEmpty() || topHalf != this.lastTopHalf) {
            this.lastTopHalf = topHalf;
            this.renderModules = new java.util.ArrayList<>(moduleManager.getModules());
            if (this.hideRenderModules.getCurrentValue()) {
                this.renderModules.removeIf(modulex -> modulex.getCategory() == Category.RENDER);
            }

            this.renderModules.sort((o1, o2) -> {
                float o1Width = Skia.getStringWidth(getModuleDisplayName(o1), miSans);
                float o2Width = Skia.getStringWidth(getModuleDisplayName(o2), miSans);
                return topHalf ? Float.compare(o2Width, o1Width) : Float.compare(o1Width, o2Width);
            });

            update = false;
        }


        boolean right = dragValue.getX() >= (float) mc.getWindow().getGuiScaledWidth() / 2;
        float height = arrayListSize.getCurrentValue() * 1.5f;
        float iconWidth = height;
        float margen = arrayListSize.getCurrentValue() / 4.0f;

        float targetMaxWidth = 0.0F;
        float realMaxWidth = 0.0F;
        for (Module m : renderModules) {
            if (m.getCategory() == Category.RENDER && hideRenderModules.getCurrentValue()) {
                continue;
            }
            if (!m.isEnabled()) {
                continue;
            }
            String text = getModuleDisplayName(m);
            String stableText = this.prettyModuleName.getCurrentValue() ? m.getPrettyName() : m.getName();
            
            float stringWidth = Skia.getStringWidth(text, miSans);
            float stableStringWidth = Skia.getStringWidth(stableText, miSans);
            
            float width = stringWidth + margen * 2;
            float stableWidth = stableStringWidth + margen * 2;
            
            float fullWidth = width + margen + iconWidth;
            float fullStableWidth = stableWidth + margen + iconWidth;
            
            if (fullStableWidth > targetMaxWidth) {
                targetMaxWidth = fullStableWidth;
            }
            if (fullWidth > realMaxWidth) {
                realMaxWidth = fullWidth;
            }
        }
        maxWidthTimer.target = targetMaxWidth;
        maxWidthTimer.update(true);
        float maxWidth = maxWidthTimer.value;

        float startY = dragValue.getY();
        for (Module m : renderModules) {
            if (m.getCategory() == Category.RENDER && hideRenderModules.getCurrentValue()) {
                continue;
            }

            String text = getModuleDisplayName(m);
            float stringWidth = Skia.getStringWidth(text, miSans);
            float width = stringWidth + margen * 2;
            float fullWidth = width + margen + iconWidth;
            float targetX = right ? dragValue.getX() + maxWidth - fullWidth : dragValue.getX();
            float offscreenX = right ? dragValue.getX() + maxWidth + fullWidth : dragValue.getX() - fullWidth - margen;

            SmoothAnimationTimer xTimer = xTimers.get(m);
            if (xTimer == null) {
                xTimer = new SmoothAnimationTimer(offscreenX, offscreenX, 0.2F);
                xTimers.put(m, xTimer);
            }
            SmoothAnimationTimer yTimer = yTimers.get(m);
            if (yTimer == null) {
                yTimer = new SmoothAnimationTimer(startY, startY, 0.2F);
                yTimers.put(m, yTimer);
            }

            xTimer.target = m.isEnabled() ? targetX : offscreenX;
            xTimer.update(true);
            if (m.isEnabled()) {
                yTimer.target = startY;
                yTimer.update(true);
                startY += height + margen;
            } else {
                yTimer.target = yTimer.value;
                yTimer.update(true);
            }

            if (!m.isEnabled() && xTimer.isAnimationDone(true)) {
                continue;
            }

            float startX = xTimer.value;
            float renderY = yTimer.value;
            float iconStartX = startX + width + margen;

            Color color = Color.white;
            if (this.rainbow.getCurrentValue()) {
                color = ColorUtil.interpolateColorsBackAndForth((int) this.rainbowSpeed.getCurrentValue(), (int) -renderY, HUD.getColor1(), HUD.getColor2(), false);
            }

            Skia.drawShadow(startX, renderY, width, height, margen);
            Skia.drawRoundedBlur(startX, renderY, width, height, margen);
            Skia.drawRoundedRect(startX, renderY, width, height, margen, BACKGROUND_COLOR);
            Skia.drawText(text, startX + margen, renderY + margen, color, miSans);

            Skia.drawShadow(iconStartX, renderY, iconWidth, height, margen);
            Skia.drawRoundedBlur(iconStartX, renderY, iconWidth, height, margen);
            Skia.drawRoundedRect(iconStartX, renderY, iconWidth, height, margen, BACKGROUND_COLOR);
            Skia.drawText(m.getCategory().getIcon(), iconStartX + margen, renderY + margen, Color.WHITE, icon);
        }

        dragValue.setWidth(realMaxWidth);
        dragValue.setHeight(startY - dragValue.getY());

    }

    public String getModuleDisplayName(Module module) {
        String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
        return name + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
    }
}
