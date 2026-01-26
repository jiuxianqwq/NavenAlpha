package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.AntiBots;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.ClientFriend;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Target;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

@ModuleInfo(
        name = "LagRange",
        cnName = "打滑",
        description = "Delay movement packets when targets are in range",
        category = Category.COMBAT
)
public class LagRange extends Module {
    private final Queue<DelayedPacket> delayedPackets = new ArrayDeque<>();
    private int currentDelayTicks;
    private boolean hasTarget;
    private Vec3 serverPosition;
    private Vec3 lastServerPosition;
    private SmoothAnimationTimer smoothX;
    private SmoothAnimationTimer smoothY;
    private SmoothAnimationTimer smoothZ;

    private final FloatValue delay = ValueBuilder.create(this, "Delay (ms)")
            .setDefaultFloatValue(150.0F)
            .setFloatStep(50.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1000.0F)
            .build()
            .getFloatValue();
    private final FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(3.0F)
            .setMaxFloatValue(100.0F)
            .build()
            .getFloatValue();
    private final BooleanValue weaponsOnly = ValueBuilder.create(this, "Weapons Only")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final BooleanValue allowTools = ValueBuilder.create(this, "Allow Tools")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> weaponsOnly.getCurrentValue())
            .build()
            .getBooleanValue();
    private final BooleanValue botCheck = ValueBuilder.create(this, "Bot Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final BooleanValue teams = ValueBuilder.create(this, "Teams")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final ModeValue showPosition = ValueBuilder.create(this, "Show Position")
            .setModes("None", "Default", "HUD")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    @Override
    public void onEnable() {
        if (mc.player != null) {
            serverPosition = mc.player.position();
            lastServerPosition = serverPosition;
            smoothX = new SmoothAnimationTimer((float) serverPosition.x, (float) serverPosition.x, 0.12F);
            smoothY = new SmoothAnimationTimer((float) serverPosition.y, (float) serverPosition.y, 0.12F);
            smoothZ = new SmoothAnimationTimer((float) serverPosition.z, (float) serverPosition.z, 0.12F);
        }
        delayedPackets.clear();
        currentDelayTicks = 0;
        hasTarget = false;
    }

    @Override
    public void onDisable() {
        flushAll();
        delayedPackets.clear();
        currentDelayTicks = 0;
        hasTarget = false;
        serverPosition = null;
        lastServerPosition = null;
        smoothX = null;
        smoothY = null;
        smoothZ = null;
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (event.type() == EventType.PRE) {
            updateTargetState();
            tickDelayedPackets();
        } else if (event.type() == EventType.POST) {
            if (serverPosition != null) {
                lastServerPosition = serverPosition;
            }
        }
    }

    @EventTarget(4)
    public void onPacket(EventPacket event) {
        if (!this.isEnabled() || mc.player == null) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (event.getType() == EventType.SEND) {
            if (shouldResetOnPacket(packet)) {
                currentDelayTicks = 0;
                flushAll();
            }

            if ((currentDelayTicks > 0 || !delayedPackets.isEmpty()) && (packet instanceof ServerboundMovePlayerPacket || packet instanceof ServerboundKeepAlivePacket || packet instanceof ServerboundPongPacket)) {
                event.setCancelled(true);
                delayedPackets.add(new DelayedPacket(packet, currentDelayTicks));
                int maxQueued = Math.max(6, getDelayTicks() * 4);
                if (delayedPackets.size() > maxQueued) {
                    processFlush();
                }
                return;
            }

            if (packet instanceof ServerboundMovePlayerPacket movePacket) {
                updateServerPosition(movePacket);
            }
        }
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (!this.isEnabled()) {
            return;
        }

        if (showPosition.isCurrentMode("None")) {
            return;
        }

        if (!hasTarget || serverPosition == null || lastServerPosition == null) {
            return;
        }

        if (mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        float partialTicks = event.getRenderPartialTicks();
        double x = Mth.lerp(partialTicks, lastServerPosition.x, serverPosition.x);
        double y = Mth.lerp(partialTicks, lastServerPosition.y, serverPosition.y);
        double z = Mth.lerp(partialTicks, lastServerPosition.z, serverPosition.z);

        if (smoothX == null || smoothY == null || smoothZ == null) {
            smoothX = new SmoothAnimationTimer((float) x, (float) x, 0.12F);
            smoothY = new SmoothAnimationTimer((float) y, (float) y, 0.12F);
            smoothZ = new SmoothAnimationTimer((float) z, (float) z, 0.12F);
        }

        smoothX.target = (float) x;
        smoothY.target = (float) y;
        smoothZ.target = (float) z;
        smoothX.update(true);
        smoothY.update(true);
        smoothZ.update(true);

        Color baseColor = showPosition.isCurrentMode("HUD") ? HUD.getColor1() : new Color(255, 255, 255);
        Color drawColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 60);

        PoseStack poseStack = event.getPMatrixStack();
        RenderUtils.drawEntitySolidBox(
                poseStack,
                smoothX.value,
                smoothY.value,
                smoothZ.value,
                mc.player.getBbWidth(),
                mc.player.getBbHeight(),
                drawColor.getRGB()
        );
    }

    private void updateTargetState() {
        float currentRange = range.getCurrentValue();
        hasTarget = false;
        currentDelayTicks = 0;

        if (mc.gameMode == null || mc.gameMode.isDestroying()) {
            if (!delayedPackets.isEmpty()) {
                processFlush();
            }
            return;
        }

        if (weaponsOnly.getCurrentValue() && !isHoldingWeapon()) {
            if (!delayedPackets.isEmpty()) {
                processFlush();
            }
            return;
        }

        Target targetModule = Naven.getInstance().getModuleManager().getModule(Target.class);
        AABB searchBox = mc.player.getBoundingBox().inflate(currentRange);
        for (Entity entity : mc.level.getEntities(mc.player, searchBox, e -> e instanceof LivingEntity)) {
            if (!(entity instanceof Player)) {
                continue;
            }
            if (entity == mc.player) {
                continue;
            }
            if (!entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            if (botCheck.getCurrentValue() && AntiBots.isBot(entity)) {
                continue;
            }
            if (teams.getCurrentValue() && Teams.isSameTeam(entity)) {
                continue;
            }
            if (FriendManager.isFriend(entity) || ClientFriend.isUser(entity)) {
                continue;
            }
            if (targetModule != null && !targetModule.isTarget(entity)) {
                continue;
            }

            hasTarget = true;
            currentDelayTicks = getDelayTicks();
            break;
        }

        if (!hasTarget && !delayedPackets.isEmpty()) {
            processFlush();
        }

        this.setSuffix((int) delay.getCurrentValue() + "ms");
    }

    private boolean isHoldingWeapon() {
        ItemStack stack = mc.player.getMainHandItem();
        Item item = stack.getItem();
        if (item instanceof SwordItem) {
            return true;
        }
        return allowTools.getCurrentValue() && (item instanceof AxeItem || item instanceof PickaxeItem || item instanceof ShovelItem);
    }

    private int getDelayTicks() {
        return Math.max(0, (int) Math.ceil(delay.getCurrentValue() / 50.0F));
    }

    private void tickDelayedPackets() {
        if (delayedPackets.isEmpty()) {
            return;
        }

        Iterator<DelayedPacket> iterator = delayedPackets.iterator();
        while (iterator.hasNext()) {
            DelayedPacket delayed = iterator.next();
            delayed.ticks--;
            if (delayed.ticks <= 0) {
                sendPacket(delayed.packet);
                iterator.remove();
            }
        }
    }

    private void sendPacket(Packet<?> packet) {
        if (packet instanceof ServerboundMovePlayerPacket movePacket) {
            updateServerPosition(movePacket);
        }
        NetworkUtils.sendPacketNoEvent(packet);
    }

    private void flushAll() {
        while (!delayedPackets.isEmpty()) {
            sendPacket(delayedPackets.poll().packet);
        }
    }

    private void processFlush() {
        int count = 0;
        while (!delayedPackets.isEmpty() && count < 3) {
            sendPacket(delayedPackets.poll().packet);
            count++;
        }
    }

    private void updateServerPosition(ServerboundMovePlayerPacket packet) {
        if (mc.player == null) {
            return;
        }
        double x = packet.getX(mc.player.getX());
        double y = packet.getY(mc.player.getY());
        double z = packet.getZ(mc.player.getZ());
        serverPosition = new Vec3(x, y, z);
    }

    private boolean shouldResetOnPacket(Packet<?> packet) {
        if (packet instanceof ServerboundInteractPacket) {
            return true;
        }
        if (packet instanceof ServerboundPlayerActionPacket actionPacket) {
            return actionPacket.getAction() != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM;
        }
        if (packet instanceof ServerboundUseItemPacket) {
            return true;
        }
        return packet instanceof ServerboundUseItemOnPacket;
    }

    private static class DelayedPacket {
        private final Packet<?> packet;
        private int ticks;

        private DelayedPacket(Packet<?> packet, int ticks) {
            this.packet = packet;
            this.ticks = ticks;
        }
    }
}
