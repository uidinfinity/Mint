package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;


@AllArgsConstructor
@Getter
public class AttackBlockEvent extends Event {
    private final BlockPos position;
    private final Direction direction;
}
