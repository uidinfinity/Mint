package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.melbourne.events.Event;
import net.minecraft.client.render.VertexConsumerProvider;

@Getter
@Setter
@AllArgsConstructor
public class RenderHandEvent extends Event {
    private VertexConsumerProvider vertexConsumers;

    public static class Post extends Event
    {

    }
}
