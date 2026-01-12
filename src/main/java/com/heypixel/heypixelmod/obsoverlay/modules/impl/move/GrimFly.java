package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPongPacket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(
        name = "GrimFly",
        cnName = "浪子闲话",
        description = "Attempts to desync brief keepalive timing after knockback",
        category = Category.MOVEMENT
)
public class GrimFly extends Module {
    private final Queue<Packet<?>> delayedPackets = new ConcurrentLinkedQueue<>();
    private boolean intercepting;
    private boolean shouldStartFallFlying;
    private int ticksUntilFallFlying;

    @Override
    public void onEnable() {
        resetState(false);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        flushDelayedPackets();
        resetState(false);
        super.onDisable();
    }

    @EventTarget
    public void onRespawn(EventRespawn event) {
        if (this.isEnabled()) {
            this.setEnabled(false);
        }
        resetState(true);
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (!shouldStartFallFlying || mc.player == null) {
            return;
        }

        ticksUntilFallFlying++;
        if (ticksUntilFallFlying < 8) {
            return;
        }

        NetworkUtils.sendPacketNoEvent(
                new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING)
        );
        shouldStartFallFlying = false;
        ticksUntilFallFlying = 0;
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (event.getType() == EventType.SEND) {
            if (intercepting && packet instanceof ServerboundPongPacket) {
                event.setCancelled(true);
                if (delayedPackets.isEmpty()) {
                    shouldStartFallFlying = true;
                    ticksUntilFallFlying = 0;
                }
                delayedPackets.add(packet);
                if (delayedPackets.size() > 200) {
                    flushAndStopIntercepting();
                }
                return;
            }

            if (packet instanceof ServerboundInteractPacket) {
                if (intercepting && !delayedPackets.isEmpty()) {
                    flushAndStopIntercepting();
                }
                return;
            }
        }

        if (event.getType() == EventType.RECEIVE) {
            if (packet instanceof ClientboundPlayerPositionPacket) {
                if (intercepting && !delayedPackets.isEmpty()) {
                    flushAndStopIntercepting();
                }
                return;
            }

            if (packet instanceof ClientboundSetEntityMotionPacket motionPacket && motionPacket.getId() == mc.player.getId()) {
                if (intercepting || !delayedPackets.isEmpty()) {
                    return;
                }
                intercepting = true;
                shouldStartFallFlying = false;
                ticksUntilFallFlying = 0;
                event.setCancelled(true);
            }
        }
    }

    private void flushAndStopIntercepting() {
        flushDelayedPackets();
        intercepting = false;
        shouldStartFallFlying = false;
        ticksUntilFallFlying = 0;
    }

    private void flushDelayedPackets() {
        while (!delayedPackets.isEmpty()) {
            NetworkUtils.sendPacketNoEvent(delayedPackets.poll());
        }
    }

    private void resetState(boolean clearQueue) {
        intercepting = false;
        shouldStartFallFlying = false;
        ticksUntilFallFlying = 0;
        if (clearQueue) {
            delayedPackets.clear();
        }
    }
}
