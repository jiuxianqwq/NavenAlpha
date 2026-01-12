package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

import java.lang.reflect.Method;
import java.util.Base64;

@ModuleInfo(
        name = "ClientFriend",
        cnName = "客户端朋友",
        description = "Treat other users as friend!",
        category = Category.MISC
)
public class ClientFriend extends Module {
    public static boolean isUser(Entity entity) {
        return Naven.getInstance().getModuleManager().getModule(ClientFriend.class).isEnabled() && AuthUtils.transport.isUser(entity.getName().getString());
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getPacket() instanceof ServerboundInteractPacket) {
            if (AuthUtils.transport == null)
                try {
                    Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                    Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                    exit.invoke(null, 0);
                } catch (Exception ex) {
                }
            if (mc.hitResult instanceof EntityHitResult entityHitResult && AuthUtils.transport.isUser(entityHitResult.getEntity().getName().getString())) {
                event.setCancelled(true);
                ChatUtils.addChatMessage("无法攻击相同客户端的用户，请关闭" + this.getName() + "模块");
            }
        }
    }
}
