package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;

public record EventRunTicks(EventType type) implements Event {
}
