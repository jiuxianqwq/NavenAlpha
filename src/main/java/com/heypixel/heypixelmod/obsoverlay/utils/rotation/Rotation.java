package com.heypixel.heypixelmod.obsoverlay.utils.rotation;

import com.heypixel.heypixelmod.obsoverlay.utils.vector.Vector2f;
import lombok.Data;

@Data
public class Rotation {
    private float yaw;
    private float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Rotation(Vector2f vector2f) {
        this.yaw = vector2f.x;
        this.pitch = vector2f.y;
    }
}
