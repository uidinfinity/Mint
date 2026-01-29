package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.modules.PlaceFeature;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.utils.inventory.SwitchType;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@FeatureInfo(name = "AntiPhase", category = Category.Combat)
public class AntiPhaseFeature extends PlaceFeature {
    private final ModeSetting blockMode = new ModeSetting("Block", "Object to place", "Scaffolding", new String[]{"Scaffolding", "ItemFrames", "String", "Trapdoors", "Vines"});

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || shouldDisable()) return;

        PlayerEntity target = findTarget();
        if (target == null) return;

        BlockPos targetPos = target.getBlockPos();
        Item targetItem = getRequiredItem();

        Slot itemSlot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, targetItem);
        if (itemSlot == null) return;

        if (blockMode.getValue().equals("ItemFrames")) {
            List<ItemFrameEntity> frames = mc.world.getEntitiesByClass(ItemFrameEntity.class, new Box(targetPos),
                    f -> f.getHorizontalFacing() == Direction.UP || f.getHorizontalFacing() == Direction.DOWN);

            if (frames.isEmpty()) {
                placeBlocks(Collections.singletonList(targetPos));
            } else {
                ItemFrameEntity frame = frames.get(0);
                if (frame.getHeldItemStack().isEmpty()) {
                    int prev = mc.player.getInventory().getSelectedSlot();
                    Services.INVENTORY.switchTo(itemSlot.getIndex(), getSwitchType());

                    mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(frame, mc.player.isSneaking(), Hand.MAIN_HAND));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

                    Services.INVENTORY.switchTo(prev, getSwitchType());
                }
            }
        } else {
            placeBlocks(Collections.singletonList(targetPos));
        }
    }


    private Item getRequiredItem() {
        return switch (blockMode.getValue()) {
            case "ItemFrames" -> Items.ITEM_FRAME;
            case "String" -> Items.STRING;
            case "Trapdoors" -> Items.OAK_TRAPDOOR;
            case "Vines" -> Items.VINE;
            default -> Items.SCAFFOLDING;
        };
    }

    public SwitchType getSwitchType() {
        return SwitchType.valueOf(switchMode.getValue());
    }

    private PlayerEntity findTarget() {
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive())
                .filter(p -> !Managers.FRIEND.isFriend(p.getName().getString()))
                .filter(p -> mc.player.distanceTo(p) <= placeRange.getValue().floatValue())
                .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)))
                .orElse(null);
    }
}