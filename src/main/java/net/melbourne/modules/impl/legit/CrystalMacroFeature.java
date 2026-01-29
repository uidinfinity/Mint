package net.melbourne.modules.impl.legit;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.Set;

@FeatureInfo(name = "CrystalMacro", category = Category.Legit)
public class CrystalMacroFeature extends Feature {

    private final NumberSetting activateKey = new NumberSetting("ActivateKey", "Key to activate", 1, -1, 400);
    private final NumberSetting placeDelay = new NumberSetting("PlaceDelay", "Delay between placing crystals", 0.0, 0.0, 20.0);
    private final NumberSetting breakDelay = new NumberSetting("BreakDelay", "Delay between breaking crystals", 0.0, 0.0, 20.0);
    private final BooleanSetting stopOnKill = new BooleanSetting("StopOnKill", "Pause macro when player dies", true);
    private final BooleanSetting placeObsidianIfMissing = new BooleanSetting("PlaceObsidianIfMissing", "Place obsidian if missing", true);

    private int placeDelayCounter;
    private int breakDelayCounter;

    private final Set<PlayerEntity> deadPlayers = new HashSet<>();
    private boolean paused = false;
    private long resumeTime = 0;

    @Override
    public void onDisable() {
        resetCounters();
        deadPlayers.clear();
        paused = false;
        resumeTime = 0;
    }

    @Override
    public void onEnable() {
        resetCounters();
        deadPlayers.clear();
        paused = false;
        resumeTime = 0;
    }

    private void resetCounters() {
        placeDelayCounter = 0;
        breakDelayCounter = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.currentScreen != null) return;

        updateCounters();

        if (paused && System.currentTimeMillis() >= resumeTime) {
            paused = false;
        }

        if (paused) return;
        if (!isKeyActive()) return;
        if (mc.player.isUsingItem()) return;
        if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;

        if (stopOnKill.getValue() && checkForDeadPlayers()) {
            paused = true;
            resumeTime = System.currentTimeMillis() + 5000;
            return;
        }

        HitResult target = mc.crosshairTarget;
        if (target instanceof BlockHitResult blockHit) {
            if (placeDelayCounter > 0) return;

            BlockPos pos = blockHit.getBlockPos();
            boolean isObsidianOrBedrock = mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN ||
                    mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;

            if (!isObsidianOrBedrock && placeObsidianIfMissing.getValue()) {
                int obsidianSlot = findObsidianSlot();
                int crystalSlot = findCrystalSlot();
                if (obsidianSlot == -1 || crystalSlot == -1) return;

                mc.player.getInventory().setSelectedSlot(obsidianSlot);
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(obsidianSlot));
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
                mc.player.swingHand(Hand.MAIN_HAND);
                placeDelayCounter = placeDelay.getValue().intValue();

                mc.player.getInventory().setSelectedSlot(crystalSlot);
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(crystalSlot));
                return;
            }

            if (isObsidianOrBedrock && canPlaceCrystal(pos)) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
                placeDelayCounter = placeDelay.getValue().intValue();
            }
        } else if (target instanceof EntityHitResult entityHit) {
            if (breakDelayCounter > 0) return;

            Entity entity = entityHit.getEntity();
            if (!(entity instanceof EndCrystalEntity) && !(entity instanceof SlimeEntity)) return;

            mc.interactionManager.attackEntity(mc.player, entity);
            mc.player.swingHand(Hand.MAIN_HAND);
            breakDelayCounter = breakDelay.getValue().intValue();
        }
    }

    private void updateCounters() {
        if (placeDelayCounter > 0) placeDelayCounter--;
        if (breakDelayCounter > 0) breakDelayCounter--;
    }

    private boolean isKeyActive() {
        int key = (int) activateKey.getValue();
        return key == -1 || mc.options.attackKey.isPressed();
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        BlockPos up = pos.up();
        if (!mc.world.isAir(up)) return false;

        Box box = new Box(up);
        return mc.world.getOtherEntities(null, box).isEmpty();
    }

    private boolean checkForDeadPlayers() {
        if (mc.world == null) return false;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            if ((player.isDead() || player.getHealth() <= 0) && !deadPlayers.contains(player)) {
                deadPlayers.add(player);
                return true;
            }
        }

        deadPlayers.removeIf(p -> !p.isDead() && p.getHealth() > 0);
        return false;
    }

    private int findObsidianSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.OBSIDIAN)) return i;
        }
        return -1;
    }

    private int findCrystalSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) return i;
        }
        return -1;
    }
}
