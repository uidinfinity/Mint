package net.melbourne.engine.simulation;

import net.melbourne.Managers;
import net.melbourne.utils.block.hole.HoleUtils;
import net.melbourne.utils.entity.DamageUtils;
import net.melbourne.engine.prediction.PredictionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import java.util.*;

import static net.melbourne.utils.Globals.mc;

public class Simulations {

    public static BlockPos getSuperSmartTarget(PlayerEntity target, float range, float minDamage) {
        Set<BlockPos> potentialBlocks = HoleUtils.getFeetPositions(target, true, true, false);
        BlockPos targetFeet = target.getBlockPos();

        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    potentialBlocks.add(targetFeet.add(x, y, z));
                }
            }
        }

        return potentialBlocks.stream()
                .filter(pos -> canBeMined(pos, range))
                .max(Comparator.comparingDouble(pos -> calculateWeightedValue(pos, target, minDamage)))
                .filter(pos -> calculateWeightedValue(pos, target, minDamage) > 0)
                .orElse(null);
    }

    private static double calculateWeightedValue(BlockPos pos, PlayerEntity target, float minDamage) {
        double weight = 0;
        Vec3d predictedPos = PredictionUtil.predictPosition(target, 2.0f, true);
        Box predictedBox = target.getBoundingBox().offset(predictedPos.subtract(target.getPos()));

        float maxDmg = 0;
        BlockPos[] crystalPositions = {pos, pos.down()};

        for (BlockPos cp : crystalPositions) {
            if (canPlaceCrystal(cp)) {
                float dmg = DamageUtils.getDamage(target, predictedBox, cp.toCenterPos(), 6.0f, pos, false);
                if (dmg > maxDmg) maxDmg = dmg;
            }
        }

        if (maxDmg < minDamage) return 0;

        weight += maxDmg * 1.5;
        weight += (5.0 - Math.sqrt(pos.getSquaredDistance(target.getBlockPos()))) * 2.0;

        if (HoleUtils.getFeetPositions(target, true, true, false).contains(pos)) weight += 15.0;
        
        weight += calculateCityWeight(pos, target);

        if (pos.getY() > target.getY()) weight += 3.0;

        int airSurrounding = 0;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (mc.world.getBlockState(pos.offset(dir)).isAir()) airSurrounding++;
        }
        weight += (airSurrounding * 2.5);

        return weight;
    }

    private static double calculateCityWeight(BlockPos pos, PlayerEntity primaryTarget) {
        double cityWeight = 0;
        for (PlayerEntity other : mc.world.getPlayers()) {
            if (other == mc.player || other == primaryTarget || Managers.FRIEND.isFriend(other.getName().getString())) continue;
            if (other.squaredDistanceTo(primaryTarget) > 9.0) continue;

            if (HoleUtils.getFeetPositions(other, true, true, false).contains(pos)) {
                cityWeight += 12.0;
            }
        }
        return cityWeight;
    }

    public static boolean canPlaceCrystal(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK) && !state.isOf(Blocks.RESPAWN_ANCHOR)) return false;
        return mc.world.getBlockState(pos.up()).isAir() && (mc.world.getBlockState(pos.up(2)).isAir() || !mc.world.getBlockState(pos.up(2)).isFullCube(mc.world, pos.up(2)));
    }

    public static boolean canBeMined(BlockPos pos, float range) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir() || state.getHardness(mc.world, pos) < 0) return false;
        return mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) <= MathHelper.square(range);
    }

    public static List<BlockPos> getSidePlacementSpots(BlockPos miningPos, PlayerEntity target, float minDamage) {
        List<BlockPos> spots = new ArrayList<>();
        Vec3d predictedPos = PredictionUtil.predictPosition(target, 2.0f, true);
        Box predictedBox = target.getBoundingBox().offset(predictedPos.subtract(target.getPos()));

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos sidePos = miningPos.offset(dir);

            if (canPlaceCrystal(sidePos)) {
                float dmg = DamageUtils.getDamage(target, predictedBox, sidePos.up().toCenterPos(), 6.0f, miningPos, false);
                if (dmg >= minDamage) {
                    spots.add(sidePos);
                }
            }
        }
        return spots;
    }
}