package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.NoRenderFeature;
import net.melbourne.utils.Globals;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin implements Globals {

    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void render(E blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        if (Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).isEnabled() && !Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).tileEntities.getValue().equals("None")) {
            if (Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).tileEntities.getValue().equals("Always") ||
                    (Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).tileEntities.getValue().equals("Distance") &&
                            Math.sqrt(mc.player.squaredDistanceTo(blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ())) > Managers.FEATURE.getFeatureFromClass(NoRenderFeature.class).tileDistance.getValue().floatValue()))
            {
                info.cancel();
            }
        }
    }
}
