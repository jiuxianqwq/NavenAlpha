package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.core.particles.ParticleTypes;

import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(
        name = "Weather",
        cnName = "天气",
        description = "Customize world weather",
        category = Category.RENDER
)
public class Weather extends Module {
    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Clear", "Rain", "Snow")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    private float lastRainLevel;
    private float lastThunderLevel;
    private boolean stored;

    @Override
    public void onEnable() {
        if (mc.level == null) {
            stored = false;
            return;
        }

        lastRainLevel = mc.level.getRainLevel(1.0F);
        lastThunderLevel = mc.level.getThunderLevel(1.0F);
        stored = true;
    }

    @Override
    public void onDisable() {
        if (mc.level != null && stored) {
            mc.level.setRainLevel(lastRainLevel);
            mc.level.setThunderLevel(lastThunderLevel);
        }
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() != EventType.PRE || mc.level == null) {
            return;
        }

        setSuffix(mode.getCurrentMode());
        switch (mode.getCurrentMode()) {
            case "Clear" -> applyClear();
            case "Rain" -> applyRain();
            case "Snow" -> applySnow();
        }
    }

    private void applyClear() {
        mc.level.setRainLevel(0.0F);
        mc.level.setThunderLevel(0.0F);
    }

    private void applyRain() {
        mc.level.setRainLevel(1.0F);
        mc.level.setThunderLevel(0.0F);
    }

    private void applySnow() {
        mc.level.setRainLevel(1.0F);
        mc.level.setThunderLevel(0.0F);

        if (mc.player == null) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double baseX = mc.player.getX();
        double baseY = mc.player.getY() + 2.0;
        double baseZ = mc.player.getZ();
        for (int i = 0; i < 20; i++) {
            double x = baseX + random.nextDouble(-8.0, 8.0);
            double y = baseY + random.nextDouble(0.0, 6.0);
            double z = baseZ + random.nextDouble(-8.0, 8.0);
            mc.level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0, -0.01, 0.0);
        }
    }
}
