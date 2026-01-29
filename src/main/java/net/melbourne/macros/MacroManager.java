package net.melbourne.macros;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.melbourne.Manager;
import net.melbourne.Managers;
import com.google.gson.*;
import net.melbourne.utils.miscellaneous.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MacroManager extends Manager {

    public final List<Macro> macros = new ArrayList<>();

    public MacroManager() {
        super("Macro", "Handles all macros");
    }

    @Override
    public void onInit() {
        loadMacros();
        // stupid, replace ts
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MacroHandler.tick();
        });
    }

    public void addMacro(Macro macro) {
        macros.add(macro);
    }

    public void removeMacro(String name) {
        macros.removeIf(m -> m.name.equalsIgnoreCase(name));
    }

    public Macro getMacroByBind(int bind) {
        return macros.stream().filter(m -> m.bind == bind).findFirst().orElse(null);
    }

    public Macro getMacroByName(String name) {
        return macros.stream().filter(m -> m.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public void triggerBind(int bind) {
        Macro macro = getMacroByBind(bind);
        if (macro != null) macro.trigger();
    }

    public void saveMacros() {
        try {
            FileUtils.createDirectory(Managers.CONFIG.getClient());
            File file = new File(Managers.CONFIG.getClient() + "/macros.json");
            if (!file.exists()) file.createNewFile();
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            for (Macro m : macros) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", m.name);
                obj.addProperty("bind", m.bind);
                obj.addProperty("type", m.type);
                obj.addProperty("command", m.command);
                array.add(obj);
            }
            root.add("Macros", array);
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadMacros() {
        try {
            FileUtils.createDirectory(Managers.CONFIG.getClient());
            File file = new File(Managers.CONFIG.getClient() + "/macros.json");
            if (!file.exists()) {
                try (Writer writer = new FileWriter(file)) {
                    JsonObject root = new JsonObject();
                    root.add("Macros", new JsonArray());
                    new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
                }
            }

            JsonObject obj;
            try (Reader reader = new FileReader(file)) {
                obj = JsonParser.parseReader(reader).getAsJsonObject();
            }

            JsonArray array = obj.getAsJsonArray("Macros");
            macros.clear();

            for (JsonElement e : array) {
                JsonObject macro = e.getAsJsonObject();
                String name = macro.get("name").getAsString();
                int bind = macro.get("bind").getAsInt();
                String type = macro.get("type").getAsString();
                String command = macro.get("command").getAsString();
                macros.add(new Macro(name, bind, type, command));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}