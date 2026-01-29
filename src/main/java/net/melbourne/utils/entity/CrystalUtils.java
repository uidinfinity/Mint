package net.melbourne.utils.entity;

import net.melbourne.services.Services;
import net.melbourne.utils.Globals;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.RegistryKeys;

import static net.melbourne.utils.entity.DamageUtils.PROTECTION_MAP;

public final class CrystalUtils implements Globals {

    public static float calculateDamage(BlockPos crystalPos, PlayerEntity target, Box box, boolean ignoreTerrain) {
        return crystalDamageAt(new Vec3d(crystalPos.getX() + 0.5, crystalPos.getY(), crystalPos.getZ() + 0.5), target, box, ignoreTerrain);
    }

    private static float crystalDamageAt(Vec3d pos, PlayerEntity base, Box box, boolean ignoreTerrain) {
        if (mc.world == null || mc.world.getDifficulty() == Difficulty.PEACEFUL) return 0f;

        double dist = Math.sqrt(box.getCenter().add(0, -0.9, 0).squaredDistanceTo(pos)) / 12.0;
        if (dist > 1.0) return 0f;

        double exposure = (1.0 - dist) * getExposure(pos, box, ignoreTerrain);
        float damage = (float) ((exposure * exposure + exposure) / 2.0 * 7.0 * 12.0 + 1.0);

        Difficulty d = mc.world.getDifficulty();
        if (d == Difficulty.EASY) damage = Math.min(damage / 2f + 1f, damage);
        else if (d == Difficulty.HARD) damage *= 1.5f;

        float armor = base.getArmor();
        float tough = (float) base.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
        damage = getDamageAfterArmor(damage, armor, tough);

        if (base.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int amp = base.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1;
            damage *= Math.max(0f, 1f - 0.2f * amp);
        }

        int epf = getProtectionAmountFromArmor(base);
        damage *= (1.0f - MathHelper.clamp(epf, 0, 20) / 25.0f);
        return Math.max(damage, 0f);
    }

    public static float calculateDamage(BlockPos crystalPos, PlayerEntity target, PlayerEntity self, boolean ignoreTerrain) {
        return crystalDamageAt(new Vec3d(crystalPos.getX() + 0.5, crystalPos.getY(), crystalPos.getZ() + 0.5), target, self, ignoreTerrain);
    }

    private static float crystalDamageAt(Vec3d pos, PlayerEntity base, PlayerEntity predict, boolean ignoreTerrain) {
        if (mc.world == null || mc.world.getDifficulty() == Difficulty.PEACEFUL) return 0f;

        Box box = predict.getBoundingBox();
        double dist = Math.sqrt(box.getCenter().add(0, -0.9, 0).squaredDistanceTo(pos)) / 12.0;
        if (dist > 1.0) return 0f;

        double exposure = (1.0 - dist) * getExposure(pos, box, ignoreTerrain);
        float damage = (float) ((exposure * exposure + exposure) / 2.0 * 7.0 * 12.0 + 1.0);

        Difficulty d = mc.world.getDifficulty();
        if (d == Difficulty.EASY) damage = Math.min(damage / 2f + 1f, damage);
        else if (d == Difficulty.HARD) damage *= 1.5f;

        float armor = base.getArmor();
        float tough = (float) base.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
        damage = getDamageAfterArmor(damage, armor, tough);

        if (base.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int amp = base.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1;
            damage *= Math.max(0f, 1f - 0.2f * amp);
        }

        int epf = getProtectionAmountFromArmor(base);
        damage *= (1.0f - MathHelper.clamp(epf, 0, 20) / 25.0f);
        return Math.max(damage, 0f);
    }

    private static int getProtectionAmountFromArmor(PlayerEntity p) {
        int x = 0;

        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getEquippedStack(s);
            if (st.isEmpty())
                continue;

            x += getProtectionAmount(st);
        }

