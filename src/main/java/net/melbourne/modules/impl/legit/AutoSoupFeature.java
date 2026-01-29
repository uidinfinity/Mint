package net.melbourne.modules.impl.legit;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@FeatureInfo(name = "AutoSoup", category = Category.Legit)
public class AutoSoupFeature extends Feature {

    public final NumberSetting minHealth = new NumberSetting("MinHealth", "guess", 12, 1, 20);
    public final NumberSetting cooldown = new NumberSetting("Cooldown", "cooldown", 500, 0, 5000);
    public final NumberSetting swapDelay = new NumberSetting("SwapDelay", "swapdelay", 150, 0, 1000);
    public final BooleanSetting autoCraft = new BooleanSetting("AutoCraft", "crafts the soup", true);

    private long lastUsed = 0L;
    private long swapTime = 0L;
    private int oldSlot = -1;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        long now = System.currentTimeMillis();

        if (oldSlot != -1 && now >= swapTime) {
            mc.player.getInventory().setSelectedSlot(oldSlot);
            oldSlot = -1;
            return;
        }

        if (mc.player.getHealth() > minHealth.getValue().doubleValue()) return;
        if (now - lastUsed < cooldown.getValue().longValue()) return;

        int soupSlot = findSoupInHotbar();
        if (soupSlot == -1) {
            soupSlot = moveSoupFromInventoryToHotbar();
            if (soupSlot == -1 && autoCraft.getValue()) {
                craftSoupIfPossible();
                return;
            }
        }

        if (soupSlot == -1) return;

        oldSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(soupSlot);

        try {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        } catch (Exception ignored) {}

        lastUsed = now;
        swapTime = now + swapDelay.getValue().longValue();
    }

    private int findSoupInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getItem() == Items.MUSHROOM_STEW) return i;
        }
        return -1;
    }

    private int moveSoupFromInventoryToHotbar() {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getItem() == Items.MUSHROOM_STEW) {
                int emptyHotbarSlot = findEmptyHotbarSlot();
                if (emptyHotbarSlot != -1) {
                    mc.interactionManager.clickSlot(0, i < 9 ? i + 36 : i, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(0, emptyHotbarSlot, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(0, i < 9 ? i + 36 : i, 0, SlotActionType.PICKUP, mc.player);
                    return emptyHotbarSlot;
                }
            }
        }
        return -1;
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    private void craftSoupIfPossible() {
        int redMushroomSlot = -1;
        int brownMushroomSlot = -1;
        int bowlSlot = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack == null) continue;
            if (stack.getItem() == Items.RED_MUSHROOM && redMushroomSlot == -1) redMushroomSlot = i;
            else if (stack.getItem() == Items.BROWN_MUSHROOM && brownMushroomSlot == -1) brownMushroomSlot = i;
            else if (stack.getItem() == Items.BOWL && bowlSlot == -1) bowlSlot = i;
        }

        if (redMushroomSlot == -1 || brownMushroomSlot == -1 || bowlSlot == -1) return;

        int emptyHotbarSlot = findEmptyHotbarSlot();
        if (emptyHotbarSlot == -1) return;

        try {
            mc.interactionManager.clickSlot(0, redMushroomSlot < 9 ? redMushroomSlot + 36 : redMushroomSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, emptyHotbarSlot, 0, SlotActionType.PICKUP, mc.player);

            mc.interactionManager.clickSlot(0, brownMushroomSlot < 9 ? brownMushroomSlot + 36 : brownMushroomSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, emptyHotbarSlot, 0, SlotActionType.PICKUP, mc.player);

            mc.interactionManager.clickSlot(0, bowlSlot < 9 ? bowlSlot + 36 : bowlSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, emptyHotbarSlot, 0, SlotActionType.PICKUP, mc.player);
        } catch (Exception ignored) {}
    }

    @Override
    public String getInfo() {
        int totalSoups = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getItem() == Items.MUSHROOM_STEW) {
                totalSoups += stack.getCount();
            }
        }

        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getItem() == Items.MUSHROOM_STEW) {
                totalSoups += stack.getCount();
            }
        }

        return String.valueOf(totalSoups);
    }

}
