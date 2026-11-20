package net.sbo.mod.diana.guesses

import net.minecraft.world.level.ChunkPos
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.diana.burrows.BurrowParticleClock
import net.sbo.mod.settings.categories.Burrows
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.game.Mayor
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.waypoint.Waypoint
import net.sbo.mod.utils.waypoint.WaypointManager
import java.io.DataInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * The Spade Guess Optimizer: places to stand and use the spade to find a new chain.
 *
 * Look For Max Chains: after starting the game and /sboclearburrows it shows the best places across the
 * Hub until [STARTUP_CHAINS] burrows are known, or until [STARTUP_REPEATS] spade uses in a row point at burrows already
 * known (no new chain is left near enough). Those come before any burrow.
 *
 * After that, every time a chain is finished the spade is used where it was finished first. Only when that finds no new
 * chain (its guess points at a burrow known before it, or no guess comes within [NO_GUESS_NANOS]) it shows the best
 * place within [detourFor] blocks of where the chain was finished, however many chains are going. A spade use that finds
 * a new chain removes it till the next chain is finished. It comes before any burrow, but the warp key doesn't warp to it.
 *
 * Before any dig and after a chain is finished the spade points at the nearest burrow, so from a place it only finds a
 * new chain closer than every known burrow and guess (its reach). After any other dig it points at the next burrow of
 * that chain however far it is, so nothing is shown until the chain is finished.
 *
 * Every block a burrow can be on in the Hub is shipped in [SPOTS_RESOURCE]. A block counts as searched when:
 * - its burrow particles would have shown while you held the spade: within [BurrowParticleClock.SHOWN_RANGE] of
 *   [BurrowParticleClock.shownFrom]. Only with Close Burrow Detection, without it a burrow there is never found
 * - a spade guess pointed farther away than the block, seen from where the spade was used while it points at the
 *   nearest burrow
 *
 * A place is as good as the amount of unsearched blocks within its reach. The best places are picked
 * one after another, each only counting the blocks the ones before it don't cover, and the nearest of them to you is
 * shown. A place can be on a searched block, what counts is what a spade use there still finds. They stay until
 * under [MIN_COVERED_SHARE] of the unsearched blocks is within their reach, then the next ones are picked. The shown place only moves after a spade
 * use, holding the spade while walking up to it doesn't move it. It goes away when the spade is used, the next one shows
 * once the spade guess settled. Searched blocks are cleared when a chain
 * finishes (a new chain starts anywhere) and on /sboclearburrows. A lobby swap keeps them, the burrows stay the same.
 */
object SpadeGuessOptimizer {
    private const val SPOTS_RESOURCE = "/assets/sbo/diana/hub_spots.bin.gz"
    private const val WAYPOINT_TYPE = "spadeSpot"
    /** How far around where you held the spade every burrow showed, and the least reach a place needs. */
    private const val HOLD_SEARCH_RADIUS = BurrowParticleClock.SHOWN_RANGE
    /** Holding the spade only searches again after moving this far. */
    private const val HOLD_MOVE_DISTANCE = 2.0
    /** A burrow just as far as the guessed one could have been the one pointed at, so this much less is searched. */
    private const val THROW_DISTANCE_MARGIN = 3.0
    /** The spade guess is final when it did not move for this long (the particles come every 0.1 seconds). */
    private val THROW_SETTLE_NANOS = TimeUnit.SECONDS.toNanos(1)
    /** Waypoints this close are the same burrow, like a guess and the burrow found at it. */
    private const val SAME_BURROW_DISTANCE = 3.0
    /** With no known burrow, how far around a place its unsearched blocks count. */
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
    /** The same for the old spade guess, it is less precise and merged with burrows this close. */
    private const val OLD_GUESS_KNOWN_BURROW_DISTANCE = 30.0
    /** A spade use with no spade guess this long after it found nothing, the first guess comes within ~350 ms almost always. */
    private val NO_GUESS_NANOS = TimeUnit.MILLISECONDS.toNanos(500)
    /** The same for the old spade guess, it needs 4 particles (~420 ms) for its first guess. */
    private val OLD_GUESS_NO_GUESS_NANOS = TimeUnit.MILLISECONDS.toNanos(800)
    /** A place is only worth going to with this share of all unsearched blocks within its reach. */
    private const val MIN_COVERED_SHARE = 0.03
    /** The most places near a finished chain that are tried, evenly out of all of them. */
    private const val MAX_TRIED_SPOTS = 400
    /** The waypoint types that are a burrow, found or guessed. */
    private val BURROW_TYPES = listOf("burrow", "arrow", "guess")

