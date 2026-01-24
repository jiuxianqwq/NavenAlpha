package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
        name = "Scoreboard",
        cnName = "计分板",
        description = "Modifies the scoreboard",
        category = Category.RENDER
)
public class Scoreboard extends Module {
    public BooleanValue hideScore = ValueBuilder.create(this, "Hide Red Score").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue useArrayListFont = ValueBuilder.create(this, "Font").setDefaultBooleanValue(false).build().getBooleanValue();
    public FloatValue xOffset = ValueBuilder.create(this, "X Offset")
            .setDefaultFloatValue(0.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(-300.0F)
            .setMaxFloatValue(300.0F)
            .build()
            .getFloatValue();
    public FloatValue down = ValueBuilder.create(this, "Down")
            .setDefaultFloatValue(120.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(300.0F)
            .build()
            .getFloatValue();
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = Float.MIN_VALUE;
    private float maxY = Float.MIN_VALUE;
    private float baseMinX = Float.MAX_VALUE;
    private float baseMinY = Float.MAX_VALUE;
    private float baseMaxX = Float.MIN_VALUE;
    private float baseMaxY = Float.MIN_VALUE;
    private boolean hasBounds = false;
    private boolean hasBaseBounds = false;
    private int renderSequence = 0;
    private int drawnSequence = -1;
    private boolean syncingDragValue = false;
    private boolean hasPendingDrag = false;
    private float pendingDragX = 0.0F;
    private float pendingDragY = 0.0F;
    private boolean hasLastBaseBounds = false;
    private float lastBaseMinX = 0.0F;
    private float lastBaseMinY = 0.0F;
    private final List<TextEntry> textEntries = new ArrayList<>();

    public DragValue dragValue = ValueBuilder.create(this, "Position")
            .setDefaultX(0.0F)
            .setDefaultY(120.0F)
            .setOnUpdate(value -> {
                if (syncingDragValue) {
                    return;
                }
                DragValue drag = value.getDragValue();
                if (!hasBaseBounds) {
                    if (hasLastBaseBounds) {
                        syncingDragValue = true;
                        xOffset.setCurrentValue(drag.getX() - lastBaseMinX);
                        down.setCurrentValue(drag.getY() - lastBaseMinY);
                        syncingDragValue = false;
                        hasPendingDrag = false;
                    } else {
                        pendingDragX = drag.getX();
                        pendingDragY = drag.getY();
                        hasPendingDrag = true;
                    }
                    return;
                }
                syncingDragValue = true;
                xOffset.setCurrentValue(drag.getX() - baseMinX);
                down.setCurrentValue(drag.getY() - baseMinY);
                syncingDragValue = false;
            })
            .build()
            .getDragValue();

    public void beginScoreboardRender() {
        renderSequence++;
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;
        baseMinX = Float.MAX_VALUE;
        baseMinY = Float.MAX_VALUE;
        baseMaxX = Float.MIN_VALUE;
        baseMaxY = Float.MIN_VALUE;
        hasBounds = false;
        hasBaseBounds = false;
        textEntries.clear();
        if (!hasPendingDrag) {
            syncDragValueFromOffsets();
        }
    }

    private void applyPendingDragIfNeeded() {
        if (!hasPendingDrag || !hasBaseBounds) {
            return;
        }
        float currentOffsetX = xOffset.getCurrentValue();
        float currentOffsetY = down.getCurrentValue();
        float targetOffsetX = pendingDragX - baseMinX;
        float targetOffsetY = pendingDragY - baseMinY;
        float deltaX = targetOffsetX - currentOffsetX;
        float deltaY = targetOffsetY - currentOffsetY;
        syncingDragValue = true;
        xOffset.setCurrentValue(targetOffsetX);
        down.setCurrentValue(targetOffsetY);
        syncingDragValue = false;
        if (hasBounds) {
            minX += deltaX;
            maxX += deltaX;
            minY += deltaY;
            maxY += deltaY;
        }
        hasPendingDrag = false;
    }

    public void syncDragValueFromOffsets() {
        if (syncingDragValue) {
            return;
        }
        float targetX = xOffset.getCurrentValue();
        float targetY = down.getCurrentValue();
        if (hasBaseBounds) {
            targetX = baseMinX + targetX;
            targetY = baseMinY + targetY;
        } else if (hasLastBaseBounds) {
            targetX = lastBaseMinX + targetX;
            targetY = lastBaseMinY + targetY;
        }
        if (dragValue.getX() != targetX || dragValue.getY() != targetY) {
            syncingDragValue = true;
            dragValue.setPosition(targetX, targetY);
            syncingDragValue = false;
        }
    }

    public void trackScoreboardString(float textWidth, float textHeight, int x, int y) {
        if (!this.isEnabled()) {
            return;
        }
        float offsetX = this.xOffset.getCurrentValue();
        float offsetY = this.down.getCurrentValue();
        float baseLeft = x - 2.0F;
        float baseTop = y - 2.0F;
        float baseRight = x + textWidth + 2.0F;
        float baseBottom = y + textHeight + 2.0F;
        float left = baseLeft + offsetX;
        float top = baseTop + offsetY;
        float right = baseRight + offsetX;
        float bottom = baseBottom + offsetY;
        baseMinX = Math.min(baseMinX, baseLeft);
        baseMinY = Math.min(baseMinY, baseTop);
        baseMaxX = Math.max(baseMaxX, baseRight);
        baseMaxY = Math.max(baseMaxY, baseBottom);
        hasBaseBounds = true;
        hasLastBaseBounds = true;
        lastBaseMinX = baseMinX;
        lastBaseMinY = baseMinY;
        minX = Math.min(minX, left);
        minY = Math.min(minY, top);
        maxX = Math.max(maxX, right);
        maxY = Math.max(maxY, bottom);
        hasBounds = true;
        float width = maxX - minX;
        float height = maxY - minY;
        if (width > 0.0F && height > 0.0F) {
            dragValue.setWidth(width);
            dragValue.setHeight(height);
        }
        if (width > 0.0F && height > 0.0F) {
            syncingDragValue = true;
            dragValue.setPosition(minX, minY);
            syncingDragValue = false;
        }
    }

    public void addScoreboardText(String text, int x, int y, int color, float fontSize) {
        if (!this.isEnabled()) {
            return;
        }
        textEntries.add(new TextEntry(text, x, y, color, fontSize));
    }

    public void trackScoreboardBackground(int left, int top, int right, int bottom) {
        if (!this.isEnabled()) {
            return;
        }
        float offsetX = this.xOffset.getCurrentValue();
        float offsetY = this.down.getCurrentValue();
        float x1 = Math.min(left, right) + offsetX;
        float y1 = Math.min(top, bottom) + offsetY;
        float x2 = Math.max(left, right) + offsetX;
        float y2 = Math.max(top, bottom) + offsetY;
        float baseX1 = Math.min(left, right);
        float baseY1 = Math.min(top, bottom);
        float baseX2 = Math.max(left, right);
        float baseY2 = Math.max(top, bottom);
        baseMinX = Math.min(baseMinX, baseX1);
        baseMinY = Math.min(baseMinY, baseY1);
        baseMaxX = Math.max(baseMaxX, baseX2);
        baseMaxY = Math.max(baseMaxY, baseY2);
        hasBaseBounds = true;
        hasLastBaseBounds = true;
        lastBaseMinX = baseMinX;
        lastBaseMinY = baseMinY;
        minX = Math.min(minX, x1);
        minY = Math.min(minY, y1);
        maxX = Math.max(maxX, x2);
        maxY = Math.max(maxY, y2);
        hasBounds = true;
        float width = maxX - minX;
        float height = maxY - minY;
        if (width > 0.0F && height > 0.0F) {
            dragValue.setWidth(width);
            dragValue.setHeight(height);
        }
        if (width > 0.0F && height > 0.0F) {
            syncingDragValue = true;
            dragValue.setPosition(minX, minY);
            syncingDragValue = false;
        }
    }

    @EventTarget
    public void onRenderSkia(EventRenderSkia event) {
        if (!this.isEnabled()) {
            return;
        }
        if (!hasBounds || drawnSequence == renderSequence) {
            return;
        }
        applyPendingDragIfNeeded();
        float width = maxX - minX;
        float height = maxY - minY;
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        Skia.drawShadow(minX, minY, width, height, 5.0F);
        Skia.drawRoundedBlur(minX, minY, width, height, 5.0F);
        Skia.drawRoundedRect(minX, minY, width, height, 5.0F, new Color(0, 0, 0, 60));
        if (useArrayListFont.getCurrentValue()) {
            float offsetX = this.xOffset.getCurrentValue();
            float offsetY = this.down.getCurrentValue();
            for (TextEntry entry : textEntries) {
                Skia.drawText(entry.text, entry.x + offsetX, entry.y + offsetY, new Color(entry.color, true), Fonts.getMiSans(entry.fontSize));
            }
        }
        drawnSequence = renderSequence;
    }

    private static class TextEntry {
        private final String text;
        private final int x;
        private final int y;
        private final int color;
        private final float fontSize;

        private TextEntry(String text, int x, int y, int color, float fontSize) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.fontSize = fontSize;
        }
    }
}
