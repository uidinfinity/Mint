package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.modules.PlaceFeature;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.movement.HitboxDesyncFeature;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.utils.entity.player.movement.position.PositionUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@FeatureInfo(name = "Surround", category = Category.Combat)
public class SurroundFeature extends PlaceFeature {
    private final BooleanSetting extension = new BooleanSetting("Extension", "Extends surround around entities", true);
    private final BooleanSetting floor = new BooleanSetting("Floor", "Places blocks under feet", true);

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (shouldDisable()) return;

        boolean moving = mc.player.getVelocity().horizontalLengthSquared() > 0.0025;
        boolean useFloor = floor.getValue() && !moving;

        Set<BlockPos> targetPositions = getFeetPositions(mc.player, extension.getValue(), useFloor);
        if (targetPositions.isEmpty()) return;

        placeBlocks(targetPositions);
    }


    private Set<BlockPos> getFeetPositions(PlayerEntity target, boolean extension, boolean floor) {
        HashSet<BlockPos> positions = new HashSet<>();
        HashSet<BlockPos> blacklist = new HashSet<>();
        HitboxDesyncFeature desync = Managers.FEATURE.getFeatureFromClass(HitboxDesyncFeature.class);

        BlockPos feetPos = PositionUtils.getFlooredPosition(target);
        blacklist.add(feetPos);

        if (extension) {
            for (Direction dir : Direction.values()) {
                if (dir.getAxis().isVertical()) continue;
                BlockPos off = feetPos.offset(dir);
                List<PlayerEntity> collisions = mc.world.getEntitiesByClass(PlayerEntity.class, new Box(off), e -> e.isAlive());
                for (PlayerEntity player : collisions) {
                    if (player == mc.player && desync != null && desync.isEnabled()) continue;
                    Box box = player.getBoundingBox();
                    for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
                        for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                            blacklist.add(new BlockPos(x, feetPos.getY(), z));
                        }
                    }
                }
            }
        }

//      if (!BotManager.INSTANCE.isAuthed())
//          System.exit(0);

        for (BlockPos pos : blacklist) {
            if (floor) positions.add(pos.down());
            for (Direction dir : Direction.values()) {
                if (!dir.getAxis().isHorizontal()) continue;
                BlockPos off = pos.offset(dir);
                if (!blacklist.contains(off)) positions.add(off);
            }
        }

        if (target == mc.player && desync != null && desync.isEnabled()) {
            List<BlockPos> desyncPositions = new ArrayList<>();
            Vec3d center = mc.player.getBlockPos().toCenterPos();
            boolean flagX = (center.x - mc.player.getX()) > 0;
            boolean flagZ = (center.z - mc.player.getZ()) > 0;
            desyncPositions.add(mc.player.getBlockPos().add(flagX ? -1 : 1, 0, 0));
            desyncPositions.add(mc.player.getBlockPos().add(0, 0, flagZ ? -1 : 1));
            positions.removeIf(desyncPositions::contains);
        }
        return positions;
    }
}