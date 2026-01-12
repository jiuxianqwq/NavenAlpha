package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.font.Fonts;

import java.awt.*;
import java.util.Objects;

public class Notification {
    public static byte[] authTokens;
    private NotificationLevel level;
    private String message;
    private long maxAge;
    private long createTime = System.currentTimeMillis();
    private SmoothAnimationTimer widthTimer = new SmoothAnimationTimer(0.0F);
    private SmoothAnimationTimer heightTimer = new SmoothAnimationTimer(0.0F);

    public Notification(NotificationLevel level, String message, long age) {
        this.level = level;
        this.message = message;
        this.maxAge = age;
    }

    public void render(float x, float y) {
        Skia.drawShadow(x + 2.0F, y + 4.0F, this.getWidth(), 20.0F, 5.0F, this.level.getColor());
        Skia.drawRoundedBlur(x + 2.0F, y + 4.0F, this.getWidth(), 20.0F, 5.0F);
        Skia.drawRoundedRect(x + 2.0F, y + 4.0F, this.getWidth(), 20.0F, 5.0F, new Color(0, 0, 0, 100));
        Skia.drawText(message, x + 6.0F, y + 9.0F, Color.WHITE, Fonts.getMiSans(12f));
    }

    public float getWidth() {
        float stringWidth = Skia.getStringWidth(message, Fonts.getMiSans(12f));
        return stringWidth + 12.0F;
    }

    public float getHeight() {
        return 24.0F;
    }

    public NotificationLevel getLevel() {
        return this.level;
    }

    public void setLevel(NotificationLevel level) {
        this.level = level;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getMaxAge() {
        return this.maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public SmoothAnimationTimer getWidthTimer() {
        return this.widthTimer;
    }

    public void setWidthTimer(SmoothAnimationTimer widthTimer) {
        this.widthTimer = widthTimer;
    }

    public SmoothAnimationTimer getHeightTimer() {
        return this.heightTimer;
    }

    public void setHeightTimer(SmoothAnimationTimer heightTimer) {
        this.heightTimer = heightTimer;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Notification other)) {
            return false;
        } else if (!other.canEqual(this)) {
            return false;
        } else if (this.getMaxAge() != other.getMaxAge()) {
            return false;
        } else if (this.getCreateTime() != other.getCreateTime()) {
            return false;
        } else {
            Object this$level = this.getLevel();
            Object other$level = other.getLevel();
            if (Objects.equals(this$level, other$level)) {
                Object this$message = this.getMessage();
                Object other$message = other.getMessage();
                if (Objects.equals(this$message, other$message)) {
                    Object this$widthTimer = this.getWidthTimer();
                    Object other$widthTimer = other.getWidthTimer();
                    if (Objects.equals(this$widthTimer, other$widthTimer)) {
                        Object this$heightTimer = this.getHeightTimer();
                        Object other$heightTimer = other.getHeightTimer();
                        return Objects.equals(this$heightTimer, other$heightTimer);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Notification;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $maxAge = this.getMaxAge();
        result = result * 59 + (int) ($maxAge >>> 32 ^ $maxAge);
        long $createTime = this.getCreateTime();
        result = result * 59 + (int) ($createTime >>> 32 ^ $createTime);
        Object $level = this.getLevel();
        result = result * 59 + ($level == null ? 43 : $level.hashCode());
        Object $message = this.getMessage();
        result = result * 59 + ($message == null ? 43 : $message.hashCode());
        Object $widthTimer = this.getWidthTimer();
        result = result * 59 + ($widthTimer == null ? 43 : $widthTimer.hashCode());
        Object $heightTimer = this.getHeightTimer();
        return result * 59 + ($heightTimer == null ? 43 : $heightTimer.hashCode());
    }

    @Override
    public String toString() {
        return "Notification(level="
                + this.getLevel()
                + ", message="
                + this.getMessage()
                + ", maxAge="
                + this.getMaxAge()
                + ", createTime="
                + this.getCreateTime()
                + ", widthTimer="
                + this.getWidthTimer()
                + ", heightTimer="
                + this.getHeightTimer()
                + ")";
    }
}
