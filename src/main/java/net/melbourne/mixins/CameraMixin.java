package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.FreelookFeature;
import net.melbourne.modules.impl.render.ViewClipFeature;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public class CameraMixin {

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void update$setRotationArgs(Args args) {
        if (Managers.FEATURE.getFeatureFromClass(FreelookFeature.class).isEnabled()) {
            args.set(0, Managers.FEATURE.getFeatureFromClass(FreelookFeature.class).yaw);
            args.set(1, Managers.FEATURE.getFeatureFromClass(FreelookFeature.class).pitch);
        }
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"))
    private void update(Args args) {
        if (Managers.FEATURE.getFeatureFromClass(ViewClipFeature.class).isEnabled())
            args.set(0, Managers.FEATURE.getFeatureFromClass(ViewClipFeature.class).distance.getValue().floatValue());
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void clipToSpace(float f, CallbackInfoReturnable<Float> info) {
        if (Managers.FEATURE.getFeatureFromClass(ViewClipFeature.class).isEnabled())
            info.setReturnValue(f);
    }

}
