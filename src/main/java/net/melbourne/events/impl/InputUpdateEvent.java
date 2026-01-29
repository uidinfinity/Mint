package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;
import net.minecraft.client.input.Input;

@AllArgsConstructor
@Getter
public class InputUpdateEvent extends Event {
    public final Input input;
}
