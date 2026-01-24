package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ModuleInfo(
        name = "DynamicIsland",
        cnName = "灵动岛",
        description = "",
        category = Category.RENDER
)
public class DynamicIslandHud extends Module {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static volatile Component capturedTabHeader;
    private static volatile Component capturedTabFooter;
    private static volatile List<PlayerInfo> capturedTabEntries = List.of();

    private static final class Size {
        static final float BASE_W = 65f;
        static final float BASE_H = 19f;
        static final float EXPANDED_W = 90f;
        static final float EXPANDED_H = 25f;
        static final float ELEMENT_SPACING = 20f;
        static final float ELEMENT_WIDTH = 50f;
        static final float LOGO_FONT_SIZE = 12f;
        static final float INFO_FONT_SIZE = 10f;
        static final Color INVENTORY_BG_COLOR = new Color(18, 18, 18, 70);

        static final float TAB_PLAYER_HEIGHT = 14f;
        static final float TAB_PADDING = 8f;
        static final float TAB_HEADER_Y = 12f;
        static final float TAB_LIST_Y = 30f;
        static final int TAB_COLUMNS = 1;
    }

    private static final class Timing {
        static final long EXPAND = 300L;
        static final long DISPLAY = 1500L;
        static final long COLLAPSE_1 = 300L;
        static final long COLLAPSE_2 = 400L;
        static final long TOTAL = EXPAND + DISPLAY + COLLAPSE_1 + COLLAPSE_2;
        static final long TAB_TRANSITION = 450L;
    }

    private enum Phase {
        IDLE,
        EXPANDING,
        DISPLAY,
        COLLAPSE_1,
        COLLAPSE_2,
        TAB_EXPAND,
        TAB_DISPLAY,
        TAB_COLLAPSE
    }

