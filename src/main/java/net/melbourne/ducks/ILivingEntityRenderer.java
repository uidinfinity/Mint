package net.melbourne.ducks;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public interface ILivingEntityRenderer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    // In 1.21.8, we render using the State, not the Entity directly
    void melbourne$render(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider provider, int light);
}