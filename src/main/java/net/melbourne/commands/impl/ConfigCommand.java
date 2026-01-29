package net.melbourne.commands.impl;

import net.melbourne.commands.Command;
import net.melbourne.commands.CommandInfo;
import net.melbourne.Managers;
import net.melbourne.services.Services;

import java.io.File;

@CommandInfo(name = "Config", desc = "Save, load, list, or delete a client configuration.")
public class ConfigCommand extends Command {

    @Override
    public void onCommand(String[] args) {
        if (args.length < 1) {
            Services.CHAT.sendRaw("§sUsage: §7.config <save/load/list/delete> <name>");
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "save" -> {
                if (args.length < 2) {
                    Services.CHAT.sendRaw("§sUsage: §7.config <save/load> <name>");
                    return;
                }
                saveConfig(args[1]);
            }
            case "load" -> {
                if (args.length < 2) {
                    Services.CHAT.sendRaw("§sUsage: §7.config <save/load> <name>");
                    return;
                }
                loadConfig(args[1]);
            }
            case "delete" -> {
                if (args.length < 2) {
                    Services.CHAT.sendRaw("§sUsage: §7.config <delete> <name>");
                    return;
                }
                deleteConfig(args[1]);
            }
            case "list" -> listConfigs();
            default -> Services.CHAT.sendRaw("§sUsage: §7.config <save/load/list/delete> <name>");
        }
    }

    private void saveConfig(String name) {
        String previous = Managers.CONFIG.getCurrentConfig();

        try {
            Managers.CONFIG.setCurrentConfig(name);
            Managers.CONFIG.saveConfig();
            Services.CHAT.sendRaw("§s" + name + " §7has been saved as a config.");
        } catch (Exception e) {
            Services.CHAT.sendRaw("§cFailed to save config §s" + name);
        } finally {
            Managers.CONFIG.setCurrentConfig(previous);
        }
    }

    private void loadConfig(String name) {
        File configFile = new File(Managers.CONFIG.getConfigs() + "/" + name + ".json");
        if (!configFile.exists()) {
            Services.CHAT.sendRaw("§s" + name + " §7does not exist as a config.");
            return;
        }

        try {
            Managers.CONFIG.loadConfig(name);
            Services.CHAT.sendRaw("§s" + name + " §7has been loaded as a config.");
        } catch (Exception e) {
            Services.CHAT.sendRaw("§cFailed to load config §s" + name);
        }
    }

    private void deleteConfig(String name) {
        if (Managers.CONFIG.getCurrentConfig().equalsIgnoreCase(name)) {
            Services.CHAT.sendRaw("§cCannot delete the currently loaded config.");
            return;
        }

        File configFile = new File(Managers.CONFIG.getConfigs() + "/" + name + ".json");
        if (!configFile.exists()) {
            Services.CHAT.sendRaw("§s" + name + " §7does not exist as a config.");
            return;
        }

        if (configFile.delete()) {
            Services.CHAT.sendRaw("§s" + name + " §7has been deleted.");
        } else {
            Services.CHAT.sendRaw("§cFailed to delete config §s" + name);
        }
    }

    private void listConfigs() {
        File folder = new File(Managers.CONFIG.getConfigs());
        File[] files = folder.listFiles((dir, filename) -> filename.endsWith(".json"));

        if (files == null || files.length == 0) {
            Services.CHAT.sendRaw("§7No configs found.");
            return;
        }

        StringBuilder sb = new StringBuilder("§sConfigs: §7");
        for (File file : files) {
            String name = file.getName().replace(".json", "");
            if (name.equalsIgnoreCase(Managers.CONFIG.getCurrentConfig())) {
                sb.append("§a").append(name).append("§7, ");
            } else {
                sb.append(name).append(", ");
            }
        }

        if (sb.length() >= 2) sb.setLength(sb.length() - 2);
        Services.CHAT.sendRaw(sb.toString());
    }
}
