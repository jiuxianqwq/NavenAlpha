package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.files.Config.ProxyData;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;

@CommandInfo(
        name = "proxy",
        description = "Set client proxy",
        aliases = {"prox"}
)
public class CommandProxy extends Command {
    @Override
    public void onCommand(String[] args) {
        ProxyData proxyData = Naven.getInstance().getFileManager().getConfig().proxy;

        if (args.length == 0) {
            if (proxyData.host == null) {
                ChatUtils.addChatMessage("No proxy set.");
            } else {
                ChatUtils.addChatMessage("Current Proxy: " + proxyData.host + ":" + proxyData.port);
            }
        } else if (args.length == 1) {
            if (args[0].equals("cancel")) {
                proxyData.host = null;
                proxyData.port = 0;
                ChatUtils.addChatMessage("Proxy cancelled.");
            } else {
                try {
                    String[] proxy = args[0].split(":");
                    proxyData.host = proxy[0];
                    proxyData.port = Integer.parseInt(proxy[1]);
                    ChatUtils.addChatMessage("Proxy set to " + proxyData.host + ":" + proxyData.port);
                } catch (Exception var3) {
                    ChatUtils.addChatMessage("Invalid proxy.");
                }
            }
        }
    }

    @Override
    public String[] onTab(String[] args) {
        return new String[0];
    }
}
