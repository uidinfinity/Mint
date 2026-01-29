package net.melbourne.utils.graphics.impl.font;

import lombok.Setter;
import net.melbourne.Managers;
import net.melbourne.modules.impl.client.FontFeature;
import net.melbourne.utils.Globals;
import net.melbourne.utils.font.FontRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

import java.awt.*;

@Setter
public class FontUtils implements Globals {
    public static FontRenderer fontRenderer;

    public static boolean isGlobal() {
        FontFeature font = Managers.FEATURE.getFeatureFromClass(FontFeature.class);
        return font != null && font.isEnabled() && font.global.getValue() && fontRenderer != null;
    }

    public static void drawTextWithShadow(DrawContext context, String text, float x, float y, Color color) {
        if (Managers.FEATURE.getFeatureFromClass(FontFeature.class).isEnabled()) {
            fontRenderer.drawString(context, text, x + getShadowOffset(), y + getShadowOffset(), color.getRGB(), true);
            fontRenderer.drawString(context, text, x, y, color.getRGB(), false);
        } else {
            context.drawText(mc.textRenderer, text, (int) x, (int) y, color.getRGB(), true);
        }
    }

    public static void drawTextWithShadow(DrawContext context, OrderedText text, float x, float y, Color color) {
        if (Managers.FEATURE.getFeatureFromClass(FontFeature.class).isEnabled()) {
            fontRenderer.drawText(context, text, x + getShadowOffset(), y + getShadowOffset(), color.getRGB(), true);
            fontRenderer.drawText(context, text, x, y, color.getRGB(), false);
        } else {
            context.drawText(mc.textRenderer, text, (int) x, (int) y, color.getRGB(), true);
        }
    }

    public static void drawText(DrawContext context, OrderedText text, float x, float y, Color color) {
        if (Managers.FEATURE.getFeatureFromClass(FontFeature.class).isEnabled()) {
            fontRenderer.drawText(context, text, x, y, color.getRGB(), false);
        } else {
            context.drawText(mc.textRenderer, text, (int) x, (int) y, color.getRGB(), false);
        }
    }

    public static void drawText(DrawContext context, String text, float x, float y, Color color) {
        if (Managers.FEATURE.getFeatureFromClass(FontFeature.class).isEnabled()) {
            fontRenderer.drawString(context, text, x, y, color.getRGB(), false);
        } else {
            context.drawText(mc.textRenderer, text, (int) x, (int) y, color.getRGB(), false);
        }
    }

    public static void drawCenteredTextWithShadow(DrawContext context, String text, int centerX, int y, Color color) {
        drawTextWithShadow(context, text, centerX - getWidth(text) / 2.0f, y - getHeight() / 2.0f, color);
    }

    public static float getShadowOffset() {
        return Managers.FEATURE.getFeatureFromClass(FontFeature.class).shadowOffset.getValue().floatValue();
    }

    public static int getWidth(String text) {
        if (Managers.FEATURE.getFeatureFromClass(FontFeature.class).isEnabled()) {
            return (int) fontRenderer.getTextWidth(text);
        } else {
            return mc.textRenderer.getWidth(text);
        }
    }

    public static int getWidth(OrderedText text) {
        if (Managers.FEATURE.getFeatureFromClass(FontFeature.class).isEnabled()) {
            return (int) fontRenderer.getTextWidth(text);
        } else {
            return mc.textRenderer.getWidth(text);
        }
    }

    public static float getHeight() {
        if (Managers.FEATURE.getFeatureFromClass(FontFeature.class).isEnabled()) {
            return fontRenderer.getHeight();
        } else {
            return mc.textRenderer.fontHeight;
        }
    }
}