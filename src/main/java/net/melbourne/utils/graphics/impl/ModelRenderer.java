package net.melbourne.utils.graphics.impl;

import net.melbourne.ducks.ILivingEntityRenderer;
import net.melbourne.utils.Globals;
import net.melbourne.utils.graphics.api.WorldContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;

public class ModelRenderer implements Globals {
    private static Render render;
    private static Matrix4f modelMatrix;
    private static Vec3d entityPos;
    private static Vec3d cameraPos;

    public static void renderModel(WorldContext context, Entity entity, float scale, float tickDelta, Render render) {
        ModelRenderer.render = render;
        ModelRenderer.cameraPos = mc.gameRenderer.getCamera().getPos();

        double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
        ModelRenderer.entityPos = new Vec3d(x, y, z);

        EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState renderState = renderer.createRenderState();

        if (renderer instanceof LivingEntityRenderer livingRenderer && entity instanceof LivingEntity living) {
            livingRenderer.updateRenderState(living, (LivingEntityRenderState) renderState, tickDelta);
        } else if (renderer instanceof EndCrystalEntityRenderer crystalRenderer && entity instanceof EndCrystalEntity crystal) {
            crystalRenderer.updateRenderState(crystal, (EndCrystalEntityRenderState) renderState, tickDelta);
        } else {
            renderer.updateRenderState(entity, renderState, tickDelta);
        }

        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.scale(scale, scale, scale);
        ModelRenderer.modelMatrix = matrices.peek().getPositionMatrix();

        if (renderer instanceof LivingEntityRenderer livingRenderer && renderState instanceof LivingEntityRenderState state) {
            ((ILivingEntityRenderer) livingRenderer).melbourne$render(state, matrices, CustomVertexConsumerProvider.INSTANCE, 15);
        } else if (renderer instanceof EndCrystalEntityRenderer crystalRenderer && renderState instanceof EndCrystalEntityRenderState state) {
            crystalRenderer.render(state, matrices, CustomVertexConsumerProvider.INSTANCE, 15);
        }

        matrices.pop();
    }

    private static class CustomVertexConsumer implements VertexConsumer {
        public static final CustomVertexConsumer INSTANCE = new CustomVertexConsumer();
        private final float[] xs = new float[4];
        private final float[] ys = new float[4];
        private final float[] zs = new float[4];
        private int i = 0;

        private Renderer3D.Vertex transform(float x, float y, float z, int color) {
            Vector4f localVec = new Vector4f(x, y, z, 1.0f).mul(modelMatrix);
            float finalX = (float) (entityPos.x + localVec.x - cameraPos.x);
            float finalY = (float) (entityPos.y + localVec.y - cameraPos.y);
            float finalZ = (float) (entityPos.z + localVec.z - cameraPos.z);
            return new Renderer3D.Vertex(finalX, finalY, finalZ, color);
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            xs[i] = x; ys[i] = y; zs[i] = z;
            if (++i == 4) {
                if (render.fill) {
                    Renderer3D.QUADS.add(new Renderer3D.VertexCollection(
                            transform(xs[0], ys[0], zs[0], render.fillColor().getRGB()),
                            transform(xs[1], ys[1], zs[1], render.fillColor().getRGB()),
                            transform(xs[2], ys[2], zs[2], render.fillColor().getRGB()),
                            transform(xs[3], ys[3], zs[3], render.fillColor().getRGB())
                    ));
                }
                if (render.outline) {
                    Renderer3D.DEBUG_LINES.add(new Renderer3D.VertexCollection(
                            transform(xs[0], ys[0], zs[0], render.outlineColor().getRGB()),
                            transform(xs[1], ys[1], zs[1], render.outlineColor().getRGB()),
                            transform(xs[1], ys[1], zs[1], render.outlineColor().getRGB()),
                            transform(xs[2], ys[2], zs[2], render.outlineColor().getRGB()),
                            transform(xs[2], ys[2], zs[2], render.outlineColor().getRGB()),
                            transform(xs[3], ys[3], zs[3], render.outlineColor().getRGB()),
                            transform(xs[3], ys[3], zs[3], render.outlineColor().getRGB()),
                            transform(xs[0], ys[0], zs[0], render.outlineColor().getRGB())
                    ));
                }
                i = 0;
            }
            return this;
        }

        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer texture(float u, float v) { return this; }
        @Override public VertexConsumer overlay(int u, int v) { return this; }
        @Override public VertexConsumer light(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
    }

    private static class CustomVertexConsumerProvider implements VertexConsumerProvider {
        public static final CustomVertexConsumerProvider INSTANCE = new CustomVertexConsumerProvider();
        @Override public VertexConsumer getBuffer(net.minecraft.client.render.RenderLayer layer) { return CustomVertexConsumer.INSTANCE; }
    }

    public record Render(boolean fill, Color fillColor, boolean outline, Color outlineColor) { }
}