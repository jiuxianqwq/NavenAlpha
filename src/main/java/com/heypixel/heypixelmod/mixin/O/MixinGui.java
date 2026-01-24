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
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import io.github.humbleui.skija.FontMetrics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
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
        module.beginScoreboardRender();
        if (module.isEnabled()) {
            pPoseStack.pose().translate(module.xOffset.getCurrentValue(), module.down.getCurrentValue(), 0.0F);
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
        boolean hideScore = module.isEnabled() && module.hideScore.getCurrentValue();
        boolean useArrayListFont = module.isEnabled() && module.useArrayListFont.getCurrentValue();
        int resolvedColor = p_281560_;
        boolean hideRed = hideScore && isRedColor(resolvedColor);
        if (hideRed) {
            return 0;
        }
        float fontSize = p_283343_.lineHeight;
        if (useArrayListFont) {
            io.github.humbleui.skija.Font skiaFont = Fonts.getMiSans(fontSize);
            float textWidth = Skia.getStringWidth(p_281896_, skiaFont);
            FontMetrics metrics = skiaFont.getMetrics();
            float textHeight = metrics.getDescent() - metrics.getAscent();
            module.addScoreboardText(p_281896_, p_283569_, p_283418_, resolvedColor, fontSize);
            module.trackScoreboardString(textWidth, textHeight, p_283569_, p_283418_);
            return 0;
        } else {
            float textWidth = p_283343_.width(p_281896_);
            float textHeight = p_283343_.lineHeight;
            module.trackScoreboardString(textWidth, textHeight, p_283569_, p_283418_);
        }
        return instance.drawString(p_283343_, p_281896_, p_283569_, p_283418_, resolvedColor);
    }

    @Redirect(
            method = {"displayScoreboardSidebar"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"
            ),
            require = 0
    )
    public int hookRenderScoreComponent(GuiGraphics instance, Font font, Component component, int x, int y, int color, boolean shadow) {
        Scoreboard module = Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
        boolean hideScore = module.isEnabled() && module.hideScore.getCurrentValue();
        boolean useArrayListFont = module.isEnabled() && module.useArrayListFont.getCurrentValue();
        String text = component.getString();
        int resolvedColor = resolveColorFromStyle(component.getStyle(), color);
        boolean hideRed = hideScore && isRedColor(resolvedColor);
        if (hideRed) {
            return 0;
        }
        float fontSize = font.lineHeight;
        if (useArrayListFont) {
            io.github.humbleui.skija.Font skiaFont = Fonts.getMiSans(fontSize);
            float textWidth = Skia.getStringWidth(text, skiaFont);
            FontMetrics metrics = skiaFont.getMetrics();
            float textHeight = metrics.getDescent() - metrics.getAscent();
            module.addScoreboardText(text, x, y, resolvedColor, fontSize);
            module.trackScoreboardString(textWidth, textHeight, x, y);
            return 0;
        } else {
            float textWidth = font.width(component);
            float textHeight = font.lineHeight;
            module.trackScoreboardString(textWidth, textHeight, x, y);
        }
        return instance.drawString(font, component, x, y, resolvedColor, shadow);
    }

    @Redirect(
            method = {"displayScoreboardSidebar"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"
            ),
            require = 0
    )
    public int hookRenderScoreFormatted(GuiGraphics instance, Font font, FormattedCharSequence sequence, int x, int y, int color, boolean shadow) {
        Scoreboard module = Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
        boolean hideScore = module.isEnabled() && module.hideScore.getCurrentValue();
        boolean useArrayListFont = module.isEnabled() && module.useArrayListFont.getCurrentValue();
        String text = flattenFormatted(sequence);
        int resolvedColor = resolveColorFromSequence(sequence, color);
        boolean hideRed = hideScore && isRedColor(resolvedColor);
        if (hideRed) {
            return 0;
        }
        float fontSize = font.lineHeight;
        if (useArrayListFont) {
            io.github.humbleui.skija.Font skiaFont = Fonts.getMiSans(fontSize);
            float textWidth = Skia.getStringWidth(text, skiaFont);
            FontMetrics metrics = skiaFont.getMetrics();
            float textHeight = metrics.getDescent() - metrics.getAscent();
            module.addScoreboardText(text, x, y, resolvedColor, fontSize);
            module.trackScoreboardString(textWidth, textHeight, x, y);
            return 0;
        } else {
            float textWidth = font.width(sequence);
            float textHeight = font.lineHeight;
            module.trackScoreboardString(textWidth, textHeight, x, y);
        }
        return instance.drawString(font, sequence, x, y, resolvedColor, shadow);
    }

    private static String flattenFormatted(FormattedCharSequence sequence) {
        StringBuilder builder = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    private static int resolveColorFromSequence(FormattedCharSequence sequence, int fallbackColor) {
        int[] resolved = new int[]{fallbackColor};
        sequence.accept((index, style, codePoint) -> {
            if (style != null && style.getColor() != null) {
                resolved[0] = resolveColorFromStyle(style, fallbackColor);
                return false;
            }
            return true;
        });
        return resolved[0];
    }

    private static int resolveColorFromStyle(Style style, int fallbackColor) {
        if (style == null) {
            return fallbackColor;
        }
        TextColor textColor = style.getColor();
        if (textColor == null) {
            return fallbackColor;
        }
        int alpha = (fallbackColor >> 24) & 0xFF;
        return (alpha << 24) | (textColor.getValue() & 0xFFFFFF);
    }

    private static boolean isRedColor(int color) {
        int rgb = color & 0xFFFFFF;
        Integer red = ChatFormatting.RED.getColor();
        if (red != null && rgb == (red & 0xFFFFFF)) {
            return true;
        }
        Integer darkRed = ChatFormatting.DARK_RED.getColor();
        if (darkRed != null && rgb == (darkRed & 0xFFFFFF)) {
            return true;
        }
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r > 160 && g < 80 && b < 80;
    }

    @Redirect(
            method = {"displayScoreboardSidebar"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
            ),
            require = 0
    )
    public void hookScoreboardBackground(GuiGraphics instance, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor) {
        Scoreboard module = Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
        module.trackScoreboardBackground(pMinX, pMinY, pMaxX, pMaxY);
        if (!module.isEnabled()) {
            instance.fill(pMinX, pMinY, pMaxX, pMaxY, pColor);
        }
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
