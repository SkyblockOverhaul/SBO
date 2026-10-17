package net.sbo.mod.utils.hubcache

import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkStatus
import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTrackerHolder
import net.minecraft.client.multiplayer.ClientLevel

/**
 * Sodium tracks loaded chunks itself and does not see fake chunks. Only touched when Sodium is loaded.
 */
internal object SodiumCompat {
    fun onFakeChunkAdded(level: ClientLevel, chunkX: Int, chunkZ: Int) {
        // Fake chunks always come with light, so all flags at once.
        ChunkTrackerHolder.get(level).onChunkStatusAdded(chunkX, chunkZ, ChunkStatus.FLAG_ALL)
    }

    fun onFakeChunkRemoved(level: ClientLevel, chunkX: Int, chunkZ: Int, willBeReplaced: Boolean) {
        // When a real chunk replaces it, keep the light flag so Sodium does not hide the chunk until
        // vanilla's delayed light update arrives.
        val flags = if (willBeReplaced) ChunkStatus.FLAG_HAS_BLOCK_DATA else ChunkStatus.FLAG_ALL
        ChunkTrackerHolder.get(level).onChunkStatusRemoved(chunkX, chunkZ, flags)
    }
}
