package net.melbourne.utils.inventory.switches.types;

import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@SuppressWarnings("DataFlowIssue")
public class InvSwitch extends Switch {
    public InvSwitch(Slot from, Slot to, boolean switchBack) {
        super(from, to, switchBack);
    }

    @Override
    public boolean execute(Slot from, Slot to) {
        if (mc.player == null) return false;

        if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            mc.interactionManager.clickSlot(0, from.id, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, to.id, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, from.id, 0, SlotActionType.PICKUP, mc.player);
            this.syncItem(mc.player.getInventory().getSelectedSlot());
            return true;
        }
        return false;
    }
}
