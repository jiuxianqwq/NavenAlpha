package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.ColorUtil;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInfo(
        name = "Particles",
        cnName = "粒子",
        description = "Renders world particles",
        category = Category.RENDER
)
public class Particles extends Module {
    private static final ResourceLocation TEX_FIREFLY = ResourceLocation.fromNamespaceAndPath("heypixel", "vcx6svvqmet8/particles/firefly.png");
    private static final ResourceLocation TEX_SNOWFLAKE = ResourceLocation.fromNamespaceAndPath("heypixel", "vcx6svvqmet8/particles/snowflake.png");
    private static final ResourceLocation TEX_STAR = ResourceLocation.fromNamespaceAndPath("heypixel", "vcx6svvqmet8/particles/star.png");
    private static final ResourceLocation TEX_HEART = ResourceLocation.fromNamespaceAndPath("heypixel", "vcx6svvqmet8/particles/heart.png");
    private static final ResourceLocation TEX_DOLLAR = ResourceLocation.fromNamespaceAndPath("heypixel", "vcx6svvqmet8/particles/dollar.png");

    private final BooleanValue fireFlies = ValueBuilder.create(this, "FireFlies")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final FloatValue fireFlyCount = ValueBuilder.create(this, "FFCount")
            .setDefaultFloatValue(30.0F)
            .setMinFloatValue(20.0F)
            .setMaxFloatValue(200.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> fireFlies.getCurrentValue())
            .build()
            .getFloatValue();

    private final FloatValue fireFlySize = ValueBuilder.create(this, "FFSize")
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(2.0F)
            .setFloatStep(0.05F)
            .setVisibility(() -> fireFlies.getCurrentValue())
            .build()
            .getFloatValue();

    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Off", "SnowFlake", "Stars", "Hearts", "Dollars", "Bloom")
            .setDefaultModeIndex(1)
            .build()
            .getModeValue();

    private final FloatValue count = ValueBuilder.create(this, "Count")
            .setDefaultFloatValue(100.0F)
            .setMinFloatValue(20.0F)
            .setMaxFloatValue(800.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> !mode.isCurrentMode("Off"))
            .build()
            .getFloatValue();

    private final FloatValue size = ValueBuilder.create(this, "Size")
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(6.0F)
            .setFloatStep(0.1F)
            .setVisibility(() -> !mode.isCurrentMode("Off"))
            .build()
            .getFloatValue();

    private final ModeValue physics = ValueBuilder.create(this, "Physics")
            .setModes("Drop", "Fly")
            .setDefaultModeIndex(1)
            .setVisibility(() -> !mode.isCurrentMode("Off"))
            .build()
            .getModeValue();

    private final BooleanValue syncColor = ValueBuilder.create(this, "Sync Color")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final FloatValue colorSpeed = ValueBuilder.create(this, "Color Speed")
            .setDefaultFloatValue(10.0F)
            .setMinFloatValue(2.0F)
            .setMaxFloatValue(30.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> syncColor.getCurrentValue())
            .build()
            .getFloatValue();

    private final Random random = new Random();

    private final ArrayList<ParticleBase> fireFlyParticles = new ArrayList<>();
    private final ArrayList<ParticleBase> particles = new ArrayList<>();

    @Override
    public void onEnable() {
        fireFlyParticles.clear();
        particles.clear();
    }

    @Override
    public void onDisable() {
        fireFlyParticles.clear();
        particles.clear();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.level == null) return;

        fireFlyParticles.removeIf(ParticleBase::tick);
        particles.removeIf(ParticleBase::tick);

