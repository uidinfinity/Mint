package net.melbourne.utils.inventory.kit;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Map;

public class Kit {

    public final String name;
    public final Map<Integer, KitItem> inventoryItems;

    public Kit(String name, Map<Integer, KitItem> inventoryItems) {
        this.name = name;
        this.inventoryItems = inventoryItems;
    }

    public static class KitItem {
        public final String itemId;
        public final int count;

        public KitItem(String itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }

        public Item getItem() {
            return Registries.ITEM.get(Identifier.tryParse(itemId));
        }
    }
}