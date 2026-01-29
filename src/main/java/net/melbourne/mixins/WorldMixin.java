package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.NoRenderFeature;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    public void onGetRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).isEnabled() && Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).weather.getValue()) {
            cir.setReturnValue(0.0f);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    public void onGetThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).isEnabled() && Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).weather.getValue()) {
            cir.setReturnValue(0.0f);
        }
    }
}