package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.ChamsFeature;
import net.melbourne.utils.Globals;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin implements Globals {

    @Unique
    private static boolean melbourne$chamsPass = false;

    @Inject(method = "render", at = @At("HEAD"))
    private void melbourne$renderChams(
            Entity entity,
            double x, double y, double z,
            float yaw,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            int light,
            CallbackInfo ci
    ) {
        if (melbourne$chamsPass) return;

        ChamsFeature chams = Managers.FEATURE.getFeatureFromClass(ChamsFeature.class);
        if (chams == null || !chams.shouldChams(entity)) return;

        melbourne$chamsPass = true;

        if (consumers instanceof VertexConsumerProvider.Immediate) {
            ((VertexConsumerProvider.Immediate) consumers).draw();
        }

        int prevDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_GREATER);
        GL11.glDepthMask(false);

        ((EntityRenderDispatcher) (Object) this).render(entity, x, y, z, yaw, matrices, consumers, light);

        if (consumers instanceof VertexConsumerProvider.Immediate) {
            ((VertexConsumerProvider.Immediate) consumers).draw();
        }

        GL11.glDepthFunc(prevDepthFunc);
        GL11.glDepthMask(true);

        melbourne$chamsPass = false;
    }
}