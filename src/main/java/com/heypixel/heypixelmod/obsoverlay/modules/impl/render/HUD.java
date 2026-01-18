package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;

import java.awt.*;

@ModuleInfo(
        name = "HUD",
        cnName = "抬头显示器",
        description = "Displays information on your screen",
        category = Category.RENDER
)
public class HUD extends Module {
    public static final int headerColor = new Color(150, 45, 45, 255).getRGB();
    public static final int bodyColor = new Color(0, 0, 0, 120).getRGB();
    public static final int backgroundColor = new Color(0, 0, 0, 40).getRGB();
    public BooleanValue moduleToggleSound = ValueBuilder.create(this, "Module Toggle Sound").setDefaultBooleanValue(true).build().getBooleanValue();

    public FloatValue red1 = ValueBuilder.create(this, "Red 1")
            .setDefaultFloatValue(102.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();
    public FloatValue green1 = ValueBuilder.create(this, "Green 1")
            .setDefaultFloatValue(255.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();
    public FloatValue blue1 = ValueBuilder.create(this, "Blue 1")
            .setDefaultFloatValue(209.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();
    public FloatValue red2 = ValueBuilder.create(this, "Red 2")
            .setDefaultFloatValue(6.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();
    public FloatValue green2 = ValueBuilder.create(this, "Green 2")
            .setDefaultFloatValue(149.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();
    public FloatValue blue2 = ValueBuilder.create(this, "Blue 2")
            .setDefaultFloatValue(255.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();


    public static Color getColor1() {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        return new Color(hud.red1.getCurrentValue() / 255.0f, hud.green1.getCurrentValue() / 255.0f, hud.blue1.getCurrentValue() / 255.0f);
    }

    public static Color getColor2() {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        return new Color(hud.red2.getCurrentValue() / 255.0f, hud.green2.getCurrentValue() / 255.0f, hud.blue2.getCurrentValue() / 255.0f);
    }
}
