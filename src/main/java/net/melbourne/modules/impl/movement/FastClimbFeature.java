package net.melbourne.modules.impl.movement;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.UpdateMovementEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.math.BlockPos;

@FeatureInfo(name = "FastClimb", category = Category.Movement)
public class FastClimbFeature extends Feature {

    @SubscribeEvent
    public void onUpdateMovement(UpdateMovementEvent event) {
        if (getNull())
            return;

        if (mc.player.input.getMovementInput().y > 0.01f && mc.player.isOnGround() &&
                mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ()))
                .getBlock() instanceof StairsBlock) {
            mc.player.jump();
        }
    }
}
