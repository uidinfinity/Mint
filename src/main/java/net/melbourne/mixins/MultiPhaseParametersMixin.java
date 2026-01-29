package net.melbourne.mixins;

import net.melbourne.ducks.IMultiPhaseParameters;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderLayer.MultiPhaseParameters.class)
public class MultiPhaseParametersMixin implements IMultiPhaseParameters {
    @Shadow
    @Final
    public RenderPhase.Target target;

    @Override
    public RenderPhase.Target melbourne$getTarget() {
        return this.target;
    }
}