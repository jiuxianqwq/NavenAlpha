package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.InventoryCleaner;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Icon;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;
import io.github.humbleui.types.RRect;
import io.github.humbleui.skija.ClipMode;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
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
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
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
        static final float LOGO_FONT_SIZE = 14f;
        static final float INFO_FONT_SIZE = 10f;
        static final Color INVENTORY_BG_COLOR = new Color(18, 18, 18, 70);

        static final float TAB_PLAYER_HEIGHT = 14f;
        static final float TAB_PADDING = 8f;
        static final float TAB_HEADER_Y = 12f;
        static final float TAB_LIST_Y = 30f;
        static final int TAB_COLUMNS = 1;
    }

    private static final class Timing {
        static final long EXPAND = 220L;
        static final long DISPLAY = 1200L;
        static final long COLLAPSE_1 = 220L;
        static final long COLLAPSE_2 = 260L;
        static final long TOTAL = EXPAND + DISPLAY + COLLAPSE_1 + COLLAPSE_2;
        static final long TAB_TRANSITION = 380L;
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
    private static final Queue<ToggleInfo> pendingQueue = new ConcurrentLinkedQueue<>();
    private static final Set<UUID> attackedPlayers = new CopyOnWriteArraySet<>();
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
    private float tabStartW = Size.BASE_W;
    private float expandStartW = Size.BASE_W;
    private float expandStartH = Size.BASE_H;
    private final SmoothAnimationTimer blinkProgressTimer = new SmoothAnimationTimer(0.0F, 0.0F, 0.12F);

    private List<PlayerInfo> playerList;
    private float tabTargetW;
    private float tabTargetH;

    public static void onModuleToggle(Module module, boolean enabled) {
        if (module instanceof DynamicIslandHud) return;
        ToggleType type = ToggleType.MODULE;
        if (module instanceof Scaffold) {
            type = ToggleType.SCAFFOLD;
        } else if (module instanceof Blink) {
            type = ToggleType.BLINK;
        }
        pendingQueue.offer(new ToggleInfo(module.getName(), enabled, type));
    }

    public static void onPlayerAttack(Player player) {
        if (player == null) return;
        attackedPlayers.add(player.getUUID());
    }

    private static void onKill(String playerName) {
        if (playerName == null || playerName.isEmpty()) return;
        pendingQueue.offer(new ToggleInfo("You killed " + playerName + " !", true, ToggleType.KILL));
    }

    private static void onWin() {
        pendingQueue.offer(new ToggleInfo("Victory!", true, ToggleType.WIN));
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

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getType() != EventType.RECEIVE || mc.getConnection() == null) {
            return;
        }

        if (e.getPacket() instanceof ClientboundPlayerInfoRemovePacket packet) {
            for (UUID entry : packet.profileIds()) {
                if (!attackedPlayers.contains(entry)) {
                    continue;
                }
                PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(entry);
                if (playerInfo != null) {
                    onKill(playerInfo.getProfile().getName());
                }
                attackedPlayers.remove(entry);
            }
        } else if (e.getPacket() instanceof ClientboundSetTitleTextPacket packet) {
            if (isWinText(packet.getText().getString())) {
                onWin();
            }
        } else if (e.getPacket() instanceof ClientboundSetSubtitleTextPacket packet) {
            if (isWinText(packet.getText().getString())) {
                onWin();
            }
        }
    }

    private static boolean isWinText(String text) {
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("胜利")
                || lower.contains("获胜")
                || lower.contains("victory")
                || lower.contains("you win");
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        attackedPlayers.clear();
        pendingQueue.clear();
    }

    private void update() {
        handleTabInput();
        processToggle();
        calculateState();
        updateBlinkAnimation();
    }

    private void updateBlinkAnimation() {
        if (Naven.getInstance() == null || Naven.getInstance().getModuleManager() == null) {
            blinkProgressTimer.target = 0f;
            blinkProgressTimer.update(false);
            return;
        }

        Module module = Naven.getInstance().getModuleManager().getModule(Blink.class);
        if (module != null && module.isEnabled()) {
            Blink blink = (Blink) module;
            float max = blink.maxTicks.getCurrentValue();
            float current = blink.getBlinkTicks();
            float target = clamp(current / max);
            blinkProgressTimer.target = target;
            blinkProgressTimer.update(true);
        } else {
            blinkProgressTimer.target = 0f;
            blinkProgressTimer.update(false);
        }
    }

    private void handleTabInput() {
        boolean tabPressed = mc.options.keyPlayerList.isDown();

        if (tabPressed && !isTabPhase()) {
            tabStartTime = System.currentTimeMillis();
            phase = Phase.TAB_EXPAND;
            tabStartW = animW;
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

        if (!pendingQueue.isEmpty()) {
            applyPending(pendingQueue.poll());
        } else if (currentToggle != null && elapsedToggle() >= Timing.TOTAL) {
            currentToggle = null;
            toggleStartTime = -1L;
        }
    }

    private void applyPending(ToggleInfo next) {
        float newTargetW = calculateExpandedWidth(next);
        this.expandStartW = this.animW;
        this.expandStartH = this.animH;
        this.currentToggle = next;
        this.targetExpandedWidth = newTargetW;

        if ((next.type == ToggleType.SCAFFOLD || next.type == ToggleType.BLINK) && !next.enabled) {
            this.toggleStartTime = System.currentTimeMillis() - (Timing.EXPAND + Timing.DISPLAY + Timing.COLLAPSE_1);
        } else {
            this.toggleStartTime = System.currentTimeMillis();
        }
    }

    private void calculateState() {
        long dt = elapsedToggle();
        long tabDt = elapsedTab();
        float targetAnimW = animW; // Default to keeping current
        float baseH = (isScaffoldActive() || isBlinkActive()) ? Size.EXPANDED_H : Size.BASE_H;

        if (phase == Phase.TAB_EXPAND) {
            if (tabDt < Timing.TAB_TRANSITION) {
                float t = clamp(tabDt / (float) Timing.TAB_TRANSITION);
                float mergeP = easeInOut(t);
                float expandP = easeInOut(t);
                tabMergeProgress = mergeP;
                setPhase(Phase.TAB_EXPAND, expandP,
                        lerp(mergeP, tabStartW, tabTargetW),
                        lerp(expandP, baseH, tabTargetH),
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
                        lerp(mergeP, tabStartW, tabTargetW),
                        lerp(expandP, baseH, tabTargetH),
                        1f);
                targetAnimW = animW;
            } else {
                tabMergeProgress = 0f;
                setPhase(Phase.IDLE, 0f, tabStartW, baseH, 1f);
                tabStartTime = -1L;
                targetAnimW = tabStartW;
            }
        } else if (phase == Phase.TAB_DISPLAY) {
            tabMergeProgress = 1f;
            setPhase(Phase.TAB_DISPLAY, 1f, tabTargetW, tabTargetH, 1f);
            targetAnimW = tabTargetW;
        } else {
            float idleW = isBlinkActive() ? calculateBlinkWidth() : (isScaffoldActive() ? calculateScaffoldWidth() : Size.BASE_W);
            
            if (currentToggle == null && toggleStartTime == -1L) {
                setPhase(Phase.IDLE, 0f, idleW, baseH, 1f);
                targetAnimW = idleW;
            } else if (dt < Timing.EXPAND) {
                float t = clamp(dt / (float) Timing.EXPAND);
                float p = easeOut(t);
                setPhase(Phase.EXPANDING, p,
                        lerp(p, expandStartW, targetExpandedWidth),
                        lerp(p, expandStartH, Size.EXPANDED_H),
                        1f);
                targetAnimW = lerp(p, expandStartW, targetExpandedWidth);
            } else if (dt < Timing.EXPAND + Timing.DISPLAY) {
                float p = (dt - Timing.EXPAND) / (float) Timing.DISPLAY;
                setPhase(Phase.DISPLAY, p, animW, Size.EXPANDED_H, 1f);
                targetAnimW = targetExpandedWidth;
            } else if (dt < Timing.EXPAND + Timing.DISPLAY + Timing.COLLAPSE_1) {
                float t = clamp((dt - Timing.EXPAND - Timing.DISPLAY) / (float) Timing.COLLAPSE_1);
                float p = easeInOut(t);
                setPhase(Phase.COLLAPSE_1, p, targetExpandedWidth, Size.EXPANDED_H, 1f);
                targetAnimW = targetExpandedWidth;
            } else {
                float t = clamp((dt - Timing.EXPAND - Timing.DISPLAY - Timing.COLLAPSE_1) / (float) Timing.COLLAPSE_2);
                float p = easeInOut(t);
                setPhase(Phase.COLLAPSE_2, p,
                        lerp(p, targetExpandedWidth, idleW),
                        lerp(p, Size.EXPANDED_H, baseH),
                        1f);
                targetAnimW = lerp(p, targetExpandedWidth, idleW);
            }
        }

        // Apply smooth interpolation to animW to avoid jumps when targetExpandedWidth changes
        if (!isTabPhase()) {
            this.animW = lerp(0.38f, this.animW, targetAnimW);
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
        float interpolationFactor = 0.22f;
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
        if (isBlinkActive()) {
            drawBlinkInfo(1f);
        } else if (isScaffoldActive()) {
            drawScaffoldInfo(1f);
        } else {
            drawCenteredTitle(1f);
        }
    }

    private void renderExpanding() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        drawSideInfo(1f);
        if (currentToggle != null && currentToggle.type == ToggleType.SCAFFOLD && currentToggle.enabled) {
            drawScaffoldInfo(progress);
        } else if (currentToggle != null && currentToggle.type == ToggleType.BLINK && currentToggle.enabled) {
            drawBlinkInfo(progress);
        } else if (currentToggle != null) {
            drawToggleInfo(currentToggle, alphaFromProgress(progress), 0f);
        } else if (isBlinkActive()) {
            drawBlinkInfo(1f);
        } else if (isScaffoldActive()) {
            drawScaffoldInfo(1f);
        }
    }

    private void renderDisplay() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        drawSideInfo(1f);
        if (currentToggle != null && currentToggle.type == ToggleType.SCAFFOLD && currentToggle.enabled) {
            drawScaffoldInfo(1f);
        } else if (currentToggle != null && currentToggle.type == ToggleType.BLINK && currentToggle.enabled) {
            drawBlinkInfo(1f);
        } else if (currentToggle != null) {
            drawToggleInfo(currentToggle, 255, progress);
        } else if (isBlinkActive()) {
            drawBlinkInfo(1f);
        } else if (isScaffoldActive()) {
            drawScaffoldInfo(1f);
        }
    }

    private void renderCollapse1() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        drawSideInfo(1f);
        if (currentToggle != null && currentToggle.type == ToggleType.SCAFFOLD) {
            drawScaffoldInfo(1f);
        } else if (currentToggle != null && currentToggle.type == ToggleType.BLINK) {
            drawBlinkInfo(1f);
        } else if (currentToggle != null) {
            drawToggleInfo(currentToggle, alphaFromProgress(1f - progress), 1f);
        }
    }

    private void renderCollapse2() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        drawSideInfo(1f);
        if (currentToggle != null && currentToggle.type == ToggleType.BLINK && !currentToggle.enabled) {
            drawBlinkInfo(1f - progress);
        } else if (currentToggle != null && currentToggle.type == ToggleType.SCAFFOLD && !currentToggle.enabled) {
            drawScaffoldInfo(1f - progress);
        } else if (isBlinkActive()) {
            drawBlinkInfo(1f);
        } else if (isScaffoldActive()) {
            drawScaffoldInfo(1f);
        } else {
            drawCenteredTitle(progress);
        }
    }

    private void renderTabExpand() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        float alpha = 1f - getMergeProgress();
        drawSideInfo(1f);
        if (currentToggle != null && currentToggle.type == ToggleType.SCAFFOLD) {
            drawScaffoldInfo(alpha);
        } else if (currentToggle != null && currentToggle.type == ToggleType.BLINK) {
            drawBlinkInfo(alpha);
        } else if (currentToggle != null) {
            drawToggleInfo(currentToggle, (int) (255 * alpha), 0f);
        } else if (isBlinkActive()) {
            drawBlinkInfo(alpha);
        } else if (isScaffoldActive()) {
            drawScaffoldInfo(alpha);
        } else {
            drawCenteredTitle(alpha);
        }
    }

    private void renderTabDisplay() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        drawSideInfo(1f);
    }

    private void renderTabCollapse() {
        drawBackground(Size.INVENTORY_BG_COLOR);
        float alpha = 1f - getMergeProgress();
        drawSideInfo(1f);
        if (currentToggle != null && currentToggle.type == ToggleType.SCAFFOLD) {
            drawScaffoldInfo(alpha);
        } else if (currentToggle != null && currentToggle.type == ToggleType.BLINK) {
            drawBlinkInfo(alpha);
        } else if (currentToggle != null) {
            drawToggleInfo(currentToggle, (int) (255 * alpha), 1f);
        } else if (isBlinkActive()) {
            drawBlinkInfo(alpha);
        } else if (isScaffoldActive()) {
            drawScaffoldInfo(alpha);
        } else {
            drawCenteredTitle(alpha);
        }
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
        Color color = withAlpha(new Color(255, 105, 180), (int) (255 * alpha));
        Rect bounds = font.measureText(name);
        FontMetrics metrics = font.getMetrics();
        float centerX = animX + animW / 2f;
        float centerY = animY + animH / 2f;
        float bx = centerX - bounds.getLeft() - (bounds.getWidth() / 2f);
        float by = centerY + (metrics.getAscent() - metrics.getDescent()) / 2f - metrics.getAscent();
        int skColor = io.github.humbleui.skija.Color.makeARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
        try (Paint paint = new Paint().setColor(skColor);
             Paint blurPaint = paint.makeClone().setImageFilter(ImageFilter.makeBlur(2.5F, 2.5F, FilterTileMode.DECAL))) {
            Skia.getCanvas().drawString(name, bx, by, font, blurPaint);
            Skia.getCanvas().drawString(name, bx - 0.2f, by, font, paint);
            Skia.getCanvas().drawString(name, bx + 0.2f, by, font, paint);
            Skia.getCanvas().drawString(name, bx, by, font, paint);
        }
    }

    private boolean isScaffoldActive() {
        if (Naven.getInstance() == null || Naven.getInstance().getModuleManager() == null) return false;
        Module scaffold = Naven.getInstance().getModuleManager().getModule(Scaffold.class);
        return scaffold != null && scaffold.isEnabled();
    }

    private void drawScaffoldInfo(float alpha) {
        if (alpha <= 0.05f) return;
        int blockCount = InventoryUtils.getBlockCountInInventory();
        int maxCount = Math.max(1, InventoryCleaner.getMaxBlockSize());
        float progress = clamp(blockCount / (float) maxCount);

        float padding = 6f;
        float iconSize = 10f;
        float spacing = 6f;
        float barHeight = 6f;
        float barRadius = barHeight / 2f;

        Font textFont = Fonts.getMiSans(10f);
        String text = blockCount + "/" + maxCount;
        float textW = Skia.getStringWidth(text, textFont);

        float centerY = animY + animH / 2f;
        float iconX = animX + padding;
        float iconY = centerY - iconSize / 2f;
        drawCubeIcon(iconX, iconY, iconSize, (int) (255 * alpha));

        float textX = animX + animW - padding - textW;
        float textY = getTextBaseline(centerY, textFont) - 1.0f;
        Skia.drawText(text, textX, textY, withAlpha(Color.WHITE, (int) (255 * alpha)), textFont);

        float barX = iconX + iconSize + spacing;
        float barW = textX - spacing - barX;
        if (barW > 2f) {
            float barY = centerY - barHeight / 2f;
            float fillW = barW * progress;
            Color barBg = withAlpha(new Color(210, 210, 210), (int) (160 * alpha));
            Color barFill = withAlpha(new Color(196, 128, 224), (int) (220 * alpha));
            Color barGlow = withAlpha(new Color(196, 128, 224), (int) (120 * alpha));
            Path barPath = new Path();
            barPath.addRRect(RRect.makeXYWH(barX, barY, barW, barHeight, barRadius));
            Skia.save();
            Skia.getCanvas().clipPath(barPath, ClipMode.INTERSECT, true);
            Skia.drawRoundedRect(barX, barY, barW, barHeight, barRadius, barBg);
            if (fillW > 0.5f) {
                Skia.drawShadow(barX, barY, fillW, barHeight, barRadius, barGlow);
                Skia.drawRoundedRect(barX, barY, fillW, barHeight, barRadius, barFill);
            }
            Skia.restore();
            barPath.close();
        }
    }

    private float calculateScaffoldWidth() {
        float padding = 6f;
        float iconSize = 10f;
        float spacing = 6f;
        float minBarW = 65f;
        Font textFont = Fonts.getMiSans(10f);
        int blockCount = InventoryUtils.getBlockCountInInventory();
        int maxCount = Math.max(1, InventoryCleaner.getMaxBlockSize());
        String text = blockCount + "/" + maxCount;
        float textW = Skia.getStringWidth(text, textFont);
        float needed = padding + iconSize + spacing + minBarW + spacing + textW + padding;
        return Math.max(Size.EXPANDED_W, needed);
    }

    private void drawCubeIcon(float x, float y, float size, int alpha) {
        Color color = withAlpha(new Color(180, 180, 180), alpha);
        float s = size;
        float offset = s * 0.25f;
        float x1 = x;
        float y1 = y + offset;
        float x2 = x + s - offset;
        float y2 = y + offset;
        float x3 = x + s - offset;
        float y3 = y + s;
        float x4 = x;
        float y4 = y + s;
        float x5 = x + offset;
        float y5 = y;
        float x6 = x + s;
        float y6 = y;
        float x7 = x + s;
        float y7 = y + s - offset;
        Skia.drawLine(x1, y1, x2, y2, 1.2f, color);
        Skia.drawLine(x2, y2, x3, y3, 1.2f, color);
        Skia.drawLine(x3, y3, x4, y4, 1.2f, color);
        Skia.drawLine(x4, y4, x1, y1, 1.2f, color);
        Skia.drawLine(x5, y5, x6, y6, 1.2f, color);
        Skia.drawLine(x6, y6, x7, y7, 1.2f, color);
        Skia.drawLine(x7, y7, x3, y3, 1.2f, color);
        Skia.drawLine(x5, y5, x1, y1, 1.2f, color);
    }

    private boolean isBlinkActive() {
        if (Naven.getInstance() == null || Naven.getInstance().getModuleManager() == null) return false;
        Module blink = Naven.getInstance().getModuleManager().getModule(Blink.class);
        return blink != null && blink.isEnabled();
    }

    private void drawBlinkInfo(float alpha) {
        if (alpha <= 0.05f) return;
        if (Naven.getInstance() == null || Naven.getInstance().getModuleManager() == null) return;
        Blink blink = (Blink) Naven.getInstance().getModuleManager().getModule(Blink.class);
        if (blink == null) return;

        long ticks = blink.getBlinkTicks();
        float maxTicks = blink.maxTicks.getCurrentValue();
        float progress = blinkProgressTimer.value;

        float padding = 6f;
        float iconSize = 10f;
        float spacing = 6f;
        float barHeight = 6f;
        float barRadius = barHeight / 2f;

        Font textFont = Fonts.getMiSans(10f);
        String text = ticks + "/" + (int)maxTicks;
        float textW = Skia.getStringWidth(text, textFont);

        float centerY = animY + animH / 2f;
        float iconX = animX + padding;
        float iconY = centerY - iconSize / 2f;
        drawNetworkIcon(iconX, iconY, iconSize, (int) (255 * alpha));

        float textX = animX + animW - padding - textW;
        float textY = getTextBaseline(centerY, textFont) - 1.0f;
        Skia.drawText(text, textX, textY, withAlpha(Color.WHITE, (int) (255 * alpha)), textFont);

        float barX = iconX + iconSize + spacing;
        float barW = textX - spacing - barX;
        if (barW > 2f) {
            float barY = centerY - barHeight / 2f;
            float fillW = barW * progress;
            Color barBg = withAlpha(new Color(210, 210, 210), (int) (160 * alpha));
            Color barFill = withAlpha(new Color(196, 128, 224), (int) (220 * alpha));
            Color barGlow = withAlpha(new Color(196, 128, 224), (int) (120 * alpha));
            Path barPath = new Path();
            barPath.addRRect(RRect.makeXYWH(barX, barY, barW, barHeight, barRadius));
            Skia.save();
            Skia.getCanvas().clipPath(barPath, ClipMode.INTERSECT, true);
            Skia.drawRoundedRect(barX, barY, barW, barHeight, barRadius, barBg);
            if (fillW > 0.5f) {
                Skia.drawShadow(barX, barY, fillW, barHeight, barRadius, barGlow);
                Skia.drawRoundedRect(barX, barY, fillW, barHeight, barRadius, barFill);
            }
            Skia.restore();
            barPath.close();
        }
    }

    private float calculateBlinkWidth() {
        float padding = 6f;
        float iconSize = 10f;
        float spacing = 6f;
        float minBarW = 65f;
        Font textFont = Fonts.getMiSans(10f);
        if (Naven.getInstance() == null || Naven.getInstance().getModuleManager() == null) return Size.EXPANDED_W;
        Blink blink = (Blink) Naven.getInstance().getModuleManager().getModule(Blink.class);
        if (blink == null) return Size.EXPANDED_W;

        long ticks = blink.getBlinkTicks();
        float maxTicks = blink.maxTicks.getCurrentValue();
        String text = ticks + "/" + (int)maxTicks;
        float textW = Skia.getStringWidth(text, textFont);
        float needed = padding + iconSize + spacing + minBarW + spacing + textW + padding;
        return Math.max(Size.EXPANDED_W, needed);
    }

    private void drawNetworkIcon(float x, float y, float size, int alpha) {
        Color color = withAlpha(new Color(180, 180, 180), alpha);
        float s = size;
        float centerX = x + s / 2f;
        float bottomY = y + s - 1f;

        float stroke = 1.5f;
        Paint paint = new Paint().setColor(io.github.humbleui.skija.Color.makeARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue()));
        paint.setMode(io.github.humbleui.skija.PaintMode.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(io.github.humbleui.skija.PaintStrokeCap.ROUND);

        Paint fillPaint = new Paint().setColor(io.github.humbleui.skija.Color.makeARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue()));
        Skia.getCanvas().drawCircle(centerX, bottomY, 1.2f, fillPaint);

        Skia.getCanvas().drawArc(centerX - 3.5f, bottomY - 3.5f, centerX + 3.5f, bottomY + 3.5f, 225f, 90f, false, paint);
        Skia.getCanvas().drawArc(centerX - 6.5f, bottomY - 6.5f, centerX + 6.5f, bottomY + 6.5f, 225f, 90f, false, paint);

        paint.close();
        fillPaint.close();
    }

    private void drawSideInfo(float alpha) {
        if (alpha <= 0.05f) return;
        Font font = Fonts.getMiSans(Size.INFO_FONT_SIZE);
        Font iconFont = Fonts.getIcon(Size.INFO_FONT_SIZE);
        float sideH = Size.BASE_H;
        float centerY = animY + sideH / 2f;
        float textY = getTextBaseline(centerY, font);
        Color color = withAlpha(Color.WHITE, (int) (255 * alpha));
        float padding = 6f;
        float iconSpacing = 4f;

        String time = LocalTime.now().format(TIME_FORMAT);
        String timeIcon = Icon.ALARM;
        Rect timeIconBounds = Skia.getTextBounds(timeIcon, iconFont);
        Rect timeTextBounds = Skia.getTextBounds(time, font);
        float timeIconW = timeIconBounds.getWidth();
        float timeTextW = timeTextBounds.getWidth();
        float timeTextStart = timeIconW + iconSpacing;
        float timeGroupLeft = Math.min(timeIconBounds.getLeft(), timeTextStart + timeTextBounds.getLeft());
        float timeGroupRight = Math.max(timeIconBounds.getLeft() + timeIconW, timeTextStart + timeTextBounds.getLeft() + timeTextW);
        float timeGroupW = timeGroupRight - timeGroupLeft;
        float timeBgW = Math.max(Size.ELEMENT_WIDTH, padding * 2f + timeGroupW);
        float timeBgX = animX - Size.ELEMENT_SPACING - timeBgW;
        float timeTextCenterY = textY + (timeTextBounds.getTop() + timeTextBounds.getBottom()) / 2f;
        float timeIconY = timeTextCenterY - (timeIconBounds.getTop() + timeIconBounds.getBottom()) / 2f;

        float timeInnerW = timeBgW - padding * 2f;
        float timeStartX = timeBgX + timeBgW / 2f - timeGroupW / 2f - timeGroupLeft;
        float timeClipX = timeBgX + padding;
        float timeClipW = Math.max(0f, timeInnerW);
        Skia.save();
        Skia.clip(timeClipX, animY, timeClipW, sideH, getRadius());
        Skia.drawText(timeIcon, timeStartX, timeIconY, color, iconFont);
        Skia.drawText(time, timeStartX + timeTextStart, textY, color, font);
        Skia.restore();

        String fps = "FPS:" + mc.getFps();
        String fpsIcon = Icon.LAPTOP;
        Rect fpsIconBounds = Skia.getTextBounds(fpsIcon, iconFont);
        Rect fpsTextBounds = Skia.getTextBounds(fps, font);
        float fpsIconW = fpsIconBounds.getWidth();
        float fpsTextW = fpsTextBounds.getWidth();
        float fpsTextStart = fpsIconW + iconSpacing;
        float fpsGroupLeft = Math.min(fpsIconBounds.getLeft(), fpsTextStart + fpsTextBounds.getLeft());
        float fpsGroupRight = Math.max(fpsIconBounds.getLeft() + fpsIconW, fpsTextStart + fpsTextBounds.getLeft() + fpsTextW);
        float fpsGroupW = fpsGroupRight - fpsGroupLeft;
        float fpsBgW = Math.max(Size.ELEMENT_WIDTH, padding * 2f + fpsGroupW);
        float nameBgX = animX + animW + Size.ELEMENT_SPACING;
        float fpsTextCenterY = textY + (fpsTextBounds.getTop() + fpsTextBounds.getBottom()) / 2f;
        float fpsIconY = fpsTextCenterY - (fpsIconBounds.getTop() + fpsIconBounds.getBottom()) / 2f;

        float fpsInnerW = fpsBgW - padding * 2f;
        float fpsStartX = nameBgX + fpsBgW / 2f - fpsGroupW / 2f - fpsGroupLeft;
        float fpsClipX = nameBgX + padding;
        float fpsClipW = Math.max(0f, fpsInnerW);
        Skia.save();
        Skia.clip(fpsClipX, animY, fpsClipW, sideH, getRadius());
        Skia.drawText(fpsIcon, fpsStartX, fpsIconY, color, iconFont);
        Skia.drawText(fps, fpsStartX + fpsTextStart, textY, color, font);
        Skia.restore();
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
            float lineY = getTextBaseline(y + fontH / 2f, font);
            Skia.drawText(line, x, lineY, textColor, font);
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

            String name = resolveIrcName(entry.getProfile().getName());
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
                float lineY = getTextBaseline(footerY + fontH / 2f, font);
                Skia.drawText(line, x, lineY, textColor, font);
                footerY += fontH;
            }
        }

        Skia.restore();
    }

    private String resolveIrcName(String ign) {
        if (ign == null || ign.isEmpty()) {
            return ign;
        }
        if (AuthUtils.transport == null) {
            return ign;
        }
        String ircName = null;
        try {
            ircName = AuthUtils.transport.getName(ign);
        } catch (Exception ignored) {
        }
        if (ircName == null || ircName.isBlank()) {
            return ign;
        }
        return ircName;
    }

    private void drawToggleInfo(ToggleInfo toggle, int alpha, float timeProgress) {
        if (toggle == null) return;
        if (toggle.type == ToggleType.KILL) {
            drawKillInfo(toggle, alpha);
            return;
        }
        if (toggle.type == ToggleType.WIN) {
            drawWinInfo(toggle, alpha);
            return;
        }
        float padding = 6f;
        float iconSize = 12f;
        float spacing = 5f;

        Font textFont = Fonts.getMiSans(iconSize);
        float centerY = animY + animH / 2f;

        // Draw Icon - Move down slightly to center visually
        float iconY = centerY - iconSize / 2f + 1f;
        drawStatusIcon(animX + padding, iconY, iconSize, toggle.enabled, alpha);

        // Draw Text - Move up slightly to match icon visual center
        float textStartX = animX + padding + iconSize + spacing;
        float textAreaW = animX + animW - padding - textStartX;
        float textW = Skia.getStringWidth(toggle.name, textFont);
        float textX = textStartX + Math.max(0f, (textAreaW - textW) / 2f);
        // The default getTextBaseline with -7f might be too low for 12f font, so we lift it up
        float textY = getTextBaseline(centerY, textFont) - 1.5f;
        Skia.drawText(toggle.name, textX, textY, withAlpha(Color.WHITE, alpha), textFont);
    }

    private void drawKillInfo(ToggleInfo toggle, int alpha) {
        float padding = 6f;
        float iconSize = 9f;
        float spacing = 5f;

        Font textFont = Fonts.getMiSans(iconSize);
        float centerY = animY + animH / 2f;
        float iconX = animX + padding + iconSize / 2f;
        float iconY = centerY;
        Color glowColor = withAlpha(new Color(255, 105, 180), alpha);
        int skColor = io.github.humbleui.skija.Color.makeARGB(glowColor.getAlpha(), glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue());
        try (Paint paint = new Paint().setColor(skColor);
             Paint blurPaint = paint.makeClone().setImageFilter(ImageFilter.makeBlur(1.6F, 1.6F, FilterTileMode.DECAL))) {
            Skia.getCanvas().drawCircle(iconX, iconY, iconSize / 2f, blurPaint);
            Skia.getCanvas().drawCircle(iconX, iconY, iconSize / 2f, paint);
        }

        float textStartX = animX + padding + iconSize + spacing;
        float textAreaW = animX + animW - padding - textStartX;
        float textW = Skia.getStringWidth(toggle.name, textFont);
        float textX = textStartX + Math.max(0f, (textAreaW - textW) / 2f);
        float textY = getTextBaseline(centerY, textFont) - 1.0f;
        Skia.drawText(toggle.name, textX, textY, withAlpha(Color.WHITE, alpha), textFont);
    }

    private void drawWinInfo(ToggleInfo toggle, int alpha) {
        float padding = 6f;
        float iconSize = 12f;
        float spacing = 5f;

        Font textFont = Fonts.getMiSans(iconSize);
        float centerY = animY + animH / 2f;
        float iconX = animX + padding + iconSize / 2f;
        float iconY = centerY + 1f;
        Color accent = withAlpha(new Color(255, 215, 0), alpha);
        int skColor = io.github.humbleui.skija.Color.makeARGB(accent.getAlpha(), accent.getRed(), accent.getGreen(), accent.getBlue());
        try (Paint paint = new Paint().setColor(skColor);
             Paint blurPaint = paint.makeClone().setImageFilter(ImageFilter.makeBlur(1.6F, 1.6F, FilterTileMode.DECAL))) {
            Skia.getCanvas().drawCircle(iconX, iconY, iconSize / 2f, blurPaint);
            Skia.getCanvas().drawCircle(iconX, iconY, iconSize / 2f, paint);
        }

        float textStartX = animX + padding + iconSize + spacing;
        float textAreaW = animX + animW - padding - textStartX;
        float textW = Skia.getStringWidth(toggle.name, textFont);
        float textX = textStartX + Math.max(0f, (textAreaW - textW) / 2f);
        float textY = getTextBaseline(centerY, textFont) - 1.5f;
        Skia.drawText(toggle.name, textX, textY, withAlpha(Color.WHITE, alpha), textFont);
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
        if (toggle.type == ToggleType.SCAFFOLD) return calculateScaffoldWidth();
        if (toggle.type == ToggleType.BLINK) return calculateBlinkWidth();
        float padding = 6f;
        float iconSize = toggle.type == ToggleType.KILL ? 9f : 12f;
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

    private enum ToggleType {
        MODULE,
        SCAFFOLD,
        BLINK,
        KILL,
        WIN
    }

    private record ToggleInfo(String name, boolean enabled, ToggleType type) {
    }
}
