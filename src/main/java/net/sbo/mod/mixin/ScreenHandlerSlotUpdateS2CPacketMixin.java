package net.sbo.mod.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.game.InventorySlotUpdateEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ScreenHandlerSlotUpdateS2CPacketMixin {
    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void onSlotUpdate(@NonNull final ClientboundContainerSetSlotPacket packet, @NonNull final CallbackInfo ci) {
        SBOEvent.INSTANCE.emit(new InventorySlotUpdateEvent(packet));
    }
}
