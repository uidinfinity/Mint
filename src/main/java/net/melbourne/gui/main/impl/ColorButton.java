package net.melbourne.gui.main.impl;

import net.melbourne.Melbourne;
import net.melbourne.utils.Globals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import net.melbourne.gui.main.api.Button;
import net.melbourne.gui.main.api.Window;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.graphics.impl.Renderer2D;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class ColorButton extends Button implements Globals {
    private final ColorSetting setting;
    private final float size = super.getWidth() - 6;

    private long openTime = 0L;
    private float currentProgress = 0;

    private boolean open = false;
    private boolean hovering = false;

    private final PickerElement picker = new PickerElement(0, 0, size, size);
    private final PickerElement hue = new PickerElement(size + 2, 0, super.getWidth(), size);
    private final PickerElement alpha = new PickerElement(0, size + 2, size, super.getWidth());

    private float[] hsb;

    public ColorButton(ColorSetting setting, Window window) {
        super(setting, window);
        this.setting = setting;

        hsb = getHSB();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        hovering = isHoveringElement(mouseX, mouseY, picker);

        drawTextWithShadow(context, setting.getName(), getX() + 1, getY() + getVerticalPadding(), Color.WHITE);
        Renderer2D.renderQuad(context, getX() + getWidth() - 10, getY() + 4.5f, getX() + getWidth(), getY() + super.getHeight() - 4.5f, setting.getColor());
        Renderer2D.renderOutline(context, getX() + getWidth() - 10, getY() + 4.5f, getX() + getWidth(), getY() + super.getHeight() - 4.5f, Color.BLACK);

        if (open) {
            float width = MathHelper.clamp(mouseX - getX(), 0, size);
            float height = MathHelper.clamp(mouseY - getY() - super.getHeight(), 0, size);

            if (picker.dragging) {
                hsb[1] = width/size;
                hsb[2] = 1.0f - (height/size);
                updateHSB();
            } else if (hue.dragging) {
                hsb[0] = height/size;
                updateHSB();
            } else if (alpha.dragging) {
                Color newColor = new Color(setting.getColor().getRed(), setting.getColor().getGreen(), setting.getColor().getBlue(), (int) (255 * (width/size)));
                setting.setColor(newColor);
            }
        }

        float scale = Easing.ease(Easing.toDelta(openTime, 150), Easing.Method.EASE_OUT_CUBIC);

        if (open || scale != 1.0f) {
            if (scale != 1.0f)
                context.enableScissor((int) getX(), (int) (getY() + super.getHeight()), (int) (getX() + getWidth()), (int) (getY() + super.getHeight() + (super.getWidth() * (open ? scale : 1.0f - scale))));

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0, -super.getWidth() + (super.getWidth() * (open ? scale : 1.0f - scale)));

            Renderer2D.renderSidewaysGradient(context, getX() + picker.left, getY() + super.getHeight() + picker.top, getX() + picker.right, getY() + super.getHeight() + picker.bottom, Color.WHITE, Color.getHSBColor(hsb[0], 1, 1));
            Renderer2D.renderGradient(context, getX() + picker.left, getY() + super.getHeight() + picker.top, getX() + picker.right, getY() + super.getHeight() + picker.bottom, new Color(0, 0, 0, 0), Color.BLACK);
            Renderer2D.renderOutline(context, getX() + picker.left, getY() + super.getHeight() + picker.top, getX() + picker.right, getY() + super.getHeight() + picker.bottom, Color.BLACK);

            Renderer2D.renderQuad(context, getX() + (size * hsb[1]) - 1.5f, getY() + super.getHeight() + (size * (1.0f - hsb[2])) - 1.5f, getX() + (size * hsb[1]) + 1.5f, getY() + super.getHeight() + (size * (1.0f - hsb[2])) + 1.5f, Color.BLACK);
            Renderer2D.renderQuad(context, getX() + (size * hsb[1]) - 0.5f, getY() + super.getHeight() + (size * (1.0f - hsb[2])) - 0.5f, getX() + (size * hsb[1]) + 0.5f, getY() + super.getHeight() + (size * (1.0f - hsb[2])) + 0.5f, Color.WHITE);

            for (float i = 0; i < size; i += 0.5f) {
                Renderer2D.renderQuad(context, getX() + hue.left, getY() + super.getHeight() + i, getX() + hue.right, getY() + super.getHeight() + i + 0.5f, Color.getHSBColor(i/size, 1.0f, 1.0f));
            }
            Renderer2D.renderOutline(context, getX() + hue.left, getY() + super.getHeight() + hue.top, getX() + hue.right, getY() + super.getHeight() + hue.bottom, Color.BLACK);

            float hueProgress = size * hsb[0];
            Renderer2D.renderQuad(context, getX() + hue.left - 0.5f, getY() + super.getHeight() + hueProgress - 1.5f, getX() + hue.right + 0.5f, getY() + super.getHeight() + hueProgress + 1.5f, Color.BLACK);
            Renderer2D.renderQuad(context, getX() + hue.left, getY() + super.getHeight() + hueProgress - 0.5F, getX() + hue.right, getY() + super.getHeight() + hueProgress + 0.5F, Color.WHITE);

            Renderer2D.renderSidewaysGradient(context, getX() + alpha.left, getY() + super.getHeight() + alpha.top, getX() + alpha.right, getY() + super.getHeight() + alpha.bottom, Color.BLACK, Color.getHSBColor(hsb[0], 1, 1));
            Renderer2D.renderOutline(context, getX() + alpha.left, getY() + super.getHeight() + alpha.top, getX() + alpha.right, getY() + super.getHeight() + alpha.bottom, Color.BLACK);

            float alphaProgress = size * (setting.getColor().getAlpha()/255f);
            Renderer2D.renderQuad(context, getX() + alphaProgress - 1.5f, getY() + super.getHeight() + alpha.top - 0.5f, getX() + alphaProgress + 1.5f, getY() + super.getHeight() + alpha.bottom + 0.5f, Color.BLACK);
            Renderer2D.renderQuad(context, getX() + alphaProgress - 0.5f, getY() + super.getHeight() + alpha.top, getX() + alphaProgress + 0.5f, getY() + super.getHeight() + alpha.bottom, Color.WHITE);

            context.getMatrices().popMatrix();

            if (scale != 1.0f)
                context.disableScissor();
        }

        currentProgress = super.getWidth() * (open ? scale : 1.0f - scale);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(mouseX, mouseY) && button == 1) {
            open = !open;
            openTime = System.currentTimeMillis();
        }

        if (isHoveringElement(mouseX, mouseY, picker) && button == 0) {
            picker.dragging = true;
        }

        if (isHoveringElement(mouseX, mouseY, hue) && button == 0) {
            hue.dragging = true;
        }

        if (isHoveringElement(mouseX, mouseY, alpha) && button == 0) {
            alpha.dragging = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        picker.dragging = false;
        hue.dragging = false;
        alpha.dragging = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0) return false;

        if (isHoveringElement(mouseX, mouseY, hue) && !hue.dragging) {
            hsb[0] = MathHelper.clamp(hsb[0] + ((1.0f/25) *  (verticalAmount < 0 ? 1 : -1)), 0, 1.0f);
            updateHSB();
        }

        if (isHoveringElement(mouseX, mouseY, alpha) && !alpha.dragging) {
            int alpha = MathHelper.clamp(setting.getColor().getAlpha() + ((255/25) *  (verticalAmount > 0 ? 1 : -1)), 0, 255);
            Color newColor = new Color(setting.getColor().getRed(), setting.getColor().getGreen(), setting.getColor().getBlue(), alpha);
            setting.setColor(newColor);
        }
        return false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open || !hovering) return;

        long handle = mc.getWindow().getHandle();
        boolean ctrl = InputUtil.isKeyPressed(handle, MinecraftClient.IS_SYSTEM_MAC ? GLFW.GLFW_KEY_LEFT_SUPER : GLFW.GLFW_KEY_LEFT_CONTROL);

        if (ctrl) {
            if (keyCode == GLFW.GLFW_KEY_C) {
                Melbourne.CLICK_GUI.setColorClipboard(setting.getColor());
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_V && Melbourne.CLICK_GUI.getColorClipboard() != null) {
                setting.setColor(Melbourne.CLICK_GUI.getColorClipboard());
                hsb = getHSB();
            }
        }
    }

    @Override
    public boolean isHovering(double mouseX, double mouseY) {
        return getX() <= mouseX && getY() <= mouseY && getX() + getWidth() > mouseX && getY() + super.getHeight() > mouseY;
    }

    private boolean isHoveringElement(double mouseX, double mouseY, PickerElement element) {
        return getX() + element.left <= mouseX && getY() + super.getHeight() + element.top <= mouseY && getX() + element.right > mouseX && getY() + super.getHeight() + element.bottom > mouseY;
    }

    private float[] getHSB() {
        return Color.RGBtoHSB(setting.getColor().getRed(), setting.getColor().getGreen(), setting.getColor().getBlue(), null);
    }

    private void updateHSB() {
        Color rgb = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
        Color newColor = new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), setting.getColor().getAlpha());
        setting.setColor(newColor);
    }

    @Override
    public float getHeight() {
        return super.getHeight() + currentProgress;
    }

    private static class PickerElement {
        private final float left;
        private final float top;
        private final float right;
        private final float bottom;

        private boolean dragging = false;

        public PickerElement(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
