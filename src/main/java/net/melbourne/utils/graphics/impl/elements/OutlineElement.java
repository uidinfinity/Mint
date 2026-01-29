package net.melbourne.utils.graphics.impl.elements;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import java.awt.*;


public record OutlineElement(RenderPipeline pipelines, TextureSetup textureSetup, Matrix3x2f matrix3x2f,
                          float left, float top, float right, float bottom, Color color,
                          @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds)
		implements SimpleGuiElementRenderState
{

	public OutlineElement(RenderPipeline pipelines, TextureSetup textureSetup, Matrix3x2f matrix3x2f,
	                   float left, float top, float right, float bottom, Color color,
	                   @Nullable ScreenRect scissorArea)
	{
		this(pipelines, textureSetup, matrix3x2f, left, top, right, bottom, color, scissorArea, createBounds(left, top, right, bottom, matrix3x2f, scissorArea));
	}

	public OutlineElement(RenderPipeline pipelines, TextureSetup textureSetup, Matrix3x2f matrix3x2f, float left,
	                   float top, float right, float bottom, Color color, ScreenRect scissorArea, ScreenRect bounds) {
		this.pipelines = pipelines;
		this.textureSetup = textureSetup;
		this.matrix3x2f = matrix3x2f;
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
		this.color = color;
		this.scissorArea = scissorArea;
		this.bounds = bounds;
	}

	@Override
	public void setupVertices(VertexConsumer vertices, float depth) {
		vertices.vertex(matrix3x2f, left, top, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, left, bottom, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, left + 0.5f, bottom, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, left + 0.5f, top, depth).color(color.getRGB());

		vertices.vertex(matrix3x2f, right - 0.5f, top, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, right - 0.5f, bottom, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, right, bottom, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, right, top, depth).color(color.getRGB());

		vertices.vertex(matrix3x2f, left, bottom - 0.5f, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, left, bottom, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, right, bottom, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, right, bottom - 0.5f, depth).color(color.getRGB());

		vertices.vertex(matrix3x2f, left, top, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, left, top + 0.5f, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, right, top + 0.5f, depth).color(color.getRGB());
		vertices.vertex(matrix3x2f, right, top, depth).color(color.getRGB());
	}

	@Override
	public RenderPipeline pipeline()
	{
		return pipelines;
	}

	@Override
	public @Nullable ScreenRect scissorArea()
	{
		return scissorArea;
	}

	@Override
	public @Nullable ScreenRect bounds()
	{
		return bounds;
	}

	@Nullable
	private static ScreenRect createBounds(float left, float top, float right, float bottom, Matrix3x2f matrix3x2f, @Nullable ScreenRect scissorArea)
	{
		ScreenRect screenRect = new ScreenRect((int) left, (int) top, (int) (right - left), (int) (bottom - top)).transformEachVertex(matrix3x2f);
		return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
	}
}

