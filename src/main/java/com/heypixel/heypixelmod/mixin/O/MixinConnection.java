package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventGlobalPacket;
import com.heypixel.heypixelmod.obsoverlay.utils.HttpUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.net.InetSocketAddress;

@Mixin({Connection.class})
public abstract class MixinConnection extends SimpleChannelInboundHandler<Packet<?>> {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    private static <T extends PacketListener> void genericsFtw(Packet<T> pPacket, PacketListener pListener) {
    }

    @Inject(
            method = {"connectToServer"},
            at = {@At("HEAD")}
    )
    private static void injectHook(InetSocketAddress p_178301_, boolean p_178302_, CallbackInfoReturnable<Connection> cir) {
        try {
            HttpUtils.get("http://127.0.0.1:23233/api/setHook?hook=1");
        } catch (IOException var4) {
        }
    }

    @Inject(
            method = {"connect"},
            at = {@At("HEAD")}
    )
    private static void injectHook2(InetSocketAddress inetSocketAddress, boolean bl, Connection arg, CallbackInfoReturnable<ChannelFuture> cir) {
        try {
            HttpUtils.get("http://127.0.0.1:23233/api/setHook?hook=1");
        } catch (IOException var5) {
        }
    }

    @Shadow
    protected abstract void sendPacket(Packet<?> var1, @Nullable PacketSendListener var2);

    @Redirect(
            method = {"channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"
            )
    )
    private void onGenericsFtw(Packet<?> pPacket, PacketListener pListener) {
        EventGlobalPacket event = new EventGlobalPacket(EventType.RECEIVE, pPacket);
        Naven.getInstance().getEventManager().call(event);
        if (!event.isCancelled()) {
            genericsFtw(event.getPacket(), pListener);
        }
    }

    @Redirect(
            method = {"send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Connection;sendPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V"
            )
    )
    private void onSend(Connection instance, Packet<?> pInPacket, PacketSendListener pFutureListeners) {
        if (NetworkUtils.passthroughsPackets.contains(pInPacket)) {
            NetworkUtils.passthroughsPackets.remove(pInPacket);
            this.sendPacket(pInPacket, pFutureListeners);
        } else {
            EventGlobalPacket event = new EventGlobalPacket(EventType.SEND, pInPacket);
            Naven.getInstance().getEventManager().call(event);
            if (!event.isCancelled()) {
                this.sendPacket(event.getPacket(), pFutureListeners);
            }
        }
    }
}
