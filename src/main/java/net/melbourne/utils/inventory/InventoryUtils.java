package net.melbourne.utils.inventory;

import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;

import static net.melbourne.utils.Globals.mc;


public class InventoryUtils {

    public static int indexToSlot(int index) {
        if (index >= 0 && index <= 8) return 36 + index;
        return index;
    }

    public static int find(Item item, int start, int end) {
        for (int i = end; i >= start; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) return i;
        }
        return -1;
    }

    public static int find(Class<? extends Item> clazz, int start, int end) {
        for (int i = end; i >= start; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (clazz.isInstance(stack.getItem())) return i;
        }
        return -1;
    }

    public static EquipmentSlot getSlotType(ItemStack stack) {
        if (stack.contains(DataComponentTypes.GLIDER)) return EquipmentSlot.CHEST;
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable != null) return equippable.slot();
        return null;
    }

    public static boolean inInventoryScreen() {
        return mc.currentScreen instanceof InventoryScreen 
            || mc.currentScreen instanceof CreativeInventoryScreen 
            || mc.currentScreen instanceof GenericContainerScreen 
            || mc.currentScreen instanceof ShulkerBoxScreen;
    }

    public static void clickSlot(int slot, int button, SlotActionType action) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, button, action, mc.player);
    }
}