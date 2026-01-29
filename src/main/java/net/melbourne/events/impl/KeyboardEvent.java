package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;

@AllArgsConstructor
@Getter
public class KeyboardEvent extends Event {
    private final int key, modifiers;
    private boolean action;
}
