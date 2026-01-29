package net.melbourne.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.PlayerTravelEvent;
import net.melbourne.modules.impl.movement.KeepSprintFeature;
import net.melbourne.modules.impl.movement.SafeWalkFeature;
import net.melbourne.modules.impl.player.ReachFeature;
import net.melbourne.utils.Globals;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements Globals {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V", shift = At.Shift.AFTER))
    private void attack(CallbackInfo callbackInfo) {
        if (Managers.FEATURE.getFeatureFromClass(KeepSprintFeature.class).isEnabled())
        {
            float multiplier = 0.6f + 0.4f * (Managers.FEATURE.getFeatureFromClass(KeepSprintFeature.class).motion.getValue().floatValue() / 100);
            mc.player.setVelocity(mc.player.getVelocity().x / 0.6 * multiplier, mc.player.getVelocity().y, mc.player.getVelocity().z / 0.6 * multiplier);
            mc.player.setSprinting(true);
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void travel(Vec3d movementInput, CallbackInfo info) {
        PlayerTravelEvent event = new PlayerTravelEvent(movementInput);
        Melbourne.EVENT_HANDLER.post(event);

        if (event.isCancelled()) {
            move(MovementType.SELF, getVelocity());
            info.cancel();
        }
    }

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void getBlockInteractionRange(CallbackInfoReturnable<Double> info) {
        if (Managers.FEATURE.getFeatureFromClass(ReachFeature.class).isEnabled())
        {
            info.setReturnValue(Managers.FEATURE.getFeatureFromClass(ReachFeature.class).amount.getValue().doubleValue());
        }
    }

    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void clipAtLedge(CallbackInfoReturnable<Boolean> info) {
        if (Managers.FEATURE.getFeatureFromClass(SafeWalkFeature.class).isEnabled()) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void getEntityInteractionRange(CallbackInfoReturnable<Double> info) {
        if (Managers.FEATURE.getFeatureFromClass(ReachFeature.class).isEnabled())
        {
            info.setReturnValue(Managers.FEATURE.getFeatureFromClass(ReachFeature.class).amount.getValue().doubleValue());
        }
    }
}

