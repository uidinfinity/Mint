package net.melbourne.modules.impl.player;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.utils.inventory.kit.Kit;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.melbourne.utils.inventory.kit.Kit.KitItem;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@FeatureInfo(name = "InventoryManager", category = Category.Player)
public class InventoryManagerFeature extends Feature {

    public final NumberSetting delay = new NumberSetting("Delay", "", 100, 0, 1000);
    public final BooleanSetting silent = new BooleanSetting("Silent", "", true);
    public final ModeSetting sorterMode = new ModeSetting("Sorter", "", "Hotbar", new String[]{"None", "Hotbar", "Inventory", "Both"});

    public final BooleanSetting autoXCarry = new BooleanSetting("AutoXCarry", "", false);
    public ModeSetting xCarryMode = new ModeSetting("XCarryModes", "This is the item set we fill for AutoXCarry.", "Crystals", new String[]{"Crystals", "XP", "Both"});

    public static long lastCleanerActionTime = 0;
    private long lastAction;
    private long xCarryTimer = 0;
    private final Map<Integer, Item> lastXCarryState = new HashMap<>();

    @Override
    public void onEnable() {
        lastAction = System.currentTimeMillis();
        lastCleanerActionTime = 0;
        xCarryTimer = 0;
        lastXCarryState.clear();
    }

    @Override
    public void onDisable() {
        lastCleanerActionTime = 0;
        xCarryTimer = 0;
        lastXCarryState.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        Kit currentKit = Managers.KIT.getCurrentKit();

        if (currentKit == null) {
            Services.CHAT.sendRaw("§cNo kit is currently loaded.");
            this.setEnabled(false);
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastAction >= delay.getValue().doubleValue()) {
            lastAction = now;
            boolean inventoryOpen = mc.currentScreen instanceof HandledScreen<?>;
            if (!silent.getValue() && !inventoryOpen) return;

            if (!sorterMode.getValue().equals("None")) {
                if (doSorting(currentKit)) {
                    mc.player.getInventory().updateItems();
                    lastCleanerActionTime = System.currentTimeMillis();
                    return;
                }
            }

            Map<Item, Map<Integer, Integer>> itemStacks = new HashMap<>();

            Map<Item, Integer> kitRequiredStacks = getKitRequiredStacks(currentKit);

            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                Item item = stack.getItem();

                int maxStacks = kitRequiredStacks.getOrDefault(item, 0);

                if (maxStacks <= 0) {
                    if (isXCarryItem(item)) continue;
                }

                itemStacks.putIfAbsent(item, new HashMap<>());
                itemStacks.get(item).put(i, stack.getCount());
            }

            int syncId = mc.player.currentScreenHandler.syncId;

            Optional<Map.Entry<Integer, Integer>> slotToDrop = itemStacks.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > kitRequiredStacks.getOrDefault(entry.getKey(), 0))
                    .flatMap(entry -> {
                        int maxStacks = kitRequiredStacks.getOrDefault(entry.getKey(), 0);
                        List<Map.Entry<Integer, Integer>> slots = entry.getValue().entrySet().stream()
                                .sorted(Comparator
                                        .comparing((Map.Entry<Integer, Integer> s) -> s.getKey() < 9, Comparator.reverseOrder())
                                        .thenComparing(Map.Entry::getValue)
                                )
                                .collect(Collectors.toList());
                        return slots.subList(maxStacks, slots.size()).stream();
                    })
                    .min(Comparator
                            .comparing((Map.Entry<Integer, Integer> s) -> s.getKey() < 9, Comparator.reverseOrder())
                            .thenComparing(Map.Entry::getValue)
                    );

