package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.Path;
import io.github.humbleui.types.RRect;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.awt.*;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInfo(
        name = "EffectHUD",
        cnName = "效果显示",
        description = "Displays potion effects on the HUD",
        category = Category.RENDER
)
public class EffectDisplay extends Module {
    private final Map<MobEffect, EffectDisplay.MobEffectInfo> infos = new ConcurrentHashMap<>();
    private final DragValue dragValue = ValueBuilder.create(this, "Position")
            .setDefaultX(10f)
            .setDefaultY(250f)
            .build()
            .getDragValue();


    @EventTarget
    public void onRender(EventRenderSkia e) {
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            EffectDisplay.MobEffectInfo info;
            if (this.infos.containsKey(effect.getEffect())) {
                info = this.infos.get(effect.getEffect());
            } else {
                info = new EffectDisplay.MobEffectInfo();
                this.infos.put(effect.getEffect(), info);
            }

            info.maxDuration = Math.max(info.maxDuration, effect.getDuration());
            info.duration = effect.getDuration();
            info.amplifier = effect.getAmplifier();
            info.shouldDisappear = false;
        }

        int startY = (int) dragValue.getY();

        for (Entry<MobEffect, EffectDisplay.MobEffectInfo> entry : this.infos.entrySet()) {
            EffectDisplay.MobEffectInfo effectInfo = entry.getValue();
            String text = this.getDisplayName(entry.getKey(), effectInfo);
            if (effectInfo.yTimer.value == -1.0F) {
                effectInfo.yTimer.value = (float) startY;
            }

            float x = effectInfo.xTimer.value;
            float y = effectInfo.yTimer.value;
            String duration = StringUtil.formatTickDuration(effectInfo.duration);
            float iconX = x;
            float iconY = y;
            float iconW = 20.0f;
            float iconH = 20.0f;

            float infoX = x + 25.0f;
            float infoY = y;
            float infoW = Skia.getStringWidth(text, Fonts.getMiSans(10f)) + Skia.getStringWidth(duration, Fonts.getMiSans(8f)) + 12.0f;
            float infoH = 20.0f;

            effectInfo.width = 25.0F + infoW;
            effectInfo.shouldDisappear = !mc.player.hasEffect(entry.getKey());
            if (effectInfo.shouldDisappear) {
                effectInfo.xTimer.target = -effectInfo.width - 20.0F;
                if (x <= -effectInfo.width - 20.0F) {
                    this.infos.remove(entry.getKey());
                }
            } else {
                effectInfo.durationTimer.target = (float) effectInfo.duration / (float) effectInfo.maxDuration * effectInfo.width;
                if (effectInfo.durationTimer.value <= 0.0F) {
                    effectInfo.durationTimer.value = effectInfo.durationTimer.target;
                }

                effectInfo.xTimer.target = dragValue.getX();
                effectInfo.yTimer.target = (float) startY;
                effectInfo.yTimer.update(true);
            }

            effectInfo.durationTimer.update(true);
            effectInfo.xTimer.update(true);

            Skia.drawShadow(iconX, iconY, iconW, iconH, 5.0f);
            Skia.drawRoundedBlur(iconX, iconY, iconW, iconH, 5.0f);
            Skia.drawShadow(infoX, infoY, infoW, infoH, 5.0f);
            Skia.drawRoundedBlur(infoX, infoY, infoW, infoH, 5.0f);
            Path path = new Path();
            path.addRRect(RRect.makeXYWH(iconX, iconY, iconW, iconH, 5.0F));
            path.addRRect(RRect.makeXYWH(infoX, infoY, infoW, infoH, 5.0F));
            Skia.save();
            Skia.getCanvas().clipPath(path, ClipMode.INTERSECT, true);
            Skia.drawRect(x, y, effectInfo.width, 30.0F, new Color(0, 0, 0, 50));
            Skia.drawRect(x, y, effectInfo.durationTimer.value, 30.0F, new Color(0, 0, 0, 50));
            Skia.restore();
            Skia.drawEffectIcon(entry.getKey(), iconX + 2.0f, iconY + 2.0f, 16, 16);
            Skia.drawText(text, infoX + 5, infoY + 5, new Color(255, 255, 255), Fonts.getMiSans(10f));
            Skia.drawText(duration, infoX + 7 + Skia.getStringWidth(text, Fonts.getMiSans(10f)), infoY + 8, new Color(200, 200, 200), Fonts.getMiSans(8));
            startY += 28;
            dragValue.setWidth(Math.max(effectInfo.width, dragValue.getWidth()));
            dragValue.setHeight(startY - dragValue.getY());
        }
    }

    public String getDisplayName(MobEffect effect, EffectDisplay.MobEffectInfo info) {
        String effectName = effect.getDisplayName().getString();
        String amplifierName;
        if (info.amplifier == 0) {
            amplifierName = "";
        } else if (info.amplifier == 1) {
            amplifierName = " " + I18n.get("enchantment.level.2");
        } else if (info.amplifier == 2) {
            amplifierName = " " + I18n.get("enchantment.level.3");
        } else if (info.amplifier == 3) {
            amplifierName = " " + I18n.get("enchantment.level.4");
        } else {
            amplifierName = " " + info.amplifier;
        }

        return effectName + amplifierName;
    }

    public static class MobEffectInfo {
        public SmoothAnimationTimer xTimer = new SmoothAnimationTimer(-60.0F, 0.2F);
        public SmoothAnimationTimer yTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
        public SmoothAnimationTimer durationTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
        public int maxDuration = -1;
        public int duration = 0;
        public int amplifier = 0;
        public boolean shouldDisappear = false;
        public float width;
    }
}
