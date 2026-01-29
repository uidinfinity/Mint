package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;
import net.minecraft.entity.player.PlayerEntity;


@Getter
@AllArgsConstructor
public class PlayerDeathEvent extends Event {
    private final PlayerEntity player;
}
