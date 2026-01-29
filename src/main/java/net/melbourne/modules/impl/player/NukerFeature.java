package net.melbourne.modules.impl.player;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.misc.PacketMineFeature;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.settings.types.WhitelistSetting;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FeatureInfo(name = "Nuker", category = Category.Player)
public class NukerFeature extends Feature {

    public NumberSetting range = new NumberSetting("Range", "The range to search and mine blocks.", 5.0f, 1.0f, 6.0f);
    public WhitelistSetting whitelist = new WhitelistSetting("Blocks", "Blocks to mine.", WhitelistSetting.Type.BLOCKS);
    public ModeSetting priority = new ModeSetting("Priority", "Mines blocks based on distance.", "Closest", new String[]{"Closest", "Furthest"});

    private PacketMineFeature packetMine;
    private long lastTime = 0;
    private int blocksMined = 0;
    private int blocksMinedLastSecond = 0;
    private BlockPos currentlyMining = null;

    @Override
    public void onEnable() {
        packetMine = Managers.FEATURE.getFeatureFromClass(PacketMineFeature.class);
        lastTime = System.currentTimeMillis();
        blocksMined = 0;
        blocksMinedLastSecond = 0;
        currentlyMining = null;
    }

    @Override
    public String getInfo() {
        return String.valueOf(blocksMinedLastSecond);
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull() || mc.player.isCreative() || mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) return;
        if (packetMine == null || packetMine.isFeaturePaused()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime >= 1000) {
            blocksMinedLastSecond = blocksMined;
            blocksMined = 0;
            lastTime = currentTime;
        }

        if (currentlyMining != null && packetMine.getMiningData() == null && !packetMine.isMining(currentlyMining)) {
            if (mc.world.getBlockState(currentlyMining).isReplaceable()) {
                blocksMined++;
            }
            currentlyMining = null;
        }

        List<BlockPos> potentialBlocks = getBlocksInNukerRange();
        BlockPos targetPos = findNextTargetBlock(potentialBlocks);

        if (targetPos != null) {
            if (packetMine.getMiningData() == null || !targetPos.equals(packetMine.getMiningData().getPos())) {
                handleMine(targetPos);
            }
        }

        if (packetMine.getMiningData() != null) {
            currentlyMining = packetMine.getMiningData().getPos();
        }
    }

    private void handleMine(BlockPos pos) {
        if (packetMine == null) return;
        Direction dir = Direction.getFacing(mc.player.getEyePos().subtract(Vec3d.ofCenter(pos)));
        packetMine.startMiningPos(pos, dir);
    }

    private List<BlockPos> getBlocksInNukerRange() {
        float currentRange = range.getValue().floatValue();
        int rangeInt = MathHelper.ceil(currentRange);
        BlockPos playerPos = mc.player.getBlockPos();
        Box searchBox = new Box(playerPos).expand(rangeInt, rangeInt, rangeInt);
        Stream<BlockPos> posStream = BlockPos.stream(searchBox).map(BlockPos::toImmutable);

        return posStream
                .filter(pos -> !isOutOfRange(pos) && isBlockMineable(pos))
                .collect(Collectors.toList());
    }

    private BlockPos findNextTargetBlock(List<BlockPos> potentialBlocks) {
        if (potentialBlocks.isEmpty()) return null;
        Stream<BlockPos> targetStream = potentialBlocks.stream();
        String priorityMode = priority.getValue();
        Comparator<BlockPos> distanceComparator = Comparator.comparingDouble(pos ->
                mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())
        );

        if (priorityMode.equalsIgnoreCase("Closest")) {
            targetStream = targetStream.sorted(distanceComparator);
        } else if (priorityMode.equalsIgnoreCase("Furthest")) {
            targetStream = targetStream.sorted(distanceComparator.reversed());
        }

        return targetStream.findFirst().orElse(null);
    }

    private boolean isOutOfRange(BlockPos pos) {
        float currentRange = range.getValue().floatValue();
        return mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > MathHelper.square(currentRange);
    }

    private boolean isBlockMineable(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isReplaceable()) return false;
        if (!whitelist.getWhitelist().isEmpty() && !whitelist.isWhitelistContains(state.getBlock())) return false;
        float hardness = state.getHardness(mc.world, pos);
        return hardness >= 0;
    }
}