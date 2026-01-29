package net.melbourne.commands;

import net.melbourne.Manager;
import net.melbourne.Melbourne;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class CommandManager extends Manager {
    public final List<Command> commands = new ArrayList<>();
    public static final String PREFIX = ".";

    public CommandManager() {
        super("Command", "Allows you to register every command in " + Melbourne.NAME);
    }

    @Override
    public void onInit() {
        Set<Class<? extends Command>> set = new Reflections("net.melbourne.commands.impl").getSubTypesOf(Command.class);

        for (Class<? extends Command> clazz : set) {
            try {
                if (clazz.getAnnotation(CommandInfo.class) == null)
                    continue;

                Command features = clazz.getDeclaredConstructor().newInstance();

                commands.add(features);
            } catch (Exception e) {
                Melbourne.getLogger().error("An error has occurred while instantiating {}", clazz.getName(), e);
            }
        }

        commands.sort(Comparator.comparing(Command::getName));
    }

    public boolean handleChatMessage(String message) {
        if (message == null)
            return false;

        if (!message.startsWith(PREFIX))
            return false;

        String raw = message.substring(PREFIX.length()).trim();
        if (raw.isEmpty())
            return true;

        String[] parts = raw.split("\\s+");
        String cmdName = parts[0];
        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        Command cmd = findCommand(cmdName);
        if (cmd == null) {
            Melbourne.getLogger().info("Unknown command: {}", cmdName);
            return true;
        }

        try {
            cmd.onCommand(args);
        } catch (Exception e) {
            Melbourne.getLogger().error("An error occurred while executing command {}", cmdName, e);
        }

        return true;
    }

    public Command findCommand(String name) {
        for (Command c : commands) {
            if (c.getName().equalsIgnoreCase(name))
                return c;
        }
        return null;
    }
}
