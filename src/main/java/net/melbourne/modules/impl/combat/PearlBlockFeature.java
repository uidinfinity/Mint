package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.modules.PlaceFeature;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@FeatureInfo(name = "PearlBlock", category = Category.Combat)
public class PearlBlockFeature extends PlaceFeature {

    private final NumberSetting predictTicks = new NumberSetting("PredictTicks", "Max ticks to look ahead", 10, 1, 40);

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || shouldDisable()) return;

        List<EnderPearlEntity> pearls = mc.world.getEntitiesByClass(
                EnderPearlEntity.class,
                mc.player.getBoundingBox().expand(20.0),
                pearl -> {
                    if (pearl.getOwner() == null || pearl.getOwner() == mc.player || !pearl.isAlive()) return false;
                    if (pearl.getOwner() instanceof PlayerEntity owner) {
                        return !Managers.FRIEND.isFriend(owner.getName().getString());
                    }
                    return true;
                }
        );

        if (pearls.isEmpty()) return;

        EnderPearlEntity targetPearl = pearls.stream()
                .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)))
                .orElse(null);

        if (targetPearl == null) return;

        List<BlockPos> path = simulatePath(targetPearl);
        if (path.isEmpty()) return;

        Slot obbySlot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.OBSIDIAN, Items.ENDER_CHEST.canBeNested());
        if (obbySlot == null) return;

        placeBlocks(getPath(path));
    }

    private List<BlockPos> simulatePath(EnderPearlEntity pearl) {
        List<BlockPos> positions = new ArrayList<>();
        Vec3d currentPos = pearl.getPos();
        Vec3d velocity = pearl.getVelocity();

        for (int i = 0; i < predictTicks.getValue().intValue(); i++) {
            Vec3d nextPos = currentPos.add(velocity);

            for (double step = 0; step <= 1.0; step += 0.25) {
                Vec3d interp = currentPos.lerp(nextPos, step);
                BlockPos bp = BlockPos.ofFloored(interp);

                if (mc.world.getBlockState(bp).isReplaceable() && !positions.contains(bp)) {
                    positions.add(bp);
                }
            }

            currentPos = nextPos;
            velocity = velocity.multiply(0.99).subtract(0, 0.03, 0);

            if (mc.world.getBlockState(BlockPos.ofFloored(currentPos)).isFullCube(mc.world, BlockPos.ofFloored(currentPos))) {
                break;
            }
        }
        return positions;
    }
}