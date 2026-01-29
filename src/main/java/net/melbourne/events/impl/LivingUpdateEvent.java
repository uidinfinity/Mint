package net.melbourne.events.impl;

import net.melbourne.events.Event;
import net.minecraft.entity.LivingEntity;

public class LivingUpdateEvent extends Event {
    private final LivingEntity entity;
    private final boolean preUpdate;

    public LivingUpdateEvent(LivingEntity entity, boolean preUpdate) {
        this.entity = entity;
        this.preUpdate = preUpdate;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public boolean isPreUpdate() {
        return preUpdate;
    }

    public boolean isPostUpdate() {
        return !preUpdate;
    }
}