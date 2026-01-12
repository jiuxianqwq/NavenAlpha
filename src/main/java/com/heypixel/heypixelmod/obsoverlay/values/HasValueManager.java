package com.heypixel.heypixelmod.obsoverlay.values;

import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;

import java.lang.reflect.Method;
import java.util.*;

public class HasValueManager {
    private final List<HasValue> hasValues = new ArrayList<>();
    private final Map<String, HasValue> nameMap = new HashMap<>();

    public HasValueManager() {

        if (AuthUtils.transport == null || AuthUtils.authed.get().length() != 32) {
            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
        }
    }

    public void registerHasValue(HasValue hasValue) {
        this.hasValues.add(hasValue);
        this.nameMap.put(hasValue.getName().toLowerCase(), hasValue);
    }

    public HasValue getHasValue(String name) {
        return this.nameMap.get(name.toLowerCase());
    }

    public List<HasValue> getHasValues() {
        return this.hasValues;
    }
}
