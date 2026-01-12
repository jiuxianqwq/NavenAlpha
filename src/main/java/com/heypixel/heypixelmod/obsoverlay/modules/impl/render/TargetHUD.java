package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.KillAura;
import com.heypixel.heypixelmod.obsoverlay.ui.HUDEditor;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Path;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.EntityHitResult;

import java.awt.*;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/29 01:43
 * @Filename：Watermark
 */
@ModuleInfo(name = "TargetHUD", cnName = "目标状态显示", description = "Display target info.", category = Category.RENDER)
public class TargetHUD extends Module {
    private final SmoothAnimationTimer heal = new SmoothAnimationTimer(20.0F);
    private final SmoothAnimationTimer alpha = new SmoothAnimationTimer(255.0F);
    private final DragValue dragValue = ValueBuilder.create(this, "Position")
            .setDefaultX(500f)
            .setDefaultY(200f)
            .build()
            .getDragValue();
    private AbstractClientPlayer target;

    @EventTarget
    public void onRenderSkia(EventRenderSkia event) {
        if (mc.screen instanceof HUDEditor) {
            target = mc.player;
            alpha.value = 255.0f;
        }
        alpha.update(true);
        if (alpha.value < 1.0f) return;
        heal.update(true);
        float x = dragValue.getX();
        float y = dragValue.getY();
        float width = 125.0f;
        float height = 40.0f;
        Font miSans = Fonts.getMiSans(10.0f);
        Font iconFill = Fonts.getIconFill(10.0f);
        float heartIconWidth = iconFill.measureTextWidth("\ue87d");
        String name = target.getName().getString();
        name = name + (AuthUtils.transport.isUser(name) ? " §f(§b" + AuthUtils.transport.getName(name) + "§f)" : "");
        float nameWidth = Skia.getStringWidth(name, miSans);
        float healBarWidth = (nameWidth + 45.0f > width) ? nameWidth : 78.0f;
        if (nameWidth + 45.0f > width) width = nameWidth + 45.0f;

        try (Paint layerPaint = new Paint()) {
            layerPaint.setAlpha((int) alpha.value);
            Skia.getCanvas().saveLayer(Rect.makeXYWH(x - 10, y - 10, width + 20, height + 20), layerPaint);
            Skia.drawShadow(x, y, width, height, 6.0f);
            Skia.drawRoundedBlur(x, y, width, height, 6.0f);
            Skia.drawRoundedRect(x, y, width, height, 6.0f, new Color(0, 0, 0, 100));
            Skia.drawPlayerHead(target, x + 5.0f, y + 5.0f, 30.0f, 30.0f, 4.0f);
            Path path = new Path();
            path.addRRect(RRect.makeXYWH(x + 40.0f, y + 30.5f, healBarWidth, 5.0f, 3.0f));
            Skia.save();
            Skia.getCanvas().clipPath(path, ClipMode.INTERSECT, true);
            Skia.drawRect(x + 40.0f, y + 30.5f, healBarWidth, 5.0f, new Color(0, 0, 0, 50));
            float healthWidth = (healBarWidth / 20.0f) * heal.value;
            Skia.drawRect(x + 40.0f, y + 30.5f, healthWidth, 5.0f, new Color(200, 200, 200));
            Skia.restore();
            Skia.drawText(name, x + 40.0f, y + 4.5f, new Color(255, 255, 255), miSans);
            Skia.drawText("\ue87d", x + 40.0f, y + 18f, new Color(200, 200, 200), iconFill);
            Skia.drawText(String.format("%.2f", heal.value), x + 40.0f + heartIconWidth, y + 17.5f, new Color(200, 200, 200), miSans);
            path.close();
            Skia.restore();
        }
        dragValue.setWidth(width);
        dragValue.setHeight(height);
    }

    @EventTarget
    public void onPreTick(EventRunTicks event) {
        if (mc.player == null || event.type() != EventType.PRE) return;
        KillAura module = Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (mc.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof AbstractClientPlayer player) {
            alpha.target = 255.0f;
            target = player;
            heal.target = player.getHealth();
        } else if (module.isEnabled() && module.target instanceof AbstractClientPlayer player) {
            alpha.target = 255.0f;
            target = player;
            heal.target = player.getHealth();
        } else {
            alpha.target = 0.0f;
        }
    }
}
