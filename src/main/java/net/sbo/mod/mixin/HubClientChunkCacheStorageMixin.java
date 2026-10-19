package net.sbo.mod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.sbo.mod.utils.hubcache.FakeChunkManagerKt;
import net.sbo.mod.diana.guesses.ArrowGuessBurrow;
import net.sbo.mod.utils.game.World;

@Mixin(targets = "net/minecraft/client/multiplayer/ClientChunkCache$Storage")
final class HubClientChunkCacheStorageMixin {
    @Inject(method = "inRange", at = @At("HEAD"), cancellable = true)
    private final void sbo$filterHubChunk(final int chunkX, final int chunkZ, @NonNull final CallbackInfoReturnable<Boolean> cir) {
        if (!FakeChunkManagerKt.renderHubIslandOnlyEnabled() || !World.INSTANCE.isInHub() || FakeChunkManagerKt.inArea(chunkX, chunkZ, ArrowGuessBurrow.INSTANCE.getHUB_BOUNDS())) return;
        cir.setReturnValue(false);
    }
}