    private final BooleanValue enableBloom = ValueBuilder.create(this, "Bloom")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final BooleanValue blur = ValueBuilder.create(this, "Blur")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final FloatValue radius = ValueBuilder.create(this, "Radius")
            .setDefaultFloatValue(6.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(15.0f)
            .setFloatStep(1.0f)
            .build()
            .getFloatValue();
    private final FloatValue yOffset = ValueBuilder.create(this, "YOffset")
            .setDefaultFloatValue(8.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(200.0f)
            .setFloatStep(1.0f)
            .build()
            .getFloatValue();

    private static ToggleInfo currentToggle;
    private static ToggleInfo pendingToggle;
    private long toggleStartTime = -1L;
    private long tabStartTime = -1L;
    private float targetExpandedWidth = Size.EXPANDED_W;

    private Phase phase = Phase.IDLE;
    private float progress;
    private float blurOpacity = 1f;
    private float animX;
    private float animY;
    private float animW;
    private float animH;
    private float tabMergeProgress;

    private List<PlayerInfo> playerList;
    private float tabTargetW;
    private float tabTargetH;

    public static void onModuleToggle(Module module, boolean enabled) {
        if (module instanceof DynamicIslandHud) return;
        // Prevent re-triggering animation for the same module and state if it's currently displaying
        if (currentToggle != null && currentToggle.name.equals(module.getName()) && currentToggle.enabled == enabled) {
            // Refresh timer by creating a new pending toggle which will reset the timer in processToggle
            // This avoids accessing non-static toggleStartTime from static context
            pendingToggle = new ToggleInfo(module.getName(), enabled);
            return;
        }
        pendingToggle = new ToggleInfo(module.getName(), enabled);
    }

    @EventTarget
    public void onRenderSkia(EventRenderSkia event) {
        update();

        renderContent();

        if (isTabPhase()) {
            renderCapturedTab();
        }
    }

    @EventTarget
    public void onRenderTab(EventRenderTabOverlay e) {
        if (e.getType() == EventType.HEADER) {
            capturedTabHeader = e.getComponent();
        } else if (e.getType() == EventType.FOOTER) {
            capturedTabFooter = e.getComponent();
        }
    }

    private void update() {
        handleTabInput();
        processToggle();
        calculateState();
    }

    private void handleTabInput() {
        boolean tabPressed = mc.options.keyPlayerList.isDown();

        if (tabPressed && !isTabPhase()) {
            tabStartTime = System.currentTimeMillis();
            phase = Phase.TAB_EXPAND;
            updatePlayerList();
        } else if (tabPressed && isTabPhase()) {
            updatePlayerList();
        } else if (phase == Phase.TAB_DISPLAY || phase == Phase.TAB_EXPAND) {
            tabStartTime = System.currentTimeMillis();
            phase = Phase.TAB_COLLAPSE;
        }
    }

    private void updatePlayerList() {
        if (mc.getConnection() != null) {
            List<PlayerInfo> source = capturedTabEntries.isEmpty() ? List.copyOf(mc.getConnection().getOnlinePlayers()) : capturedTabEntries;
            playerList = source.stream()
                    .sorted(Comparator.comparingInt((PlayerInfo e) -> e.getGameMode() == GameType.SPECTATOR ? 1 : 0)
                            .thenComparing(e -> e.getProfile().getName()))
                    .limit(80)
                    .collect(Collectors.toList());

            int count = playerList.size();
            int rows = (int) Math.ceil((double) count / Size.TAB_COLUMNS);

            tabTargetW = Size.BASE_W + 2f * (Size.ELEMENT_WIDTH + Size.ELEMENT_SPACING);
            float innerW = Math.max(0f, tabTargetW - Size.TAB_PADDING * 2f);
            Font font = Fonts.getMiSans(Size.INFO_FONT_SIZE);
            float fontH = getFontHeight(font);

            int headerLines = 1;
            if (capturedTabHeader != null && !capturedTabHeader.getString().isEmpty()) {
                headerLines = wrapLines(capturedTabHeader.getString(), innerW, font).size();
            }

            int footerLines = 0;
            if (capturedTabFooter != null && !capturedTabFooter.getString().isEmpty()) {
                footerLines = wrapLines(capturedTabFooter.getString(), innerW, font).size();
            }

            float headerH = headerLines * fontH;
            float footerH = footerLines * fontH;
            float listY = Math.max(Size.TAB_LIST_Y, Size.TAB_HEADER_Y + headerH + 8f);

            tabTargetH = listY + rows * Size.TAB_PLAYER_HEIGHT + Size.TAB_PADDING + (footerLines > 0 ? (footerH + 8f) : 0f);
            tabTargetH = Math.max(tabTargetH, Size.BASE_H * 2f);
        }
    }

    private void processToggle() {
        if (isTabPhase()) return;

        if (pendingToggle != null) {
            float newTargetW = calculateExpandedWidth(pendingToggle);
            
            // Smart State Transition
            if (phase == Phase.EXPANDING || phase == Phase.DISPLAY) {
                // If expanding or displaying, just update content and target width.
                // Maintain current timing to avoid "restart" glitch.
                // For DISPLAY, we reset time to extend the display period.
                if (phase == Phase.DISPLAY) {
                    toggleStartTime = System.currentTimeMillis() - Timing.EXPAND;
                }
                currentToggle = pendingToggle;
                targetExpandedWidth = newTargetW;
            } else if (phase == Phase.COLLAPSE_1 || phase == Phase.COLLAPSE_2) {
                // If collapsing, we need to reverse to expanding.
                // Calculate "equivalent progress" to match current width and avoid jump.
                // currentW = lerp(p, BASE, TARGET) => p = (currentW - BASE) / (TARGET - BASE)
                float currentW = animW;
                float baseW = Size.BASE_W;
                float p = clamp((currentW - baseW) / (newTargetW - baseW));
                
                // Inverse EaseOut: t = 1 - cbrt(1 - p)
                // We want to set startTime such that easeOut(elapsed/TOTAL) == p
                float t = 1f - (float) Math.cbrt(1f - p);
                toggleStartTime = System.currentTimeMillis() - (long)(t * Timing.EXPAND);
                
                currentToggle = pendingToggle;
                targetExpandedWidth = newTargetW;
                // Phase will be set to EXPANDING in calculateState based on the rewound time
            } else {
                // IDLE or other
                currentToggle = pendingToggle;
                toggleStartTime = System.currentTimeMillis();
                targetExpandedWidth = newTargetW;
            }
            pendingToggle = null;
        } else if (currentToggle != null && elapsedToggle() >= Timing.TOTAL) {
            currentToggle = null;
            toggleStartTime = -1L;
        }
    }

    private void calculateState() {
        long dt = elapsedToggle();
        long tabDt = elapsedTab();
        float targetAnimW = animW; // Default to keeping current

        if (phase == Phase.TAB_EXPAND) {
            if (tabDt < Timing.TAB_TRANSITION) {
                float t = clamp(tabDt / (float) Timing.TAB_TRANSITION);
                float mergeP = easeInOut(t);
                float expandP = easeInOut(t);
                tabMergeProgress = mergeP;
                setPhase(Phase.TAB_EXPAND, expandP,
                        lerp(mergeP, Size.BASE_W, tabTargetW),
                        lerp(expandP, Size.BASE_H, tabTargetH),
                        1f);
                targetAnimW = animW; // Tab phase handles interpolation internally
            } else {
                tabMergeProgress = 1f;
                setPhase(Phase.TAB_DISPLAY, 1f, tabTargetW, tabTargetH, 1f);
                targetAnimW = tabTargetW;
            }
        } else if (phase == Phase.TAB_COLLAPSE) {
            if (tabDt < Timing.TAB_TRANSITION) {
                float t = clamp(tabDt / (float) Timing.TAB_TRANSITION);
                float mergeP = easeInOut(1f - t);
                float expandP = easeInOut(1f - t);
                tabMergeProgress = mergeP;
                setPhase(Phase.TAB_COLLAPSE, expandP,
                        lerp(mergeP, Size.BASE_W, tabTargetW),
                        lerp(expandP, Size.BASE_H, tabTargetH),
                        1f);
                targetAnimW = animW;
            } else {
                tabMergeProgress = 0f;
                setPhase(Phase.IDLE, 0f, Size.BASE_W, Size.BASE_H, 1f);
                tabStartTime = -1L;
                targetAnimW = Size.BASE_W;
            }
        } else if (phase == Phase.TAB_DISPLAY) {
            tabMergeProgress = 1f;
            setPhase(Phase.TAB_DISPLAY, 1f, tabTargetW, tabTargetH, 1f);
            targetAnimW = tabTargetW;
        } else {
            if (currentToggle == null && toggleStartTime == -1L) {
                setPhase(Phase.IDLE, 0f, Size.BASE_W, Size.BASE_H, 1f);
                targetAnimW = Size.BASE_W;
            } else if (dt < Timing.EXPAND) {
                float p = easeOut(dt / (float) Timing.EXPAND);
                setPhase(Phase.EXPANDING, p,
                        lerp(p, Size.BASE_W, targetExpandedWidth),
                        lerp(p, Size.BASE_H, Size.EXPANDED_H),
                        1f);
                targetAnimW = lerp(p, Size.BASE_W, targetExpandedWidth);
            } else if (dt < Timing.EXPAND + Timing.DISPLAY) {
                float p = (dt - Timing.EXPAND) / (float) Timing.DISPLAY;
                setPhase(Phase.DISPLAY, p, targetExpandedWidth, Size.EXPANDED_H, 1f);
                targetAnimW = targetExpandedWidth;
            } else if (dt < Timing.EXPAND + Timing.DISPLAY + Timing.COLLAPSE_1) {
                float p = easeOut((dt - Timing.EXPAND - Timing.DISPLAY) / (float) Timing.COLLAPSE_1);
                setPhase(Phase.COLLAPSE_1, p, targetExpandedWidth, Size.EXPANDED_H, 1f);
                targetAnimW = targetExpandedWidth;
            } else {
                float p = easeOut((dt - Timing.EXPAND - Timing.DISPLAY - Timing.COLLAPSE_1) / (float) Timing.COLLAPSE_2);
                setPhase(Phase.COLLAPSE_2, p,
                        lerp(p, targetExpandedWidth, Size.BASE_W),
                        lerp(p, Size.EXPANDED_H, Size.BASE_H),
                        1f);
                targetAnimW = lerp(p, targetExpandedWidth, Size.BASE_W);
            }
        }

        // Apply smooth interpolation to animW to avoid jumps when targetExpandedWidth changes
        if (!isTabPhase()) {
            this.animW = lerp(0.3f, this.animW, targetAnimW);
        } else {
            // In Tab phase, we use the exact calculated value because the lerp is already in the phase logic
            // and we don't want to double-smooth or lag behind the complex tab animation
        }
        
        animX = (mc.getWindow().getGuiScaledWidth() - animW) / 2f;
        animY = yOffset.getCurrentValue();
    }

    private float getRadius() {
        return radius.getCurrentValue();
    }

    private void setPhase(Phase p, float prog, float w, float h, float blur) {
        this.phase = p;
        this.progress = prog;
        this.animW = w;
        this.animH = h;
        this.blurOpacity = interpolateBlurOpacity(blur);
    }

    private boolean isTabPhase() {
        return phase == Phase.TAB_EXPAND || phase == Phase.TAB_DISPLAY || phase == Phase.TAB_COLLAPSE;
    }

    private float getMergeProgress() {
        if (phase == Phase.TAB_EXPAND || phase == Phase.TAB_DISPLAY || phase == Phase.TAB_COLLAPSE) {
            return tabMergeProgress;
        }
        return progress;
    }

    private float interpolateBlurOpacity(float targetBlur) {
        float delta = targetBlur - this.blurOpacity;
        float interpolationFactor = 0.15f;
        return this.blurOpacity + delta * interpolationFactor;
    }

    private void renderContent() {
        switch (phase) {
            case IDLE -> renderIdle();
            case EXPANDING -> renderExpanding();
            case DISPLAY -> renderDisplay();
            case COLLAPSE_1 -> renderCollapse1();
            case COLLAPSE_2 -> renderCollapse2();
            case TAB_EXPAND -> renderTabExpand();
            case TAB_DISPLAY -> renderTabDisplay();
            case TAB_COLLAPSE -> renderTabCollapse();
        }
    }

    private void renderIdle() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        drawSideInfo(1f);
        drawCenteredTitle(1f);
    }

    private void renderExpanding() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        drawSideInfo(1f);
        if (currentToggle != null) {
            drawToggleInfo(alphaFromProgress(progress), 0f);
        }
    }

