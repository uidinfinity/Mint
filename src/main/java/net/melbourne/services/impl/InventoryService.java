package net.melbourne.services.impl;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.melbourne.mixins.accessors.ClientPlayerInteractionManagerAccessor;
import net.melbourne.services.Service;
import net.melbourne.utils.Globals;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.melbourne.utils.inventory.switches.SwitchPriority;
import net.melbourne.utils.inventory.SwitchType;
import net.melbourne.utils.inventory.switches.types.HotbarSwitch;
import net.melbourne.utils.inventory.switches.types.InvSwitch;
import net.melbourne.utils.inventory.switches.types.SwapSwitch;
import net.melbourne.utils.inventory.switches.types.Switch;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static net.melbourne.utils.inventory.SwitchType.*;

@SuppressWarnings({"DataFlowIssue", "unchecked", "unused", "deprecation"})
public class InventoryService extends Service implements Globals {
    Map<InventoryService, List<Switch>> switchMap = new Object2ObjectOpenHashMap<>();

    public InventoryService() {
        super("Inventory", "Allows you to access your inventory.");
    }

    public SwitchPriority getSwitchPriority() {
        return SwitchPriority.Hotbar;
    }

    public boolean isLockable() {
        return true;
    }

    public boolean isUseXCarry() {
        return false;
    }


    public boolean isCheckCurrentSlot() {
        return true;
    }

    /*locker*/


    public List<Switch> getActions() {
        return switchMap.computeIfAbsent(this, l -> new ObjectArrayList<>());
    }

    public void lock(Switch action) {
        this.getActions().add(action);
    }


    public void clearAction() {
        while (!this.getActions().isEmpty()) {
            this.switchBack();
        }
    }

    public boolean isSwitchedBack() {
        return this.getActions().stream().allMatch(Switch::isSwitchedBack);
    }

    public boolean isAllowSwitchBack() {
        return true;
    }


    /*switcher*/

    public boolean swapOffhand() {
        boolean result = this.swapSlots(this.getOffHandSlot(), this.getSelectedSlot(), Swap);
        if (result) this.clearAction();

        return result;
    }

    public boolean switchTo(SearchLogic logic, boolean mainHand, SwitchType switchType, Block... blocks) {
        return this.switchTo(logic, mainHand, switchType, Stream.of(blocks).map(Item::fromBlock).toArray(Item[]::new));
    }

    public boolean switchTo(SearchLogic logic, SwitchType switchType, Block... blocks) {
        return this.switchTo(logic, true, switchType, Stream.of(blocks).map(Item::fromBlock).toArray(Item[]::new));
    }

    public boolean switchTo(SearchLogic logic, SwitchType switchType, Item... items) {
        return this.switchTo(logic, true, switchType, items);
    }

    public boolean switchTo(SearchLogic logic, boolean mainHand, SwitchType switchType, Item... items) {
        if (items.length < 1) throw new IllegalArgumentException("Not specified item");
        for (Item item : items) {
            Slot slot = find(logic, s -> s != null && s.getStack().getItem() == item ? s : null);
            if (slot == this.getSelectedSlot() || this.switchTo(slot, mainHand, switchType))
                return true;
        }

        return false;
    }

    public boolean switchTo(int slot, SwitchType type) {
        if (slot == -1) return false;
        return this.switchTo(slot, true, type);
    }

    public boolean switchTo(int slot, boolean mainHand, SwitchType type) {
        if (slot == -1) return false;
        return this.switchTo(this.getSlot(slot, !this.isUseXCarry() && this.isHotbar(slot)), mainHand, type);
    }

    public boolean switchTo(Slot to, boolean mainHand, SwitchType type) {
        Slot from;

        if (mainHand) {
            from = this.getSelectedSlot();
        } else from = this.getOffHandSlot();

        return this.swapSlots(from, to, type);
    }

    public boolean swapSlots(int from, int to, SwitchType type) {
        return this.swapSlots(this.getSlot(from), this.getSlot(to), type);
    }

    public boolean swapSlots(Slot from, Slot to, SwitchType type) {
        if (from != null && to != null && type != null) {

            if (from == to) {
                return true;
            }

            Switch action = switch (type) {
                case None -> null;
                case Normal -> new HotbarSwitch(from, to, false);
                case Silent -> new HotbarSwitch(from, to, this.isAllowSwitchBack());
                case Swap -> new SwapSwitch(from, to, this.isAllowSwitchBack());
                case PickUp -> new InvSwitch(from, to, this.isAllowSwitchBack());
            };

            if (action != null) {
                this.getActions().add(action);
                return this.getActions().getLast().execute();
            }

        }

        return false;
    }

    public void syncItem() {
        if (mc.player == null)
            return;

        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
    }

    public boolean switchBack() {
        if (this.getActions().isEmpty()) {
            return true;
        }

        var actions = this.getActions();

        Switch action = actions.removeLast();

        return action.revert();
    }


    /*==== Finders ====*/


