package net.sbo.mod.mixin.events;

import net.minecraft.client.player.LocalPlayer;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.game.ItemDropEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//#if MC > 26.2
//$$ import net.minecraft.client.multiplayer.MultiPlayerGameMode;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

//#if MC > 26.2
//$$ @Mixin(MultiPlayerGameMode.class)
//#else
@Mixin(LocalPlayer.class)
//#endif
final class ItemDropMixin {
    //#if MC > 26.2
    //$$ @Inject(method = "dropItem", at = @At("HEAD"))
    //$$ private final void sbo$onDrop(@NonNull final LocalPlayer player, final boolean fullStack, @NonNull final CallbackInfo ci) {
    //#else
    @Inject(method = "drop", at = @At("HEAD"))
    private final void sbo$onDrop(final boolean fullStack, @NonNull final CallbackInfoReturnable<Boolean> cir) {
    //#endif
        SBOEvent.INSTANCE.emit(new ItemDropEvent());
    }
}
