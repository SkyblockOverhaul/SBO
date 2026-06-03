package net.sbo.mod.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.packets.PacketReceiveEvent;
import net.sbo.mod.utils.events.impl.packets.PacketSendEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelFutureListener;

@Mixin(Connection.class)
public class PacketMixin {
    // received S2C packets
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void onPacketReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        SBOEvent.INSTANCE.emit(new PacketReceiveEvent(packet));
    }

    // sent C2S packets
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"))
    private void onPacketSend(Packet<?> packet, ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        SBOEvent.INSTANCE.emit(new PacketSendEvent(packet));
    }
}