    /** Every spot's block, one array per axis: there are about 37 000 of them. */
    private var spotX = IntArray(0)
    private var spotY = IntArray(0)
    private var spotZ = IntArray(0)
    /** Whether each spot was searched since the last chain finished. */
    private var searched = BooleanArray(0)
    /** The spots in each chunk by [ChunkPos.pack]. */
    private val spotsByChunk = HashMap<Long, IntArray>()
    /** Whether [loadSpots] ran, the spots load the first time you're in the Hub with a spade. */
    private var spotsLoaded = false
    /** Goes up every time a spot gets searched or the searched spots are cleared. */
    private var searchVersion = 0
    /** The unsearched spots per chunk and their middle, as of [unsearchedVersion]. */
    private var unsearchedByChunk = HashMap<Long, Int>()
    private var middleByChunk = HashMap<Long, SboVec>()
    private var unsearchedTotal = 0
    private var unsearchedVersion = -1

    /** Where holding the spade last searched from, see [HOLD_MOVE_DISTANCE]. */
    private var lastHoldSearch: SboVec? = null

    /** The last spade use and the burrow its guess points at, searched once the guess settles. */
    private var throwFrom: SboVec? = null
    private var throwGuess: SboVec? = null
    /** When the last spade guess moved, it settles after [THROW_SETTLE_NANOS]. */
    private var throwGuessTime = 0L
    /** Whether the spade points at the nearest burrow, which is before any dig and after a chain is finished. */
    private var pointsAtNearest = true
    /** Whether Look For Max Chains is still looking: [STARTUP_CHAINS] burrows were not known yet since starting the game or /sboclearburrows. */
    private var startup = true
    /** Spade uses in a row at startup that pointed at a burrow already known. */
    private var startupRepeats = 0
    /** The known burrows on the last tick, and when the current spade use started. */
    private var lastKnownBurrows = emptyList<SboVec>()
    private var knownBeforeThrow = emptyList<SboVec>()

    /** The picked spots, each removed once it is searched or a burrow is found within [HOLD_SEARCH_RADIUS] blocks of it. */
    private val picked = mutableListOf<Int>()
    /** The picked spot nearest to you, the one shown. */
    private var shown: Int? = null
    /** The place the Use Spade title was last shown for, it is shown once per place. */
    private var titled: Int? = null
    /** Where the last chain was finished, places are picked within [detourFor] blocks of it. */
    private var chainEnd: SboVec? = null
    /** Whether the spade was not used yet where the last chain was finished, no place is shown till it finds nothing new there. */
    private var waitingForSpade = false
    /** When the spade was last used, 0 once it got a spade guess or none in time. */
    private var spadeUseTime = 0L
    /** Whether a spade use settled since the last update, only then the shown place can move. */
    private var spadeUsed = false
    /** Whether the shown place went away for a spade use, the next one waits for its guess to settle. */
    private var hiddenForUse = false
    /** The waypoint of the shown place, empty while none is shown. */
    private val waypoints = mutableListOf<Waypoint>()
    /** The known burrows and searched spots the last pick was made with, nothing changes picking again with them. */
    private var lastPickKey: Any? = null
    /** The searched spots, startup, detour and shown place the picked spots were last checked with, see [update]. */
    private var checkedSearchVersion = -1
    private var checkedShown: Int? = null
    private var checkedStartup = true
    private var checkedDetour = 0.0

    fun init() {
        Register.onTick(1) { tick() }
    }

