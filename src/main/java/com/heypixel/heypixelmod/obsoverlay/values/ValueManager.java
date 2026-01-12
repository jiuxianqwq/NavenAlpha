package com.heypixel.heypixelmod.obsoverlay.values;

import com.heypixel.heypixelmod.obsoverlay.exceptions.NoSuchValueException;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ValueManager {

    private final List<Value> values = new ArrayList<>();

    public ValueManager() {
        if (AuthUtils.transport == null || AuthUtils.authed.get().length() != 32) {
            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
        }
    }

    public void addValue(Value value) {
        this.values.add(value);
    }

    public List<Value> getValuesByHasValue(HasValue key) {
        List<Value> values = new ArrayList<>();

        for (Value value : this.values) {
            if (value.getKey() == key) {
                values.add(value);
            }
        }

        return values;
    }

    public Value getValue(HasValue key, String name) {
        for (Value value : this.values) {
            if (value.getKey() == key && value.getName().equals(name)) {
                return value;
            }
        }

        throw new NoSuchValueException();
    }

    public List<Value> getValues() {
        return this.values;
    }
}
