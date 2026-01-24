package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.BlockUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ChunkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@ModuleInfo(
        name = "BedESP",
        cnName = "床透视",
        description = "Highlights beds",
        category = Category.RENDER
)
public class BedESP extends Module {
    private static final float[] bedColor = new float[]{1.0F, 0.0F, 0.0F};
    private final List<AABB> renderBoundingBoxes = new CopyOnWriteArrayList<>();

    @EventTarget
    public void onRespawn(EventRespawn e) {
        this.renderBoundingBoxes.clear();
    }

    @EventTarget
    public void onTick(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            ArrayList<BlockEntity> blockEntities = ChunkUtils.getLoadedBlockEntities().collect(Collectors.toCollection(ArrayList::new));
            this.renderBoundingBoxes.clear();

            for (BlockEntity blockEntity : blockEntities) {
                if (blockEntity instanceof BedBlockEntity bedBE) {
                    AABB box = BlockUtils.getBoundingBox(bedBE.getBlockPos());
                    this.renderBoundingBoxes.add(box);
                }
            }
        }
    }

    @EventTarget
    public void onRender(EventRender e) {
        PoseStack stack = e.getPMatrixStack();
        stack.pushPose();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        RenderSystem.setShaderColor(bedColor[0], bedColor[1], bedColor[2], 0.25F);

        for (AABB box : this.renderBoundingBoxes) {
            RenderUtils.装女人(bufferBuilder, stack.last().pose(), box);
        }

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        stack.popPose();
    }
}
