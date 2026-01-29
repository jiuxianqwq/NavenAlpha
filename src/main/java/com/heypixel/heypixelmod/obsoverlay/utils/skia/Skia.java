package com.heypixel.heypixelmod.obsoverlay.utils.skia;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.PostProcess;
import com.heypixel.heypixelmod.obsoverlay.utils.shader.impl.KawaseBlur;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.context.SkiaContext;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.image.ImageHelper;
import com.mojang.blaze3d.platform.Window;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Skia {

    public static final Minecraft mc = Minecraft.getInstance();
    private static final ImageHelper imageHelper = new ImageHelper();

    // Optimization: Pre-allocate filters
    private static final ImageFilter BLUR_1PX = ImageFilter.makeBlur(1.0F, 1.0F, FilterTileMode.DECAL);
    private static final ImageFilter BLUR_2_5PX = ImageFilter.makeBlur(2.5F, 2.5F, FilterTileMode.DECAL);

    // Batch rendering tasks
    private static final List<TextGlowTask> glowTasks = new ArrayList<>();
    
    // Helper class for batch rendering
    public static class SkiaEventHandler {
        @EventTarget(1)
        public void onShader(EventRenderSkia event) {
            renderGlows();
        }
    }

    private static class TextGlowTask {
        String text;
        float x, y;
        Color color;
        Font font;
        float blurRadius;
        boolean styled;

        public TextGlowTask(String text, float x, float y, Color color, Font font, float blurRadius, boolean styled) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.font = font;
            this.blurRadius = blurRadius;
            this.styled = styled;
        }
    }

    private static void renderGlows() {
        if (glowTasks.isEmpty()) return;

        Paint paint = new Paint();
        
        for (TextGlowTask task : glowTasks) {
            paint.setColor(task.color.getRGB());
            paint.setImageFilter(task.blurRadius > 1.5f ? BLUR_2_5PX : BLUR_1PX);
            
            if (task.styled) {
                drawStyledText(getCanvas(), task.text, task.x, task.y, task.font, paint);
            } else {
                getCanvas().drawString(task.text, task.x, task.y, task.font, paint);
            }
        }
        
        paint.close(); // Important to close the paint
        glowTasks.clear();
    }

    public static void flushGlows() {
        renderGlows();
    }

    private static final int[] COLOR_CODES = new int[]{
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFAA5500, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
    };

    public static void drawRect(float x, float y, float width, float height, Color color) {
        getCanvas().drawRect(Rect.makeXYWH(x, y, width, height), getPaint(color));
    }

    public static void drawCircle(float x, float y, float radius, Color color) {
        Paint paint = getPaint(color);
        getCanvas().drawCircle(x, y, radius, paint);
    }

    public static void drawCircle(float x, float y, float radius, float strokeWidth, Color color) {
        Paint paint = getPaint(color);
        paint.setMode(PaintMode.STROKE);
        paint.setStrokeWidth(strokeWidth);
        getCanvas().drawCircle(x, y, radius, paint);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        getCanvas().drawRRect(RRect.makeXYWH(x, y, width, height, radius), getPaint(color));
    }

    public static void drawRoundedRectVarying(float x, float y, float width, float height, float topLeft,
                                              float topRight, float bottomRight, float bottomLeft, Color color) {

        float[] corners = new float[]{topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft,
                bottomLeft};

        getCanvas().drawRRect(RRect.makeComplexXYWH(x, y, width, height, corners), getPaint(color));
    }

    public static void drawBlur(float x, float y, float width, float height) {
        Window window = Minecraft.getInstance().getWindow();
        Path path = new Path();
        path.addRect(Rect.makeXYWH(x, y, width, height));

        save();
        getCanvas().clipPath(path, ClipMode.INTERSECT, true);
        drawImage(KawaseBlur.INGAME_BLUR.getTexture(), 0, 0, window.getWidth() / (float) window.getGuiScale(), window.getHeight() / (float) window.getGuiScale(), 1F,
                SurfaceOrigin.BOTTOM_LEFT);
        restore();

    }

    public static void drawRoundedBlur(float x, float y, float width, float height, float radius) {
        Window window = Minecraft.getInstance().getWindow();
        Path path = new Path();
        path.addRRect(RRect.makeXYWH(x, y, width, height, radius));

        save();
        getCanvas().clipPath(path, ClipMode.INTERSECT, true);
        drawImage(KawaseBlur.INGAME_BLUR.getTexture(), 0, 0, window.getWidth() / (float) window.getGuiScale(), window.getHeight() / (float) window.getGuiScale(), 1F,
                SurfaceOrigin.BOTTOM_LEFT);
        restore();

    }

    public static void drawRoundedGuiBlur(float x, float y, float width, float height, float radius) {
        Window window = Minecraft.getInstance().getWindow();
        Path path = new Path();
        path.addRRect(RRect.makeXYWH(x, y, width, height, radius));

        save();
        getCanvas().clipPath(path, ClipMode.INTERSECT, true);
        drawImage(KawaseBlur.GUI_BLUR.getTexture(), 0, 0, window.getWidth() / (float) window.getGuiScale(), window.getHeight() / (float) window.getGuiScale(), 1F,
                SurfaceOrigin.BOTTOM_LEFT);
        restore();
    }

    public static void drawGuiBlur() {
        Window window = Minecraft.getInstance().getWindow();
        drawImage(KawaseBlur.GUI_BLUR.getTexture(), 0, 0, window.getWidth() / (float) window.getGuiScale(), window.getHeight() / (float) window.getGuiScale(), 1F,
                SurfaceOrigin.BOTTOM_LEFT);
    }

    public static void drawShadow(float x, float y, float width, float height, float radius) {

        Paint paint = getPaint(new Color(0, 0, 0, 120));

        paint.setImageFilter(BLUR_2_5PX);

        save();
        clip(x, y, width, height, radius, ClipMode.DIFFERENCE);
        getCanvas().drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
        restore();
    }

    public static void drawShadow(float x, float y, float width, float height, float radius, Color color) {

        Paint paint = getPaint(color);

        paint.setImageFilter(BLUR_2_5PX);

        save();
        clip(x, y, width, height, radius, ClipMode.DIFFERENCE);
        getCanvas().drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
        restore();
    }

    public static void drawOutline(float x, float y, float width, float height, float radius, float strokeWidth,
                                   Color color) {

        float halfStroke = strokeWidth / 2;

        Path path = new Path();
        path.addRRect(RRect.makeXYWH(x + halfStroke, y + halfStroke, width - strokeWidth, height - strokeWidth,
                radius - halfStroke));

        Paint paint = getPaint(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setMode(PaintMode.STROKE);

        getCanvas().drawPath(path, paint);
    }

    public static void drawImage(String path, float x, float y, float width, float height) {

        path = "/assets/heypixel/VcX6svVqmeT8/" + path;

        if (imageHelper.load(path)) {
            getCanvas().drawImageRect(imageHelper.get(path), Rect.makeXYWH(x, y, width, height));
        }
    }

    public static void drawImage(int textureId, float x, float y, float width, float height, float alpha,
                                 SurfaceOrigin origin) {

        if (imageHelper.load(textureId, width, height, origin)) {
            Paint paint = new Paint();
            paint.setAlpha((int) (255 * alpha));
            getCanvas().drawImageRect(imageHelper.get(textureId), Rect.makeXYWH(x, y, width, height), paint);
        }
    }

    public static void drawImage(int textureId, float x, float y, float width, float height, float alpha) {
        drawImage(textureId, x, y, width, height, alpha, SurfaceOrigin.TOP_LEFT);
    }

    public static void drawImage(File file, float x, float y, float width, float height) {
        if (imageHelper.load(file)) {
            getCanvas().drawImageRect(imageHelper.get(file.getName()), Rect.makeXYWH(x, y, width, height));
        }
    }

    public static void drawImage(int textureId, float x, float y, float width, float height, SurfaceOrigin origin) {

        if (imageHelper.load(textureId, width, height, origin)) {
            getCanvas().drawImageRect(imageHelper.get(textureId), Rect.makeXYWH(x, y, width, height));
        }
    }

    public static void drawImage(int textureId, float x, float y, float width, float height) {
        drawImage(textureId, x, y, width, height, SurfaceOrigin.TOP_LEFT);
    }

    public static void drawRoundedImage(int textureId, float x, float y, float width, float height, float radius) {

        Path path = new Path();
        path.addRRect(RRect.makeXYWH(x, y, width, height, radius));

        save();
        getCanvas().clipPath(path, ClipMode.INTERSECT, true);
        drawImage(textureId, x, y, width, height);
        restore();
    }

    public static void drawRoundedImage(String filePath, float x, float y, float width, float height, float radius) {

        Path path = new Path();
        path.addRRect(RRect.makeXYWH(x, y, width, height, radius));

        save();
        getCanvas().clipPath(path, ClipMode.INTERSECT, true);
        drawImage(filePath, x, y, width, height);
        restore();
    }

    public static void drawRoundedImage(File file, float x, float y, float width, float height, float radius) {

        Path path = new Path();
        path.addRRect(RRect.makeXYWH(x, y, width, height, radius));

        save();
        getCanvas().clipPath(path, ClipMode.INTERSECT, true);
        drawImage(file, x, y, width, height);
        restore();
    }

    public static void drawRoundedImage(int textureId, float x, float y, float width, float height, float radius,
                                        float alpha, SurfaceOrigin origin) {
        Path path = new Path();
        path.addRRect(RRect.makeXYWH(x, y, width, height, radius));

        save();
        getCanvas().clipPath(path, ClipMode.INTERSECT, true);
        drawImage(textureId, x, y, width, height, alpha, origin);
        restore();
    }

    public static void drawRoundedImage(int textureId, float x, float y, float width, float height, float radius,
                                        float alpha) {
        drawRoundedImage(textureId, x, y, width, height, radius, alpha, SurfaceOrigin.TOP_LEFT);
    }

    public static void drawPlayerHead(AbstractClientPlayer player, float x, float y, float width, float height, float radius) {
        if (player == null) return;
        drawPlayerHead(player.getSkinTextureLocation(), x, y, width, height, radius);
    }

    public static void drawPlayerHead(ResourceLocation skinLoc, float x, float y, float width, float height, float radius) {
        if (skinLoc == null) return;
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(skinLoc);
        texture.bind();
        int textureId = texture.getId();

        if (textureId != -1) {
            if (imageHelper.load(textureId, 64, 64, SurfaceOrigin.TOP_LEFT)) {

                Path path = new Path();
                path.addRRect(RRect.makeXYWH(x, y, width, height, radius));

                Rect srcRectHead = Rect.makeXYWH(8, 8, 8, 8);
                Rect srcRectOverlay = Rect.makeXYWH(40, 8, 8, 8);

                Rect dstRect = Rect.makeXYWH(x, y, width, height);

                save();
                getCanvas().clipPath(path, ClipMode.INTERSECT, true);

                Image skinImage = imageHelper.get(textureId);

                if (skinImage != null) {
                    getCanvas().drawImageRect(skinImage, srcRectHead, dstRect, null, false);
                    getCanvas().drawImageRect(skinImage, srcRectOverlay, dstRect, null, false);
                }

                restore();
            }
        }
    }

    public static void drawSkin(AbstractClientPlayer player, float x, float y, float scale) {
        if (player == null) return;
        var skiaPath = player.getSkinTextureLocation().getPath();
        if (imageHelper.load(skiaPath)) {

            Rect head = Rect.makeXYWH(8, 8, 8, 8);
            Rect headLayer = Rect.makeXYWH(40, 8, 8, 8);
            Rect body = Rect.makeXYWH(20, 20, 8, 12);
            Rect bodyLayer = Rect.makeXYWH(20, 36, 8, 12);
            Rect leftArm = Rect.makeXYWH(36, 52, 4, 12);
            Rect leftArmLayer = Rect.makeXYWH(52, 52, 4, 12);
            Rect rightArm = Rect.makeXYWH(44, 20, 4, 12);
            Rect rightArmLayer = Rect.makeXYWH(44, 36, 4, 12);
            Rect leftLeg = Rect.makeXYWH(20, 52, 4, 12);
            Rect leftLegLayer = Rect.makeXYWH(4, 52, 4, 12);
            Rect rightLeg = Rect.makeXYWH(4, 20, 4, 12);
            Rect rightLegLayer = Rect.makeXYWH(4, 36, 4, 12);

            save();
            scale(x, y, scale);
            getCanvas().drawImageRect(imageHelper.get(skiaPath), head,
                    Rect.makeXYWH(x + leftArm.getWidth(), y, head.getWidth(), head.getHeight()), null, false);
            getCanvas().drawImageRect(imageHelper.get(skiaPath), headLayer,
                    Rect.makeXYWH(x + leftArm.getWidth(), y, headLayer.getWidth(), headLayer.getHeight()), null, false);
            getCanvas().drawImageRect(imageHelper.get(skiaPath), body,
                    Rect.makeXYWH(x + leftArm.getWidth(), y + head.getHeight(), body.getWidth(), body.getHeight()),
                    null, false);
            getCanvas().drawImageRect(imageHelper.get(skiaPath), bodyLayer, Rect.makeXYWH(x + leftArm.getWidth(),
                    y + headLayer.getHeight(), bodyLayer.getWidth(), bodyLayer.getHeight()), null, false);
            getCanvas().drawImageRect(imageHelper.get(skiaPath), leftArm,
                    Rect.makeXYWH(x, y + head.getHeight(), leftArm.getWidth(), leftArm.getHeight()), null, false);
            getCanvas().drawImageRect(imageHelper.get(skiaPath), leftArmLayer,
                    Rect.makeXYWH(x, y + headLayer.getHeight(), leftArmLayer.getWidth(), leftArmLayer.getHeight()),
                    null, false);
            getCanvas().drawImageRect(imageHelper.get(skiaPath), rightArm,
                    Rect.makeXYWH(x + leftArm.getWidth() + body.getWidth(), y + head.getHeight(), rightArm.getWidth(),
                            rightArm.getHeight()),
                    null, false);
            getCanvas().drawImageRect(imageHelper.get(skiaPath), rightArmLayer,
                    Rect.makeXYWH(x + leftArmLayer.getWidth() + bodyLayer.getWidth(), y + headLayer.getHeight(),
                            rightArmLayer.getWidth(), rightArmLayer.getHeight()),
                    null, false);
            getCanvas().drawImageRect(
                    imageHelper.get(skiaPath), leftLeg, Rect.makeXYWH(x + leftArm.getWidth(),
                            y + head.getHeight() + body.getHeight(), leftLeg.getWidth(), leftLeg.getHeight()),
                    null, false);
            getCanvas().drawImageRect(imageHelper.get(skiaPath), leftLegLayer,
                    Rect.makeXYWH(x + leftArmLayer.getWidth(), y + headLayer.getHeight() + bodyLayer.getHeight(),
                            leftLegLayer.getWidth(), leftLegLayer.getHeight()),
                    null, false);
            getCanvas()
                    .drawImageRect(imageHelper.get(skiaPath), rightLeg,
                            Rect.makeXYWH(x + leftArm.getWidth() + leftLeg.getWidth(),
                                    y + head.getHeight() + body.getHeight(), rightLeg.getWidth(), rightLeg.getHeight()),
                            null, false);
            getCanvas().drawImageRect(imageHelper.get(skiaPath), rightLegLayer,
                    Rect.makeXYWH(x + leftArmLayer.getWidth() + leftLegLayer.getWidth(),
                            y + headLayer.getHeight() + bodyLayer.getHeight(), rightLegLayer.getWidth(),
                            rightLegLayer.getHeight()),
                    null, false);

            restore();
        }
    }

    public static void drawArc(float x, float y, float radius, float startAngle, float endAngle, float strokeWidth,
                               Color color) {

        Paint paint = getPaint(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setMode(PaintMode.STROKE);

        getCanvas().drawArc(x - radius, y - radius, x + radius, y + radius, startAngle - 90, endAngle, false, paint);
    }

    public static void drawLine(float x, float y, float endX, float endY, float width, Color color) {

        Paint paint = getPaint(color);

        paint.setStroke(true);
        paint.setStrokeWidth(width);
        paint.setAntiAlias(true);

        getCanvas().drawLine(x, y, endX, endY, paint);
    }

    public static void drawGradientRoundedRect(float x, float y, float width, float height, float radius, Color color1,
                                               Color color2) {

        long currentTime = System.nanoTime();
        double speed = 0.0000000006;
        double tick = (currentTime * speed) % (2 * Math.PI);
        float max = Math.max(width, height);

        Path path = new Path();

        path.addRRect(RRect.makeXYWH(x, y, width, height, radius));

        float startX = x + width / 2 - (max / 2) * (float) Math.cos(tick);
        float startY = y + height / 2 - (max / 2) * (float) Math.sin(tick);
        float endX = x + width / 2 + (max / 2) * (float) Math.cos(tick);
        float endY = y + height / 2 + (max / 2) * (float) Math.sin(tick);

        int skColor1 = io.github.humbleui.skija.Color.makeARGB(color1.getAlpha(), color1.getRed(), color1.getGreen(),
                color1.getBlue());
        int skColor2 = io.github.humbleui.skija.Color.makeARGB(color2.getAlpha(), color2.getRed(), color2.getGreen(),
                color2.getBlue());

        int skColorMid = io.github.humbleui.skija.Color.makeARGB(color1.getAlpha(),
                (color1.getRed() + color2.getRed()) / 2, (color1.getGreen() + color2.getGreen()) / 2,
                (color1.getBlue() + color2.getBlue()) / 2);

        Paint paint = new Paint();

        paint.setShader(Shader.makeLinearGradient(new Point(startX, startY), new Point(endX, endY),
                new int[]{skColor1, skColorMid, skColor2}, new float[]{0, 0.5f, 1}));

        getCanvas().drawPath(path, paint);
    }

    public static void clipPath(Path path, ClipMode mode, boolean arg) {
        getCanvas().clipPath(path, mode, arg);
    }

    public static void clipPath(Path path) {
        getCanvas().clipPath(path, ClipMode.INTERSECT, true);
    }

    public static void clip(float x, float y, float width, float height, float radius, ClipMode mode) {

        Path path = new Path();

        path.addRRect(RRect.makeXYWH(x, y, width, height, radius));
        clipPath(path, mode, true);
    }

    public static void clip(float x, float y, float width, float height, float topLeft, float topRight,
                            float bottomRight, float bottomLeft) {

        float[] corners = new float[]{topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft,
                bottomLeft};

        Path path = new Path();

        path.addRRect(RRect.makeComplexXYWH(x, y, width, height, corners));
        clipPath(path, ClipMode.INTERSECT, true);
    }

    public static void clip(float x, float y, float width, float height, float radius) {
        clip(x, y, width, height, radius, ClipMode.INTERSECT);
    }

    private static int getColorIndex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }

    public static float getStringWidth(String text, Font font) {
        if (text == null || text.isEmpty()) return 0;

        float width = 0;
        StringBuilder cleanText = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
            } else {
                cleanText.append(c);
            }
        }

        return font.measureTextWidth(cleanText.toString());
    }

    private static void drawStyledText(Canvas canvas, String text, float x, float y, Font font, Paint basePaint) {
        float currentX = x;
        int originalColor = basePaint.getColor(); // 记录初始颜色

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length()) {
                char colorCode = text.charAt(i + 1);
                int colorIndex = getColorIndex(colorCode);

                if (colorIndex != -1) {
                    basePaint.setColor(COLOR_CODES[colorIndex]);
                } else if (colorCode == 'r') {
                    basePaint.setColor(originalColor);
                }

                i++;
                continue;
            }

            int nextSection = i;
            StringBuilder sb = new StringBuilder();
            while (nextSection < text.length()) {
                char nc = text.charAt(nextSection);
                if (nc == '§' && nextSection + 1 < text.length()) {
                    break;
                }
                sb.append(nc);
                nextSection++;
            }

            String segment = sb.toString();

            canvas.drawString(segment, currentX, y, font, basePaint);

            currentX += font.measureTextWidth(segment);

            i = nextSection - 1;
        }

        basePaint.setColor(originalColor);
    }

    public static void drawText(String text, float x, float y, Color color, Font font) {
        Rect bounds = font.measureText(text.replaceAll("§.", ""));
        float bx = x - bounds.getLeft();
        float by = y - bounds.getTop();

        // Queue blur task
        if (PostProcess.isGlowEnabled()) {
            glowTasks.add(new TextGlowTask(text, bx, by, color, font, 1.0F, true));
        }

        // Draw main text immediately
        Paint mainPaint = getPaint(color);
        drawStyledText(getCanvas(), text, bx, by, font, mainPaint);
        mainPaint.close();
    }

    public static void drawCenteredText(String text, float x, float y, Color color, Font font) {
        Rect bounds = font.measureText(text);
        float bx = x - bounds.getLeft();
        float by = y - bounds.getTop();
        
        // Queue blur task (simple render mode)
        if (PostProcess.isGlowEnabled()) {
            glowTasks.add(new TextGlowTask(text, bx, by, color, font, 1.0F, false));
        }
        
        getCanvas().drawString(text, bx, by, font, getPaint(color));
        getCanvas().drawString(text, x - bounds.getLeft() - (bounds.getWidth() / 2), y - bounds.getTop(), font,
                getPaint(color));
    }

    public static void drawHeightCenteredText(String text, float x, float y, Color color, Font font) {

        FontMetrics metrics = font.getMetrics();
        Rect bounds = font.measureText(text);
        
        float textCenterY = y + (metrics.getAscent() - metrics.getDescent()) / 2 - metrics.getAscent();
        float drawX = x - bounds.getLeft();

        // Queue blur task (simple)
        if (PostProcess.isGlowEnabled()) {
            glowTasks.add(new TextGlowTask(text, drawX, textCenterY, color, font, 1.0F, false));
        }

        getCanvas().drawString(text, drawX, textCenterY, font, getPaint(color));
    }

    public static void drawFullCenteredText(String text, float x, float y, Color color, Font font) {
        Rect bounds = font.measureText(text);
        FontMetrics metrics = font.getMetrics();

        float textCenterX = x - bounds.getLeft() - (bounds.getWidth() / 2);
        float textCenterY = y + (metrics.getAscent() - metrics.getDescent()) / 2 - metrics.getAscent();
        
        // Queue blur task (simple)
        if (PostProcess.isGlowEnabled()) {
            glowTasks.add(new TextGlowTask(text, textCenterX, textCenterY, color, font, 1.0F, false));
        }

        getCanvas().drawString(text, textCenterX, textCenterY, font, getPaint(color));
    }

    public static Rect getTextBounds(String text, Font font) {
        return font.measureText(text);
    }

    public static void drawTextShadow(String text, float x, float y, Color color, Font font) {
        Rect bounds = font.measureText(text);
        float bx = x - bounds.getLeft();
        float by = y - bounds.getTop();
        
        // Only queue blur task (simple), 2.5F radius
        if (PostProcess.isGlowEnabled()) {
            glowTasks.add(new TextGlowTask(text, bx, by, color, font, 2.5F, false));
        }
    }

    public static String getLimitText(String text, Font font, float width) {

        boolean isInRange = false;
        boolean isRemoved = false;

        while (!isInRange) {

            if (getTextBounds(text, font).getWidth() > width - getTextBounds("...", font).getWidth()) {
                text = text.substring(0, text.length() - 1);
                isRemoved = true;
            } else {
                isInRange = true;
            }
        }

        return text + (isRemoved ? "..." : "");
    }

    public static Paint getPaint(Color color) {
        Paint paint = new Paint();
        paint.setARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
        return paint;
    }

    public static void save() {
        getCanvas().save();
    }

    public static void restore() {
        getCanvas().restore();
    }

    public static void scale(float scale) {
        getCanvas().scale(scale, scale);
    }

    public static void scale(float x, float y, float scale) {
        getCanvas().translate(x, y);
        getCanvas().scale(scale, scale);
        getCanvas().translate(-x, -y);
    }

    public static void scale(float x, float y, float width, float height, float scale) {

        float centerX = x + width / 2;
        float centerY = y + height / 2;

        getCanvas().translate(centerX, centerY);
        getCanvas().scale(scale, scale);
        getCanvas().translate(-centerX, -centerY);
    }

    public static void translate(float x, float y) {
        getCanvas().translate(x, y);
    }

    public static void rotate(float x, float y, float width, float height, float rotate) {

        float centerX = x + width / 2;
        float centerY = y + height / 2;

        getCanvas().translate(centerX, centerY);
        getCanvas().rotate(rotate);
        getCanvas().translate(-centerX, -centerY);
    }

    public static void setAlpha(int alpha) {

        Paint paint = new Paint();
        paint.setAlpha(alpha);

        getCanvas().saveLayer(null, paint);
    }

    public static Canvas getCanvas() {
        return SkiaContext.getCanvas();
    }

    public static ImageHelper getImageHelper() {
        return imageHelper;
    }


    public static void drawSprite(TextureAtlasSprite sprite, float x, float y, float width, float height) {
        if (sprite == null) return;

        ResourceLocation atlasLoc = sprite.atlasLocation();
        AbstractTexture texture = mc.getTextureManager().getTexture(atlasLoc);
        int textureId = texture.getId();
        drawImage(textureId, x, y, width, height);
    }

    public static void drawEffectIcon(MobEffect effect, float x, float y, float width, float height) {// 1. 获取效果的注册名 (例如 "minecraft:speed")
        ResourceLocation registryName = ForgeRegistries.MOB_EFFECTS.getKey(effect);

        if (registryName == null) {
            return;
        }

        ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
                registryName.getNamespace(),
                "textures/mob_effect/" + registryName.getPath() + ".png"
        );

        AbstractTexture texture = mc.getTextureManager().getTexture(textureLocation);
        int textureId = texture.getId();
        drawImage(textureId, x, y, width, height);
    }
}
