package net.melbourne.mixins;


import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.AttackBlockEvent;
import net.melbourne.events.impl.AttackEntityEvent;
import net.melbourne.events.impl.BreakBlockEvent;
import net.melbourne.modules.impl.player.NoInteractFeature;
import net.melbourne.utils.block.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ClientPlayerInteractionManager.class)
class ClientPlayerInteractionManagerMixin {
    @Shadow @Final private MinecraftClient client;


    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void attackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        Melbourne.EVENT_HANDLER.post(new AttackEntityEvent(player, target));
    }


    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void attackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        AttackBlockEvent event = new AttackBlockEvent(pos, direction);
        Melbourne.EVENT_HANDLER.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }


    @Inject(method = "breakBlock", at = @At(value = "HEAD"), cancellable = true)
    private void breakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BreakBlockEvent event = new BreakBlockEvent(pos);
        Melbourne.EVENT_HANDLER.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }


    @Inject(method = "interactBlock", at = @At(value = "HEAD"), cancellable = true)
    private void interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> info) {
        NoInteractFeature m = Managers.FEATURE.getFeatureFromClass(NoInteractFeature.class);
        if (m.isEnabled() && m.shouldNoInteract() && m.mode.getValue().equalsIgnoreCase("Disable") && BlockUtils.RIGHT_CLICKABLE_BLOCKS.contains(client.world.getBlockState(hitResult.getBlockPos()).getBlock())) {
            info.setReturnValue(ActionResult.FAIL);
        }
    }


    @Inject(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;sendSequencedPacket(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/SequencedPacketCreator;)V"))
    private void interactBlockBefore(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        NoInteractFeature m = Managers.FEATURE.getFeatureFromClass(NoInteractFeature.class);
        if (!client.player.isSneaking() && m.isEnabled() && m.shouldNoInteract() && m.mode.getValue().equalsIgnoreCase("Sneak") && BlockUtils.RIGHT_CLICKABLE_BLOCKS.contains(client.world.getBlockState(hitResult.getBlockPos()).getBlock())) {
            client.player.setSneaking(true);
        }
    }


    @Inject(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;sendSequencedPacket(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/SequencedPacketCreator;)V", shift = At.Shift.AFTER))
    private void interactBlockAfter(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> info) {
        NoInteractFeature m = Managers.FEATURE.getFeatureFromClass(NoInteractFeature.class);
        if (!client.player.isSneaking() && m.isEnabled() && m.shouldNoInteract() && m.mode.getValue().equalsIgnoreCase("Sneak") && BlockUtils.RIGHT_CLICKABLE_BLOCKS.contains(client.world.getBlockState(hitResult.getBlockPos()).getBlock())) {
            client.player.setSneaking(false);
        }
    }
}