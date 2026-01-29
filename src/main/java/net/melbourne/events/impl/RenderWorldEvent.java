package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;
import net.melbourne.utils.graphics.api.WorldContext;

@Getter
@AllArgsConstructor
public class RenderWorldEvent extends Event {
    private final WorldContext context;
    private final float tickDelta;

    @Getter
    @AllArgsConstructor
    public static class Post extends Event {
        private final WorldContext context;
        private final float tickDelta;
    }
}
