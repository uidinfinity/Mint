package net.melbourne.events.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;

@Getter
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class LocationEvent extends Event {
    private final double x;
    private final double y;
    private final double z;
    private float yaw;
    private float pitch;
    private boolean onGround;
    private boolean modified;
    private final boolean horizontal;

    public LocationEvent(double x, double y, double z, float yaw, float pitch, boolean onGround, boolean horizontal) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.horizontal = horizontal;
    }

    public void setYaw(float yaw) {
        modified = true;
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        modified = true;
        this.pitch = pitch;
    }

    public void setOnGround(boolean onGround) {
        modified = true;
        this.onGround = onGround;
    }

    public boolean getHorizontal() {
        return horizontal;
    }
}