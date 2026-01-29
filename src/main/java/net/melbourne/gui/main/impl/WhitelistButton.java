package net.melbourne.gui.main.impl;

import net.melbourne.gui.main.api.Button;
import net.melbourne.gui.main.api.Window;
import net.melbourne.settings.types.WhitelistSetting;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.melbourne.utils.graphics.impl.font.FontUtils;
import net.melbourne.utils.inventory.IdentifierUtils;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WhitelistButton extends Button {
    private final WhitelistSetting setting;
    private boolean open = false;
    private long openTime = 0L;
    private float currentAnimHeight = 0;
    private String searchQuery = "";
    private boolean searching = false;
    private int scrollOffset = 0;
    private final List<String> allElements;
    private final int maxDisplayed = 10;
    private final int searchBarHeight = 12;
    private final int elementHeight = 12;

    private final Map<String, Float> colorInterpolationMap = new HashMap<>();
    private final Map<String, Float> rowExpansionMap = new HashMap<>();

    public WhitelistButton(WhitelistSetting setting, Window window) {
        super(setting, window);
        this.setting = setting;
        this.allElements = generateElements();
    }

    private List<String> generateElements() {
        List<String> list = new ArrayList<>();
        switch (setting.getType()) {
            case BLOCKS -> Registries.BLOCK.getIds().forEach(id -> list.add(id.toString()));
            case ITEMS -> Registries.ITEM.forEach(item -> {
                if (!(item instanceof BlockItem)) list.add(Registries.ITEM.getId(item).toString());
            });
            case BOTH -> {
                Registries.BLOCK.getIds().forEach(id -> list.add(id.toString()));
                Registries.ITEM.forEach(item -> {
                    if (!(item instanceof BlockItem)) list.add(Registries.ITEM.getId(item).toString());
                });
            }
            case ENTITIES -> list.addAll(Arrays.asList("Players", "Animals", "Hostiles", "Passives", "Ambient", "Invisibles", "Boats", "ShulkerBullets"));
            case CUSTOM -> list.addAll(Arrays.asList(setting.getCustomElements()));
        }
        list.sort(String::compareTo);
        return list;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        List<String> activeInUi = allElements.stream()
                .filter(id -> id.toLowerCase().contains(searchQuery.toLowerCase()) || rowExpansionMap.getOrDefault(id, 0f) > 0.01f)
                .collect(Collectors.toList());

        for (String id : allElements) {
            float current = rowExpansionMap.getOrDefault(id, 0.0f);
            boolean matches = id.toLowerCase().contains(searchQuery.toLowerCase());
            float speed = delta * 0.4f;
            rowExpansionMap.put(id, matches ? Math.min(1.0f, current + speed) : Math.max(0.0f, current - speed));
        }

        float targetContentHeight = 0;
        int count = 0;
        for (String id : activeInUi) {
            if (count >= maxDisplayed) break;
            targetContentHeight += elementHeight * rowExpansionMap.get(id);
            count++;
        }

        float totalListHeight = searchBarHeight + targetContentHeight;
        float progress = Easing.ease(Easing.toDelta(openTime, 200), Easing.Method.EASE_OUT_CUBIC);
        currentAnimHeight = open ? (totalListHeight * progress) : (totalListHeight * (1.0f - progress));

        drawTextWithShadow(context, setting.getTag(), getX() + 1, getY() + getVerticalPadding(), Color.WHITE);
        String activeText = (setting.getType() == WhitelistSetting.Type.CUSTOM || setting.getType() == WhitelistSetting.Type.ENTITIES)
                ? (setting.getWhitelistIds().isEmpty() ? "None" : String.join(", ", setting.getWhitelistIds()))
                : String.valueOf(setting.getWhitelist().size());

        String rightSideText = Formatting.GRAY + activeText;
        context.enableScissor((int) (getX() + FontUtils.getWidth(setting.getTag()) + 5), (int) getY(), (int) (getX() + getWidth()), (int) (getY() + (int)super.getHeight()));
        drawTextWithShadow(context, rightSideText, getX() + getWidth() - FontUtils.getWidth(rightSideText), getY() + getVerticalPadding(), Color.WHITE);
        context.disableScissor();

        if (currentAnimHeight > 1) {
            int contentY = (int) (getY() + super.getHeight());
            context.enableScissor((int) getX(), contentY, (int) (getX() + getWidth()), contentY + (int)currentAnimHeight);

            Renderer2D.renderOutline(context, getX(), contentY, getX() + getWidth(), contentY + searchBarHeight, ColorUtils.getGlobalColor(100));
            Renderer2D.renderQuad(context, getX(), contentY, getX() + getWidth(), contentY + searchBarHeight, new Color(0, 0, 0, 60));
            String disp = searchQuery.isEmpty() ? (searching ? "_" : "Search...") : searchQuery + (searching ? "_" : "");
            drawTextWithShadow(context, disp, getX() + 2, contentY + 2, searching ? Color.WHITE : Color.GRAY);

            float currentYOffset = contentY + searchBarHeight;
            int drawn = 0;
            for (int i = scrollOffset; i < activeInUi.size() && drawn < maxDisplayed; i++) {
                String id = activeInUi.get(i);
                float expansion = rowExpansionMap.get(id);
                if (expansion <= 0.01f) continue;

                boolean active = setting.getWhitelistIds().contains(id);

                float colorVal = colorInterpolationMap.getOrDefault(id, active ? 1.0f : 0.0f);
                float colorSpeed = delta * 0.35f;
                colorVal = active ? Math.min(1.0f, colorVal + colorSpeed) : Math.max(0.0f, colorVal - colorSpeed);
                colorInterpolationMap.put(id, colorVal);

                Color global = ColorUtils.getGlobalColor();
                Color textColor = interpolate(Color.GRAY, global, colorVal);
                Color signColorRaw = interpolate(Color.WHITE, global, colorVal);

                int alpha = (int)(255 * expansion);
                Color finalTextColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha);
                Color finalSignColor = new Color(signColorRaw.getRed(), signColorRaw.getGreen(), signColorRaw.getBlue(), alpha);

                drawTextWithShadow(context, id.replace("minecraft:", ""), getX() + 4, (int)currentYOffset + 2, finalTextColor);
                drawTextWithShadow(context, active ? "-" : "+", getX() + getWidth() - FontUtils.getWidth(active ? "-" : "+"), (int)currentYOffset + 2, finalSignColor);

                currentYOffset += (elementHeight * expansion);
                drawn++;
            }
            context.disableScissor();
        }
    }

    private Color interpolate(Color start, Color end, float progress) {
        return new Color(
                (int) (start.getRed() + (end.getRed() - start.getRed()) * progress),
                (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * progress),
                (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * progress)
        );
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(mouseX, mouseY)) {
            if (button == 1) {
                open = !open;
                openTime = System.currentTimeMillis();
            }
            searching = false;
            return;
        }
        if (open) {
            int contentY = (int) (getY() + super.getHeight());
            if (mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= contentY && mouseY < contentY + searchBarHeight) {
                searching = !searching;
                if (searching) searchQuery = "";
                return;
            }

            float currentYOffset = contentY + searchBarHeight;
            List<String> visible = allElements.stream().filter(id -> id.toLowerCase().contains(searchQuery.toLowerCase())).collect(Collectors.toList());

            for (int i = scrollOffset; i < Math.min(visible.size(), scrollOffset + maxDisplayed); i++) {
                String id = visible.get(i);
                if (mouseY >= currentYOffset && mouseY < currentYOffset + elementHeight) {
                    if (setting.getWhitelistIds().contains(id)) setting.remove(findObjectById(id));
                    else setting.add(findObjectById(id));
                    return;
                }
                currentYOffset += elementHeight;
            }
            searching = false;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<String> matches = allElements.stream().filter(id -> id.toLowerCase().contains(searchQuery.toLowerCase())).collect(Collectors.toList());
        if (open && matches.size() > maxDisplayed) {
            if (mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= (getY() + super.getHeight())) {
                if (verticalAmount > 0) scrollOffset = Math.max(0, scrollOffset - 1);
                else if (verticalAmount < 0) scrollOffset = Math.min(matches.size() - maxDisplayed, scrollOffset + 1);
                return true;
            }
        }
        return false;
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!searching) return;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) { searching = false; return; }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); scrollOffset = 0; }
    }

    @Override public void charTyped(char chr, int modifiers) {
        if (searching) { searchQuery += chr; scrollOffset = 0; }
    }

    private Object findObjectById(String id) {
        if (setting.getType() == WhitelistSetting.Type.ENTITIES || setting.getType() == WhitelistSetting.Type.CUSTOM) return id;
        Item item = IdentifierUtils.getItem(id);
        return (item instanceof BlockItem bi) ? bi.getBlock() : item;
    }

    @Override public float getHeight() { return super.getHeight() + currentAnimHeight; }
    @Override public boolean isHovering(double mouseX, double mouseY) { return mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= getY() && mouseY <= getY() + super.getHeight(); }
}