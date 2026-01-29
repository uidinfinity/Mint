package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.LivingUpdateEvent;
import net.melbourne.modules.impl.movement.NoJumpDelayFeature;
import net.melbourne.modules.impl.movement.SprintFeature;
import net.melbourne.modules.impl.movement.StepFeature;
import net.melbourne.modules.impl.render.HandProgressFeature;
import net.melbourne.modules.impl.player.SwingFeature;
import net.melbourne.utils.Globals;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Globals {

    @Shadow @Final private static EntityAttributeModifier SPRINTING_SPEED_BOOST;
    @Shadow private int jumpingCooldown;

    @Shadow public abstract boolean isUsingItem();
    @Shadow public abstract float getHandSwingProgress(float tickDelta);

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract @Nullable EntityAttributeInstance getAttributeInstance(RegistryEntry<EntityAttribute> attribute);

    @Inject(method = "tick", at = @At("HEAD"))
    private void onPreLivingUpdate(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        Melbourne.EVENT_HANDLER.post(new LivingUpdateEvent(entity, true));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPostLivingUpdate(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        Melbourne.EVENT_HANDLER.post(new LivingUpdateEvent(entity, false));
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", ordinal = 2, shift = At.Shift.BEFORE))
    private void doItemUse(CallbackInfo info) {
        if (Managers.FEATURE != null && Managers.FEATURE.getFeatureFromClass(NoJumpDelayFeature.class).isEnabled() && jumpingCooldown == 10) {
            jumpingCooldown = Managers.FEATURE.getFeatureFromClass(NoJumpDelayFeature.class).ticks.getValue().intValue();
        }
    }

    @Inject(method = "setSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setSprinting(Z)V", shift = At.Shift.AFTER), cancellable = true)
    private void setSprinting$setSprinting(boolean sprinting, CallbackInfo info) {
        if ((Object) this == mc.player && Managers.FEATURE.getFeatureFromClass(SprintFeature.class).isEnabled()) {
            EntityAttributeInstance entityAttributeInstance = getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
            entityAttributeInstance.removeModifier(SPRINTING_SPEED_BOOST.id());

            if (Managers.FEATURE.getFeatureFromClass(SprintFeature.class).canSprint() &&
                    Managers.FEATURE.getFeatureFromClass(SprintFeature.class).mode.getValue().equalsIgnoreCase("Rage")) {
                setFlag(3, true);
                entityAttributeInstance.addTemporaryModifier(SPRINTING_SPEED_BOOST);
            } else {
                setFlag(3, false);
            }

            info.cancel();
        }
    }


    @Inject(method = "getStepHeight", at = @At("RETURN"), cancellable = true)
    private void onGetStepHeight(CallbackInfoReturnable<Float> cir) {
        if ((Object) this == mc.player) {
            var stepFeature = Managers.FEATURE.getFeatureFromClass(StepFeature.class);
            if (stepFeature.isEnabled()) {
                cir.setReturnValue(stepFeature.getHeight());
            }
        }
    }

    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != mc.player) return;

        var swing = Managers.FEATURE.getFeatureFromClass(SwingFeature.class);
        if (swing.isEnabled() && swing.modifySpeed.getValue()) {
            int modified = 21 - swing.speed.getValue().intValue();
            cir.setReturnValue(Math.max(1, modified));
        }
    }

    @Inject(method = "getItemUseTimeLeft", at = @At("RETURN"), cancellable = true)
    private void staticEating(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != mc.player) return;

        var feature = Managers.FEATURE.getFeatureFromClass(HandProgressFeature.class);
        if (feature.isEnabled() && feature.staticEating.getValue() && isUsingItem()) {
            cir.setReturnValue(cir.getReturnValue());
        }
    }
}