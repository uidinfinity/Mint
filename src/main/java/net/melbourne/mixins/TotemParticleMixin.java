package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.ParticlesFeature;
import net.minecraft.client.particle.AnimatedParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TotemParticle.class)
public abstract class TotemParticleMixin extends AnimatedParticle {
    protected TotemParticleMixin(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider, float upwardsAcceleration) {
        super(world, x, y, z, spriteProvider, upwardsAcceleration);
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void onInit(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider, CallbackInfo ci) {
        ParticlesFeature particles = Managers.FEATURE.getFeatureFromClass(ParticlesFeature.class);

        if (particles != null && particles.isEnabled() && particles.types.getWhitelistIds().contains("Totem")) {
            this.scale *= particles.scale.getValue().floatValue();

            boolean useColor1 = random.nextBoolean();
            int color = useColor1 ? particles.totemColor.getColor().getRGB() : particles.totemColor.getColor().getRGB();

            this.red = (float) (color >> 16 & 255) / 255.0F;
            this.green = (float) (color >> 8 & 255) / 255.0F;
            this.blue = (float) (color & 255) / 255.0F;
        }
    }
}