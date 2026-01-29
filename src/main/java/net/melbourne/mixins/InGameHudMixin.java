package net.melbourne.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.RenderHudEvent;
import net.melbourne.modules.impl.client.HudFeature;
import net.melbourne.modules.impl.render.CrosshairFeature;
import net.melbourne.modules.impl.render.NoRenderFeature;
import net.melbourne.utils.Globals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profilers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin implements Globals {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (mc.options.hudHidden)
            return;

        Profilers.get().push(Melbourne.MOD_ID + "_render2d");

        context.createNewRootLayer();

        RenderHudEvent event = new RenderHudEvent(context);
        Melbourne.EVENT_HANDLER.post(event);

        if (event.isCancelled())
            ci.cancel();

        context.createNewRootLayer();

        Profilers.get().pop();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Managers.FEATURE.getFeatureFromClass(CrosshairFeature.class).isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void renderPortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo info) {
        if (Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).isEnabled() && Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).portalOverlay.getValue())
            info.cancel();
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void renderVignetteOverlay(DrawContext context, Entity entity, CallbackInfo info) {
        if (Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).isEnabled() && Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).vignette.getValue())
            info.cancel();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo info) {
        if (Managers.FEATURE.getFeatureFromClass(HudFeature.class).isEnabled())
            info.cancel();
    }

    @WrapWithCondition(method = "renderMiscOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V", ordinal = 0))
    private boolean renderPumpkinOverlay(InGameHud instance, DrawContext context, Identifier texture, float opacity) {
        return !(Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).isEnabled() && Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).pumpkinOverlay.getValue());
    }

    @WrapWithCondition(method = "renderMiscOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V", ordinal = 1))
    private boolean renderSnowOverlay(InGameHud instance, DrawContext context, Identifier texture, float opacity) {
        return !(Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).isEnabled() && Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).snowOverlay.getValue());
    }
}
