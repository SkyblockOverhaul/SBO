package net.sbo.mod.mixin.hubcache;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.sbo.mod.utils.hubcache.HubChunkCache;

@Mixin(targets = "net/minecraft/client/multiplayer/ClientChunkCache$Storage")
final class HubClientChunkCacheStorageMixin {
    @Inject(method = "inRange", at = @At("HEAD"), cancellable = true)
    private final void sbo$filterHubChunk(final int chunkX, final int chunkZ, @NonNull final CallbackInfoReturnable<Boolean> cir) {
        if (HubChunkCache.hidesChunk(chunkX, chunkZ)) cir.setReturnValue(false);
    }
}
