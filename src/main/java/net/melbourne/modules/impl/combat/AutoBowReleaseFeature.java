package net.melbourne.modules.impl.combat;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@FeatureInfo(name = "AutoBowRelease", category = Category.Combat)
public class AutoBowReleaseFeature extends Feature {

    private final NumberSetting delay = new NumberSetting("Release Delay", "Seconds to hold bow before auto-release (0.1s = instant fastbow)", 0.3, 0.05, 5.0, true);

    private int ticksUsing = 0;
    private boolean wasUsingBow = false;

    @Override
    public void onEnable() {
        ticksUsing = 0;
        wasUsingBow = false;
    }

    @Override
    public void onDisable() {
        ticksUsing = 0;
        wasUsingBow = false;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            reset();
            return;
        }

        boolean isUsingBow = mc.player.isUsingItem()
                && mc.player.getActiveItem().isOf(Items.BOW)
                && mc.player.getActiveHand() == Hand.MAIN_HAND;

        if (isUsingBow && !wasUsingBow) {
            ticksUsing = 0;
            wasUsingBow = true;
            return;
        }

        if (!isUsingBow) {
            reset();
            return;
        }

        if (wasUsingBow && isUsingBow) {
            ticksUsing++;

            int targetTicks = (int) Math.round(delay.getValue().doubleValue() * 20.0);

            if (ticksUsing >= targetTicks) {
                releaseBow();
                reset();
            }
        }
    }

    private void releaseBow() {
        if (mc.player == null || mc.interactionManager == null) return;

        mc.player.stopUsingItem();

        mc.getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                        BlockPos.ORIGIN,
                        Direction.DOWN
                )
        );
    }

    private void reset() {
        ticksUsing = 0;
        wasUsingBow = false;
    }

    @Override
    public String getInfo() {
        return String.format("%.2fs", delay.getValue().floatValue());
    }
}