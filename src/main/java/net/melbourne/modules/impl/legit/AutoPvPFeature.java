package net.melbourne.modules.impl.legit;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.pathfinding.movement.TravelPathfinding;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.settings.types.WhitelistSetting;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.util.Hand;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@FeatureInfo(name = "AutoPvP", category = Category.Legit)
public class AutoPvPFeature extends Feature {

    public ModeSetting version = new ModeSetting("Version", "Switch between 1.8 spam and 1.9+ cooldown combat styles.", "1.8", new String[]{"1.8", "1.9"});
    public ModeSetting targeting = new ModeSetting("Targeting", "Method for selecting targets.", "Normal", new String[]{"Normal", "Advanced"});
    public WhitelistSetting movementFeatures = new WhitelistSetting("Movement", "Select enabled movement behaviors.", WhitelistSetting.Type.CUSTOM, new String[]{"Random Strafes", "Random Jumps", "CombatJump"});
    public NumberSetting baseCps = new NumberSetting("CPS", "Target clicks per second for 1.8 mode.", 12, 1, 20);
    public NumberSetting comboThreshold = new NumberSetting("ComboThreshold", "Amount of hits received before retreating.", 3, 1, 10);
    public BooleanSetting thePit = new BooleanSetting("ThePit", "Automatically drops into the pit and disables utilities.", false);
    public BooleanSetting render = new BooleanSetting("Render", "Draws paths and target boxes.", true);

    private PlayerEntity target = null;
    private List<BlockPos> currentPath = Collections.emptyList();
    private final Random random = new Random();

    private long lastAttackTime, nextAttackDelay;
    private long rodTimer, bowTimer, antiComboTimer, reengageTimer, strafeTimer;
    private long chatTimer = -1;
    private boolean chatWasOpened = false;
    private int hitsTaken = 0;
    private boolean isEscaping = false;
    private boolean isChargingBow = false;
    private Vec3d lastVelocity = Vec3d.ZERO;
    private int strafeDirection = 0;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        handleChatLogic();

        if (mc.currentScreen != null) {
            stopMovement();
            return;
        }

        if (mc.player.getY() > 87.0) {
            target = null;
            if (thePit.getValue()) {
                handlePitDrop();
            } else {
                stopMovement();
            }
            return;
        }

        if (mc.options.forwardKey.isPressed()) mc.player.setSprinting(true);

        if (target != null) {
            if (!target.isAlive() || target.isSpectator() || mc.player.distanceTo(target) > 6.0 || (thePit.getValue() && target.getY() > 105)) {
                target = null;
            }
        }

        if (target == null) {
            target = findTarget();
        }

        if (target == null) {
            stopMovement();
            return;
        }

        if (!thePit.getValue()) handleAntiComboLogic();

        if (!thePit.getValue() && System.currentTimeMillis() < antiComboTimer) {
            isEscaping = true;
            executeSmoothEscape();
            return;
        } else if (isEscaping) {
            handleEscapeRecovery();
            return;
        }

        double dist = mc.player.distanceTo(target);

        if (!thePit.getValue() && dist > 10.0 && dist < 20.0 && findItem(BowItem.class) != -1) {
            handleBowLogic();
        } else {
            if (isChargingBow) {
                mc.player.stopUsingItem();
                isChargingBow = false;
            }
            handleCombat();
            handleAggressiveMovement(dist);
            if (!thePit.getValue() && version.getValue().equals("1.8")) handleAutoRod();
        }
        handleStuckCheck();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (getNull() || mc.currentScreen != null || mc.player.getY() > 87.0) return;

        if (target != null) handleRotation(event.getTickDelta());

        if (!render.getValue()) return;
        if (!currentPath.isEmpty()) {
            Vec3d pathTarget = currentPath.get(currentPath.size() - 1).toCenterPos();
            Renderer3D.renderCircle(event.getContext(), pathTarget, 0.3f, new Color(255, 255, 255, 100));
            Renderer3D.renderLine(event.getContext(), mc.player.getPos(), pathTarget, Color.WHITE, Color.WHITE);
        }

