package net.melbourne.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.GameLoopEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.impl.misc.AutoRespawnFeature;
import net.melbourne.modules.impl.player.FastPlaceFeature;
import net.melbourne.modules.impl.player.MultiTaskFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    private int itemUseCooldown;

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setOverlay(Lnet/minecraft/client/gui/screen/Overlay;)V", shift = At.Shift.BEFORE))
    private void init(RunArgs args, CallbackInfo ci) {
        Melbourne.onPostInitializeClient();
    }

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    public void setWindowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(Melbourne.NAME + " - " + Melbourne.MOD_VERSION);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;runTasks()V", shift = At.Shift.AFTER))
    private void runTickHook(boolean tick, CallbackInfo info) {
        Melbourne.EVENT_HANDLER.post(new GameLoopEvent());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo info) {
        Melbourne.EVENT_HANDLER.post(new TickEvent());
    }

    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isRiding()Z", ordinal = 0, shift = At.Shift.BY))
    private void doItemUse(CallbackInfo info) {
        if (Managers.FEATURE.getFeatureFromClass(FastPlaceFeature.class).isEnabled() &&
                (Managers.FEATURE.getFeatureFromClass(FastPlaceFeature.class).allowItem(player.getMainHandStack().getItem()) ||
                        Managers.FEATURE.getFeatureFromClass(FastPlaceFeature.class).allowItem(player.getOffHandStack().getItem())))
            itemUseCooldown = Managers.FEATURE.getFeatureFromClass(FastPlaceFeature.class).delay.getValue().intValue();
    }

    @ModifyExpressionValue(method = "handleBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean handleBlockBreaking(boolean original) {
        if (Managers.FEATURE.getFeatureFromClass(MultiTaskFeature.class).isEnabled())
            return false;

        return original;
    }


    @ModifyExpressionValue(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"))
    private boolean handleInputEvents(boolean original) {
        if (Managers.FEATURE.getFeatureFromClass(MultiTaskFeature.class).isEnabled())
            return false;

        return original;
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void setScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof DeathScreen && player != null && Managers.FEATURE.getFeatureFromClass(AutoRespawnFeature.class).isEnabled()) {
            player.requestRespawn();
            info.cancel();
        }
    }
}
