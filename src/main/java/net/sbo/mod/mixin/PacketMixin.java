package net.sbo.mod.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.packets.PacketReceiveEvent;
import net.sbo.mod.utils.events.impl.packets.PacketSendEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelFutureListener;

@Mixin(ClientConnection.class)
public class PacketMixin {
    // recived S2C packets
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"))
    private void onPacketReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        SBOEvent.INSTANCE.emit(new PacketReceiveEvent(packet));
    }

    // sended C2S packets
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"))
    private void onPacketSend(Packet<?> packet, ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        SBOEvent.INSTANCE.emit(new PacketSendEvent(packet));
    }
}