package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;

public record EventMouseClick(int key, boolean state) implements Event {

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof EventMouseClick other)) {
            return false;
        } else if (!other.canEqual(this)) {
            return false;
        } else {
            return this.key() == other.key() && this.state() == other.state();
        }
    }

    private boolean canEqual(Object other) {
        return other instanceof EventMouseClick;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.key();
        return result * 59 + (this.state() ? 79 : 97);
    }

    @Override
    public String toString() {
        return "EventMouseClick(key=" + this.key() + ", state=" + this.state() + ")";
    }
}