    private void renderDisplay() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        drawSideInfo(1f);
        if (currentToggle != null) {
            drawToggleInfo(255, progress);
        }
    }

    private void renderCollapse1() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        drawSideInfo(1f);
        if (currentToggle != null) {
            drawToggleInfo(alphaFromProgress(1f - progress), 1f);
        }
    }

    private void renderCollapse2() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        drawSideInfo(1f);
    }

    private void renderTabExpand() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        float alpha = 1f - getMergeProgress();
        drawSideInfo(1f);
        drawCenteredTitle(alpha);
    }

    private void renderTabDisplay() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        drawSideInfo(1f);
    }

    private void renderTabCollapse() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        float alpha = 1f - getMergeProgress();
        drawSideInfo(1f);
        drawCenteredTitle(alpha);
    }

    private void drawBackground(Color color) {
        if (enableBloom.getCurrentValue()) {
            Skia.drawShadow(animX, animY, animW, animH, getRadius());
        }
        if (blur.getCurrentValue() && blurOpacity > 0.05f) {
            Skia.drawRoundedBlur(animX, animY, animW, animH, getRadius());
        }
        Skia.drawRoundedRect(animX, animY, animW, animH, getRadius(), color);
    }

    private void drawCenteredTitle(float alpha) {
        if (alpha <= 0.05f) return;
        Font font = Fonts.getUrbanistVariable(Size.LOGO_FONT_SIZE);
        String name = "Naven";
        float textW = Skia.getStringWidth(name, font);
        Color color = withAlpha(Color.WHITE, (int) (255 * alpha));
        float centerY = animY + animH / 2f;
        float textY = getTextBaseline(centerY, font);
        Skia.drawText(name, animX + (animW - textW) / 2f, textY, color, font);
    }

    private void drawSideInfo(float alpha) {
        if (alpha <= 0.05f) return;
        Font font = Fonts.getMiSans(Size.INFO_FONT_SIZE);
        float sideH = Size.BASE_H;
        float centerY = animY + sideH / 2f;
        float textY = getTextBaseline(centerY, font);
        Color color = withAlpha(Color.WHITE, (int) (255 * alpha));
        Color bgColor = withAlpha(Size.INVENTORY_BG_COLOR, (int) (70 * alpha));

        String time = LocalTime.now().format(TIME_FORMAT);
        float timeW = Skia.getStringWidth(time, font);
        float timeBgX = animX - Size.ELEMENT_SPACING - Size.ELEMENT_WIDTH;

        if (enableBloom.getCurrentValue()) {
            Skia.drawShadow(timeBgX, animY, Size.ELEMENT_WIDTH, sideH, getRadius());
        }
        if (blur.getCurrentValue() && blurOpacity > 0.05f) {
            Skia.drawRoundedBlur(timeBgX, animY, Size.ELEMENT_WIDTH, sideH, getRadius());
        }
        Skia.drawRoundedRect(timeBgX, animY, Size.ELEMENT_WIDTH, sideH, getRadius(), bgColor);
        Skia.drawText(time, timeBgX + (Size.ELEMENT_WIDTH - timeW) / 2f, textY, color, font);

        String fps = "FPS:" + mc.getFps();
        float fpsW = Skia.getStringWidth(fps, font);
        float nameBgX = animX + animW + Size.ELEMENT_SPACING;

        if (enableBloom.getCurrentValue()) {
            Skia.drawShadow(nameBgX, animY, Size.ELEMENT_WIDTH, sideH, getRadius());
        }
        if (blur.getCurrentValue() && blurOpacity > 0.05f) {
            Skia.drawRoundedBlur(nameBgX, animY, Size.ELEMENT_WIDTH, sideH, getRadius());
        }
        Skia.drawRoundedRect(nameBgX, animY, Size.ELEMENT_WIDTH, sideH, getRadius(), bgColor);
        Skia.drawText(fps, nameBgX + (Size.ELEMENT_WIDTH - fpsW) / 2f, textY, color, font);
    }

    private void renderCapturedTab() {
        if (playerList == null) return;

        float innerX1 = animX + Size.TAB_PADDING;
        float innerY1 = animY + Size.TAB_PADDING;
        float innerX2 = animX + animW - Size.TAB_PADDING;
        float innerY2 = animY + animH - Size.TAB_PADDING;
        if (innerX2 <= innerX1 || innerY2 <= innerY1) return;

        float innerW = innerX2 - innerX1;
        Font font = Fonts.getMiSans(Size.INFO_FONT_SIZE);
        float fontH = getFontHeight(font);
        float appear = clamp((getMergeProgress() - 0.15f) / 0.85f);
        if (appear <= 0f) return;
        int alpha = (int) (255 * appear);
        Color textColor = withAlpha(Color.WHITE, alpha);
        Color pingColor = new Color(160, 160, 160, alpha);

        Skia.save();
        Skia.clip(innerX1, innerY1, innerW, innerY2 - innerY1, 0f);

        float y = animY + Size.TAB_HEADER_Y;
        Component headerText = capturedTabHeader;
        if (headerText == null || headerText.getString().isEmpty()) {
            headerText = Component.literal("Players: " + playerList.size());
        }
        List<String> headerLines = wrapLines(headerText.getString(), innerW, font);
        for (String line : headerLines) {
            float lineW = Skia.getStringWidth(line, font);
            float x = animX + (animW - lineW) / 2f;
            Skia.drawText(line, x, y, textColor, font);
            y += fontH;
        }
        y += 8f;

        float listY = Math.max(animY + Size.TAB_LIST_Y, y);
        float rowH = Size.TAB_PLAYER_HEIGHT;
        float headSize = 10f;
        float headYOffset = Math.max(0f, (rowH - headSize) / 2f);

        int i = 0;
        for (PlayerInfo entry : playerList) {
            float rowY = listY + i * rowH;
            if (rowY + rowH > innerY2) break;

            float headX = innerX1;
            float headY = rowY + headYOffset;
            float rowCenterY = rowY + rowH / 2f;
            float textY = getTextBaseline(rowCenterY, font) - 1.5f;
            ResourceLocation skinLoc = null;
            Player playerEntity = mc.level == null ? null : mc.level.getPlayerByUUID(entry.getProfile().getId());
            if (playerEntity instanceof AbstractClientPlayer clientPlayer) {
                skinLoc = clientPlayer.getSkinTextureLocation();
            } else {
                skinLoc = entry.getSkinLocation();
            }

            if (skinLoc != null) {
                Skia.drawPlayerHead(skinLoc, headX, headY, headSize, headSize, 2f);
            }

            String ping = entry.getLatency() + "ms";
            float pingW = Skia.getStringWidth(ping, font);
            float pingX = innerX2 - pingW;
            Skia.drawText(ping, pingX, textY, pingColor, font);

            String name = entry.getProfile().getName();
            float nameX = headX + headSize + 4f;
            float nameClipX2 = pingX - 6f;
            if (nameClipX2 > nameX) {
                Skia.save();
                Skia.clip(nameX, rowY, nameClipX2 - nameX, rowH, 0f);
                Skia.drawText(name, nameX, textY, textColor, font);
                Skia.restore();
            }

            i++;
        }

        Component footerText = capturedTabFooter;
        if (footerText != null && !footerText.getString().isEmpty()) {
            List<String> footerLines = wrapLines(footerText.getString(), innerW, font);
            float footerY = innerY2 - footerLines.size() * fontH;
            for (String line : footerLines) {
                float lineW = Skia.getStringWidth(line, font);
                float x = animX + (animW - lineW) / 2f;
                Skia.drawText(line, x, footerY, textColor, font);
                footerY += fontH;
            }
        }

        Skia.restore();
    }

    private void drawToggleInfo(int alpha, float timeProgress) {
        if (currentToggle == null) return;
        float padding = 6f;
        float iconSize = 12f;
        float spacing = 5f;

        Font textFont = Fonts.getMiSans(iconSize);
        float centerY = animY + animH / 2f;

        // Draw Icon - Move down slightly to center visually
        float iconY = centerY - iconSize / 2f + 1f;
        drawStatusIcon(animX + padding, iconY, iconSize, currentToggle.enabled, alpha);

        // Draw Text - Move up slightly to match icon visual center
        float textX = animX + padding + iconSize + spacing;
        // The default getTextBaseline with -7f might be too low for 12f font, so we lift it up
        float textY = getTextBaseline(centerY, textFont) - 1.5f;
        Skia.drawText(currentToggle.name, textX, textY, withAlpha(Color.WHITE, alpha), textFont);
    }

    private void drawStatusIcon(float x, float y, float size, boolean enabled, int alpha) {
        Color color = enabled ? new Color(80, 220, 100, alpha) : new Color(220, 80, 80, alpha);
        float stroke = 2f;

        if (enabled) {
            float x1 = x + size * 0.15f;
            float y1 = y + size * 0.5f;
            float x2 = x + size * 0.4f;
            float y2 = y + size * 0.8f;
            float x3 = x + size * 0.85f;
            float y3 = y + size * 0.25f;
            Skia.drawLine(x1, y1, x2, y2, stroke, color);
            Skia.drawLine(x2, y2, x3, y3, stroke, color);
        } else {
            float padding = size * 0.2f;
            Skia.drawLine(x + padding, y + padding, x + size - padding, y + size - padding, stroke, color);
            Skia.drawLine(x + size - padding, y + padding, x + padding, y + size - padding, stroke, color);
        }
    }

    private float calculateExpandedWidth() {
        return calculateExpandedWidth(currentToggle);
    }

    private float calculateExpandedWidth(ToggleInfo toggle) {
        if (toggle == null) return Size.EXPANDED_W;
        float padding = 6f;
        float iconSize = 12f;
        float spacing = 5f;
        Font textFont = Fonts.getMiSans(iconSize);
        float textW = Skia.getStringWidth(toggle.name, textFont);
        float needed = padding + iconSize + spacing + textW + padding + 6f;
        return Math.max(Size.EXPANDED_W, needed);
    }

    private long elapsedToggle() {
        return toggleStartTime == -1L ? 0 : System.currentTimeMillis() - toggleStartTime;
    }

    private long elapsedTab() {
        return tabStartTime == -1L ? 0 : System.currentTimeMillis() - tabStartTime;
    }

    private static float easeOut(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private static float easeInOut(float t) {
        if (t < 0.5f) {
            return 4f * t * t * t;
        }
        float inv = -2f * t + 2f;
        return 1f - (inv * inv * inv) / 2f;
    }

    private static int alphaFromProgress(float p) {
        return (int) (255 * p);
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static float clamp(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }

    private static float getFontHeight(Font font) {
        FontMetrics metrics = font.getMetrics();
        return metrics.getDescent() - metrics.getAscent();
    }

    private static float getTextBaseline(float centerY, Font font) {
        FontMetrics metrics = font.getMetrics();
        return centerY - (metrics.getAscent() + metrics.getDescent()) / 2f - 7f;
    }

    private static List<String> wrapLines(String text, float maxWidth, Font font) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (Skia.getStringWidth(test, font) <= maxWidth) {
                current.setLength(0);
                current.append(test);
            } else {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                if (Skia.getStringWidth(word, font) <= maxWidth) {
                    current.append(word);
                } else {
                    StringBuilder partial = new StringBuilder();
                    for (int i = 0; i < word.length(); i++) {
                        partial.append(word.charAt(i));
                        if (Skia.getStringWidth(partial.toString(), font) > maxWidth) {
                            if (partial.length() > 1) {
                                partial.deleteCharAt(partial.length() - 1);
                                lines.add(partial.toString());
                                partial.setLength(0);
                                partial.append(word.charAt(i));
                            }
                        }
                    }
                    if (partial.length() > 0) {
                        current.append(partial);
                    }
                }
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private record ToggleInfo(String name, boolean enabled) {
    }
}
