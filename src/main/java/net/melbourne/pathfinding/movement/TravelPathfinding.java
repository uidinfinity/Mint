package net.melbourne.pathfinding.movement;

import net.melbourne.Managers;
import net.melbourne.modules.impl.movement.StepFeature;
import net.melbourne.utils.Globals;
import net.minecraft.block.*;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

import java.util.*;

public class TravelPathfinding implements Globals {

    private static final double FLAT_COST = 1.0;
    private static final double DIAGONAL_COST = 1.41421356237;
    private static final double STEP_COST = 0.4;
    private static final double FALL_COST = 1.2;
    private static final double WATER_COST = 3.0;
    private static final double DOOR_COST = 1.0;

    private static final int MAX_FALL = 5;

    public static List<BlockPos> findPath(BlockPos start, Set<BlockPos> rawGoals, int maxRange) {
        World world = mc.world;
        if (world == null || rawGoals.isEmpty()) return Collections.emptyList();

        float stepLimit = getEffectiveMaxStepHeight();

        Set<BlockPos> goals = new HashSet<>();
        for (BlockPos g : rawGoals) {
            if (walkable(world, g) && space(world, g.up()) && space(world, g.up(2))) {
                goals.add(g);
            }
        }
        if (goals.isEmpty()) return Collections.emptyList();

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<BlockPos, Double> gScore = new HashMap<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        gScore.put(start, 0.0);
        open.add(new Node(start, 0.0, heuristic(start, goals)));

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (!closed.add(current.pos)) continue;

            if (goals.contains(current.pos)) {
                return optimize(reconstruct(cameFrom, current.pos), stepLimit);
            }

            if (manhattan(start, current.pos) > maxRange) continue;

            for (Neighbor n : neighbors(world, current.pos, stepLimit)) {
                if (closed.contains(n.pos)) continue;

                double tentative = current.g + n.cost;
                if (tentative < gScore.getOrDefault(n.pos, Double.MAX_VALUE)) {
                    cameFrom.put(n.pos, current.pos);
                    gScore.put(n.pos, tentative);
                    open.add(new Node(n.pos, tentative, tentative + heuristic(n.pos, goals)));
                }
            }
        }

