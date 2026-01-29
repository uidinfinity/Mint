package net.melbourne.mixins;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.PacketSendEvent;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Shadow
    private Channel channel;

    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    public void onPacketReceive(ChannelHandlerContext chc, Packet<?> packet, CallbackInfo ci) {
        if (this.channel.isOpen() && packet != null) {
            PacketReceiveEvent event = new PacketReceiveEvent(packet);
            Melbourne.EVENT_HANDLER.post(event);

            if (event.isCancelled())
                ci.cancel();

        }
    }

    @Inject(method = "sendImmediately", at = @At("HEAD"), cancellable = true)
    private void onPacketSend(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flush, CallbackInfo ci) {
        PacketSendEvent event = new PacketSendEvent(packet);
        Melbourne.EVENT_HANDLER.post(event);

        if (event.isCancelled())
            ci.cancel();
    }

    @Inject(method = "sendImmediately", at = @At("TAIL"), cancellable = true)
    private void onPacketSendPost(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flush, CallbackInfo ci) {
        PacketSendEvent.Post event = new PacketSendEvent.Post(packet);
        Melbourne.EVENT_HANDLER.post(event);

        if (event.isCancelled())
            ci.cancel();
    }
}
