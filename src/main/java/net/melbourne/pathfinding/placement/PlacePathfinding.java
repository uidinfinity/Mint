package net.melbourne.pathfinding.placement;

import it.unimi.dsi.fastutil.longs.Long2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.melbourne.pathfinding.PathNode;
import net.melbourne.utils.Globals;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.MinecraftClient;

import java.util.*;

public class PlacePathfinding implements Globals {

    private static final Direction[] DIRECTIONS = Direction.values();

    public boolean canPlace(BlockPos pos, float range) {
        return !(mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > MathHelper.square(range)) && mc.world.getBlockState(pos).isReplaceable();
    }

    public List<BlockPos> findPathToPlace(BlockPos target, float range, int maxBlocks) {
        if (!mc.world.getBlockState(target).isReplaceable()) return Collections.emptyList();

        BlockPos playerPos = mc.player.getBlockPos();
        int searchRadius = (int) Math.ceil(range);

        List<BlockPos> validCandidates = new ArrayList<>();

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos candidate = playerPos.add(x, y, z);

                    if (!canPlace(candidate, range)) continue;
                    validCandidates.add(candidate);
                }
            }
        }

        if (validCandidates.isEmpty()) {
            return Collections.emptyList();
        }

        validCandidates.sort(Comparator.comparingDouble(c -> c.getSquaredDistance(target)));

        int limit = Math.min(3, validCandidates.size());
        List<BlockPos> bestPath = Collections.emptyList();
        double bestLength = Double.MAX_VALUE;

        for (int i = 0; i < limit; i++) {
            BlockPos start = validCandidates.get(i);
            List<BlockPos> path = compute(start, target, range, maxBlocks);
            if (!path.isEmpty() && path.size() < bestLength) {
                bestLength = path.size();
                bestPath = path;
            }
        }

        return bestPath;
    }

    public List<BlockPos> compute(BlockPos start, BlockPos end, float range, int maxBlocks) {
        if (start.equals(end)) {
            return Collections.singletonList(start);
        }

        PriorityQueue<PathNode> openSet = new PriorityQueue<>(1000, Comparator.comparingDouble(PathNode::getF));
        Long2ObjectLinkedOpenHashMap<PathNode> allNodes = new Long2ObjectLinkedOpenHashMap<>();
        Long2BooleanLinkedOpenHashMap checked = new Long2BooleanLinkedOpenHashMap();


        PathNode startNode = new PathNode(start, null, 0.0, heuristic(start, end));
        openSet.add(startNode);
        allNodes.put(start.asLong(), startNode);

        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();

            if (current.pos.equals(end)) {
                return reconstructPath(current);
            }

            if (current.g >= maxBlocks) {
                continue;
            }

            for (Direction dir : DIRECTIONS) {
                BlockPos neighborPos = current.pos.offset(dir);
                long neighborPosLong = neighborPos.asLong();

                if (!checked.computeIfAbsent(neighborPosLong, l -> canPlace(neighborPos, range))) {
                    continue;
                }

                double tentativeG = current.g + 1.0;

                if (tentativeG > maxBlocks) {
                    continue;
                }

                PathNode existing = allNodes.get(neighborPosLong);
                if (existing != null && tentativeG >= existing.g) {
                    continue;
                }

                double h = heuristic(neighborPos, end);

                PathNode neighborNode = new PathNode(neighborPos, current, tentativeG, h);
                allNodes.put(neighborPosLong, neighborNode);
                openSet.add(neighborNode);
            }
        }

        return Collections.emptyList();
    }

    private double heuristic(BlockPos a, BlockPos b) {
        return a.getSquaredDistance(b);
    }

    private List<BlockPos> reconstructPath(PathNode node) {
        List<BlockPos> path = new LinkedList<>();
        while (node != null) {
            path.addFirst(node.pos);
            node = node.parent;
        }
        return path;
    }
}