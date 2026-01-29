package net.melbourne.gui.main;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.Managers;
import net.melbourne.modules.Category;
import net.melbourne.modules.impl.client.ClickGuiFeature;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.text.Text;
import net.melbourne.Melbourne;
import net.melbourne.gui.main.api.Window;
import net.melbourne.utils.animations.Animation;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.miscellaneous.Timer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;

@Getter @Setter
public class ClickGuiScreen extends Screen {
    private final ArrayList<Window> windows = new ArrayList<>();

    private final Animation animation = new Animation(300, Easing.Method.EASE_OUT_CUBIC);

    private boolean close = false;

    private final Timer timer = new Timer();
    private boolean line = false;

    private Color colorClipboard = null;

    public static final VertexConsumerProvider.Immediate VERTEX_CONSUMERS = VertexConsumerProvider.immediate(new BufferAllocator(786432));

    public ClickGuiScreen() {
        super(Text.literal(Melbourne.MOD_ID + "-click-gui"));

        int x = 3;
        for(Category category : Category.values()) {
            Window window = new Window(category, x, 3);
            windows.add(window);
            x += (int) (window.getWidth() + 4);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float progress = animation.get(close ? 0.0f : 1.0f);

        if (close && progress == 0.0f) {
            this.close();
            return;
        }

        if (timer.hasTimeElapsed(400)) {
            line = !line;
            timer.reset();
        }


//      if (!BotManager.INSTANCE.isAuthed())
//          System.exit(0);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.0f, -height * (1.0f - progress));

        for (Window window : windows) {
            window.render(context, mouseX, mouseY, delta);
        }

        context.getMatrices().popMatrix();

        VERTEX_CONSUMERS.draw();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        windows.forEach(w -> w.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        windows.forEach(w -> w.mouseClicked(mouseX, mouseY, button));

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        windows.forEach(w -> w.mouseReleased(mouseX, mouseY, button));

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        windows.forEach(w -> w.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount));

        return this.hoveredElement(mouseX, mouseY).filter(element -> element.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)).isPresent();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.applyBlur(context);

        float progress = animation.get(close ? 0.0f : 1.0f);
        float p = MathHelper.clamp(progress, 0.0f, 1.0f);
        Renderer2D.renderQuad(context, 0, 0, width, height, new Color(0, 0, 0, (int) (100 * p)));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == GLFW.GLFW_KEY_ESCAPE && !close) {
            close = true;
        }

        windows.forEach(w -> w.keyPressed(keyCode, scanCode, modifiers));

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        windows.forEach(w -> w.charTyped(chr, modifiers));

        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        super.close();
        Managers.FEATURE.getFeatureFromClass(ClickGuiFeature.class).setEnabled(false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
