package net.melbourne;

import net.fabricmc.api.ClientModInitializer;
import net.melbourne.events.EventHandler;
import net.melbourne.gui.main.ClickGuiScreen;
import net.melbourne.services.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Melbourne implements ClientModInitializer {
    private static final Logger LOGGER = LogManager.getLogger("Mint");
    public static final EventHandler EVENT_HANDLER = new EventHandler();
    public static Managers managers = new Managers();
    public static Services services = new Services();
    public static ClickGuiScreen CLICK_GUI;

    public static final String NAME = "Mint";
    public static final String MOD_ID = "melbourne";
    public static final String MOD_VERSION = BuildConstants.MOD_VERSION;
    public static final String MINECRAFT_VERSION = BuildConstants.MINECRAFT_VERSION;
    public static final String GIT_HASH = BuildConstants.GIT_HASH;
    public static final String GIT_REVISION = BuildConstants.GIT_REVISION;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing " + NAME + " - " + MOD_VERSION);

        managers.onInit();
        LOGGER.info("Fully initialized all managers");

        services.onInit();
        LOGGER.info("Fully initialized all services");

        CLICK_GUI = new ClickGuiScreen();
    }

    public static void onPostInitializeClient() {
        managers.onPostInit();
        services.onPostInit();
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
