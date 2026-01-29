package net.melbourne.utils.block.hole;

import net.melbourne.Managers;
import net.melbourne.modules.impl.client.EngineFeature;
import net.melbourne.utils.Globals;
import net.melbourne.utils.entity.player.movement.position.PositionUtils;
import net.melbourne.utils.world.WorldUtils;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class HoleUtils implements Globals {

    private static final Vec3i[] holeOffsets = new Vec3i[]{new Vec3i(0, -1, 0), new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1)};
    private static final Vec3i[] fullTrapOffsets = new Vec3i[]{new Vec3i(1, 1, 0), new Vec3i(0, 1, 1), new Vec3i(-1, 1, 0), new Vec3i(0, 1, -1), new Vec3i(1, 2, 0), new Vec3i(0, 2, 0)};
    private static final Vec3i[] singleOffsets = {new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(0, 0, 1), new Vec3i(0, -1, 0)};
    private static final Vec3i[] doubleXOffsets = {new Vec3i(-1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(0, 0, 1), new Vec3i(0, -1, 0), new Vec3i(2, 0, 0), new Vec3i(1, 0, -1), new Vec3i(1, 0, 1), new Vec3i(1, -1, 0)};
    private static final Vec3i[] doubleZOffsets = {new Vec3i(0, 0, -1), new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0), new Vec3i(0, -1, 0), new Vec3i(0, 0, 2), new Vec3i(-1, 0, 1), new Vec3i(1, 0, 1), new Vec3i(0, -1, 1)};
    private static final Vec3i[] quadOffsets = {new Vec3i(-1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(0, -1, 0), new Vec3i(2, 0, 0), new Vec3i(1, 0, -1), new Vec3i(1, -1, 0), new Vec3i(-1, 0, 1), new Vec3i(0, 0, 2), new Vec3i(0, -1, 1), new Vec3i(2, 0, 1), new Vec3i(1, 0, 2), new Vec3i(1, -1, 1)};
    private static final Vec3i[] incompleteHoleOffsets = {new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1), new Vec3i(1, -1, 0), new Vec3i(-1, -1, 0), new Vec3i(0, -1, 1), new Vec3i(0, -1, -1)};

    public static boolean isPlayerInHole(PlayerEntity player) {
        if (mc.world == null || player == null) return false;
        return getFeetPositions(player, true, true, false).stream().noneMatch(pos -> mc.world.getBlockState(pos).isReplaceable());
    }

    public static boolean isIncompleteHole(BlockPos pos) {
        if (mc.world == null || pos == null) return false;

        if (!mc.world.getBlockState(pos).isAir()) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;
        if (!mc.world.getBlockState(pos.up(2)).isAir()) return false;

        int missing = 0;
        BlockPos missingPos = null;

        for (Vec3i off : incompleteHoleOffsets) {
            BlockPos p = pos.add(off);
            if (!isSafeBlock(p)) {
                if (off.getY() == -1) return false;
                missing++;
                missingPos = p;
                if (missing > 1) return false;
            }
        }

        if (missing == 0 || missingPos == null) return false;
        if (mc.world.getBlockState(missingPos.down()).isReplaceable()) return false;

        if (mc.world.getBlockState(missingPos).isAir() && mc.world.getBlockState(missingPos.up()).isAir()) {
            int adjSafe = 0;
            Vec3i[] checkOffs = {new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1), new Vec3i(0, -1, 0)};
            for (Vec3i a : checkOffs) {
                if (isSafeBlock(missingPos.add(a))) adjSafe++;
            }
            if (adjSafe >= 4) return false;
        }

        EngineFeature engine = Managers.FEATURE.getFeatureFromClass(EngineFeature.class);
        if (engine != null && "Above".equals(engine.incompleteMode.getValue())) {
            if (mc.world.getBlockState(missingPos.up()).isAir()) return false;
        }

        Vec3i[] adj = {new Vec3i(0, -1, 0), new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1)};
        for (Vec3i a : adj) {
            if (!mc.world.getBlockState(missingPos.add(a)).isAir()) return true;
        }

        return false;
    }

    public static boolean isHole(BlockPos pos) {
        if (mc.world == null || pos == null) return false;
        if (!mc.world.getBlockState(pos).isAir()) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;
        if (!mc.world.getBlockState(pos.up(2)).isAir()) return false;

        for (Vec3i off : incompleteHoleOffsets) {
            if (!isSafeBlock(pos.add(off))) return false;
        }
        return true;
    }

    public static List<BlockPos> getBlocksToCompleteHole(BlockPos holePos) {
        List<BlockPos> list = new ArrayList<>();
        if (mc.world == null || holePos == null) return list;

        for (Vec3i off : incompleteHoleOffsets) {
            BlockPos p = holePos.add(off);
            if (mc.world.getBlockState(p).isReplaceable()) list.add(p);
        }
        return list;
    }

    private static boolean isSafeBlock(BlockPos pos) {
        if (mc.world == null || pos == null) return false;
        var b = mc.world.getBlockState(pos).getBlock();
        return b == Blocks.BEDROCK || b == Blocks.OBSIDIAN || b == Blocks.ENDER_CHEST || b == Blocks.RESPAWN_ANCHOR;
    }

    public static Hole getIncompleteHole(BlockPos pos, double height) {
        if (!isIncompleteHole(pos)) return null;
        return new Hole(new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1), HoleType.INCOMPLETE, HoleSafety.OBSIDIAN);
    }



    public static List<Hole> getHoles(PlayerEntity player, float range, boolean singles, boolean doubles, boolean quads, boolean ignoreOwn, boolean requireReachable) {
        BlockPos center = PositionUtils.getFlooredPosition(player);
        List<Hole> holes = new ArrayList<>();
        if (mc.world == null || player == null) return holes;

        int r = (int) Math.ceil(range);
        for (int x = center.getX() - r; x <= center.getX() + r; x++) {
            for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                BlockPos pos = new BlockPos(x, center.getY(), z);
                if (mc.player != null && mc.player.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)) > range) continue;

                if (isIncompleteHole(pos)) {
                    Hole inc = getIncompleteHole(pos, player.getHeight());
                    if (inc != null) holes.add(inc);
                    continue;
                }

                Hole quad = quads ? getQuadHole(pos, player.getHeight()) : null;
                if (quad != null) { holes.add(quad); continue; }

                Hole dbl = doubles ? getDoubleHole(pos, player.getHeight()) : null;
                if (dbl != null) { holes.add(dbl); continue; }

                Hole single = singles ? getSingleHole(pos, player.getHeight(), requireReachable) : null;
                if (single != null) holes.add(single);
            }
        }

        if (ignoreOwn) holes.removeIf(h -> player.getBoundingBox().intersects(h.box()));
        return holes;
    }

    public static Hole getHole(BlockPos pos, boolean reachable) {
        Hole quad = getQuadHole(pos, 1.0);
        if (quad != null) return quad;

        Hole dbl = getDoubleHole(pos, 1.0);
        if (dbl != null) return dbl;

        return getSingleHole(pos, 1.0, reachable);
    }

    public static Hole getDoubleHoleForPlayer(PlayerEntity player) {
        BlockPos pos = PositionUtils.getFlooredPosition(player);
        Hole dbl = getDoubleHole(pos, player.getHeight());
        if (dbl != null && dbl.type() == HoleType.DOUBLE) {
            if (player.getBoundingBox().intersects(dbl.box())) return dbl;
        }

        BlockPos posNegX = pos.add(-1, 0, 0);
        Hole dblNegX = getDoubleHole(posNegX, player.getHeight());
        if (dblNegX != null && dblNegX.type() == HoleType.DOUBLE && dblNegX.box().getLengthX() > 1) {
            if (player.getBoundingBox().intersects(dblNegX.box())) return dblNegX;
        }

        BlockPos posNegZ = pos.add(0, 0, -1);
        Hole dblNegZ = getDoubleHole(posNegZ, player.getHeight());
        if (dblNegZ != null && dblNegZ.type() == HoleType.DOUBLE && dblNegZ.box().getLengthZ() > 1) {
            if (player.getBoundingBox().intersects(dblNegZ.box())) return dblNegZ;
        }

        return null;
    }

    public static Hole getQuadHoleForPlayer(PlayerEntity player) {
        BlockPos pos = PositionUtils.getFlooredPosition(player);

        BlockPos[] potentialStartPositions = {
                pos,
                pos.add(-1, 0, 0),
                pos.add(0, 0, -1),
                pos.add(-1, 0, -1)
        };

        for (BlockPos startPos : potentialStartPositions) {
            Hole quad = getQuadHole(startPos, player.getHeight());
            if (quad != null && quad.type() == HoleType.QUAD) {
                if (player.getBoundingBox().intersects(quad.box())) return quad;
            }
        }

        return null;
    }

    public static BlockPos getFirst(Hole h) {
        return new BlockPos(MathHelper.floor(h.box().minX), MathHelper.floor(h.box().minY), MathHelper.floor(h.box().minZ));
    }

    public static BlockPos getSecond(Hole h) {
        if (h.type() == HoleType.SINGLE || h.type() == HoleType.QUAD || h.type() == HoleType.INCOMPLETE) return null;
        return h.type() == HoleType.DOUBLE && h.box().getLengthX() > 1
                ? new BlockPos(MathHelper.floor(h.box().maxX - 1), MathHelper.floor(h.box().minY), MathHelper.floor(h.box().minZ))
                : new BlockPos(MathHelper.floor(h.box().minX), MathHelper.floor(h.box().minY), MathHelper.floor(h.box().maxZ - 1));
    }

    public static List<BlockPos> getInsidePositions(Entity targetEntity) {
        List<BlockPos> list = new ArrayList<>();
        if (mc.world == null || targetEntity == null) return list;

        BlockPos pos = PositionUtils.getFlooredPosition(targetEntity);
        for (Vec3i v : holeOffsets) {
            if (v.getY() >= pos.getY()) continue;
            BlockPos off = pos.add(v);
            List<Entity> ents = mc.world.getOtherEntities(null, new Box(off)).stream().filter(e -> e == targetEntity).toList();
            if (ents.isEmpty()) continue;
            Box b = ents.getFirst().getBoundingBox();
            for (int x = (int) Math.floor(b.minX); x < Math.ceil(b.maxX); x++) {
                for (int z = (int) Math.floor(b.minZ); z < Math.ceil(b.maxZ); z++) {
                    BlockPos p = new BlockPos(x, pos.getY(), z);
                    if (!list.contains(p)) list.add(p);
                }
            }
        }
        if (list.isEmpty()) list.add(pos);
        return list;
    }

    public static HashSet<BlockPos> getFeetPositions(PlayerEntity target, boolean extension, boolean floor, boolean targetOnly) {
        HashSet<BlockPos> positions = new HashSet<>();
        HashSet<BlockPos> blacklist = new HashSet<>();
        if (mc.world == null || target == null) return positions;

        BlockPos feet = PositionUtils.getFlooredPosition(target);
        blacklist.add(feet);

        if (extension) {
            for (Direction d : Direction.values()) {
                if (d.getAxis().isVertical()) continue;
                BlockPos off = feet.offset(d);
                List<PlayerEntity> cols = WorldUtils.getCollisions(off);
                if (cols.isEmpty()) continue;
                for (PlayerEntity p : cols) {
                    if (mc.player != null && p == mc.player) continue;
                    if (targetOnly && p != target) continue;
                    Box b = p.getBoundingBox();
                    for (int x = (int) Math.floor(b.minX); x < Math.ceil(b.maxX); x++) {
                        for (int z = (int) Math.floor(b.minZ); z < Math.ceil(b.maxZ); z++) {
                            blacklist.add(new BlockPos(x, feet.getY(), z));
                        }
                    }
                }
            }
        }

        for (BlockPos p : blacklist) {
            if (floor) positions.add(p.down());
            for (Direction d : Direction.values()) {
                if (!d.getAxis().isHorizontal()) continue;
                BlockPos off = p.offset(d);
                if (!blacklist.contains(off)) positions.add(off);
            }
        }

        return positions;
    }

    public static List<BlockPos> getTrapPositions(PlayerEntity player, boolean partial, boolean head, boolean antiStep, boolean antiBomb, boolean strictDirection) {
        List<BlockPos> list = new ArrayList<>();
        if (mc.world == null || player == null) return list;

        BlockPos pos = PositionUtils.getFlooredPosition(player);

        if (antiStep) {
            list.add(pos.add(1, 2, 0));
            list.add(pos.add(-1, 2, 0));
            list.add(pos.add(0, 2, 1));
            list.add(pos.add(0, 2, -1));
        }
        if (antiBomb) list.add(pos.add(0, 3, 0));

        if (partial) {
            BlockPos headPos = pos.add(0, 2, 0);
            if (WorldUtils.getDirection(headPos, strictDirection) != null) {
                list.add(headPos);
                return list;
            }
            Vec3i[] offs = {new Vec3i(1, 1, 0), new Vec3i(1, 2, 0), new Vec3i(0, 2, 0)};
            for (Vec3i v : offs) list.add(pos.add(v));
        } else {
            for (Vec3i v : fullTrapOffsets) {
                if (!head && v.getY() == 2) continue;
                list.add(pos.add(v));
            }
        }

        return list;
    }

    public static Hole getSingleHole(BlockPos pos, double height) {
        return getSingleHole(pos, height, true);
    }

    public static Hole getSingleHole(BlockPos pos, double height, boolean reachable) {
        if (mc.world == null || pos == null) return null;

        if (!mc.world.getBlockState(pos).isAir()) return null;
        if (reachable && !mc.world.getBlockState(pos.up()).isAir()) return null;
        if (reachable && !mc.world.getBlockState(pos.up(2)).isAir()) return null;

        HoleSafety safety = null;

        for (Vec3i off : singleOffsets) {
            var b = mc.world.getBlockState(pos.add(off)).getBlock();
            if (!(b == Blocks.BEDROCK || b == Blocks.OBSIDIAN || b == Blocks.RESPAWN_ANCHOR || b == Blocks.ENDER_CHEST)) return null;

            if (b == Blocks.BEDROCK) safety = (safety == HoleSafety.OBSIDIAN) ? HoleSafety.MIXED : (safety != HoleSafety.MIXED ? HoleSafety.BEDROCK : safety);
            if (b == Blocks.OBSIDIAN || b == Blocks.RESPAWN_ANCHOR || b == Blocks.ENDER_CHEST) safety = (safety == HoleSafety.BEDROCK) ? HoleSafety.MIXED : (safety != HoleSafety.MIXED ? HoleSafety.OBSIDIAN : safety);
        }

        if (safety == null) safety = HoleSafety.OBSIDIAN;

        return new Hole(new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1), HoleType.SINGLE, safety);
    }

    public static Hole getDoubleHole(BlockPos pos, double height) {
        if (mc.world == null || pos == null) return null;

        if (!mc.world.getBlockState(pos).isAir()) return null;
        if (!mc.world.getBlockState(pos.up()).isAir()) return null;
        if (!mc.world.getBlockState(pos.up(2)).isAir()) return null;

        boolean x = mc.world.getBlockState(pos.add(1, 0, 0)).isAir() && mc.world.getBlockState(pos.add(1, 0, 0).up()).isAir() && mc.world.getBlockState(pos.add(1, 0, 0).up(2)).isAir();
        boolean z = mc.world.getBlockState(pos.add(0, 0, 1)).isAir() && mc.world.getBlockState(pos.add(0, 0, 1).up()).isAir() && mc.world.getBlockState(pos.add(0, 0, 1).up(2)).isAir();
        if (!x && !z) return null;

        Box box = null;
        HoleSafety safety = null;

        if (x) {
            boolean valid = true;
            for (Vec3i off : doubleXOffsets) {
                var b = mc.world.getBlockState(pos.add(off)).getBlock();
                if (!(b == Blocks.BEDROCK || b == Blocks.OBSIDIAN || b == Blocks.RESPAWN_ANCHOR || b == Blocks.ENDER_CHEST)) { valid = false; break; }

                if (b == Blocks.BEDROCK) safety = (safety == HoleSafety.OBSIDIAN) ? HoleSafety.MIXED : (safety != HoleSafety.MIXED ? HoleSafety.BEDROCK : safety);
                if (b == Blocks.OBSIDIAN || b == Blocks.RESPAWN_ANCHOR || b == Blocks.ENDER_CHEST) safety = (safety == HoleSafety.BEDROCK) ? HoleSafety.MIXED : (safety != HoleSafety.MIXED ? HoleSafety.OBSIDIAN : safety);
            }
            if (valid) box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 2, pos.getY() + height, pos.getZ() + 1);
        }

        if (z && box == null) {
            boolean valid = true;
            for (Vec3i off : doubleZOffsets) {
                var b = mc.world.getBlockState(pos.add(off)).getBlock();
                if (!(b == Blocks.BEDROCK || b == Blocks.OBSIDIAN || b == Blocks.RESPAWN_ANCHOR || b == Blocks.ENDER_CHEST)) { valid = false; break; }

                if (b == Blocks.BEDROCK) safety = (safety == HoleSafety.OBSIDIAN) ? HoleSafety.MIXED : (safety != HoleSafety.MIXED ? HoleSafety.BEDROCK : safety);
                if (b == Blocks.OBSIDIAN || b == Blocks.RESPAWN_ANCHOR || b == Blocks.ENDER_CHEST) safety = (safety == HoleSafety.BEDROCK) ? HoleSafety.MIXED : (safety != HoleSafety.MIXED ? HoleSafety.OBSIDIAN : safety);
            }
            if (valid) box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 2);
        }

        if (box == null) return null;
        if (safety == null) safety = HoleSafety.OBSIDIAN;

        return new Hole(box, HoleType.DOUBLE, safety);
    }



    public static Hole getQuadHole(BlockPos pos, double height) {
        if (mc.world == null || pos == null) return null;

        if (!mc.world.getBlockState(pos).isAir()) return null;
        if (!mc.world.getBlockState(pos.up()).isAir()) return null;
        if (!mc.world.getBlockState(pos.add(1, 0, 0)).isAir()) return null;
        if (!mc.world.getBlockState(pos.add(0, 0, 1)).isAir()) return null;
        if (!mc.world.getBlockState(pos.add(1, 0, 1)).isAir()) return null;

        if (!mc.world.getBlockState(pos.up(2)).isAir()) return null;
        if (!mc.world.getBlockState(pos.add(1, 0, 0).up()).isAir()) return null;
        if (!mc.world.getBlockState(pos.add(1, 0, 0).up(2)).isAir()) return null;
        if (!mc.world.getBlockState(pos.add(0, 0, 1).up()).isAir()) return null;
        if (!mc.world.getBlockState(pos.add(0, 0, 1).up(2)).isAir()) return null;
        if (!mc.world.getBlockState(pos.add(1, 0, 1).up()).isAir()) return null;
        if (!mc.world.getBlockState(pos.add(1, 0, 1).up(2)).isAir()) return null;

        HoleSafety safety = null;

        for (Vec3i off : quadOffsets) {
            var b = mc.world.getBlockState(pos.add(off)).getBlock();
            if (!(b == Blocks.BEDROCK || b == Blocks.OBSIDIAN || b == Blocks.RESPAWN_ANCHOR || b == Blocks.ENDER_CHEST)) return null;

            if (b == Blocks.BEDROCK) safety = (safety == HoleSafety.OBSIDIAN) ? HoleSafety.MIXED : (safety != HoleSafety.MIXED ? HoleSafety.BEDROCK : safety);
            if (b == Blocks.OBSIDIAN || b == Blocks.RESPAWN_ANCHOR || b == Blocks.ENDER_CHEST) safety = (safety == HoleSafety.BEDROCK) ? HoleSafety.MIXED : (safety != HoleSafety.MIXED ? HoleSafety.OBSIDIAN : safety);
        }

        if (safety == null) safety = HoleSafety.OBSIDIAN;

        return new Hole(new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 2, pos.getY() + height, pos.getZ() + 2), HoleType.QUAD, safety);
    }



    public record Hole(Box box, HoleType type, HoleSafety safety) {}
    public enum HoleType { SINGLE, DOUBLE, QUAD, INCOMPLETE }
    public enum HoleSafety { OBSIDIAN, MIXED, BEDROCK }
}
