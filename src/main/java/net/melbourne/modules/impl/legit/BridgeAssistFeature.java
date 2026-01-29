package net.melbourne.modules.impl.legit;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@FeatureInfo(name = "BridgeAssist", category = Category.Legit)
public class BridgeAssistFeature extends Feature {

    @Override
    public void onDisable() {
        if (mc.options != null) {
            mc.options.sneakKey.setPressed(false);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        BlockPos stoodOn = mc.player.getBlockPos().down();

        if (mc.world.getBlockState(stoodOn).isAir()) {
            if (mc.player.isOnGround()) {
                mc.options.sneakKey.setPressed(true);
            }
            return;
        }

        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        double fracX = MathHelper.fractionalPart(playerX);
        double fracZ = MathHelper.fractionalPart(playerZ);

        boolean shouldSneak = false;

        if (fracX < 0.1) {
            if (mc.world.getBlockState(stoodOn.offset(Direction.WEST)).isAir()) shouldSneak = true;
        }
        if (1.0 - fracX < 0.1) {
            if (mc.world.getBlockState(stoodOn.offset(Direction.EAST)).isAir()) shouldSneak = true;
        }
        if (fracZ < 0.1) {
            if (mc.world.getBlockState(stoodOn.offset(Direction.NORTH)).isAir()) shouldSneak = true;
        }
        if (1.0 - fracZ < 0.1) {
            if (mc.world.getBlockState(stoodOn.offset(Direction.SOUTH)).isAir()) shouldSneak = true;
        }

        mc.options.sneakKey.setPressed(shouldSneak);
    }

    public boolean getNull() {
        return mc.player == null || mc.world == null;
    }
}