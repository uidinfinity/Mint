package net.melbourne.modules.impl.player;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.PacketSendEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.misc.FastLatencyFeature;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

@FeatureInfo(name = "PingSpoof", category = Category.Player)
public class PingSpoofFeature extends Feature {
    private final ConcurrentLinkedQueue<DelayedPacket> queue = new ConcurrentLinkedQueue<>();
    public NumberSetting delay = new NumberSetting("Delay", "The delay of to send the packets at.", 200, 0, 2000);
    public NumberSetting jitter = new NumberSetting("Jitter", "Adds randomness to the delay in ms.", 25, 0, 200);
    public NumberSetting maxPerTick = new NumberSetting("MaxPerTick", "Maximum packets to send per tick.", 1, 1, 10);
    public BooleanSetting flushOnDisable = new BooleanSetting("FlushOnDisable", "Clears queued packets when disabled.", true);
    Random random = new Random();

    @Override
    public void onDisable() {
        if (flushOnDisable.getValue()) queue.clear();
    }

    @Override
    public void onEnable() {
        if (Managers.FEATURE.getFeatureFromClass(FastLatencyFeature.class).isEnabled()) setToggled(false);
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (getNull() || Managers.FEATURE.getFeatureFromClass(FastLatencyFeature.class).isEnabled()) return;

        if (event.getPacket() instanceof KeepAliveS2CPacket packet) {
            event.setCancelled(true);
            queue.add(new DelayedPacket(packet, System.currentTimeMillis()));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        int sent = 0;
        while (sent < maxPerTick.getValue().intValue()) {
            DelayedPacket packet = queue.peek();
            if (packet == null) break;

            long base = delay.getValue().intValue();
            long extra = jitter.getValue().intValue();
            long actualDelay = base + random.nextInt((int) (extra + 1));

            if (System.currentTimeMillis() - packet.time() >= actualDelay) {
                mc.getNetworkHandler().sendPacket(new KeepAliveC2SPacket(queue.poll().packet().getId()));
                sent++;
            } else break;
        }
    }

    @Override
    public String getInfo() {
        return delay.getValue().intValue() + "ms";
    }

    private record DelayedPacket(KeepAliveS2CPacket packet, long time)
    {

    }
}