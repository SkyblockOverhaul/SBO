package net.sbo.mod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.sbo.mod.qol.FastAotv;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * startUseItem sets the right click delay to 4 ticks first, this lowers it again while holding an AOTV. Same hook as
 * Nopo Mod's Fast AOTV.
 */
@Mixin(Minecraft.class)
final class FastAotvMixin {
    @Shadow
    private int rightClickDelay;

    @Shadow
    public @Nullable LocalPlayer player;

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z", shift = At.Shift.BEFORE))
    private final void sbo$fastAotv(@NonNull final CallbackInfo ci) {
        final var delay = FastAotv.delay(this.player);
        if (delay > 0) this.rightClickDelay = delay;
    }
}
