package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.misc.PacketMineFeature;
import net.melbourne.modules.impl.movement.HitboxDesyncFeature;
import net.melbourne.pathfinding.placement.PlacePathfinding;
import net.melbourne.services.Services;
import net.melbourne.settings.types.*;
import net.melbourne.utils.entity.player.movement.position.PositionUtils;
import net.melbourne.utils.rotation.RotationPoint;
import net.melbourne.utils.rotation.RotationUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

import java.util.*;
import java.util.stream.Collectors;

@FeatureInfo(name = "AutoMine", category = Category.Combat)
public class AutoMineFeature extends Feature {

    public NumberSetting range = new NumberSetting("Range", "Target range.", 5.0f, 1.0f, 6.0f);
    public ModeSetting sequence = new ModeSetting("Sequence", "Mining order.", "Dynamic", new String[]{"Surround", "Phase", "Dynamic"});
    public ModeSetting blockerSequence = new ModeSetting("Blocker", "Blocker mining order.", "None", new String[]{"None", "Surround", "Above", "Smart"});
    public BooleanSetting face = new BooleanSetting("Face", "Mines face blocks.", false);
    public BooleanSetting aboveHead = new BooleanSetting("Head", "Mines above head.", false);
    public BooleanSetting antiCrawl = new BooleanSetting("AntiCrawl", "Prevent crawling.", true);
    public BooleanSetting doubleMine = new BooleanSetting("DoubleMine", "Use double break packets.", false);
    public BooleanSetting cev = new BooleanSetting("Cev", "Places a crystal right before the mined obsidian breaks and breaks it after the block turns to air.", false);
    public BooleanSetting bedrock = new BooleanSetting("Bedrock", "Allows targeting bedrock blocks.", false);

    private final PlacePathfinding pathfinder = new PlacePathfinding();
    private PacketMineFeature packetMine;
    private Target target;
    private BlockPos pos;
    private int priority = 99;

    private BlockPos cevFor = null;
    private long cevUntilMs = 0L;

    @Override
    public void onEnable() {
        packetMine = Managers.FEATURE.getFeatureFromClass(PacketMineFeature.class);
        target = null;
        pos = null;
        priority = 99;
        resetCev();
    }

    @Override
    public String getInfo() {
        String mode = sequence.getValue();
        if (target != null && pos != null && packetMine != null && packetMine.isMining(pos)) {
            return target.player().getName().getString() + ", " + mode;
        }
        return mode;
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull() || mc.player.isCreative() || mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE)
            return;
        if (packetMine == null || !packetMine.isEnabled() || packetMine.isFeaturePaused()) return;

        tickCevPost();

        if (antiCrawl.getValue() && mc.player.getPose() == EntityPose.SWIMMING) {
            BlockPos headPos = mc.player.getBlockPos().up();
            if (isValid(headPos) && !isOutOfRange(headPos)) {
                handle(headPos, 0);
                return;
            }
        }

        Target target = getTarget();
        this.target = target;

        if (target == null) {
            pos = null;
            priority = 99;
            resetCev();
            return;
        }

        if (pos != null && packetMine.isMining(pos)) {
            tickCevWhileMining(pos);

            if (!isOutOfRange(pos)) {
                if (packetMine.reBreak.getValue() || !mc.world.getBlockState(pos).isAir()) {
                    return;
                }
            }
        }

        BlockPos antiCrawlPos = getAntiCrawlPos(target.player());
        if (antiCrawl.getValue() && target.player().getPose().equals(EntityPose.SWIMMING) && antiCrawlPos != null) {
            if (isValid(antiCrawlPos) && !isOutOfRange(antiCrawlPos)) {
                handle(antiCrawlPos, 0);
                return;
            }
        }

