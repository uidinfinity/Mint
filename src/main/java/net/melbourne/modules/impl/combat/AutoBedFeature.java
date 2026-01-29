package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.GameLoopEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.client.RendersFeature;
import net.melbourne.services.Services;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.entity.player.PlayerUtils;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.inventory.SwitchType;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.melbourne.utils.miscellaneous.NetworkUtils;
import net.melbourne.utils.miscellaneous.Timer;
import net.melbourne.utils.rotation.RotationPoint;
import net.melbourne.utils.rotation.RotationUtils;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@FeatureInfo(name = "AutoBed", category = Category.Combat)
public final class AutoBedFeature extends Feature {

    public final BooleanSetting gameLoop = new BooleanSetting("Gameloop", "Run logic in game loop", true);
    public final NumberSetting loopDelay = new NumberSetting("GameloopDelay", "Delay for game loop", 0.0, 0.0, 500.0);
    public final BooleanSetting eatingPause = new BooleanSetting("EatingPause", "Pause while eating", true);
    public final NumberSetting switchPause = new NumberSetting("SwitchPause", "Pause after switching", 100, 0, 1000);

    public final NumberSetting targetRange = new NumberSetting("TargetRange", "Range to target", 12.0, 4.0, 20.0);
    public final NumberSetting wallRange = new NumberSetting("WallRange", "Range through walls", 6.0, 0.0, 6.0);
    public final NumberSetting minDamage = new NumberSetting("MinDamage", "Minimum damage to target", 7.0, 0.0, 36.0);
    public final NumberSetting maxSelf = new NumberSetting("MaxSelf", "Maximum damage to self", 8.0, 0.0, 36.0);
    public final NumberSetting range = new NumberSetting("Range", "Place range", 5.5, 1.0, 6.0);
    public final NumberSetting antiSuicide = new NumberSetting("AntiSuicide", "Stop if health too low", 4.0, 0.0, 20.0);

    public final NumberSetting placeDelay = new NumberSetting("PlaceDelay", "Delay between places", 80, 0, 1000);
    public final NumberSetting breakDelay = new NumberSetting("BreakDelay", "Delay between breaks", 0, 0, 1000);
    public final BooleanSetting instant = new BooleanSetting("Instant", "Explode immediately after placing", true);

    public final BooleanSetting inventoryPlace = new BooleanSetting("InventoryPlace", "Place beds directly from inventory", true);

    public final ModeSetting switching = new ModeSetting("Switch", "Switch mode", "Silent", new String[]{"None", "Normal", "Silent"});
    public final ModeSetting airPlace = new ModeSetting("AirPlace", "Air placement mode", "None", new String[]{"None", "Normal"});
    public final ModeSetting swing = new ModeSetting("Swing", "Swing mode", "Mainhand", new String[]{"None", "Packet", "Mainhand", "Offhand"});
    public final BooleanSetting rotate = new BooleanSetting("Rotate", "Rotate on place", true);
    public final BooleanSetting strictDirection = new BooleanSetting("StrictDirection", "Face directions only", false);

    public final BooleanSetting terrainIgnore = new BooleanSetting("TerrainIgnore", "Ignore terrain for damage calc", true);
    public final BooleanSetting forcePlace = new BooleanSetting("ForcePlace", "Force place on low HP", true);
    public final NumberSetting forceMaxHealth = new NumberSetting("ForceMax", "Max health for force place", 8.0, 0.0, 36.0);
    public final NumberSetting forceMin = new NumberSetting("ForceMin", "Min damage for force place", 2.0, 0.0, 36.0);

    public final BooleanSetting render = new BooleanSetting("Render", "Render placement", true);
    public final BooleanSetting fade = new BooleanSetting("Fade", "Fades the last position", true);

    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer switchTimer = new Timer();
    private final Timer loopTimer = new Timer();

    private BlockPos currentBedFoot = null;
    private Direction currentBedDir = null;
    private boolean bedPlaced = false;

    private float lastDamage = 0f;
    private float lastSelfDamage = 0f;

