package net.melbourne.modules.impl.legit;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Random;

@FeatureInfo(name = "Trigger", category = Category.Legit)
public class TriggerFeature extends Feature {

    public final ModeSetting mode = new ModeSetting("Mode", "Combat version logic.", "1.9", new String[]{"1.8", "1.9"});

    public final NumberSetting CPS = new NumberSetting("CPS", "Target clicks per second.", 10.0, 1.0, 20.0,
            () -> mode.getValue().equals("1.8"));

    public final BooleanSetting Blockhit = new BooleanSetting("Blockhit", "Auto-block every few hits.", true,
            () -> mode.getValue().equals("1.8"));

    public final NumberSetting CooldownProgress = new NumberSetting("Cooldown Progress", "Attack at % charge.", 90.0, 0.0, 100.0,
            () -> mode.getValue().equals("1.9"));

    public final NumberSetting HitRange = new NumberSetting("HitRange", "Maximum attack range.", 3.0, 1.0, 7.0);
    public final BooleanSetting CritTiming = new BooleanSetting("CritTiming", "Only hit while falling.", false);
    public final BooleanSetting RequireWeapon = new BooleanSetting("RequireWeapon", "Sword or axe only.", true);

    private long lastHitTime = 0;
    private long nextDelay = 0;
    private long blockEndTime = 0;
    private int hitCounter = 0;
    private int hitsToBlock = 4;
    private boolean isBlocking = false;
    private final Random random = new Random();

    @Override
    public String getInfo() {
        return mode.getValue();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull() || mc.player.isDead()) return;

        if (isBlocking) {
            if (System.currentTimeMillis() >= blockEndTime) {
                stopBlocking();
            } else {
                return;
            }
        }

        if (mc.player.isUsingItem()) return;

        if (RequireWeapon.getValue()) {
            var stack = mc.player.getMainHandStack();
            if (!stack.isIn(ItemTags.SWORDS) && !stack.isIn(ItemTags.AXES)) return;
        }

        if (mc.crosshairTarget instanceof EntityHitResult res
                && res.getType() == HitResult.Type.ENTITY
                && res.getEntity() instanceof PlayerEntity target) {

            if (target == mc.player || !target.isAlive() || target.isSpectator()) return;
            if (mc.player.squaredDistanceTo(target) > Math.pow(HitRange.getValue().doubleValue(), 2)) return;

            if (mode.getValue().equals("1.9")) {
                if (mc.player.getAttackCooldownProgress(0.5f) < CooldownProgress.getValue().floatValue() / 100f) return;
            }

            if (CritTiming.getValue() && (mc.player.isOnGround() || mc.player.fallDistance <= 0)) return;

            long now = System.currentTimeMillis();
            if (now - lastHitTime >= nextDelay) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);

                lastHitTime = now;
                hitCounter++;

                if (mode.getValue().equals("1.8") && Blockhit.getValue() && hitCounter >= hitsToBlock) {
                    startBlocking();
                } else {
                    calculateNextDelay();
                }
            }
        }
    }

    private void startBlocking() {
        isBlocking = true;
        hitCounter = 0;
        hitsToBlock = 4 + random.nextInt(2);
        long blockDuration = 350 + random.nextInt(101); // 0.35s to 0.45s
        blockEndTime = System.currentTimeMillis() + blockDuration;

        mc.options.useKey.setPressed(true);
        mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
    }

    private void stopBlocking() {
        isBlocking = false;
        mc.options.useKey.setPressed(false);
        calculateNextDelay();
    }

    private void calculateNextDelay() {
        if (mode.getValue().equals("1.8")) {
            double baseCps = CPS.getValue().doubleValue();
            double randomCps = baseCps + (random.nextDouble() * 6.0) - 3.0; // +/- 3 variation
            nextDelay = (long) (1000.0 / Math.max(1.0, randomCps));
        } else {
            nextDelay = 10;
        }
    }

    public boolean getNull() {
        return mc.player == null || mc.world == null;
    }
}