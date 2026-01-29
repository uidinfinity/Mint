package net.melbourne.modules.impl.combat;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.AttackEntityEvent;
import net.melbourne.mixins.accessors.ClientPlayerEntityAccessor;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.miscellaneous.Timer;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

@FeatureInfo(name = "Criticals", category = Category.Combat)
public class CriticalsFeature extends Feature {
    public ModeSetting mode = new ModeSetting("Mode", "The method used for bypassing the anticheat.", "Packet", new String[]{"Packet", "Strict", "Grim", "GrimV2", "GrimCC", "LowHop", "Jump"});
    public BooleanSetting sprinting = new BooleanSetting("Sprinting", "To perform a critical hit, you must be sprinting.", false);
    public BooleanSetting ignoreCrystals = new BooleanSetting("IgnoreCrystals", "Ignore attacks on crystals.", true);
    public NumberSetting delay = new NumberSetting("Delay", "Minimum delay between critical packets.", 0, 0, 1000);
    private final Timer timer = new Timer();

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (getNull())
            return;

        if (ignoreCrystals.getValue() && event.getTarget() instanceof EndCrystalEntity)
            return;

        if (sprinting.getValue() && !mc.player.isSprinting())
            return;

        if (!timer.hasTimeElapsed(delay.getValue().floatValue()))
            return;

        if (mc.player.isOnGround() || mc.player.getAbilities().flying || mode.getValue().equalsIgnoreCase("Grim") && !mc.player.isInLava() && !mc.player.isSubmergedInWater()) {

            switch (mode.getValue()) {
                case "Packet" -> {
                    sendOffset(0.05);
                    sendOffset(0);
                    sendOffset(0.03);
                    sendOffset(0);
                }
                case "Strict" -> {
                    sendOffset(0.11);
                    sendOffset(0.1100013579);
                    sendOffset(0.0000013579);
                }
                case "Jump" -> {
                    if (mc.player.isOnGround())
                        mc.player.jump();
                }
                case "LowHop" -> {
                    mc.player.setVelocity(new Vec3d(0, 0.3425, 0));
                    mc.player.fallDistance = 0.1f;
                    mc.player.setOnGround(false);
                }
                case "Grim" -> {
                    sendFull(0);
                    sendFull(0.0625D);
                    sendFull(0.045D);
                }
                case "GrimV2" -> {
                    sendFull(0.0001);
                }
                case "GrimCC" -> {
                    sendFull(0);
                    sendFull(0.0625);
                    sendFull(0.0625013579);
                    sendFull(1.3579e-6);
                }
            }

            ((ClientPlayerEntityAccessor) mc.player).setLastOnGround(false);
            timer.reset();
        }
    }

    private void sendOffset(double yOffset) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + yOffset, mc.player.getZ(), false, mc.player.horizontalCollision));
    }

    private void sendFull(double yOffset) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                mc.player.getX(), mc.player.getY() + yOffset, mc.player.getZ(),
                mc.player.getYaw(), mc.player.getPitch(), false, mc.player.horizontalCollision));
    }

    @Override
    public String getInfo() {
        return mode.getValue();
    }
}