package net.melbourne.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.RenderEntityEvent;
import net.melbourne.events.impl.RenderShaderEvent;
import net.melbourne.modules.impl.render.NoRenderFeature;
import net.melbourne.services.Services;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private Vector4f render$noFogColor(Vector4f fogColor) {
        NoRenderFeature nr = Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class);
        if (nr != null && nr.isEnabled() && nr.fog.getValue()) {
            return new Vector4f(fogColor.x, fogColor.y, fogColor.z, 0.0f);
        }
        return fogColor;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        Renderer3D.PROJECTION_MATRIX.set(projectionMatrix);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 0, shift = At.Shift.BEFORE))
    private void render$swap(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo info) {
        Melbourne.EVENT_HANDLER.post(new RenderShaderEvent());
    }

    @WrapOperation(method = "renderEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private <E extends Entity> void renderEntity$render(EntityRenderDispatcher instance, E entity, double x, double y, double z, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Operation<Void> original) {
        RenderEntityEvent event = new RenderEntityEvent(entity, vertexConsumers);
        Melbourne.EVENT_HANDLER.post(event);
        original.call(instance, entity, x, y, z, tickDelta, matrices, event.getVertexConsumers(), light);
    }

    @Inject(method = "onResized", at = @At("TAIL"))
    private void onResized(int width, int height, CallbackInfo info) {
        if (Services.SHADER != null) Services.SHADER.getFramebuffer().resize(width, height);
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void renderWeather(CallbackInfo info) {
        if (Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).isEnabled() && Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).weather.getValue()) {
            info.cancel();
        }
    }
}
