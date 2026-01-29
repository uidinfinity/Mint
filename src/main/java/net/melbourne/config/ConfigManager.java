package net.melbourne.config;

import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;
import net.melbourne.Manager;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.modules.Feature;
import net.melbourne.settings.Setting;
import net.melbourne.settings.types.*;
import net.melbourne.utils.miscellaneous.FileUtils;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Getter
@Setter
public class ConfigManager extends Manager {

    private String currentConfig = "default";
    private String lastLoadedKit = "";
    private final String client = Melbourne.NAME + "/client";
    private final String configs = Melbourne.NAME + "/configs";

    public ConfigManager() {
        super("Config", "Saves everything in " + Melbourne.NAME);
    }

    @Override
    public void onInit() {
        try {
            FileUtils.createDirectory(Melbourne.NAME);
            FileUtils.createDirectory(configs);
            FileUtils.createDirectory(client);
        } catch (IOException e) {
            Melbourne.getLogger().error("Failed to create client directories", e);
        }

        loadLastUsedConfig();

        if (!lastLoadedKit.isEmpty()) {
            Managers.KIT.loadKit(lastLoadedKit);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                saveConfig();
            } catch (Exception e) {
                Melbourne.getLogger().error("Failed to save config on shutdown", e);
            }
        }));
    }

    private void loadLastUsedConfig() {
        File dataFile = new File(client + "/config-data.json");
        if (dataFile.exists()) {
            try (InputStream stream = Files.newInputStream(Paths.get(client + "/config-data.json"))) {
                JsonObject obj = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
                if (obj.has("Config")) {
                    currentConfig = obj.get("Config").getAsString();
                }
                if (obj.has("LastKit")) {
                    lastLoadedKit = obj.get("LastKit").getAsString();
                }
            } catch (IOException | JsonParseException e) {
                Melbourne.getLogger().error("Failed to load config-data.json", e);
            }
        }

        loadModules(currentConfig);
    }

    public void loadConfig(String configName) {
        File configFile = new File(configs + "/" + configName + ".json");
        if (!configFile.exists()) return;

        loadModules(configName);
        currentConfig = configName;
        saveCurrentConfigName();
    }

    public void saveConfig() {
        saveModules(currentConfig);
        saveCurrentConfigName();
    }

    private void saveCurrentConfigName() {
        JsonObject configData = new JsonObject();
        configData.addProperty("Config", currentConfig);
        configData.addProperty("LastKit", lastLoadedKit);
        try {
            FileUtils.resetFile(client + "/config-data.json");
        } catch (IOException e) {
            Melbourne.getLogger().error("Failed to reset config-data.json", e);
            return;
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(client + "/config-data.json"), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(configData, writer);
        } catch (IOException e) {
            Melbourne.getLogger().error("Failed to save config-data.json", e);
        }
    }

    public void loadModules(String config) {
        File configFile = new File(configs + "/" + config + ".json");
        if (!configFile.exists()) return;

        try (InputStream stream = Files.newInputStream(configFile.toPath())) {
            JsonObject configObject = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            if (!configObject.has("Modules")) return;

            JsonObject modulesObject = configObject.get("Modules").getAsJsonObject();

            for (Feature feature : Managers.FEATURE.getFeatures()) {
                if (!modulesObject.has(feature.getName())) {
                    feature.setEnabled(false);
                    resetSettingValues(feature);
                    continue;
                }

                JsonObject moduleObject = modulesObject.get(feature.getName()).getAsJsonObject();
                feature.setEnabled(moduleObject.has("Status") && moduleObject.get("Status").getAsBoolean());

                if (!moduleObject.has("Settings")) {
                    resetSettingValues(feature);
                    continue;
                }

                JsonObject settingsObject = moduleObject.get("Settings").getAsJsonObject();
                for (Setting setting : feature.getSettings()) {
                    JsonElement valueObject = settingsObject.get(setting.getName());
                    if (valueObject == null) {
                        resetSingleSetting(setting);
                        continue;
                    }

                    try {
                        if (setting instanceof BooleanSetting b) b.setValue(valueObject.getAsBoolean());
                        else if (setting instanceof NumberSetting n) n.setValue(valueObject.getAsNumber());
                        else if (setting instanceof ModeSetting m) m.setValue(valueObject.getAsString());
                        else if (setting instanceof TextSetting t) t.setValue(valueObject.getAsString());
                        else if (setting instanceof BindSetting bd) bd.setValue(valueObject.getAsInt());
                        else if (setting instanceof WhitelistSetting w) {
                            w.clear();
                            if (valueObject.isJsonArray()) {
                                JsonArray array = valueObject.getAsJsonArray();
                                for (JsonElement element : array) {
                                    Object obj = w.findObjectById(element.getAsString());
                                    if (obj != null) w.add(obj);
                                }
                            }
                        } else if (setting instanceof ColorSetting c) {
                            if (valueObject.isJsonObject()) {
                                JsonObject colorObj = valueObject.getAsJsonObject();
                                int r = Math.clamp(colorObj.get("r").getAsInt(), 0, 255);
                                int g = Math.clamp(colorObj.get("g").getAsInt(), 0, 255);
                                int b = Math.clamp(colorObj.get("b").getAsInt(), 0, 255);
                                int a = Math.clamp(colorObj.get("a").getAsInt(), 0, 255);
                                boolean sync = colorObj.has("sync") && colorObj.get("sync").getAsBoolean();

                                c.setColor(new Color(r, g, b, a));
                                c.setSync(sync);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (IOException | JsonParseException e) {
            Melbourne.getLogger().error("Failed to load config: " + config, e);
        }
    }

    public void saveModules(String config) {
        try {
            FileUtils.resetFile(configs + "/" + config + ".json");
        } catch (IOException e) {
            Melbourne.getLogger().error("Failed to reset config file: " + config, e);
            return;
        }

        JsonObject configObject = new JsonObject();
        configObject.add("Config", new JsonPrimitive(config));

        JsonObject modulesObject = new JsonObject();
        for (Feature feature : Managers.FEATURE.getFeatures()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.add("Status", new JsonPrimitive(feature.isEnabled()));

            JsonObject settingsObject = new JsonObject();
            for (Setting setting : feature.getSettings()) {
                if (setting instanceof BooleanSetting b) settingsObject.add(b.getName(), new JsonPrimitive(b.getValue()));
                else if (setting instanceof NumberSetting n) settingsObject.add(n.getName(), new JsonPrimitive(n.getValue()));
                else if (setting instanceof ModeSetting m) settingsObject.add(m.getName(), new JsonPrimitive(m.getValue()));
                else if (setting instanceof TextSetting t) settingsObject.add(t.getName(), new JsonPrimitive(t.getValue()));
                else if (setting instanceof BindSetting bd) settingsObject.add(bd.getName(), new JsonPrimitive(bd.getValue()));
                else if (setting instanceof WhitelistSetting w) {
                    JsonArray array = new JsonArray();
                    w.getWhitelistIds().forEach(array::add);
                    settingsObject.add(w.getName(), array);
                } else if (setting instanceof ColorSetting c) {
                    JsonObject colorObj = new JsonObject();
                    colorObj.addProperty("r", c.getColor().getRed());
                    colorObj.addProperty("g", c.getColor().getGreen());
                    colorObj.addProperty("b", c.getColor().getBlue());
                    colorObj.addProperty("a", c.getColor().getAlpha());
                    colorObj.addProperty("sync", c.isSync());
                    settingsObject.add(c.getName(), colorObj);
                }
            }

            moduleObject.add("Settings", settingsObject);
            modulesObject.add(feature.getName(), moduleObject);
        }

        configObject.add("Modules", modulesObject);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configs + "/" + config + ".json"), StandardCharsets.UTF_8)) {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(configObject.toString())));
        } catch (IOException e) {
            Melbourne.getLogger().error("Failed to save config: " + config, e);
        }
    }

    private void resetSettingValues(Feature feature) {
        for (Setting setting : feature.getSettings()) resetSingleSetting(setting);
    }

    private void resetSingleSetting(Setting setting) {
        if (setting instanceof BooleanSetting b) b.resetValue();
        else if (setting instanceof NumberSetting n) n.resetValue();
        else if (setting instanceof ModeSetting m) m.resetValue();
        else if (setting instanceof TextSetting t) t.resetValue();
        else if (setting instanceof BindSetting bd) bd.resetValue();
        else if (setting instanceof ColorSetting c) c.resetValue();
        else if (setting instanceof WhitelistSetting w) w.clear();
    }
}