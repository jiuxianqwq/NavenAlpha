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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.lang.reflect.Field;

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

    public BooleanValue betterGui = ValueBuilder.create(this, "Better GUI")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue betterGuiBlur = ValueBuilder.create(this, "Better GUI Blur")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> betterGui.getCurrentValue())
            .build()
            .getBooleanValue();

    public BooleanValue betterGuiShadow = ValueBuilder.create(this, "Better GUI Shadow")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> betterGui.getCurrentValue())
            .build()
            .getBooleanValue();

    public FloatValue betterGuiRadius = ValueBuilder.create(this, "Better GUI Radius")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(30.0F)
            .setVisibility(() -> betterGui.getCurrentValue())
            .build()
            .getFloatValue();

    public FloatValue betterGuiOpacity = ValueBuilder.create(this, "Better GUI Opacity")
            .setDefaultFloatValue(110.0F)
            .setFloatStep(5.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .setVisibility(() -> betterGui.getCurrentValue())
            .build()
            .getFloatValue();

    public FloatValue betterGuiSlotOpacity = ValueBuilder.create(this, "Better GUI Slot Opacity")
            .setDefaultFloatValue(35.0F)
            .setFloatStep(5.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .setVisibility(() -> betterGui.getCurrentValue())
            .build()
            .getFloatValue();

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

    private static Field leftPosField, topPosField, imageWidthField, imageHeightField, hoveredSlotField;
    private static boolean reflectionFailed = false;

    private void initReflection() {
        if (reflectionFailed || leftPosField != null) return;
        try {
            Class<?> clazz = AbstractContainerScreen.class;
            try {
                leftPosField = clazz.getDeclaredField("leftPos");
                topPosField = clazz.getDeclaredField("topPos");
                imageWidthField = clazz.getDeclaredField("imageWidth");
                imageHeightField = clazz.getDeclaredField("imageHeight");
            } catch (NoSuchFieldException e) {
                reflectionFailed = true;
                return;
            }
            leftPosField.setAccessible(true);
            topPosField.setAccessible(true);
            imageWidthField.setAccessible(true);
            imageHeightField.setAccessible(true);
            try {
                hoveredSlotField = clazz.getDeclaredField("hoveredSlot");
                hoveredSlotField.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
                hoveredSlotField = null;
            }
        } catch (Exception e) {
            reflectionFailed = true;
        }
    }

    @EventTarget
    public void onRenderSkia(EventRenderSkia event) {
        boolean renderChat = shouldRenderChat();
        boolean renderInput = shouldRenderChatInput();
        boolean renderBetterGui = betterGui.getCurrentValue() && mc.screen instanceof AbstractContainerScreen;

        if (!renderChat && !renderInput && !renderBetterGui) {
            return;
        }

        if (renderBetterGui) {
            initReflection();
            if (!reflectionFailed) {
                try {
                    AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.screen;
                    int leftPos = leftPosField.getInt(screen);
                    int topPos = topPosField.getInt(screen);
                    int imageWidth = imageWidthField.getInt(screen);
                    int imageHeight = imageHeightField.getInt(screen);

                    float x = (float) leftPos;
                    float y = (float) topPos;
                    float w = (float) imageWidth;
                    float h = (float) imageHeight;
                    float radius = betterGuiRadius.getCurrentValue();

                    if (betterGuiShadow.getCurrentValue()) {
                        Skia.drawShadow(x, y, w, h, radius);
                    }
                    if (betterGuiBlur.getCurrentValue()) {
                        Skia.drawRoundedBlur(x, y, w, h, radius);
                    }
                    Skia.drawRoundedRect(x, y, w, h, radius, new Color(18, 18, 18, (int) betterGuiOpacity.getCurrentValue()));

                    AbstractContainerMenu menu = screen.getMenu();
                    if (menu != null) {
                        Color slotColor = new Color(25, 25, 25, (int) betterGuiSlotOpacity.getCurrentValue());
                        for (Slot slot : menu.slots) {
                            float slotX = x + slot.x;
                            float slotY = y + slot.y;
                            Skia.drawRoundedRect(slotX, slotY, 16, 16, 4, slotColor);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
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

    public void renderBetterGuiItems(AbstractContainerScreen<?> screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (!betterGui.getCurrentValue()) {
            return;
        }
        initReflection();
        if (reflectionFailed) {
            return;
        }
        try {
            int leftPos = leftPosField.getInt(screen);
            int topPos = topPosField.getInt(screen);
            AbstractContainerMenu menu = screen.getMenu();
            if (menu == null) {
                return;
            }
            Slot hovered = null;
            float x = (float) leftPos;
            float y = (float) topPos;
            for (Slot slot : menu.slots) {
                float slotX = x + slot.x;
                float slotY = y + slot.y;
                if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                    hovered = slot;
                }
                if (slot.hasItem()) {
                    ItemStack stack = slot.getItem();
                    graphics.renderItem(stack, (int) slotX, (int) slotY);
                    graphics.renderItemDecorations(mc.font, stack, (int) slotX, (int) slotY);
                }
            }
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty()) {
                int drawX = mouseX - 8;
                int drawY = mouseY - 8;
                graphics.renderItem(carried, drawX, drawY);
                graphics.renderItemDecorations(mc.font, carried, drawX, drawY);
            }
            mc.renderBuffers().bufferSource().endBatch();
            if (hoveredSlotField != null) {
                hoveredSlotField.set(screen, hovered);
            }
        } catch (Exception ignored) {
        }
    }
}
