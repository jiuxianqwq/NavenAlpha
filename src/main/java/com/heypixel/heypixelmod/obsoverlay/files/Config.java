package com.heypixel.heypixelmod.obsoverlay.files;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    public Map<String, ModuleData> modules = new HashMap<>();
    public List<String> friends = new ArrayList<>();
    public List<String> killSays = new ArrayList<>();
    public List<String> spammerMessages = new ArrayList<>();
    public ProxyData proxy = new ProxyData();
    public GuiData gui = new GuiData();

    public static class ModuleData {
        public int key;
        public boolean enabled;
        public Map<String, Object> values = new HashMap<>();
    }

    public static class ProxyData {
        public String host;
        public int port;
    }

    public static class GuiData {
        public float x = 0, y = 0, width = 600, height = 400; // Defaults
    }
}
