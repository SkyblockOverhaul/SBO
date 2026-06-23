package net.sbo.mod.utils.hubcache

import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.LevelChunk
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent

/**
 * Manages cached chunk data for Hub area.
 * Intercepts chunk unloading via mixin and stores data.
 */
object ChunkCacheManager {

    // Store cached chunks (actual LevelChunk with block data)
    private val cachedChunks: Long2ObjectMap<LevelChunk> = Long2ObjectOpenHashMap()

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        clear()
    }

    /**
     * Cache a chunk when it's being unloaded
     */
    @JvmStatic
    fun cacheChunk(chunk: LevelChunk) {
        val pos = chunk.pos
        val chunkX = pos.x
        val chunkZ = pos.z

        // Check if in Hub bounds
        if (!ChunkCache.isInHubBounds(chunkX, chunkZ)) return

        val key = ChunkPos.asLong(chunkX, chunkZ)

        // Don't re-cache if already cached
        if (cachedChunks.containsKey(key)) return


        // Store the chunk
        cachedChunks[key] = chunk

        SBOKotlin.logger.info("HubChunkCacheManager: Cached chunk $chunkX, $chunkZ (total: ${cachedChunks.size})")
    }

    /**
     * Get a cached chunk if available
     */
    @JvmStatic
    fun getCachedChunk(x: Int, z: Int): LevelChunk? {
        val key = ChunkPos.asLong(x, z)
        return cachedChunks[key]
    }

    /**
     * Clear all cached data (called when leaving Hub)
     */
    fun clear() {
        cachedChunks.clear()
        SBOKotlin.logger.info("HubChunkCacheManager: Cache cleared")
    }
}