package net.melbourne.events.impl;


import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;

@Getter
@AllArgsConstructor
public class KeyInputEvent extends Event {
    public final int key;
    public final int scancode;
    public final int action;
    public final int modifiers;
}