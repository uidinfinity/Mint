package net.melbourne.mixins;

import net.melbourne.Melbourne;
import net.melbourne.events.impl.ChatSendEvent;
import net.melbourne.events.impl.ClientConnectEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo info) {
        Melbourne.EVENT_HANDLER.post(new ClientConnectEvent());
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        ChatSendEvent event = new ChatSendEvent(message, ci.isCancelled());
        Melbourne.EVENT_HANDLER.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        } else if (!event.getMessage().equals(message)) {
            ((ClientPlayNetworkHandler)(Object)this).sendChatMessage(event.getMessage());
            ci.cancel();
        }
    }
}