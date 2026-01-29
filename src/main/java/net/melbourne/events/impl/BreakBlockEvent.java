package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;
import net.minecraft.util.math.BlockPos;

@Getter
@AllArgsConstructor
public class BreakBlockEvent extends Event {
    public BlockPos pos;
}
