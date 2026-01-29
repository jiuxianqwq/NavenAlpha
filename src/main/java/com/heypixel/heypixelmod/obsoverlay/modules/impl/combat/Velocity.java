package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.HUDEditor;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.vector.Vector3d;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Path;
import io.github.humbleui.types.RRect;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(name = "Velocity", cnName = "反击退", description = "Reduces knockback.", category = Category.COMBAT)
public class Velocity extends Module {

    //    private final BooleanValue logging = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();
    private final ModeValue mode = ValueBuilder.create(this, "Mode").setModes("Legit", "NoXZ").setDefaultModeIndex(0).build().getModeValue();
    private final Queue<Packet<? super ClientPacketListener>> packets = new ConcurrentLinkedQueue<>();
    private final FloatValue attacks = ValueBuilder.create(this, "AttackCounts").setDefaultFloatValue(3.0F).setFloatStep(1.0F).setMinFloatValue(1.0F).setMaxFloatValue(5.0F).setVisibility(() -> mode.isCurrentMode("NoXZ")).build().getFloatValue();
    private final FloatValue alinkTime = ValueBuilder.create(this, "MaxAlinkTime (ms)").setDefaultFloatValue(5000.0F).setFloatStep(50.0F).setMinFloatValue(50.0F).setMaxFloatValue(10000.0F).setVisibility(() -> mode.isCurrentMode("NoXZ")).build().getFloatValue();
    private final DragValue alinkHudPosition = ValueBuilder.create(this, "AlinkHUD Position")
            .setDefaultX(0f)
            .setDefaultY(0f)
            .build()
            .getDragValue();
    private final SmoothAnimationTimer alinkHudAlpha = new SmoothAnimationTimer(1.0F, 0.0F, 0.2F);
    private final SmoothAnimationTimer alinkProgress = new SmoothAnimationTimer(0.0F, 0.0F, 0.12F);
    private final Map<Entity, Vector3d> targets = new HashMap<>();
    public boolean lag;
    private boolean jump;
    private Vec3 velocity;
    private long velocityTime;
    private Player target;
    private VelocityStage stage;
    private boolean positionInitialized;


//    private void log(String message) {
//        if (this.logging.getCurrentValue()) {
//            ChatUtils.addChatMessage(message);
//        }
//    }

