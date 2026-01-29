package net.melbourne.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.KeyboardTickEvent;
import net.melbourne.utils.Globals;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends Input implements Globals {

    @Unique
    private KeyboardTickEvent event;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z", ordinal = 4))
    private boolean jumpables(KeyBinding instance) {
        return event != null && event.isJump();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/KeyboardInput;getMovementMultiplier(ZZ)F", ordinal = 0))
    private float forwardables(boolean positive, boolean negative) {
        return event == null ? 0 : event.getForward();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/KeyboardInput;getMovementMultiplier(ZZ)F", ordinal = 1))
    private float strafables(boolean positive, boolean negative) {
        return event == null ? 0 : event.getSideways();
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
    private PlayerInput tick(PlayerInput original) {
        event = new KeyboardTickEvent(
                getMovementMultiplier(mc.options.forwardKey.isPressed(), mc.options.backKey.isPressed()),
                getMovementMultiplier(mc.options.leftKey.isPressed(), mc.options.rightKey.isPressed()),
                mc.options.jumpKey.isPressed(), original.sneak()
        );

        Melbourne.EVENT_HANDLER.post(event);

        return new PlayerInput(
                event.getForward() > 0,
                event.getForward() < 0,
                event.getSideways() > 0,
                event.getSideways() < 0,
                event.isJump(),
                event.isSneak(),
                original.sprint()
        );
    }

    @Unique
    private static float getMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative)
            return 0.0f;

        return positive ? 1.0f : -1.0f;
    }

}