package net.melbourne.utils.entity.player;

import net.melbourne.utils.Globals;
import net.melbourne.utils.entity.player.movement.MovementUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.consume.UseAction;

public class PlayerUtils implements Globals {

    public static boolean isMoving() {
        return mc.player.input.getMovementInput().y >= 0.8 ||
                mc.player.input.getMovementInput().y <= -0.8 ||
                mc.player.input.getMovementInput().x >= 0.8 ||
                mc.player.input.getMovementInput().x <= -0.8;
    }

    public static float getHealth(PlayerEntity player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }

    public static double getStrictBaseSpeed(double speed) {
        if (mc.player.hasStatusEffect(StatusEffects.SPEED))
            speed *= 1.0 + 0.1 * (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS))
            speed /= 1.0 + 0.1 * (mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);
        return speed;
    }

    public static void setSpeed(LivingEntity entity, double speed) {
        double[] dir = MovementUtils.forwardDouble(speed);
        entity.setVelocity(dir[0], entity.getVelocity().y, dir[1]);
    }

    public static void setStepHeight(float height) {
        mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT).setBaseValue(height);
    }

    public static void instantTp(double x, double y, double z) {
        mc.player.refreshPositionAfterTeleport(x, y, z);
    }

    public static boolean isEating() {
        return isEating(mc.player);
    }

    public static boolean isEating(PlayerEntity player) {
        if (player.getActiveHand() == null) {
            return false;
        } else if (!player.isUsingItem()) {
            return false;
        } else {
            return player.getStackInHand(player.getActiveHand()).getUseAction() == UseAction.EAT;
        }
    }
}