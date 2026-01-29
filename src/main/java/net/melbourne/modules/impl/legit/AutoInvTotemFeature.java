package net.melbourne.modules.impl.legit;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@FeatureInfo(name = "AutoInvTotem", category = Category.Legit)
public class AutoInvTotemFeature extends Feature {

    private final NumberSetting delay = new NumberSetting("Delay", "Delay in ticks before moving totem", 3, 1, 20);
    private final BooleanSetting moveFromHotbar = new BooleanSetting("MoveFromHotbar", "Move totems from hotbar", true);
    private final BooleanSetting openInv = new BooleanSetting("OpenInv", "Auto open inventory after pop", false);
    private final NumberSetting invOpenDelay = new NumberSetting("InvOpenDelay", "Ticks before opening inventory", 2, 1, 10);
    private final NumberSetting invCloseDelay = new NumberSetting("InvCloseDelay", "Ticks before closing inventory", 8, 5, 20);

    private boolean needsTotem = false;
    private int delayTicks = 0;
    private boolean hadTotemInOffhand = false;

    private boolean shouldOpenInv = false;
    private int invOpenTicks = 0;
    private int invCloseTicks = 0;
    private boolean invAutoOpened = false;

    @Override
    public void onEnable() {
        if (mc.player != null) {
            hadTotemInOffhand = hasTotemInOffhand();
            needsTotem = false;
            delayTicks = 0;
            resetInvState();
        }
    }

    @Override
    public void onDisable() {
        resetInvState();
    }

    private void resetInvState() {
        shouldOpenInv = false;
        invOpenTicks = 0;
        invCloseTicks = 0;
        invAutoOpened = false;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        handleAutoInventory();

        boolean hasTotem = hasTotemInOffhand();
        if (hadTotemInOffhand && !hasTotem) {
            needsTotem = true;
            if (openInv.getValue() && mc.currentScreen == null) {
                shouldOpenInv = true;
                invOpenTicks = (int) invOpenDelay.getValue();
            }
        }
        hadTotemInOffhand = hasTotem;

        if (hasTotem && needsTotem) {
            needsTotem = false;
            delayTicks = 0;
        }

        if (delayTicks > 0) {
            delayTicks--;
            if (delayTicks == 0) moveTotemToOffhand();
        }

        if (mc.currentScreen instanceof InventoryScreen && needsTotem) {
            delayTicks = (int) delay.getValue();
        }
    }

    private void handleAutoInventory() {
        if (shouldOpenInv && invOpenTicks > 0) {
            invOpenTicks--;
            if (invOpenTicks == 0 && mc.currentScreen == null) {
                mc.setScreen(new InventoryScreen(mc.player));
                invAutoOpened = true;
                invCloseTicks = (int) invCloseDelay.getValue();
                shouldOpenInv = false;
            }
        }

        if (invAutoOpened && invCloseTicks > 0) {
            invCloseTicks--;
            if (invCloseTicks == 0 && mc.currentScreen instanceof InventoryScreen) {
                mc.setScreen(null);
                invAutoOpened = false;
            }
        }

        if (invAutoOpened && !(mc.currentScreen instanceof InventoryScreen)) {
            invAutoOpened = false;
            invCloseTicks = 0;
        }
    }

    private void moveTotemToOffhand() {
        int totemSlot = findTotemSlot();
        if (totemSlot == -1) return;

        try {
            int containerSlot = totemSlot < 9 ? totemSlot + 36 : totemSlot;

            ItemStack offhandStack = mc.player.getOffHandStack();

            if (offhandStack.isEmpty()) {
                mc.interactionManager.clickSlot(0, containerSlot, 40, SlotActionType.SWAP, mc.player);
            } else {
                mc.interactionManager.clickSlot(0, containerSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, containerSlot, 0, SlotActionType.PICKUP, mc.player);
            }

            needsTotem = false;

        } catch (Exception ignored) {}
    }

    private int findTotemSlot() {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) return i;
        }
        if (moveFromHotbar.getValue()) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) return i;
            }
        }
        return -1;
    }

    private boolean hasTotemInOffhand() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getOffHandStack();
        return !stack.isEmpty() && stack.getItem() == Items.TOTEM_OF_UNDYING;
    }
}
