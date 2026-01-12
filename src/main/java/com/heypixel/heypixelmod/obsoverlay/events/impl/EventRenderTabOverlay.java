package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import net.minecraft.network.chat.Component;

public class EventRenderTabOverlay implements Event {
    private EventType type;
    private Component component;

    public EventRenderTabOverlay(EventType type, Component component) {
        this.type = type;
        this.component = component;
    }

    public EventType getType() {
        return this.type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Component getComponent() {
        return this.component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }
}
