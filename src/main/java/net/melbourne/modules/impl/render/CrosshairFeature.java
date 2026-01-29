package net.melbourne.modules.impl.render;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderHudEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec2f;

import java.awt.*;

@FeatureInfo(name = "Crosshair", category = Category.Render)
public class CrosshairFeature extends Feature {

    public NumberSetting width = new NumberSetting("Width", "The width of the crosshair.", 1.5, 0.0, 5.0);
    public NumberSetting height = new NumberSetting("Height", "The height of the crosshair.", 4.0, 0.0, 10.0);
    public NumberSetting gap = new NumberSetting("Gap", "The gap of the crosshair.", 2.0, 0.0, 10.0);
    public BooleanSetting dynamic = new BooleanSetting("Dynamic", "Makes the crosshair dynamic.", false);
    public BooleanSetting outline = new BooleanSetting("Outline", "Whether or not to render an outline for the crosshair.", true);
    public ColorSetting color = new ColorSetting("Color", "The color used for the rendering.", new Color(255, 255, 255));

    @SubscribeEvent
    public void onRenderHud(RenderHudEvent event) {
        if (getNull()) return;

        DrawContext context = event.getContext();

        float x = mc.getWindow().getScaledWidth() / 2f;
        float y = mc.getWindow().getScaledHeight() / 2f;

        float w = width.getValue().floatValue() / 2f;
        float h = height.getValue().floatValue();
        float g = gap.getValue().floatValue() + (moving() ? 2f : 0f);

        Renderer2D.renderQuad(context, x - w, y - h - g, x + w, y - g, color.getColor());
        Renderer2D.renderQuad(context, x + g, y - w, x + h + g, y + w, color.getColor());
        Renderer2D.renderQuad(context, x - w, y + g, x + w, y + h + g, color.getColor());
        Renderer2D.renderQuad(context, x - h - g, y - w, x - g, y + w, color.getColor());

        if (outline.getValue()) {
            Renderer2D.renderOutline(context, x - w, y - h - g, x + w, y - g, Color.BLACK);
            Renderer2D.renderOutline(context, x + g, y - w, x + h + g, y + w, Color.BLACK);
            Renderer2D.renderOutline(context, x - w, y + g, x + w, y + h + g, Color.BLACK);
            Renderer2D.renderOutline(context, x - h - g, y - w, x - g, y + w, Color.BLACK);
        }
    }

    private boolean moving() {
        if (!dynamic.getValue()) return false;
        Vec2f movement = mc.player.input.getMovementInput();
        boolean isMoving = movement.x != 0 || movement.y != 0;
        return (mc.player.isSneaking() || isMoving || !mc.player.isOnGround());
    }
}