package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.Screenshot;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ModuleInfo(name = "AutoHeypixel", cnName = "自动岛吉吉", description = "Auto play Heypixel server.", category = Category.MISC)
public class AutoHeypixel extends Module {
    public BooleanValue autoScreenshot = ValueBuilder.create(this, "Auto Screenshot").setDefaultBooleanValue(true).build().getBooleanValue();

    @EventTarget
    public void onPacker(EventPacket event) {
        if (mc.player == null || mc.level == null) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundSetTitleTextPacket) {
            boolean win = ((ClientboundSetTitleTextPacket) packet).getText().getString().contains("胜利");
            if (win && autoScreenshot.getCurrentValue()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).thenRun(() -> {
                    mc.execute(() -> {
                        if (mc.player != null && mc.level != null) {
                            Screenshot.grab(
                                    FileManager.clientFolder.getParentFile(),
                                    mc.getMainRenderTarget(),
                                    (message) -> ChatUtils.addChatMessage(message.getString())
                            );
                        }
                    });
                });
            }
        }
    }


}
