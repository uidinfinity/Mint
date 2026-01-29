package net.melbourne.utils.graphics.api;

import lombok.Getter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;


@Getter
public class WorldContext
{
	public MatrixStack matrix;
	public VertexConsumerProvider buffer;

	public WorldContext(MatrixStack stack, VertexConsumerProvider buffer)
	{
		this.matrix = stack;
		this.buffer = buffer;
	}

}
