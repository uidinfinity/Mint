package net.melbourne.mixins;

import net.melbourne.utils.graphics.impl.font.FontUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(DrawContext.class)
public class DrawContextMixin {

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void onDrawText(TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        if (text != null && !text.isEmpty() && FontUtils.isGlobal()) {
            try {
                if (shadow) {
                    FontUtils.drawTextWithShadow((DrawContext) (Object) this, text, (float) x, (float) y, new Color(color, true));
                } else {
                    FontUtils.drawText((DrawContext) (Object) this, text, (float) x, (float) y, new Color(color, true));
                }
                ci.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void onDrawText(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        if (text != null && FontUtils.isGlobal()) {
            try {
                if (shadow) {
                    FontUtils.drawTextWithShadow((DrawContext) (Object) this, text, (float) x, (float) y, new Color(color, true));
                } else {
                    FontUtils.drawText((DrawContext) (Object) this, text, (float) x, (float) y, new Color(color, true));
                }
                ci.cancel();
            } catch (Exception ignored) {
            }
        }
    }
}