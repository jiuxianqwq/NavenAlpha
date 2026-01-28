package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.context.SkiaContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.awt.Color;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {
    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    protected int imageWidth;

    @Shadow
    protected int imageHeight;

    @Shadow
    protected abstract void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY);

    @Inject(method = "render", at = @At("HEAD"))
    private void renderSkia(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        if (hud == null || !hud.isEnabled() || !hud.betterGui.getCurrentValue()) {
            return;
        }
        try {
            SkiaContext.draw((context) -> {
                Skia.save();
                Skia.scale((float) Minecraft.getInstance().getWindow().getGuiScale());
                float x = (float) leftPos;
                float y = (float) topPos;
                float w = (float) imageWidth;
                float h = (float) imageHeight;
                float radius = hud.betterGuiRadius.getCurrentValue();
                if (hud.betterGuiShadow.getCurrentValue()) {
                    Skia.drawShadow(x, y, w, h, radius);
                }
                if (hud.betterGuiBlur.getCurrentValue()) {
                    Skia.drawRoundedBlur(x, y, w, h, radius);
                }
                Skia.drawRoundedRect(x, y, w, h, radius, new Color(18, 18, 18, (int) hud.betterGuiOpacity.getCurrentValue()));
                AbstractContainerMenu menu = ((AbstractContainerScreen<?>) (Object) this).getMenu();
                if (menu != null) {
                    Color slotColor = new Color(25, 25, 25, (int) hud.betterGuiSlotOpacity.getCurrentValue());
                    for (Slot slot : menu.slots) {
                        float slotX = x + slot.x;
                        float slotY = y + slot.y;
                        Skia.drawRoundedRect(slotX, slotY, 16, 16, 4, slotColor);
                    }
                }
                Skia.restore();
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"))
    private void redirectRenderBg(AbstractContainerScreen<?> instance, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        if (hud != null && hud.isEnabled() && hud.betterGui.getCurrentValue() && !(instance instanceof InventoryScreen)) {
            return;
        }
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }
}
