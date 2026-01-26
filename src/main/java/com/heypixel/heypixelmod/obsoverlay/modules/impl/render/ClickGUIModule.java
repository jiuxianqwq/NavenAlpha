package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.ClickGUI;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;

@ModuleInfo(
        name = "ClickGUI",
        cnName = "点击用户图形界面",
        category = Category.RENDER,
        description = "The ClickGUI"
)
public class ClickGUIModule extends Module {
    public ModeValue lang = ValueBuilder
            .create(this, "Language")
            .setModes("English", "中文")
            .setOnUpdate(value -> Module.update = true)
            .build()
            .getModeValue();
    public ModeValue style = ValueBuilder
            .create(this, "Style")
            .setModes("Naven")
            .setOnUpdate(value -> Module.update = true)
            .build()
            .getModeValue();
    ClickGUI clickGUI = null;

    @Override
    protected void initModule() {
        super.initModule();
        this.setKey(344);
    }

    @Override
    public void onEnable() {
        if (this.clickGUI == null) {
            this.clickGUI = new ClickGUI();
        }

        mc.setScreen(this.clickGUI);
        this.toggle();
    }

    public String getLanguage() {
        return lang.getCurrentMode();
    }
}
