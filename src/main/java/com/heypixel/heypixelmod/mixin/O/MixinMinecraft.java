package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShutdown;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.Glow;
import com.heypixel.heypixelmod.obsoverlay.utils.AnimationUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthInit;
import com.heypixel.heypixelmod.obsoverlay.utils.shader.impl.KawaseBlur;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.context.SkiaContext;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin({Minecraft.class})
public class MixinMinecraft {
    @Unique
    private int skipTicks;
    @Unique
    private long naven_Modern$lastFrame;

    @Inject(
            method = {"<init>"},
            at = {@At("TAIL")}
    )
    private void onInit(CallbackInfo info) {
        new AuthInit();
    }

    @Inject(
            method = {"<init>"},
            at = {@At("RETURN")}
    )
    public void onInit(GameConfig pGameConfig, CallbackInfo ci) {
        System.setProperty("java.awt.headless", "false");
        ModList.get().getMods().removeIf(modInfox -> modInfox.getModId().contains("naven"));
        List<IModFileInfo> fileInfoToRemove = new ArrayList<>();

        for (IModFileInfo fileInfo : ModList.get().getModFiles()) {
            for (IModInfo modInfo : fileInfo.getMods()) {
                if (modInfo.getModId().contains("naven")) {
                    fileInfoToRemove.add(fileInfo);
                }
            }
        }

        ModList.get().getModFiles().removeAll(fileInfoToRemove);
    }

    @Inject(
            method = {"close"},
            at = {@At("HEAD")},
            remap = false
    )
    private void shutdown(CallbackInfo ci) {
        if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
            Naven.getInstance().getEventManager().call(new EventShutdown());
        }
    }

    @Inject(
            method = {"tick"},
            at = {@At("HEAD")}
    )
    private void tickPre(CallbackInfo ci) {
        if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
            Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.PRE));
        }
    }

    @Inject(
            method = {"tick"},
            at = {@At("TAIL")}
    )
    private void tickPost(CallbackInfo ci) {
        if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
            Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.POST));
        }
    }

    @Inject(
            method = {"shouldEntityAppearGlowing"},
            at = {@At("RETURN")},
            cancellable = true
    )
    private void shouldEntityAppearGlowing(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
        if (Glow.shouldGlow(pEntity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = {"runTick"},
            at = {@At("HEAD")}
    )
    private void runTick(CallbackInfo ci) {
        long currentTime = System.nanoTime() / 1000000L;
        int deltaTime = (int) (currentTime - this.naven_Modern$lastFrame);
        this.naven_Modern$lastFrame = currentTime;
        AnimationUtils.delta = deltaTime;
    }

    @ModifyArg(
            method = {"runTick"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"
            )
    )
    private float fixSkipTicks(float g) {
        if (this.skipTicks > 0) {
            g = 0.0F;
        }

        return g;
    }

    @Inject(
            method = {"handleKeybinds"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
                    ordinal = 0,
                    shift = Shift.BEFORE
            )},
            cancellable = true
    )
    private void clickEvent(CallbackInfo ci) {
        if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
            EventClick event = new EventClick();
            Naven.getInstance().getEventManager().call(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(
            method = {"resizeDisplay"},
            at = {@At("HEAD")}
    )
    private void onResizeDisplay(CallbackInfo ci) {
        Window window = Minecraft.getInstance().getWindow();
        SkiaContext.createSurface(window.getWidth(), window.getHeight());
        KawaseBlur.GUI_BLUR.resize();
        KawaseBlur.INGAME_BLUR.resize();
    }
}
