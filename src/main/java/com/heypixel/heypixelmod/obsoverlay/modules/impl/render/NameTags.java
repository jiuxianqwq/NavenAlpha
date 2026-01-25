package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ProjectionUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.shader.impl.KawaseBlur;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.vector.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.platform.Window;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInfo(
        name = "NameTags",
        cnName = "名称标签",
        category = Category.RENDER,
        description = "Renders name tags"
)
public class NameTags extends Module {
    private final Map<Entity, Vector2f> entityPositions = new ConcurrentHashMap<>();

    public FloatValue scale = ValueBuilder.create(this, "Scale")
            .setDefaultFloatValue(0.3F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
    public com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue blur = ValueBuilder.create(this, "Blur")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue bloom = ValueBuilder.create(this, "Bloom")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private static final float BASE_SCALE = 10.0f;
    private static final float DISTANCE_CLAMP_MIN = 8.0f;
    private static final float DISTANCE_CLAMP_MAX = 16.0f;

    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 100);
    private static final Color HEALTH_TEXT_COLOR = new Color(200, 200, 200);
    private static final Color HEART_ICON_COLOR = new Color(255, 85, 85);
    
    private final List<String> statusParts = new ArrayList<>();

    @EventTarget
    public void update(com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender e) {
        try {
            this.updatePositions(e.getRenderPartialTicks());
        } catch (Exception ignored) {
        }
    }

    @EventTarget
    public void onRenderSkia(EventRenderSkia e) {
        for (Entry<Entity, Vector2f> entry : this.entityPositions.entrySet()) {
            if (entry.getKey() != mc.player && entry.getKey() instanceof Player living) {
                float hp = living.getHealth();
                if (hp > 20.0F) {
                    living.setHealth(20.0F);
                }

                Vector2f position = entry.getValue();


                float padding = 6.0f * scale.getCurrentValue();
                float radius = 6.0f * scale.getCurrentValue();
                float spacing = 4.0f * scale.getCurrentValue();
                float iconGap = 3.0f * scale.getCurrentValue();

                statusParts.clear();
                if (Teams.isSameTeam(living)) statusParts.add("§aTeam");
                if (FriendManager.isFriend(living)) statusParts.add("§aFriend");

                String statusText = String.join(" ", statusParts);
                String nameText = living.getName().getString() + (AuthUtils.transport.isUser(living.getName().getString()) ? " §f(§b" + AuthUtils.transport.getName(living.getName().getString()) + "§f)" : "");
                String healthText = String.valueOf(Math.round(hp));
                if (living.getAbsorptionAmount() > 0.0F) {
                    healthText += "+" + Math.round(living.getAbsorptionAmount());
                }

                float baseFontSize = 14f * scale.getCurrentValue();
                Font font = Fonts.getMiSans(baseFontSize);
                Font iconFont = Fonts.getIconFill(baseFontSize);

                float statusWidth = statusText.isEmpty() ? 0 : Skia.getStringWidth(statusText, font) + (padding * 2);
                float nameWidth = Skia.getStringWidth(nameText, font) + (padding * 2);
                float heartWidth = iconFont.measureTextWidth("\ue87d");
                float healthWidth = Skia.getStringWidth(healthText, font) + heartWidth + (padding * 2) + iconGap;

                float totalWidth = nameWidth + healthWidth + (statusWidth > 0 ? statusWidth + spacing : 0) + spacing;
                float height = baseFontSize + (padding * 1.5f);

                float startX = position.x - totalWidth / 2.0f;
                float startY = position.y - height / 2.0f;

                float currentX = startX;

                if (!statusText.isEmpty()) {
                    renderSegment(currentX, startY, statusWidth, height, radius, padding, iconGap, statusText, font, Color.WHITE, null, null);
                    currentX += statusWidth + spacing;
                }

                renderSegment(currentX, startY, nameWidth, height, radius, padding, iconGap, nameText, font, Color.WHITE, null, null);
                currentX += nameWidth + spacing;

                renderSegment(currentX, startY, healthWidth, height, radius, padding, iconGap, healthText, font, HEALTH_TEXT_COLOR, "\ue87d", iconFont);
            }
        }
    }

    private float computeScale(double distance) {
        double clampedDistance = Math.max(DISTANCE_CLAMP_MIN, Math.min(distance, DISTANCE_CLAMP_MAX));
        return (float) ((BASE_SCALE * this.scale.getCurrentValue()) / clampedDistance);
    }

    private void renderSegment(float x, float y, float width, float height, float radius, float padding, float iconGap, String text, Font font, Color textColor, String icon, Font iconFont) {
        if (bloom.getCurrentValue()) {
            Skia.drawShadow(x, y, width, height, radius);
        }
        if (blur.getCurrentValue()) {
            Skia.drawRoundedBlur(x, y, width, height, radius);
        }
        Skia.drawRoundedRect(x, y, width, height, radius, BACKGROUND_COLOR);

        float textY = y + (height / 2.0f) - (font.getMetrics().getCapHeight() / 2.0f);
        float contentX = x + padding;

        Skia.drawText(text, contentX, textY, textColor, font);

        if (icon != null && iconFont != null) {
            float textWidth = Skia.getStringWidth(text, font);
            Skia.drawText(icon, contentX + textWidth + iconGap, textY, HEART_ICON_COLOR, iconFont);
        }
    }

    private void updatePositions(float renderPartialTicks) {
        this.entityPositions.clear();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player && !entity.getName().getString().startsWith("CIT-")) {
                double x = MathUtils.interpolate(entity.xo, entity.getX(), renderPartialTicks);
                double y = MathUtils.interpolate(entity.yo, entity.getY(), renderPartialTicks) + (double) entity.getBbHeight() + 0.5;
                double z = MathUtils.interpolate(entity.zo, entity.getZ(), renderPartialTicks);
                Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
                vector.setY(vector.getY() - 2.0F);
                this.entityPositions.put(entity, vector);
            }
        }
    }
}