package net.melbourne.modules.impl.movement;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.block.hole.HoleUtils;
import net.minecraft.util.math.BlockPos;

@FeatureInfo(name = "AntiPop", category = Category.Movement)
public class AntiPopFeature extends Feature {
    public final NumberSetting maxPackets = new NumberSetting("MaxPackets", "Maximum number of tick speed boost packets", 15, 1, 50);
    public final NumberSetting timerIntensity = new NumberSetting("TimerIntensity", "How much to multiply game speed by when boosting", 3.0f, 1.0f, 10.0f);
    public final NumberSetting chargeSpeed = new NumberSetting("ChargeSpeed", "Rate at which packet budget recharges per tick", 0.25f, 0.01f, 2.0f);
    public final NumberSetting chargeAmount = new NumberSetting("ChargeAmount", "Number of packets added per full charge cycle", 5, 1, 20);
    public final NumberSetting healthThreshold = new NumberSetting("HealthThreshold", "Health below which AntiPop activates", 6.0f, 1.0f, 20.0f);
    public final BooleanSetting autoDisable = new BooleanSetting("AutoDisable", "Disable module when packet budget is depleted", false);

    private int packetsLeft = 0;
    private float chargeProgress = 0f;
    private boolean isUsingTimer = false;

    @Override
    public void onEnable() {
        packetsLeft = maxPackets.getValue().intValue();
        chargeProgress = 0f;
        isUsingTimer = false;
        resetTimerIfNotInUse();
    }

    @Override
    public void onDisable() {
        resetTimerIfNotInUse();
        packetsLeft = 0;
        chargeProgress = 0f;
        isUsingTimer = false;
    }

    @SubscribeEvent(priority = 50)
    public void onTick(TickEvent event) {
        if (getNull() || mc.player.isSpectator()) {
            if (autoDisable.getValue()) setToggled(false);
            return;
        }

        Feature holeSnap = Managers.FEATURE.getFeatureByName("Holesnap");
        if (holeSnap != null && holeSnap.isEnabled()) {
            resetTimerIfNotInUse();
            isUsingTimer = false;
            recharge();
            return;
        }

        if (isInHole() || mc.player.isSubmergedInWater() || mc.player.isInLava()) {
            resetTimerIfNotInUse();
            isUsingTimer = false;
            recharge();
            return;
        }

        Feature tickShift = Managers.FEATURE.getFeatureByName("TickShift");
        boolean tickShiftUsing = tickShift != null && tickShift.isEnabled() && ((TickShiftFeature) tickShift).isTimering();

        if (tickShiftUsing) {
            isUsingTimer = false;
            recharge();
            return;
        }

        boolean isMoving = isPlayerMoving();
        boolean lowHealth = mc.player.getHealth() <= healthThreshold.getValue().floatValue();

        isUsingTimer = false;

        if (lowHealth && isMoving && packetsLeft > 0) {
            Services.WORLD.setTimerMultiplier(timerIntensity.getValue().floatValue());
            packetsLeft--;
            chargeProgress = 0f;
            isUsingTimer = true;

            if (packetsLeft <= 0 && autoDisable.getValue()) {
                setEnabled(false);
            }
        } else {
            if (!isUsingTimer) {
                resetTimerIfNotInUse();
            }
            if (!isMoving || !lowHealth) {
                recharge();
            }
        }
    }

    public boolean isUsingPackets() {
        return isUsingTimer;
    }

    public boolean isUsingTimer() {
        return isUsingTimer;
    }

    private void resetTimerIfNotInUse() {
        Feature tickShift = Managers.FEATURE.getFeatureByName("TickShift");
        if (tickShift == null || !tickShift.isEnabled() || !((TickShiftFeature) tickShift).isTimering()) {
            Services.WORLD.resetTimerMultiplier();
        }
    }

    private boolean isInHole() {
        BlockPos playerPos = mc.player.getBlockPos();
        HoleUtils.Hole singleHole = HoleUtils.getSingleHole(playerPos, 1);
        if (singleHole != null) return true;
        HoleUtils.Hole doubleHole = HoleUtils.getDoubleHole(playerPos, 1);
        return doubleHole != null;
    }

    private void recharge() {
        chargeProgress += chargeSpeed.getValue().floatValue();
        while (chargeProgress >= 1.0f) {
            chargeProgress -= 1.0f;
            packetsLeft = Math.min(packetsLeft + chargeAmount.getValue().intValue(), maxPackets.getValue().intValue());
        }
    }

    private boolean isPlayerMoving() {
        return mc.options.forwardKey.isPressed() ||
                mc.options.backKey.isPressed() ||
                mc.options.leftKey.isPressed() ||
                mc.options.rightKey.isPressed() ||
                mc.options.jumpKey.isPressed();
    }

    @Override
    public String getInfo() {
        return "" + packetsLeft;
    }
}