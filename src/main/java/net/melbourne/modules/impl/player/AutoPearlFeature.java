package net.melbourne.modules.impl.player;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@FeatureInfo(name = "AutoPearl", category = Category.Player)
public class AutoPearlFeature extends Feature {

    private final Set<Integer> processedPearls = new HashSet<>();
    private final Map<Integer, Integer> pendingPearls = new HashMap<>();
    private String targetPlayerName = null;
    private double pearlLandTime = 0.0;
    private long lastThrowTime = 0;
    private double ourPearlLandTime = 0.0;
    private long ourPearlThrowTime = 0;
    private int ourPearlId = -1;
    private Vec3d ourPearlLanding = null;

    @Override
    public void onEnable() {
        processedPearls.clear();
        pendingPearls.clear();
        targetPlayerName = null;
        pearlLandTime = 0.0;
        lastThrowTime = 0;
        ourPearlLandTime = 0.0;
        ourPearlThrowTime = 0;
        ourPearlId = -1;
        ourPearlLanding = null;
    }

    @Override
    public void onDisable() {
        processedPearls.clear();
        pendingPearls.clear();
        targetPlayerName = null;
        pearlLandTime = 0.0;
        lastThrowTime = 0;
        ourPearlLandTime = 0.0;
        ourPearlThrowTime = 0;
        ourPearlId = -1;
        ourPearlLanding = null;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull() || mc.world == null || mc.player == null || mc.interactionManager == null) {
            targetPlayerName = null;
            pendingPearls.clear();
            ourPearlLandTime = 0.0;
            ourPearlThrowTime = 0;
            ourPearlId = -1;
            ourPearlLanding = null;
            return;
        }

