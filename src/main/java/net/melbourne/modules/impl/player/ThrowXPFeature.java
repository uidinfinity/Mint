package net.melbourne.modules.impl.player;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.combat.AutoCrystalFeature;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.services.Services;
import net.melbourne.utils.inventory.SwitchType;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.melbourne.utils.miscellaneous.Timer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

@FeatureInfo(name = "ThrowXP", category = Category.Player)
public class ThrowXPFeature extends Feature {

    public final NumberSetting delay = new NumberSetting("Delay", "Delay between throwing actions (ms)", 100, 0, 1000);
    public final NumberSetting xpPer = new NumberSetting("Packets", "Number of XP bottles to throw per action", 1, 1, 64);
    public final NumberSetting maxDurability = new NumberSetting("MaxDurability", "Stop when all armor >= this %", 100, 0, 100);
    public final BooleanSetting rotateDown = new BooleanSetting("Rotation", "Look straight down while throwing", true);
    public final ModeSetting autoSwap = new ModeSetting("Swap", "Swap mode.", "Swap", new String[]{"None", "Normal", "Silent", "Swap", "Pickup"});
    public final BooleanSetting pauseCA = new BooleanSetting("PauseCA", "Pause all crystal aura while ThrowXP is enabled.", true);

    private final Timer throwTimer = new Timer();

    @Override
    public void onEnable() {
        AutoCrystalFeature.setExternalPause(pauseCA.getValue());
    }

    @Override
    public void onDisable() {
        AutoCrystalFeature.setExternalPause(false);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        AutoCrystalFeature.setExternalPause(isEnabled() && pauseCA.getValue());

        if (checkArmorDurability()) {
            setToggled(false);
            return;
        }

        if (!throwTimer.hasTimeElapsed(delay.getValue().longValue())) return;

        Slot xpSlot = Services.INVENTORY.findSlot(SearchLogic.All, Items.EXPERIENCE_BOTTLE);
        if (xpSlot == null) return;

        if (rotateDown.getValue()) {
            mc.player.setPitch(90.0f);
        }

        if (Services.INVENTORY.switchTo(xpSlot, true, getSwitchType())) {
            int throwCount = Math.max(1, xpPer.getValue().intValue());

            for (int i = 0; i < throwCount; i++) {
                ItemStack stack = xpSlot.getStack();
                if (stack.isEmpty() || !stack.isOf(Items.EXPERIENCE_BOTTLE)) break;

                mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
                mc.player.swingHand(Hand.MAIN_HAND);
            }

            Services.INVENTORY.switchBack();
            throwTimer.reset();
        }
    }

    private boolean checkArmorDurability() {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        boolean hasArmor = false;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (!stack.isEmpty() && stack.isDamageable()) {
                hasArmor = true;
                int percent = (int) (((stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage()) * 100);
                if (percent < maxDurability.getValue().intValue()) return false;
            }
        }
        return hasArmor;
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

    @Override
    public String getInfo() {
        return "" + Services.INVENTORY.getCount(stack -> stack.isOf(Items.EXPERIENCE_BOTTLE));
    }
}