        return Collections.emptyList();
    }

    private static double heuristic(BlockPos p, Set<BlockPos> goals) {
        double best = Double.MAX_VALUE;
        for (BlockPos g : goals) {
            double d = manhattan(p, g);
            if (d < best) best = d;
        }
        return best;
    }

    private static int manhattan(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    private static List<Neighbor> neighbors(World world, BlockPos pos, float stepLimit) {
        List<Neighbor> out = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                double base = (dx != 0 && dz != 0) ? DIAGONAL_COST : FLAT_COST;
                BlockPos target = pos.add(dx, 0, dz);

                if (!cornerClear(world, pos, target)) continue;
                if (blockedByTallWall(world, pos, target, stepLimit)) continue;

                if (walkable(world, target)) {
                    out.add(new Neighbor(target, base));
                    continue;
                }

                if (water(world, target)) {
                    out.add(new Neighbor(target, base * WATER_COST));
                }

                if (door(world, target)) {
                    out.add(new Neighbor(target, base + DOOR_COST));
                }

                for (int dy = 1; dy <= stepLimit; dy++) {
                    BlockPos up = pos.add(dx, dy, dz);
                    if (stepable(world, pos, up, dy)) {
                        out.add(new Neighbor(up, base + STEP_COST));
                        break;
                    }
                }

                for (int dy = 1; dy <= MAX_FALL; dy++) {
                    BlockPos down = pos.add(dx, -dy, dz);
                    if (fallable(world, pos, down)) {
                        out.add(new Neighbor(down, base + FALL_COST * dy));
                        break;
                    }
                }
            }
        }

        return out;
    }

    private static boolean blockedByTallWall(World world, BlockPos from, BlockPos to, float stepLimit) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        BlockPos base = from.add(dx, 0, dz);
        for (int y = 1; y <= stepLimit + 1; y++) {
            if (!space(world, base.up(y))) return true;
        }
        return false;
    }

    private static boolean walkable(World world, BlockPos pos) {
        BlockState floor = world.getBlockState(pos.down());
        boolean solid = floor.isSolidBlock(world, pos.down())
                || floor.getBlock() instanceof CarpetBlock
                || floor.getBlock() instanceof SlabBlock && floor.get(SlabBlock.TYPE) == SlabType.BOTTOM;
        return solid && space(world, pos) && space(world, pos.up());
    }

    private static boolean stepable(World world, BlockPos from, BlockPos to, int dy) {
        for (int i = 1; i <= dy; i++) if (!space(world, from.up(i))) return false;
        return walkable(world, to);
    }

    private static boolean fallable(World world, BlockPos from, BlockPos to) {
        if (!walkable(world, to)) return false;
        for (int y = to.getY() + 1; y < from.getY(); y++) {
            if (!space(world, new BlockPos(to.getX(), y, to.getZ()))) return false;
        }
        return true;
    }

    private static boolean cornerClear(World world, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (dx == 0 || dz == 0) return true;
        return space(world, from.add(dx, 0, 0)) && space(world, from.add(0, 0, dz));
    }

    private static boolean water(World world, BlockPos pos) {
        return world.getBlockState(pos).getFluidState().isIn(FluidTags.WATER) && space(world, pos) && space(world, pos.up());
    }

    private static boolean door(World world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        if (s.getBlock() instanceof DoorBlock) return !s.get(DoorBlock.OPEN);
        if (s.getBlock() instanceof FenceGateBlock) return !s.get(FenceGateBlock.OPEN);
        return false;
    }

    private static boolean space(World world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        if (s.isAir()) return true;
        if (s.getFluidState().isIn(FluidTags.LAVA)) return false;
        VoxelShape shape = s.getCollisionShape(world, pos);
        if (shape.isEmpty()) return true;
        VoxelShape player = VoxelShapes.cuboid(new Box(0.2, 0, 0.2, 0.8, 1.8, 0.8));
        return VoxelShapes.combine(shape, player, BooleanBiFunction.AND).isEmpty();
    }

    private static List<BlockPos> reconstruct(Map<BlockPos, BlockPos> cameFrom, BlockPos end) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos cur = end;
        while (cur != null) {
            path.add(cur);
            cur = cameFrom.get(cur);
        }
        Collections.reverse(path);
        return path;
    }

    private static List<BlockPos> optimize(List<BlockPos> path, float stepLimit) {
        if (path.size() < 3) return path;
        List<BlockPos> out = new ArrayList<>();
        out.add(path.get(0));
        int i = 0;
        while (i < path.size() - 1) {
            int best = i + 1;
            for (int j = path.size() - 1; j > i + 1; j--) {
                if (direct(mc.world, path.get(i), path.get(j), stepLimit)) {
                    best = j;
                    break;
                }
            }
            out.add(path.get(best));
            i = best;
        }
        return out;
    }

    private static boolean direct(World world, BlockPos a, BlockPos b, float stepLimit) {
        if (b.getY() - a.getY() > stepLimit) return false;
        int dx = b.getX() - a.getX();
        int dy = b.getY() - a.getY();
        int dz = b.getZ() - a.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            BlockPos p = new BlockPos(
                    (int) Math.round(a.getX() + dx * t),
                    (int) Math.round(a.getY() + dy * t),
                    (int) Math.round(a.getZ() + dz * t)
            );
            if (!walkable(world, p)) return false;
        }
        return true;
    }

    private static float getEffectiveMaxStepHeight() {
        StepFeature f = Managers.FEATURE.getFeatureFromClass(StepFeature.class);
        return f != null && f.isEnabled() ? Math.max(0.0f, f.getHeight()) : 0.6f;
    }

    private static class Node {
        BlockPos pos;
        double g;
        double f;

        Node(BlockPos pos, double g, double f) {
            this.pos = pos;
            this.g = g;
            this.f = f;
        }
    }

    private static class Neighbor {
        BlockPos pos;
        double cost;

        Neighbor(BlockPos pos, double cost) {
            this.pos = pos;
            this.cost = cost;
        }
    }
}
