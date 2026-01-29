package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.PlaceFeature;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;

@FeatureInfo(name = "AntiMace", category = Category.Combat)
public class AntiMaceFeature extends PlaceFeature {

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;
        if (shouldDisable()) return;

        PlayerEntity target = findTarget(6.0);
        if (target == null || !target.isGliding()) return;

        BlockPos placePos = BlockPos.ofFloored(target.getX(), target.getY() + 1.8, target.getZ());

        if (canPlace(placePos)) {
            placeBlocks(Collections.singletonList(placePos));
        }
    }

    private PlayerEntity findTarget(double range) {
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && mc.player.distanceTo(p) <= range && !p.isDead() && !Managers.FRIEND.isFriend(p.getName().getString()))
                .min((a, b) -> Double.compare(mc.player.distanceTo(a), mc.player.distanceTo(b)))
                .orElse(null);
    }
}