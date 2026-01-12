package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderScoreboard;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventSetTitle;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.NoRender;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.PostProcess;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.Scoreboard;
import com.heypixel.heypixelmod.obsoverlay.utils.shader.impl.KawaseBlur;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.context.SkiaContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(
        value = {Gui.class},
        priority = 100
)
public class MixinGui {
    @Shadow
    @Nullable
    protected Component title;
    @Shadow
    protected int titleTime;
    @Shadow
    protected int titleFadeInTime;
    @Shadow
    protected int titleStayTime;
    @Shadow
    protected int titleFadeOutTime;
    @Shadow
    @Nullable
    protected Component subtitle;

    @Inject(
            method = {"displayScoreboardSidebar"},
            at = {@At("HEAD")}
    )
    public void hookScoreboardHead(GuiGraphics pPoseStack, Objective pObjective, CallbackInfo ci) {
        pPoseStack.pose().pushPose();
        Scoreboard module = Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
        if (module.isEnabled()) {
            pPoseStack.pose().translate(0.0F, module.down.getCurrentValue(), 0.0F);
        }
    }

    @Inject(
            method = {"displayScoreboardSidebar"},
            at = {@At("RETURN")}
    )
    public void hookScoreboardReturn(GuiGraphics pPoseStack, Objective pObjective, CallbackInfo ci) {
        pPoseStack.pose().popPose();
    }

    @Redirect(
            method = {"displayScoreboardSidebar"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"
            )
    )
    public int hookRenderScore(GuiGraphics instance, Font p_283343_, String p_281896_, int p_283569_, int p_283418_, int p_281560_, boolean p_282130_) {
        Scoreboard module = Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
        return module.isEnabled() && module.hideScore.getCurrentValue() ? 0 : instance.drawString(p_283343_, p_281896_, p_283569_, p_283418_, p_281560_);
    }

    @Redirect(
            method = {"displayScoreboardSidebar"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/scores/PlayerTeam;formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"
            )
    )
    public MutableComponent hookScoreboardName(Team pPlayerTeam, Component pPlayerName) {
        MutableComponent mutableComponent = PlayerTeam.formatNameForTeam(pPlayerTeam, pPlayerName);
        EventRenderScoreboard event = new EventRenderScoreboard(mutableComponent);
        Naven.getInstance().getEventManager().call(event);
        return (MutableComponent) event.getComponent();
    }

    @Redirect(
            method = {"displayScoreboardSidebar"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/scores/Objective;getDisplayName()Lnet/minecraft/network/chat/Component;"
            )
    )
    public Component hookScoreboardTitle(Objective instance) {
        Component component = instance.getDisplayName();
        EventRenderScoreboard event = new EventRenderScoreboard(component);
        Naven.getInstance().getEventManager().call(event);
        return event.getComponent();
    }

    @Inject(
            method = {"setTitle"},
            at = {@At("HEAD")},
            cancellable = true
    )
    public void hookTitle(Component pTitle, CallbackInfo ci) {
        EventSetTitle event = new EventSetTitle(EventType.TITLE, pTitle);
        Naven.getInstance().getEventManager().call(event);
        if (!event.isCancelled()) {
            this.title = event.getTitle();
            this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
            ci.cancel();
        }
    }

    @Inject(
            method = {"setSubtitle"},
            at = {@At("RETURN")},
            cancellable = true
    )
    public void hookSubtitle(Component pSubtitle, CallbackInfo ci) {
        EventSetTitle event = new EventSetTitle(EventType.SUBTITLE, pSubtitle);
        Naven.getInstance().getEventManager().call(event);
        if (!event.isCancelled()) {
            this.subtitle = event.getTitle();
            ci.cancel();
        }
    }

    @Inject(
            method = {"renderEffects"},
            at = {@At("HEAD")},
            cancellable = true
    )
    public void hookRenderEffects(GuiGraphics pPoseStack, CallbackInfo ci) {
        NoRender noRender = Naven.getInstance().getModuleManager().getModule(NoRender.class);
        if (noRender.isEnabled() && noRender.disableEffects.getCurrentValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    public void renderCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
        try {
            KawaseBlur.INGAME_BLUR.draw(Naven.getInstance().getModuleManager().getModule(PostProcess.class).getStrength());
            SkiaContext.draw((context) -> {
                Skia.save();
                Skia.scale((float) Minecraft.getInstance().getWindow().getGuiScale());
                Naven.getInstance().getEventManager().call(new EventRenderSkia());
                Skia.restore();
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
