package net.melbourne.utils.inventory.switches.types;

import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;

public class HotbarSwitch extends Switch {
    public HotbarSwitch(Slot from, Slot to, boolean switchBack) {
        super(from, to, switchBack);
    }

    @Override
    public boolean execute(Slot from, Slot to) {
        if (mc.player == null) return false;

        if (mc.player.currentScreenHandler instanceof PlayerScreenHandler && this.isHotbar(to)) {
            mc.player.getInventory().setSelectedSlot(to.getIndex());
            this.syncItem(mc.player.getInventory().getSelectedSlot());
            return true;
        }

        return false;
    }
}
