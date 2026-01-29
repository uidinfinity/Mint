package net.melbourne.utils.inventory.switches.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.melbourne.utils.Globals;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;

@SuppressWarnings("DataFlowIssue")
@Getter
@RequiredArgsConstructor
public abstract class Switch implements Globals {
    protected final Slot from;
    protected final Slot to;
    protected final boolean allowSwitchBack;
    @Setter
    protected boolean switchedBack = false;

    public abstract boolean execute(Slot from, Slot to);

    public boolean execute() {
        return execute(from, to);
    }

    public boolean revert() {
        if (!allowSwitchBack || switchedBack) {
            return false;
        }
        return switchedBack = execute(to, from);
    }

    protected boolean isHotbar(int slot) {
        return slot >= 0 && slot < 9;
    }

    protected boolean isHotbar(Slot slot) {
        return slot.getIndex() >= 0 && slot.getIndex() < 9;
    }

    protected void closeScreen() {
        if (mc.world == null || mc.player == null)
            return;

        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
    }

    @SuppressWarnings("DataFlowIssue")
    protected void syncItem(int slot) {
        if (mc.world == null || mc.player == null)
            return;

        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }
}
