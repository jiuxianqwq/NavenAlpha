package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.callables.EventCancellable;

public class EventKey extends EventCancellable {
    private final int key;
    private final boolean state;

    public EventKey(int key, boolean state) {
        this.key = key;
        this.state = state;
    }

    public int getKey() {
        return this.key;
    }

    public boolean isState() {
        return this.state;
    }
}
