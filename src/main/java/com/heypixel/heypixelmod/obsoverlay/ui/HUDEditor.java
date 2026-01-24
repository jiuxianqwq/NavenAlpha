package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.PostProcess;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.shader.impl.KawaseBlur;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.context.SkiaContext;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueType;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HUDEditor extends Screen {
    private final List<DragValue> dragValues = new ArrayList<>();
    private DragValue draggingValue = null;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;

    public HUDEditor() {
        super(Component.literal("HUD Editor"));
    }

    @Override
    protected void init() {
        dragValues.clear();
        ModuleManager moduleManager = Naven.getInstance().getModuleManager();
        for (Module module : moduleManager.getModules()) {
            for (Value value : Naven.getInstance().getValueManager().getValues()) {
                if (value.getKey() == module && value.getValueType() == ValueType.DRAG) {
                    dragValues.add(value.getDragValue());
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
//        this.renderBackground(guiGraphics);
        KawaseBlur.GUI_BLUR.draw(Naven.getInstance().getModuleManager().getModule(PostProcess.class).getStrength());
        SkiaContext.draw((context) -> {
            Skia.save();
            Skia.scale((float) Minecraft.getInstance().getWindow().getGuiScale());
//            Skia.drawGuiBlur();
            for (DragValue dragValue : dragValues) {
                float x = dragValue.getX();
                float y = dragValue.getY();
                float width = dragValue.getWidth();
                float height = dragValue.getHeight();
                float renderWidth = width <= 0.0F ? 80.0F : width;
                float renderHeight = height <= 0.0F ? 20.0F : height;

                // Handle dragging
                if (draggingValue == dragValue) {
                    dragValue.setX(mouseX - dragOffsetX);
                    dragValue.setY(mouseY - dragOffsetY);
                    x = dragValue.getX();
                    y = dragValue.getY();
                }
                boolean isHovering = RenderUtils.isHoveringBound(mouseX, mouseY, x, y, renderWidth, renderHeight);
                if (isHovering) {
//                pose.pushPose();
//                RenderUtils.drawRoundedBorder(pose, x - 5, y - 5, width + 10, height + 10, 5, 5, new Color(255, 255, 255, 255).getRGB());
//                RenderUtils.drawRectBound(pose, x, y, width, height, new Color(255, 255, 255, 255).getRGB());
//                pose.popPose();
                    Skia.drawOutline(x - 5, y - 5, renderWidth + 10, renderHeight + 10, 5, 2, new Color(255, 255, 255));
                }

            }
            Skia.restore();
        });


        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            for (int i = dragValues.size() - 1; i >= 0; i--) { // Reverse order to select top-most element
                DragValue dragValue = dragValues.get(i);
                float width = dragValue.getWidth();
                float height = dragValue.getHeight();
                float renderWidth = width <= 0.0F ? 80.0F : width;
                float renderHeight = height <= 0.0F ? 20.0F : height;
                if (RenderUtils.isHoveringBound((int) mouseX, (int) mouseY, dragValue.getX(), dragValue.getY(), renderWidth, renderHeight)) {
                    draggingValue = dragValue;
                    dragOffsetX = (float) mouseX - dragValue.getX();
                    dragOffsetY = (float) mouseY - dragValue.getY();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingValue != null) {
            draggingValue = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingValue != null) {
            draggingValue.setX((float) mouseX - dragOffsetX);
            draggingValue.setY((float) mouseY - dragOffsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onClose() {
        Naven.getInstance().getFileManager().save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
