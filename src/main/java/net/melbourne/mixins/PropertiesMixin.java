package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.AtmosphereFeature;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.Properties.class)
public class PropertiesMixin {

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void getTimeOfDay(CallbackInfoReturnable<Long> info) {
        AtmosphereFeature atmosphere = Managers.FEATURE.getFeatureFromClass(AtmosphereFeature.class);
        if (atmosphere.isEnabled() && atmosphere.timeChanger.getValue()) {
            info.setReturnValue(atmosphere.time.getValue().longValue());
        }
    }
}