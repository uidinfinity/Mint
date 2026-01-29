package net.melbourne.modules.impl.movement;

import lombok.Getter;
import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderHudEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.client.AntiCheatFeature;
import net.melbourne.modules.impl.client.HudFeature;
import net.melbourne.modules.impl.misc.RoboticsFeature;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.block.hole.HoleUtils;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.util.math.BlockPos;

import java.awt.*;

@FeatureInfo(name = "TickShift", category = Category.Movement)
public class TickShiftFeature extends Feature {
    public final NumberSetting maxPackets = new NumberSetting("MaxPackets", "MaxPackets", 15, 1, 50);
    public final NumberSetting timerIntensity = new NumberSetting("TimerIntensity", "Intensity", 3.0f, 1.0f, 10.0f);
    public final NumberSetting chargeSpeed = new NumberSetting("ChargeSpeed", "Speed", 0.25f, 0.01f, 2.0f);
    public final NumberSetting chargeAmount = new NumberSetting("ChargeAmount", "Amount", 5, 1, 20);
    public final BooleanSetting autoDisable = new BooleanSetting("AutoDisable", "AutoDisable", false);
    public BooleanSetting render = new BooleanSetting("Render", "Render", true);

    private int packetsLeft = 0;
    private float chargeProgress = 0f;
    @Getter
    private boolean timering = false;
    private boolean physics = true;

    private boolean isRoboticsSlave() {
        RoboticsFeature r = Managers.FEATURE.getFeatureFromClass(RoboticsFeature.class);
        return r != null && r.isEnabled() && r.role.getValue().equals("Server");
    }

    @Override
    public void onEnable() {
        packetsLeft = maxPackets.getValue().intValue();
        chargeProgress = 0f;
        timering = false;
        physics = true;
        resetTimer();
    }

    @Override
    public void onDisable() {
        resetTimer();
        packetsLeft = 0;
        chargeProgress = 0f;
        timering = false;
        physics = true;
    }

    @SubscribeEvent(priority = 100)
    public void onTick(TickEvent event) {
        if (getNull() || mc.player.isSpectator() || isRoboticsSlave()) return;

        Feature holeSnap = Managers.FEATURE.getFeatureByName("Holesnap");
        if (holeSnap != null && holeSnap.isEnabled()) {
            resetTimer();
            timering = false;
            recharge();
            return;
        }

        if (isInHole() || mc.player.isSubmergedInWater() || mc.player.isInLava()) {
            resetTimer();
            timering = false;
            recharge();
            return;
        }

        boolean isMoving = isPlayerMoving();
        Feature antiPop = Managers.FEATURE.getFeatureByName("AntiPop");
        boolean antiPopActive = antiPop != null && antiPop.isEnabled() && ((AntiPopFeature) antiPop).isUsingTimer();

        timering = false;

        if (isMoving && packetsLeft > 0 && !antiPopActive) {
            AntiCheatFeature antiCheat = (AntiCheatFeature) Managers.FEATURE.getFeatureByName("AntiCheat");
            String mode = antiCheat != null && antiCheat.isEnabled() ? antiCheat.timerMode.getValue() : "Normal";

            if (mode.equals("Physics")) {
                if (physics) {
                    physics = false;
                    for (int i = 0; i < timerIntensity.getValue().intValue(); i++) mc.player.tick();
                }
            } else {
                Services.WORLD.setTimerMultiplier(timerIntensity.getValue().floatValue());
            }

            packetsLeft--;
            chargeProgress = 0f;
            timering = true;

            if (packetsLeft <= 0 && autoDisable.getValue()) setToggled(false);
        } else {
            resetTimer();
            if (!isMoving || antiPopActive) recharge();
        }

        if (!isMoving) physics = true;
    }

    private void resetTimer() {
        Feature antiPop = Managers.FEATURE.getFeatureByName("AntiPop");
        if (antiPop == null || !antiPop.isEnabled() || !((AntiPopFeature) antiPop).isUsingTimer()) {
            Services.WORLD.resetTimerMultiplier();
        }
    }

    private boolean isInHole() {
        BlockPos playerPos = mc.player.getBlockPos();
        return HoleUtils.getSingleHole(playerPos, 1) != null || HoleUtils.getDoubleHole(playerPos, 1) != null;
    }

    private void recharge() {
        chargeProgress += chargeSpeed.getValue().floatValue();
        while (chargeProgress >= 1.0f) {
            chargeProgress -= 1.0f;
            packetsLeft = Math.min(packetsLeft + chargeAmount.getValue().intValue(), maxPackets.getValue().intValue());
        }
    }

    private boolean isPlayerMoving() {
        return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.jumpKey.isPressed();
    }

    @SubscribeEvent
    public void renderHud(RenderHudEvent event) {
        if (getNull() || !render.getValue()) return;
        float width = event.getContext().getScaledWindowWidth();
        float height = event.getContext().getScaledWindowHeight();
        float barWidth = 20f, barHeight = 2f, baseY = (height + 10) / 2f;
        HudFeature hud = (HudFeature) Managers.FEATURE.getFeatureFromClass(HudFeature.class);
        float finalY = (hud != null && hud.isHealthBarEnabled()) ? baseY + 4f : baseY;
        Renderer2D.renderQuad(event.getContext(), (width - barWidth) / 2f, finalY, (width - barWidth) / 2f + barWidth, finalY + barHeight, new Color(0, 0, 0, 100));
        float progress = (float) packetsLeft / maxPackets.getValue().intValue();
        float filledWidth = barWidth * Easing.ease(progress, Easing.Method.EASE_OUT_QUAD);
        if (filledWidth > 0f) Renderer2D.renderQuad(event.getContext(), (width - barWidth) / 2f, finalY, (width - barWidth) / 2f + filledWidth, finalY + barHeight, ColorUtils.getGlobalColor());
        Renderer2D.renderOutline(event.getContext(), (width - barWidth) / 2f, finalY, (width - barWidth) / 2f + barWidth, finalY + barHeight, Color.black);
    }

    @Override
    public String getInfo() {
        return "" + packetsLeft;
    }
}