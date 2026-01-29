package net.melbourne.modules.impl.player;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketSendEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

@FeatureInfo(name = "XCarry", category = Category.Player)
public class XCarryFeature extends Feature {

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (getNull()) return;
        if (event.getPacket() instanceof CloseHandledScreenC2SPacket) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onDisable() {
        if (getNull()) return;
        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
    }
}