        if (target != null) {
            double dist = mc.player.distanceTo(target);
            Color boxColor = dist <= 3.3 ? new Color(0, 255, 0, 100) : new Color(255, 0, 0, 100);
            Box box = target.getBoundingBox().offset(target.getX() - target.lastRenderX, target.getY() - target.lastRenderY, target.getZ() - target.lastRenderZ);
            Renderer3D.renderBox(event.getContext(), box, boxColor);
        }
    }

    private void handleCombat() {
        if (!isLineOfSightClear(target)) return;

        if (version.getValue().equals("1.9")) {
            if (mc.player.getAttackCooldownProgress(0.5f) >= 0.90f) performAttack();
        } else {
            if (System.currentTimeMillis() - lastAttackTime >= nextAttackDelay) {
                performAttack();
                lastAttackTime = System.currentTimeMillis();
                nextAttackDelay = (long) (1000.0 / (baseCps.getValue().doubleValue() + (random.nextGaussian() * 1.5)));
            }
        }
    }

    private void performAttack() {
        if (mc.player.getMainHandStack().isIn(ItemTags.SWORDS)) {
            double dist = mc.player.distanceTo(target);
            if (dist <= 3.15 + (random.nextDouble() * 0.15)) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                if (movementFeatures.isWhitelistContains("CombatJump") && mc.player.isOnGround()) mc.options.jumpKey.setPressed(true);
            }
        }
    }

    private boolean isLineOfSightClear(PlayerEntity targetEntity) {
        Vec3d start = mc.player.getEyePos();
        Vec3d end = targetEntity.getEyePos();
        BlockHitResult result = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }

    private void handleRotation(float partialTicks) {
        if (target == null) return;

        Vec3d targetPos = target.getLerpedPos(partialTicks);

        double height = target.getBoundingBox().maxY - target.getBoundingBox().minY;

        double centerOffset = height / 2.0;
        double randomOffset = (random.nextDouble() - 0.5) * (height * 0.3);
        double targetY = targetPos.y + centerOffset + randomOffset;

        double dx = targetPos.x - mc.player.getX();
        double dz = targetPos.z - mc.player.getZ();
        double dy = targetY - mc.player.getEyeY();

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        applyLegitRotation(yaw, pitch);
    }

    private void applyLegitRotation(float targetYaw, float targetPitch) {
        float yawDiff = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
        float pitchDiff = targetPitch - mc.player.getPitch();

        double sensitivity = mc.options.getMouseSensitivity().getValue();
        double f = sensitivity * 0.6 + 0.2;
        double gcd = (f * f * f) * 1.2;

        float smooth = 0.8f;
        float absYaw = Math.abs(yawDiff);
        float absPitch = Math.abs(pitchDiff);

        float yawStep = (float) ((1.0 - smooth) * (1.0 + 0.1 * (absYaw / 180.0)));
        float pitchStep = (float) ((1.0 - smooth) * (1.0 + 0.1 * (absPitch / 180.0)));

        float nextYaw = mc.player.getYaw() + (yawDiff * yawStep);
        float nextPitch = mc.player.getPitch() + (pitchDiff * pitchStep);

        float finalYaw = (float) (nextYaw - (nextYaw % gcd));
        float finalPitch = (float) (nextPitch - (nextPitch % gcd));

        mc.player.setYaw(finalYaw);
        mc.player.setPitch(MathHelper.clamp(finalPitch, -90.0f, 90.0f));
    }

    private void smoothRotate(float tYaw, float tPitch) {
        float yDiff = MathHelper.wrapDegrees(tYaw - mc.player.getYaw());
        float pDiff = tPitch - mc.player.getPitch();
        float dist = (float) Math.sqrt(yDiff * yDiff + pDiff * pDiff);
        float acceleration = Math.min(1.0f, dist / 40.0f);
        float speed = (0.25f + (acceleration * 0.55f)) + (random.nextFloat() * 0.12f);
        mc.player.setYaw(mc.player.getYaw() + yDiff * speed);
        mc.player.setPitch(mc.player.getPitch() + pDiff * speed);
    }

    private void handleChatLogic() {
        if (mc.player.getY() >= 110) {
            if (chatTimer == -1 && !chatWasOpened) {
                mc.setScreen(new ChatScreen(""));
                chatTimer = System.currentTimeMillis() + 450 + random.nextInt(100);
                chatWasOpened = true;
            } else if (chatTimer != -1 && System.currentTimeMillis() >= chatTimer) {
                if (mc.currentScreen instanceof ChatScreen) mc.setScreen(null);
                chatTimer = -1;
            }
        } else {
            chatWasOpened = false;
            chatTimer = -1;
        }
    }

    private PlayerEntity findTarget() {
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive() && !p.isSpectator())
                .filter(p -> !thePit.getValue() || p.getY() <= 105)
                .min(Comparator.comparingDouble(this::getTargetScore))
                .orElse(null);
    }

    private double getTargetScore(PlayerEntity player) {
        double score = mc.player.distanceTo(player);

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            net.minecraft.item.ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;

            String name = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath();

            if (name.contains("diamond") || name.contains("netherite")) {
                score += 50.0;
            }
        }

        if (targeting.getValue().equals("Advanced")) {
            float yawDiff = Math.abs(MathHelper.wrapDegrees(getRotationToEntity(player)[0] - mc.player.getYaw()));
            score += (yawDiff * 0.45);
        }

        return score;
    }

    private float[] getRotationToEntity(PlayerEntity entity) {
        double dx = entity.getX() - mc.player.getX();
        double dy = entity.getEyeY() - mc.player.getEyeY();
        double dz = entity.getZ() - mc.player.getZ();
        return new float[]{(float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0), (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))};
    }

    private void handleEscapeRecovery() {
        if (reengageTimer == 0) reengageTimer = System.currentTimeMillis() + (long)(250 + random.nextDouble() * 400);
        if (System.currentTimeMillis() < reengageTimer) {
            mc.options.forwardKey.setPressed(true);
            mc.player.setSprinting(true);
            smoothRotateTowards(target.getPos());
            return;
        }
        isEscaping = false;
        reengageTimer = 0;
    }

    private void handlePitDrop() {
        BlockPos pitEntrance = new BlockPos(0, 113, 0);

        if (!mc.world.getBlockState(pitEntrance).isAir()) {
            stopMovement();
            currentPath = Collections.emptyList();
            return;
        }

        Vec3d center = new Vec3d(0.5, 114, 0.5);
        smoothRotateTowards(center);
        mc.options.forwardKey.setPressed(true);
        mc.player.setSprinting(true);

        if (mc.player.age % 20 == 0) {
            currentPath = TravelPathfinding.findPath(mc.player.getBlockPos(), Collections.singleton(BlockPos.ofFloored(center)), 100);
        }

        mc.options.jumpKey.setPressed(mc.player.horizontalCollision && mc.player.isOnGround());
    }

    private void handleAntiComboLogic() {
        double velocityDiff = mc.player.getVelocity().length() - lastVelocity.length();
        lastVelocity = mc.player.getVelocity();
        if (velocityDiff > 0.08 && mc.player.hurtTime > 0) hitsTaken++;
        if (hitsTaken >= comboThreshold.getValue().intValue()) {
            antiComboTimer = System.currentTimeMillis() + 600 + (long)(random.nextDouble() * 300);
            hitsTaken = 0;
        }
    }

    private void executeSmoothEscape() {
        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0) + 180;
        smoothRotate(targetYaw, 10.0f);
        mc.options.forwardKey.setPressed(true);
        mc.player.setSprinting(true);
    }

    private void handleAggressiveMovement(double dist) {
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);

        boolean isTargetRunningAway = false;
        if (target != null) {
            float angleToTarget = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - target.getYaw()));
            boolean facingSameWay = angleToTarget < 60.0f;
            boolean targetMoving = target.getVelocity().horizontalLengthSquared() > 0.01;
            isTargetRunningAway = facingSameWay && targetMoving;
        }

        if (dist <= 4.0) {
            currentPath = Collections.emptyList();
            mc.options.forwardKey.setPressed(dist > 2.0);
            mc.options.backKey.setPressed(dist < 1.4);
            if (dist > 2.0) mc.player.setSprinting(true);

            if (isTargetRunningAway) {
                mc.player.setSprinting(true);
                mc.options.forwardKey.setPressed(true);
                if (mc.player.isOnGround()) mc.options.jumpKey.setPressed(true);
            } else if (movementFeatures.isWhitelistContains("Random Jumps") || movementFeatures.isWhitelistContains("CombatJump")) {
                if (mc.player.isOnGround() && dist < 3.2 && random.nextDouble() > 0.94) mc.options.jumpKey.setPressed(true);
                else if (mc.player.age % 4 == 0) mc.options.jumpKey.setPressed(false);
            }

            if (!isTargetRunningAway && movementFeatures.isWhitelistContains("Random Strafes") && dist < 5.0) {
                if (System.currentTimeMillis() > strafeTimer) {
                    strafeDirection = random.nextInt(3) - 1;
                    strafeTimer = System.currentTimeMillis() + 400 + random.nextInt(600);
                }
                if (strafeDirection == -1) mc.options.leftKey.setPressed(true);
                if (strafeDirection == 1) mc.options.rightKey.setPressed(true);
            }
        } else {
            if (mc.player.age % 15 == 0) currentPath = TravelPathfinding.findPath(mc.player.getBlockPos(), Collections.singleton(target.getBlockPos()), 100);
            if (!currentPath.isEmpty()) {
                mc.options.forwardKey.setPressed(true);
                mc.player.setSprinting(true);
                if (isTargetRunningAway && mc.player.isOnGround()) mc.options.jumpKey.setPressed(true);
                if (mc.player.getBlockPos().getSquaredDistance(currentPath.get(0).toCenterPos()) < 1.3) currentPath.remove(0);
            }
        }
    }

    private void handleBowLogic() {
        int bowSlot = findItem(BowItem.class);
        mc.player.getInventory().setSelectedSlot(bowSlot);
        if (!isChargingBow) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            bowTimer = System.currentTimeMillis();
            isChargingBow = true;
        }
        long duration = System.currentTimeMillis() - bowTimer;
        float pull = BowItem.getPullProgress((int) (duration / 50));
        predictAndAim(pull);
        if (pull >= 0.94f + (random.nextFloat() * 0.06f)) {
            mc.player.stopUsingItem();
            mc.interactionManager.stopUsingItem(mc.player);
            isChargingBow = false;
            int sword = findItemByTag(ItemTags.SWORDS);
            if (sword != -1) mc.player.getInventory().setSelectedSlot(sword);
        }
    }

    private void predictAndAim(float pull) {
        double travelTime = mc.player.distanceTo(target) / (pull * 3.0);
        double velX = target.getX() - target.lastRenderX;
        double velZ = target.getZ() - target.lastRenderZ;
        Vec3d predicted = target.getPos().add(velX * travelTime * 17, target.getHeight() * 0.5, velZ * travelTime * 17);
        double dx = predicted.x - mc.player.getX();
        double dy = predicted.y - mc.player.getEyeY();
        double dz = predicted.z - mc.player.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float v = pull * 3.0f;
        float g = 0.0506f;
        float pitch = (float) -Math.toDegrees(Math.atan((v * v - Math.sqrt(v * v * v * v - g * (g * hDist * hDist + 2 * dy * v * v))) / (g * hDist)));
        if (Float.isNaN(pitch)) pitch = (float) -Math.toDegrees(Math.atan2(dy, hDist));
        smoothRotate(yaw, pitch);
    }

    private void smoothRotateTowards(Vec3d pos) {
        double dx = pos.x - mc.player.getX();
        double dy = pos.y - mc.player.getEyeY();
        double dz = pos.z - mc.player.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        smoothRotate(yaw, pitch);
    }

    private void handleAutoRod() {
        if (mc.player.distanceTo(target) > 5.0 || mc.player.distanceTo(target) < 2.0) return;
        if (mc.player.hurtTime > 0 && random.nextDouble() > 0.80 && System.currentTimeMillis() > rodTimer) {
            int rod = findItem(FishingRodItem.class);
            int sword = findItemByTag(ItemTags.SWORDS);
            if (rod != -1 && sword != -1) {
                mc.player.getInventory().setSelectedSlot(rod);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                rodTimer = System.currentTimeMillis() + 2000 + random.nextInt(1000);
                new Thread(() -> {
                    try { Thread.sleep(130 + random.nextInt(100)); } catch (InterruptedException ignored) {}
                    mc.player.getInventory().setSelectedSlot(sword);
                }).start();
            }
        }
    }

    private void handleStuckCheck() {
        if (mc.player.horizontalCollision && mc.player.isOnGround()) mc.options.jumpKey.setPressed(true);
    }

    private int findItem(Class<?> clazz) {
        for (int i = 0; i < 9; i++) if (clazz.isInstance(mc.player.getInventory().getStack(i).getItem())) return i;
        return -1;
    }

    private int findItemByTag(net.minecraft.registry.tag.TagKey<net.minecraft.item.Item> tag) {
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isIn(tag)) return i;
        return -1;
    }

    private void stopMovement() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.player.setSprinting(false);
    }
}