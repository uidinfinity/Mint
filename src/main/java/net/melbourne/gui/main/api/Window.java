package net.melbourne.gui.main.api;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.Managers;
import net.melbourne.gui.main.impl.FeatureButton;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.utils.animations.Animation;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.melbourne.utils.graphics.impl.font.FontUtils;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;

@Getter @Setter
public class Window {
    private final Category category;
    private final ArrayList<FeatureButton> buttons = new ArrayList<>();

    private final Animation animation = new Animation(300, Easing.Method.EASE_OUT_CUBIC);

    private long openTime = 0L;

    private float x;
    private float y;

    private final float width = 100;
    private final float height = 14;

    private int dragX = 0;
    private int dragY = 0;

    private boolean open = true;
    private boolean dragging = false;

    public Window(Category category, int x, int y) {
        this.category = category;

        for (Feature feature : Managers.FEATURE.getFeaturesInCategory(category)) {
            this.buttons.add(new FeatureButton(feature, this));
        }

        this.x = x;
        this.y = y;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (dragging) {
            setX(mouseX - dragX);
            setY(mouseY - dragY);
        }

        Renderer2D.renderQuad(context, x, y, x + width, y + height, ColorUtils.getGlobalColor(200));

        int alpha = (int) animation.get(dragging ? 75 : 0);
        Renderer2D.renderQuad(context, x, y, x + width, y + height, new Color(0, 0, 0, alpha));

        FontUtils.drawCenteredTextWithShadow(context, category.getName(), (int) (x + width/2), (int) (y + height/2), Color.WHITE);

        float currentY = height + 1;
        float targetY = 2;

        for(FeatureButton button : buttons) {
            button.setX(x + 4);
            button.setY(y + currentY);

            currentY += button.getHeight();
            targetY += button.getHeight();
        }

        float scale = Easing.ease(Easing.toDelta(openTime, 150), Easing.Method.EASE_OUT_CUBIC);

        if (open || scale != 1.0f) {
            if(scale != 1.0f)
                context.enableScissor((int) x, (int) (y + height), (int) (x + width), (int) (y + height + (targetY * (open ? scale : 1.0f - scale)) + 2));

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0, -targetY + (targetY * (open ? scale : 1.0f - scale)));

            Renderer2D.renderQuad(context, x, y + height, x + width, y + currentY + 1, new Color(30, 30, 30, 150));
            Renderer2D.renderOutline(context, x, y + height, x + width, y + currentY + 1, ColorUtils.getGlobalColor(200));

            Renderer2D.renderOutline(context, x, y + height, x + width, y + currentY + 1, new Color(0, 0, 0, alpha));

            buttons.forEach(b -> b.render(context, mouseX, mouseY, delta));

            context.getMatrices().popMatrix();

            if(scale != 1.0f)
                context.disableScissor();
        }
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {

    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                dragging = true;
                dragX = (int) (mouseX - x);
                dragY = (int) (mouseY - y);
                return true;
            } else if(button == 1) {
                open = !open;
                openTime = System.currentTimeMillis();
                return true;
            }
        }

        if(open) buttons.forEach(b -> b.mouseClicked(mouseX, mouseY, button));

        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if(button == 0) dragging = false;

        buttons.forEach(b -> b.mouseReleased(mouseX, mouseY, button));
    }

    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (x <= mouseX && x + width > mouseX) {
            if (verticalAmount < 0) {
                setY(getY() - 15);
            } else if (verticalAmount > 0) {
                setY(getY() + 15);
            }
        }

        if(open) buttons.forEach(b -> b.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount));
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if(open) buttons.forEach(b -> b.keyPressed(keyCode, scanCode, modifiers));
    }

    public void charTyped(char chr, int modifiers) {
        if(open) buttons.forEach(b -> b.charTyped(chr, modifiers));
    }

    public boolean isHovering(double mouseX, double mouseY) {
        return x <= mouseX && y <= mouseY && x + width > mouseX && y + height > mouseY;
    }
}