package net.melbourne.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.services.Services;
import net.melbourne.events.impl.*;
import net.melbourne.modules.impl.movement.NoSlowFeature;
import net.melbourne.modules.impl.player.VelocityFeature;
import net.melbourne.modules.impl.player.SwingFeature;
import net.melbourne.utils.Globals;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity implements Globals {
    @Shadow private double lastXClient;
    @Shadow private double lastYClient;
    @Shadow private double lastZClient;
    @Shadow private float lastYawClient;
    @Shadow private float lastPitchClient;
    @Shadow private boolean lastOnGround;
    @Shadow private boolean lastHorizontalCollision;
    @Shadow private int ticksSinceLastPositionPacketSent;
    @Shadow @Final public ClientPlayNetworkHandler networkHandler;
    @Shadow private boolean autoJumpEnabled;
    @Shadow protected abstract boolean isCamera();
    @Shadow public Input input;


    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow protected abstract void autoJump(float dx, float dz);
    @Shadow private static Vec2f applyDirectionalMovementSpeedFactors(Vec2f vec) { return null; }
    @Shadow public abstract boolean shouldSlowDown();
    @Shadow protected abstract void sendSprintingPacket();
    @Shadow public abstract boolean isSneaking();

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = At.Shift.AFTER))
    private void tick$AFTER(CallbackInfo info) {
        Melbourne.EVENT_HANDLER.post(new UpdateMovementEvent());
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;tickables:Ljava/util/List;", shift = At.Shift.BEFORE))
    private void tick$tickables(CallbackInfo ci) {
        Melbourne.EVENT_HANDLER.post(new UpdateMovementEvent.Post());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = At.Shift.BEFORE))
    private void tick$BEFORE(CallbackInfo info) {
        Melbourne.EVENT_HANDLER.post(new PlayerUpdateEvent());
    }

    @Inject(method = "tickMovementInput", at = @At(value = "TAIL", target = "Lnet/minecraft/client/input/Input;tick(ZF)V", shift = At.Shift.AFTER))
    private void tickMovement(CallbackInfo ci) {
        Melbourne.EVENT_HANDLER.post(new InputUpdateEvent(input));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick$HEAD(CallbackInfo ci) {
        Services.ROTATION.update();
    }


    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void move(MovementType movementType, Vec3d movement, CallbackInfo info) {
        MoveEvent event = new MoveEvent(movementType, movement);
        Melbourne.EVENT_HANDLER.post(event);

        if (event.isCancelled()) {
            info.cancel();

            double prevX = getX();
            double prevZ = getZ();

            super.move(movementType, event.getMovement());
            autoJump((float) (getX() - prevX), (float) (getZ() - prevZ));
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void pushOutOfBlocks(double x, double z, CallbackInfo info) {
        VelocityFeature velocity = Managers.FEATURE.getFeatureFromClass(VelocityFeature.class);
        if (velocity.isEnabled() && velocity.mode.getValue().equalsIgnoreCase("Cancel")) {
            info.cancel();
        }
    }


    @Inject(method = "applyMovementSpeedFactors", at = @At("HEAD"), cancellable = true)
    private void onApplyMove(Vec2f input, CallbackInfoReturnable<Vec2f> cir) {
        if (!(Managers.FEATURE.getFeatureFromClass(NoSlowFeature.class).isEnabled() &&
                Managers.FEATURE.getFeatureFromClass(NoSlowFeature.class).mode.getValue().equalsIgnoreCase("Normal"))) return;

        if (input.lengthSquared() == 0.0F) {
            cir.setReturnValue(input);
            return;
        }

        Vec2f vec2f = input.multiply(0.98F);
        if (this.shouldSlowDown()) {
            float f = (float) this.getAttributeValue(EntityAttributes.SNEAKING_SPEED);
            vec2f = vec2f.multiply(f);
        }

        cir.setReturnValue(applyDirectionalMovementSpeedFactors(vec2f));
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean tickMovement$isUsingItem(boolean original) {
        if (Managers.FEATURE.getFeatureFromClass(NoSlowFeature.class).isEnabled()) {
            if (Managers.FEATURE.getFeatureFromClass(NoSlowFeature.class).mode.getValue().equalsIgnoreCase("StrictNCP"))
                return false;

            if (Managers.FEATURE.getFeatureFromClass(NoSlowFeature.class).mode.getValue().equalsIgnoreCase("GrimV3"))
                return Managers.FEATURE.getFeatureFromClass(NoSlowFeature.class).getTickState();
        }

        return original;
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void sendMovementPackets(CallbackInfo info) {
        SendMovementEvent event = new SendMovementEvent();
        Melbourne.EVENT_HANDLER.post(event);

        if (event.isCancelled()) {
            info.cancel();
        }
    }



    @ModifyExpressionValue(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean canStartSprinting$isUsingItem(boolean original) {
        if (Managers.FEATURE.getFeatureFromClass(NoSlowFeature.class).isEnabled()) {
            if (Managers.FEATURE.getFeatureFromClass(NoSlowFeature.class).mode.getValue().equalsIgnoreCase("StrictNCP"))
                return false;

            if (Managers.FEATURE.getFeatureFromClass(NoSlowFeature.class).mode.getValue().equalsIgnoreCase("GrimV3"))
                return Managers.FEATURE.getFeatureFromClass(NoSlowFeature.class).getTickState();
        }

        return original;
    }

    @Inject(method = "swingHand", at = @At("HEAD"), cancellable = true)
    private void onSwingHand(Hand hand, CallbackInfo ci) {
        var swing = Managers.FEATURE.getFeatureFromClass(SwingFeature.class);
        if (swing.isEnabled()) {
            String mode = swing.hand.getValue();
            if (!mode.equalsIgnoreCase("None")) {
                switch (mode) {
                    case "Default" -> super.swingHand(hand);
                    case "Mainhand" -> super.swingHand(Hand.MAIN_HAND);
                    case "Offhand" -> super.swingHand(Hand.OFF_HAND);
                    case "Both" -> {
                        super.swingHand(Hand.MAIN_HAND);
                        super.swingHand(Hand.OFF_HAND);
                    }
                }
                if (mode.equalsIgnoreCase("Packet") || !swing.noPacket.getValue()) {
                    this.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
                }
            }
            ci.cancel();
        }
    }

    @Inject(method = "sendMovementPackets", at = @At(value = "HEAD"), cancellable = true)
    private void onSendMovementPacketsHead(CallbackInfo info) {
        LocationEvent event = new LocationEvent(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision);
        Melbourne.EVENT_HANDLER.post(event);
        double x = event.getX();
        double y = event.getY();
        double z = event.getZ();
        float yaw = event.getYaw();
        float pitch = event.getPitch();
        boolean onGround = event.isOnGround();

        if (event.isCancelled()) {
            info.cancel();
            this.sendSprintingPacket();
            if (this.isCamera()) {
                double d = x - this.lastXClient;
                double e = y - this.lastYClient;
                double f = z - this.lastZClient;
                double g = yaw - this.lastYawClient;
                double h = pitch - this.lastPitchClient;
                this.ticksSinceLastPositionPacketSent++;
                boolean bl = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
                boolean bl2 = g != 0.0 || h != 0.0;
                if (bl && bl2) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround, this.horizontalCollision));
                } else if (bl) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, this.horizontalCollision));
                } else if (bl2) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround, this.horizontalCollision));
                } else if (this.lastOnGround != onGround || this.lastHorizontalCollision != this.horizontalCollision) {
                    this.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(onGround, this.horizontalCollision));
                }

                if (bl) {
                    this.lastXClient = x;
                    this.lastYClient = x;
                    this.lastZClient = x;
                    this.ticksSinceLastPositionPacketSent = 0;
                }

                if (bl2) {
                    this.lastYawClient = yaw;
                    this.lastPitchClient = pitch;
                }

                this.lastOnGround = onGround;
                this.lastHorizontalCollision = this.horizontalCollision;
                this.autoJumpEnabled = mc.options.getAutoJump().getValue();
            }
        }
    }
}