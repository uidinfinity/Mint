package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.ParticlesFeature;
import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(FireworksSparkParticle.FireworkParticle.class)
public class FireworkParticleMixin extends NoRenderParticle {
    protected FireworkParticleMixin(ClientWorld clientWorld, double d, double e, double f) {
        super(clientWorld, d, e, f);
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void init(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, ParticleManager particleManager, List fireworkExplosions, CallbackInfo ci) {
        ParticlesFeature particles = Managers.FEATURE.getFeatureFromClass(ParticlesFeature.class);

        if (particles.isEnabled() && particles.types.getWhitelistIds().contains("Rockets")) {
            scale(particles.scale.getValue().floatValue());
            int rando = random.nextInt(2);
            switch (rando) {
                case 0 -> setColor(particles.rocketColor.getColor().getRed() / 255.0f,
                        particles.rocketColor.getColor().getGreen() / 255.0f,
                        particles.rocketColor.getColor().getBlue() / 255.0f);
                case 1 -> setColor(particles.rocketColor.getColor().getRed() / 255.0f,
                        particles.rocketColor.getColor().getGreen() / 255.0f,
                        particles.rocketColor.getColor().getBlue() / 255.0f);
            }
        }
    }
}