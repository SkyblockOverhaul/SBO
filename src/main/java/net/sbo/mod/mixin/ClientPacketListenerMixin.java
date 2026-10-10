package net.sbo.mod.mixin;

import java.util.EnumSet;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.game.SentCommandEvent;
import net.sbo.mod.utils.events.impl.game.SentMessageEvent;
import net.sbo.mod.utils.game.TabList;
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

    // TAIL runs on the main thread after the tab list was actually modified
    @Inject(method = "handlePlayerInfoUpdate", at = @At("TAIL"))
    private final void sbo$onPlayerInfoUpdate(@NonNull final ClientboundPlayerInfoUpdatePacket packet, @NonNull final CallbackInfo ci) {
        // only entries being added or display names changing affect the cached lines (ignores latency, gamemode, ...)
        final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = packet.actions();
        if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER) || actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME)) {
            TabList.INSTANCE.markDirty();
        }
    }

    @Inject(method = "handlePlayerInfoRemove", at = @At("TAIL"))
    private final void sbo$onPlayerInfoRemove(@NonNull final ClientboundPlayerInfoRemovePacket packet, @NonNull final CallbackInfo ci) {
        TabList.INSTANCE.markDirty();
    }
}
