package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;

import java.lang.reflect.Method;
import java.util.Base64;

@CommandInfo(
        name = "config",
        description = "Open client config folder or manage cloud configs.",
        aliases = {"conf"}
)
public class CommandConfig extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length > 0) {
            if (AuthUtils.transport == null) {
                try {
                    Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                    Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                    exit.invoke(null, 0);
                } catch (Exception ex) {
                }
                return;
            }
            String sub = args[0];
            if (sub.equalsIgnoreCase("list")) {
                AuthUtils.transport.listCloudConfigs();
                return;
            }
            if (sub.equalsIgnoreCase("load") || sub.equalsIgnoreCase("get")) {
                if (args.length < 2) {
                    ChatUtils.addChatMessage("用法: .config load [用户] <配置名>");
                    return;
                }
                if (args.length == 2) {
                    AuthUtils.transport.getCloudConfig(args[1]);
                } else {
                    AuthUtils.transport.getCloudConfig(args[1], args[2]);
                }
                return;
            }
            if (sub.equalsIgnoreCase("save") || sub.equalsIgnoreCase("upload")) {
                if (args.length < 2) {
                    ChatUtils.addChatMessage("用法: .config save <name>");
                    return;
                }
                String content = Naven.getInstance().getFileManager().saveConfigToString();
                AuthUtils.transport.uploadCloudConfig(args[1], content);
                return;
            }
            if (sub.equalsIgnoreCase("delete") || sub.equalsIgnoreCase("remove")) {
                if (args.length < 2) {
                    ChatUtils.addChatMessage("用法: .config delete <name>");
                    return;
                }
                AuthUtils.transport.deleteCloudConfig(args[1]);
                return;
            }
        }

        ChatUtils.addChatMessage("§bConfig 命令用法:");
        ChatUtils.addChatMessage("§7.config list §f- 列出云配置");
        ChatUtils.addChatMessage("§7.config load [用户] <配置名> §f- 加载云配置");
        ChatUtils.addChatMessage("§7.config save <name> §f- 保存云配置");
        ChatUtils.addChatMessage("§7.config delete <name> §f- 删除云配置");
    }

    @Override
    public String[] onTab(String[] args) {
        return new String[0];
    }
}
