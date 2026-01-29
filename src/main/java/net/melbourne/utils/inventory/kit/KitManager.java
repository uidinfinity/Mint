package net.melbourne.utils.inventory.kit;

import com.google.gson.*;
import net.melbourne.Manager;
import net.melbourne.Managers;
import net.melbourne.utils.inventory.kit.Kit.KitItem;
import net.melbourne.utils.miscellaneous.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitManager extends Manager {

    public final List<Kit> kits = new ArrayList<>();
    private Kit currentKit = null;

    public KitManager() {
        super("Kit", "Handles kit saving and loading");
    }

    @Override
    public void onInit() {
        loadKits();
    }

    public void addKit(Kit kit) {
        kits.add(kit);
    }

    public void removeKit(String name) {
        kits.removeIf(k -> k.name.equalsIgnoreCase(name));
    }

    public Kit getKitByName(String name) {
        return kits.stream().filter(k -> k.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public void setCurrentKit(Kit kit) {
        this.currentKit = kit;
        if (kit != null) {
            Managers.CONFIG.setLastLoadedKit(kit.name);
        } else {
            Managers.CONFIG.setLastLoadedKit("");
        }
    }

    public boolean loadKit(String name) {
        Kit kit = getKitByName(name);
        if (kit != null) {
            setCurrentKit(kit);
            return true;
        }
        return false;
    }

    public Kit getCurrentKit() {
        return currentKit;
    }

    public void saveKits() {
        try {
            FileUtils.createDirectory(Managers.CONFIG.getClient());
            File file = new File(Managers.CONFIG.getClient() + "/kits.json");
            if (!file.exists()) file.createNewFile();
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            for (Kit k : kits) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", k.name);
                JsonObject inventory = new JsonObject();
                for (Map.Entry<Integer, KitItem> entry : k.inventoryItems.entrySet()) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("id", entry.getValue().itemId);
                    itemObj.addProperty("count", entry.getValue().count);
                    inventory.add(String.valueOf(entry.getKey()), itemObj);
                }
                obj.add("inventory", inventory);
                array.add(obj);
            }
            root.add("Kits", array);
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadKits() {
        try {
            FileUtils.createDirectory(Managers.CONFIG.getClient());
            File file = new File(Managers.CONFIG.getClient() + "/kits.json");
            if (!file.exists()) {
                try (Writer writer = new FileWriter(file)) {
                    JsonObject root = new JsonObject();
                    root.add("Kits", new JsonArray());
                    new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
                }
            }

            JsonObject obj;
            try (Reader reader = new FileReader(file)) {
                obj = JsonParser.parseReader(reader).getAsJsonObject();
            }

            JsonArray array = obj.getAsJsonArray("Kits");
            kits.clear();

            for (JsonElement e : array) {
                JsonObject kitObj = e.getAsJsonObject();
                String name = kitObj.get("name").getAsString();
                JsonObject inventory = kitObj.getAsJsonObject("inventory");
                Map<Integer, KitItem> inventoryItems = new HashMap<>();

                for (Map.Entry<String, JsonElement> entry : inventory.entrySet()) {
                    int slot = Integer.parseInt(entry.getKey());
                    JsonObject itemObj = entry.getValue().getAsJsonObject();
                    String itemId = itemObj.get("id").getAsString();
                    int count = itemObj.get("count").getAsInt();
                    inventoryItems.put(slot, new KitItem(itemId, count));
                }
                kits.add(new Kit(name, inventoryItems));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}