        processedPearls.removeIf(id -> mc.world.getEntityById(id) == null);
        pendingPearls.entrySet().removeIf(entry -> mc.world.getEntityById(entry.getKey()) == null);
        if (ourPearlId != -1 && mc.world.getEntityById(ourPearlId) == null) {
            ourPearlId = -1;
            ourPearlLanding = null;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EnderPearlEntity pearl)) continue;
            if (processedPearls.contains(pearl.getId())) continue;

            Entity owner = pearl.getOwner();
            if (owner == null) {
                int retries = pendingPearls.getOrDefault(pearl.getId(), 0);
                if (retries < 5) {
                    pendingPearls.put(pearl.getId(), retries + 1);
                    continue;
                } else {
                    processedPearls.add(pearl.getId());
                    continue;
                }
            }

            if (owner == mc.player) {
                if (ourPearlThrowTime != 0 && System.currentTimeMillis() - ourPearlThrowTime < 100) {
                    ourPearlId = pearl.getId();
                }
                processedPearls.add(pearl.getId());
                continue;
            }

            if (!(owner instanceof AbstractClientPlayerEntity targetPlayer)) {
                processedPearls.add(pearl.getId());
                continue;
            }

            if (Managers.FRIEND.isFriend(targetPlayer.getName().getString())) {
                processedPearls.add(pearl.getId());
                continue;
            }

            double currentDist = mc.player.getPos().distanceTo(owner.getPos());
            if (currentDist > 15.0) {
                processedPearls.add(pearl.getId());
                continue;
            }

            processedPearls.add(pearl.getId());
            pendingPearls.remove(pearl.getId());

            Vec3d landing = predictLanding(pearl.getPos(), pearl.getVelocity());
            if (landing == null) continue;

            double landDist = mc.player.getPos().distanceTo(landing);
            if (landDist > currentDist) {
                if (landing.distanceTo(owner.getPos()) <= 2.0) {
                    continue;
                }

                int desiredTicks = calculateLandTicks(pearl.getPos(), pearl.getVelocity());
                float[] yawPitch = findBestYawPitch(desiredTicks, landing);
                if (yawPitch != null) {
                    targetPlayerName = targetPlayer.getName().getString();
                    pearlLandTime = desiredTicks / 20.0;
                    throwPearlTo(yawPitch[0], yawPitch[1]);
                    lastThrowTime = System.currentTimeMillis();
                    ourPearlThrowTime = lastThrowTime;
                    ourPearlLanding = predictLanding(mc.player.getEyePos(), getThrowVelocity(yawPitch[0], yawPitch[1]));
                    ourPearlLandTime = calculateLandTicks(mc.player.getEyePos(), getThrowVelocity(yawPitch[0], yawPitch[1])) / 20.0;
                    targetPlayerName = null;
                    pearlLandTime = 0.0;
                }
            }
        }
    }

    private Vec3d predictLanding(Vec3d pos, Vec3d vel) {
        for (int tick = 0; tick < 300; tick++) {
            Vec3d nextPos = pos.add(vel);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(pos, nextPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit.getPos();
            }
            pos = nextPos;
            vel = vel.multiply(0.99).add(0, -0.03, 0);
        }
        return null;
    }

    private int calculateLandTicks(Vec3d pos, Vec3d vel) {
        for (int tick = 0; tick < 300; tick++) {
            Vec3d nextPos = pos.add(vel);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(pos, nextPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                return tick + 1;
            }
            pos = nextPos;
            vel = vel.multiply(0.99).add(0, -0.03, 0);
        }
        return -1;
    }

    private float[] findBestYawPitch(int desiredTicks, Vec3d targetLanding) {
        ClientPlayerEntity player = mc.player;
        double bestError = Double.MAX_VALUE;
        float bestPitch = 0.0f;
        double bestDistToTarget = Double.MAX_VALUE;
        Vec3d playerPos = player.getPos();
        float targetYaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(targetLanding.z - playerPos.z, targetLanding.x - playerPos.x)) - 90.0f);

        for (float p = -90.0f; p <= 90.0f; p += 0.5f) {
            Vec3d vel = getThrowVelocity(targetYaw, p);
            Vec3d start = player.getEyePos();
            int ticks = calculateLandTicks(start, vel);
            if (ticks == -1) continue;

            double error = Math.abs(ticks - desiredTicks);
            Vec3d land = predictLanding(start, vel);
            if (land == null) continue;

            double distToTarget = land.distanceTo(targetLanding);
            if (error < bestError || (error == bestError && distToTarget < bestDistToTarget)) {
                bestError = error;
                bestPitch = p;
                bestDistToTarget = distToTarget;
            }
        }

        if (bestError > 5) {
            return null;
        }
        return new float[]{targetYaw, bestPitch};
    }

    private Vec3d getThrowVelocity(float yaw, float pitch) {
        float yawRad = yaw * (float) Math.PI / 180.0F;
        float pitchRad = pitch * (float) Math.PI / 180.0F;
        float cosPitch = MathHelper.cos(pitchRad);
        float sinPitch = MathHelper.sin(pitchRad);
        float cosYaw = MathHelper.cos(yawRad);
        float sinYaw = MathHelper.sin(yawRad);
        float h = cosYaw * cosPitch;
        float i = sinYaw * cosPitch;
        float j = -sinPitch;
        return new Vec3d(h, j, i).multiply(1.5);
    }

    private void throwPearlTo(float yaw, float pitch) {
        ClientPlayerEntity player = mc.player;
        ItemStack pearlStack = findInventoryItemStack(Items.ENDER_PEARL);
        if (pearlStack.isEmpty() || player.getItemCooldownManager().isCoolingDown(pearlStack)) {
            return;
        }

        float originalYaw = player.getYaw();
        float originalPitch = player.getPitch();

        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround(), true));

        int originalSlot = player.getInventory().getSelectedSlot();
        int pearlSlot = findHotbarItemSlot(Items.ENDER_PEARL);
        if (pearlSlot == -1) {
            pearlSlot = silentSwapPearl();
            if (pearlSlot == -1) {
                return;
            }
        }

        player.getInventory().setSelectedSlot(pearlSlot);
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pearlSlot));

        player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, yaw, pitch));
        player.swingHand(Hand.MAIN_HAND);

        player.getInventory().setSelectedSlot(originalSlot);
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));

        restoreInventory();
        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(originalYaw, originalPitch, player.isOnGround(), true));
    }

    private int findHotbarItemSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private ItemStack findInventoryItemStack(net.minecraft.item.Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private int silentSwapPearl() {
        int targetSlot = -1;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) {
                targetSlot = invIndexToHandlerId(i);
                break;
            }
        }
        if (targetSlot == -1) return -1;

        int hotbarSlot = findEmptyHotbar();
        if (hotbarSlot == -1) hotbarSlot = 8;
        int hotbarId = invIndexToHandlerId(hotbarSlot);

        clickSlot(hotbarId, SlotActionType.PICKUP);
        clickSlot(targetSlot, SlotActionType.PICKUP);
        clickSlot(hotbarId, SlotActionType.PICKUP);

        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            int emptySlot = findEmptyHandlerSlot();
            clickSlot(emptySlot != -1 ? emptySlot : targetSlot, SlotActionType.PICKUP);
        }
        return hotbarSlot;
    }

    private void restoreInventory() {
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            int emptySlot = findEmptyHandlerSlot();
            if (emptySlot != -1) clickSlot(emptySlot, SlotActionType.PICKUP);
        }
    }

    private int findEmptyHandlerSlot() {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return invIndexToHandlerId(i);
        }
        return -1;
    }

    private static int invIndexToHandlerId(int index) {
        if (index >= 0 && index < 9) return 36 + index;
        if (index >= 9 && index < 36) return index;
        if (index == 40) return 45;
        return -1;
    }

    private void clickSlot(int slot, SlotActionType type) {
        if (mc.player.currentScreenHandler != null) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, type, mc.player);
        }
    }

    private int findEmptyHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    @Override
    public String getInfo() {
        return targetPlayerName != null ? targetPlayerName : "";
    }
}