package net.melbourne.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.melbourne.Managers;
import net.melbourne.modules.impl.render.AtmosphereFeature;
import net.melbourne.modules.impl.render.FullbrightFeature;
import net.melbourne.modules.impl.render.NoRenderFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.util.OptionalInt;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Shadow
    private boolean dirty;

    @Final
    @Shadow
    private MinecraftClient client;

    @Shadow
    private float flickerIntensity;

    @Shadow
    @Final
    private MappableRingBuffer buffer;

    @Shadow
    @Final
    private GpuTextureView glTextureView;

    @Shadow
    @Final
    private GameRenderer renderer;

    @Shadow
    protected abstract float getDarkness(LivingEntity var1, float var2, float var3);

    @Shadow
    @Final
    private GpuTexture glTexture;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
	private void update(float tickProgress, CallbackInfo ci) {
        if (dirty) {
            dirty = false;
            Profiler profiler = Profilers.get();
            profiler.push("lightTex");
            ClientWorld clientWorld = client.world;
            if (clientWorld != null) {
                float f = clientWorld.getSkyBrightness(1.0F);
                float g;
                if (clientWorld.getLightningTicksLeft() > 0) {
                    g = 1.0F;
                } else {
                    g = f * 0.95F + 0.05F;
                }

                float h = client.options.getDarknessEffectScale().getValue().floatValue();
                float i = client.player.getEffectFadeFactor(StatusEffects.DARKNESS, tickProgress) * h;
                float j = getDarkness(client.player, i, tickProgress) * h;
                float k = client.player.getUnderwaterVisibility();
                float l;
                if (client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    l = GameRenderer.getNightVisionStrength(client.player, tickProgress);
                } else if (k > 0.0F && client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
                    l = k;
                } else {
                    l = 0.0F;
                }

                Vector3f vector3f = (new Vector3f(f, f, 1.0F)).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
                if (Managers.FEATURE.getFeatureFromClass(AtmosphereFeature.class).isEnabled()) {
						Color color = Managers.FEATURE.getFeatureFromClass(AtmosphereFeature.class).ambienceColor.getColor();
						vector3f = new Vector3f(
								color.getRed() / 255f,
								color.getGreen() / 255f,
								color.getBlue() / 255f
						).lerp(new Vector3f(1.0F, 1.0F, 1.0F), Managers.FEATURE.getFeatureFromClass(AtmosphereFeature.class).vibrancy.getValue().floatValue());
                }
                
                float m = flickerIntensity + 1.5F;
                float n = clientWorld.getDimension().ambientLight();
                boolean bl = clientWorld.getDimensionEffects().shouldBrightenLighting();
                float o = client.options.getGamma().getValue().floatValue();
                RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
                GpuBuffer gpuBuffer = shapeIndexBuffer.getIndexBuffer(6);
                CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

                try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(buffer.getBlocking(), false, true)) {
                    Color color = Managers.FEATURE.getFeatureFromClass(AtmosphereFeature.class).ambienceColor.getColor();
                    Std140Builder.intoBuffer(mappedView.data())
                            .putFloat(n).putFloat(g).putFloat(m).putInt(bl ? 1 : 0).putFloat(l).putFloat(j)
                            .putFloat(renderer.getSkyDarkness(tickProgress)).putFloat(Math.max(0.0F, o - i))
                            .putVec3(new Vector3f(color.getRed() / 255f,
                                    color.getGreen() / 255f,
                                    color.getBlue() / 255f));
                }

                try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "Update light", glTextureView, OptionalInt.empty())) {
                    renderPass.setPipeline(RenderPipelines.BILT_SCREEN_LIGHTMAP);
                    RenderSystem.bindDefaultUniforms(renderPass);
                    renderPass.setUniform("LightmapInfo", buffer.getBlocking());
                    renderPass.setVertexBuffer(0, RenderSystem.getQuadVertexBuffer());
                    renderPass.setIndexBuffer(gpuBuffer, shapeIndexBuffer.getIndexType());
                    renderPass.drawIndexed(0, 0, 6, 1);
                }

                buffer.rotate();
                profiler.pop();
            }
        }
    }

    @Inject(method = "getDarkness", at = @At("HEAD"), cancellable = true)
    private void getDarknessFactor(LivingEntity entity, float factor, float tickProgress, CallbackInfoReturnable<Float> info) {
        if (Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).isEnabled() && Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).blindness.getValue())
            info.setReturnValue(0.0f);
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", shift = At.Shift.AFTER), cancellable = true)
    private void update$skip(float tickProgress, CallbackInfo ci, @Local Profiler profiler) {
        if (Managers.FEATURE.getFeatureFromClass(FullbrightFeature.class).isEnabled() && Managers.FEATURE.getFeatureFromClass(FullbrightFeature.class).mode.getValue().equalsIgnoreCase("Gamma")) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(glTexture, ColorHelper.getArgb(255, 255, 255, 255));
            profiler.pop();
            ci.cancel();
        }
    }

}
