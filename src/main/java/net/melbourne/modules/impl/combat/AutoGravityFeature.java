package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.PlaceFeature;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.Comparator;

@FeatureInfo(name = "AutoGravity", category = Category.Combat)
public class AutoGravityFeature extends PlaceFeature {

    private static final Item[] CONCRETE_POWDERS = new Item[]{
            Items.WHITE_CONCRETE_POWDER,
            Items.ORANGE_CONCRETE_POWDER,
            Items.MAGENTA_CONCRETE_POWDER,
            Items.LIGHT_BLUE_CONCRETE_POWDER,
            Items.YELLOW_CONCRETE_POWDER,
            Items.LIME_CONCRETE_POWDER,
            Items.PINK_CONCRETE_POWDER,
            Items.GRAY_CONCRETE_POWDER,
            Items.LIGHT_GRAY_CONCRETE_POWDER,
            Items.CYAN_CONCRETE_POWDER,
            Items.PURPLE_CONCRETE_POWDER,
            Items.BLUE_CONCRETE_POWDER,
            Items.BROWN_CONCRETE_POWDER,
            Items.GREEN_CONCRETE_POWDER,
            Items.RED_CONCRETE_POWDER,
            Items.BLACK_CONCRETE_POWDER
    };

    private final ModeSetting blockMode = new ModeSetting("Block", "Gravity block to drop", "Anvil", new String[]{"Anvil", "ConcretePowder", "Sand", "RedSand"});
    private final BooleanSetting continuous = new BooleanSetting("Continuous", "Continuously places gravity blocks", false);
    private final NumberSetting enemyRange = new NumberSetting("EnemyRange", "Target range", 6.0, 1.0, 12.0);
    private final NumberSetting extrapolation = new NumberSetting("Extrapolation", "Target prediction", 0, 0, 10);
    private final NumberSetting height = new NumberSetting("Height", "Placement height", 5, 2, 10);

    private BlockPos pos;
    private boolean placedOnceOnBase;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || shouldDisable()) return;

        PlayerEntity target = findTarget();
        if (target == null) return;

        Item[] items = getItems();
        if (items == null || items.length == 0) return;

        Vec3d predicted = target.getPos().add(target.getVelocity().multiply(extrapolation.getValue().doubleValue()));
        BlockPos base = BlockPos.ofFloored(predicted);
        int y = base.getY() + height.getValue().intValue();

        if (!continuous.getValue()) {
            if (pos == null || !pos.equals(base)) {
                pos = base;
                placedOnceOnBase = false;
            }
            if (placedOnceOnBase) return;
        }

        boolean sneakForAnvil = "Anvil".equals(blockMode.getValue());
        boolean prevSneak = mc.options.sneakKey.isPressed();

        for (int i = 0; i <= 3; i++) {
            BlockPos pos = new BlockPos(base.getX(), y + i, base.getZ());

            if (!mc.world.getBlockState(pos).isReplaceable()) continue;
            if (isEntityBlocking(pos, target)) continue;

            if (sneakForAnvil && !prevSneak) mc.options.sneakKey.setPressed(true);

            boolean placed = placeBlocks(Collections.singletonList(pos), items);

            if (sneakForAnvil && !prevSneak) mc.options.sneakKey.setPressed(false);

            if (placed) {
                if (!continuous.getValue()) {
                    placedOnceOnBase = true;
                    setEnabled(false);
                }
                return;
            }
        }

        if (sneakForAnvil && !prevSneak && mc.options.sneakKey.isPressed()) {
            mc.options.sneakKey.setPressed(false);
        }
    }

    private Item[] getItems() {
        return switch (blockMode.getValue()) {
            case "Sand" -> new Item[]{Items.SAND};
            case "RedSand" -> new Item[]{Items.RED_SAND};
            case "ConcretePowder" -> CONCRETE_POWDERS;
            default -> new Item[]{Items.ANVIL};
        };
    }

    private boolean isEntityBlocking(BlockPos pos, PlayerEntity target) {
        return mc.world.getOtherEntities(null, new Box(pos)).stream()
                .anyMatch(e -> e != target && e.isAlive());
    }

    private PlayerEntity findTarget() {
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive())
                .filter(p -> !Managers.FRIEND.isFriend(p.getName().getString()))
                .filter(p -> mc.player.distanceTo(p) <= enemyRange.getValue().floatValue())
                .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)))
                .orElse(null);
    }
}