package net.sbo.mod.diana.guesses

import net.minecraft.world.level.ChunkPos
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.settings.categories.Burrows
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.game.ItemLookup
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.waypoint.Waypoint
import net.sbo.mod.utils.waypoint.WaypointManager
import java.io.DataInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * The Spade Guess Optimizer: places to stand and use the spade to find a new chain.
 *
 * Look For Max Chains: after starting the game, a lobby swap and /sboclearburrows it shows the best places across the
 * Hub until [STARTUP_CHAINS] burrows are known, or until [STARTUP_REPEATS] spade uses in a row point at burrows already
 * known (no new chain is left near enough). Those come before any burrow.
 *
 * After that, every time a chain is finished it shows the best place within [detourFor] blocks of where it was
 * finished, however many chains are going. It comes before any burrow, but the warp key doesn't warp to it.
 *
 * Before any dig and after a chain is finished the spade points at the nearest burrow, so the places are kept
 * [MIN_BURROW_DISTANCE] blocks away from every known burrow and guess. After any other dig it points at the next burrow
 * of that chain however far it is, so nothing is shown until the chain is finished.
 *
 * Every block a burrow can be on in the Hub is shipped in [SPOTS_RESOURCE]. A block counts as searched when:
 * - the spade was held for [HOLD_TICKS] ticks within [HOLD_SEARCH_RADIUS] blocks of it (burrow particles show there)
 * - a spade guess pointed farther away than the block, seen from where the spade was used while it points at the
 *   nearest burrow
 *
 * A place is as good as the amount of unsearched blocks within [QUALITY_RADIUS] blocks of it. The best places are picked
 * one after another, each only counting the blocks the ones before it don't cover, and the nearest of them to you is
 * shown. They stay until they are searched, then the next ones are picked. Searched blocks are cleared when a chain
 * finishes (a new chain starts anywhere), on a lobby swap and on /sboclearburrows.
 */
object SpadeGuessOptimizer {
    private const val SPOTS_RESOURCE = "/assets/sbo/diana/hub_spots.bin.gz"
    private const val WAYPOINT_TYPE = "spadeSpot"
    private const val MIN_BURROW_DISTANCE = 100.0
    private const val HOLD_SEARCH_RADIUS = 30.0
    private const val HOLD_TICKS = 15
    /** Holding the spade only searches again after moving this far. */
    private const val HOLD_MOVE_DISTANCE = 2.0
    /** A burrow just as far as the guessed one could have been the one pointed at, so this much less is searched. */
    private const val THROW_DISTANCE_MARGIN = 3.0
    /** The spade guess is final when it did not move for this long (the particles come every 0.1 seconds). */
    private val THROW_SETTLE_NANOS = TimeUnit.SECONDS.toNanos(1)
    /** Waypoints this close are the same burrow, like a guess and the burrow found at it. */
    private const val SAME_BURROW_DISTANCE = 3.0
    /** How far around a place its unsearched blocks count, about how far a spade use there tells you something. */
    private const val QUALITY_RADIUS = 85.0
    /** The most chains, Look For Max Chains shows places until that many burrows are known. */
    private const val STARTUP_CHAINS = 7
    /** How many of the best places across the Hub Look For Max Chains keeps, the nearest is shown. */
    private const val STARTUP_LOCATIONS = 4
    /** How much farther from a finished chain places may be for every chain under [STARTUP_CHAINS] you have going. */
    private const val DETOUR_PER_MISSING_CHAIN = 5.0
    /** How close you have to be to the place, ignoring height, for the Use Spade title. */
    private const val USE_SPADE_DISTANCE = 5.0
    /** Spade uses in a row pointing at a known burrow that end the startup. */
    private const val STARTUP_REPEATS = 3
    /** A spade guess this close to a burrow known before the spade use points at it, arrow guesses are up to ~10 off. */
    private const val KNOWN_BURROW_DISTANCE = 10.0

