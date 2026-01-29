package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.PostProcess;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.shader.impl.KawaseBlur;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.context.SkiaContext;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import io.github.humbleui.skija.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainMenuScreen extends Screen {
    private static final Minecraft mc = Minecraft.getInstance();
    private final List<MenuButton> buttons = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    public MainMenuScreen() {
        super(Component.nullToEmpty(Naven.CLIENT_DISPLAY_NAME));
    }

    @Override
    protected void init() {
        if (this.buttons.isEmpty()) {
            this.buttons.add(new MenuButton("Singleplayer", () -> mc.setScreen(new SelectWorldScreen(this))));
            this.buttons.add(new MenuButton("Multiplayer", () -> mc.setScreen(new JoinMultiplayerScreen(this))));
            this.buttons.add(new MenuButton("Settings", () -> mc.setScreen(new OptionsScreen(this, mc.options))));
            this.buttons.add(new MenuButton("Exit", () -> mc.close()));
        }
        if (this.particles.isEmpty()) {
            for (int i = 0; i < 80; i++) {
                this.particles.add(new Particle(random.nextFloat() * this.width, random.nextFloat() * this.height, randomSpeed(), randomSpeed(), 2.0F + random.nextFloat() * 2.0F));
            }
        }
    }

    @Override
    public void tick() {
        for (Particle particle : this.particles) {
            particle.x += particle.vx;
            particle.y += particle.vy;
            if (particle.x < -10.0F || particle.x > this.width + 10.0F || particle.y < -10.0F || particle.y > this.height + 10.0F) {
                particle.x = random.nextFloat() * this.width;
                particle.y = random.nextFloat() * this.height;
                particle.vx = randomSpeed();
                particle.vy = randomSpeed();
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PostProcess postProcess = Naven.getInstance().getModuleManager().getModule(PostProcess.class);
        int blurStrength = postProcess != null ? postProcess.getStrength() : 0;

        float buttonWidth = 160.0F;
        float buttonHeight = 24.0F;
        float buttonGap = 10.0F;
        float panelPadding = 18.0F;
        float panelHeaderHeight = 56.0F;
        float panelWidth = buttonWidth + panelPadding * 2.0F;
        float panelHeight = this.buttons.size() * (buttonHeight + buttonGap) - buttonGap + panelHeaderHeight + panelPadding;
        float panelX = this.width / 2.0F - panelWidth / 2.0F;
        float panelY = this.height / 2.0F - panelHeight / 2.0F;
        float panelRadius = 10.0F;

        SkiaContext.draw((context) -> {
            Skia.save();
            Skia.scale((float) mc.getWindow().getGuiScale());

            Skia.drawImage("mainmenu/cat.png", 0.0F, 0.0F, this.width, this.height);

            for (Particle particle : this.particles) {
                Skia.drawCircle(particle.x, particle.y, particle.size / 2.0F, new Color(255, 255, 255, 80));
            }

            Skia.restore();
        });

        if (blurStrength > 0) {
            KawaseBlur.GUI_BLUR.draw(blurStrength);
        }

        SkiaContext.draw((context) -> {
            Skia.save();
            Skia.scale((float) mc.getWindow().getGuiScale());

            Skia.drawShadow(panelX, panelY, panelWidth, panelHeight, panelRadius);
            if (blurStrength > 0) {
                Skia.drawRoundedGuiBlur(panelX, panelY, panelWidth, panelHeight, panelRadius);
            }
            Skia.drawRoundedRect(panelX, panelY, panelWidth, panelHeight, panelRadius, new Color(0, 0, 0, 90));

            renderTitle(panelX, panelY);

            float startY = panelY + panelHeaderHeight;
            Font buttonFont = Fonts.getMiSans(16.0F);
            for (MenuButton button : this.buttons) {
                button.x = this.width / 2.0F - buttonWidth / 2.0F;
                button.y = startY;
                button.width = buttonWidth;
                button.height = buttonHeight;
                button.render(mouseX, mouseY, buttonFont);
                startY += buttonHeight + buttonGap;
            }

            Skia.flushGlows();
            Skia.restore();
        });

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (MenuButton menuButton : this.buttons) {
                if (menuButton.isHovering((int) mouseX, (int) mouseY)) {
                    menuButton.onClick.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTitle(float panelX, float panelY) {
        String text = Naven.CLIENT_DISPLAY_NAME;
        int splitPoint = Math.min(5, text.length());
        String colorPart = text.substring(0, splitPoint);
        String whitePart = text.substring(splitPoint);
        Color accent = new Color(120, 180, 255, 255);
        Color normal = new Color(255, 255, 255, 255);
        Font font = Fonts.getMiSans(28.0F);
        float colorWidth = Skia.getStringWidth(colorPart, font);
        float whiteWidth = Skia.getStringWidth(whitePart, font);
        float totalWidth = colorWidth + whiteWidth;
        float x = this.width / 2.0F - totalWidth / 2.0F;
        float y = panelY + 18.0F;
        Skia.drawText(colorPart, x, y, accent, font);
        Skia.drawText(whitePart, x + colorWidth, y, normal, font);
    }

    private float randomSpeed() {
        float speed = 0.3F + random.nextFloat() * 0.6F;
        return random.nextBoolean() ? speed : -speed;
    }

    private static class MenuButton {
        private final String text;
        private final Runnable onClick;
        private float x;
        private float y;
        private float width;
        private float height;

        private MenuButton(String text, Runnable onClick) {
            this.text = text;
            this.onClick = onClick;
        }

        private void render(int mouseX, int mouseY, Font font) {
            boolean hovering = isHovering(mouseX, mouseY);
            Color baseColor = hovering ? new Color(30, 30, 35, 170) : new Color(20, 20, 25, 140);
            Skia.drawRoundedRect(x, y, width, height, 6.0F, baseColor);
            Skia.drawFullCenteredText(text, x + width / 2.0F, y + height / 2.0F, new Color(255, 255, 255, 230), font);
        }

        private boolean isHovering(int mouseX, int mouseY) {
            return RenderUtils.isHoveringBound(mouseX, mouseY, x, y, width, height);
        }
    }

    private static class Particle {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private final float size;

        private Particle(float x, float y, float vx, float vy, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
        }
    }
}
