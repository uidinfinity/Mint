package net.melbourne;

import net.melbourne.commands.CommandManager;
import net.melbourne.config.ConfigManager;
import net.melbourne.macros.MacroManager;
import net.melbourne.modules.FeatureManager;
import net.melbourne.script.impl.ScriptManager;
import net.melbourne.utils.entity.player.socials.FriendManager;
import net.melbourne.utils.inventory.kit.KitManager;
import net.melbourne.utils.miscellaneous.irc.BotManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allows us to register, and initialize every manager in the client.
 */
public class Managers {
    public static final FeatureManager FEATURE = new FeatureManager();
    public static final CommandManager COMMAND = new CommandManager();
    public static final ConfigManager CONFIG = new ConfigManager();
    public static final ScriptManager SCRIPT = new ScriptManager();
    public static final FriendManager FRIEND = new FriendManager();
    public static final MacroManager MACRO = new MacroManager();
    public static final KitManager KIT = new KitManager();
    public static BotManager BOT = new BotManager();

    public static final List<Manager> managers = new ArrayList<>();

    public void onInit() {
        register(FEATURE, COMMAND, CONFIG, SCRIPT, FRIEND, MACRO, KIT, BOT);

        for (Manager manager : managers) {
            if (manager == CONFIG)
                continue;

            manager.onInit();
        }
    }

    public void onPostInit() {
        CONFIG.onInit();
    }



    private void register(Manager... managers) {
        Collections.addAll(Managers.managers, managers);
    }
}
