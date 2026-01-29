package net.melbourne.gui.main.impl;

import net.melbourne.gui.main.api.Button;
import net.melbourne.gui.main.api.Window;
import net.melbourne.settings.types.BindSetting;
import net.melbourne.utils.graphics.impl.font.FontUtils;
import net.melbourne.utils.input.KeyboardUtils;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class BindButton extends Button {
    private final BindSetting setting;

    private boolean listening = false;

    public BindButton(BindSetting setting, Window window) {
        super(setting, window);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        String text = (listening ? "..." : KeyboardUtils.getKeyName(setting.getValue()));
        drawTextWithShadow(context, setting.getName(), getX() + 1, getY() + getVerticalPadding(), Color.WHITE);
        drawTextWithShadow(context, text, getX() + getWidth() - FontUtils.getWidth(text), getY() + getVerticalPadding(), Color.LIGHT_GRAY);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(mouseX, mouseY)) {
            if (button == 0) {
                listening = true;
            } else if (button == 1) {
                setting.setValue(0);
            }

            if (listening && (button == 1 || button == 2 || button == 3 || button == 4)) {
                setting.setValue(-button - 1);
                listening = false;
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                setting.setValue(0);
            } else {
                setting.setValue(keyCode);
            }

            listening = false;
        }
    }
}
