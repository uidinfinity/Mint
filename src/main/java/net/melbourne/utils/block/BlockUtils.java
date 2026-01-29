package net.melbourne.utils.block;

import com.google.common.collect.Sets;
import net.melbourne.utils.Globals;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldAccess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockUtils implements Globals {

    public static class Offsets {
        public static final BlockPos[] FACE_PLACE = new BlockPos[]{
                new BlockPos(1, 1, 0),
                new BlockPos(-1, 1, 0),
                new BlockPos(0, 1, 1),
                new BlockPos(0, 1, -1),
        };

        public static final BlockPos[] FEET_PLACE = new BlockPos[]{
                new BlockPos(1, 0, 0),
                new BlockPos(-1, 0, 0),
                new BlockPos(0, 0, 1),
                new BlockPos(0, 0, -1),
        };
    }

    public static Set<Block> RIGHT_CLICKABLE_BLOCKS = Sets.newHashSet(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX, Blocks.ANVIL, Blocks.BELL, Blocks.OAK_BUTTON, Blocks.ACACIA_BUTTON, Blocks.BIRCH_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.JUNGLE_BUTTON, Blocks.SPRUCE_BUTTON, Blocks.STONE_BUTTON, Blocks.COMPARATOR, Blocks.REPEATER, Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.BIRCH_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE, Blocks.ACACIA_FENCE_GATE, Blocks.BREWING_STAND, Blocks.DISPENSER, Blocks.DROPPER, Blocks.LEVER, Blocks.NOTE_BLOCK, Blocks.JUKEBOX, Blocks.BEACON, Blocks.BLACK_BED, Blocks.BLUE_BED, Blocks.BROWN_BED, Blocks.CYAN_BED, Blocks.GRAY_BED, Blocks.GREEN_BED, Blocks.LIGHT_BLUE_BED, Blocks.LIGHT_GRAY_BED, Blocks.LIME_BED, Blocks.MAGENTA_BED, Blocks.ORANGE_BED, Blocks.PINK_BED, Blocks.PURPLE_BED, Blocks.RED_BED, Blocks.WHITE_BED, Blocks.YELLOW_BED, Blocks.FURNACE, Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR, Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.CAKE, Blocks.ENCHANTING_TABLE, Blocks.DRAGON_EGG, Blocks.HOPPER, Blocks.REPEATING_COMMAND_BLOCK, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.CRAFTING_TABLE, Blocks.ACACIA_TRAPDOOR, Blocks.BIRCH_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.JUNGLE_TRAPDOOR, Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.CAKE, Blocks.ACACIA_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.BIRCH_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.DARK_OAK_SIGN, Blocks.DARK_OAK_WALL_SIGN, Blocks.JUNGLE_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.OAK_SIGN, Blocks.OAK_WALL_SIGN, Blocks.SPRUCE_SIGN, Blocks.SPRUCE_WALL_SIGN, Blocks.CRIMSON_SIGN, Blocks.CRIMSON_WALL_SIGN, Blocks.WARPED_SIGN, Blocks.WARPED_WALL_SIGN, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.CARTOGRAPHY_TABLE, Blocks.GRINDSTONE, Blocks.LECTERN, Blocks.LOOM, Blocks.STONECUTTER, Blocks.SMITHING_TABLE);

    public static Direction getFacing(BlockPos pos, WorldAccess provider) {
        for (Direction facing : Direction.values()) {
            if (!provider.getBlockState(pos.offset(facing)).isReplaceable()) {
                return facing;
            }
        }
        return null;
    }

    public static double getDistanceSq(BlockPos pos) {
        return getDistanceSq(mc.player.getBlockPos(), pos);
    }

    public static double getDistanceSq(BlockPos from, BlockPos to) {
        return from.getSquaredDistance(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5);
    }

    public static boolean canBreak(BlockPos pos) {
        return canBreak(mc.world.getBlockState(pos), pos);
    }

    public static boolean canBreak(BlockState state, BlockPos pos) {
        return state.getHardness(mc.world, pos) != -1.0f && state.getBlock() != Blocks.AIR && !state.isLiquid();
    }

    public static boolean placeBlock(final BlockPos pos, final boolean strictDirection, final boolean clientSwing) {
        Direction direction = getInteractDirection(pos, strictDirection);
        if (direction == null) {
            return false;
        }
        final BlockPos neighbor = pos.offset(direction.getOpposite());
        return placeBlock(neighbor, direction, clientSwing);
    }

    public static boolean placeBlock(final BlockPos pos, final Direction direction, final boolean clientSwing) {
        Vec3d hitVec = pos.toCenterPos().add(new Vec3d(direction.getVector()).multiply(0.5));
        return placeBlock(new BlockHitResult(hitVec, direction, pos, false), clientSwing);
    }

    public static boolean placeBlock(final BlockHitResult hitResult, final boolean clientSwing) {
        return placeBlockImmediately(hitResult, clientSwing);
    }

    public static boolean placeBlockImmediately(final BlockHitResult result, final boolean clientSwing) {
        final ActionResult actionResult = placeBlockInternally(result);
        if (mc.getNetworkHandler() == null) return false;
        if (actionResult.isAccepted()) {
            if (clientSwing) {
                mc.player.swingHand(Hand.MAIN_HAND);
            } else {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }
        return actionResult.isAccepted();
    }

    private static ActionResult placeBlockInternally(final BlockHitResult hitResult) {
        return mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
    }

    public static Direction getInteractDirection(final BlockPos blockPos, final boolean strictDirection) {
        Set<Direction> ncpDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
        for (Direction direction : Direction.values()) {
            final BlockState state = mc.world.getBlockState(blockPos.offset(direction));
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            if (strictDirection && !ncpDirections.contains(direction.getOpposite())) {
                continue;
            }
            return direction.getOpposite();
        }
        return null;
    }

    public static Set<Direction> getPlaceDirectionsNCP(Vec3d eyePos, Vec3d blockPos) {
        return getPlaceDirectionsNCP(eyePos.x, eyePos.y, eyePos.z, blockPos.x, blockPos.y, blockPos.z);
    }

    public static Set<Direction> getPlaceDirectionsNCP(final double x, final double y, final double z, final double dx, final double dy, final double dz) {
        final double xdiff = x - dx;
        final double ydiff = y - dy;
        final double zdiff = z - dz;
        final Set<Direction> dirs = new HashSet<>(6);
        if (ydiff > 0.5) {
            dirs.add(Direction.UP);
        } else if (ydiff < -0.5) {
            dirs.add(Direction.DOWN);
        } else {
            dirs.add(Direction.UP);
            dirs.add(Direction.DOWN);
        }
        if (xdiff > 0.5) {
            dirs.add(Direction.EAST);
        } else if (xdiff < -0.5) {
            dirs.add(Direction.WEST);
        } else {
            dirs.add(Direction.EAST);
            dirs.add(Direction.WEST);
        }
        if (zdiff > 0.5) {
            dirs.add(Direction.SOUTH);
        } else if (zdiff < -0.5) {
            dirs.add(Direction.NORTH);
        } else {
            dirs.add(Direction.SOUTH);
            dirs.add(Direction.NORTH);
        }
        return dirs;
    }

    public static Vec3d getEyesPos() {
        return new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
    }

    public static List<BlockPos> getSphere(double range, BlockPos pos, boolean sphere, boolean hollow) {
        ArrayList<BlockPos> circleblocks = new ArrayList<>();
        int cx = pos.getX();
        int cy = pos.getY();
        int cz = pos.getZ();
        for (int x = cx - (int) range; x <= cx + range; x++) {
            for (int z = cz - (int) range; z <= cz + range; z++) {
                for (int y = sphere ? cy - (int) range : cy; y < (sphere ? cy + range : cy + 1); y++) {
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                    if (dist < range * range && !(hollow && dist < (range - 1.0) * (range - 1.0))) {
                        BlockPos l = new BlockPos(x, y, z);
                        circleblocks.add(l);
                    }
                }
            }
        }
        return circleblocks;
    }

    public static boolean areBoxesEmpty(final BlockPos pos) {
        return mc.world.getNonSpectatingEntities(LivingEntity.class, new Box(pos)).isEmpty();
    }

    public static boolean canPlaceBlock(final BlockPos pos) {
        boolean allow = mc.world.getBlockState(pos).isReplaceable();
        if (!areBoxesEmpty(pos)) {
            allow = false;
        }
        return allow;
    }

    public static boolean isAir(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.AIR;
    }

    public static boolean isObby(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN;
    }

    public static boolean isBedrock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
    }

    public static boolean isEchest(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST;
    }

    public static boolean isSafe(BlockPos pos) {
        return isObby(pos) || isBedrock(pos) || isEchest(pos);
    }

    public static List<BlockPos> getSphere(Entity entity, float radius, boolean ignoreAir) {
        List<BlockPos> sphere = new ArrayList<>();
        BlockPos pos = entity.getBlockPos();
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();
        int radiuss = (int) radius;
        for (int x = posX - radiuss; x <= posX + radius; x++) {
            for (int z = posZ - radiuss; z <= posZ + radius; z++) {
                for (int y = posY - radiuss; y < posY + radius; y++) {
                    if ((posX - x) * (posX - x) + (posZ - z) * (posZ - z) + (posY - y) * (posY - y) < radius * radius) {
                        BlockPos position = new BlockPos(x, y, z);
                        if (ignoreAir && mc.world.getBlockState(position).getBlock() == Blocks.AIR) {
                            continue;
                        }
                        sphere.add(position);
                    }
                }
            }
        }
        return sphere;
    }

    public static boolean isInside(Entity entity, Box bb) {
        return mc.world.getBlockCollisions(entity, bb).iterator().hasNext();
    }
}