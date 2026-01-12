package com.heypixel.heypixelmod.obsoverlay.files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.files.Config.ModuleData;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.ui.ClickGUI;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class FileManager {
    public static final Logger logger = LogManager.getLogger(FileManager.class);
    public static final File clientFolder = new File(System.getenv("APPDATA"), Naven.CLIENT_NAME);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configFile;
    private Config config;

    public FileManager() {
        if (!clientFolder.exists()) {
            clientFolder.mkdirs();
        }
        this.configFile = new File(clientFolder, "config.json");
        this.config = new Config();
        if (AuthUtils.transport == null || AuthUtils.authed.get().length() != 32) {
            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
        }
    }

    public Config getConfig() {
        return config;
    }

    public void load() {
        if (configFile.exists()) {
            try (Reader reader = new InputStreamReader(Files.newInputStream(configFile.toPath()), StandardCharsets.UTF_8)) {
                config = gson.fromJson(reader, Config.class);
                if (config == null) config = new Config();
                logger.info("Loaded config from JSON.");
            } catch (Exception e) {
                logger.error("Failed to load JSON config!", e);
                config = new Config();
//                loadDefaults();
            }
        } else {
            logger.info("No config found. Loading defaults.");
//            loadDefaults();
        }

        applyConfig();
        if (AuthUtils.transport == null || AuthUtils.authed.get().length() != 32) {
            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
        }
    }

    public void save() {
        updateConfigFromRuntime();
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(configFile.toPath()), StandardCharsets.UTF_8)) {
            gson.toJson(config, writer);
            logger.info("Saved config to JSON.");
        } catch (Exception e) {
            logger.error("Failed to save JSON config!", e);
        }
    }

    public void loadConfigFromString(String json) {
        try {
            config = gson.fromJson(json, Config.class);
            if (config == null) config = new Config();
            applyConfig();
            logger.info("Loaded config from Cloud String.");
        } catch (Exception e) {
            logger.error("Failed to load Cloud config!", e);
        }
    }

    public String saveConfigToString() {
        updateConfigFromRuntime();
        return gson.toJson(config);
    }

