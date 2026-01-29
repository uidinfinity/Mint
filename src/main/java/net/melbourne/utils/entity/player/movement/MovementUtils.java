package net.melbourne.utils.entity.player.movement;

import net.melbourne.utils.Globals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Vector2d;

public class MovementUtils implements Globals {
    public static double DEFAULT_SPEED = 0.2873;

    public static double[] forwardDouble(double speeda) {
        return new double[] {forward(speeda).x, forward(speeda).y};
    }

    public static double getSpeed(boolean slowness) {
        int amplifier;
        double defaultSpeed = 0.2873;
        if (MinecraftClient.getInstance().player.hasStatusEffect(StatusEffects.SPEED)) {
            amplifier = MinecraftClient.getInstance().player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            defaultSpeed *= 1.0 + 0.2 * (double) (amplifier + 1);
        }
        if (slowness && MinecraftClient.getInstance().player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            amplifier = MinecraftClient.getInstance().player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            defaultSpeed /= 1.0 + 0.2 * (double) (amplifier + 1);
        }
        return defaultSpeed;
    }

    public static Vector2d forward(double speed) {
        float forward = mc.player.input.getMovementInput().y;
        float sideways = mc.player.input.getMovementInput().x;
        float yaw = mc.player.getYaw();

        if (forward == 0.0f && sideways == 0.0f)
            return new Vector2d(0, 0);

        if (forward != 0.0f) {
            if (sideways > 0.0f)
                yaw += (forward > 0.0f) ? -45 : 45;
            else if (sideways < 0.0f)
                yaw += (forward > 0.0f) ? 45 : -45;

            sideways = 0.0f;

            if (forward > 0.0f) forward = 1.0f;
            else if (forward < 0.0f) forward = -1.0f;
        }

        double magnitude = Math.sqrt(forward * forward + sideways * sideways);
        if (magnitude < 1.0f && magnitude > 0.0f) {
            forward /= (float) magnitude;
            sideways /= (float) magnitude;
        }

        double motionX = Math.cos(Math.toRadians(yaw + 90.0f));
        double motionZ = Math.sin(Math.toRadians(yaw + 90.0f));

        return new Vector2d(forward * speed * motionX + sideways * speed * motionZ,
                forward * speed * motionZ - sideways * speed * motionX);
    }

    public static boolean anyMovementKeys() {
        return mc.player.input.getMovementInput().y != 0.0f ||
                mc.player.input.getMovementInput().x != 0.0f ||
                mc.options.jumpKey.isPressed() ||
                mc.options.sneakKey.isPressed();
    }

    public static boolean anyMovementKeysNoSneak() {
        return mc.player.input.getMovementInput().y != 0.0f ||
                mc.player.input.getMovementInput().x != 0.0f ||
                mc.options.jumpKey.isPressed();
    }

    public static boolean anyMovementKeysNoSneakNoJump() {
        return mc.player.input.getMovementInput().y != 0.0f ||
                mc.player.input.getMovementInput().x != 0.0f;
    }


    public static double getPotionSpeed(double speed) {
        if (mc.player.hasStatusEffect(StatusEffects.SPEED))
            speed *= 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS))
            speed /= 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);

        return speed;
    }

    public static double getPotionJump(double jump) {
        if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST))
            jump += (mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1f;

        return jump;
    }

    public static boolean isMoving() {
        return mc.player.sidewaysSpeed != 0.0f || mc.player.forwardSpeed != 0.0f;
    }
}
