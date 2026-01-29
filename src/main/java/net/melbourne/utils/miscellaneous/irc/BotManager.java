package net.melbourne.utils.miscellaneous.irc;

import net.melbourne.Manager;
import net.melbourne.Managers;
import net.melbourne.utils.Globals;

import java.util.*;
import java.util.concurrent.*;

public final class BotManager extends Manager implements Globals {

    private final Map<UUID, String> playerCapes = new ConcurrentHashMap<>();
    private final Set<UUID> melbourneUsers = new HashSet<>();

    private final Map<UUID, String> mintByUuid = new ConcurrentHashMap<>();

    public BotManager() {
        super("Bot", "protectionn");
    }

    @Override
    public void onInit() {
        Managers.BOT = this;
    }

    public String getMintNameFor(UUID playerUUID) {
        if (playerUUID == null) return null;
        return mintByUuid.get(playerUUID);
    }

    public Set<UUID> getMelbourneUsers() {
        return Collections.unmodifiableSet(melbourneUsers);
    }

    public String getPlayerCape(UUID uuid) {
        return playerCapes.get(uuid);
    }

    public void sendPingString() {
    }

    public void requestPing(String targetName) {
    }

    private void sendIrcMessage(String message) {
    }

    public void sendMessageToIrc(String message) {
        sendIrcMessage(message);
    }

    public boolean isConnected() {
        return false;
    }

}