    private final ConcurrentHashMap<BlockPos, Long> crystalFadeMap = new ConcurrentHashMap<>();
    private volatile BlockPos currentCrystalBlock = null;

    private final DecimalFormat df = new DecimalFormat("0.00");
    private String infoDamage = "0.00";
    private String infoSelf = "0.00";

    private static final Item[] BEDS = {
            Items.WHITE_BED, Items.ORANGE_BED, Items.MAGENTA_BED, Items.LIGHT_BLUE_BED,
            Items.YELLOW_BED, Items.LIME_BED, Items.PINK_BED, Items.GRAY_BED,
            Items.LIGHT_GRAY_BED, Items.CYAN_BED, Items.PURPLE_BED, Items.BLUE_BED,
            Items.BROWN_BED, Items.GREEN_BED, Items.RED_BED, Items.BLACK_BED
    };

    @Override
    public void onEnable() {
        placeTimer.reset();
        breakTimer.reset();
        switchTimer.reset();
        currentBedFoot = null;
        currentBedDir = null;
        bedPlaced = false;

        crystalFadeMap.clear();
        currentCrystalBlock = null;

        Services.ROTATION.setRotationPoint(null);
    }

    @Override
    public void onDisable() {
        currentBedFoot = null;
        currentBedDir = null;
        bedPlaced = false;

        crystalFadeMap.clear();
        currentCrystalBlock = null;

        Services.ROTATION.setRotationPoint(null);
    }

    private boolean checkDimension() {
        if (mc.world == null) return false;
        return mc.world.getRegistryKey() == World.NETHER || mc.world.getRegistryKey() == World.END;
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull() || !checkDimension()) return;

        if (eatingPause.getValue() && mc.player.isUsingItem()) return;
        if (!switchTimer.hasTimeElapsed(switchPause.getValue().longValue())) return;

