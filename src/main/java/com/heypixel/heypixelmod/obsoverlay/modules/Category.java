package com.heypixel.heypixelmod.obsoverlay.modules;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule;
import com.heypixel.heypixelmod.obsoverlay.utils.FontIcons;

public enum Category {
    COMBAT("Combat", "战斗", FontIcons.SWORD),
    MOVEMENT("Movement", "移动", FontIcons.RUNNING),
    RENDER("Render", "视觉", FontIcons.EYE),
    MISC("Misc", "杂项", FontIcons.OTHER);

    private final String displayName;
    private final String cnName;
    private final String icon;

    Category(final String displayName, final String cnName, final String icon) {
        this.displayName = displayName;
        this.cnName = cnName;
        this.icon = icon;
    }

    public String getDisplayName() {
//      return this.displayName;
//      return this.name;
        if (Naven.getInstance().getModuleManager() == null || Naven.getInstance() == null) return this.displayName;
        ClickGUIModule module = Naven.getInstance().getModuleManager().getModule(ClickGUIModule.class);
        if (module == null) return this.displayName;

        return module.lang.getCurrentMode().equals("English") ? this.displayName : this.cnName;
    }

    public String getIcon() {
        return this.icon;
    }
}
