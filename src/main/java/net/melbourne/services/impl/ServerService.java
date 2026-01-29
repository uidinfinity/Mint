package net.melbourne.services.impl;

import lombok.Getter;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.services.Service;
import net.melbourne.utils.Globals;
import net.melbourne.utils.entity.EntityUtils;
import net.melbourne.utils.miscellaneous.Timer;
import net.melbourne.utils.miscellaneous.math.MathUtils;
import net.melbourne.modules.impl.client.NotificationsFeature;
import net.melbourne.services.Services;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.text.Text;

import java.util.ArrayDeque;

@Getter
public class ServerService extends Service implements Globals {
    private final Timer timer = new Timer();
    private final ArrayDeque<Float> tpsQueue = new ArrayDeque<>(20);
    @Getter
    private float tps = 20.0f;
    @Getter
    private float averageTps = 20.0f;
    private long lastTime = 0L;
    private long lastSpikeTime = 0L;
    private int lastPing = 0;
    private boolean isSpiking = false;

    public ServerService() {
        super("Server", "Does server stuff");
        Melbourne.EVENT_HANDLER.subscribe(this);
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        timer.reset();

        handlePingSpike();

        if (!(event.getPacket() instanceof WorldTimeUpdateS2CPacket))
            return;

        long currentTime = System.currentTimeMillis();

        if (lastTime != 0L) {
            long delta = currentTime - lastTime;
            if (delta <= 0) return;

            float tickTps = 20.0f * (1000.0f / delta);
            tickTps = MathUtils.clamp(tickTps, 0.0f, 20.0f);

            if (tpsQueue.size() >= 20) {
                tpsQueue.poll();
            }
            tpsQueue.add(tickTps);

            tps = tickTps;

            float sum = 0.0f;
            for (float value : tpsQueue) {
                sum += value;
            }
            averageTps = sum / tpsQueue.size();
        }

        lastTime = currentTime;
    }

    private void handlePingSpike() {
        if (mc.player == null) return;

        NotificationsFeature notifications = Managers.FEATURE.getFeatureFromClass(NotificationsFeature.class);
        if (notifications == null || !notifications.pingSpike.getValue()) return;

        int currentPing = EntityUtils.getLatency(mc.player);
        int threshold = notifications.spikeThreshold.getValue().intValue();

        if (currentPing > lastPing + threshold && lastPing != 0) {
            isSpiking = true;

            Text msg = Text.literal("§7Your ping has spiked from §s" + lastPing + "ms§7 to §s" + currentPing + "ms§7.");

            Services.CHAT.sendPersistent("pingspike", msg, true);
        }

        lastPing = currentPing;
    }

    public void reset() {
        tpsQueue.clear();
        tps = 20.0f;
        averageTps = 20.0f;
        lastTime = 0L;
        isSpiking = false;
        lastPing = 0;
    }
}