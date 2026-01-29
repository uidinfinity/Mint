package net.melbourne.utils.miscellaneous;

import net.melbourne.mixins.accessors.ClientWorldAccessor;
import net.melbourne.utils.Globals;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.*;

public class NetworkUtils implements Globals {

    public static void sendIgnoredPacket(Packet<?> packet) {
        mc.getNetworkHandler().getConnection().send(packet, null, true);
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        try (PendingUpdateManager pendingUpdateManager = ((ClientWorldAccessor) mc.world).invokeGetPendingUpdateManager().incrementSequence();) {
            Packet<ServerPlayPacketListener> packet = packetCreator.predict(pendingUpdateManager.getSequence());
            mc.getNetworkHandler().sendPacket(packet);
        }
    }

    public static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(packet);
        }
    }
}