        BlockPos bestBlock = findHighestPriorityBlock(target.player());
        if (bestBlock != null) {
            handle(bestBlock, getPriorityForPos(target.player(), bestBlock));
        } else {
            handleBlockerMining(target.player());
        }
    }

    private void tickCevWhileMining(BlockPos miningPos) {
        if (!cev.getValue()) return;

        AutoCrystalFeature ca = Managers.FEATURE.getFeatureFromClass(AutoCrystalFeature.class);
        if (ca == null || !ca.isEnabled()) return;

        PacketMineFeature.MiningData data = packetMine.getMiningData();
        if (data == null || data.getPos() == null || !miningPos.equals(data.getPos())) return;

        BlockState state = mc.world.getBlockState(miningPos);
        if (!state.isOf(Blocks.OBSIDIAN)) return;

        float denom = packetMine.progressBreak.getValue().floatValue();
        if (denom <= 0.0f) return;

        float progress = data.getBlockDamage() / denom;
        if (progress < 0.95f) return;

        BlockPos placePos = miningPos.up();
        if (!mc.world.getBlockState(placePos).isAir()) return;

        Box area = new Box(placePos).expand(1).offset(0, 1, 0);
        if (!mc.world.getEntitiesByClass(EndCrystalEntity.class, area, e -> true).isEmpty()) return;

        ca.doPlace(placePos);
        armCev(miningPos);
    }

    private void tickCevPost() {
        if (!cev.getValue()) {
            resetCev();
            return;
        }
        if (cevFor == null) return;
        if (System.currentTimeMillis() > cevUntilMs) {
            resetCev();
            return;
        }

        if (!mc.world.getBlockState(cevFor).isAir()) return;

        AutoCrystalFeature ca = Managers.FEATURE.getFeatureFromClass(AutoCrystalFeature.class);
        if (ca == null || !ca.isEnabled()) {
            resetCev();
            return;
        }

        BlockPos crystalPos = cevFor.up();
        if (breakLikeAutoCrystal(crystalPos)) {
            resetCev();
        }
    }

    private void armCev(BlockPos miningPos) {
        cevFor = miningPos;
        cevUntilMs = System.currentTimeMillis() + 1200L;
    }

    private boolean breakLikeAutoCrystal(BlockPos pos) {
        Box area = new Box(pos).expand(1).offset(0, 1, 0);
        List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(EndCrystalEntity.class, area, e -> true);

        if (crystals.isEmpty()) {
            if (System.currentTimeMillis() > cevUntilMs - 100) {
                cevUntilMs += 100;
            }
            return false;
        }

        EndCrystalEntity c = crystals.get(0);
        float[] rotations = RotationUtils.getRotationsTo(mc.player.getEyePos(), c.getPos());
        mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround(
                rotations[0],
                rotations[1],
                mc.player.isOnGround(),
                mc.player.horizontalCollision
        ));
        Services.ROTATION.setRotationPoint(new RotationPoint(rotations[0], rotations[1], 100, true));
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(c, mc.player.isSneaking()));
        Hand hand = mc.player.getOffHandStack().getItem() == net.minecraft.item.Items.END_CRYSTAL ? Hand.OFF_HAND : Hand.MAIN_HAND;
        mc.player.swingHand(hand);
        mc.player.resetLastAttackedTicks();
        return true;
    }

    private void resetCev() {
        cevFor = null;
        cevUntilMs = 0L;
    }

    private BlockPos findHighestPriorityBlock(PlayerEntity target) {
        List<BlockPos> occupiedPositions = getOccupiedPositions(target);
        List<BlockPos> validBurrows = occupiedPositions.stream()
                .filter(p -> !mc.world.getBlockState(p).isOf(Blocks.COBWEB))
                .collect(Collectors.toList());

        BlockPos burrow = findBest(validBurrows);
        if (burrow != null) return burrow;

        Set<BlockPos> surroundBlocks = getFeetPositions(target);
        BlockPos surround = findBest(new ArrayList<>(surroundBlocks));
        if (surround != null) return surround;

        if (aboveHead.getValue()) {
            List<BlockPos> headBlocks = occupiedPositions.stream()
                    .map(p -> p.up(2))
                    .filter(p -> !mc.world.getBlockState(p).isReplaceable())
                    .toList();
            BlockPos head = findBest(headBlocks);
            if (head != null) return head;
        }

        if (face.getValue()) {
            List<BlockPos> faceBlocks = new ArrayList<>();
            for (BlockPos feet : surroundBlocks) {
                faceBlocks.add(feet.up());
            }
            BlockPos facePos = findBest(faceBlocks);
            if (facePos != null) return facePos;
        }

        List<BlockPos> pathBlocks = getTargetMinePath(target);
        BlockPos path = findBest(pathBlocks);
        if (path != null) return path;

        return null;
    }

    private int getPriorityForPos(PlayerEntity target, BlockPos pos) {
        if (pos == null) return 99;
        if (getOccupiedPositions(target).contains(pos)) return 0;
        if (getFeetPositions(target).contains(pos)) return 1;

        for (BlockPos p : getOccupiedPositions(target)) {
            if (aboveHead.getValue() && p.up(2).equals(pos)) return 2;
        }

        if (face.getValue()) {
            for (BlockPos feet : getFeetPositions(target)) {
                if (feet.up().equals(pos)) return 3;
            }
        }
        return 4;
    }

    private BlockPos findBest(List<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            if (isInvalid(pos) || isOutOfRange(pos)) continue;
            if (doubleMine.getValue()) {
                if (packetMine.getMiningData() != null && packetMine.getPrevMiningData() != null) return null;
            } else if (packetMine.getMiningData() != null && pos.equals(packetMine.getMiningData().getPos())) {
                this.pos = pos;
                return null;
            }
            return pos;
        }
        return null;
    }

    private void handleBlockerMining(PlayerEntity target) {
        String blockerMode = blockerSequence.getValue();
        if (blockerMode.equalsIgnoreCase("None")) return;

        Set<BlockPos> surroundPositions = getFeetPositions(target);
        List<BlockPos> blockerTargets = new ArrayList<>();

        if (blockerMode.equalsIgnoreCase("Surround") || blockerMode.equalsIgnoreCase("Smart")) {
            for (BlockPos feetPos : surroundPositions) {
                BlockPos blockAbove = feetPos.up();
                if (isObbyOrBedrock(blockAbove)) {
                    surroundPositions.stream()
                            .filter(p -> p != feetPos && !isObbyOrBedrock(p.up()))
                            .findFirst().ifPresent(blockerTargets::add);
                }
            }
        }

        if (blockerTargets.isEmpty() && (blockerMode.equalsIgnoreCase("Above") || blockerMode.equalsIgnoreCase("Smart"))) {
            for (BlockPos feetPos : surroundPositions) {
                if (mc.world.getBlockState(feetPos).isReplaceable()) continue;
                BlockPos blockAbove = feetPos.up();
                if (isObbyOrBedrock(blockAbove) && !isOutOfRange(blockAbove)) {
                    blockerTargets.add(blockAbove);
                    break;
                }
            }
        }

        BlockPos finalBlocker = findBest(blockerTargets);
        if (finalBlocker != null) handle(finalBlocker, 5);
    }

    private boolean isObbyOrBedrock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isOf(Blocks.BEDROCK)) return bedrock.getValue();
        return state.isOf(Blocks.OBSIDIAN);
    }

    private List<BlockPos> getTargetMinePath(PlayerEntity target) {
        List<BlockPos> path = new ArrayList<>();
        float currentRange = range.getValue().floatValue();
        List<BlockPos> occupiedPos = getOccupiedPositions(target);

        for (BlockPos pos : occupiedPos) {
            path.addAll(pathfinder.findPathToPlace(pos, currentRange, 5).stream()
                    .filter(p -> !mc.world.getBlockState(p).isReplaceable())
                    .toList());
        }
        return path.stream().distinct().collect(Collectors.toList());
    }

    private void handle(BlockPos pos, int priority) {
        this.pos = pos;
        this.priority = priority;
        resetCev();
        Direction dir = Direction.getFacing(mc.player.getEyePos().subtract(pos.toCenterPos()));
        packetMine.startMiningPos(pos, dir);
    }

    private Target getTarget() {
        PlayerEntity optimal = null;
        double closest = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive() || Managers.FRIEND.isFriend(player.getName().getString()))
                continue;
            double dist = mc.player.squaredDistanceTo(player);
            if (dist > MathHelper.square(range.getValue().floatValue())) continue;

            if (dist < closest) {
                closest = dist;
                optimal = player;
            }
        }
        return optimal != null ? new Target(optimal, 0) : null;
    }

    private List<BlockPos> getOccupiedPositions(PlayerEntity target) {
        List<BlockPos> intersections = new ArrayList<>();
        Box box = target.getBoundingBox();
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    intersections.add(new BlockPos(x, y, z));
                }
            }
        }
        return intersections;
    }

    private Set<BlockPos> getFeetPositions(PlayerEntity target) {
        HashSet<BlockPos> positions = new HashSet<>();
        HashSet<BlockPos> blacklist = new HashSet<>();
        HitboxDesyncFeature desync = Managers.FEATURE.getFeatureFromClass(HitboxDesyncFeature.class);

        BlockPos feetPos = PositionUtils.getFlooredPosition(target);
        blacklist.add(feetPos);

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

        for (BlockPos pos : blacklist) {
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos off = pos.offset(dir);
                if (!blacklist.contains(off)) positions.add(off);
            }
        }
        return positions;
    }

    private BlockPos getAntiCrawlPos(PlayerEntity target) {
        for (BlockPos targetPos : getOccupiedPositions(target)) {
            for (BlockPos pos : new BlockPos[]{targetPos.down(), targetPos.down().north(), targetPos.down().east(), targetPos.down().south(), targetPos.down().west()}) {
                if (isObbyOrBedrock(pos) && mc.world.getBlockState(pos.up()).isReplaceable()) return pos;
            }
        }
        return null;
    }

    private boolean isValid(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isOf(Blocks.BEDROCK)) return bedrock.getValue();
        return state.isReplaceable() || state.getHardness(mc.world, pos) >= 0;
    }

    private boolean isInvalid(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isOf(Blocks.BEDROCK)) return !bedrock.getValue();
        return state.isReplaceable() || state.getHardness(mc.world, pos) < 0;
    }

    private boolean isOutOfRange(BlockPos pos) {
        return mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > MathHelper.square(range.getValue().floatValue());
    }

    private record Target(PlayerEntity player, int priority) {
    }
}