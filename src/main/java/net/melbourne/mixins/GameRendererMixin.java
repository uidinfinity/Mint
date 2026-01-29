package net.melbourne.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.RenderEntityEvent;
import net.melbourne.events.impl.RenderShaderEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.impl.render.NoRenderFeature;
import net.melbourne.utils.graphics.api.WorldContext;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private Camera camera;

    @Shadow
    protected abstract void bobView(MatrixStack matrices, float tickDelta);

    @Shadow
    protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickDelta);


    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = {"ldc=hand"}))
    private void renderWorld$swap(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 2) Matrix4f matrix4f3, @Local(ordinal = 1) float tickDelta, @Local MatrixStack matrixStack) {
        if (client.player == null || client.world == null) return;

        RenderSystem.getModelViewStack().pushMatrix();

        RenderSystem.getModelViewStack().mul(matrix4f3);

        MatrixStack matrices = new MatrixStack();
        matrices.push();

        tiltViewWhenHurt(matrices, camera.getLastTickProgress());
        if (client.options.getBobView().getValue())
            bobView(matrices, camera.getLastTickProgress());

        RenderSystem.getModelViewStack().mul(matrices.peek().getPositionMatrix().invert());
        matrices.pop();

        Renderer3D.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        Renderer3D.POSITION_MATRIX.set(matrixStack.peek().getPositionMatrix());

        WorldContext context = new WorldContext(matrixStack, Renderer3D.VERTEX_CONSUMERS);
        Melbourne.EVENT_HANDLER.post(new RenderWorldEvent(context, tickDelta));
        Renderer3D.draw(context, Renderer3D.QUADS, Renderer3D.DEBUG_LINES);

        Renderer3D.VERTEX_CONSUMERS.draw();

        Melbourne.EVENT_HANDLER.post(new RenderWorldEvent.Post(context, tickDelta));
        Melbourne.EVENT_HANDLER.post(new RenderEntityEvent.Post());

        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void renderWorld$TAIL(RenderTickCounter renderTickCounter, CallbackInfo info) {
        Melbourne.EVENT_HANDLER.post(new RenderShaderEvent.Post());
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void tiltViewWhenHurt(CallbackInfo info) {
        if (Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).isEnabled() && Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).hurtCamera.getValue())
            info.cancel();
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void showFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).isEnabled() && Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).totemAnimation.getValue())
            info.cancel();
    }
}
