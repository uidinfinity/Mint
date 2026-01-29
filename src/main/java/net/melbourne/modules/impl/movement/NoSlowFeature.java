package net.melbourne.modules.impl.movement;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.events.impl.UpdateMovementEvent;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.utils.miscellaneous.NetworkUtils;
import net.melbourne.utils.entity.player.PlayerUtils;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;

@FeatureInfo(name = "NoSlow", category = Category.Movement)
public class NoSlowFeature extends Feature {

    public ModeSetting mode = new ModeSetting("Mode", "The mode used for NoSlow.", "Normal", new String[]{"Normal", "Grim", "GrimV3", "StrictNCP"});
    public BooleanSetting guiMove = new BooleanSetting("GuiMove", "Allows you to move in guis.", true);
    private int ticks = 0;

    @Override
    public String getInfo() {
        return mode.getValue();
    }

    @SubscribeEvent
    public void onUpdateMovement(UpdateMovementEvent event) {
        if (getNull()) {
            return;
        }

        if (!mc.player.isUsingItem() || mc.player.isRiding())
            return;

        switch (mode.getValue()) {
            case "StrictNCP" -> {
                if (canUse()) {
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(
                            mc.player.getInventory().getSelectedSlot()));
                }
            }
            case "Grim" -> {
                Hand hand = mc.player.getActiveHand();
                if (mc.player.getItemUseTime() <= 3 || mc.player.age % 2 == 0) {
                    if (hand == Hand.MAIN_HAND) {
                        NetworkUtils.sendSequencedPacket(seq -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND,
                                seq, mc.player.getYaw(), mc.player.getPitch()));
                    } else if (hand == Hand.OFF_HAND) {
                        NetworkUtils.sendSequencedPacket(seq -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND,
                                seq, mc.player.getYaw(), mc.player.getPitch()));
                    }
                }
            }
        }
    }



    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!getNull() &&
                mc.currentScreen != null &&
                !(mc.currentScreen instanceof ChatScreen) &&
                guiMove.getValue()) {
            for (KeyBinding k : new KeyBinding[]{
                    mc.options.forwardKey,
                    mc.options.backKey,
                    mc.options.leftKey,
                    mc.options.rightKey,
                    mc.options.jumpKey
            }) {
                k.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                        InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey())
                                .getCode()));
            }

        }
    }


    public boolean canUse() {
        if (mode.getValue().equalsIgnoreCase("GrimV3") && mc.player.isUsingItem()) {
            return !mc.player.isSneaking() && !mc.player.isCrawling() && !mc.player.isRiding() &&
                    mc.player.getItemUseTimeLeft() < 5 || ((mc.player.getItemUseTime() > 1) && mc.player.getItemUseTime() % 2 != 0);
        }

        return PlayerUtils.isEating();
    }

    public boolean getTickState() {
        ticks++;
        return ticks % 2 == 0;
    }

}