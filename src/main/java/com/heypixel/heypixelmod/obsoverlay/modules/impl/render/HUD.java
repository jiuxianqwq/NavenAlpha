package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;

import java.awt.*;

@ModuleInfo(
        name = "HUD",
        cnName = "抬头显示器",
        description = "Displays information on your screen",
        category = Category.RENDER
)
public class HUD extends Module {
    public static final int headerColor = new Color(150, 45, 45, 255).getRGB();
    public static final int bodyColor = new Color(0, 0, 0, 120).getRGB();
    public static final int backgroundColor = new Color(0, 0, 0, 40).getRGB();
    public BooleanValue moduleToggleSound = ValueBuilder.create(this, "Module Toggle Sound").setDefaultBooleanValue(true).build().getBooleanValue();

    public FloatValue red1 = ValueBuilder.create(this, "Red 1")
            .setDefaultFloatValue(102.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();
    public FloatValue green1 = ValueBuilder.create(this, "Green 1")
            .setDefaultFloatValue(255.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();
    public FloatValue blue1 = ValueBuilder.create(this, "Blue 1")
            .setDefaultFloatValue(209.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();
    public FloatValue red2 = ValueBuilder.create(this, "Red 2")
            .setDefaultFloatValue(6.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();
    public FloatValue green2 = ValueBuilder.create(this, "Green 2")
            .setDefaultFloatValue(149.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();
    public FloatValue blue2 = ValueBuilder.create(this, "Blue 2")
            .setDefaultFloatValue(255.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .build()
            .getFloatValue();

    public BooleanValue chatBackground = ValueBuilder.create(this, "Chat Background")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue chatBlur = ValueBuilder.create(this, "Chat Blur")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue chatShadow = ValueBuilder.create(this, "Chat Shadow")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public FloatValue chatRadius = ValueBuilder.create(this, "Chat Radius")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();

    public BooleanValue chatInputBackground = ValueBuilder.create(this, "Chat Input Background")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private float chatMinX = Float.MAX_VALUE;
    private float chatMinY = Float.MAX_VALUE;
    private float chatMaxX = Float.MIN_VALUE;
    private float chatMaxY = Float.MIN_VALUE;
    private boolean hasChatBounds = false;
    private int chatRenderSequence = 0;
    private int chatDrawnSequence = -1;

    private float chatInputMinX = Float.MAX_VALUE;
    private float chatInputMinY = Float.MAX_VALUE;
    private float chatInputMaxX = Float.MIN_VALUE;
    private float chatInputMaxY = Float.MIN_VALUE;
    private boolean hasChatInputBounds = false;
    private int chatInputRenderSequence = 0;
    private int chatInputDrawnSequence = -1;
    private long chatInputOpenTime = 0;
    private long chatInputCloseTime = 0;
    private boolean chatInputClosing = false;


    public static Color getColor1() {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        return new Color(hud.red1.getCurrentValue() / 255.0f, hud.green1.getCurrentValue() / 255.0f, hud.blue1.getCurrentValue() / 255.0f);
    }

    public static Color getColor2() {
        HUD hud = Naven.getInstance().getModuleManager().getModule(HUD.class);
        return new Color(hud.red2.getCurrentValue() / 255.0f, hud.green2.getCurrentValue() / 255.0f, hud.blue2.getCurrentValue() / 255.0f);
    }

    public void beginChatRender() {
        if (!shouldRenderChat()) {
            hasChatBounds = false;
            return;
        }
        chatRenderSequence++;
        chatMinX = Float.MAX_VALUE;
        chatMinY = Float.MAX_VALUE;
        chatMaxX = Float.MIN_VALUE;
        chatMaxY = Float.MIN_VALUE;
        hasChatBounds = false;
    }

    public void trackChatBackground(int left, int top, int right, int bottom) {
        if (!shouldRenderChat()) {
            return;
        }
        float x1 = Math.min(left, right);
        float y1 = Math.min(top, bottom);
        float x2 = Math.max(left, right);
        float y2 = Math.max(top, bottom);
        chatMinX = Math.min(chatMinX, x1);
        chatMinY = Math.min(chatMinY, y1);
        chatMaxX = Math.max(chatMaxX, x2);
        chatMaxY = Math.max(chatMaxY, y2);
        hasChatBounds = true;
    }

    public void beginChatInputRender() {
        if (!shouldRenderChatInput()) {
            hasChatInputBounds = false;
            return;
        }
        chatInputClosing = false;
        chatInputRenderSequence++;
        chatInputMinX = Float.MAX_VALUE;
        chatInputMinY = Float.MAX_VALUE;
        chatInputMaxX = Float.MIN_VALUE;
        chatInputMaxY = Float.MIN_VALUE;
        hasChatInputBounds = false;
    }

    public void trackChatInputBackground(int left, int top, int right, int bottom) {
        if (!shouldRenderChatInput()) {
            return;
        }
        float x1 = Math.min(left, right);
        float y1 = Math.min(top, bottom);
        float x2 = Math.max(left, right);
        float y2 = Math.max(top, bottom);
        chatInputMinX = Math.min(chatInputMinX, x1);
        chatInputMinY = Math.min(chatInputMinY, y1);
        chatInputMaxX = Math.max(chatInputMaxX, x2);
        chatInputMaxY = Math.max(chatInputMaxY, y2);
        hasChatInputBounds = true;
    }

    public void resetChatInputAnimation() {
        chatInputOpenTime = System.currentTimeMillis();
        chatInputCloseTime = 0;
        chatInputClosing = false;
        chatInputDrawnSequence = -1;
        hasChatInputBounds = false;
    }

    public void beginChatInputClose() {
        if (!shouldRenderChatInput()) {
            return;
        }
        chatInputClosing = true;
        chatInputCloseTime = System.currentTimeMillis();
        chatInputDrawnSequence = -1;
    }

    @EventTarget
    public void onRenderSkia(EventRenderSkia event) {
        boolean renderChat = shouldRenderChat();
        boolean renderInput = shouldRenderChatInput();
        if (!renderChat && !renderInput) {
            return;
        }
        if (renderChat) {
            if (hasChatBounds && chatDrawnSequence != chatRenderSequence) {
                float padding = 4.0F;
                float minX = chatMinX - padding;
                float minY = chatMinY - padding;
                float maxX = chatMaxX + padding;
                float maxY = chatMaxY + padding;
                float width = maxX - minX;
                float height = maxY - minY;
                if (width > 0.0F && height > 0.0F) {
                    float radius = chatRadius.getCurrentValue();
                    if (chatShadow.getCurrentValue()) {
                        Skia.drawShadow(minX, minY, width, height, radius);
                    }
                    if (chatBlur.getCurrentValue()) {
                        Skia.drawRoundedBlur(minX, minY, width, height, radius);
                    }
                    Skia.drawRoundedRect(minX, minY, width, height, radius, new Color(0, 0, 0, 60));
                    chatDrawnSequence = chatRenderSequence;
                }
            }
        }

        if (renderInput) {
            if (!hasChatInputBounds) {
                return;
            }
            if (!chatInputClosing && chatInputDrawnSequence == chatInputRenderSequence) {
                return;
            }
            float inputPadding = 2.0F;
            float inputMinX = chatInputMinX - inputPadding;
            float inputMinY = chatInputMinY - inputPadding;
            float inputMaxX = chatInputMaxX + inputPadding;
            float inputMaxY = chatInputMaxY + inputPadding;
            float inputWidth = inputMaxX - inputMinX;
            float inputHeight = inputMaxY - inputMinY;
            if (inputWidth <= 0.0F || inputHeight <= 0.0F) {
                return;
            }
            long elapsed = System.currentTimeMillis() - (chatInputClosing ? chatInputCloseTime : chatInputOpenTime);
            float progress = Math.min(1.0f, elapsed / 300.0f);
            float eased = (float) (1 - Math.pow(1 - progress, 3));
            float factor = chatInputClosing ? (1.0F - eased) : eased;

            if (chatInputClosing && progress >= 1.0f) {
                chatInputClosing = false;
                hasChatInputBounds = false;
                return;
            }

            float startY = (float) mc.getWindow().getGuiScaledHeight();
            float animatedY = inputMinY + (startY - inputMinY) * (1 - factor);

            float radiusInput = chatRadius.getCurrentValue();
            int alpha = Math.min(255, Math.max(0, (int) (70.0F * factor)));
            Color backgroundColor = new Color(18, 18, 18, alpha);
            if (chatShadow.getCurrentValue()) {
                Skia.drawShadow(inputMinX, animatedY, inputWidth, inputHeight, radiusInput);
            }
            if (chatBlur.getCurrentValue()) {
                Skia.drawRoundedBlur(inputMinX, animatedY, inputWidth, inputHeight, radiusInput);
            }
            Skia.drawRoundedRect(inputMinX, animatedY, inputWidth, inputHeight, radiusInput, backgroundColor);
            chatInputDrawnSequence = chatInputRenderSequence;
        }
    }

    private boolean shouldRenderChat() {
        return this.isEnabled() && chatBackground.getCurrentValue();
    }

    private boolean shouldRenderChatInput() {
        return this.isEnabled() && chatInputBackground.getCurrentValue();
    }
}
