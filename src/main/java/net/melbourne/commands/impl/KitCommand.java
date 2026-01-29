package net.melbourne.commands.impl;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.commands.Command;
import net.melbourne.commands.CommandInfo;
import net.melbourne.utils.inventory.kit.Kit;
import net.melbourne.utils.inventory.kit.KitManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.melbourne.utils.inventory.kit.Kit.KitItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandInfo(name = "Kit", desc = "Save, load, and manage inventory kits.")
public class KitCommand extends Command {

    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            Services.CHAT.sendRaw("§sUsage:");
            Services.CHAT.sendRaw("§7.kit save <name> - Save current inventory as a new kit.");
            Services.CHAT.sendRaw("§7.kit load <name> - Load a kit for InventoryManager use.");
            Services.CHAT.sendRaw("§7.kit delete <name> - Delete a saved kit.");
            Services.CHAT.sendRaw("§7.kit list - List all saved kits.");
            Services.CHAT.sendRaw("§7.kit unload - Unload the current kit.");
            return;
        }

        String action = args[0].toLowerCase();
        String kitName = args.length > 1 ? args[1] : null;
        KitManager kitManager = Managers.KIT;

        switch (action) {
            case "save":
                if (kitName == null) {
                    Services.CHAT.sendRaw("§sUsage: §7.kit save <name>");
                    return;
                }

                Kit existingKit = kitManager.getKitByName(kitName);
                if (existingKit != null) {
                    kitManager.removeKit(kitName);
                    Services.CHAT.sendRaw("§7Kit §s" + kitName + " §7overwritten.");
                }

                Map<Integer, KitItem> inventory = new HashMap<>();
                for (int i = 0; i < 36; i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty()) {
                        Identifier itemId = Registries.ITEM.getId(stack.getItem());
                        inventory.put(i, new KitItem(itemId.toString(), stack.getCount()));
                    }
                }

                Kit newKit = new Kit(kitName, inventory);
                kitManager.addKit(newKit);
                kitManager.saveKits();

                if (kitManager.getCurrentKit() == null || kitManager.getCurrentKit().name.equalsIgnoreCase(kitName)) {
                    kitManager.setCurrentKit(newKit);
                }

                Services.CHAT.sendRaw("§7Kit §s" + kitName + " §7saved successfully.");
                break;

            case "load":
                if (kitName == null) {
                    Services.CHAT.sendRaw("§sUsage: §7.kit load <name>");
                    return;
                }
                if (kitManager.loadKit(kitName)) {
                    Services.CHAT.sendRaw("§7Kit §s" + kitName + " §7loaded. InventoryManager is now using its requirements.");
                } else {
                    Services.CHAT.sendRaw("§cKit §s" + kitName + " §cnot found.");
                }
                break;

            case "delete":
            case "del":
                if (kitName == null) {
                    Services.CHAT.sendRaw("§sUsage: §7.kit delete <name>");
                    return;
                }
                Kit kitToDelete = kitManager.getKitByName(kitName);
                if (kitToDelete == null) {
                    Services.CHAT.sendRaw("§cKit §s" + kitName + " §cnot found.");
                    return;
                }

                if (kitManager.getCurrentKit() != null && kitManager.getCurrentKit().name.equalsIgnoreCase(kitName)) {
                    kitManager.setCurrentKit(null);
                }

                kitManager.removeKit(kitName);
                kitManager.saveKits();
                Services.CHAT.sendRaw("§7Kit §s" + kitName + " §7deleted successfully.");
                break;

            case "list":
                if (kitManager.kits.isEmpty()) {
                    Services.CHAT.sendRaw("§7No kits saved.");
                    return;
                }

                List<String> kitNames = new ArrayList<>();
                for (Kit kit : kitManager.kits) {
                    String name = kit.name;
                    if (kit.equals(kitManager.getCurrentKit())) {
                        name = "§a" + name + " §7(Loaded)";
                    } else {
                        name = "§s" + name;
                    }
                    kitNames.add(name);
                }

                Services.CHAT.sendRaw("§sSaved Kits: §7" + String.join(", ", kitNames));
                break;

            case "unload":
                if (kitManager.getCurrentKit() == null) {
                    Services.CHAT.sendRaw("§7No kit is currently loaded.");
                    return;
                }
                kitManager.setCurrentKit(null);
                Services.CHAT.sendRaw("§7Kit unloaded. InventoryManager is now using feature settings.");
                break;

            default:
                Services.CHAT.sendRaw("§sUsage:");
                Services.CHAT.sendRaw("§7.kit save <name> - Save current inventory as a new kit.");
                Services.CHAT.sendRaw("§7.kit load <name> - Load a kit for InventoryManager use.");
                Services.CHAT.sendRaw("§7.kit delete <name> - Delete a saved kit.");
                Services.CHAT.sendRaw("§7.kit list - List all saved kits.");
                Services.CHAT.sendRaw("§7.kit unload - Unload the current kit.");
                break;
        }
    }
}