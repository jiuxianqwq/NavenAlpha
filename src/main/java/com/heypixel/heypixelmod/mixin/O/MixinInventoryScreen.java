package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InventoryScreen.class)
public class MixinInventoryScreen {
    @Redirect(method = "renderBg", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void redirectBlit(GuiGraphics instance, ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        if (hud != null && hud.isEnabled() && hud.betterGui.getCurrentValue()) {
            return;
        }
        instance.blit(texture, x, y, u, v, width, height);
    }
}
