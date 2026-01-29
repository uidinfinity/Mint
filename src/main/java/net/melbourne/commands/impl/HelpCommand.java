package net.melbourne.commands.impl;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.commands.Command;
import net.melbourne.commands.CommandInfo;

@CommandInfo(name = "Help", desc = "Lists all available commands and their descriptions.")
public class HelpCommand extends Command {

    @Override
    public void onCommand(String[] args) {
        Services.CHAT.sendRaw("§sAvailable commands:");

        for (Command cmd : Managers.COMMAND.commands) {
            Services.CHAT.sendRaw("§s" + cmd.getName() + ": §7" + cmd.getDescription());
        }
    }
}
