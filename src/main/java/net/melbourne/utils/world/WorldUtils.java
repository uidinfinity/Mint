package net.melbourne.utils.world;

import net.melbourne.utils.Globals;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class WorldUtils implements Globals {

    public static Vec3d getHitVector(BlockPos position, Direction direction) {
        return position.toCenterPos().add(direction.getOffsetX() / 2.0, direction.getOffsetY() / 2.0, direction.getOffsetZ() / 2.0);
    }

    public static boolean canSee(Entity entity) {
        return canSee(entity.getX(), entity.getY(), entity.getZ());
    }

    public static boolean canSee(BlockPos position)
    {
        return canSee(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
    }

    public static boolean canSee(double x, double y, double z) {
        return mc.world.raycast(new RaycastContext(new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()), new Vec3d(x, y, z), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
    }

    public static Direction getClosestDirection(BlockPos position, boolean strictDirection) {
        if (strictDirection) {
            if (mc.player.getY() >= position.getY()) return Direction.UP;

            BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), Vec3d.ofCenter(position), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            if (result == null || result.getType() != HitResult.Type.BLOCK || result.getSide() == null) {
                return getClosestDirection(position);
            }

            return result.getSide();
        } else {
            return getClosestDirection(position);
        }
    }

    private static Direction getClosestDirection(BlockPos position) {
        Direction closestDirection = null;
        Vec3d offsetPosition = null;

        for (Direction direction : Direction.values()) {
            Vec3d newOffset = getHitVector(position, direction);
            if (closestDirection == null) {
                closestDirection = direction;
                offsetPosition = newOffset;
                continue;
            }

            if (mc.player.squaredDistanceTo(newOffset) < mc.player.squaredDistanceTo(offsetPosition)) {
                closestDirection = direction;
                offsetPosition = newOffset;
            }
        }

        return closestDirection;
    }

    public static Direction getDirection(BlockPos position, boolean strictDirection) {
        return getDirection(position, null, strictDirection);
    }

    public static Direction getDirection(BlockPos position, List<BlockPos> exceptions, boolean strictDirection) {
        List<Direction> strictDirections = new ArrayList<>();
        if (strictDirection) strictDirections = getStrictDirections(mc.player.getEyePos(), Vec3d.ofCenter(position));

        for (Direction direction : Direction.values()) {
            BlockPos offset = position.offset(direction);
            if (strictDirection && !strictDirections.contains(direction.getOpposite())) continue;
            if (mc.world.getBlockState(offset).isReplaceable() && (exceptions == null || !exceptions.contains(offset))) {
                continue;
            }

            return direction;
        }

        return null;
    }


    public static List<Direction> getStrictDirections(Vec3d eyePos, Vec3d blockPos) {
        List<Direction> directions = new ArrayList<>();

        double differenceX = eyePos.getX() - blockPos.getX();
        double differenceY = eyePos.getY() - blockPos.getY();
        double differenceZ = eyePos.getZ() - blockPos.getZ();

        if (differenceY > 0.5) {
            directions.add(Direction.UP);
        } else if (differenceY < -0.5) {
            directions.add(Direction.DOWN);
        } else {
            directions.add(Direction.UP);
            directions.add(Direction.DOWN);
        }

        if (differenceX > 0.5) {
            directions.add(Direction.EAST);
        } else if (differenceX < -0.5) {
            directions.add(Direction.WEST);
        } else {
            directions.add(Direction.EAST);
            directions.add(Direction.WEST);
        }

        if (differenceZ > 0.5) {
            directions.add(Direction.SOUTH);
        } else if (differenceZ < -0.5) {
            directions.add(Direction.NORTH);
        } else {
            directions.add(Direction.SOUTH);
            directions.add(Direction.NORTH);
        }

        return directions;
    }

    public static List<PlayerEntity> getCollisions(BlockPos pos) {
        List<PlayerEntity> collisions = new ArrayList<>();
        for(PlayerEntity player : mc.world.getPlayers()) {
            if(player == null || player.isDead()) continue;
            if(player.getBoundingBox().intersects(new Box(pos))) collisions.add(player);
        }
        return collisions;
    }
}
