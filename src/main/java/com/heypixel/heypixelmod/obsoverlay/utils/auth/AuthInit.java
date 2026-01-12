package com.heypixel.heypixelmod.obsoverlay.utils.auth;

import cn.paradisemc.ZKMIndy;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventGlobalPacket;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/22 05:00
 * @Filename：AuthInit
 */

@ZKMIndy
public class AuthInit {
    public AuthInit() {
        System.setProperty("sun.stdout.encoding", StandardCharsets.UTF_8.name());
        System.setProperty("file.encoding", StandardCharsets.UTF_8.name());
        System.setProperty("java.awt.headless", "false");
        CompletableFuture.runAsync(() -> init(null));
        while (AuthUtils.authed == null || AuthUtils.authed.get() == null || AuthUtils.authed.get().length() == 4) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void init(EventGlobalPacket e) {
        AuthUtils.init();
        AuthUtils.showLoginDialog();
    }
}
