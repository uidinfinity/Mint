package net.melbourne.modules.impl.misc;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.util.Hand;

@FeatureInfo(name = "AutoPlay", category = Category.Misc)
public class AutoPlayFeature extends Feature {

    public final BooleanSetting autoplay = new BooleanSetting("AutoPlay", "Automatically queue next game when match ends", true);
    public final BooleanSetting autowho = new BooleanSetting("AutoWho", "Run /who when someone joins that mentions you", true);
    public final ModeSetting mode = new ModeSetting("Mode", "Which gamemode to queue after a match", "Solo Insane", new String[]{"Solo Insane", "Solo Normal", "BedWars Solo", "BedWars Duo", "BedWars Trio", "BedWars 4s", "Classic Duel", "Replay"});
    public final NumberSetting delayAP = new NumberSetting("DelayAP", "Delay (ms) before sending autoplay command", 1500, 0, 10000);

    private String queuedPlayCommand = "";
    private long queuedPlayTimestamp = 0L;
    private long queuedPlaySendAfter = 0L;

    private static final String WIN = "You won! Want to play again? Click here!";
    private static final String LOSE = "You died! Want to play again? Click here!";
    private static final String BW = "1st Killer";
    private static final String DUEL = "Accuracy";
    private static final String JOIN = "has joined";

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        if (mode.getValue().equals("Replay")) {
            int oldSlot = mc.player.getInventory().getSelectedSlot();
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack != null && stack.getItem() == Items.PAPER) {
                    mc.player.getInventory().setSelectedSlot(i);
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    mc.player.getInventory().setSelectedSlot(oldSlot);
                    break;
                }
            }
        }

        if (!queuedPlayCommand.isEmpty() && queuedPlayTimestamp > 0L) {
            long now = System.currentTimeMillis();
            if (now - queuedPlayTimestamp >= queuedPlaySendAfter) {
                mc.player.networkHandler.sendChatMessage(queuedPlayCommand);
                queuedPlayCommand = "";
                queuedPlayTimestamp = 0L;
                queuedPlaySendAfter = 0L;
            }
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (getNull()) return;
        if (event.isCancelled()) return;

        if (event.getPacket() instanceof ChatMessageS2CPacket chat) {
            String msg;
            try {
                msg = chat.body().content().toString();
            } catch (Exception e) {
                return;
            }

            String username = String.valueOf(mc.player.getName());

            if (autoplay.getValue() && !mode.getValue().equals("Replay")) {
                if (msg.contains(WIN) || msg.contains(LOSE) || msg.contains(BW) || msg.contains(DUEL)) {
                    String command = "/play ";
                    switch (mode.getValue()) {
                        case "Solo Insane" -> command += "solo_insane";
                        case "Solo Normal" -> command += "solo_normal";
                        case "BedWars Solo" -> command += "bedwars_eight_one";
                        case "BedWars Duo" -> command += "bedwars_eight_two";
                        case "BedWars Trio" -> command += "bedwars_four_three";
                        case "BedWars 4s" -> command += "bedwars_four_four";
                        case "Classic Duel" -> command += "duels_classic_duel";
                        default -> command += "solo_insane";
                    }

                    queuedPlayCommand = command;
                    queuedPlayTimestamp = System.currentTimeMillis();
                    queuedPlaySendAfter = (long) delayAP.getValue().doubleValue();
                }
            }

            if (autowho.getValue()) {
                if (msg.contains(username + " " + JOIN)) {
                    mc.player.networkHandler.sendChatMessage("who");
                }
            }
        }
    }

    @Override
    public String getInfo() {
        return mode.getValue();
    }
}
