package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.EntitySpawnEvent;
import net.melbourne.modules.impl.render.AtmosphereFeature;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void getSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> info) {
        if (Managers.FEATURE.getFeatureFromClass(AtmosphereFeature.class).isEnabled()) {
            info.cancel();
            info.setReturnValue(Managers.FEATURE.getFeatureFromClass(AtmosphereFeature.class).skyColor.getColor().getRGB());
        }
    }

    @Inject(method = "addEntity", at = @At(value = "HEAD"))
    private void addEntity(Entity entity, CallbackInfo info) {
        Melbourne.EVENT_HANDLER.post(new EntitySpawnEvent(entity));
    }
}
