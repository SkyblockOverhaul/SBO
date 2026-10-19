package net.sbo.mod.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.sbo.mod.utils.hubcache.HubChunkCache;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
final class HubOptionsMixin {
    @Shadow @Final private OptionInstance<Integer> renderDistance;

    // Vanilla caps the render distance at the server's view distance, fake chunks go up to the client's.
    @Inject(method = "getEffectiveRenderDistance", at = @At("HEAD"), cancellable = true)
    private final void sbo$useClientRenderDistance(@NonNull final CallbackInfoReturnable<Integer> cir) {
        if (HubChunkCache.isActive()) cir.setReturnValue(this.renderDistance.get());
    }
}
