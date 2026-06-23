package net.sbo.mod.mixin;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.sbo.mod.utils.hubcache.ChunkCache;
import net.sbo.mod.utils.hubcache.ChunkCacheManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to capture chunk unloading and provide cached chunk data.
 */
@Mixin(ClientChunkCache.class)
public abstract class ChunkCacheMixin {

    @Shadow @Nullable
    public abstract LevelChunk getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl);

    @Shadow @Final
    private LevelChunk emptyChunk;

    /**
     * Capture when a chunk is being dropped/unloaded
     */
    @Inject(method = "drop", at = @At("HEAD"))
    private void onChunkDrop(ChunkPos pos, CallbackInfo ci) {
        if (!ChunkCache.isEnabled()) return;

        LevelChunk chunk = getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        if (chunk == null || chunk == emptyChunk) return;

        ChunkCacheManager.cacheChunk(chunk);
    }

    /**
     * Inject at the end of getChunk to check for cached data
     */
    @Inject(
        method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onGetChunk(int x, int z, ChunkStatus chunkStatus, boolean orEmpty, CallbackInfoReturnable<LevelChunk> cir) {
        // If we found a real chunk, don't interfere
        if (cir.getReturnValue() != (orEmpty ? emptyChunk : null)) {
            return;
        }

        // Only handle FULL chunks
        if (chunkStatus != ChunkStatus.FULL) return;

        // Check if we have cached data
        LevelChunk cachedChunk = ChunkCacheManager.getCachedChunk(x, z);
        if (cachedChunk != null) cir.setReturnValue(cachedChunk);
    }
}