package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatComponent.class})
public class MixinChatComponent {
    @Inject(method = "render", at = @At("HEAD"))
    private void beginChatRender(GuiGraphics guiGraphics, int tickCount, int mouseX, int mouseY, CallbackInfo ci) {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        if (hud != null) {
            hud.beginChatRender();
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
            )
    )
    private void redirectChatBackground(GuiGraphics instance, int x1, int y1, int x2, int y2, int color) {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        if (hud != null && hud.isEnabled() && hud.chatBackground.getCurrentValue()) {
            hud.trackChatBackground(x1, y1, x2, y2);
            return;
        }
        instance.fill(x1, y1, x2, y2, color);
    }
}
