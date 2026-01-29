package net.melbourne.modules.impl.misc;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketSendEvent;
import net.melbourne.mixins.accessors.CustomPayloadC2SPacketAccessor;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;

@FeatureInfo(name = "Handshake", category = Category.Misc)
public class HandshakeFeature extends Feature {
    public ModeSetting mode = new ModeSetting("Mode", "The mode your brand would be set to.", "Lunar", new String[]{"Lunar", "Geyser"});

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof CustomPayloadC2SPacket packet) {
            CustomPayload payload = packet.payload();

            if (!payload.getId().id().equals(BrandCustomPayload.ID.id()))
                return;

            ((CustomPayloadC2SPacketAccessor) (Object) packet).setPayload(new BrandCustomPayload(getBrand()));
        }
    }

    public String getBrand() {
        return switch (mode.getValue()) {
            case "Lunar" -> "lunarclient:v2.16.8-2433";
            case "Geyser" -> "geyser";
            default -> "vanilla";
        };
    }
}
