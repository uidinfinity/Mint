package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.modules.PlaceFeature;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.block.hole.HoleUtils;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@FeatureInfo(name = "HoleFill", category = Category.Combat)
public class HoleFillFeature extends PlaceFeature {

    private final ModeSetting mode = new ModeSetting("Mode", "Normal or Auto", "Auto", new String[]{"Normal", "Auto"});
    private final BooleanSetting twoByOne = new BooleanSetting("2x1", "Include 2x1 holes", false);
    public final NumberSetting xDist = new NumberSetting("HorizontalRange", "Horizontal auto range", 3.5, 1.0, 6.0);
    private final NumberSetting yDist = new NumberSetting("VerticalRange", "Vertical auto range", 2.0, 0.5, 6.0);
    private final NumberSetting noSelfRange = new NumberSetting("SelfRange", "Avoid self-filling", 1.8, 0.5, 5.0);
    private final List<BlockPos> positions = new ArrayList<>();

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) return;
        updatePlayerHole();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (shouldDisable()) return;

        updatePlayerHole();

        List<BlockPos> targetPositions = getToPlacePositions();
        if (targetPositions.isEmpty()) return;

        Slot obbySlot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.OBSIDIAN, Items.ENDER_CHEST.canBeNested());
        if (obbySlot == null) return;

        placeBlocks(getPath(targetPositions));
    }

    private void updatePlayerHole() {
        if (mc.player == null) return;
        positions.clear();
        BlockPos playerPos = mc.player.getBlockPos();
        HoleUtils.Hole insideHole = HoleUtils.getSingleHole(playerPos, 0);

        if (insideHole == null && twoByOne.getValue()) {
            insideHole = HoleUtils.getDoubleHole(playerPos, 0);
        }

        if (insideHole != null) {
            positions.add(HoleUtils.getFirst(insideHole));
            BlockPos second = HoleUtils.getSecond(insideHole);
            if (second != null) positions.add(second);
        }
    }

    private List<BlockPos> getToPlacePositions() {
        List<BlockPos> positions = new ArrayList<>();
        double range = placeRange.getValue().doubleValue();
        int rangeInt = (int) Math.ceil(range);

        for (int x = -rangeInt; x <= rangeInt; x++) {
            for (int y = -rangeInt; y <= rangeInt; y++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y, z);
                    if (mc.player.squaredDistanceTo(pos.toCenterPos()) > Math.pow(range, 2)) continue;
                    if (this.positions.contains(pos)) continue;

                    HoleUtils.Hole singleHole = HoleUtils.getSingleHole(pos, 0);
                    if (singleHole != null) addHolePositions(singleHole, positions);
                    else if (twoByOne.getValue()) {
                        HoleUtils.Hole doubleHole = HoleUtils.getDoubleHole(pos, 0);
                        if (doubleHole != null) addHolePositions(doubleHole, positions);
                    }
                }
            }
        }
        return positions.stream().distinct().collect(Collectors.toList());
    }

    private void addHolePositions(HoleUtils.Hole hole, List<BlockPos> positions) {
        BlockPos first = HoleUtils.getFirst(hole);
        BlockPos second = HoleUtils.getSecond(hole);

        if (mc.player.squaredDistanceTo(first.toCenterPos()) > Math.pow(placeRange.getValue().doubleValue(), 2)) return;
        if (this.positions.contains(first)) return;
        if (second != null && this.positions.contains(second)) return;

        if (mode.getValue().equals("Auto")) {
            boolean nearOtherPlayer = mc.world.getPlayers().stream().anyMatch(p -> p != mc.player
                    && p.isAlive()
                    && !Managers.FRIEND.isFriend(p.getName().getString())
                    && Math.sqrt(Math.pow(p.getX() - first.getX() - 0.5, 2) + Math.pow(p.getZ() - first.getZ() - 0.5, 2)) <= xDist.getValue().doubleValue()
                    && Math.abs(p.getY() - first.getY() - 0.5) <= yDist.getValue().doubleValue());
            if (!nearOtherPlayer) return;
        } else if (mc.player.squaredDistanceTo(first.toCenterPos()) < Math.pow(noSelfRange.getValue().doubleValue(), 2)) return;

        if (mc.world.getBlockState(first).isReplaceable() && !isPlayerInBlock(first)) positions.add(first);
        if (second != null && mc.world.getBlockState(second).isReplaceable() && !isPlayerInBlock(second)) positions.add(second);
    }

    private boolean isPlayerInBlock(BlockPos pos) {
        Box box = new Box(pos);
        return !mc.world.getEntitiesByClass(PlayerEntity.class, box, e -> e != mc.player).isEmpty();
    }
}