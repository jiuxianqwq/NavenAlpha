package com.heypixel.heypixelmod.obsoverlay.values.impl;

import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueType;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class DragValue extends Value {
    private final float defaultX;
    private final float defaultY;
    private final Consumer<Value> update;
    private float x;
    private float y;
    private float width;
    private float height;

    public DragValue(HasValue key, String name, float defaultX, float defaultY, Consumer<Value> update, Supplier<Boolean> visibility) {
        super(key, name, visibility);
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.x = defaultX;
        this.y = defaultY;
        this.update = update;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.DRAG;
    }

    @Override
    public DragValue getDragValue() {
        return this;
    }

    public float getDefaultX() {
        return defaultX;
    }

    public float getDefaultY() {
        return defaultY;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
        if (this.update != null) {
            this.update.accept(this);
        }
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
        if (this.update != null) {
            this.update.accept(this);
        }
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        if (this.update != null) {
            this.update.accept(this);
        }
    }
}
