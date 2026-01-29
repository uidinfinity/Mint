package net.melbourne.mixins;

import net.melbourne.Melbourne;
import net.melbourne.events.impl.KeyInputEvent;
import net.melbourne.events.impl.KeyboardEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo info) {
        if (client.currentScreen == null)
            Melbourne.EVENT_HANDLER.post(new KeyboardEvent(key, modifiers, action != 0));
    }

    // this is ultra stupid but we doing it eitherwy
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    public void onKey$2(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (key == GLFW.GLFW_KEY_F3) return;

        KeyInputEvent event = new KeyInputEvent(key, scancode, action, modifiers);
        Melbourne.EVENT_HANDLER.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}