            if (slotToDrop.isPresent()) {
                int clientSlot = slotToDrop.get().getKey();
                int serverSlot = clientSlot < 9 ? clientSlot + 36 : clientSlot;
                mc.interactionManager.clickSlot(
                        syncId,
                        serverSlot,
                        1,
                        SlotActionType.THROW,
                        mc.player
                );
                mc.player.getInventory().updateItems();
                lastCleanerActionTime = System.currentTimeMillis();
                return;
            }
        }

        if (autoXCarry.getValue() && now - xCarryTimer >= 250L) {
            xCarryTimer = now;
            if (needsXCarryUpdate()) moveXCarryItems();
            syncXCarryToInventory();
        }
    }

    private boolean doSorting(Kit kit) {
        if (System.currentTimeMillis() - lastCleanerActionTime < 150) return false;

        String mode = sorterMode.getValue();
        int syncId = mc.player.currentScreenHandler.syncId;

        int startSlot = mode.equals("Inventory") || mode.equals("Both") ? 9 : 0;
        int endSlot = mode.equals("Hotbar") || mode.equals("Both") ? 35 : 8;

        if (mode.equals("Hotbar")) endSlot = 8;
        if (mode.equals("Inventory")) startSlot = 9;

        for (int kitSlot = startSlot; kitSlot <= endSlot; kitSlot++) {
            KitItem requiredItem = kit.inventoryItems.get(kitSlot);
            ItemStack currentStack = mc.player.getInventory().getStack(kitSlot);

            int targetServerSlot = kitSlot < 9 ? kitSlot + 36 : kitSlot;

            boolean currentMatchesRequired = (requiredItem != null)
                    ? currentStack.getItem() == requiredItem.getItem()
                    : currentStack.isEmpty();

            if (currentMatchesRequired) {
                continue;
            }

            Item desiredItem = requiredItem != null ? requiredItem.getItem() : null;

            for (int sourceSlot = 0; sourceSlot < 36; sourceSlot++) {
                if (sourceSlot == kitSlot) continue;
                ItemStack sourceStack = mc.player.getInventory().getStack(sourceSlot);
                int sourceServerSlot = sourceSlot < 9 ? sourceSlot + 36 : sourceSlot;

                if (requiredItem != null) {
                    if (sourceStack.getItem() == desiredItem) {

                        KitItem sourceRequiredItem = kit.inventoryItems.get(sourceSlot);
                        boolean sourceMatchesRequired = (sourceRequiredItem != null)
                                ? sourceStack.getItem() == sourceRequiredItem.getItem()
                                : sourceStack.isEmpty();

                        if (currentStack.isEmpty()) {
                            mc.interactionManager.clickSlot(syncId, sourceServerSlot, 0, SlotActionType.PICKUP, mc.player);
                            mc.interactionManager.clickSlot(syncId, targetServerSlot, 0, SlotActionType.PICKUP, mc.player);
                            return true;
                        }

                        if (!sourceMatchesRequired) {
                            mc.interactionManager.clickSlot(syncId, sourceServerSlot, 0, SlotActionType.PICKUP, mc.player);
                            mc.interactionManager.clickSlot(syncId, targetServerSlot, 0, SlotActionType.PICKUP, mc.player);

                            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                                mc.interactionManager.clickSlot(syncId, sourceServerSlot, 0, SlotActionType.PICKUP, mc.player);
                            }
                            return true;
                        }
                    }
                }
            }

            if (requiredItem == null && !currentStack.isEmpty()) {

                boolean moved = false;
                for (int mergeSlot = 0; mergeSlot < 36; mergeSlot++) {
                    if (mergeSlot == kitSlot) continue;
                    ItemStack mergeStack = mc.player.getInventory().getStack(mergeSlot);

                    if (!mergeStack.isEmpty() && mergeStack.getItem() == currentStack.getItem() && mergeStack.getCount() < mergeStack.getMaxCount()) {
                        int targetServerSlot_Merge = kitSlot < 9 ? kitSlot + 36 : kitSlot;
                        mc.interactionManager.clickSlot(syncId, targetServerSlot_Merge, 0, SlotActionType.QUICK_MOVE, mc.player);
                        moved = true;
                        break;
                    }
                }

                if (moved) return true;

                for (int emptySlot = 0; emptySlot < 36; emptySlot++) {
                    if (mc.player.getInventory().getStack(emptySlot).isEmpty()) {
                        int emptyServerSlot = emptySlot < 9 ? emptySlot + 36 : emptySlot;

                        mc.interactionManager.clickSlot(syncId, targetServerSlot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(syncId, emptyServerSlot, 0, SlotActionType.PICKUP, mc.player);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private Map<Item, Integer> getKitRequiredStacks(Kit kit) {
        Map<Item, Integer> requiredStacks = new HashMap<>();
        if (kit == null) return requiredStacks;

        kit.inventoryItems.values().stream()
                .filter(kitItem -> kitItem.getItem() != null)
                .collect(Collectors.groupingBy(Kit.KitItem::getItem))
                .forEach((item, kitItems) -> requiredStacks.put(item, kitItems.size()));

        return requiredStacks;
    }

    private boolean needsXCarryUpdate() {
        String mode = xCarryMode.getValue();
        int slotsToCheck = 4;

        for (int i = 0; i < slotsToCheck; i++) {
            Slot slot = mc.player.currentScreenHandler.getSlot(1 + i);
            Item expectedItem = null;

            if (mode.equals("Crystals")) expectedItem = Items.END_CRYSTAL;
            else if (mode.equals("XP")) expectedItem = Items.EXPERIENCE_BOTTLE;
            else if (mode.equals("Both")) expectedItem = (i % 2 == 0) ? Items.END_CRYSTAL : Items.EXPERIENCE_BOTTLE;

            Item currentItem = slot.hasStack() ? slot.getStack().getItem() : null;
            if (currentItem != expectedItem) return true;
        }

        return false;
    }

    private void moveXCarryItems() {
        String mode = xCarryMode.getValue();
        int syncId = mc.player.currentScreenHandler.syncId;
        int slotsToCheck = 4;

        for (int i = 0; i < slotsToCheck; i++) {
            Slot slot = mc.player.currentScreenHandler.getSlot(1 + i);
            Item expectedItem = null;

            if (mode.equals("Crystals")) expectedItem = Items.END_CRYSTAL;
            else if (mode.equals("XP")) expectedItem = Items.EXPERIENCE_BOTTLE;
            else if (mode.equals("Both")) expectedItem = (i % 2 == 0) ? Items.END_CRYSTAL : Items.EXPERIENCE_BOTTLE;

            if (slot.hasStack() && slot.getStack().getItem() == expectedItem) continue;

            for (int j = 0; j < 36; j++) {
                ItemStack stack = mc.player.getInventory().getStack(j);
                if (stack.getItem() != expectedItem) continue;

                Slot inventorySlot = mc.player.currentScreenHandler.getSlot(j < 9 ? j + 36 : j);

                mc.interactionManager.clickSlot(syncId, inventorySlot.id, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(syncId, inventorySlot.id, 0, SlotActionType.PICKUP, mc.player);

                break;
            }
        }
    }

    private void syncXCarryToInventory() {
        if (!autoXCarry.getValue()) return;

        String mode = xCarryMode.getValue();
        int syncId = mc.player.currentScreenHandler.syncId;
        int slotsToCheck = 4;

        for (int i = 0; i < slotsToCheck; i++) {
            Slot slot = mc.player.currentScreenHandler.getSlot(1 + i);
            Item expectedItem = null;

            if (mode.equals("Crystals")) expectedItem = Items.END_CRYSTAL;
            else if (mode.equals("XP")) expectedItem = Items.EXPERIENCE_BOTTLE;
            else if (mode.equals("Both")) expectedItem = (i % 2 == 0) ? Items.END_CRYSTAL : Items.EXPERIENCE_BOTTLE;

            if (!slot.hasStack()) continue;

            ItemStack stack = slot.getStack();
            int maxStacks = getMaxItemStacks(stack.getItem());
            int totalInInventory = getTotalItemCount(stack.getItem());

            if (totalInInventory < maxStacks) {

                for (int j = 0; j < 36; j++) {
                    ItemStack invStack = mc.player.getInventory().getStack(j);
                    if (invStack.isEmpty() || (invStack.getItem() == stack.getItem() && invStack.getCount() < invStack.getMaxCount())) {
                        Slot inventorySlot = mc.player.currentScreenHandler.getSlot(j < 9 ? j + 36 : j);

                        mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(syncId, inventorySlot.id, 0, SlotActionType.PICKUP, mc.player);

                        if (mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                            break;
                        }

                        mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                        break;
                    }
                }
            }
        }
    }

    private int getTotalItemCount(Item item) {
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) total += stack.getCount();
        }
        return total;
    }

    private boolean isXCarryItem(Item item) {
        if (!autoXCarry.getValue()) return false;
        String mode = xCarryMode.getValue();
        if (mode.equals("Crystals")) return item == Items.END_CRYSTAL;
        if (mode.equals("XP")) return item == Items.EXPERIENCE_BOTTLE;
        if (mode.equals("Both")) return item == Items.END_CRYSTAL || item == Items.EXPERIENCE_BOTTLE;
        return false;
    }

    private int getMaxItemStacks(Item item) {
        Kit currentKit = Managers.KIT.getCurrentKit();
        if (currentKit != null) {
            Map<Item, Integer> requiredStacks = getKitRequiredStacks(currentKit);
            return requiredStacks.getOrDefault(item, 0);
        }
        return 0;
    }
}