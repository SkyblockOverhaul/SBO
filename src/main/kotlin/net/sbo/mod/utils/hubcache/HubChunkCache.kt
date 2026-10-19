package net.sbo.mod.utils.hubcache

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.multiplayer.ClientChunkCache
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.chat.Component
import net.minecraft.world.level.ChunkPos
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.guesses.ArrowGuessBurrow
import net.sbo.mod.settings.categories.General
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.game.World
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Lightweight Bobby for the Hub: chunks the server unloads are kept in memory and rendered as fake chunks,
 * up to the client render distance. Each chunk only has to load once per connection, also across Hub
 * servers, so the guess solvers see far chunks.
 *
 * The technique (fake LevelChunks served from ClientChunkCache, fake light in the light engine) is the one
 * Bobby by Johni0702 uses (https://github.com/Johni0702/bobby). This is an independent implementation,
 * no Bobby code is used.
 *
 * Differences to Bobby: only the areas from [General.chunkCacheAreas] (tab list "Area:", default Hub),
 * optionally only the Hub island and only while the client render distance is above the server's,
 * memory only (cleared on disconnect).
 */
object HubChunkCache {
    /** Wait after a level change before trusting the tab list area, it still shows the old server at first. */
    private val CONFIRM_DELAY_NANOS = TimeUnit.SECONDS.toNanos(3)

    @JvmField
    val sodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium")
    private val bobbyLoaded = FabricLoader.getInstance().isModLoaded("bobby")

    /** Per area, all chunks seen this connection by [ChunkPos.pack]. Client thread only, the maps itself are shared. */
    private val stores = HashMap<String, ConcurrentHashMap<Long, ChunkSnapshot>>()

    @Volatile
    private var manager: FakeChunkManager? = null
    private var levelChangedAt = System.nanoTime()
    /** A level that was already checked and gets no fake chunks (not a cached world, or Bobby runs in it). */
    private var skippedLevel: ClientLevel? = null
    private var lastAreas: Set<String> = emptySet()
    private var lastIslandOnly = General.chunkCacheHubIslandOnly
    private var ticks = 0

    fun init() {
        Register.onTick(1) { tick() }
    }

    @JvmStatic
    fun managerFor(cache: ClientChunkCache): FakeChunkManager? {
        val current = manager ?: return null
        return if (current.chunkCache === cache) current else null
    }

    /** True while fake chunks are shown, the render distance then ignores the server's limit. */
    @JvmStatic
    fun isActive(): Boolean = manager != null

    /** Debug line for F3, appended to the client chunk cache stats. */
    @JvmStatic
    fun debugString(): String {
        val current = manager ?: return "SBO: off"
        return "SBO: F ${current.fakeChunkCount} S ${current.storedChunkCount}"
    }

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        manager?.let {
            // Vanilla does not unload the chunks of a level it leaves, so copy them now.
            it.captureLoaded()
            it.close(unloadFakes = false)
            (mc.connection as? FakeLightQueueExt)?.sbo_flushFakeLightRemoval()
            SBOKotlin.logger.debug("[HubChunkCache] Left ${it.world}, ${it.storedChunkCount} chunks stored")
        }
        manager = null
        skippedLevel = null
        levelChangedAt = System.nanoTime()
    }

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        // Fired on the network thread, everything here belongs to the client thread.
        mc.execute {
            manager?.close(unloadFakes = false)
            manager = null
            skippedLevel = null
            // Biomes in the snapshots belong to this connection's registries.
            stores.clear()
            SBOKotlin.logger.debug("[HubChunkCache] Disconnected, stores cleared")
        }
    }

    private fun tick() {
        val current = manager
        if (!General.hubChunkCache) {
            if (current != null) {
                current.close(unloadFakes = true)
                manager = null
            }
            stores.clear()
            return
        }

        val areas = cachedAreas()
        if (areas != lastAreas) {
            lastAreas = areas
            skippedLevel = null // an added area may be the one the player is in
            stores.keys.retainAll(areas)
            if (current != null && current.world !in areas) {
                current.close(unloadFakes = true)
                manager = null
                return
            }
        }

        if (General.chunkCacheHubIslandOnly != lastIslandOnly) {
            lastIslandOnly = General.chunkCacheHubIslandOnly
            if (current != null) {
                // Restarted next tick with the new limit.
                current.close(unloadFakes = true)
                manager = null
                skippedLevel = null
                return
            }
        }

        val level = mc.level ?: return
        if (current != null) {
            if (current.level !== level) {
                current.close(unloadFakes = false)
                manager = null
            } else if (!extendsServerView(level)) {
                current.close(unloadFakes = true)
                manager = null
            } else {
                current.tick()
                if (++ticks % 200 == 0) logStats(current)
            }
            return
        }

        if (level === skippedLevel) return
        if (System.nanoTime() - levelChangedAt < CONFIRM_DELAY_NANOS) return
        val world = World.getWorld()
        if (world == "None") return // tab list not there yet
        if (world.lowercase() !in areas) {
            skippedLevel = level
            return
        }
        if (!World.isInSkyblock()) return // scoreboard not there yet
        if (!extendsServerView(level)) return // checked again every tick, the setting and render distance can change
        skippedLevel = level
        if (bobbyLoaded && BobbyCompat.isEnabled()) {
            // Bobby already shows fake chunks in this level, two caches would fight. Ours starts on the
            // next level, once Bobby is off.
            BobbyCompat.disableOnce()
            return
        }

        val key = world.lowercase()
        val store = stores.getOrPut(key) { ConcurrentHashMap() }
        skippedLevel = null
        val area = if (key == "hub" && General.chunkCacheHubIslandOnly) ArrowGuessBurrow.HUB_BOUNDS else null
        manager = FakeChunkManager(level, key, store, area)
        SBOKotlin.logger.info("[HubChunkCache] Showing fake chunks in $world, ${store.size} chunks stored")
    }

    /**
     * Fake chunks only fill the client render distance, so while the server already sends that far they only
     * help right after a lobby switch. [General.chunkCacheAboveServerOnly] skips them then.
     */
    private fun extendsServerView(level: ClientLevel): Boolean {
        if (!General.chunkCacheAboveServerOnly) return true
        return mc.options.renderDistance().get() > 8
    }

    private fun logStats(current: FakeChunkManager) {
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) shr 20
        SBOKotlin.logger.debug(
            "[HubChunkCache] ${current.world}: fake=${current.fakeChunkCount} stored=${current.storedChunkCount} " +
                "areas=${stores.mapValues { it.value.size }} heap=${usedMb}MB/${runtime.maxMemory() shr 20}MB"
        )
    }

    private fun cachedAreas(): Set<String> =
        General.chunkCacheAreas.mapNotNullTo(HashSet()) { it.trim().lowercase().ifEmpty { null } }

    /** Bobby is not a dependency, so it is only reached through its config file and reflection. */
    private object BobbyCompat {
        fun isEnabled(): Boolean = try {
            val bobby = Class.forName("de.johni0702.minecraft.bobby.Bobby")
            val instance = bobby.getMethod("getInstance").invoke(null)
            bobby.getMethod("isEnabled").invoke(instance) as Boolean
        } catch (e: ReflectiveOperationException) {
            SBOKotlin.logger.warn("[HubChunkCache] Could not check if Bobby is enabled", e)
            true // play safe, never run both
        }

        /**
         * Turns Bobby off in its config, but only the first time: if the player turns it back on, SBO leaves it
         * alone and just does not cache. Bobby watches its config file and picks the change up by itself.
         */
        fun disableOnce() {
            if (DataManager.sboData.bobbyDisabledBySbo) return

            val config = FabricLoader.getInstance().configDir.resolve("bobby.conf")
            try {
                val text = if (Files.exists(config)) Files.readString(config) else ""
                val enabledLine = Regex("""(?m)^([ \t]*)enabled[ \t]*[=:][ \t]*true\b""")
                val updated = if (enabledLine.containsMatchIn(text)) {
                    enabledLine.replace(text) { "${it.groupValues[1]}enabled=false" }
                } else {
                    "enabled=false\n$text"
                }
                Files.writeString(config, updated)
                DataManager.sboData.bobbyDisabledBySbo = true
            } catch (e: Exception) {
                SBOKotlin.logger.error("[HubChunkCache] Could not disable Bobby", e)
                Chat.chat("§6[SBO] §cCould not disable Bobby, please turn it off yourself so the SBO Chunk Cache can work.")
                return
            }

            Chat.chat("§6[SBO] §eBobby was disabled, the SBO Chunk Cache replaces it. It starts on your next lobby. §7You can turn Bobby back on in its settings, SBO will not touch it again.")
            SBOKotlin.toast(Component.literal("SBO disabled Bobby"), Component.literal("The SBO Chunk Cache replaces it, from your next lobby on."))
        }
    }
}
