package net.melbourne.utils.graphics.impl.elements;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;


public record TexturedQuadElement(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, int x1, int y1, int x2, int y2,
                                  float u1, float u2, float v1, float v2, int color, @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds)
		implements SimpleGuiElementRenderState
{
	public TexturedQuadElement(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, int x1, int y1, int x2, int y2,
	                           float u1, float u2, float v1, float v2, int color, @Nullable ScreenRect scissorArea) {
		this(pipeline, textureSetup, pose, x1, y1, x2, y2, u1, u2, v1, v2, color, scissorArea, createBounds(x1, y1, x2, y2, pose, scissorArea));
	}

	public TexturedQuadElement(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, int x1, int y1, int x2, int y2,
	                           float u1, float u2, float v1, float v2, int color, @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds) {
		this.pipeline = pipeline;
		this.textureSetup = textureSetup;
		this.pose = pose;
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.u1 = u1;
		this.u2 = u2;
		this.v1 = v1;
		this.v2 = v2;
		this.color = color;
		this.scissorArea = scissorArea;
		this.bounds = bounds;
	}

	public void setupVertices(VertexConsumer vertices, float depth) {
		vertices.vertex(pose(), (float) x1(), (float) y1(), depth).texture(u1(), v1()).color(color());
		vertices.vertex(pose(), (float) x1(), (float) y2(), depth).texture(u1(), v2()).color(color());
		vertices.vertex(pose(), (float) x2(), (float) y2(), depth).texture(u2(), v2()).color(color());
		vertices.vertex(pose(), (float) x2(), (float) y1(), depth).texture(u2(), v1()).color(color());
	}

	@Nullable
	private static ScreenRect createBounds(int x1, int y1, int x2, int y2, Matrix3x2f pose, @Nullable ScreenRect scissorArea) {
		ScreenRect screenRect = new ScreenRect(x1, y1, x2 - x1, y2 - y1).transformEachVertex(pose);
		return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
	}

	public RenderPipeline pipeline() {
		return pipeline;
	}

	public TextureSetup textureSetup() {
		return textureSetup;
	}

	public Matrix3x2f pose() {
		return pose;
	}

	public int x1() {
		return x1;
	}

	public int y1() {
		return y1;
	}

	public int x2() {
		return x2;
	}

	public int y2() {
		return y2;
	}

	public float u1() {
		return u1;
	}

	public float u2() {
		return u2;
	}

	public float v1() {
		return v1;
	}

	public float v2() {
		return v2;
	}

	public int color() {
		return color;
	}

	@Nullable
	public ScreenRect scissorArea() {
		return scissorArea;
	}

	@Nullable
	public ScreenRect bounds() {
		return bounds;
	}
}
