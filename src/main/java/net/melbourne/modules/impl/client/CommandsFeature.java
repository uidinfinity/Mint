package net.melbourne.modules.impl.client;

import net.melbourne.Managers;
import net.melbourne.commands.CommandManager;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.ChatSendEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;

@FeatureInfo(name = "Commands", category = Category.Client)
public class CommandsFeature extends Feature {

    @SubscribeEvent
    public void onChat(ChatSendEvent event) {
        if (mc.player == null) return;

        String msg = event.getMessage();
        if (msg == null) return;

        if (msg.startsWith(CommandManager.PREFIX)) {
            boolean handled = Managers.COMMAND.handleChatMessage(msg);
            if (handled) event.setCancelled(true);
        }
    }
}