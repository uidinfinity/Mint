package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.player.VelocityFeature;
import net.melbourne.utils.Globals;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin implements Globals {

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void pushAwayFrom(Entity entity, CallbackInfo info) {
        if ((Object) this == mc.player) {
            VelocityFeature velocity = Managers.FEATURE.getFeatureFromClass(VelocityFeature.class);
            if (velocity.isEnabled() && velocity.mode.getValue().equalsIgnoreCase("Cancel")) {
                info.cancel();
            }
        }
    }
}
