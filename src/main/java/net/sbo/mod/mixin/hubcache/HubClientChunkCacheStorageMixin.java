package net.sbo.mod.mixin.hubcache;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.sbo.mod.utils.hubcache.FakeChunkManagerKt;
import net.sbo.mod.utils.hubcache.HubChunkCache;
import net.sbo.mod.diana.guesses.ArrowGuessBurrow;
import net.sbo.mod.utils.game.World;

@Mixin(targets = "net/minecraft/client/multiplayer/ClientChunkCache$Storage")
final class HubClientChunkCacheStorageMixin {
    @Inject(method = "inRange", at = @At("HEAD"), cancellable = true)
    private final void sbo$filterHubChunk(final int chunkX, final int chunkZ, @NonNull final CallbackInfoReturnable<Boolean> cir) {
        // Runs for every chunk lookup, so the plain field checks go before the setting
        if (!HubChunkCache.isActive() || !World.INSTANCE.isInHub() || !FakeChunkManagerKt.renderHubIslandOnlyEnabled() || FakeChunkManagerKt.inArea(chunkX, chunkZ, ArrowGuessBurrow.INSTANCE.getHUB_BOUNDS())) return;
        cir.setReturnValue(false);
    }
}
