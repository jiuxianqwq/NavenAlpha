package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.utils.shader.impl.KawaseBlur;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.context.SkiaContext;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class MixinWindow {
    @Shadow
    private int width;

    @Shadow
    private int height;

    @Inject(method = "onFramebufferResize", at = @At("HEAD"))
    private void onFramebufferResize(long window, int width, int height, CallbackInfo ci) {
        SkiaContext.createSurface(width, height);
        KawaseBlur.GUI_BLUR.resize();
        KawaseBlur.INGAME_BLUR.resize();
    }
}
