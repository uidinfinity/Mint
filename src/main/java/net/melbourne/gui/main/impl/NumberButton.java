package net.melbourne.gui.main.impl;

import net.melbourne.Melbourne;
import net.melbourne.utils.Globals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import net.melbourne.gui.main.api.Button;
import net.melbourne.gui.main.api.Window;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.melbourne.utils.graphics.impl.font.FontUtils;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.melbourne.utils.miscellaneous.math.MathUtils;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class NumberButton extends Button implements Globals {
    private final NumberSetting setting;

    private boolean dragging = false;

    private boolean listening = false;
    private String currentString = "";

    public NumberButton(NumberSetting setting, Window window) {
        super(setting, window);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float width = MathHelper.clamp(mouseX - getX(), 0, getWidth());
        float percentage;

        switch (setting.getType()) {
            case INTEGER -> {
                int difference = setting.getMaximum().intValue() - setting.getMinimum().intValue();

                if (dragging) {
                    int value = (int) MathUtils.round(difference * (width / getWidth()) + setting.getMinimum().intValue(), 0);
                    setting.setValue(value);
                }

                percentage = (float) (setting.getValue().intValue() - setting.getMinimum().intValue()) / difference;
            }
            case FLOAT -> {
                float difference = setting.getMaximum().floatValue() - setting.getMinimum().floatValue();

                if (dragging) {
                    float value = (float) MathUtils.round(difference * (width / getWidth()) + setting.getMinimum().floatValue(), 2);
                    setting.setValue(value);
                }

                percentage = (setting.getValue().floatValue() - setting.getMinimum().floatValue()) / difference;
            }
            case DOUBLE -> {
                double difference = setting.getMaximum().doubleValue() - setting.getMinimum().doubleValue();

                if (dragging) {
                    double value = MathUtils.round(difference * (width / getWidth()) + setting.getMinimum().doubleValue(), 2);
                    setting.setValue(value);
                }

                percentage = (float) (setting.getValue().doubleValue() - setting.getMinimum().doubleValue()) / (float) difference;
            }
            default -> {
                long difference = setting.getMaximum().longValue() - setting.getMinimum().longValue();

                if (dragging) {
                    long value = (long) MathUtils.round((difference * (width / getWidth())) + setting.getMinimum().longValue(), 0);
                    setting.setValue(value);
                }

                percentage = (float) (setting.getValue().longValue() - setting.getMinimum().longValue()) / difference;
            }
        }

        if (listening) {
            drawTextWithShadow(context, currentString + (Melbourne.CLICK_GUI.isLine() ? "|" : " "), getX() + (getWidth()/2) - (FontUtils.getWidth(currentString) / 2.0f), getY() + getVerticalPadding(), Color.WHITE);
        } else {
            drawTextWithShadow(context, setting.getName(), getX() + 1, getY() + getVerticalPadding(), Color.WHITE);
            drawTextWithShadow(context, setting.getValue() + "", getX() + getWidth() - FontUtils.getWidth(setting.getValue() + ""), getY() + getVerticalPadding(), Color.LIGHT_GRAY);
        }

        Renderer2D.renderQuad(context, getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), ColorUtils.getGlobalColor(100));
        Renderer2D.renderQuad(context, getX(), getY() + getHeight() - 1, getX() + (getWidth() * percentage), getY() + getHeight(), ColorUtils.getGlobalColor());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isHovering(mouseX, mouseY))
                dragging = true;

            listening = false;
        } else if (button == 1) {
            if (isHovering(mouseX, mouseY) && !listening) {
                listening = true;
                currentString = "";
            } else {
                listening = false;
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if(button == 0)
            dragging = false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!listening)
            return;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            listening = false;
            return;
        }

        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_ENTER)) {
            try {
                switch (setting.getType()) {
                    case LONG -> setting.setValue(Long.parseLong(currentString));
                    case DOUBLE -> setting.setValue(Double.parseDouble(currentString));
                    case FLOAT -> setting.setValue(Float.parseFloat(currentString));
                    default -> setting.setValue(Integer.parseInt(currentString));
                }
            } catch (NumberFormatException exception) {
                Melbourne.getLogger().error("Invalid value, expected a {}.", setting.getType());
            }
            listening = false;
            return;
        }

        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_BACKSPACE)) {
            currentString = (!currentString.isEmpty() ? currentString.substring(0, currentString.length() - 1) : currentString);
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (listening) currentString = currentString + chr;
    }
}
