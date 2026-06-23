package net.sbo.mod.utils.hubcache

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.sbo.mod.settings.categories.General
import net.sbo.mod.utils.game.World

/**
 * Hub Chunk Cache - provides access to block data from unloaded chunks.
 * Uses mixin to capture chunks when they unload and stores the data.
 */
object ChunkCache {

    // Hub bounds (chunk coordinates)
    const val HUB_CHUNK_MIN_X = -18
    const val HUB_CHUNK_MAX_X = 11
    const val HUB_CHUNK_MIN_Z = -14
    const val HUB_CHUNK_MAX_Z = 13


    /**
     * Check if caching is enabled (in Hub and Bobby not loaded)
     */
    @JvmStatic
    fun isEnabled(): Boolean {
        if (!General.hubChunkCache) return false
        if (World.getWorld() != "Hub") return false
        if (isBobbyLoaded()) return false
        return true
    }

    /**
     * Get block state at position
     */
    fun getBlockState(pos: BlockPos): BlockState? {
        if (!isEnabled()) return null

        val chunkPos = ChunkPos(pos)

        if (!isInHubBounds(chunkPos.x, chunkPos.z)) return null

        return ChunkCacheManager.getCachedChunk(chunkPos.x, chunkPos.z)
            ?.getBlockState(pos)
    }

    /**
     * Check if position is within Hub bounds
     */
    fun isInHubBounds(chunkX: Int, chunkZ: Int): Boolean {
        return chunkX in HUB_CHUNK_MIN_X..HUB_CHUNK_MAX_X &&
                chunkZ in HUB_CHUNK_MIN_Z..HUB_CHUNK_MAX_Z
    }


    private fun isBobbyLoaded(): Boolean {
        return try {
            Class.forName("de.johni0702.minecraft.bobby.Bobby")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}