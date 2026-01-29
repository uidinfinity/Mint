package net.melbourne.modules.impl.player;

import net.melbourne.services.Services;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.melbourne.utils.inventory.SwitchType;
import net.melbourne.utils.miscellaneous.NetworkUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

@FeatureInfo(name = "ThrowPearl", category = Category.Player)
public class ThrowPearlFeature extends Feature {

    public final ModeSetting autoSwap = new ModeSetting("Swap", "Swap mode.", "Swap", new String[]{"None", "Normal", "Silent", "Swap", "Pickup"});

    @Override
    public void onEnable() {
        if (getNull()) {
            setToggled(false);
            return;
        }

        if (mc.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))) {
            setToggled(false);
            return;
        }

        Slot slot = Services.INVENTORY.findSlot(SearchLogic.All, Items.ENDER_PEARL);

        if (slot == null) {
            setToggled(false);
            return;
        }

        if (Services.INVENTORY.switchTo(slot, true, getSwitchType())) {
            NetworkUtils.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch()));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            Services.INVENTORY.switchBack();
        }

        setToggled(false);
    }

    private SwitchType getSwitchType() {
        return switch (autoSwap.getValue()) {
            case "None" -> SwitchType.None;
            case "Normal" -> SwitchType.Normal;
            case "Silent" -> SwitchType.Silent;
            case "Swap" -> SwitchType.Swap;
            case "Pickup" -> SwitchType.PickUp;
            default -> SwitchType.Swap;
        };
    }
}