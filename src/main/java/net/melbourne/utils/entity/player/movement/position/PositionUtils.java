package net.melbourne.utils.entity.player.movement.position;

import net.melbourne.utils.Globals;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class PositionUtils implements Globals {
    public static BlockPos getFlooredPosition(Entity entity) {
        return new BlockPos(entity.getBlockX(), MathHelper.floor(((entity.getY() - Math.floor(entity.getY())) > 0.8) ? (Math.floor(entity.getY()) + 1.0) : Math.floor(entity.getY())), entity.getBlockZ());
    }
}