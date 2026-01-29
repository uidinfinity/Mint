package net.melbourne.services.impl;

import net.melbourne.Melbourne;
import net.melbourne.Managers;
import net.melbourne.modules.impl.combat.AutoMineFeature;
import net.melbourne.services.Service;
import net.melbourne.utils.block.hole.HoleUtils;
import net.melbourne.utils.entity.DamageUtils;
import net.melbourne.engine.prediction.PredictionUtil;
import net.melbourne.engine.simulation.Simulations;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import java.util.*;

import static net.melbourne.utils.Globals.mc;

public class SimulationService extends Service {

    public SimulationService() {
        super("Simulation", "Simulates player behaviors and localized physics updates.");
        Melbourne.EVENT_HANDLER.subscribe(this);
    }

    public Vec3d getMovementPrediction(PlayerEntity player, float magnitude) {
        if (Managers.FRIEND.isFriend(player.getName().getString())) return player.getPos();
        return PredictionUtil.predictPosition(player, magnitude, true);
    }

    public BlockPos predictMiningTarget(PlayerEntity enemy) {
        if (Managers.FRIEND.isFriend(enemy.getName().getString())) return null;

        AutoMineFeature autoMine = Managers.FEATURE.getFeatureFromClass(AutoMineFeature.class);
        if (autoMine == null) return null;

        List<BlockPos> targets = new ArrayList<>();
        if (autoMine.sequence.getValue().equalsIgnoreCase("Surround")) {
            targets.addAll(getSimulatedSurround(enemy));
        } else {
            targets.addAll(getSimulatedPath(enemy, autoMine));
            targets.addAll(getSimulatedSurround(enemy));
        }

        for (BlockPos pos : targets) {
            if (Simulations.canBeMined(pos, autoMine.range.getValue().floatValue())) return pos;
        }
        return null;
    }

    public BlockPos findSurroundMineTarget(PlayerEntity targetPlayer) {
        if (Managers.FRIEND.isFriend(targetPlayer.getName().getString())) return null;

        BlockPos myPos = mc.player.getBlockPos();
        BlockPos closestToTarget = null;
        double minDistance = Double.MAX_VALUE;

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos check = myPos.offset(dir);
            BlockState state = mc.world.getBlockState(check);
            if (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.ENDER_CHEST)) {
                double dist = targetPlayer.squaredDistanceTo(check.getX() + 0.5, check.getY() + 0.5, check.getZ() + 0.5);
                if (dist < minDistance) {
                    minDistance = dist;
                    closestToTarget = check;
                }
            }
        }
        return closestToTarget;
    }

    public BlockPos getSuperSmartTarget(PlayerEntity target, float range, float minDamage) {
        if (Managers.FRIEND.isFriend(target.getName().getString())) return null;
        return Simulations.getSuperSmartTarget(target, range, minDamage);
    }

    public List<BlockPos> getSimulatedSurround(PlayerEntity enemy) {
        return new ArrayList<>(HoleUtils.getFeetPositions(enemy, true, true, false));
    }

    public List<BlockPos> getSimulatedPath(PlayerEntity enemy, AutoMineFeature autoMine) {
        List<BlockPos> list = new ArrayList<>();
        Box box = enemy.getBoundingBox();
        List<BlockPos> occupied = new ArrayList<>();
        for (double x = box.minX; x <= box.maxX; x += (box.maxX - box.minX)) {
            for (double z = box.minZ; z <= box.maxZ; z += (box.maxZ - box.minZ)) {
                BlockPos pos = new BlockPos(MathHelper.floor(x), MathHelper.floor(box.minY), MathHelper.floor(z));
                if (!occupied.contains(pos)) occupied.add(pos);
            }
        }

        for (BlockPos pos : occupied) {
            if (autoMine.face.getValue()) {
                for (Direction dir : Direction.Type.HORIZONTAL) {
                    list.add(pos.offset(dir).up());
                }
            }
            if (autoMine.aboveHead.getValue()) list.add(pos.up(2));
        }
        return list;
    }

    public boolean isSelfPop(BlockPos crystalPos, float maxSelfDamage) {
        if (mc.player == null) return false;
        Vec3d crystalVec = crystalPos.toCenterPos().subtract(0, 0.5, 0);
        float damage = DamageUtils.getDamage(mc.player, mc.player.getBoundingBox(), crystalVec, 6.0f, null, false);
        return damage > maxSelfDamage || damage >= (mc.player.getHealth() + mc.player.getAbsorptionAmount());
    }
}