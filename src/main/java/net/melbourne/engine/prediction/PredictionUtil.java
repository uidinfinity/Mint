package net.melbourne.engine.prediction;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PredictionUtil {

    private static final double GRAVITY = 0.08;
    private static final double HORIZONTAL_FRICTION = 0.98;

    public static Vec3d predictPosition(Entity entity, float magnitude, boolean useGravity) {
        Vec3d currentPos = entity.getPos();
        Vec3d velocity = entity.getVelocity();

        double horizontalVelocity = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalVelocity < 0.01) {
            return currentPos;
        }

        Vec3d predictedVelocity = velocity.multiply(magnitude);
        Vec3d predictedPos = currentPos;

        predictedVelocity = predictedVelocity.multiply(HORIZONTAL_FRICTION, 1.0, HORIZONTAL_FRICTION);

        if (useGravity) {
            if (!entity.isOnGround()) {
                predictedVelocity = predictedVelocity.subtract(0.0, GRAVITY, 0.0);
            } else {
                predictedVelocity = new Vec3d(predictedVelocity.x, 0.0, predictedVelocity.z);
            }
        }

        predictedPos = predictedPos.add(predictedVelocity);

        if (isPredictedGrounded(entity.getWorld(), predictedPos)) {
            predictedVelocity = new Vec3d(predictedVelocity.x, 0.0, predictedVelocity.z);
        }

        return predictedPos;
    }

    private static boolean isPredictedGrounded(World world, Vec3d pos) {
        BlockPos checkPos = BlockPos.ofFloored(pos.x, pos.y - 0.1, pos.z);
        return world.getBlockState(checkPos).isSolidBlock(world, checkPos);
    }
}