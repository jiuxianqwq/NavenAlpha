package com.heypixel.heypixelmod.obsoverlay.modules;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventKey;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMouseClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.exceptions.NoSuchModuleException;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.*;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.*;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.*;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.*;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.ArrayList;

public class ModuleManager {
    private static final Logger log = LogManager.getLogger(ModuleManager.class);
    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> classMap = new HashMap<>();
    private final Map<String, Module> nameMap = new HashMap<>();

    public ModuleManager() {
        try {
            this.initModules();
            this.modules.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        } catch (Exception var2) {
            log.error("Failed to initialize modules", var2);
            throw new RuntimeException(var2);
        }

        Naven.getInstance().getEventManager().register(this);
    }

    public static ModuleManager b(String string) {
        return new ModuleManager();
    }

    public static String getName(String string) {
        for (String s : AuthUtils.transport.getIgnToUserMap().keySet()) {
            if (string.contains(s)) string = string.replace(s, s + " §f(§b" + AuthUtils.transport.getName(s) + "§f)");
        }
        return string;
    }

    private void initModules() {
        this.registerModule(
                new KillAura(),
                new HUD(),
                new Velocity(),
                new NameTags(),
                new ChestStealer(),
                new InventoryCleaner(),
                new Scaffold(),
                new AntiBots(),
                new Sprint(),
                new ChestESP(),
                new ClickGUIModule(),
                new Teams(),
                new Glow(),
                new ItemTracker(),
//                new AutoMLG(),
                new ClientFriend(),
                new NoJumpDelay(),
                new FastPlace(),
                new AntiFireball(),
                new Stuck(),
                new ScoreboardSpoof(),
                new AutoTools(),
                new ViewClip(),
                new Disabler(),
                new Projectile(),
                new TimeChanger(),
                new FullBright(),
                new NameProtect(),
                new NoHurtCam(),
                new AutoClicker(),
                new AntiBlindness(),
                new AntiNausea(),
                new Scoreboard(),
                new Compass(),
//                new Spammer(),
//                new KillSay(),
                new Blink(),
                new FastWeb(),
                new PostProcess(),
                new AttackCrystal(),
                new EffectDisplay(),
                new NoRender(),
                new ItemTags(),
                new SafeWalk(),
//                new MotionBlur(),
                new Helper(),
                new NoSlow(),
                new LongJump(),
                new Target(),
                new KeepSprint(),
                new Animations(),
                new Fov(),
                new GhostHand(),
                new Eagle(),
                new HUDEditModule(),
                new Watermark(),
                new TargetHUD(),
                new Notification(),
                new AutoHeypixel(),
                new ArrayListModule(),
//                new Speed(),
                new GrimLowHop()
        );
    }

    private void registerModule(Module... modules) {
        for (Module module : modules) {
            this.registerModule(module);
        }
    }

    private void registerModule(Module module) {
        module.initModule();
        this.modules.add(module);
        this.classMap.put(module.getClass(), module);
        this.nameMap.put(module.getName().toLowerCase(), module);
    }

    public List<Module> getModulesByCategory(Category category) {
        List<Module> modules = new ArrayList<>();

        for (Module module : this.modules) {
            if (module.getCategory() == category) {
                modules.add(module);
            }
        }

        return modules;
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (module.getClass().equals(clazz)) {
                return clazz.cast(module);
            }
        }
        return null;
    }

    public Module getModule(String name) {
        Module module = this.nameMap.get(name.toLowerCase());
        if (module == null) {
            throw new NoSuchModuleException();
        } else {
            return module;
        }
    }

    @EventTarget
    public void onKey(EventKey e) {
        if (e.isState() && Minecraft.getInstance().screen == null) {
            for (Module module : this.modules) {
                if (module.getKey() == e.getKey()) {
                    module.toggle();
                }
            }
        }
    }

    @EventTarget
    public void onKey(EventMouseClick e) {
        if (!e.state() && (e.key() == 3 || e.key() == 4)) {
            for (Module module : this.modules) {
                if (module.getKey() == -e.key()) {
                    module.toggle();
                }
            }
        }
    }

    @EventTarget
    public void onRespawn(EventRespawn event) {
        if (AuthUtils.transport != null) AuthUtils.transport.sendInGameUsername();
        else
            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
    }

    @EventTarget
    public void onRenderTab(EventRenderTabOverlay e) {
        if (AuthUtils.transport != null) e.setComponent(Component.literal(getName(e.getComponent().getString())));
        else
            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
    }

    public List<Module> getModules() {
        return this.modules;
    }
}
