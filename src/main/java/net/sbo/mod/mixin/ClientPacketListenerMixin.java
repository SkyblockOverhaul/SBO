package net.sbo.mod.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.game.SentCommandEvent;
import net.sbo.mod.utils.events.impl.game.SentMessageEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "sendChat", at = @At("HEAD"))
    private void sbo$onSendMessage(@NonNull String content, @NonNull CallbackInfo ci) {
        SBOEvent.INSTANCE.emit(new SentMessageEvent(content));
    }

    @Inject(method = "sendCommand", at = @At("HEAD"))
    private void sbo$onSendCommand(@NonNull String command, @NonNull CallbackInfo ci) {
        SBOEvent.INSTANCE.emit(new SentCommandEvent(command));
    }
}
