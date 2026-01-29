package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.melbourne.events.Event;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fired when the player starts breaking a block (left-click / mining).
 * The event is cancelable – if cancelled the client will not send the
 * START_DESTROY_BLOCK packet.
 */
@AllArgsConstructor
@Getter
@Setter
public class BlockBreakStartEvent extends Event {

    /** The player that initiated the break */
    private final ClientPlayerEntity player;

    /** Position of the block being broken */
    private final BlockPos pos;

    /** Face of the block that was clicked */
    private final Direction direction;
}