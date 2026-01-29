package net.melbourne.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.ItemRenderEvent;
import net.melbourne.events.impl.RenderHandEvent;
import net.melbourne.modules.impl.render.HandProgressFeature;
import net.melbourne.modules.impl.render.ViewmodelFeature;
import net.melbourne.modules.impl.player.SwingFeature;
import net.melbourne.utils.Globals;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HeldItemRenderer.class, priority = 1001)
public abstract class HeldItemRendererMixin implements Globals {

    @Shadow private float equipProgressMainHand;
    @Shadow private ItemStack mainHand;

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", shift = At.Shift.BEFORE))
    private void onRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        var swing = Managers.FEATURE.getFeatureFromClass(SwingFeature.class);
        if (swing.isEnabled() && swing.translations.getValue()) {
            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            int direction = arm == Arm.RIGHT ? 1 : -1;

            float swingX = swing.translateX.getValue().floatValue() * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
            float swingY = swing.translateY.getValue().floatValue() * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float) Math.PI * 2));
            float swingZ = swing.translateZ.getValue().floatValue() * MathHelper.sin(swingProgress * (float) Math.PI);

            matrices.translate(direction * swingX, swingY, swingZ);
        }
        Melbourne.EVENT_HANDLER.post(new ItemRenderEvent(hand, matrices));
    }

    @WrapWithCondition(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V")
    )
    private boolean cancelSwayIfDisabled(MatrixStack instance, Quaternionfc quaternion) {
        ViewmodelFeature vm = Managers.FEATURE.getFeatureFromClass(ViewmodelFeature.class);
        return vm == null || vm.sway.getValue();
    }

    @WrapOperation(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F", ordinal = 2)
    )
    private float modifyMainHandLerp(float delta, float start, float end, Operation<Float> original) {
        HandProgressFeature hp = Managers.FEATURE.getFeatureFromClass(HandProgressFeature.class);
        if (hp != null && hp.isEnabled() && hp.modifyMainHand.getValue()) {
            float custom = hp.mainHandProgress.getValue().floatValue();
            return MathHelper.lerp(delta, custom, custom);
        }
        return original.call(delta, start, end);
    }

    @WrapOperation(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F", ordinal = 3)
    )
    private float modifyOffHandLerp(float delta, float start, float end, Operation<Float> original) {
        HandProgressFeature hp = Managers.FEATURE.getFeatureFromClass(HandProgressFeature.class);
        if (hp != null && hp.isEnabled() && hp.modifyOffHand.getValue()) {
            float custom = hp.offHandProgress.getValue().floatValue();
            return MathHelper.lerp(delta, custom, custom);
        }
        return original.call(delta, start, end);
    }

    @Inject(method = "applyEatOrDrinkTransformation", at = @At("HEAD"), cancellable = true)
    private void applyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, net.minecraft.entity.player.PlayerEntity player, CallbackInfo ci) {
        HandProgressFeature hp = Managers.FEATURE.getFeatureFromClass(HandProgressFeature.class);
        if (hp != null && hp.isEnabled() && hp.staticEating.getValue()) {
            applyStaticEatOrDrink(matrices, arm);
            ci.cancel();
        }
    }

    @WrapOperation(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void renderItem$renderFirstPersonItem(HeldItemRenderer instance, AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Operation<Void> original) {
        RenderHandEvent event = new RenderHandEvent(vertexConsumers);
        Melbourne.EVENT_HANDLER.post(event);
        original.call(instance, player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, event.getVertexConsumers(), light);
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("TAIL"))
    private void renderItem$TAIL(float tickProgress, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        Melbourne.EVENT_HANDLER.post(new RenderHandEvent.Post());
    }

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void onApplySwingOffset(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        var swing = Managers.FEATURE.getFeatureFromClass(SwingFeature.class);
        if (swing.isEnabled() && swing.rotations.getValue()) {
            ci.cancel();
            int i = arm == Arm.RIGHT ? 1 : -1;
            float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
            float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);

            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (swing.rotationY.getValue().floatValue() + f * -20.0F)));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * g * swing.rotationZ.getValue().floatValue()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * swing.rotationX.getValue().floatValue()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
        }
    }

    @Unique
    private void applyStaticEatOrDrink(MatrixStack matrices, Arm arm) {
        float h = 1.0F;
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(h * 0.6F * i, h * -0.5F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * h * 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * h * 30.0F));
    }
}