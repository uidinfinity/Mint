package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.MouseEvent;
import net.melbourne.modules.impl.render.FreelookFeature;
import net.melbourne.utils.Globals;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin implements Globals {

    @Shadow
    private double cursorDeltaX;

    @Shadow
    private double cursorDeltaY;

    @Redirect(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void updateMouse(ClientPlayerEntity instance, double dk, double dk1) {
        FreelookFeature freelook = Managers.FEATURE.getFeatureFromClass(FreelookFeature.class);

        if (!freelook.isEnabled()) {
            instance.changeLookDirection(cursorDeltaX, cursorDeltaY);
            return;
        }

        float speed = freelook.speed.getValue().floatValue();
        freelook.yaw += (float) cursorDeltaX / speed;
        freelook.pitch += (float) cursorDeltaY / speed;

        freelook.pitch = Math.max(-90.0F, Math.min(90.0F, freelook.pitch));
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo info) {
        if (window == mc.getWindow().getHandle() && action == 1 && mc.currentScreen == null)
            Melbourne.EVENT_HANDLER.post(new MouseEvent(button));
    }

}
