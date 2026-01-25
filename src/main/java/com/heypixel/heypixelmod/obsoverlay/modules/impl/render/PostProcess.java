package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.BlurUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.ShadowUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
        name = "PostProcess",
        cnName = "后处理",
        description = "Post process effects",
        category = Category.RENDER
)
public class PostProcess extends Module {
    private static PostProcess instance;

    private final BooleanValue fastBlur = ValueBuilder.create(this, "FastBlur")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final FloatValue blurFPS = ValueBuilder.create(this, "Blur FPS")
            .setFloatStep(1.0F)
            .setDefaultFloatValue(90.0F)
            .setMinFloatValue(15.0F)
            .setMaxFloatValue(120.0F)
            .setVisibility(fastBlur::getCurrentValue)
            .build()
            .getFloatValue();
    private final FloatValue strength = ValueBuilder.create(this, "Blur Strength")
            .setDefaultFloatValue(2.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(19.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    
    private final BooleanValue glow = ValueBuilder.create(this, "Glow")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public PostProcess() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.setEnabled(false);
    }

    public int getStrength() {
        return (int) strength.getCurrentValue();
    }

    public int getBlurFPS() {
        return (int) blurFPS.getCurrentValue();
    }

    public boolean getFastBlur() {
        return fastBlur.getCurrentValue();
    }

    public static boolean isGlowEnabled() {
        return instance != null && instance.glow.getCurrentValue();
    }
}
