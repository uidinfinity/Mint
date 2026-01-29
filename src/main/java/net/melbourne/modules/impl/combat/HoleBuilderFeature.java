package net.melbourne.modules.impl.combat;

import net.melbourne.modules.PlaceFeature;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.utils.block.hole.HoleUtils;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.melbourne.services.Services;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

@FeatureInfo(name = "HoleBuilder", category = Category.Combat)
public class HoleBuilderFeature extends PlaceFeature {

    private BlockPos pos = null;
    private final List<BlockPos> placables = new ArrayList<>();

    @Override
    public void onEnable() {
        super.onEnable();
        pos = null;
        placables.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (shouldDisable()) return;

        BlockPos playerPos = mc.player.getBlockPos();

        if (!HoleUtils.isIncompleteHole(playerPos)) {
            pos = null;
            placables.clear();
            return;
        }

        if (pos == null || !pos.equals(playerPos)) {
            pos = playerPos;
            placables.clear();
            placables.addAll(HoleUtils.getBlocksToCompleteHole(playerPos));
        }

        placables.removeIf(pos -> !mc.world.getBlockState(pos).isReplaceable());
        if (placables.isEmpty()) return;

        Slot obbySlot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.OBSIDIAN, Items.ENDER_CHEST.canBeNested());
        if (obbySlot == null) return;

        List<BlockPos> validPositions = placables.stream()
                .filter(pos -> !isEntityBlocking(pos))
                .toList();

        if (validPositions.isEmpty()) return;

        placeBlocks(getPath(validPositions));
    }

    private boolean isEntityBlocking(BlockPos pos) {
        return mc.world.getOtherEntities(null, new Box(pos)).stream()
                .anyMatch(e -> !(e instanceof EndCrystalEntity));
    }
}