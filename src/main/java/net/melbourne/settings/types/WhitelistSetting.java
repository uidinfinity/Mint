package net.melbourne.settings.types;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.melbourne.utils.inventory.IdentifierUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.function.Supplier;

@Getter @Setter
public class WhitelistSetting extends Setting {
    private final Type type;
    private final Set<Object> whitelist = new HashSet<>();
    private final String tag;
    private String[] customElements = new String[0];

    public WhitelistSetting(String name, String description, Type type) {
        super(name, description, true);
        this.tag = name;
        this.type = type;
    }

    public WhitelistSetting(String name, String description, Type type, Supplier<Boolean> visibility) {
        super(name, description, visibility);
        this.tag = name;
        this.type = type;
    }

    public WhitelistSetting(String name, String description, Type type, String[] customElements) {
        super(name, description, true);
        this.tag = name;
        this.type = type;
        this.customElements = customElements;
    }

    public WhitelistSetting(String name, String description, Type type, String[] defaults, String[] customElements) {
        super(name, description, true);
        this.tag = name;
        this.type = type;
        this.customElements = customElements;
        for (String s : defaults) {
            Object obj = findObjectById(s);
            if (obj != null) whitelist.add(obj);
        }
    }

    public Object findObjectById(String id) {
        if (type == Type.ENTITIES || type == Type.CUSTOM) return id;
        try {
            Item item = IdentifierUtils.getItem(id);
            if (item instanceof net.minecraft.item.BlockItem bi) return bi.getBlock();
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public void add(Object object) {
        whitelist.add(object);
    }

    public void remove(Object object) {
        whitelist.remove(object);
    }

    public boolean isWhitelistContains(Object object) {
        return whitelist.contains(object);
    }

    public List<String> getWhitelistIds() {
        return whitelist.stream().map(object -> {
            if (object instanceof Item item) return Registries.ITEM.getId(item).toString();
            if (object instanceof Block block) return Registries.BLOCK.getId(block).toString();
            if (object instanceof String str) return str;
            return null;
        }).filter(Objects::nonNull).toList();
    }

    public void clear() {
        whitelist.clear();
    }

    public enum Type {
        ITEMS, BLOCKS, BOTH, ENTITIES, CUSTOM
    }
}