    public boolean isPresent(SearchLogic logic, Block... blocks) {
        if (blocks.length < 1) throw new IllegalArgumentException("Not specified block");

        for (Block Block : blocks) {
            Slot slot = find(logic, s -> s.getStack().getItem() == Block.asItem() ? s : null);
            if (slot != null) return true;
        }
        return false;
    }

    public boolean isPresent(SearchLogic logic, Item... items) {
        if (items.length < 1) throw new IllegalArgumentException("Not specified item");
        for (Item item : items) {
            Slot slot = find(logic, s -> s.getStack().getItem() == item ? s : null);
            if (slot != null) return true;
        }
        return false;
    }


    public @Nullable Slot findSlot(SearchLogic logic, Item item) {
        return find(logic, false, s -> s.getStack().getItem() == item ? s : null);
    }
    public @Nullable Slot findSlot(SearchLogic logic, Item item, boolean ignoreOffhand) {
        return find(logic, ignoreOffhand, s -> s.getStack().getItem() == item ? s : null);
    }

    @SuppressWarnings("RedundantCast")
    public Item find(SearchLogic logic, Item... items) {
        return (Item) find(logic, Arrays.stream(items).map(this::findItemFunction).toArray(Function[]::new)); // DO NOT REMOVE CAST!!!!!!
    }

    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Block findBlock(SearchLogic logic, Block... blocks) {
        return (Block) find(logic, Arrays.stream(blocks).map(this::findBlockFunction).toArray(Function[]::new));
    }

    private Function<Slot, Block> findBlockFunction(Block block) {
        return slot -> slot.getStack().isOf(block.asItem()) && slot.getStack().getCount() != 0 ? block : null;
    }

    private Function<Slot, Item> findItemFunction(Item item) {
        return slot -> slot.getStack().isOf(item) && slot.getStack().getCount() != 0 ? item : null;
    }


    public <T> @Nullable T find(SearchLogic logic, Function<Slot, @Nullable T>... functions) {
        return find(logic, false, functions);
    }

    public <T> @Nullable T find(SearchLogic logic, boolean ignoreOffhand, Function<Slot, @Nullable T>... functions) {
        for (var function : functions) {
            T result = function.apply(mc.player.playerScreenHandler.getSlot(45));
            if (result != null && !ignoreOffhand) {
                return result;
            }

            switch (this.getSwitchPriority()) {
                case Hotbar -> {
                    result = getHotbarResult(logic, function);
                    if (result != null) {
                        return result;
                    }
                    return getInventoryResult(logic, function);
                }
                case Inventory -> {
                    result = getInventoryResult(logic, function);
                    if (result != null) {
                        return result;
                    }
                    return getHotbarResult(logic, function);
                }
            }
        }

        return null;
    }

    public <T> T getHotbarResult(SearchLogic logic, Function<Slot, @Nullable T> function) {
        T result;
        if (logic == SearchLogic.All || logic == SearchLogic.OnlyHotbar) {
            if (this.isCheckCurrentSlot()) {
                result = function.apply(this.getSelectedSlot()); // Current slot
                if (result != null) {
                    return result;
                }
            }

            for (int i = 36; i < 45; i++) { // Hotbar
                result = function.apply(this.getSlot(i));
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    public <T> T getInventoryResult(SearchLogic logic, Function<Slot, @Nullable T> function) {
        T result = null;
        if (logic == SearchLogic.All || logic == SearchLogic.IgnoreHotbar) {
            if (this.isUseXCarry()) {
                for (int i = 0; i < 9; i++) {// CarryExploit
                    result = function.apply(this.getSlot(i));
                    if (result != null) {
                        return result;
                    }
                }
            }

            for (int i = 9; i < 36; i++) { // Inventory
                result = function.apply(this.getSlot(i));
                if (result != null) {
                    return result;
                }
            }
        }
        if (logic == SearchLogic.IgnoreOffhandAndHotbar) {
            for (int i = 9; i < 36; i++) { // Inventory
                result = function.apply(this.getSlot(i));
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    public int getCount(Predicate<ItemStack> check) {
        int count = 0;
        for (Slot slot : mc.player.playerScreenHandler.slots) {
            if (check.test(slot.getStack())) {
                count += slot.getStack().getCount();
            }
        }

        return count;
    }

    public boolean isInventorySwitch(SwitchType type) {
        return type == Swap || type == PickUp;
    }

    public Slot getOffHandSlot() {
        return this.getSlot(45);
    }

    public Slot getSelectedSlot() {
        return this.getSlot(mc.player.getInventory().getSelectedSlot(), true);
    }

    private Slot getSlot(int slot) {
        return this.getSlot(slot, false);
    }

    private Slot getSlot(int slot, boolean isHotbar) {
        if (slot < 0)
            return null;
        return mc.player.playerScreenHandler.getSlot(isHotbar ? slot + 36 : slot);
    }

    private boolean isSelected(Slot slot) {
        return slot.getIndex() == getSelectedSlot().getIndex();
    }

    private boolean isHotbar(int slot) {
        return slot >= 0 && slot < 9;
    }

    public int getNextSlot() {
        int slot = mc.player.getInventory().getSelectedSlot() + 1;
        return slot < 9 ? slot : 0;
    }

}
