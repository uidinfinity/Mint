package net.melbourne.modules.impl.misc;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;

@FeatureInfo(name = "AutoTool", category = Category.Misc)
public class AutoToolFeature extends Feature {

    @SubscribeEvent
    public void onTick(TickEvent e) {
        if (getNull()) return;

        boolean mining =
                mc.options.attackKey.isPressed()
                        && mc.crosshairTarget instanceof BlockHitResult bhr
                        && mc.world != null
                        && mc.world.getBlockState(bhr.getBlockPos()) != null;

        if (!mining) return;

        BlockHitResult bhr = (BlockHitResult) mc.crosshairTarget;
        BlockState state = mc.world.getBlockState(bhr.getBlockPos());
        if (state == null) return;

        int best = findBestToolSlot(state);
        if (best == -1) return;

        int clientSlot = mc.player.getInventory().getSelectedSlot();
        if (clientSlot != best) {
            mc.player.getInventory().setSelectedSlot(best);
        }
    }

    private int findBestToolSlot(BlockState state) {
        int bestSlot = -1;
        float bestScore = -1f;

        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.isEmpty()) continue;

            float speed = s.getMiningSpeedMultiplier(state);
            boolean suitable = s.isSuitableFor(state);

            float score = speed + (suitable ? 1000f : 0f);

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }
}