        if (!gameLoop.getValue()) execute();
    }

    @SubscribeEvent
    public void onGameLoop(GameLoopEvent event) {
        if (getNull() || !checkDimension()) return;
        if (!gameLoop.getValue()) return;
        if (!loopTimer.hasTimeElapsed(loopDelay.getValue().longValue())) return;

        execute();
        loopTimer.reset();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (getNull()) return;
        if (!render.getValue()) return;
        RendersFeature renders = Managers.FEATURE.getFeatureFromClass(RendersFeature.class);
        if (renders == null || !renders.isEnabled()) return;

        long now = System.currentTimeMillis();
        long lifetime = renders.getRenderTimeMillis();

        BlockPos targetBlock = currentBedFoot;

        if (targetBlock != null) {
            if (!targetBlock.equals(currentCrystalBlock)) {
                if (currentCrystalBlock != null && fade.getValue()) {
                    crystalFadeMap.put(currentCrystalBlock, now);
                }
                currentCrystalBlock = targetBlock;
            }
        } else {
            if (currentCrystalBlock != null) {
                if (fade.getValue()) {
                    crystalFadeMap.put(currentCrystalBlock, now);
                }
                currentCrystalBlock = null;
            }
        }

        if (currentCrystalBlock != null) {
            Box box = new Box(
                    currentCrystalBlock.getX(), currentCrystalBlock.getY(), currentCrystalBlock.getZ(),
                    currentCrystalBlock.getX() + 1, currentCrystalBlock.getY() + 0.5625, currentCrystalBlock.getZ() + 1
            );
            Renderer3D.renderBox(event.getContext(), box, ColorUtils.getGlobalColor(55));
            Renderer3D.renderBoxOutline(event.getContext(), box, ColorUtils.getGlobalColor());
        }

        Iterator<ConcurrentHashMap.Entry<BlockPos, Long>> fadeIt = crystalFadeMap.entrySet().iterator();
        while (fadeIt.hasNext()) {
            var e = fadeIt.next();
            BlockPos pos = e.getKey();
            long age = now - e.getValue();

            if (age > lifetime) {
                fadeIt.remove();
                continue;
            }

            float progress = 1.0f - (age / (float) lifetime);
            Box box = new Box(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 0.5625, pos.getZ() + 1
            );

            Color c = ColorUtils.getGlobalColor();
            Color fill = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (55 * progress));
            Color line = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * progress));

            Renderer3D.renderBox(event.getContext(), box, fill);
            Renderer3D.renderBoxOutline(event.getContext(), box, line);
        }
    }

    private void execute() {
        if (currentBedFoot == null || currentBedDir == null) {
            if (!findTarget()) {
                Services.ROTATION.setRotationPoint(null);
                return;
            }
        }

        BlockPos head = currentBedFoot.offset(currentBedDir);
        BlockState footState = mc.world.getBlockState(currentBedFoot);

        boolean isBed = footState.getBlock() instanceof BedBlock;

        if (isBed) {
            if (breakTimer.hasTimeElapsed(breakDelay.getValue().longValue())) {
                BlockPos clickPos = footState.get(BedBlock.PART) == BedPart.HEAD ? head : currentBedFoot;
                BlockHitResult hit = new BlockHitResult(clickPos.toCenterPos(), Direction.UP, clickPos, false);

                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                swingHand();

                breakTimer.reset();
                bedPlaced = false;
                currentBedFoot = null;
            }
            return;
        }

        if (!bedPlaced && placeTimer.hasTimeElapsed(placeDelay.getValue().longValue())) {
            int slot = findBedSlot();
            if (slot == -1) return;

            placeBed(currentBedFoot, currentBedDir, slot);

            if (instant.getValue()) {
                BlockHitResult hit = new BlockHitResult(currentBedFoot.toCenterPos(), Direction.UP, currentBedFoot, false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                swingHand();
            }

            placeTimer.reset();
            bedPlaced = true;
        }
    }

    private void swingHand() {
        switch (swing.getValue()) {
            case "Packet" -> mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            case "Mainhand" -> mc.player.swingHand(Hand.MAIN_HAND);
            case "Offhand" -> mc.player.swingHand(Hand.OFF_HAND);
        }
    }

    private void placeBed(BlockPos pos, Direction dir, int slot) {
        boolean isInventorySlot = slot >= 9;

        if (isInventorySlot) {
            doInventorySwap(slot);
        } else {
            SwitchType type = SwitchType.valueOf(switching.getValue());
            Services.INVENTORY.switchTo(slot, true, type);
        }

        if (rotate.getValue()) {
            float[] rotations = RotationUtils.getRotationsTo(mc.player.getEyePos(), pos.toCenterPos());
            float bedYaw = getDirectionYaw(dir);
            Services.ROTATION.setRotationPoint(new RotationPoint(bedYaw, rotations[1], 100, true));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    bedYaw,
                    rotations[1],
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
            ));
        }

        BlockHitResult hit = getHitResult(pos);
        if (hit == null && airPlace.getValue().equalsIgnoreCase("Normal")) {
            hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        }

        if (hit != null) {
            BlockHitResult finalHit = hit;
            NetworkUtils.sendSequencedPacket(s -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, finalHit, s));
            swingHand();
        }

        if (isInventorySlot) {
            doInventorySwap(slot);
        } else {
            Services.INVENTORY.switchBack();
        }
    }

    private void doInventorySwap(int slot) {
        int selected = mc.player.getInventory().getSelectedSlot();
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, selected, SlotActionType.SWAP, mc.player);
    }

    private float getDirectionYaw(Direction dir) {
        return switch (dir) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> -90f;
            default -> 0f;
        };
    }

    public BlockHitResult getHitResult(BlockPos pos) {
        Vec3d eyes = mc.player.getEyePos();
        BlockPos below = pos.down();

        if (airPlace.getValue().equalsIgnoreCase("None") || !mc.world.getBlockState(below).isReplaceable()) {
            if (!mc.world.getBlockState(below).isReplaceable()) {
                Vec3d hitVec = Vec3d.ofCenter(below).add(0, 0.5, 0);
                return new BlockHitResult(hitVec, Direction.UP, below, false);
            }
        }

        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;
            BlockPos neighbor = pos.offset(dir);
            BlockState state = mc.world.getBlockState(neighbor);
            if (state.isAir() || state.isReplaceable()) continue;

            Direction side = dir.getOpposite();
            Vec3d hitVec = Vec3d.ofCenter(pos).add(dir.getOffsetX() * 0.5, dir.getOffsetY() * 0.5, dir.getOffsetZ() * 0.5);
            if (strictDirection.getValue()) {
                Vec3d eyeToHit = hitVec.subtract(eyes);
                if (eyeToHit.dotProduct(Vec3d.of(side.getVector())) >= 0) continue;
            }
            return new BlockHitResult(hitVec, side, neighbor, false);
        }
        return null;
    }

    private boolean findTarget() {
        List<PlayerEntity> enemies = new ArrayList<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) continue;
            if (Managers.FRIEND.isFriend(p.getName().getString())) continue;
            if (mc.player.squaredDistanceTo(p) > MathHelper.square(targetRange.getValue().floatValue())) continue;
            enemies.add(p);
        }

        if (enemies.isEmpty()) {
            resetTarget();
            return false;
        }

        PlayerEntity bestTarget = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        BlockPos bestPos = null;
        Direction bestDir = null;
        float bestDmg = 0f;

        float highestSelf = 0f;
        float highestDmgReject = 0f;

        int r = (int) Math.ceil(range.getValue().floatValue());
        BlockPos playerPos = mc.player.getBlockPos();

        for (PlayerEntity t : enemies) {
            BlockPos targetBest = null;
            float targetDmg = 0f;
            Direction targetDir = null;

            for (int x = -r; x <= r; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -r; z <= r; z++) {
                        BlockPos foot = playerPos.add(x, y, z);

                        if (mc.player.getEyePos().distanceTo(foot.toCenterPos()) > range.getValue().floatValue()) continue;
                        if (behindWall(foot)) continue;

                        if (airPlace.getValue().equalsIgnoreCase("None") && mc.world.getBlockState(foot.down()).isReplaceable()) {
                            continue;
                        }

                        for (Direction dir : Direction.Type.HORIZONTAL) {
                            BlockPos head = foot.offset(dir);
                            if (!mc.world.getBlockState(foot).isReplaceable() || !mc.world.getBlockState(head).isReplaceable()) continue;

                            if (airPlace.getValue().equalsIgnoreCase("None") && mc.world.getBlockState(head.down()).isReplaceable()) {
                            }

                            Vec3d explosion = Vec3d.ofCenter(head);

                            float dmg = calculateBedDamage(explosion, t, terrainIgnore.getValue());
                            float self = calculateBedDamage(explosion, mc.player, terrainIgnore.getValue());

                            if (self > highestSelf) highestSelf = self;
                            if (dmg > highestDmgReject) highestDmgReject = dmg;

                            if (self > maxSelf.getValue().floatValue()) continue;
                            if (antiSuicide.getValue().floatValue() > 0 && self > PlayerUtils.getHealth(mc.player) - antiSuicide.getValue().floatValue()) continue;

                            float minAllowed = forcePlace.getValue() && PlayerUtils.getHealth(t) <= forceMaxHealth.getValue().floatValue()
                                    ? forceMin.getValue().floatValue()
                                    : minDamage.getValue().floatValue();

                            if (dmg < minAllowed) continue;

                            if (targetBest == null || dmg > targetDmg) {
                                targetBest = foot;
                                targetDmg = dmg;
                                targetDir = dir;
                                lastDamage = dmg;
                                lastSelfDamage = self;
                            }
                        }
                    }
                }
            }

            if (targetBest != null) {
                float score = targetDmg - mc.player.distanceTo(t);
                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = t;
                    bestPos = targetBest;
                    bestDmg = targetDmg;
                    bestDir = targetDir;
                }
            }
        }

        if (bestPos == null || bestDir == null) {
            resetTarget();
            return false;
        }

        if (currentBedFoot == null || !currentBedFoot.equals(bestPos) || currentBedDir != bestDir) {
            currentBedFoot = bestPos;
            currentBedDir = bestDir;
            bedPlaced = false;
        }

        infoDamage = df.format(lastDamage);
        infoSelf = df.format(lastSelfDamage);
        return true;
    }

    public float calculateBedDamage(Vec3d explosionPos, LivingEntity target, boolean ignoreTerrain) {

        float power = 5.0f;
        double dist = Math.sqrt(target.squaredDistanceTo(explosionPos));
        if (dist > power * 2.0) return 0f;

        double exposure = getExposure(explosionPos, target, ignoreTerrain);
        double impact = (1.0 - (dist / (power * 2.0))) * exposure;
        float damage = (float) ((impact * impact + impact) / 2.0 * 7.0 * (double) power + 1.0);

        Difficulty difficulty = mc.world.getDifficulty();
        if (difficulty == Difficulty.EASY) damage = Math.min(damage / 2.0f + 1.0f, damage);
        else if (difficulty == Difficulty.HARD) damage = damage * 3.0f / 2.0f;

        float armor = (float)target.getArmor();
        float toughness = (float)target.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
        damage = calculateArmorReduction(damage, armor, toughness);

        if (target.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int amplifier = target.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier();
            damage -= damage * (0.2f * (amplifier + 1));
        }

        if (damage < 0f) damage = 0f;
        return damage;
    }

    private float calculateArmorReduction(float damage, float armor, float toughness) {
        float f = 2.0f + toughness / 4.0f;
        float g = MathHelper.clamp(armor - damage / f, armor * 0.2f, 20.0f);
        return damage * (1.0f - g / 25.0f);
    }

    private float getExposure(Vec3d source, Entity entity, boolean ignoreTerrain) {
        Box box = entity.getBoundingBox();
        double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double f = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double g = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
        double h = (1.0 - Math.floor(1.0 / e) * e) / 2.0;
        double i = (1.0 - Math.floor(1.0 / f) * f) / 2.0;

        if (!(d >= 0.0) || !(e >= 0.0) || !(f >= 0.0)) return 0.0f;

        int j = 0;
        int k = 0;

        for (float l = 0.0f; l <= 1.0f; l = (float) ((double) l + d)) {
            for (float m = 0.0f; m <= 1.0f; m = (float) ((double) m + e)) {
                for (float n = 0.0f; n <= 1.0f; n = (float) ((double) n + f)) {
                    double o = box.minX + (box.maxX - box.minX) * (double) l;
                    double p = box.minY + (box.maxY - box.minY) * (double) m;
                    double q = box.minZ + (box.maxZ - box.minZ) * (double) n;
                    Vec3d vec3d = new Vec3d(o + g, p + h, q + i);

                    if (ignoreTerrain) {
                        j++;
                        k++;
                        continue;
                    }

                    if (raycast(source, vec3d).getType() == HitResult.Type.MISS) {
                        j++;
                    }
                    k++;
                }
            }
        }
        return (float) j / (float) k;
    }

    private BlockHitResult raycast(Vec3d start, Vec3d end) {
        return mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
    }

    private void resetTarget() {
        currentBedFoot = null;
        currentBedDir = null;
        lastDamage = 0f;
        lastSelfDamage = 0f;
        bedPlaced = false;
    }

    private int findBedSlot() {
        SearchLogic logic = inventoryPlace.getValue() ? SearchLogic.All : SearchLogic.OnlyHotbar;
        for (Item bed : BEDS) {
            Slot slot = Services.INVENTORY.findSlot(logic, bed);
            if (slot != null) {
                return slot.id;
            }
        }
        return -1;
    }

    private boolean behindWall(BlockPos pos) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d center = pos.toCenterPos();
        return mc.world.raycast(new RaycastContext(eyes, center, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() != net.minecraft.util.hit.HitResult.Type.MISS
                && eyes.distanceTo(center) > wallRange.getValue().floatValue();
    }

    @Override
    public String getInfo() {
        return infoDamage + ", " + infoSelf;
    }
}