package net.melbourne.services.impl;

import lombok.Getter;
import net.melbourne.Melbourne;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.PacketSendEvent;
import net.melbourne.services.Service;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;

@Getter
public class PlayerService extends Service {
    public int serverSlot;

    public PlayerService() {
        super("Player", "Handles player functions within the client");
        Melbourne.EVENT_HANDLER.subscribe(this);
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet) {
            int packetSlot = packet.getSelectedSlot();

            if (!PlayerInventory.isValidHotbarIndex(packetSlot) || serverSlot == packetSlot) {
                event.setCancelled(true);
                return;
            }

//          if (!BotManager.INSTANCE.isAuthed())
//              System.exit(0);

            serverSlot = packetSlot;
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof UpdateSelectedSlotS2CPacket(int slot))
            serverSlot = slot;
    }
}
