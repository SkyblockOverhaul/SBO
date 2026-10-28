package net.sbo.mod.mixin.events;

import net.minecraft.client.player.LocalPlayer;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.game.ItemDropEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
final class LocalPlayerDropMixin {
    @Inject(method = "drop", at = @At("HEAD"))
    private final void sbo$onDrop(final boolean fullStack, @NonNull final CallbackInfoReturnable<Boolean> cir) {
        SBOEvent.INSTANCE.emit(new ItemDropEvent());
    }
}
