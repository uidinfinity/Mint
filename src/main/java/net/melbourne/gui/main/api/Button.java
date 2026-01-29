package net.melbourne.gui.main.api;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.settings.Setting;
import net.melbourne.utils.animations.Animation;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.graphics.impl.font.FontUtils;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

@Getter @Setter
public class Button {
    private final Setting setting;
    private final Window window;

    private float x;
    private float y;

    private float width = 92;
    private float height = 13;

    private int horizontalPadding = 4;
    private int verticalPadding = 3;

    private final Animation hoverAnimation = new Animation(300, Easing.Method.EASE_OUT_CUBIC);

    public Button(Window window) {
        this.setting = null;
        this.window = window;
    }

    public Button(Setting setting, Window window) {
        this.setting = setting;
        this.window = window;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) { }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { }

    public void mouseClicked(double mouseX, double mouseY, int button) { }

    public void mouseReleased(double mouseX, double mouseY, int button) { }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) { }

    public void charTyped(char chr, int modifiers) { }

    public boolean isHovering(double mouseX, double mouseY) {
        return x <= mouseX && y <= mouseY && x + width > mouseX && y + height > mouseY;
    }

    public void drawTextWithShadow(DrawContext context, String text, float x, float y, Color color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);

        FontUtils.drawTextWithShadow(context, text, 0, 0, color);

        context.getMatrices().popMatrix();
    }

    public Color getTextColor(Animation animation, boolean flag) {
        int value = 192 + (int) animation.get(flag ? 63 : 0);
        return new Color(value, value, value, 255);
    }
}