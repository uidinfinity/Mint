package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.AtmosphereFeature;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AtmosphericFogModifier.class)
public class AtmosphericFogModifierMixin {

    @Inject(method = "applyStartEndModifier", at = @At("TAIL"))
    private void onApply(FogData data, Entity cameraEntity, BlockPos cameraPos, ClientWorld world, float viewDistance, RenderTickCounter tickCounter, CallbackInfo ci) {
        AtmosphereFeature atmosphere = Managers.FEATURE.getFeatureFromClass(AtmosphereFeature.class);

        if (atmosphere.isEnabled() && atmosphere.fogModifier.getValue()) {
            data.environmentalStart = viewDistance * atmosphere.fogDistance.getValue().floatValue();
            data.environmentalEnd = viewDistance;
        }
    }
}