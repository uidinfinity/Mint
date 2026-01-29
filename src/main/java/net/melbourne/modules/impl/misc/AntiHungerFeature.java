package net.melbourne.modules.impl.misc;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketSendEvent;
import net.melbourne.events.impl.SendMovementEvent;
import net.melbourne.mixins.accessors.PlayerMoveC2SPacketAccessor;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@FeatureInfo(name = "AntiHunger", category = Category.Misc)
public class AntiHungerFeature extends Feature {
    private final BooleanSetting onground = new BooleanSetting("OnGround", "Spoofs being onground.", true);
    private final BooleanSetting sprinting = new BooleanSetting("Sprinting", "Change packets when sprinting.", true);
    private boolean og, ignore;
    
    @Override
    public void onEnable() {
        og = mc.player.isOnGround();
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (getNull()) return;

        if (ignore && event.getPacket() instanceof PlayerMoveC2SPacket) {
            ignore = false;
            return;
        }

        if (mc.player != null) {
            if (mc.player.hasVehicle() || mc.player.isTouchingWater() || mc.player.isSubmergedInWater())
                return;

            if (event.getPacket() instanceof PlayerMoveC2SPacket packet && onground.getValue() && mc.player.isOnGround()
                    && mc.player.fallDistance <= 0.0 && !mc.interactionManager.isBreakingBlock()) {
                ((PlayerMoveC2SPacketAccessor) packet).setOnGround(false);
            }
        }

        if (event.getPacket() instanceof ClientCommandC2SPacket packet && sprinting.getValue()) {
            if (packet.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING)
                event.setCancelled(true);
        }
    }

    @SubscribeEvent
    public void onSendMovement(SendMovementEvent event) {
        if (getNull()) return;

        if (mc.player.isOnGround() && !og && onground.getValue()) {
            ignore = true;
        }

        og = mc.player.isOnGround();
    }
}
