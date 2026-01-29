package net.melbourne.macros;

import net.melbourne.commands.Command;
import net.melbourne.Managers;

public class Macro {
    public String name;
    public int bind;
    public String type;
    public String command;
    private boolean toggled;

    public Macro(String name, int bind, String type, String command) {
        this.name = name;
        this.bind = bind;
        this.type = type.toLowerCase();
        this.command = command;
        this.toggled = false;
    }

    public void trigger() {
        if (type.equals("simple")) executeCommand();
        else if (type.equals("flow")) {
            toggled = !toggled;
            executeCommand();
        }
    }

    private void executeCommand() {
        if (!command.startsWith(".")) return;

        String[] split = command.substring(1).split(" ");
        String cmdName = split[0];
        String[] args = split.length > 1 ? java.util.Arrays.copyOfRange(split, 1, split.length) : new String[0];

        if (type.equals("flow")) {
            if (args.length >= 4) {
                String opt1 = args[2];
                String opt2 = args[3];
                args = new String[]{args[0], args[1], toggled ? opt2 : opt1};
            }
        }

        Command cmd = Managers.COMMAND.findCommand(cmdName);
        if (cmd != null) cmd.onCommand(args);
    }
}
