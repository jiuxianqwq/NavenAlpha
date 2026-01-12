package com.heypixel.heypixelmod.obsoverlay.utils.skia.font;

import io.github.humbleui.skija.Font;

public class Fonts {

    private static final String ICON_FILL = "MaterialSymbolsRounded_Fill.ttf";
    private static final String ICON = "MaterialSymbolsRounded.ttf";
    private static final String navenIcon = "icon.ttf";
    private static final String MISANS = "MiSans-Regular.ttf";
    private static final String urbanistVariable = "Urbanist-VariableFont_wght.ttf";

    public static void loadAll() {
        FontHelper.preloadFonts(MISANS, ICON_FILL, ICON);
    }

    public static Font getMiSans(float size) {
        return FontHelper.load(MISANS, size);
    }


    public static Font getIconFill(float size) {
        return FontHelper.load(ICON_FILL, size);
    }

    public static Font getIcon(float size) {
        return FontHelper.load(ICON, size);
    }

    public static Font getUrbanistVariable(float size) {
        return FontHelper.load(urbanistVariable, size);
    }

    public static Font getNavenIcon(float size) {
        return FontHelper.load(navenIcon, size);
    }
}
