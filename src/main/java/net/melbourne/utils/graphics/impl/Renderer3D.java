package net.melbourne.utils.graphics.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.melbourne.Melbourne;
import net.melbourne.mixins.accessors.WorldRendererAccessor;
import net.melbourne.utils.Globals;
import net.melbourne.utils.graphics.api.RenderGlobals;
import net.melbourne.utils.graphics.api.RenderLayers;
import net.melbourne.utils.graphics.api.WorldContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public class Renderer3D implements Globals
{
    public static final VertexConsumerProvider.Immediate VERTEX_CONSUMERS = VertexConsumerProvider.immediate(new BufferAllocator(786432));

    public static final RenderLayer.MultiPhase QUADS_LAYER = RenderLayer.of(Melbourne.MOD_ID + "_quads",
            786432,
            RenderLayers.QUADS_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder().layering(RenderGlobals.LINE_SMOOTH_LAYERING).build(false));

    public static final RenderLayer.MultiPhase DEBUG_LINES_LAYER = RenderLayer.of(Melbourne.MOD_ID + "_debug_lines",
            786432,
            RenderLayers.DEBUG_LINES_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder().layering(RenderGlobals.LINE_SMOOTH_LAYERING).build(false));

    public static final Function<GpuTextureView, RenderLayer> GUI_TEXTURED_LAYER =
            Util.memoize(texture -> RenderLayer.of(Melbourne.MOD_ID + "_gui_font_textured",
                    786432, RenderPipelines.GUI_TEXTURED,
                    RenderLayer.MultiPhaseParameters.builder().layering(new RenderPhase.Layering(Melbourne.MOD_ID + "_gl_texture",
                                    () -> RenderSystem.setShaderTexture(0, texture) , () -> RenderSystem.setShaderTexture(0, null)))
                            .build(false)));

    public static final Function<GpuTextureView, RenderLayer> GUI_LAYER =
            Util.memoize(texture -> RenderLayer.of(Melbourne.MOD_ID + "_gui_layer",
                    786432, RenderPipelines.GUI,
                    RenderLayer.MultiPhaseParameters.builder().layering(new RenderPhase.Layering(Melbourne.MOD_ID + "_gl_texture",
                                    () -> RenderSystem.setShaderTexture(0, texture) , () -> RenderSystem.setShaderTexture(0, null)))
                            .build(false)));

    public static List<VertexCollection> QUADS = new ArrayList<>();
    public static List<VertexCollection> DEBUG_LINES = new ArrayList<>();

    public static final Matrix4f PROJECTION_MATRIX = new Matrix4f();
    public static final Matrix4f MODEL_VIEW_MATRIX = new Matrix4f();
    public static final Matrix4f POSITION_MATRIX = new Matrix4f();

    public static void renderBox(WorldContext context, Box box, Color color) {
        renderBox(context, box, color, color);
    }

    public static void renderBox(WorldContext context, Box box, Color color, Color endColor) {
        Matrix4f matrices = context.matrix.peek().getPositionMatrix();
        VertexConsumer consumer = context.buffer.getBuffer(QUADS_LAYER);

        box = cameraTransform(box);

        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());


//      if (!BotManager.INSTANCE.isAuthed())
//          System.exit(0);
    }

    public static void renderBoxOutline(WorldContext context, Box box, Color color) {
        renderBoxOutline(context, box, color, color);
    }

    public static void renderBoxOutline(WorldContext context, Box box, Color color, Color endColor) {
        Matrix4f matrices = context.matrix.peek().getPositionMatrix();
        VertexConsumer consumer = context.buffer.getBuffer(DEBUG_LINES_LAYER);

        box = cameraTransform(box);

        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());

        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());

        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
    }

    public static void renderBoxCross(WorldContext context, Box box, Color color, Color endColor) {
        Matrix4f matrices = context.matrix.peek().getPositionMatrix();
        VertexConsumer consumer = context.buffer.getBuffer(DEBUG_LINES_LAYER);

        box = cameraTransform(box);

        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());

        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());

        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.minZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.minY, (float) box.maxZ).color(endColor.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());

        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        consumer.vertex(matrices, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
    }

    public static void renderLine(WorldContext context, Vec3d startPos, Vec3d endPos, Color startColor, Color endColor) {
        Matrix4f matrices = context.matrix.peek().getPositionMatrix();
        VertexConsumer consumer = context.buffer.getBuffer(DEBUG_LINES_LAYER);

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        consumer.vertex(matrices, (float) (startPos.x - cameraPos.x), (float) (startPos.y - cameraPos.y), (float) (startPos.z - cameraPos.z))
                .color(startColor.getRGB());
        consumer.vertex(matrices, (float) (endPos.x - cameraPos.x), (float) (endPos.y - cameraPos.y), (float) (endPos.z - cameraPos.z))
                .color(endColor.getRGB());
    }

    public static void renderCircle(WorldContext context, Vec3d center, double radius, Color color) {
        Matrix4f matrices = context.matrix.peek().getPositionMatrix();
        VertexConsumer consumer = context.buffer.getBuffer(QUADS_LAYER);
        Vec3d c = center.subtract(mc.gameRenderer.getCamera().getPos());
        int segments = 90, rgb = color.getRGB();
        for (int i = 0; i < segments; i++) {
            double a1 = i * (Math.PI * 2) / segments, a2 = (i + 1) * (Math.PI * 2) / segments;
            consumer.vertex(matrices, (float) c.x, (float) c.y, (float) c.z).color(rgb);
            consumer.vertex(matrices, (float) (c.x + Math.cos(a1) * radius), (float) c.y, (float) (c.z + Math.sin(a1) * radius)).color(rgb);
            consumer.vertex(matrices, (float) (c.x + Math.cos(a2) * radius), (float) c.y, (float) (c.z + Math.sin(a2) * radius)).color(rgb);
            consumer.vertex(matrices, (float) c.x, (float) c.y, (float) c.z).color(rgb);
        }
    }

    public static void renderCircleOutline(WorldContext context, Vec3d center, double radius, Color color) {
        Matrix4f matrices = context.matrix.peek().getPositionMatrix();
        VertexConsumer consumer = context.buffer.getBuffer(DEBUG_LINES_LAYER);
        Vec3d c = center.subtract(mc.gameRenderer.getCamera().getPos());
        int segments = 90, rgb = color.getRGB();
        for (int i = 0; i < segments; i++) {
            double a1 = i * (Math.PI * 2) / segments, a2 = (i + 1) * (Math.PI * 2) / segments;
            consumer.vertex(matrices, (float) (c.x + Math.cos(a1) * radius), (float) c.y, (float) (c.z + Math.sin(a1) * radius)).color(rgb);
            consumer.vertex(matrices, (float) (c.x + Math.cos(a2) * radius), (float) c.y, (float) (c.z + Math.sin(a2) * radius)).color(rgb);
        }
    }

    public static void draw(WorldContext context, List<VertexCollection> quads, List<VertexCollection> debugLines) {
        VertexConsumer consumer;

        if (!quads.isEmpty()) {
            consumer = context.buffer.getBuffer(QUADS_LAYER);

            for (VertexCollection collection : quads)
                collection.vertex(context, consumer);
        }

        if (!debugLines.isEmpty()) {
            consumer = context.buffer.getBuffer(DEBUG_LINES_LAYER);

            for (VertexCollection collection : debugLines)
                collection.vertex(context, consumer);
        }
    }

    public static void renderParticleCircle(WorldContext context, Vec3d pos, float size, Color color, boolean glow) {
        Matrix4f matrices = new Matrix4f(context.matrix.peek().getPositionMatrix());
        VertexConsumer consumer = context.buffer.getBuffer(QUADS_LAYER);
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        matrices.translate((float) (pos.x - cameraPos.x), (float) (pos.y - cameraPos.y), (float) (pos.z - cameraPos.z));
        matrices.rotate(mc.gameRenderer.getCamera().getRotation());

        renderCircleGeometry(matrices, consumer, size, color, false);

        if (glow) {
            renderCircleGeometry(matrices, consumer, size * 3.5f, color, true);
        }
    }

    private static void renderCircleGeometry(Matrix4f matrices, VertexConsumer consumer, float size, Color color, boolean isGlow) {
        float halfSize = size / 2f;
        int segments = isGlow ? 24 : 12;
        int centerColor = color.getRGB();
        int edgeColor = isGlow ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 0).getRGB() : centerColor;

        for (int i = 0; i < segments; i++) {
            double angle1 = i * (Math.PI * 2) / segments;
            double angle2 = (i + 1) * (Math.PI * 2) / segments;
            consumer.vertex(matrices, 0, 0, 0).color(centerColor);
            consumer.vertex(matrices, (float) Math.cos(angle1) * halfSize, (float) Math.sin(angle1) * halfSize, 0).color(edgeColor);
            consumer.vertex(matrices, (float) Math.cos(angle2) * halfSize, (float) Math.sin(angle2) * halfSize, 0).color(edgeColor);
            consumer.vertex(matrices, 0, 0, 0).color(centerColor);
        }
    }

    public static void renderParticleQuad(WorldContext context, Vec3d pos, float size, Color color, boolean glow) {
        Matrix4f matrices = new Matrix4f(context.matrix.peek().getPositionMatrix());
        VertexConsumer consumer = context.buffer.getBuffer(QUADS_LAYER);
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        matrices.translate((float) (pos.x - cameraPos.x), (float) (pos.y - cameraPos.y), (float) (pos.z - cameraPos.z));
        matrices.rotate(mc.gameRenderer.getCamera().getRotation());

        float h = size / 2f;
        int c = color.getRGB();
        consumer.vertex(matrices, -h, -h, 0).color(c);
        consumer.vertex(matrices, -h, h, 0).color(c);
        consumer.vertex(matrices, h, h, 0).color(c);
        consumer.vertex(matrices, h, -h, 0).color(c);

        if (glow) {
            float gh = (size * 3.5f) / 2f;
            int invisible = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0).getRGB();
            int segments = 16;
            for (int i = 0; i < segments; i++) {
                double angle1 = i * (Math.PI * 2) / segments;
                double angle2 = (i + 1) * (Math.PI * 2) / segments;
                consumer.vertex(matrices, 0, 0, 0).color(c);
                consumer.vertex(matrices, (float) Math.cos(angle1) * gh, (float) Math.sin(angle1) * gh, 0).color(invisible);
                consumer.vertex(matrices, (float) Math.cos(angle2) * gh, (float) Math.sin(angle2) * gh, 0).color(invisible);
                consumer.vertex(matrices, 0, 0, 0).color(c);
            }
        }
    }

    public record VertexCollection(Vertex... vertices) {
        public void vertex(WorldContext context, VertexConsumer consumer) {
            Matrix4f matrices = context.matrix.peek().getPositionMatrix();

            for (Vertex vertex : vertices)
                consumer.vertex(matrices, vertex.x, vertex.y, vertex.z).color(vertex.color);
        }
    }

    public static boolean isFrustumVisible(Box box) {
        return ((WorldRendererAccessor) mc.worldRenderer).getFrustum().isVisible(box);
    }

    private static Vec3d cameraTransform(Vec3d vec3d) {
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        return new Vec3d(vec3d.x - camera.getX(),
                vec3d.y - camera.getY(),
                vec3d.z - camera.getZ());
    }

    private static Box cameraTransform(Box box) {
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        return new Box(box.minX - camera.getX(),
                box.minY - camera.getY(),
                box.minZ - camera.getZ(),
                box.maxX - camera.getX(),
                box.maxY - camera.getY(),
                box.maxZ - camera.getZ());
    }

    public static Vec3d project(Vec3d vec3d) {
        vec3d = cameraTransform(vec3d);

        int displayHeight = mc.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        Vector4f transformedCoordinates = new Vector4f((float) vec3d.x, (float) vec3d.y, (float) vec3d.z, 1.f).mul(POSITION_MATRIX);
        Matrix4f matrixProj = new Matrix4f(PROJECTION_MATRIX);
        Matrix4f matrixModel = new Matrix4f(MODEL_VIEW_MATRIX);
        matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);

        double scale = mc.getWindow().getScaleFactor();

        return new Vec3d(target.x / scale, (displayHeight - target.y) / scale, target.z);
    }

    public static boolean projectionVisible(Vec3d vec3d) {
        return vec3d.z > 0 && vec3d.z < 1;
    }

    public record Vertex(float x, float y, float z, int color)
    {

    }
}