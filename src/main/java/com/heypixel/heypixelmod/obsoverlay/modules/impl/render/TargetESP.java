package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.KillAura;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ProjectionUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.vector.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInfo(
        name = "TargetESP",
        cnName = "目标ESP",
        description = "Render face overlay",
        category = Category.RENDER
)
public class TargetESP extends Module {
    private static final float BASE_SCALE = 10.0f;
    private static final float DISTANCE_CLAMP_MIN = 6.0f;
    private static final float DISTANCE_CLAMP_MAX = 18.0f;
    private static final float BASE_SIZE = 48.0f;
    private final Map<Entity, Vector2f> entityPositions = new ConcurrentHashMap<>();
    private final Map<Entity, SmoothAnimationTimer> hurtAlphas = new ConcurrentHashMap<>();
    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Face", "Rectangle")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();
    private final ModeValue image = ValueBuilder.create(this, "Image")
            .setModes("于哲", "于思礼", "李丹")
            .setDefaultModeIndex(0)
            .setVisibility(() -> mode.isCurrentMode("Face"))
            .build()
            .getModeValue();
    private final FloatValue size = ValueBuilder.create(this, "Size")
            .setDefaultFloatValue(BASE_SIZE)
            .setFloatStep(1.0f)
            .setMinFloatValue(8.0f)
            .setMaxFloatValue(128.0f)
            .build()
            .getFloatValue();
    private final BooleanValue hurt = ValueBuilder.create(this, "Hurt")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> mode.isCurrentMode("Face"))
            .build()
            .getBooleanValue();

    @EventTarget
    public void onRender(EventRender event) {
        try {
            updatePositions(event.getRenderPartialTicks());
        } catch (Exception ignored) {
        }
    }

    @EventTarget
    public void onRenderSkia(EventRenderSkia event) {
        for (Map.Entry<Entity, Vector2f> entry : entityPositions.entrySet()) {
            if (entry.getKey() != mc.player && entry.getKey() instanceof Player player) {
                Vector2f position = entry.getValue();
                if (position.x == Float.MAX_VALUE || position.y == Float.MAX_VALUE) {
                    continue;
                }
                if (mode.isCurrentMode("Rectangle")) {
                    drawRectangleStyle(player, position);
                } else {
                    double distance = mc.player.distanceTo(player);
                    float currentScale = computeScale(distance);
                    float drawSize = Math.max(6.0f, size.getCurrentValue() * currentScale);
                    float drawX = position.x - drawSize / 2.0f;
                    float drawY = position.y - drawSize / 2.0f;
                    Skia.drawImage(getFaceImagePath(), drawX, drawY, drawSize, drawSize);
                    drawHurtOverlay(player, drawX, drawY, drawSize);
                }
            }
        }
    }

    private void drawHurtOverlay(Player player, float drawX, float drawY, float drawSize) {
        SmoothAnimationTimer alpha = hurtAlphas.computeIfAbsent(player, ignored -> new SmoothAnimationTimer(0.0F));
        alpha.target = hurt.getCurrentValue() && player.hurtTime > 0 ? 120.0f : 0.0f;
        alpha.update(true);
        if (alpha.value > 1.0f) {
            Skia.drawRoundedRect(drawX, drawY, drawSize, drawSize, 0.0f, new Color(255, 0, 0, (int) alpha.value));
        }
    }

    private String getFaceImagePath() {
        if (image.isCurrentMode("于思礼")) {
            return "yuzhe/ysl.png";
        }
        if (image.isCurrentMode("李丹")) {
            return "yuzhe/lidan.png";
        }
        return "yuzhe/yuzhe.jpg";
    }

    private String getRectangleImagePath() {
        return "targetesp/rectangle.png";
    }

    private float computeScale(double distance) {
        double clampedDistance = Math.max(DISTANCE_CLAMP_MIN, Math.min(distance, DISTANCE_CLAMP_MAX));
        return (float) (BASE_SCALE / clampedDistance);
    }

    private void updatePositions(float renderPartialTicks) {
        entityPositions.clear();
        KillAura aura = Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (aura == null || !aura.isEnabled() || aura.target == null) {
            hurtAlphas.clear();
            return;
        }
        List<Entity> targets = aura.getTargets();
        if (targets == null || targets.isEmpty()) {
            hurtAlphas.clear();
            return;
        }
        for (Entity entity : targets) {
            if (entity instanceof Player) {
                Vector2f vector = mode.isCurrentMode("Rectangle")
                        ? projectRectanglePosition(entity, renderPartialTicks)
                        : projectFacePosition(entity, renderPartialTicks);
                entityPositions.put(entity, vector);
            }
        }
        hurtAlphas.keySet().retainAll(entityPositions.keySet());
    }

    private Vector2f projectFacePosition(Entity entity, float renderPartialTicks) {
        double x = MathUtils.interpolate(entity.xo, entity.getX(), renderPartialTicks);
        double y = MathUtils.interpolate(entity.yo, entity.getY(), renderPartialTicks) + entity.getEyeHeight();
        double z = MathUtils.interpolate(entity.zo, entity.getZ(), renderPartialTicks);
        return ProjectionUtils.project(x, y, z, renderPartialTicks);
    }

    private Vector2f projectRectanglePosition(Entity entity, float renderPartialTicks) {
        double x = MathUtils.interpolate(entity.xo, entity.getX(), renderPartialTicks);
        double y = MathUtils.interpolate(entity.yo, entity.getY(), renderPartialTicks);
        double z = MathUtils.interpolate(entity.zo, entity.getZ(), renderPartialTicks);
        double height = entity.getBbHeight();
        if (entity instanceof LivingEntity living && living.isBaby()) {
            height /= 1.75f;
        }
        height /= 2.0f;
        Vector2f bottom = ProjectionUtils.project(x, y, z, renderPartialTicks);
        Vector2f top = ProjectionUtils.project(x, y + height, z, renderPartialTicks);
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        if (bottom.x != Float.MAX_VALUE && bottom.y != Float.MAX_VALUE) {
            minX = Math.min(minX, bottom.x);
            minY = Math.min(minY, bottom.y);
        }
        if (top.x != Float.MAX_VALUE && top.y != Float.MAX_VALUE) {
            minX = Math.min(minX, top.x);
            minY = Math.min(minY, top.y);
        }
        return minX == Float.MAX_VALUE ? new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE) : new Vector2f(minX, minY);
    }

    private void drawRectangleStyle(Player player, Vector2f position) {
        float distance = mc.player.distanceTo(player);
        float scale = 1.0f - Mth.clamp(Math.abs(distance - 6.0f) / 60.0f, 0.0f, 0.75f);
        long millis = System.currentTimeMillis() + 1200L;
        double angle = Mth.clamp((Math.sin(millis / 150.0) + 1.0) / 2.0 * 30.0, 0.0, 30.0);
        double scaled = Mth.clamp((Math.sin(millis / 500.0) + 1.0) / 2.0, 0.8, 1.0);
        double rotate = Mth.clamp((Math.sin(millis / 1000.0) + 1.0) / 2.0 * 360.0, 0.0, 360.0);
        rotate = 45.0 - (angle - 15.0) + rotate;
        float drawSize = 128.0f * scale * (float) scaled;
        float drawX = position.x - drawSize / 2.0f;
        float drawY = position.y - drawSize / 2.0f;
        Skia.save();
        Skia.rotate(drawX, drawY, drawSize, drawSize, (float) rotate);
        Skia.drawImage(getRectangleImagePath(), drawX, drawY, drawSize, drawSize);
        Skia.restore();
    }
}
