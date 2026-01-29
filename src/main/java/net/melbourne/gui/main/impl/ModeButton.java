package net.melbourne.gui.main.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.melbourne.gui.main.api.Button;
import net.melbourne.gui.main.api.Window;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.graphics.impl.font.FontUtils;

import java.awt.*;

public class ModeButton extends Button {
    private final ModeSetting setting;

    private long openTime = 0L;
    private float currentProgress = 0;

    private boolean open = false;

    public ModeButton(ModeSetting setting, Window window) {
        super(setting, window);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        String text = open ? "..." : setting.getValue();
        drawTextWithShadow(context, setting.getName(), getX() + 1, getY() + getVerticalPadding(), Color.WHITE);
        drawTextWithShadow(context, text, getX() + getWidth() - FontUtils.getWidth(text), getY() + getVerticalPadding(), Color.LIGHT_GRAY);

        float targetY = super.getHeight() * setting.getModes().size();
        float scale = Easing.ease(Easing.toDelta(openTime, 150), Easing.Method.EASE_OUT_CUBIC);

        if (open || scale != 1.0f) {
            float currentY = 0;

            if(scale != 1.0f)
                context.enableScissor((int) getX(), (int) (getY() + super.getHeight()), (int) (getX() + getWidth()), (int) (getY() + super.getHeight() + (targetY * (open ? scale : 1.0f - scale))));

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0, -targetY + (targetY * (open ? scale : 1.0f - scale)));

            for (String value : setting.getModes()) {
                drawTextWithShadow(context, value, getX() + (getWidth()/2) - (FontUtils.getWidth(value) / 2.0f), getY() + getVerticalPadding() + super.getHeight() + currentY, setting.equalsValue(value) ? Color.WHITE : Color.GRAY);
                currentY += super.getHeight();
            }

            context.getMatrices().popMatrix();

            if (scale != 1.0f)
                context.disableScissor();
        }

        currentProgress = targetY * (open ? scale : 1.0f - scale);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(mouseX, mouseY)) {
            if (button == 0) {
                int choice = setting.getModes().indexOf(setting.getValue());
                choice++;
                if (choice > setting.getModes().size() - 1) choice = 0;

                setting.setValue(setting.getModes().get(choice));
            } else if (button == 1) {
                open = !open;
                openTime = System.currentTimeMillis();
            }
        }

        if (open && button == 0 && isHoveringValues(mouseX, mouseY)) {
            int choice = MathHelper.clamp((int) ((mouseY - getY() - super.getHeight()) / super.getHeight()), 0, setting.getModes().size() - 1);
            setting.setValue(setting.getModes().get(choice));
        }
    }

    @Override
    public boolean isHovering(double mouseX, double mouseY) {
        return getX() <= mouseX && getY() <= mouseY && getX() + getWidth() > mouseX && getY() + super.getHeight() > mouseY;
    }

    private boolean isHoveringValues(double mouseX, double mouseY) {
        float height = super.getHeight() * setting.getModes().size();
        return getX() <= mouseX && getY() + super.getHeight() <= mouseY && getX() + getWidth() > mouseX && getY() + super.getHeight() + height > mouseY;
    }

    @Override
    public float getHeight() {
        return super.getHeight() + currentProgress;
    }
}