//    private void loadDefaults() {
//        config.killSays.addAll(Arrays.asList(
//                "%s L",
//                "%s fw",
//                "%s 我喜欢你",
//                "%s 我喜欢你♥",
//                "%s 兄弟你好香",
//                "%s 可以和我交往吗？",
//                "%s 你好可爱",
//                "%s 别急",
//                "%s 你已被清朝杀手陈安健害死！快使用Hack Lunar！",
//                "%s 你已被狂笑的蛇陈安健本人害死，快使用我编写的Hack Lunar端！"
//        ));
//
//        config.spammerMessages.addAll(Arrays.asList(
//                "我是狂笑的蛇陈安健，我是花雨庭反作弊兼Hack Lunar端制作者！",
//                "我是狂笑的蛇陈安健，我使用Hack Lunar不会Ban！因为我制作了花雨庭反作弊！",
//                "你知道吗，我是狂笑的蛇陈安健！我制作了花雨庭反作弊，误封你们！",
//                "不如我狂笑的蛇将写散文制作的Hack Lunar，我很稳定因为花雨庭反作弊就是出自本人之手！",
//                "兄弟，你听我说！我不会被封，因为我是狂笑的蛇，我制作了花雨庭反作弊！",
//                "我是狂笑的蛇陈安健，我疯狂巴结花雨庭的技术！",
//                "陈安健制作的Hack Lunar依然稳定绕过花雨庭，因为我是狂笑的蛇！",
//                "我是大名鼎鼎的狂笑的蛇将写散文本人陈安健，我能无视反作弊飘主播因为我制作了花雨庭反作弊！",
//                "羡慕我不会被封吗？因为我狂笑的蛇本人陈安健制作了花雨庭的反作弊！",
//                "B站搜索狂笑的蛇将写散文，看我陈安健精彩集锦！",
//                "我狂笑的蛇陈安健能无视反作弊，因为花雨庭反作弊就是我做的！",
//                "哈哈你知道你为什么会被误封吗？因为我狂笑的蛇陈安健制作了花雨庭反作弊！"
//        ));
//    }

    private void applyConfig() {
        ModuleManager moduleManager = Naven.getInstance().getModuleManager();

        // Modules & Values
        for (Map.Entry<String, ModuleData> entry : config.modules.entrySet()) {
            try {
                Module module = moduleManager.getModule(entry.getKey());
                ModuleData data = entry.getValue();
                module.setKey(data.key);
                module.setEnabled(data.enabled);

                for (Map.Entry<String, Object> valEntry : data.values.entrySet()) {
                    Value value = Naven.getInstance().getValueManager().getValue(module, valEntry.getKey());
                    if (value != null) {
                        applyValue(value, valEntry.getValue());
                    }
                }
            } catch (Exception e) {
                logger.warn("Module or value not found: " + entry.getKey());
            }
        }

        // Friends
        FriendManager.getFriends().clear();
        for (String friend : config.friends) {
            FriendManager.addFriend(friend);
        }

//        // KillSays
//        KillSay killSay = (KillSay) moduleManager.getModule(KillSay.class);
//        if (killSay != null) {
//            for (String msg : config.killSays) {
//                boolean exists = false;
//                for (BooleanValue v : killSay.getValues()) {
//                    if (v.getName().equals(msg)) {
//                        exists = true;
//                        break;
//                    }
//                }
//                if (!exists) {
//                    killSay.getValues().add(ValueBuilder.create(killSay, msg).setDefaultBooleanValue(false).build().getBooleanValue());
//                }
//            }
//        }
//
//        // Spammer
//        Spammer spammer = (Spammer) moduleManager.getModule(Spammer.class);
//        if (spammer != null) {
//            for (String msg : config.spammerMessages) {
//                boolean exists = false;
//                for (BooleanValue v : spammer.getValues()) {
//                    if (v.getName().equals(msg)) {
//                        exists = true;
//                        break;
//                    }
//                }
//                if (!exists) {
//                    spammer.getValues().add(ValueBuilder.create(spammer, msg).setDefaultBooleanValue(false).build().getBooleanValue());
//                }
//            }
//        }

        // GUI
        ClickGUI.windowX = config.gui.x;
        ClickGUI.windowY = config.gui.y;
        ClickGUI.windowWidth = config.gui.width;
        ClickGUI.windowHeight = config.gui.height;

        // Proxy update
        // We might want to set the ProxyData back to runtime if needed, but CommandProxy usually reads from Config directly now.
        // But if there is any other place using ProxyFile static fields (which we deleted), we need to ensure everything uses Config.
    }

    private void applyValue(Value value, Object val) {
        if (val == null) return;
        switch (value.getValueType()) {
            case BOOLEAN:
                if (val instanceof Boolean) value.getBooleanValue().setCurrentValue((Boolean) val);
                break;
            case FLOAT:
                if (val instanceof Number) value.getFloatValue().setCurrentValue(((Number) val).floatValue());
                break;
            case STRING:
                if (val instanceof String) value.getStringValue().setCurrentValue((String) val);
                break;
            case MODE:
                if (val instanceof Number) {
                    int idx = ((Number) val).intValue();
                    ModeValue mv = value.getModeValue();
                    if (idx >= 0 && idx < mv.getValues().length) {
                        mv.setCurrentValue(idx);
                    }
                }
                break;
            case DRAG:
                if (val instanceof Map<?, ?> map) {
                    DragValue dv = value.getDragValue();
                    if (map.containsKey("x") && map.get("x") instanceof Number) {
                        dv.setX(((Number) map.get("x")).floatValue());
                    }
                    if (map.containsKey("y") && map.get("y") instanceof Number) {
                        dv.setY(((Number) map.get("y")).floatValue());
                    }
                }
                break;
        }
    }

    private void updateConfigFromRuntime() {
        ModuleManager moduleManager = Naven.getInstance().getModuleManager();

        // Modules & Values
        config.modules.clear();
        for (Module module : moduleManager.getModules()) {
            ModuleData data = new ModuleData();
            data.key = module.getKey();
            data.enabled = module.isEnabled();

            for (Value value : Naven.getInstance().getValueManager().getValues()) {
                if (value.getKey() == module) { // Check if value belongs to this module
                    switch (value.getValueType()) {
                        case BOOLEAN:
                            data.values.put(value.getName(), value.getBooleanValue().getCurrentValue());
                            break;
                        case FLOAT:
                            data.values.put(value.getName(), value.getFloatValue().getCurrentValue());
                            break;
                        case STRING:
                            data.values.put(value.getName(), value.getStringValue().getCurrentValue());
                            break;
                        case MODE:
                            data.values.put(value.getName(), value.getModeValue().getCurrentValue());
                            break;
                        case DRAG:
                            Map<String, Float> dragData = new HashMap<>();
                            dragData.put("x", value.getDragValue().getX());
                            dragData.put("y", value.getDragValue().getY());
                            data.values.put(value.getName(), dragData);
                            break;
                    }
                }
            }
            config.modules.put(module.getEnName(), data);
        }

        // Friends
        config.friends = new ArrayList<>(FriendManager.getFriends());

//        // KillSays & Spammer
//        KillSay killSay = (KillSay) moduleManager.getModule(KillSay.class);
//        if (killSay != null) {
//            config.killSays.clear();
//            for (BooleanValue v : killSay.getValues()) {
//                config.killSays.add(v.getName());
//            }
//        }
//
//        Spammer spammer = (Spammer) moduleManager.getModule(Spammer.class);
//        if (spammer != null) {
//            config.spammerMessages.clear();
//            for (BooleanValue v : spammer.getValues()) {
//                config.spammerMessages.add(v.getName());
//            }
//        }

        // GUI
        config.gui.x = ClickGUI.windowX;
        config.gui.y = ClickGUI.windowY;
        config.gui.width = ClickGUI.windowWidth;
        config.gui.height = ClickGUI.windowHeight;
    }
}
