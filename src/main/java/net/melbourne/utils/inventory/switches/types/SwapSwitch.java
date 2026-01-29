package net.melbourne.utils.inventory.switches.types;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.modules.impl.client.AntiCheatFeature;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@SuppressWarnings("DataFlowIssue")
public class SwapSwitch extends Switch {
    public SwapSwitch(Slot from, Slot to, boolean switchback) {
        super(from, to, switchback);
    }

    @Override
    public boolean execute(Slot from, Slot to) {
        if (mc.player == null) return false;
        if (mc.player.currentScreenHandler instanceof PlayerScreenHandler handler) {
            boolean isSwapFix = this.isSwapSlot(from) || this.isSwapSlot(to);

            if (Managers.FEATURE.getFeatureFromClass(AntiCheatFeature.class).altSwap.getValue() && !isSwapFix) {
                Slot hotbarSlot = handler.getSlot(Services.INVENTORY.getNextSlot() + 36);

                return this.isSwapSlot(from)
                        ? this.execute(hotbarSlot, from) && this.execute(hotbarSlot, to) && this.execute(hotbarSlot, from)
                        : this.execute(hotbarSlot, to) && this.execute(hotbarSlot, from) && this.execute(hotbarSlot, to);
            }

            int index;
            int slotId;

            if (this.isSwapSlot(from)) {
                index = from.getIndex();
                slotId = to.id;
            } else {
                index = to.getIndex();
                slotId = from.id;
            }

            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slotId, index, SlotActionType.SWAP, mc.player);
            this.closeScreen();
            return true;
        }

        return false;
    }

    @Override
    public boolean revert() {
        if (!allowSwitchBack) {
            return false;
        }
        return switchedBack = execute(from, to);
    }

    private boolean isSwapSlot(Slot slot) {
        return slot.getIndex() >= 0 && slot.getIndex() < 9 || slot.getIndex() == 40;
    }
}