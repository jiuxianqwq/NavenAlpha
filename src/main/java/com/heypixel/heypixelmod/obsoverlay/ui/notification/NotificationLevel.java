package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import java.awt.*;

public enum NotificationLevel {
    SUCCESS(new Color(23, 150, 38, 255)),
    INFO(new Color(23, 22, 38, 255)),
    WARNING(new Color(138, 90, 92, 255)),
    ERROR(new Color(148, 42, 43, 255));

    private final Color color;

    NotificationLevel(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }
}
