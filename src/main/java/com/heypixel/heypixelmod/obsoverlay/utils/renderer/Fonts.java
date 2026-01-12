package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;

import java.awt.*;
import java.io.IOException;

public class Fonts {
    public static CustomTextRenderer miSans;
    public static CustomTextRenderer icons;

    public static void loadFonts() throws IOException, FontFormatException {
        miSans = new CustomTextRenderer("MiSans-Regular", 32, 0, 65535, 16384);
        icons = new CustomTextRenderer("icon", 32, 59648, 59652, 512);
    }
}
