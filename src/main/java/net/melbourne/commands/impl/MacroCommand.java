package net.melbourne.commands.impl;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.commands.Command;
import net.melbourne.commands.CommandInfo;
import net.melbourne.macros.Macro;
import net.minecraft.text.Text;

@CommandInfo(name = "Macro", desc = "Add or remove custom macros.")
public class MacroCommand extends Command {

    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            Services.CHAT.sendRaw("§sUsage: §7.macro <add/delete> <args>");
            Services.CHAT.sendRaw("§sAdd: §7.macro add <key> <name> <type> <command>");
            Services.CHAT.sendRaw("§sDelete: §7.macro delete <name>");
            Services.CHAT.sendRaw("§sTypes: §7simple or flow");
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "add":
                if (args.length < 5) {
                    Services.CHAT.sendRaw("§sUsage: §7.macro add <key> <name> <type> <command>");
                    return;
                }

                String name = args[2];
                int bind = getKeyFromString(args[1]);
                if (bind == -1) {
                    Services.CHAT.sendRaw("§cInvalid key: §s" + args[1]);
                    return;
                }

                String type = args[3].toLowerCase();
                if (!type.equals("simple") && !type.equals("flow")) {
                    Services.CHAT.sendRaw("§cInvalid macro type: §s" + type + " §sUse §ssimple §sor §sflow");
                    return;
                }

                StringBuilder commandBuilder = new StringBuilder();
                for (int i = 4; i < args.length; i++) commandBuilder.append(args[i]).append(" ");
                String command = commandBuilder.toString().trim();

                Macro macro = new Macro(name, bind, type, command);
                Managers.MACRO.addMacro(macro);
                Managers.MACRO.saveMacros();
                Text textAdd = Text.literal(name + " §7macro has been added");
                Services.CHAT.sendMacroToggle(name, textAdd, true);
                break;

            case "delete":
            case "del":
                if (args.length < 2) {
                    Services.CHAT.sendRaw("§sUsage: §7.macro delete <name>");
                    return;
                }
                Managers.MACRO.removeMacro(args[1]);
                Managers.MACRO.saveMacros();
                Text textDel = Text.literal(args[1] + " §7macro has been removed.");
                Services.CHAT.sendMacroToggle(args[1], textDel, true);
                break;

            default:
                Services.CHAT.sendRaw("§sUsage: §7.macro <add/delete> <args>");
                Services.CHAT.sendRaw("§sAdd: §7.macro add <key> <name> <type> <command>");
                Services.CHAT.sendRaw("§sDelete: §7.macro delete <name>");
                Services.CHAT.sendRaw("§sTypes: §7simple or flow");
                break;
        }
    }

    private int getKeyFromString(String keyName) {
        try {
            java.lang.reflect.Field field = org.lwjgl.glfw.GLFW.class.getField("GLFW_KEY_" + keyName.toUpperCase());
            return field.getInt(null);
        } catch (Exception e) {
            return -1;
        }
    }
}