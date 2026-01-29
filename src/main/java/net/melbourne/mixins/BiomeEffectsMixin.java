package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.AtmosphereFeature;
import net.minecraft.world.biome.BiomeEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeEffects.class)
public class BiomeEffectsMixin {

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void getSkyColor(CallbackInfoReturnable<Integer> info) {
        if (Managers.FEATURE.getFeatureFromClass(AtmosphereFeature.class).isEnabled()) {
            info.cancel();
            info.setReturnValue(Managers.FEATURE.getFeatureFromClass(AtmosphereFeature.class).skyColor.getColor().getRGB());
        }
    }
}