        spawnFireflies();
        spawnParticles();
    }

    @EventTarget
    public void onRender3D(EventRender event) {
        if (mc.player == null || mc.level == null) return;

        PoseStack stack = event.getPMatrixStack();
        float partialTicks = event.getRenderPartialTicks();

        if (fireFlies.getCurrentValue() && !fireFlyParticles.isEmpty()) {
            renderList(stack, partialTicks, TEX_FIREFLY, fireFlyParticles, fireFlySize.getCurrentValue(), true);
        }

        if (!mode.isCurrentMode("Off") && !particles.isEmpty()) {
            ResourceLocation tex = getModeTexture();
            if (tex != null) {
                renderList(stack, partialTicks, tex, particles, size.getCurrentValue(), false);
            }
        }
    }

    private void renderList(PoseStack stack, float partialTicks, ResourceLocation texture, List<ParticleBase> list, float quadSize, boolean fireFly) {
        stack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (ParticleBase p : list) {
            p.render(stack, buffer, partialTicks, quadSize, getColor(p.age * (fireFly ? 10 : 2)), fireFly);
        }
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        stack.popPose();
    }

    private ResourceLocation getModeTexture() {
        if (mode.isCurrentMode("Bloom")) return TEX_FIREFLY;
        if (mode.isCurrentMode("SnowFlake")) return TEX_SNOWFLAKE;
        if (mode.isCurrentMode("Stars")) return TEX_STAR;
        if (mode.isCurrentMode("Hearts")) return TEX_HEART;
        if (mode.isCurrentMode("Dollars")) return TEX_DOLLAR;
        return null;
    }

    private int getColor(int counter) {
        if (syncColor.getCurrentValue()) {
            return ColorUtil.interpolateColorsBackAndForth((int) colorSpeed.getCurrentValue(), -counter, HUD.getColor1(), HUD.getColor2(), false).getRGB();
        }
        return -1;
    }

    private void spawnFireflies() {
        if (!fireFlies.getCurrentValue()) return;

        int target = (int) fireFlyCount.getCurrentValue();
        for (int i = fireFlyParticles.size(); i < target; i++) {
            fireFlyParticles.add(new FireFly(
                    (float) (mc.player.getX() + random(-25.0F, 25.0F)),
                    (float) (mc.player.getY() + random(2.0F, 15.0F)),
                    (float) (mc.player.getZ() + random(-25.0F, 25.0F)),
                    random(-0.2F, 0.2F),
                    random(-0.1F, 0.1F),
                    random(-0.2F, 0.2F)
            ));
        }
    }

    private void spawnParticles() {
        if (mode.isCurrentMode("Off")) return;

        int target = (int) count.getCurrentValue();
        boolean drop = physics.isCurrentMode("Drop");
        for (int i = particles.size(); i < target; i++) {
            particles.add(new ParticleBase(
                    (float) (mc.player.getX() + random(-48.0F, 48.0F)),
                    (float) (mc.player.getY() + random(2.0F, 48.0F)),
                    (float) (mc.player.getZ() + random(-48.0F, 48.0F)),
                    drop ? 0.0F : random(-0.4F, 0.4F),
                    drop ? random(-0.2F, -0.05F) : random(-0.1F, 0.1F),
                    drop ? 0.0F : random(-0.4F, 0.4F)
            ));
        }
    }

    private float random(float min, float max) {
        return min + (max - min) * random.nextFloat();
    }

    private class FireFly extends ParticleBase {
        private final ArrayList<TrailPoint> trail = new ArrayList<>();

        private FireFly(float posX, float posY, float posZ, float motionX, float motionY, float motionZ) {
            super(posX, posY, posZ, motionX, motionY, motionZ);
        }

        @Override
        public boolean tick() {
            if (mc.player == null || mc.level == null) return true;

            double dx = mc.player.getX() - posX;
            double dy = mc.player.getY() - posY;
            double dz = mc.player.getZ() - posZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > 100.0) {
                age -= 4;
            } else if (!mc.level.getBlockState(BlockPos.containing(posX, posY, posZ)).isAir()) {
                age -= 8;
            } else {
                age--;
            }

            if (age < 0) return true;

            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            trail.add(new TrailPoint(posX, posY, posZ, age));
            if (trail.size() > 20) {
                trail.remove(0);
            }

            motionX *= 0.99F;
            motionY *= 0.99F;
            motionZ *= 0.99F;

            return false;
        }

        @Override
        public void render(PoseStack stack, BufferBuilder buffer, float partialTicks, float quadSize, int rgb, boolean fireFly) {
            int trailSize = trail.size();
            if (trailSize > 1) {
                for (int i = 0; i < trailSize; i++) {
                    TrailPoint tp = trail.get(i);
                    float k = (float) i / (float) trailSize;
                    int alpha = (int) (255.0F * (age / (float) maxAge) * k);
                    renderAt(stack, buffer, partialTicks, tp.x, tp.y, tp.z, quadSize, applyAlpha(rgb, alpha));
                }
            }
            super.render(stack, buffer, partialTicks, quadSize, rgb, true);
        }
    }

    private class ParticleBase {
        protected float prevposX;
        protected float prevposY;
        protected float prevposZ;
        protected float posX;
        protected float posY;
        protected float posZ;
        protected float motionX;
        protected float motionY;
        protected float motionZ;
        protected int age;
        protected int maxAge;

        private ParticleBase(float posX, float posY, float posZ, float motionX, float motionY, float motionZ) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.prevposX = posX;
            this.prevposY = posY;
            this.prevposZ = posZ;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.age = (int) random(100.0F, 300.0F);
            this.maxAge = this.age;
        }

        public boolean tick() {
            if (mc.player == null) return true;

            double dx = mc.player.getX() - posX;
            double dy = mc.player.getY() - posY;
            double dz = mc.player.getZ() - posZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > 4096.0) {
                age -= 8;
            } else {
                age--;
            }

            if (age < 0) return true;

            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            motionX *= 0.9F;
            if (physics.isCurrentMode("Fly")) {
                motionY *= 0.9F;
            }
            motionZ *= 0.9F;
            motionY -= 0.001F;

            return false;
        }

        public void render(PoseStack stack, BufferBuilder buffer, float partialTicks, float quadSize, int rgb, boolean fireFly) {
            float x = (float) Mth.lerp(partialTicks, prevposX, posX);
            float y = (float) Mth.lerp(partialTicks, prevposY, posY);
            float z = (float) Mth.lerp(partialTicks, prevposZ, posZ);

            int alpha = (int) (255.0F * ((float) age / (float) maxAge));
            renderAt(stack, buffer, partialTicks, x, y, z, quadSize, applyAlpha(rgb, alpha));
        }

        protected void renderAt(PoseStack stack, BufferBuilder buffer, float partialTicks, float x, float y, float z, float quadSize, int argb) {
            Vec3 cameraPos = RenderUtils.getCameraPos();
            Camera camera = mc.gameRenderer.getMainCamera();

            stack.pushPose();
            stack.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
            stack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
            stack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

            float a = (float) (argb >> 24 & 0xFF) / 255.0F;
            float r = (float) (argb >> 16 & 0xFF) / 255.0F;
            float g = (float) (argb >> 8 & 0xFF) / 255.0F;
            float b = (float) (argb & 0xFF) / 255.0F;

            var matrix = stack.last().pose();
            buffer.vertex(matrix, -quadSize, -quadSize, 0.0F).uv(0.0F, 1.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, quadSize, -quadSize, 0.0F).uv(1.0F, 1.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, quadSize, quadSize, 0.0F).uv(1.0F, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, -quadSize, quadSize, 0.0F).uv(0.0F, 0.0F).color(r, g, b, a).endVertex();

            stack.popPose();
        }
    }

    private static int applyAlpha(int rgb, int alpha) {
        alpha = Mth.clamp(alpha, 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private record TrailPoint(float x, float y, float z, int age) {
    }
}
