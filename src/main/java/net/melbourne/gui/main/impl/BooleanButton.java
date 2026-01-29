package net.melbourne.gui.main.impl;

import net.melbourne.gui.main.api.Button;
import net.melbourne.gui.main.api.Window;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.utils.animations.Animation;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class BooleanButton extends Button {
    private final BooleanSetting setting;

    private final Animation animation = new Animation(300, Easing.Method.EASE_OUT_CUBIC);
    private final Animation textAnimation = new Animation(300, Easing.Method.EASE_OUT_CUBIC);

    public BooleanButton(BooleanSetting setting, Window window) {
        super(setting, window);
        this.setting = setting;
        animation.get(setting.getValue() ? 150 : 0);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        drawTextWithShadow(context, setting.getName(), getX() + 1, getY() + getVerticalPadding(), getTextColor(textAnimation, setting.getValue()));
        Renderer2D.renderQuad(context, getX() + getWidth() - 5, getY() + 4, getX() + getWidth(), getY() + 9, ColorUtils.getGlobalColor(100 + (int) animation.get(setting.getValue() ? 150 : 0)));
        Renderer2D.renderOutline(context, getX() + getWidth() - 5, getY() + 4, getX() + getWidth(), getY() + 9, Color.BLACK);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(mouseX, mouseY) && button == 0) {
            setting.setValue(!setting.getValue());
        }
    }
}