    /**
     * Clears all searched spots, called when a chain is finished and on /sboclearburrows.
     * @param chainFinished Whether a chain was finished, otherwise Look For Max Chains starts again.
     */
    fun reset(chainFinished: Boolean = false) {
        searched.fill(false)
        searchVersion++
        lastHoldSearch = null
        picked.clear()
        throwFrom = null
        throwGuess = null
        pointsAtNearest = true
        chainEnd = null
        titled = null
        spadeUsed = false
        spadeUseTime = 0L
        hiddenForUse = false
        waitingForSpade = chainFinished
        if (chainFinished) {
            chainEnd = mc.player?.let { SboVec(it.x, it.y, it.z) }
        } else {
            startup = true
            startupRepeats = 0
        }
    }

    /** A lobby swap keeps your burrows, only a spade use cut off by it is dropped. */
    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        throwFrom = null
        throwGuess = null
        spadeUseTime = 0L
    }

    /** Called when a burrow of a chain is dug that is not its last one. */
    fun onChainDig() {
        searchThrow()
        pointsAtNearest = false
    }

    /** Called every time the spade is used, the shown place goes away till its guess settles. */
    fun onUseSpade() {
        spadeUseTime = System.nanoTime()
        if (shown == null) return
        show(null)
        hiddenForUse = true
    }

    /**
     * Called every time the spade guess changes.
     * @param from The eye position the spade was used from.
     * @param guess The burrow the spade guess points at.
     */
    fun onSpadeGuess(from: SboVec, guess: SboVec) {
        val lastFrom = throwFrom
        if (lastFrom != null && lastFrom != from) searchThrow()
        if (lastFrom != from) knownBeforeThrow = lastKnownBurrows
        spadeUseTime = 0L
        throwFrom = from
        throwGuess = guess
        throwGuessTime = System.nanoTime()
    }

    private fun tick() {
        if (!Burrows.spadeGuessOptimizer) {
            picked.clear()
            lastPickKey = null
            throwFrom = null
            throwGuess = null
            spadeUseTime = 0L
            hiddenForUse = false
            if (shown != null) show(null)
            return
        }
        // The picked spots stay, the place shows again when you're back with a spade while Diana is on
        if (!World.isInHub() || !Helper.hasSpade || !Mayor.dianaActive) {
            if (shown != null) show(null)
            return
        }
        if (!spotsLoaded) loadSpots()
        if (spotX.isEmpty()) return
        val player = mc.player ?: return

        if (throwFrom != null && System.nanoTime() - throwGuessTime > THROW_SETTLE_NANOS) searchThrow()
        // No guess from a spade use: nothing new at the chain end, and the shown place is used
        val noGuessNanos = if (Burrows.curveSpadeGuess) NO_GUESS_NANOS else OLD_GUESS_NO_GUESS_NANOS
        if (spadeUseTime != 0L && throwFrom == null && System.nanoTime() - spadeUseTime > noGuessNanos) {
            spadeUseTime = 0L
            waitingForSpade = false
            spadeUsed = true
        }

        val shownFrom = if (Burrows.closeBurrowDetection) BurrowParticleClock.shownFrom() else null
        if (shownFrom != null) {
            val last = lastHoldSearch
            if (last == null || last.distanceTo(shownFrom) >= HOLD_MOVE_DISTANCE) {
                lastHoldSearch = shownFrom
                search(shownFrom, HOLD_SEARCH_RADIUS)
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
     * Searches everything closer than the burrow the last spade guess points at, if the spade points at the nearest burrow.
     * A guess pointing at no burrow known before the spade use found a new chain: after a finished chain no place is
     * needed anymore then.
     */
    private fun searchThrow() {
        val from = throwFrom ?: return
        val guess = throwGuess ?: return
        throwFrom = null
        throwGuess = null
        spadeUsed = true
        if (!pointsAtNearest) return
        val radius = from.distanceTo(SboVec(guess.x + 0.5, guess.y + 1.0, guess.z + 0.5)) - THROW_DISTANCE_MARGIN
        if (radius > 0) search(from, radius)
        val knownDistance = if (Burrows.curveSpadeGuess) KNOWN_BURROW_DISTANCE else OLD_GUESS_KNOWN_BURROW_DISTANCE
        val known = knownBeforeThrow.any { it.distanceTo(guess) <= knownDistance }
        if (startup) {
            startupRepeats = if (known) startupRepeats + 1 else 0
            if (startupRepeats >= STARTUP_REPEATS) startup = false
        } else if (!known) {
            chainEnd = null
        }
        waitingForSpade = false
    }

    /**
     * Marks every spot within [radius] blocks of [center] as searched.
     * @param center Where the search is from, the eye position or where you stand.
     * @param radius How far it reaches.
     */
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

    /** Every known burrow: found burrows, arrow guesses and spade guesses, each burrow counted once. */
    private fun knownBurrows(): List<SboVec> {
        val burrows = mutableListOf<SboVec>()
        for (type in BURROW_TYPES) {
            for (waypoint in WaypointManager.getWaypointsOfType(type)) {
                if (waypoint.hidden) continue
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
        val usingSpade = spadeUseTime != 0L || throwFrom != null
        if (!usingSpade) hiddenForUse = false
        val burrows = knownBurrows()
        val burrowsChanged = burrows != lastKnownBurrows
        lastKnownBurrows = burrows
        if (startup && (!Burrows.lookForMaxChains || burrows.size >= STARTUP_CHAINS)) startup = false
        val center = chainEnd
        if (!pointsAtNearest || !startup && (center == null || waitingForSpade)) {
            spadeUsed = false
            if (picked.isNotEmpty() || waypoints.isNotEmpty()) {
                picked.clear()
                show(null)
            }
            return
        }

        // A spade use hides the place till its guess settles, the next place depends on how far it points
        if (usingSpade) {
            if (shown != null) show(null)
            return
        }

        val detour = detourFor(burrows.size)
        // The picked spots only need checking again once something they depend on changed (a new center clears them)
        val changed = burrowsChanged || searchVersion != checkedSearchVersion || startup != checkedStartup ||
            detour != checkedDetour || shown != checkedShown
        checkedSearchVersion = searchVersion
        checkedShown = shown
        checkedStartup = startup
        checkedDetour = detour
        // The shown place stays till the spade is used, whatever got searched or found meanwhile
        val removed = (changed || spadeUsed) && picked.removeAll { spot ->
            (spot != shown || spadeUsed) && (!worthIt(spot, burrows) ||
                !startup && center != null && distanceSq(spot, center) > detour * detour)
        }
        spadeUsed = false
        val pickKey = if (picked.isEmpty()) listOf(burrows.mapTo(HashSet()) { it.roundLocationToBlock() }, searchVersion, startup, center, detour) else null
        val pick = pickKey != null && pickKey != lastPickKey
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

    /**
     * Whether nothing but the place to use the spade is led to (line, warp): while it is shown, and after a spade use there
     * till its guess settled. Only in the Hub with a spade while Diana is on, like the place itself.
     */
    fun hidesBurrows(): Boolean =
        (shown != null || hiddenForUse) && Burrows.spadeGuessOptimizer && World.isInHub() && Helper.hasSpade && Mayor.dianaActive

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
     * How far a spade use at [spot] finds a new chain: it points at the nearest burrow, so up to the nearest known one.
     * [QUALITY_RADIUS] with no known burrow.
     * @param spot The spot the spade is used at.
     * @param burrows The known burrows.
     */
    private fun reachOf(spot: Int, burrows: List<SboVec>): Double {
        if (burrows.isEmpty()) return QUALITY_RADIUS
        return sqrt(burrows.minOf { distanceSq(spot, it) }) - THROW_DISTANCE_MARGIN
    }

    /** Counts the unsearched spots per chunk again if any got searched since. */
    private fun countUnsearched() {
        if (unsearchedVersion == searchVersion) return
        unsearchedVersion = searchVersion
        unsearchedByChunk = HashMap()
        middleByChunk = HashMap()
        unsearchedTotal = 0
        for ((chunk, spots) in spotsByChunk) {
            var unsearched = 0
            var x = 0.0
            var y = 0.0
            var z = 0.0
            for (spot in spots) {
                if (searched[spot]) continue
                unsearched++
                x += spotX[spot]
                y += spotY[spot]
                z += spotZ[spot]
            }
            if (unsearched == 0) continue
            unsearchedByChunk[chunk] = unsearched
            middleByChunk[chunk] = SboVec(x / unsearched, y / unsearched, z / unsearched)
            unsearchedTotal += unsearched
        }
    }

    /** The least unsearched spots a place has to have within its reach, see [MIN_COVERED_SHARE]. */
    private fun minCovered(): Int = ceil(unsearchedTotal * MIN_COVERED_SHARE).toInt().coerceAtLeast(1)

    /**
     * Whether a spade use at [spot] is still worth it: a reach (see [reachOf]) of at least [HOLD_SEARCH_RADIUS] blocks
     * with enough unsearched spots within it.
     * @param spot The spot the spade is used at.
     * @param burrows The known burrows.
     */
    private fun worthIt(spot: Int, burrows: List<SboVec>): Boolean {
        val reach = reachOf(spot, burrows)
        if (reach < HOLD_SEARCH_RADIUS) return false
        countUnsearched()
        var covered = 0
        for ((chunk, middle) in middleByChunk) {
            if (distanceSq(spot, middle) <= reach * reach) covered += unsearchedByChunk.getValue(chunk)
        }
        return covered >= minCovered()
    }

    /**
     * Picks the best spots one after another: with a reach (see [reachOf]) of at least [HOLD_SEARCH_RADIUS] blocks, and
     * with the most unsearched spots within their reach that no spot picked before covers, at least [minCovered]. The
     * spot itself may be searched. Spots are counted per chunk, by the middle of its unsearched spots.
     * @param burrows The known burrows.
     * @param count How many spots to pick.
     * @param center Only spots within [radius] blocks of it are picked, the nearest to it of equally good ones. Anywhere in
     * the Hub if null, then one spot per chunk is tried.
     * @param radius How far from [center] spots are picked.
     */
    private fun pickSpots(burrows: List<SboVec>, count: Int, center: SboVec? = null, radius: Double = 0.0): List<Int> {
        countUnsearched()
        val middles = HashMap(middleByChunk)
        val minCovered = minCovered()

        val standable = mutableListOf<Int>()
        for (spots in spotsByChunk.values) {
            if (center != null) {
                for (spot in spots) if (distanceSq(spot, center) <= radius * radius) standable.add(spot)
            } else if (spots.isNotEmpty()) {
                val middleX = (spotX[spots[0]] shr 4) * 16 + 8
                val middleZ = (spotZ[spots[0]] shr 4) * 16 + 8
                standable.add(spots.minBy { (spotX[it] - middleX) * (spotX[it] - middleX) + (spotZ[it] - middleZ) * (spotZ[it] - middleZ) })
            }
        }
        val step = if (center == null) 1 else (standable.size + MAX_TRIED_SPOTS - 1) / MAX_TRIED_SPOTS
        val reachBySpot = HashMap<Int, Double>()
        for (index in standable.indices step step.coerceAtLeast(1)) {
            val spot = standable[index]
            val reach = reachOf(spot, burrows)
            if (reach >= HOLD_SEARCH_RADIUS) reachBySpot[spot] = reach
        }

        val picked = mutableListOf<Int>()
        repeat(count) {
            var best: Int? = null
            var bestCovered = minCovered - 1
            var bestDistanceSq = Double.MAX_VALUE
            for ((spot, reach) in reachBySpot) {
                var covered = 0
                for ((chunk, middle) in middles) {
                    if (distanceSq(spot, middle) <= reach * reach) covered += unsearchedByChunk.getValue(chunk)
                }
                val distanceSq = if (center == null) 0.0 else distanceSq(spot, center)
                if (covered > bestCovered || covered == bestCovered && covered >= minCovered && distanceSq < bestDistanceSq) {
                    best = spot
                    bestCovered = covered
                    bestDistanceSq = distanceSq
                }
            }
            val spot = best ?: return picked
            picked.add(spot)

            // What this spot covers no longer counts for the next ones
            val reach = reachBySpot.getValue(spot)
            middles.entries.removeIf { distanceSq(spot, it.value) <= reach * reach }
            reachBySpot.remove(spot)
        }
        return picked
    }

    /**
     * The squared distance from [spot] to [pos].
     * @param spot The spot.
     * @param pos The position.
     */
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
                for (spot in 0..<count) {
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
