package net.melbourne.utils.graphics.impl.elements;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import java.awt.*;


public record GradientElement(RenderPipeline pipelines, TextureSetup textureSetup, Matrix3x2f matrix3x2f,
                                      float left, float top, float right, float bottom, Color startColor, Color endColor,
                                      @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds)
        implements SimpleGuiElementRenderState
{

    public GradientElement(RenderPipeline pipelines, TextureSetup textureSetup, Matrix3x2f matrix3x2f,
                                   float left, float top, float right, float bottom,  Color startColor, Color endColor,
                                   @Nullable ScreenRect scissorArea)
    {
        this(pipelines, textureSetup, matrix3x2f, left, top, right, bottom, startColor, endColor, scissorArea, createBounds(left, top, right, bottom, matrix3x2f, scissorArea));
    }

    public GradientElement(RenderPipeline pipelines, TextureSetup textureSetup, Matrix3x2f matrix3x2f, float left,
                                   float top, float right, float bottom, Color startColor, Color endColor, ScreenRect scissorArea, ScreenRect bounds) {
        this.pipelines = pipelines;
        this.textureSetup = textureSetup;
        this.matrix3x2f = matrix3x2f;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.startColor = startColor;
        this.endColor = endColor;
        this.scissorArea = scissorArea;
        this.bounds = bounds;
    }

    @Override
    public void setupVertices(VertexConsumer vertices, float depth) {
        vertices.vertex(matrix3x2f, left, top, depth).color(startColor.getRGB());
        vertices.vertex(matrix3x2f, left, bottom, depth).color(endColor.getRGB());
        vertices.vertex(matrix3x2f, right, bottom, depth).color(endColor.getRGB());
        vertices.vertex(matrix3x2f, right, top, depth).color(startColor.getRGB());
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
