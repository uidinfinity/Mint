package net.melbourne.modules.impl.misc;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.miscellaneous.Timer;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;

@FeatureInfo(name = "FastLatency", category = Category.Misc)
public class FastLatencyFeature extends Feature {

    public final NumberSetting delay = new NumberSetting("Delay", "Delay between ping requests (ms)", 1000, 0, 5000);

    private final Timer timer = new Timer();
    private long pingStart = 0L;
    public int resolvedPing = 0;

    @Override
    public void onEnable() {
        if (getNull()) {
            return;
        }
        timer.reset();
        resolvedPing = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        if (timer.hasTimeElapsed(delay.getValue())) {
            mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(27845, "w "));
            pingStart = System.currentTimeMillis();
            timer.reset();
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof CommandSuggestionsS2CPacket packet) {
            if (packet.id() == 27845) {
                long time = System.currentTimeMillis() - pingStart;
                resolvedPing = (int) Math.max(0, Math.min(time, 1000L)) / 2;
                timer.reset();
            }
        }
    }

    @Override
    public String getInfo() {
        return resolvedPing + "ms";
    }
}