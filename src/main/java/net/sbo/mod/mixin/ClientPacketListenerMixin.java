package net.sbo.mod.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.game.SentCommandEvent;
import net.sbo.mod.utils.events.impl.game.SentMessageEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
final class ClientPacketListenerMixin {
    @Inject(method = "sendChat", at = @At("HEAD"))
    private final void sbo$onSendMessage(@NonNull final String content, @NonNull final CallbackInfo ci) {
        SBOEvent.INSTANCE.emit(new SentMessageEvent(content));
    }

    @Inject(method = "sendCommand", at = @At("HEAD"))
    private final void sbo$onSendCommand(@NonNull final String command, @NonNull final CallbackInfo ci) {
        SBOEvent.INSTANCE.emit(new SentCommandEvent(command));
    }
}
