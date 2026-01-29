package net.melbourne.modules.impl.misc;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

@FeatureInfo(name = "Replenish", category = Category.Misc)
public class ReplenishFeature extends Feature {

    public final NumberSetting threshold = new NumberSetting("Threshold", "Refill if count <= this", 16.0, 1.0, 64.0);
    public final NumberSetting pearlThreshold = new NumberSetting("PearlThreshold", "Special threshold for ender pearls", 4.0, 1.0, 16.0);

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull() || mc.currentScreen != null) return;

        for (int slot = 0; slot < 9; slot++) {
            if (tryRefillSlot(slot)) {
                return;
            }
        }
    }

    private boolean tryRefillSlot(int hotbarSlot) {
        ItemStack stack = mc.player.getInventory().getStack(hotbarSlot);
        if (!shouldRefill(stack)) return false;

        for (int i = 9; i < 36; i++) {
            ItemStack invStack = mc.player.getInventory().getStack(i);
            if (!invStack.isEmpty() && areItemsEqual(stack, invStack)) {
                clickSlot(i, SlotActionType.QUICK_MOVE);
                return true;
            }
        }
        return false;
    }

    private boolean shouldRefill(ItemStack stack) {
        if (stack.isEmpty() || !stack.isStackable() || stack.getCount() >= stack.getMaxCount()) {
            return false;
        }

        if (stack.getItem() == net.minecraft.item.Items.ENDER_PEARL) {
            return stack.getCount() <= pearlThreshold.getValue().intValue();
        }

        return stack.getCount() <= threshold.getValue().intValue();
    }

    private boolean areItemsEqual(ItemStack a, ItemStack b) {
        return ItemStack.areItemsEqual(a, b) && ItemStack.areItemsAndComponentsEqual(a, b);
    }

    private void clickSlot(int slotId, SlotActionType action) {
        if (getNull()) return;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slotId,
                0,
                action,
                mc.player
        );
    }
}