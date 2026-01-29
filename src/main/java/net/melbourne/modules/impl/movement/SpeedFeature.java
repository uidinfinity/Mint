package net.melbourne.modules.impl.movement;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.MoveEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.mixins.accessors.Vec3dAccessor;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.client.AntiCheatFeature;
import net.melbourne.settings.types.*;
import net.melbourne.utils.miscellaneous.math.MathUtils;
import net.melbourne.utils.entity.player.movement.MovementUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2d;
import com.google.common.collect.Streams;
import net.minecraft.util.shape.VoxelShape;
import java.util.List;

@FeatureInfo(name = "Speed", category = Category.Movement)
public class SpeedFeature extends Feature {
    public ModeSetting mode = new ModeSetting("Mode", "The method that will be used to increase your speed.", "Strafe", new String[]{"Strafe", "StrafeStrict", "StrictFast", "VHop", "Jump"});

    public BooleanSetting useTimer = new BooleanSetting("UseTimer", "Adds a timer multiplier when strafing.", false);

    public NumberSetting timerMultiplier = new NumberSetting("TimerMultiplier", "The timer multiplier that will be applied to the timer.", 1.08f, 1.0f, 1.2f,
            () -> useTimer.getValue());

    public BooleanSetting speedInWater = new BooleanSetting("SpeedInWater", "Increases your speed while in water.", false);

    public BooleanSetting slowFall = new BooleanSetting("SlowFall", "Increases your speed while in water.", false);

    public NumberSetting fallSpeed = new NumberSetting("FallSpeed", "The timer multiplier that will be applied to the timer.", 0.85f, 0.45f, 1.0f,
            () -> slowFall.getValue());

    public NumberSetting maxFallDist = new NumberSetting("FallDistance", "The timer multiplier that will be applied to the timer.", 1.25F, 0.5F, 2.0F,
            () -> slowFall.getValue());

    public BooleanSetting step = new BooleanSetting("Step", "Automatically steps when speed is active.", false);

    private double distance, speed, forward, ticks;
    private int stage;
    private boolean pressed = false;
    private boolean physicsOnce = true;
    private double currentSpeed = 0.0D;
    private double prevMotion = 0.0D;
    private int aacCounter;

    private boolean wasStepAlreadyEnabled = false;

    @Override
    public void onEnable() {
        stage = 1;
        physicsOnce = true;
        currentSpeed = 0.0D;
        prevMotion = 0.0D;
        aacCounter = 0;
        ticks = 0;

        StepFeature stepFeature = Managers.FEATURE.getFeatureFromClass(StepFeature.class);
        if (step.getValue() && stepFeature != null) {
            wasStepAlreadyEnabled = stepFeature.isEnabled();
            if (!wasStepAlreadyEnabled) {
                stepFeature.setEnabled(true);
            }
        } else {
            wasStepAlreadyEnabled = false;
        }
    }

    @Override
    public void onDisable() {
        if (getNull())
            return;

        Services.WORLD.setTimerMultiplier(1.0f);
        physicsOnce = true;

        if (pressed)
            mc.options.jumpKey.setPressed(false);

        StepFeature stepFeature = Managers.FEATURE.getFeatureFromClass(StepFeature.class);
        if (step.getValue() && stepFeature != null && !wasStepAlreadyEnabled) {
            stepFeature.setToggled(false);
        }

        currentSpeed = 0.0D;
        prevMotion = 0.0D;
        aacCounter = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull())
            return;

