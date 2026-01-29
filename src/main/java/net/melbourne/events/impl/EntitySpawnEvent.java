package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;
import net.minecraft.entity.Entity;


@AllArgsConstructor
@Getter
public class EntitySpawnEvent extends Event {
    private final Entity entity;
}
