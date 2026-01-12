package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.HUDEditor;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(
        name = "HUDEditor",
        cnName = "HUD编辑器",
        category = Category.RENDER,
        description = "HUD editor screen."
)
public class HUDEditModule extends Module {
    HUDEditor hudEditor = null;

    @Override
    protected void initModule() {
        super.initModule();
        this.setKey(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    @Override
    public void onEnable() {
        if (this.hudEditor == null) {
            this.hudEditor = new HUDEditor();
        }

        mc.setScreen(this.hudEditor);
        this.toggle();
    }

}