        if (isStrafeSpeed()) {
            distance = Math.sqrt(MathHelper.square(mc.player.getX() - mc.player.lastX) + MathHelper.square(mc.player.getZ() - mc.player.lastZ));
            prevMotion = distance;
            boolean flag = MovementUtils.isMoving() && !mc.player.isSneaking() && !mc.player.isInFluid() && mc.player.fallDistance < 5.0f;

            if (useTimer.getValue() && flag) {
                AntiCheatFeature antiCheat = (AntiCheatFeature) Managers.FEATURE.getFeatureByName("AntiCheat");
                String timerMode = antiCheat != null && antiCheat.isEnabled() ? antiCheat.timerMode.getValue() : "Normal";

                switch (timerMode) {
                    case "Normal":
                        Services.WORLD.setTimerMultiplier(timerMultiplier.getValue().floatValue());
                        break;
                    case "Physics":
                        if (physicsOnce) {
                            physicsOnce = false;
                            for (int i = 0; i < timerMultiplier.getValue().intValue(); i++) {
                                mc.player.tick();
                            }
                        }
                        break;
                    default:
                        Services.WORLD.setTimerMultiplier(timerMultiplier.getValue().floatValue());
                        break;
                }
            } else {
                Services.WORLD.setTimerMultiplier(1.0f);
                physicsOnce = true;
            }
        } else {
            Services.WORLD.setTimerMultiplier(1.0f);
            physicsOnce = true;
        }
    }

    @SubscribeEvent
    public void onMove(MoveEvent event) {
        if (getNull())
            return;

        float jumpEffect = 0.0f;
        if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST))
            jumpEffect += (mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1f;

        switch (mode.getValue()) {
            case "Strafe":
                doStrafeLogic(event);
                break;
            case "StrafeStrict":
                doRestrictedSpeed(event, 0.465, 0.44, false);
                break;
            case "StrictFast":
                if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                    doStrafeLogic(event);
                } else {
                    doRestrictedSpeed(event, 0.465, 0.44, true);
                }
                break;
            case "VHop":
                doVHopLogic(event, jumpEffect);
                break;
            case "Jump":
                doJumpLogic(event);
                break;
        }
    }

    private void doStrafeLogic(MoveEvent event) {
        if (mc.player.fallDistance >= 5.0f || mc.player.isSneaking() || mc.player.isClimbing() || mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.COBWEB || mc.player.getAbilities().flying || (mc.player.isInFluid() && !speedInWater.getValue()))
            return;

        speed = MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED) * (mc.player.input.getMovementInput().x <= 0 && forward > 0 ? 0.66 : 1);

        if (stage == 1 && MovementUtils.isMoving() && mc.player.verticalCollision) {
            ((Vec3dAccessor) mc.player.getVelocity()).setY(MovementUtils.getPotionJump(0.3999999463558197));
            event.setMovement(new Vec3d(event.getMovement().getX(), mc.player.getVelocity().getY(), event.getMovement().getZ()));
            speed *= 2.149;
            stage = 2;
        } else if (stage == 2) {
            speed = distance - (0.66 * (distance - MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED)));
            stage = 3;
        } else {
            if (!mc.world.getEntityCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)).isEmpty() || mc.player.verticalCollision)
                stage = 1;

            speed = distance - distance / 159.0;
        }

        speed = Math.max(speed, MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED));

        double ncp = MovementUtils.getPotionSpeed(mode.getValue().equalsIgnoreCase("StrafeStrict") || mc.player.input.getMovementInput().x < 1 ?  0.465 : 0.576);
        double bypass = MovementUtils.getPotionSpeed(mode.getValue().equalsIgnoreCase("StrafeStrict") || mc.player.input.getMovementInput().x < 1 ?  0.44 : 0.57);
        speed = Math.min(speed, ticks > 25 ? ncp : bypass);

        if (ticks++ > 50) ticks = 0;

        Vector2d velocity = MovementUtils.forward(speed);
        event.setMovement(new Vec3d(velocity.x, event.getMovement().getY(), event.getMovement().getZ()));
        if (slowFall.getValue()) {
            if (event.getY() < 0.0D && mc.player.fallDistance <= maxFallDist.getValue().floatValue())
                event.setY(event.getMovement().getY() * fallSpeed.getValue().floatValue());
        }
        event.setMovement(new Vec3d(event.getMovement().getX(), event.getMovement().getY(), velocity.y));
        forward = mc.player.input.getMovementInput().x;

        event.setCancelled(true);
    }

    private void doVHopLogic(MoveEvent event, float jumpEffect) {
        if (MathUtils.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtils.round(0.4, 3)) {
            ((Vec3dAccessor) mc.player.getVelocity()).setY(0.31 + jumpEffect);
            event.setY(0.31 + jumpEffect);
        } else if (MathUtils.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtils.round(0.71, 3)) {
            ((Vec3dAccessor) mc.player.getVelocity()).setY(0.04 + jumpEffect);
            event.setY(0.04 + jumpEffect);
        } else if (MathUtils.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtils.round(0.75, 3)) {
            ((Vec3dAccessor) mc.player.getVelocity()).setY(-0.2 - jumpEffect);
            event.setY(-0.2 - jumpEffect);
        }
        if (!mc.world.isSpaceEmpty(null, mc.player.getBoundingBox().offset(0.0, -0.56, 0.0))
                && MathUtils.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtils.round(0.55, 3)) {
            ((Vec3dAccessor) mc.player.getVelocity()).setY(-0.14 + jumpEffect);
            event.setY(-0.14 + jumpEffect);
        }
        if (stage != 1 || !mc.player.verticalCollision
                || mc.player.forwardSpeed == 0.0f && mc.player.sidewaysSpeed == 0.0f) {
            if (stage != 2 || !mc.player.verticalCollision
                    || mc.player.forwardSpeed == 0.0f && mc.player.sidewaysSpeed == 0.0f) {
                if (stage == 3) {
                    double moveSpeed = 0.66 * (distance - MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED));
                    speed = distance - moveSpeed;
                } else {
                    if (mc.player.isOnGround() && stage > 0) {
                        if (1.35 * MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED) - 0.01 > speed)
                            stage = 0;
                        else
                            stage = MovementUtils.isMoving() ? 1 : 0;
                    }
                    speed = distance - distance / 159.0;
                }
            } else {
                double jump = (!mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0.0, 0.21, 0.0)) ? 0.2 : 0.4) + jumpEffect;
                ((Vec3dAccessor) mc.player.getVelocity()).setY(jump);
                event.setY(jump);
                speed *= 2.149;
            }
        } else
            speed = 2.0 * MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED) - 0.01;

        if (stage > 8)
            speed = MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED);

        speed = Math.max(speed, MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED));
        Vector2d motion = MovementUtils.forward(speed);
        event.setX(motion.x);
        event.setZ(motion.y);
        stage++;
    }

    private void doJumpLogic(MoveEvent event) {
        if (mc.player.fallDistance >= 5.0f ||
                mc.player.isSneaking() ||
                mc.player.isClimbing() ||
                mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.COBWEB ||
                mc.player.getAbilities().flying ||
                mc.player.isGliding() ||
                (mc.player.isInFluid() && !speedInWater.getValue()))
            return;

        double baseSpeed = MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED);
        Vector2d velocity = MovementUtils.forward(baseSpeed);
        event.setX(velocity.x);
        event.setZ(velocity.y);

        if (MovementUtils.isMoving() && mc.player.verticalCollision) {
            ((Vec3dAccessor) mc.player.getVelocity()).setY(MovementUtils.getPotionJump(0.3999999463558197));
            event.setY(MovementUtils.getPotionJump(0.3999999463558197));
        }

        event.setCancelled(true);
    }


    public void doRestrictedSpeed(MoveEvent event, double baseRestriction, double actualRestriction, boolean fast) {
        if (getNull()) return;
        if (mc.player.isSneaking() || mc.player.isClimbing() || mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.COBWEB || mc.player.getAbilities().flying || (mc.player.isInFluid() && !speedInWater.getValue())) return;

        if (MovementUtils.isMoving()) {

            if (fast && MathUtils.round(mc.player.getY() - (int) mc.player.getY(), 3) == MathUtils.round(0.138D, 3)) {
                ((Vec3dAccessor) mc.player.getVelocity()).setY(mc.player.getVelocity().y - 0.08D);
                event.setY(event.getY() - 0.09316090325960147D);
                mc.player.setPos(mc.player.getPos().x, mc.player.getPos().y - 0.09316090325960147D, mc.player.getPos().z);
            }

            if (stage == 1) {
                currentSpeed = fast ? 1.38 : 1.35 * getBaseMotionSpeedNoPot() - 0.01;
            }
            else if (stage == 2 && mc.player.isOnGround()) {
                double jumpSpeed = 0.41999998688697815;

                if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                    double amplifier = mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier();
                    jumpSpeed += (amplifier + 1) * 0.1;
                }

                event.setY(jumpSpeed);
                ((Vec3dAccessor) mc.player.getVelocity()).setY(jumpSpeed);

                double acceleration = 2.149;

                currentSpeed *= acceleration;
            }

            else if (stage == 3) {

                double scaledcurrentSpeed = 0.66 * (prevMotion - getBaseMotionSpeedNoPot());

                currentSpeed = prevMotion - scaledcurrentSpeed;

            } else {
                List<VoxelShape> collisionBoxes = Streams.stream(mc.world.getCollisions(mc.player, mc.player.getBoundingBox().offset(0, mc.player.getVelocity().y, 0))).toList();
                if ((!collisionBoxes.isEmpty() || mc.player.verticalCollision) && stage > 0) {
                    stage = MovementUtils.isMoving() ? 1 : 0;
                }

                currentSpeed = prevMotion - (prevMotion / (160.0 - 0.923));
            }

            currentSpeed = Math.max(currentSpeed, getBaseMotionSpeedNoPot());

            double baseStrictSpeed = baseRestriction;
            double baseRestrictedSpeed = actualRestriction;

            if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                double amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
                baseStrictSpeed *= 1 + (0.2 * (amplifier + 1));
                baseRestrictedSpeed *= 1 + (0.2 * (amplifier + 1));
            }

            if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                double amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
                baseStrictSpeed /= 1 + (0.2 * (amplifier + 1));
                baseRestrictedSpeed /= 1 + (0.2 * (amplifier + 1));
            }

            currentSpeed = Math.min(currentSpeed, aacCounter > 25 ? baseStrictSpeed : baseRestrictedSpeed);

            aacCounter++;

            if (aacCounter > 50) {
                aacCounter = 0;
            }

            Vector2d motion = MovementUtils.forward(currentSpeed);
            event.setX(motion.x);
            event.setZ(motion.y);

            if (slowFall.getValue()) {
                if (event.getY() < 0.0D && mc.player.fallDistance <= maxFallDist.getValue().floatValue())
                    event.setY(event.getMovement().getY() * fallSpeed.getValue().floatValue());
            }

            stage++;
            event.setCancelled(true);
        } else {
            event.setX(0);
            event.setZ(0);
        }
    }


    private double getBaseMotionSpeedNoPot() {
        return 0.2873D;
    }

    public boolean isStrafeSpeed() {
        return mode.getValue().contains("Strafe") || mode.getValue().equals("StrictFast");
    }

    @Override
    public String getInfo() {
        return mode.getValue();
    }
}