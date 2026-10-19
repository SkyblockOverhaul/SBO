package net.sbo.mod.utils.hubcache

import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import net.minecraft.client.multiplayer.ClientChunkCache
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.phys.AABB
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.General
import java.util.concurrent.TimeUnit

fun renderHubIslandOnlyEnabled(): Boolean {
    return General.chunkCacheRenderHubIslandOnly
}

fun inArea(chunkX: Int, chunkZ: Int, area: AABB): Boolean {
    val minX = chunkX * 16.0
    val minZ = chunkZ * 16.0

    return area.minX - 16.0 < minX + 16.0 && area.maxX + 16.0 >= minX &&
            area.minZ - 16.0 < minZ + 16.0 && area.maxZ + 16.0 >= minZ
}

/**
 * Shows fake chunks in one level (one Hub server). Chunks the server unloads are copied into the
 * [store] and replaced with a fake copy, and chunks from the store fill everything within the client
 * render distance that the server did not send (yet).
 *
 * Everything runs on the client thread except [getChunk], which render threads call too.
 */
class FakeChunkManager internal constructor(
    val level: ClientLevel,
    /** Lowercase tab list area this manager shows fake chunks for, e.g. "hub". */
    val world: String,
    private val store: MutableMap<Long, ChunkSnapshot>,
    /** Chunks outside of it are neither stored nor faked, null for no limit. */
    private val area: AABB?,
) {
    val chunkCache: ClientChunkCache = level.chunkSource

    private val fakeChunks = Long2ObjectMaps.synchronize(Long2ObjectOpenHashMap<FakeChunk>())
    // getChunk runs for every chunk vanilla misses, skip the locked lookup while there are none.
    @Volatile
    private var hasFakeChunks = false
    /** Client render distance around the player, fake chunks only live inside it. */
    private val view = ChunkSquare()
    /** Real chunks copied right before vanilla unloads them, faked once vanilla is done. */
    private val pendingReplacements = LongArrayList()
    private var dirty = true

    private val lightColumns = level.lightEngine as FakeLightColumnsExt
    private val blockLight = FakeLightExt.get(level.lightEngine.getLayerListener(LightLayer.BLOCK))
    private val skyLight =
        if (level.dimensionType().hasSkyLight()) FakeLightExt.get(level.lightEngine.getLayerListener(LightLayer.SKY)) else null

    init {
        // Left over from a session without the limit.
        if (area != null) store.keys.removeIf { !inArea(ChunkPos.getX(it), ChunkPos.getZ(it)) }
    }

    fun getChunk(chunkX: Int, chunkZ: Int): LevelChunk? {
        if (!hasFakeChunks) return null
        return fakeChunks.get(ChunkPos.pack(chunkX, chunkZ))
    }

    val fakeChunkCount: Int get() = fakeChunks.size
    val storedChunkCount: Int get() = store.size

    fun tick() {
        val player = mc.player ?: return
        val center = player.chunkPosition()
        val moved = view.update(center.x(), center.z(), mc.options.renderDistance().get()) { x, z -> unload(x, z, false) }
        if (moved || dirty) loadMissing()
    }

    /** Nearest first. */
    private fun loadMissing() {
        dirty = false
        if (store.isEmpty()) return
        val missing = LongArrayList()
        for (x in view.centerX - view.radius..view.centerX + view.radius) {
            for (z in view.centerZ - view.radius..view.centerZ + view.radius) {
                val key = ChunkPos.pack(x, z)
                if (fakeChunks.containsKey(key) || !store.containsKey(key) || !inArea(x, z)) continue
                if (chunkCache.getChunk(x, z, ChunkStatus.FULL, false) != null) continue
                missing.add(key)
            }
        }
        if (missing.isEmpty) return

        val sorted = missing.toLongArray().sortedBy {
            val dx = ChunkPos.getX(it) - view.centerX
            val dz = ChunkPos.getZ(it) - view.centerZ
            dx * dx + dz * dz
        }
        val deadline = System.nanoTime() + LOAD_BUDGET_NANOS
        for (key in sorted) {
            if (System.nanoTime() > deadline) {
                dirty = true
                return
            }
            loadFromStore(ChunkPos.getX(key), ChunkPos.getZ(key))
        }
    }

    private fun loadFromStore(chunkX: Int, chunkZ: Int) {
        val snapshot = store[ChunkPos.pack(chunkX, chunkZ)] ?: return
        val chunk = snapshot.createChunk(level, chunkX, chunkZ) ?: return
        load(chunkX, chunkZ, chunk, snapshot)
    }

    private fun load(chunkX: Int, chunkZ: Int, chunk: FakeChunk, snapshot: ChunkSnapshot) {
        fakeChunks.put(ChunkPos.pack(chunkX, chunkZ), chunk)
        hasFakeChunks = true

        lightColumns.sbo_enableFakeColumn(SectionPos.getZeroNode(chunkX, chunkZ))
        for (i in snapshot.blockLight.indices) {
            val sectionPos = SectionPos.asLong(chunkX, snapshot.minLightSection + i, chunkZ)
            blockLight?.sbo_addFakeLight(sectionPos, snapshot.blockLight[i])
            skyLight?.sbo_addFakeLight(sectionPos, snapshot.skyLight[i])
        }

        markLoaded(chunkX, chunkZ, chunk)
        level.onChunkLoaded(ChunkPos(chunkX, chunkZ))
        for (sectionY in level.minSectionY..level.maxSectionY) {
            level.setSectionDirtyWithNeighbors(chunkX, sectionY, chunkZ)
        }
        if (HubChunkCache.sodiumLoaded) SodiumCompat.onFakeChunkAdded(level, chunkX, chunkZ)
    }

    /**
     * Removes a fake chunk. [willBeReplaced] means a real chunk is about to take its place: its light stays
     * until vanilla applied the real light, so the chunk never renders dark.
     */
    fun unload(chunkX: Int, chunkZ: Int, willBeReplaced: Boolean) {
        val key = ChunkPos.pack(chunkX, chunkZ)
        val chunk = fakeChunks.remove(key) ?: return
        hasFakeChunks = !fakeChunks.isEmpty()
        chunk.clearAllBlockEntities()
        lightColumns.sbo_disableFakeColumn(SectionPos.getZeroNode(chunkX, chunkZ))

        val minLight = level.lightEngine.minLightSection
        val maxLight = level.lightEngine.maxLightSection
        val removeLight = Runnable {
            for (sectionY in minLight until maxLight) {
                val sectionPos = SectionPos.asLong(chunkX, sectionY, chunkZ)
                blockLight?.sbo_removeFakeLight(sectionPos)
                skyLight?.sbo_removeFakeLight(sectionPos)
            }
        }
        val lightQueue = mc.connection as? FakeLightQueueExt
        if (willBeReplaced && lightQueue != null) {
            // If it turned fake again in the meantime, that fake chunk brought its own light.
            lightQueue.sbo_queueFakeLightRemoval { if (!fakeChunks.containsKey(key)) removeLight.run() }
        } else {
            removeLight.run()
            markUnloaded(chunkX, chunkZ, chunk)
        }
        if (HubChunkCache.sodiumLoaded) SodiumCompat.onFakeChunkRemoved(level, chunkX, chunkZ, willBeReplaced)
    }

    /** Copies a real chunk before vanilla unloads it, and fakes it afterwards if it is still in view. */
    fun saveRealChunk(chunkX: Int, chunkZ: Int) {
        if (!inArea(chunkX, chunkZ)) return
        val chunk = chunkCache.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) ?: return
        if (chunk is FakeChunk) return
        val snapshot = ChunkSnapshot.capture(chunk) ?: return
        val key = ChunkPos.pack(chunkX, chunkZ)
        store[key] = snapshot
        if (view.contains(chunkX, chunkZ)) pendingReplacements.add(key)
    }

    fun substitutePending() {
        if (pendingReplacements.isEmpty) return
        for (i in 0 until pendingReplacements.size) {
            val key = pendingReplacements.getLong(i)
            if (fakeChunks.containsKey(key)) continue
            loadFromStore(ChunkPos.getX(key), ChunkPos.getZ(key))
        }
        pendingReplacements.clear()
    }

    /** Vanilla replaced its chunk storage (view radius change), tell it about the fake chunks again. */
    fun remarkAllLoaded() {
        synchronized(fakeChunks) {
            for (chunk in fakeChunks.values) markLoaded(chunk.pos.x(), chunk.pos.z(), chunk)
        }
    }

    /** Sodium dropped a chunk vanilla unloaded, show it again if a fake chunk took its place. */
    fun refreshSodium(chunkX: Int, chunkZ: Int) {
        if (getChunk(chunkX, chunkZ) != null) SodiumCompat.onFakeChunkAdded(level, chunkX, chunkZ)
    }

    /** Copies all real chunks that are still loaded, used when leaving the level. */
    fun captureLoaded() {
        val storage = (chunkCache as ChunkStorageExt).sbo_getStorageSquare()
        if (storage.radius < 0) return
        for (x in storage.centerX - storage.radius..storage.centerX + storage.radius) {
            for (z in storage.centerZ - storage.radius..storage.centerZ + storage.radius) saveRealChunk(x, z)
        }
        pendingReplacements.clear()
    }

    /** [unloadFakes] is only needed while the level stays in use, a replaced level is dropped anyway. */
    fun close(unloadFakes: Boolean) {
        if (unloadFakes) {
            val keys = synchronized(fakeChunks) { fakeChunks.keys.toLongArray() }
            for (key in keys) unload(ChunkPos.getX(key), ChunkPos.getZ(key), false)
        }
        fakeChunks.clear()
        hasFakeChunks = false
        pendingReplacements.clear()
    }

    fun inArea(chunkX: Int, chunkZ: Int): Boolean {
        if (area == null) return true

        return inArea(chunkX, chunkZ, area)
    }

    private fun markLoaded(chunkX: Int, chunkZ: Int, chunk: LevelChunk) {
        //#if MC > 26.1
        //$$ chunkCache.addedLoadedChunks().add(ChunkPos.pack(chunkX, chunkZ))
        //$$ chunkCache.removedLoadedChunks().remove(ChunkPos.pack(chunkX, chunkZ))
        //#endif
        val sections = chunk.sections
        for (i in sections.indices) {
            if (!sections[i].hasOnlyAir()) continue
            val sectionPos = SectionPos.asLong(chunkX, chunk.getSectionYFromSectionIndex(i), chunkZ)
            //#if MC > 26.1
            //$$ chunkCache.addedEmptySections().add(sectionPos)
            //$$ chunkCache.removedEmptySections().remove(sectionPos)
            //#else
            chunkCache.loadedEmptySections.add(sectionPos)
            //#endif
        }
    }

    private fun markUnloaded(chunkX: Int, chunkZ: Int, chunk: LevelChunk) {
        //#if MC > 26.1
        //$$ chunkCache.removedLoadedChunks().add(ChunkPos.pack(chunkX, chunkZ))
        //$$ chunkCache.addedLoadedChunks().remove(ChunkPos.pack(chunkX, chunkZ))
        //#endif
        val sections = chunk.sections
        for (i in sections.indices) {
            val sectionPos = SectionPos.asLong(chunkX, chunk.getSectionYFromSectionIndex(i), chunkZ)
            //#if MC > 26.1
            //$$ if (!sections[i].hasOnlyAir()) continue
            //$$ chunkCache.removedEmptySections().add(sectionPos)
            //$$ chunkCache.addedEmptySections().remove(sectionPos)
            //#else
            chunkCache.loadedEmptySections.remove(sectionPos)
            //#endif
        }
    }

    private companion object {
        /** Max time per tick for building fake chunks, so moving fast does not drop frames. */
        val LOAD_BUDGET_NANOS = TimeUnit.MILLISECONDS.toNanos(2)
    }
}
