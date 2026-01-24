package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatScreen.class})
public class MixinChatScreen {
    @Inject(method = "render", at = @At("HEAD"))
    private void beginChatInputRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        if (hud != null) {
            hud.beginChatInputRender();
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
                    ordinal = 0
            ),
            require = 0
    )
    private void redirectChatInputBackground(GuiGraphics instance, int x1, int y1, int x2, int y2, int color) {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        if (hud != null && hud.isEnabled() && hud.chatInputBackground.getCurrentValue()) {
            hud.trackChatInputBackground(x1, y1, x2, y2);
            return;
        }
        instance.fill(x1, y1, x2, y2, color);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onChatScreenInit(CallbackInfo ci) {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        if (hud != null) {
            hud.resetChatInputAnimation();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onChatScreenRemoved(CallbackInfo ci) {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        if (hud != null) {
            hud.beginChatInputClose();
        }
    }
}
