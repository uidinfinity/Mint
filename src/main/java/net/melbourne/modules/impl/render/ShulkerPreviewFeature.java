package net.melbourne.modules.impl.render;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.melbourne.utils.graphics.impl.font.FontUtils;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.ColorHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@FeatureInfo(name = "ShulkerPreview", category = Category.Render)
public class ShulkerPreviewFeature extends Feature {

    public boolean shouldRender(Slot slot) {
        if (slot == null || !slot.hasStack()) return false;
        return isShulker(slot.getStack());
    }

    public void renderFromMixin(DrawContext ctx, Slot slot) {
        ItemStack stack = slot.getStack();
        List<ItemStack> items = readShulker(stack);
        if (items.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;

        int mx = (int) (mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth());
        int my = (int) (mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight());

        int cols = 9;
        int size = 18;
        int pad = 2;

        int titleHeight = tr.fontHeight + 2;

        int gridW = cols * size + pad * 2;
        int textW = tr.getWidth(stack.getName()) + pad * 2;

        int w = Math.max(gridW, textW);
        int h = 3 * size + pad * 2 + titleHeight;

        int x = mx + 12;
        int y = my + 12;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        if (x + w > sw) x -= w + 24;
        if (y + h > sh) y -= h + 24;
        if (x < 4) x = 4;
        if (y < 4) y = 4;

        Renderer2D.renderQuad(ctx, x, y, x + w, y + h, new Color(20, 20, 20, 150));
        Renderer2D.renderQuad(ctx, x, y, x + w, y + titleHeight + 2, new Color(20, 20, 20, 200));

        int color = getColor(getBlock(stack));
        Renderer2D.renderOutline(ctx, x, y, x + w, y + titleHeight + 2, new Color(color));
        Renderer2D.renderOutline(ctx, x, y, x + w, y + h, new Color(color));

        FontUtils.drawTextWithShadow(ctx, stack.getName().getString(), x + pad, y + pad, Color.WHITE);

        for (int i = 0; i < 27; i++) {
            ItemStack s = items.get(i);
            if (s == null || s.isEmpty()) continue;

            int sx = x + pad + (i % cols) * size;
            int sy = y + pad + titleHeight + (i / cols) * size;

            ctx.drawItem(s, sx, sy);

            String overlay = formatCount(s.getCount());
            if (!overlay.isEmpty()) {
                int tw = tr.getWidth(overlay);
                int tx = sx + size - 2 - tw;
                int ty = sy + size - tr.fontHeight;
                FontUtils.drawTextWithShadow(ctx, overlay, tx, ty, Color.WHITE);
            }
        }
    }

    private boolean isShulker(ItemStack s) {
        if (!(s.getItem() instanceof BlockItem bi)) return false;
        return bi.getBlock() instanceof ShulkerBoxBlock;
    }

    private List<ItemStack> readShulker(ItemStack stack) {
        ContainerComponent component = stack.getComponents().getOrDefault(DataComponentTypes.CONTAINER, null);
        if (component == null) return Collections.emptyList();

        List<ItemStack> input = component.stream().toList();
        List<ItemStack> out = new ArrayList<>(Collections.nCopies(27, ItemStack.EMPTY));

        int n = Math.min(input.size(), 27);
        for (int i = 0; i < n; i++) out.set(i, input.get(i));

        return out;
    }

    public static int getColor(ShulkerBoxBlock block) {
        if (block != null && block.getColor() != null) {
            return ColorHelper.withAlpha(255, block.getColor().getMapColor().color);
        }
        return 0xff9953b0;
    }

    private static ShulkerBoxBlock getBlock(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem b && b.getBlock() instanceof ShulkerBoxBlock shulker)
            return shulker;
        return null;
    }

    private static String formatCount(int count) {
        if (count <= 1) return "";
        if (count <= 999) return String.valueOf(count);
        if (count <= 999_999) return "%.1fk".formatted(count / 1000f);
        return "%.1fm".formatted(count / 1_000_000f);
    }
}
