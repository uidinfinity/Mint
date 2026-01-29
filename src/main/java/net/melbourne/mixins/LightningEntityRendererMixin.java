package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.KillEffectsFeature;
import net.melbourne.modules.impl.render.ParticlesFeature;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.LightningEntityRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(LightningEntityRenderer.class)
public abstract class LightningEntityRendererMixin {

    @Inject(method = "drawBranch", at = @At(value = "HEAD"), cancellable = true)
    private static void onSetLightningVertex(Matrix4f matrix4f, VertexConsumer vertexConsumer, float f, float g, int i, float h, float j, float k, float l, float m, float n, float o, boolean bl, boolean bl2, boolean bl3, boolean bl4, CallbackInfo ci) {

        KillEffectsFeature killEffects = Managers.FEATURE.getFeatureFromClass(KillEffectsFeature.class);
        if (killEffects.isEnabled()) {
            Color color = killEffects.color.getColor();

            vertexConsumer.vertex(matrix4f, f + (bl ? o : -o), (float)(i * 16), g + (bl2 ? o : -o)).color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.3F);
            vertexConsumer.vertex(matrix4f, h + (bl ? n : -n), (float)((i + 1) * 16), j + (bl2 ? n : -n)).color(color.getRed() / 255f, color.getGreen()/ 255f, color.getBlue() / 255f, 0.3F);
            vertexConsumer.vertex(matrix4f, h + (bl3 ? n : -n), (float)((i + 1) * 16), j + (bl4 ? n : -n)).color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.3F);
            vertexConsumer.vertex(matrix4f, f + (bl3 ? o : -o), (float)(i * 16), g + (bl4 ? o : -o)).color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.3F);

            ci.cancel();
        }
    }
}