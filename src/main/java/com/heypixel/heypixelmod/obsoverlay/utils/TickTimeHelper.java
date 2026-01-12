package com.heypixel.heypixelmod.obsoverlay.utils;

import java.util.ArrayList;
import java.util.List;

public class TickTimeHelper {
    private static final List<TickTimeHelper> timers = new ArrayList<>();
    private int tickPassed = 0;

    public TickTimeHelper() {
        timers.add(this);
    }

    public static void update() {
        for (TickTimeHelper timer : timers) {
            timer.tickPassed++;
        }
    }

    public boolean delay(int ticks) {
        return this.tickPassed >= ticks;
    }

    public boolean delay(float ticks) {
        return (float) this.tickPassed >= ticks;
    }

    public void reset() {
        this.tickPassed = 0;
    }
}
