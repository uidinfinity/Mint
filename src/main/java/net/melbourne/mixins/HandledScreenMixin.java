package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.ShulkerPreviewFeature;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void drawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        ShulkerPreviewFeature shulkerPreview = Managers.FEATURE.getFeatureFromClass(ShulkerPreviewFeature.class);

        if (shulkerPreview.isEnabled() && shulkerPreview.shouldRender(this.focusedSlot)) {
            ci.cancel();
            shulkerPreview.renderFromMixin(context, this.focusedSlot);
        }
    }
}