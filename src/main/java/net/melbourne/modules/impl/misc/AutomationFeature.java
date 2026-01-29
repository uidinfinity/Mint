package net.melbourne.modules.impl.misc;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.movement.HitboxDesyncFeature;
import net.melbourne.modules.impl.movement.StepFeature;
import net.melbourne.modules.impl.movement.TickShiftFeature;
import net.melbourne.modules.impl.player.FakeLagFeature;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.block.hole.HoleUtils;
import net.minecraft.util.math.BlockPos;

@FeatureInfo(name = "Automation", category = Category.Misc)
public class AutomationFeature extends Feature {

    public final BooleanSetting lagOnStep = new BooleanSetting("LagOnStep", "Pulses FakeLag when stepping", true);
    public final NumberSetting lagTime = new NumberSetting("LagTime", "How long to pulse FakeLag (ms)", 500, 50, 3000, () -> lagOnStep.getValue());
    public final BooleanSetting autoHitboxDesync = new BooleanSetting("AutoHitboxDesync", "Toggles HitboxDesync when in a double or quad hole", true);

    public final BooleanSetting tickShiftOnStep = new BooleanSetting("TickShiftOnStep", "Enables TickShift while Step is enabled", true);
    public final BooleanSetting tickShiftOnlyWhileStepping = new BooleanSetting("TickShiftOnlyWhileStepping", "If on, disables TickShift when Step disables", true, () -> tickShiftOnStep.getValue());

    private int lagTicksLeft = 0;
    private boolean wasStepEnabled = false;
    private BlockPos pos = null;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        // fix
        if (!this.isEnabled()) return;

        if (getNull()) {
            disableFakeLagIfActive();
            handleTickShiftOnStep(false);
            pos = null;
            return;
        }

        FakeLagFeature fakeLag = Managers.FEATURE.getFeatureFromClass(FakeLagFeature.class);
        StepFeature step = Managers.FEATURE.getFeatureFromClass(StepFeature.class);
        TickShiftFeature tickShift = Managers.FEATURE.getFeatureFromClass(TickShiftFeature.class);

        boolean stepCurrentlyEnabled = step != null && step.isEnabled();

        if (lagOnStep.getValue() && fakeLag != null && step != null) {
            if (stepCurrentlyEnabled && !wasStepEnabled) {
                if (!fakeLag.isEnabled()) fakeLag.setEnabled(true);
                lagTicksLeft = msToTicks(lagTime.getValue().intValue());
            }

            if (lagTicksLeft > 0) {
                lagTicksLeft--;
                if (lagTicksLeft <= 0) {
                    fakeLag.setEnabled(false);
                }
            }
            if (!stepCurrentlyEnabled && fakeLag.isEnabled() && lagTicksLeft > 0) {
                fakeLag.setEnabled(false);
                lagTicksLeft = 0;
            }
        } else {
            // check to make sure it disables
            disableFakeLagIfActive();
        }

        handleTickShiftOnStep(stepCurrentlyEnabled);

        wasStepEnabled = stepCurrentlyEnabled;

        HitboxDesyncFeature hitboxDesync = Managers.FEATURE.getFeatureFromClass(HitboxDesyncFeature.class);

        if (autoHitboxDesync.getValue() && hitboxDesync != null) {
            HoleUtils.Hole doubleHole = HoleUtils.getDoubleHoleForPlayer(mc.player);
            HoleUtils.Hole quadHole = HoleUtils.getQuadHoleForPlayer(mc.player);

            HoleUtils.Hole currentHole = doubleHole != null ? doubleHole : quadHole;

            if (currentHole != null) {
                BlockPos currentHolePos = HoleUtils.getFirst(currentHole);
                if (currentHolePos != null && mc.player.isOnGround() && !currentHolePos.equals(pos) && !hitboxDesync.isEnabled()) {
                    hitboxDesync.setEnabled(true);
                    pos = currentHolePos;
                }
            } else {
                pos = null;
            }
        } else {
            pos = null;
        }
    }

    private void handleTickShiftOnStep(boolean stepEnabled) {
        if (!tickShiftOnStep.getValue()) return;

        TickShiftFeature tickShift = Managers.FEATURE.getFeatureFromClass(TickShiftFeature.class);
        if (tickShift == null) return;

        if (stepEnabled) {
            if (!tickShift.isEnabled()) tickShift.setEnabled(true);
            return;
        }

        if (tickShiftOnlyWhileStepping.getValue() && tickShift.isEnabled()) {
            tickShift.setEnabled(false);
        }
    }

    private void disableFakeLagIfActive() {
        FakeLagFeature fakeLag = Managers.FEATURE.getFeatureFromClass(FakeLagFeature.class);
        if (fakeLag != null && fakeLag.isEnabled()) {
            fakeLag.setEnabled(false);
        }
        lagTicksLeft = 0;
        wasStepEnabled = false;
    }

    @Override
    public void onEnable() {
        lagTicksLeft = 0;
        wasStepEnabled = false;
        pos = null;
    }

    @Override
    public void onDisable() {
        disableFakeLagIfActive();
        handleTickShiftOnStep(false);
        pos = null;
    }

    private int msToTicks(int ms) {
        return Math.max(1, ms / 50);
    }
}