    @EventTarget
    public void onPacket(EventPacket event) {
        switch (mode.getCurrentMode()) {
            case "NoXZ" -> {
                if (event.getPacket() instanceof ClientboundPlayerPositionPacket && stage == VelocityStage.NONE) {
                    lag = true;
                    return;
                }
                if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.getId() == mc.player.getId()) {
                    if (stage == VelocityStage.NONE) {
                        if (!lag) {
                            stage = VelocityStage.DELAY;
                            velocityTime = System.currentTimeMillis();
                            event.setCancelled(true);
                            velocity = new Vec3(packet.getXa() / 8000.0D, packet.getYa() / 8000.0D, packet.getZa() / 8000.0D);

                        } else {
                            lag = false;
                        }
                        return;
                    } else {
                        velocity = new Vec3(packet.getXa() / 8000.0D, packet.getYa() / 8000.0D, packet.getZa() / 8000.0D);
                        stage = VelocityStage.LAG;
                        event.setCancelled(true);
                        return;
                    }
                }
                if (stage != VelocityStage.NONE && event.getType() == EventType.RECEIVE) {
                    Packet<? super ClientPacketListener> packet = (Packet<? super ClientPacketListener>) event.getPacket();

                    if (packet instanceof ClientboundPlayerPositionPacket) {
                        stage = VelocityStage.LAG;
                        return;
                    }

                    if (packet instanceof ClientboundPlayerLookAtPacket) {
                        stage = VelocityStage.LAG;
                        return;
                    }

                    if (packet instanceof ClientboundDisconnectPacket || packet instanceof ClientboundRespawnPacket) {
                        clear(false);
                        return;
                    }

                    if (!(packet instanceof ClientboundPingPacket) && !(packet instanceof ClientboundMoveEntityPacket) && !(packet instanceof ClientboundTeleportEntityPacket)) {
                        return;
                    }

                    if (packet instanceof ClientboundMoveEntityPacket movePacket) {
                        Entity entity = movePacket.getEntity(mc.level);
                        if (entity != null) {
                            Vector3d currentPos = targets.getOrDefault(entity, new Vector3d(entity.getX(), entity.getY(), entity.getZ()));

                            if (movePacket.hasPosition()) {
                                double dx = movePacket.getXa() / 4096.0D;
                                double dy = movePacket.getYa() / 4096.0D;
                                double dz = movePacket.getZa() / 4096.0D;

                                targets.put(entity, new Vector3d(currentPos.getX() + dx, currentPos.getY() + dy, currentPos.getZ() + dz));
                            }
                        }
                    }

                    if (packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
                        Entity entity = mc.level.getEntity(teleportPacket.getId());
                        if (entity != null) {
                            targets.put(entity, new Vector3d(teleportPacket.getX(), teleportPacket.getY(), teleportPacket.getZ()));
                        }
                    }

                    packets.add(packet);
                    event.setCancelled(true);
                }
            }

            case "Legit" -> {
                if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.getId() == mc.player.getId()) {
                    jump = true;
                }
            }
        }
    }

    @EventTarget
    public void onPreTick(EventRunTicks event) {
        if (mc.player == null) return;
        if (event.type() == EventType.POST && stage == VelocityStage.CLEAR) {
            clear(true);
        }
        if (event.type() == EventType.POST && stage == VelocityStage.LAG) {
            mc.player.setDeltaMovement(velocity.x, velocity.y, velocity.z);
            clear(true);
        }
        if (event.type() != EventType.PRE) return;

        switch (mode.getCurrentMode()) {
            case "NoXZ" -> {
                if (stage == VelocityStage.ATTACK) {
                    if (mc.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof Player player && !AntiBots.isBot(player)) {
                        var motionXZ = 1.0D;
                        for (int i = 0; i < ((int) attacks.getCurrentValue()); i++) {
                            if (mc.player.isSprinting()) mc.player.setSprinting(false);
                            mc.gameMode.attack(mc.player, target);
                            mc.player.swing(InteractionHand.MAIN_HAND);
                            motionXZ *= 0.6D;
                        }
                        mc.player.setDeltaMovement(velocity.x * motionXZ, velocity.y, velocity.z * motionXZ);
//                    clear(true);
                        stage = VelocityStage.CLEAR;
                    }
                } else if (System.currentTimeMillis() - velocityTime >= alinkTime.getCurrentValue() && stage == VelocityStage.DELAY) {
                    mc.player.setDeltaMovement(velocity.x, velocity.y, velocity.z);
//                    clear(true);
                    stage = VelocityStage.CLEAR;
                }

                if (lag && mc.player.hurtTime == 0) lag = false;

            }
        }

        this.setSuffix(mode.getCurrentMode() + (stage == VelocityStage.DELAY ? " Alink " + (System.currentTimeMillis() - velocityTime) / 50 + "Ticks" : ""));

    }

    public void clear(boolean handle) {
        lag = false;
        stage = VelocityStage.NONE;
        targets.clear();
        target = null;
        if (!handle) {
            packets.clear();
            return;
        }
        while (!packets.isEmpty()) {
            Packet<? super ClientPacketListener> packet = packets.poll();
            if (packet != null && mc.getConnection() != null) {
                packet.handle(mc.getConnection());
            }
        }
    }

    public boolean isAlinkActive() {
        return mode.isCurrentMode("NoXZ") && stage != VelocityStage.NONE;
    }

    @Override
    public void onEnable() {
        jump = false;
        lag = false;
        targets.clear();
        target = null;
        stage = VelocityStage.NONE;
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        switch (mode.getCurrentMode()) {
            case "NoXZ" -> {
                if (stage == VelocityStage.DELAY && velocity != null && mc.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof Player player && !AntiBots.isBot(player)) {
                    event.setForward(1);
                    event.setStrafe(0);
                    stage = VelocityStage.ATTACK;
                    this.target = player;
                }

            }
        }
        if (jump) {
            if (mc.player.onGround() && MoveUtils.isMoving()) event.setJump(true);
            jump = false;
        }
    }

    @Override
    public void onDisable() {
        jump = false;
        lag = false;
        targets.clear();
        target = null;
        stage = VelocityStage.NONE;
        clear(true);
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (stage == VelocityStage.NONE) return;
        PoseStack poseStack = event.getPMatrixStack();
        for (Entity entity : targets.keySet()) {
            if (!(entity instanceof Player)) continue;
            Vector3d pos = targets.get(entity);
            if (entity.equals(target))
                RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(), entity.getBbWidth(), entity.getBbHeight(), new Color(200, 0, 0, 60).getRGB());
            else
                RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(), entity.getBbWidth(), entity.getBbHeight(), new Color(0, 200, 0, 60).getRGB());
        }
    }

    @EventTarget
    public void onRenderSkia(EventRenderSkia event) {
        boolean inEditor = mc.screen instanceof HUDEditor;
        boolean noXZ = mode.isCurrentMode("NoXZ");
        boolean active = stage != null && stage != VelocityStage.NONE;
        boolean shouldRender = inEditor || (this.isEnabled() && noXZ);
        alinkHudAlpha.target = 1.0F;
        alinkHudAlpha.update(shouldRender);
        if (alinkHudAlpha.value <= 0.01f) return;

        Font font = Fonts.getMiSans(10.0f);
        long now = System.currentTimeMillis();
        long maxMs = (long) alinkTime.getCurrentValue();
        long elapsed = active && stage == VelocityStage.DELAY ? Math.max(0L, now - velocityTime) : 0L;
        float progress;
        if (inEditor) {
            progress = 0.6f;
        } else if (maxMs <= 0L) {
            progress = 0.0f;
        } else if (active && stage == VelocityStage.DELAY) {
            progress = Math.min(1.0f, elapsed / (float) maxMs);
        } else if (active && stage == VelocityStage.ATTACK) {
            progress = 1.0f;
        } else {
            progress = 0.0f;
        }
        alinkProgress.target = progress;
        alinkProgress.update(true);

        String title = "Alink";
        float textWidth = Skia.getStringWidth(title, font);
        float height = 32.0f;
        float minWidth = 120.0f;
        float width = Math.max(minWidth, textWidth + 20.0f);
        float radius = 10.0f;

        if (!positionInitialized && alinkHudPosition.getX() == alinkHudPosition.getDefaultX() && alinkHudPosition.getY() == alinkHudPosition.getDefaultY()) {
            float screenWidth = mc.getWindow().getGuiScaledWidth();
            float screenHeight = mc.getWindow().getGuiScaledHeight();
            float defaultX = screenWidth / 2.0f - width / 2.0f;
            float defaultY = screenHeight - 55.0f;
            alinkHudPosition.setPosition(defaultX, defaultY);
            positionInitialized = true;
        }

        float x = alinkHudPosition.getX();
        float y = alinkHudPosition.getY();
        int bgAlpha = Math.min(255, Math.max(0, (int) (80.0f * alinkHudAlpha.value)));
        Color bgColor = new Color(0, 0, 0, bgAlpha);
        Skia.drawShadow(x, y, width, height, radius);
        Skia.drawRoundedBlur(x, y, width, height, radius);
        Skia.drawRoundedRect(x, y, width, height, radius, bgColor);

        float padding = 5.0f;
        float barHeight = 6.0f;
        float barX = x + padding;
        float barY = y + height - padding - barHeight;
        float barWidth = width - padding * 2.0f;
        float fillWidth = barWidth * alinkProgress.value;
        float barRadius = barHeight / 2.0f;
        Path barPath = new Path();
        barPath.addRRect(RRect.makeXYWH(barX, barY, barWidth, barHeight, barRadius));
        Skia.save();
        Skia.getCanvas().clipPath(barPath, ClipMode.INTERSECT, true);
        Skia.drawRoundedRect(barX, barY, barWidth, barHeight, barRadius, new Color(255, 255, 255, 50));
        if (fillWidth > 0.5f) {
            Skia.drawRoundedRect(barX, barY, fillWidth, barHeight, barRadius, new Color(196, 128, 224, 220));
        }
        Skia.restore();
        barPath.close();

        float titleCenterX = x + width / 2.0f;
        float titleCenterY = y + 7.0f;
        Skia.drawFullCenteredText(title, titleCenterX, titleCenterY, new Color(255, 255, 255, (int) (220.0f * alinkHudAlpha.value)), font);
        String percentText = Math.round(alinkProgress.value * 100.0f) + "%";
        float percentY = y + 17.0f;
        Skia.drawFullCenteredText(percentText, x + width / 2.0f, percentY, new Color(255, 255, 255, (int) (230.0f * alinkHudAlpha.value)), font);

        alinkHudPosition.setWidth(width);
        alinkHudPosition.setHeight(height);
    }

    private enum VelocityStage {
        NONE, DELAY, ATTACK, CLEAR, LAG
    }
}
