package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;

@Getter
@AllArgsConstructor
public class MouseEvent extends Event {
    private final int button;
}