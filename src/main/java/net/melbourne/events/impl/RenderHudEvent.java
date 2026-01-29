package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;
import net.minecraft.client.gui.DrawContext;

@Getter
@AllArgsConstructor
public class RenderHudEvent extends Event {
    public DrawContext context;

}
