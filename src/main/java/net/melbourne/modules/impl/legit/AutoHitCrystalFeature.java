package net.melbourne.modules.impl.legit;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

@FeatureInfo(name = "AutoHitCrystal", category = Category.Legit)
public class AutoHitCrystalFeature extends Feature {

    public final NumberSetting delay = new NumberSetting("Delay", "", 30.0, 0.0, 200.0);
    public final ModeSetting mode = new ModeSetting("Crystal", "Single Tap", "Single Tap", new String[]{"None", "Single Tap", "Double Tap"});
    public final ModeSetting placeMode = new ModeSetting("Place Mode", "Silent", "Silent", new String[]{"Silent", "Manual"});
    public final BooleanSetting silent = new BooleanSetting("Silent", "", false);
    public final BooleanSetting perfectTiming = new BooleanSetting("Perfect Timing", "", false);
    public final BooleanSetting pauseOnKill = new BooleanSetting("Pause On Kill", "", false);
    public final BooleanSetting aimAssist = new BooleanSetting("AimAssist", "", true);

    private long lastActionTime = 0;
    private int progress = 0;
    private final Random random = new Random();

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null || !mc.isWindowFocused()) return;
        if (pauseOnKill.getValue() && mc.world.getPlayers().stream().noneMatch(p -> p.isAlive() && !p.isSpectator())) return;

        PlayerEntity playerEntity = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive() && !p.isSpectator())
                .filter(p -> mc.player.squaredDistanceTo(p) <= 100)
                .findFirst().orElse(null);

        switch (progress) {
            case 0:
                Integer obsidianSlot = findHotbarItem(Items.OBSIDIAN);
                if (obsidianSlot != null) {
                    if (placeMode.getValue().equals("Manual")) {
                        mc.player.getInventory().setSelectedSlot(obsidianSlot);
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(obsidianSlot));
                    }
                    nextProgress();
                }
                break;
            case 1:
                if (mc.crosshairTarget instanceof BlockHitResult blockHit && !mc.world.getBlockState(blockHit.getBlockPos()).isAir()) {
                    BlockPos placePos = blockHit.getBlockPos().add(blockHit.getSide().getOffsetX(), blockHit.getSide().getOffsetY(), blockHit.getSide().getOffsetZ());
                    if (mc.world.getBlockState(blockHit.getBlockPos()).getBlock() == Blocks.OBSIDIAN) {
                        nextProgress();
                        break;
                    }
                    if (placeMode.getValue().equals("Manual")) {
                        Integer slot = findHotbarItem(Items.OBSIDIAN);
                        if (slot != null) {
                            mc.player.getInventory().setSelectedSlot(slot);
                            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                        }
                    }
                    if (aimAssist.getValue()) smoothAimAt(new Vec3d(placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5), 0.3f);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    nextProgress();
                }
                break;
            case 2:
                Integer crystalSlot = findHotbarItem(Items.END_CRYSTAL);
                if (crystalSlot != null) {
                    if (placeMode.getValue().equals("Manual")) {
                        mc.player.getInventory().setSelectedSlot(crystalSlot);
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(crystalSlot));
                    }
                    nextProgress();
                }
                break;
            case 3:
                if (mc.crosshairTarget instanceof BlockHitResult blockHit && mc.world.getBlockState(blockHit.getBlockPos()).getBlock() == Blocks.OBSIDIAN) {
                    if (placeMode.getValue().equals("Manual")) {
                        Integer slot = findHotbarItem(Items.END_CRYSTAL);
                        if (slot != null) {
                            mc.player.getInventory().setSelectedSlot(slot);
                            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                        }
                    }
                    if (aimAssist.getValue()) smoothAimAt(new Vec3d(blockHit.getBlockPos().getX() + 0.5, blockHit.getBlockPos().getY() + 0.5, blockHit.getBlockPos().getZ() + 0.5), 0.3f);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    nextProgress();
                }
                break;
            case 4:
            case 6:
                if (perfectTiming.getValue() && playerEntity != null && playerEntity.hurtTime != 0) break;
                handleCrystalHit();
                break;
            case 5:
                if (mc.crosshairTarget instanceof BlockHitResult blockHit && mc.world.getBlockState(blockHit.getBlockPos()).getBlock() == Blocks.OBSIDIAN) {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    nextProgress();
                }
                break;
        }
    }

    private void handleCrystalHit() {
        HitResult hit = mc.crosshairTarget;
        if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hit;
            if (entityHit.getEntity() instanceof EndCrystalEntity) {
                if (aimAssist.getValue()) smoothAimAt(entityHit.getEntity().getPos().add(0, entityHit.getEntity().getHeight() * 0.5, 0), 0.3f);
                mc.interactionManager.attackEntity(mc.player, entityHit.getEntity());
                mc.player.swingHand(Hand.MAIN_HAND);
                if (mode.getValue().equals("Double Tap")) nextProgress();
                else progress = 0;
            }
        }
    }

    private void nextProgress() {
        progress++;
        lastActionTime = System.currentTimeMillis();
    }

    private Integer findHotbarItem(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return null;
    }

    private void smoothAimAt(Vec3d targetPos, float strength) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d diff = targetPos.subtract(eyePos);
        double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, distXZ)));

        float yawDelta = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
        float pitchDelta = targetPitch - mc.player.getPitch();

        float fraction = strength * (0.5f + random.nextFloat() * 0.5f);

        float smoothYaw = mc.player.getYaw() + yawDelta * fraction + (random.nextFloat() - 0.5f) * 0.5f;
        float smoothPitch = mc.player.getPitch() + pitchDelta * fraction + (random.nextFloat() - 0.5f) * 0.5f;

        mc.player.setYaw(smoothYaw);
        mc.player.setPitch(MathHelper.clamp(smoothPitch, -90f, 90f));
        mc.player.headYaw = smoothYaw;
        mc.player.bodyYaw = smoothYaw;
    }
}
