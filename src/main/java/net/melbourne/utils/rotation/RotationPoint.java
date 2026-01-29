package net.melbourne.utils.rotation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class RotationPoint {
    private float yaw, pitch;
    private int priority;
    private boolean instant;
}