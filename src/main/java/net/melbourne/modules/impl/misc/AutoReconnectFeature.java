package net.melbourne.modules.impl.misc;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.text.Text;

@FeatureInfo(name = "AutoReconnect", category = Category.Misc)
public class AutoReconnectFeature extends Feature {

    public final NumberSetting reconnectDelay = new NumberSetting("Delay", "Reconnect delay (seconds)", 5.0f, 1.0f, 60.0f);

    private ServerInfo lastServer;
    private long reconnectAtMs = -1L;
    private long lastOverlayUpdateMs = 0L;
    private boolean attempted = false;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!isEnabled()) return;

        if (getNull()) {
            resetTimer();
            return;
        }

        ServerInfo cur = mc.getCurrentServerEntry();
        if (cur != null) lastServer = cur;

        boolean disconnected = mc.currentScreen instanceof DisconnectedScreen;

        if (!disconnected) {
            resetTimer();
            return;
        }

        if (lastServer == null) {
            resetTimer();
            return;
        }

        if (reconnectAtMs < 0L) {
            reconnectAtMs = System.currentTimeMillis() + (long) (reconnectDelay.getValue().doubleValue() * 1000.0);
            attempted = false;
            lastOverlayUpdateMs = 0L;
        }

        long now = System.currentTimeMillis();
        long leftMs = reconnectAtMs - now;
        int leftSec = (int) Math.max(0L, (leftMs + 999L) / 1000L);

        if (now - lastOverlayUpdateMs >= 100L) {
            if (mc.inGameHud != null) {
                mc.inGameHud.setOverlayMessage(Text.literal("Reconnecting in " + leftSec + "s"), false);
            }
            lastOverlayUpdateMs = now;
        }

        if (!attempted && leftMs <= 0L) {
            attempted = true;
            tryReconnect(mc.currentScreen);
        }
    }

    @Override
    public void onEnable() {
        resetTimer();
    }

    @Override
    public void onDisable() {
        resetTimer();
    }

    private void resetTimer() {
        reconnectAtMs = -1L;
        lastOverlayUpdateMs = 0L;
        attempted = false;
    }

    private void tryReconnect(Screen parent) {
        if (lastServer == null) return;

        String addrStr = lastServer.address;
        ServerAddress addr = ServerAddress.parse(addrStr);

        ConnectScreen.connect(parent, mc, addr, lastServer, false, (CookieStorage) null);

        resetTimer();
    }
}
