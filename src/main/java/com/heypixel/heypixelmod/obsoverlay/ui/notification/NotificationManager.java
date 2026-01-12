package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderSkia;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.auth.AuthUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.Skia;
import com.heypixel.heypixelmod.obsoverlay.values.impl.DragValue;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.Path;
import io.github.humbleui.types.Rect;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public NotificationManager() {

        if (AuthUtils.transport == null || AuthUtils.authed.get().length() != 32) {
            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
        }
    }

    public void addNotification(Notification notification) {
        if (!this.notifications.contains(notification)) {
            this.notifications.add(notification);
        }
    }

    public void onRender(EventRenderSkia e, DragValue dragValue) {
        float height = 5.0F;

        Path path = new Path();
        path.addRect(Rect.makeXYWH(dragValue.getX(), dragValue.getY() + 5, dragValue.getWidth() + 10, dragValue.getHeight()));
        Skia.save();
        Skia.getCanvas().clipPath(path, ClipMode.INTERSECT, true);
        for (Notification notification : this.notifications) {
            float width = notification.getWidth();
            height += notification.getHeight();
            SmoothAnimationTimer widthTimer = notification.getWidthTimer();
            SmoothAnimationTimer heightTimer = notification.getHeightTimer();
            float lifeTime = (float) (System.currentTimeMillis() - notification.getCreateTime());
            if (lifeTime > (float) notification.getMaxAge()) {
                widthTimer.target = 0.0F;
                heightTimer.target = 0.0F;
                if (widthTimer.isAnimationDone(true)) {
                    this.notifications.remove(notification);
                }
            } else {
                widthTimer.target = width;
                heightTimer.target = height;
            }

            widthTimer.update(true);
            heightTimer.update(true);
            dragValue.setWidth(Math.max(width, dragValue.getWidth()));
            dragValue.setHeight(height);

            notification.render(dragValue.getX() - notification.getWidth() + widthTimer.value + 2.0F, dragValue.getY() - notification.getHeight() + heightTimer.value);
        }
        Skia.restore();
    }
}
