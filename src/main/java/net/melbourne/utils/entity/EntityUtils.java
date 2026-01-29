package net.melbourne.utils.entity;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.modules.impl.misc.FastLatencyFeature;
import net.melbourne.utils.Globals;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class EntityUtils implements Globals {

    public static GameMode getGameMode(PlayerEntity player) {
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return playerListEntry == null ? GameMode.CREATIVE : playerListEntry.getGameMode();
    }

    public static int getLatency(PlayerEntity player) {
        if (player == null || mc.getNetworkHandler() == null) return 0;

        if (player == mc.player) {
            FastLatencyFeature fastLatency = Managers.FEATURE.getFeatureFromClass(FastLatencyFeature.class);
            if (fastLatency != null && fastLatency.isEnabled() && fastLatency.resolvedPing > 0) {
                return fastLatency.resolvedPing;
            }
        }

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    public static String getGameModeName(GameMode gameMode) {
        return switch (gameMode) {
            case CREATIVE -> "C";
            case ADVENTURE -> "A";
            case SPECTATOR -> "SP";
            default -> "S";
        };
    }

    public static boolean isInWeb(Entity entity) {
        for (float x : new float[]{0, 0.3F, -0.3f}) {
            for (float z : new float[]{0, 0.3F, -0.3f}) {
                for (int y : new int[]{-1, 0, 1, 2}) {
                    BlockPos pos = BlockPos.ofFloored(entity.getX() + x, entity.getY(), entity.getZ() + z).up(y);
                    if (new Box(pos).intersects(entity.getBoundingBox()) && mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean isThrowable(Item item) {
        return item instanceof EnderPearlItem ||
                item instanceof TridentItem ||
                item instanceof ExperienceBottleItem ||
                item instanceof SnowballItem ||
                item instanceof EggItem ||
                item instanceof SplashPotionItem ||
                item instanceof LingeringPotionItem;
    }

    public static Vec3d getRenderPos(Entity entity, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
        return new Vec3d(x, y, z);
    }

    public static Hand getHand(int slot) {
        return slot == -2 ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    public static double getSpeed(Entity entity, SpeedUnit unit) {
        double speed = Math.sqrt(MathHelper.square(Math.abs(entity.getX() - entity.lastRenderX)) + MathHelper.square(Math.abs(entity.getZ() - entity.lastRenderZ)));

        if (unit == SpeedUnit.KILOMETERS) return (speed * 3.6 * Services.WORLD.getTimerMultiplier()) * 20;
        else return speed / 0.05 * Services.WORLD.getTimerMultiplier();
    }

    public enum SpeedUnit {
        METERS, KILOMETERS
    }
}