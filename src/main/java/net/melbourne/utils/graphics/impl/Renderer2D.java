package net.melbourne.utils.graphics.impl;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.melbourne.utils.Globals;
import net.melbourne.utils.graphics.impl.elements.*;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

import java.awt.*;


public class Renderer2D implements Globals
{

	public static void renderOutline(DrawContext context, float left, float top, float right, float bottom, Color color)
	{
		context.state.addSimpleElement(new OutlineElement(RenderPipelines.GUI,
				TextureSetup.empty(),
				new Matrix3x2f(context.getMatrices()),
				left, top, right, bottom, color, context.scissorStack.peekLast()));
	}

	public static void renderQuad(DrawContext context, float left, float top, float right, float bottom, Color color)
	{
		context.state.addSimpleElement(new QuadElement(RenderPipelines.GUI,
				TextureSetup.empty(),
				new Matrix3x2f(context.getMatrices()),
				left, top, right, bottom, color, context.scissorStack.peekLast()));
	}

	public static void renderGradient(DrawContext context, float left, float top, float right, float bottom, Color startColor, Color endColor)
	{
		context.state.addSimpleElement(new GradientElement(RenderPipelines.GUI,
				TextureSetup.empty(),
				new Matrix3x2f(context.getMatrices()),
				left, top, right, bottom, startColor, endColor, context.scissorStack.peekLast()));
	}

	public static void renderSidewaysGradient(DrawContext context, float left, float top, float right, float bottom, Color startColor, Color endColor)
	{
		context.state.addSimpleElement(new SidewaysGradientElement(RenderPipelines.GUI,
				TextureSetup.empty(),
				new Matrix3x2f(context.getMatrices()),
				left, top, right, bottom, startColor, endColor, context.scissorStack.peekLast()));
	}

	public static void renderTexturedQuad(DrawContext context, TextureSetup texture, float left, float top, float right, float bottom, Color color) {
		context.state.addSimpleElement(new TexturedQuadElement(RenderPipelines.GUI_TEXTURED,
				texture,
				new Matrix3x2f(context.getMatrices()),
				(int) left, (int) top,
				(int) right, (int) bottom,
				0.0f, 1.0f, 0.0f, 1.0f, color.getRGB(), context.scissorStack.peekLast()));
	}


	public static void renderTexturedQuad(DrawContext context, Identifier texture, float left, float top, float right, float bottom, Color color) {
		var tex = mc.getTextureManager().getTexture(texture);
		GpuTextureView view = tex.getGlTextureView();

		context.state.addSimpleElement(new TexturedQuadElement(RenderPipelines.GUI_TEXTURED,
				TextureSetup.of(view),
				new Matrix3x2f(context.getMatrices()),
				(int) left, (int) top,
				(int) right, (int) bottom,
				0.0f, 1.0f, 0.0f, 1.0f, color.getRGB(), context.scissorStack.peekLast()));
	}


}
