package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.PlayerMineEvent;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBreakingInfo.class)
public class BlockBreakingInfoMixin {
    @Inject(method = "compareTo(Lnet/minecraft/entity/player/BlockBreakingInfo;)I", at = @At("HEAD"))
    private void compareTo(BlockBreakingInfo other, CallbackInfoReturnable<Integer> cir) {
        BlockBreakingInfo info = (BlockBreakingInfo)(Object)this;
        
        Melbourne.EVENT_HANDLER.post(new PlayerMineEvent(info.getActorId(), info.getPos()));
    }
}