    private var spotX = IntArray(0)
    private var spotY = IntArray(0)
    private var spotZ = IntArray(0)
    private var searched = BooleanArray(0)
    /** The spots in each chunk by [ChunkPos.pack]. */
    private val spotsByChunk = HashMap<Long, IntArray>()
    private var spotsLoaded = false
    /** Goes up every time a spot gets searched or the searched spots are cleared. */
    private var searchVersion = 0

    private var heldSpadeTicks = 0
    private var lastHoldSearch: SboVec? = null

    /** The last spade use and the burrow its guess points at, searched once the guess settles. */
    private var throwFrom: SboVec? = null
    private var throwGuess: SboVec? = null
    private var throwGuessTime = 0L
    /** Whether the spade points at the nearest burrow, which is before any dig and after a chain is finished. */
    private var pointsAtNearest = true
    /** Whether Look For Max Chains is still looking: [STARTUP_CHAINS] burrows were not known yet since starting the game, a lobby swap or /sboclearburrows. */
    private var startup = true
    /** Spade uses in a row at startup that pointed at a burrow already known. */
    private var startupRepeats = 0
    /** The known burrows on the last tick, and when the current spade use started. */
    private var lastKnownBurrows = emptyList<SboVec>()
    private var knownBeforeThrow = emptyList<SboVec>()

    /** The picked spots, each removed once it is searched or a burrow is found near it. */
    private val picked = mutableListOf<Int>()
    /** The picked spot nearest to you, the one shown. */
    private var shown: Int? = null
    /** The place the Use Spade title was last shown for, it is shown once per place. */
    private var titled: Int? = null
    /** Where the last chain was finished, places are picked within [detourFor] blocks of it. */
    private var chainEnd: SboVec? = null
    /** Waypoints left from the last lobby, its burrows are gone. */
    private val staleWaypoints = HashSet<Waypoint>()
    private val waypoints = mutableListOf<Waypoint>()
    /** The known burrows and searched spots the last pick was made with, nothing changes picking again with them. */
    private var lastPickKey: Any? = null

    fun init() {
        Register.onTick(1) { tick() }
    }

    /**
     * Clears all searched spots, called when a chain is finished, on a lobby swap and on /sboclearburrows.
     * @param reason Why they are cleared: "chain" when a chain is finished, the others start Look For Max Chains again.
     */
    fun reset(reason: String = "clear") {
        searched.fill(false)
        searchVersion++
        lastHoldSearch = null
        picked.clear()
        throwFrom = null
        throwGuess = null
        pointsAtNearest = true
        chainEnd = null
        titled = null
        if (reason == "chain") {
            chainEnd = mc.player?.let { SboVec(it.x, it.y, it.z) }
        } else {
            if (reason == "clear") staleWaypoints.clear()
            startup = true
            startupRepeats = 0
        }
    }

