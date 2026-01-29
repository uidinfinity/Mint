package net.melbourne.commands.impl;

import net.melbourne.Manager;
import net.melbourne.Managers;
import net.melbourne.commands.Command;
import net.melbourne.commands.CommandInfo;
import net.melbourne.utils.miscellaneous.irc.BotManager;
import net.melbourne.services.Services;

@CommandInfo(name = "Ping", desc = "Ping locations in the IRC.")
public class PingCommand extends Command {

    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            Services.CHAT.sendRaw("§sUsage: §7.ping <send/request> [target]");
            return;
        }

        String sub = args[0].toLowerCase();
        BotManager bot = Managers.BOT;

        boolean allowRequest = true;
        switch (sub) {
            case "send":
                if (bot != null && bot.isConnected()) {
                    bot.sendPingString();
                }
                break;

            case "request":
                if (!allowRequest) {
                    Services.CHAT.sendRaw("§cPing requests are currently disabled.");
                    return;
                }
                if (args.length < 2) {
                    Services.CHAT.sendRaw("§sUsage: §7.ping request <target>");
                    return;
                }
                String target = args[1];
                if (bot != null && bot.isConnected()) {
                    bot.requestPing(target);
                }
                break;

            default:
                Services.CHAT.sendRaw("§sUsage: §7.ping <send/request> [target]");
                break;
        }
    }
}
