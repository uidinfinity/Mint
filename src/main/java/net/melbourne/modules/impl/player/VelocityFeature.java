package net.melbourne.modules.impl.player;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

@FeatureInfo(name = "Velocity", category = Category.Player)
public class VelocityFeature extends Feature {

    public ModeSetting mode = new ModeSetting("Mode", "Select Velocity mode", "Cancel", new String[]{"Cancel", "JumpReset"});

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.player == null)
            return;

        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() != mc.player.getId())
                return;

            if (mode.getValue().equalsIgnoreCase("Cancel")) {
                event.setCancelled(true);
            } else if (mode.getValue().equalsIgnoreCase("JumpReset")) {
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                }
            }
        }

        if (event.getPacket() instanceof ExplosionS2CPacket) {
            if (mode.getValue().equalsIgnoreCase("Cancel")) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public String getInfo() {
        return mode.getValue();
    }
}
