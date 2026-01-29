package net.melbourne.modules.impl.player;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketSendEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.mixins.accessors.PlayerInteractEntityC2SPacketAccessor;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.services.Services;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.utils.inventory.switches.SearchLogic; // You might still need this for the Mace check below, if that part works.
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;

@FeatureInfo(name = "MaceHelper", category = Category.Player)
public class MaceHelperFeature extends Feature {

    private final BooleanSetting onlyPlayers = new BooleanSetting("OnlyPlayers", "Attribute swaps on players only, instead of every entity.", true);
    private final BooleanSetting chestSwap = new BooleanSetting("ChestSwap", "Swaps from Elytra to Chestplate before hitting.", true);

    private int lastSlot = -1;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || lastSlot == -1) return;

        if (mc.player.getInventory().getSelectedSlot() == lastSlot)
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(lastSlot));

        lastSlot = -1;
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {

            if (onlyPlayers.getValue()) {
                Entity target = mc.world.getEntityById(((PlayerInteractEntityC2SPacketAccessor) packet).getEntityId());
                if (!(target instanceof PlayerEntity))
                    return;
            }

            if (chestSwap.getValue()) {
                if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {

                    int chestplateSlot = -1;

                    for (int i = 0; i < 36; i++) {
                        ItemStack stack = mc.player.getInventory().getStack(i);
                        if (stack.getItem() == Items.NETHERITE_CHESTPLATE) {
                            chestplateSlot = i;
                            break;
                        }
                        if (stack.getItem() == Items.DIAMOND_CHESTPLATE && chestplateSlot == -1) {
                            chestplateSlot = i;
                        }
                    }

                    if (chestplateSlot != -1) {
                        int serverSlot = chestplateSlot < 9 ? chestplateSlot + 36 : chestplateSlot;

                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, serverSlot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, serverSlot, 0, SlotActionType.PICKUP, mc.player);
                    }
                }
            }

            lastSlot = mc.player.getInventory().getSelectedSlot();
            @Nullable Slot mace = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.MACE);

            if (mace == null) {
                lastSlot = -1;
                return;
            }

            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mace.getIndex()));
        }
    }
}