    /** A new lobby has its own burrows: the chains are found again and the old lobby's waypoints don't count. */
    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        staleWaypoints.clear()
        for (type in listOf("burrow", "arrow", "guess")) staleWaypoints.addAll(WaypointManager.getWaypointsOfType(type))
        reset("lobby")
    }

    /** Called when a burrow of a chain is dug that is not its last one. */
    fun onChainDig() {
        searchThrow()
        pointsAtNearest = false
    }

    /**
     * Called every time the spade guess changes.
     * @param from The eye position the spade was used from.
     * @param guess The burrow the spade guess points at.
     */
    fun onSpadeGuess(from: SboVec, guess: SboVec) {
        if (throwFrom != null && throwFrom != from) searchThrow()
        if (throwFrom != from) knownBeforeThrow = lastKnownBurrows
        throwFrom = from
        throwGuess = guess
        throwGuessTime = System.nanoTime()
    }

    private fun tick() {
        if (!Burrows.spadeGuessOptimizer) {
            heldSpadeTicks = 0
            picked.clear()
            show(null)
            return
        }
        if (!World.isInHub()) {
            heldSpadeTicks = 0
            return
        }
        if (!spotsLoaded) loadSpots()
        if (spotX.isEmpty()) return
        val player = mc.player ?: return

        if (throwFrom != null && System.nanoTime() - throwGuessTime > THROW_SETTLE_NANOS) searchThrow()

        heldSpadeTicks = if (isHoldingSpade()) heldSpadeTicks + 1 else 0
        if (heldSpadeTicks >= HOLD_TICKS) {
            val pos = SboVec(player.x, player.y, player.z)
            val last = lastHoldSearch
            if (last == null || last.distanceTo(pos) >= HOLD_MOVE_DISTANCE) {
                lastHoldSearch = pos
                search(pos, HOLD_SEARCH_RADIUS)
            }
        }

        update(SboVec(player.x, player.y, player.z))

        val spot = shown ?: return
        if (spot == titled) return
        val dx = spotX[spot] + 0.5 - player.x
        val dz = spotZ[spot] + 0.5 - player.z
        if (dx * dx + dz * dz <= USE_SPADE_DISTANCE * USE_SPADE_DISTANCE) {
            titled = spot
            BurrowDetector.requestSpade("spot")
        }
    }

    /**
     * Picks the place to use the spade right away, called when a chain is finished.
     * @return Whether a place is shown, then the chain end Use Spade title is left out until you are there.
     */
    fun hasSpotAfterChain(): Boolean {
        val player = mc.player ?: return false
        if (!Burrows.spadeGuessOptimizer || !World.isInHub()) return false
        if (!spotsLoaded) loadSpots()
        if (spotX.isEmpty()) return false
        update(SboVec(player.x, player.y, player.z))
        return shown != null
    }

    private fun isHoldingSpade(): Boolean {
        val item = mc.player?.mainHandItem ?: return false
        if (item.isEmpty) return false
        val sbId = ItemLookup(item).sbId
        return sbId == "DEIFIC_SPADE" || sbId == "ARCHAIC_SPADE" || sbId == "ANCESTRAL_SPADE"
    }

    /** Searches everything closer than the burrow the last spade guess points at, if the spade points at the nearest burrow. */
    private fun searchThrow() {
        val from = throwFrom ?: return
        val guess = throwGuess ?: return
        throwFrom = null
        throwGuess = null
        val radius = from.distanceTo(SboVec(guess.x + 0.5, guess.y + 1.0, guess.z + 0.5)) - THROW_DISTANCE_MARGIN
        if (pointsAtNearest && radius > 0) search(from, radius)
        if (startup && pointsAtNearest) {
            val known = knownBeforeThrow.any { it.distanceTo(guess) <= KNOWN_BURROW_DISTANCE }
            startupRepeats = if (known) startupRepeats + 1 else 0
            if (startupRepeats >= STARTUP_REPEATS) startup = false
        }
    }

    private fun search(center: SboVec, radius: Double) {
        val radiusSq = radius * radius
        val chunkRadius = (radius.toInt() shr 4) + 1
        val centerChunkX = center.x.toInt() shr 4
        val centerChunkZ = center.z.toInt() shr 4
        var changed = false
        for (chunkX in centerChunkX - chunkRadius..centerChunkX + chunkRadius) {
            for (chunkZ in centerChunkZ - chunkRadius..centerChunkZ + chunkRadius) {
                val spots = spotsByChunk[ChunkPos.pack(chunkX, chunkZ)] ?: continue
                for (spot in spots) {
                    if (searched[spot]) continue
                    val dx = spotX[spot] + 0.5 - center.x
                    val dy = spotY[spot] + 1.0 - center.y
                    val dz = spotZ[spot] + 0.5 - center.z
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        searched[spot] = true
                        changed = true
                    }
                }
            }
        }
        if (changed) searchVersion++
    }

    /** Every known burrow in this lobby: found burrows, arrow guesses and spade guesses, each burrow counted once. */
    private fun knownBurrows(): List<SboVec> {
        val burrows = mutableListOf<SboVec>()
        for (type in listOf("burrow", "arrow", "guess")) {
            for (waypoint in WaypointManager.getWaypointsOfType(type)) {
                if (waypoint.hidden || waypoint in staleWaypoints) continue
                if (burrows.none { it.distanceTo(waypoint.pos) <= SAME_BURROW_DISTANCE }) burrows.add(waypoint.pos)
            }
        }
        return burrows
    }

    /**
     * Shows or hides the place to use the spade.
     * @param playerPos Where you are.
     */
    private fun update(playerPos: SboVec) {
        val burrows = knownBurrows()
        lastKnownBurrows = burrows
        if (startup && (!Burrows.lookForMaxChains || burrows.size >= STARTUP_CHAINS)) startup = false
        val center = chainEnd
        if (!pointsAtNearest || (!startup && center == null)) {
            if (picked.isNotEmpty() || waypoints.isNotEmpty()) {
                picked.clear()
                show(null)
            }
            return
        }

        val detour = detourFor(burrows.size)
        val minDistanceSq = MIN_BURROW_DISTANCE * MIN_BURROW_DISTANCE
        val removed = picked.removeAll { spot ->
            searched[spot] || burrows.any { distanceSq(spot, it) < minDistanceSq } ||
                (!startup && center != null && distanceSq(spot, center) > detour * detour)
        }
        val pickKey = listOf(burrows.map { it.roundLocationToBlock() }.toSet(), searchVersion, startup, center, detour)
        val pick = picked.isEmpty() && pickKey != lastPickKey
        if (pick) {
            lastPickKey = pickKey
            picked.addAll(if (startup) pickSpots(burrows, STARTUP_LOCATIONS) else pickSpots(burrows, 1, center, detour))
        }
        val nearest = picked.minByOrNull { distanceSq(it, playerPos) }
        if (removed || pick || nearest != shown) show(nearest)
    }

    /** The shown place to use the spade, it comes before any burrow. */
    fun shownSpot(): Waypoint? {
        if (shown == null) return null
        return waypoints.firstOrNull()?.takeIf { !it.hidden }
    }

    /** Whether [waypoint] is a place the optimizer picked near a finished chain, those are close enough to never warp to. */
    fun isNearChainEnd(waypoint: Waypoint): Boolean = !startup && waypoint in waypoints

    /**
     * How far from the finished chain places are picked: [Burrows.optimizerDetour], and [DETOUR_PER_MISSING_CHAIN] more
     * for every chain under [STARTUP_CHAINS] you have going. The chain the spade is about to find replaces the finished
     * one, so it counts as going.
     * @param chains The other chains going, one per known burrow.
     */
    private fun detourFor(chains: Int): Double = Burrows.optimizerDetour + DETOUR_PER_MISSING_CHAIN * (STARTUP_CHAINS - 1 - chains).coerceAtLeast(0)

    /**
     * Picks the best spots one after another: unsearched, [MIN_BURROW_DISTANCE] blocks away from every known burrow, and
     * with the most unsearched spots within [QUALITY_RADIUS] blocks of their chunk that no spot picked before covers.
     * @param burrows The known burrows.
     * @param count How many spots to pick.
     * @param center Only spots within [radius] blocks of it are picked, anywhere in the Hub if null.
     */
    private fun pickSpots(burrows: List<SboVec>, count: Int, center: SboVec? = null, radius: Double = 0.0): List<Int> {
        val minDistanceSq = MIN_BURROW_DISTANCE * MIN_BURROW_DISTANCE
        val unsearchedByChunk = HashMap<Long, Int>()
        for ((chunk, spots) in spotsByChunk) unsearchedByChunk[chunk] = spots.count { !searched[it] }

        val standableByChunk = HashMap<Long, List<Int>>()
        for ((chunk, spots) in spotsByChunk) {
            val standable = spots.filter { spot ->
                !searched[spot] && (center == null || distanceSq(spot, center) <= radius * radius) &&
                    burrows.all { distanceSq(spot, it) >= minDistanceSq }
            }
            if (standable.isNotEmpty()) standableByChunk[chunk] = standable
        }

        val chunkRadius = (QUALITY_RADIUS / 16).toInt()
        val picked = mutableListOf<Int>()
        repeat(count) {
            var bestChunk: Long? = null
            var bestQuality = 0
            for ((chunk, standable) in standableByChunk) {
                val quality = sumAround(unsearchedByChunk, spotX[standable[0]] shr 4, spotZ[standable[0]] shr 4, chunkRadius)
                if (quality > bestQuality) {
                    bestQuality = quality
                    bestChunk = chunk
                }
            }
            val chunk = bestChunk ?: return picked
            val standable = standableByChunk.getValue(chunk)
            val chunkX = spotX[standable[0]] shr 4
            val chunkZ = spotZ[standable[0]] shr 4
            picked.add(standable.minBy { spot ->
                val dx = spotX[spot] - (chunkX * 16 + 8)
                val dz = spotZ[spot] - (chunkZ * 16 + 8)
                dx * dx + dz * dz
            })

            // What this spot covers no longer counts for the next ones
            for (dx in -chunkRadius..chunkRadius) {
                for (dz in -chunkRadius..chunkRadius) {
                    if (dx * dx + dz * dz > chunkRadius * chunkRadius) continue
                    unsearchedByChunk.remove(ChunkPos.pack(chunkX + dx, chunkZ + dz))
                }
            }
            standableByChunk.remove(chunk)
        }
        return picked
    }

    /** The sum of [countByChunk] over the chunks within [chunkRadius] chunks of the given chunk. */
    private fun sumAround(countByChunk: Map<Long, Int>, chunkX: Int, chunkZ: Int, chunkRadius: Int): Int {
        var sum = 0
        for (dx in -chunkRadius..chunkRadius) {
            for (dz in -chunkRadius..chunkRadius) {
                if (dx * dx + dz * dz > chunkRadius * chunkRadius) continue
                sum += countByChunk[ChunkPos.pack(chunkX + dx, chunkZ + dz)] ?: 0
            }
        }
        return sum
    }

    private fun distanceSq(spot: Int, pos: SboVec): Double {
        val dx = spotX[spot] - pos.x
        val dy = spotY[spot] - pos.y
        val dz = spotZ[spot] - pos.z
        return dx * dx + dy * dy + dz * dz
    }

    /**
     * Replaces the waypoint of the place to use the spade.
     * @param spot The spot to show, or null to only remove it.
     */
    private fun show(spot: Int?) {
        shown = spot
        waypoints.forEach { WaypointManager.removeWaypoint(it) }
        waypoints.clear()
        if (spot == null) return
        val waypoint = Waypoint("Spade Spot", spotX[spot].toDouble(), spotY[spot].toDouble(), spotZ[spot].toDouble(), type = WAYPOINT_TYPE, ttl = 0)
        waypoint.preventInvalidRemoval = true
        WaypointManager.addWaypoint(waypoint)
        waypoints.add(waypoint)
    }

    /**
     * Loads the burrow spots: an int count, then per spot a short x, an unsigned byte y and a short z.
     */
    private fun loadSpots() {
        spotsLoaded = true
        try {
            val stream = SpadeGuessOptimizer::class.java.getResourceAsStream(SPOTS_RESOURCE) ?: return
            DataInputStream(GZIPInputStream(stream).buffered()).use { input ->
                val count = input.readInt()
                spotX = IntArray(count)
                spotY = IntArray(count)
                spotZ = IntArray(count)
                for (spot in 0 until count) {
                    spotX[spot] = input.readShort().toInt()
                    spotY[spot] = input.readUnsignedByte()
                    spotZ[spot] = input.readShort().toInt()
                }
            }
        } catch (_: Exception) {
            spotX = IntArray(0)
            return
        }
        searched = BooleanArray(spotX.size)

        val chunks = HashMap<Long, MutableList<Int>>()
        for (spot in spotX.indices) chunks.getOrPut(ChunkPos.pack(spotX[spot] shr 4, spotZ[spot] shr 4)) { mutableListOf() }.add(spot)
        for ((chunk, spots) in chunks) spotsByChunk[chunk] = spots.toIntArray()
    }
}
