package com.heypixel.heypixelmod.obsoverlay.commands;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.impl.*;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClientChat;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    public static final String PREFIX = ".";
    public final Map<String, Command> aliasMap = new HashMap<>();

    public CommandManager() {
        try {
            this.initCommands();
        } catch (Exception var2) {
            throw new RuntimeException(var2);
        }

        Naven.getInstance().getEventManager().register(this);
        if (AuthUtils.transport == null || AuthUtils.authed.get().length() != 32) {
            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
        }
    }

    private void initCommands() {
        this.registerCommand(new CommandBind());
        this.registerCommand(new CommandToggle());
        this.registerCommand(new CommandConfig());
        this.registerCommand(new CommandLanguage());
        this.registerCommand(new CommandProxy());
    }

    private void registerCommand(Command command) {
        command.initCommand();
        this.aliasMap.put(command.getName().toLowerCase(), command);

        for (String alias : command.getAliases()) {
            this.aliasMap.put(alias.toLowerCase(), command);
        }
    }

    @EventTarget
    public void onChat(EventClientChat e) {
        if (e.getMessage().startsWith(".")) {
            e.setCancelled(true);
            String chatMessage = e.getMessage().substring(".".length());
            String[] arguments = chatMessage.split(" ");
            if (arguments.length < 1) {
                ChatUtils.addChatMessage("Invalid command.");
                return;
            }

            String alias = arguments[0].toLowerCase();
            Command command = this.aliasMap.get(alias);
            if (command == null) {
                ChatUtils.addChatMessage("Invalid command.");
                return;
            }

            String[] args = new String[arguments.length - 1];
            System.arraycopy(arguments, 1, args, 0, args.length);
            command.onCommand(args);
        } else if (e.getMessage().startsWith("#")) {
            if (AuthUtils.transport != null) {
                e.setCancelled(true);
                AuthUtils.transport.sendInGameUsername();
                AuthUtils.transport.sendChat(e.getMessage().substring(1));
            } else {
                try {
                    Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                    Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                    exit.invoke(null, 0);
                } catch (Exception ex) {
                }
            }
        }
    }
}