        return x;
    }

    public static int getProtectionAmount(ItemStack armor) {
        int x = 0;
        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(armor);
        for (RegistryEntry<Enchantment> enchantment : enchantments.getEnchantments()) {
            String id = enchantment.getIdAsString().replace("minecraft:", "");
            if (PROTECTION_MAP.containsKey(id)) {
                x += enchantments.getLevel(enchantment) * PROTECTION_MAP.get(id);
                break;
            }
        }

        return x;
    }

    private static float getExposure(Vec3d source, Box box, boolean ignoreTerrain) {
        double xDiff = box.maxX - box.minX;
        double yDiff = box.maxY - box.minY;
        double zDiff = box.maxZ - box.minZ;

        double xStep = 1 / (xDiff * 2 + 1);
        double yStep = 1 / (yDiff * 2 + 1);
        double zStep = 1 / (zDiff * 2 + 1);

        if (xStep > 0 && yStep > 0 && zStep > 0) {
            int misses = 0;
            int hits = 0;

            double xOffset = (1 - Math.floor(1 / xStep) * xStep) * 0.5;
            double zOffset = (1 - Math.floor(1 / zStep) * zStep) * 0.5;

            xStep = xStep * xDiff;
            yStep = yStep * yDiff;
            zStep = zStep * zDiff;

            double startX = box.minX + xOffset;
            double startY = box.minY;
            double startZ = box.minZ + zOffset;
            double endX = box.maxX + xOffset;
            double endY = box.maxY;
            double endZ = box.maxZ + zOffset;

            for (double x = startX; x <= endX; x += xStep) {
                for (double y = startY; y <= endY; y += yStep) {
                    for (double z = startZ; z <= endZ; z += zStep) {
                        Vec3d position = new Vec3d(x, y, z);

                        if (raycast(position, source, null, ignoreTerrain) == HitResult.Type.MISS) misses++;

                        hits++;
                    }
                }
            }

            return (float) misses / hits;
        }

        return 0f;
    }


    private static float getDamageAfterArmor(float dmg, float armor, float tough) {
        float f = 2.0f + tough / 4.0f;
        float red = Math.max(armor / 5.0f, armor - dmg / f);
        red = MathHelper.clamp(red, 0.0f, 20.0f);
        return dmg * (1.0f - red / 25.0f);
    }

    public static boolean canPlaceCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        BlockPos base = pos.down();
        if (!mc.world.getBlockState(base).isOf(Blocks.OBSIDIAN) && !mc.world.getBlockState(base).isOf(Blocks.BEDROCK))
            return false;
        BlockPos lower = base.up();
        BlockPos upper = lower.up();
        if (hasEntity(lower, ignoreCrystal, ignoreItem)) return false;
        if (hasEntity(upper, ignoreCrystal, ignoreItem)) return false;
        return mc.world.getBlockState(lower).isAir() || mc.world.getBlockState(lower).isOf(Blocks.FIRE);
    }

    private static boolean hasEntity(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        Box boxtop2 = new Box(pos);

        for (Entity e : mc.world.getNonSpectatingEntities(Entity.class, boxtop2)) {
            if (e instanceof PlayerEntity player) {
                if (Services.HITBOX.isServerCrawling(player)) {
                    Box boxtop = Services.HITBOX.getCrawlingBoundingBox(player);

                    if (!boxtop.intersects(boxtop)) continue;
                }
            }
            if (!e.isAlive()) continue;
            if (ignoreItem && e instanceof ItemEntity) continue;
            if (e instanceof ArmorStandEntity) continue;
            if (e instanceof EndCrystalEntity) {
                if (!ignoreCrystal) return true;
                continue;
            }
            return true;
        }
        return false;
    }

    public static Direction clickSide(BlockPos pos) {
        if (mc.world.getBlockState(pos.up()).isAir()) return Direction.UP;
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = pos.toCenterPos();
        Direction best = Direction.UP;
        double bestD = Double.MAX_VALUE;
        for (Direction d : Direction.values()) {
            Vec3d hit = center.add(d.getVector().getX() * 0.5, d.getVector().getY() * 0.5, d.getVector().getZ() * 0.5);
            double dd = eye.squaredDistanceTo(hit);
            if (dd < bestD) {
                bestD = dd;
                best = d;
            }
        }
        return best;
    }

    public static Direction clickSide(BlockPos pos, boolean strictDirection) {
        if (mc.player.getBlockPos().up().equals(pos) || mc.player.isCrawling())
            return Direction.UP;

        Vec3d eye = mc.player.getEyePos();
        double bestDist = Double.MAX_VALUE;
        Direction bestSide = Direction.UP;

        for (Direction side : Direction.values()) {
            BlockPos offset = pos.offset(side);
            BlockState state = mc.world.getBlockState(offset);

            if (strictDirection) {
                if (!canSeeDirection(side, pos))
                    continue;

                if (!state.isReplaceable())
                    continue;
            }

            if (side.equals(Direction.UP))
                return side;

            Vec3d hit = new Vec3d(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f).add(Vec3d.of(side.getVector()));
            double dist = eye.squaredDistanceTo(hit);

            if (dist < bestDist) {
                bestDist = dist;
                bestSide = side;
            }
        }

        return bestSide;
    }

    public static boolean canSeeDirection(Direction direction, BlockPos target) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d centered = new Vec3d(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        Vec3i dir = direction.getVector();
        Vec3d pos = centered.add(dir.getX() * 0.5, dir.getY() * 0.5, dir.getZ() * 0.5);

        return switch (direction) {
            case NORTH -> eyes.z < pos.z;
            case EAST -> eyes.x > pos.x;
            case SOUTH -> eyes.z > pos.z;
            case WEST -> eyes.x < pos.x;
            case UP -> (eyes.y + 0.5) > pos.y;
            case DOWN -> eyes.y < pos.y;
        };
    }



    public static boolean isFacePlace(BlockPos placePos, PlayerEntity target) {
        return placePos.up().equals(target.getBlockPos());
    }

    public static float getTotalHealth(PlayerEntity p) {
        return p.getHealth() + p.getAbsorptionAmount();
    }

    private static HitResult.Type raycast(Vec3d start, Vec3d end, BlockPos exception, boolean ignoreTerrain) {
        return BlockView.raycast(start, end, null, (innerContext, blockPos) -> {
            BlockState blockState;
            if (blockPos.equals(exception)) {
                blockState = Blocks.AIR.getDefaultState();
            } else {
                blockState = mc.world.getBlockState(blockPos);
                if (blockState.getBlock().getBlastResistance() < 600 && ignoreTerrain) blockState = Blocks.AIR.getDefaultState();
            }

            BlockHitResult hitResult = blockState.getCollisionShape(mc.world, blockPos).raycast(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
        }, (innerContext) -> HitResult.Type.MISS);
    }
}