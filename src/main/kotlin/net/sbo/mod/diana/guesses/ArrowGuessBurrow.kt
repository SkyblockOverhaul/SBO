package net.sbo.mod.diana.guesses

import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.diana.burrows.BurrowParticleClock
import net.sbo.mod.diana.burrows.ChainTracker
import net.sbo.mod.settings.categories.Burrows
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.collection.TimeLimitedSet
import net.sbo.mod.utils.events.DianaEvents
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.diana.BurrowDugEvent
import net.sbo.mod.utils.events.impl.game.TickEvent
import net.sbo.mod.utils.events.impl.packets.ParticleReceiveEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.game.ServerStats
import net.sbo.mod.utils.math.RaycastUtils
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.waypoint.Waypoint
import net.sbo.mod.utils.waypoint.WaypointManager
import net.sbo.mod.utils.chat.Chat
import java.util.*
import java.util.function.BooleanSupplier
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign
import kotlin.time.Duration.Companion.seconds

/**
 * A utility object to guess the location of Griffin Burrows based on arrow particle effects.
 *
 * The credits to this object go fully to "SidOfThe7Cs" who made this incredible logic in the Skyhanni Mod:
 *
 * https://github.com/hannibal002/SkyHanni/pull/4916
 */
object ArrowGuessBurrow {
    private const val SHAFT_LENGTH = 20
    private const val PARTICLE_DETECTION_TOLERANCE = 0.12
    private const val PARTICLE_DETECTION_TOLERANCE_SQ = PARTICLE_DETECTION_TOLERANCE * PARTICLE_DETECTION_TOLERANCE
    private const val COUNT_NEAR_TIP = 4
    private const val COUNT_NEAR_BASE = 2
    private const val EPSILON = 1e-6
    // TODO: measured with /sbodebughubscan on the live Hub and the Alpha Network (Nov 2026). Alpha's fairy forest has
    //  grass up to x 203, so its max x is 210; on live the Hub ends at 175 and a guess past it would be over the void,
    //  never loaded, and float at the arrow's height. Once the alpha changes are live, use the alpha bounds for both.
    //  Both need min y 51 (grass at y 52 in a pit near 104 -25) and max y 110 (terrace near the castle at -207 109 88).
    //  Before: x -283..175, y 60..105.
    private val LIVE_HUB_BOUNDS = AABB(-283.0, 51.0, -208.0, 175.0, 110.0, 208.0)
    private val ALPHA_HUB_BOUNDS = AABB(-283.0, 51.0, -208.0, 210.0, 110.0, 208.0)
    /** The Hub island of the server the player is on, guesses outside of it are never valid. The min is exclusive, the max inclusive. */
    val HUB_BOUNDS: AABB get() = if (HypixelModApi.isAlpha) ALPHA_HUB_BOUNDS else LIVE_HUB_BOUNDS
    private val recentFoundArrows = TimeLimitedSet<RaycastUtils.Ray>(18.seconds)
    private var spadeTitleShown = false
    private val locations = mutableSetOf<SboVec>()
    val recentClickedBlocks = TimeLimitedSet<SboVec>(4.seconds)
    val allGuesses = mutableListOf<GuessEntry>()
    private val toRemove = mutableMapOf<Waypoint, BooleanSupplier>()
    private const val MAX_INVALID_CACHE_SIZE = 1_000_000

    private val invalidCache = mutableSetOf<SboVec>()

    private var invalidCacheCapWarningSent = false

    fun invalidCacheSize(): String = "${invalidCache.size} / $MAX_INVALID_CACHE_SIZE"

    fun estimatedInvalidCacheMemoryBytes(): Long {
        val size = invalidCache.size
        if (size == 0) return 0L

        // LinkedHashSet's backing table uses a power-of-two capacity
        // and a default load factor of 0.75.
        var capacity = 16
        while (size > capacity * 0.75) {
            capacity *= 2
        }

        // Approximate:
        // SboVec: 40 bytes
        // LinkedHashMap entry: 40 bytes
        // Bucket reference: 4 bytes per table slot
        return size * 80L + capacity * 4L
    }

    fun clearInvalidCache() {
        invalidCache.clear()
        invalidCacheCapWarningSent = false
    }

    fun queueRemoval(waypoint: Waypoint, condition: BooleanSupplier) {
        toRemove[waypoint] = condition
    }

    fun flushRemovals() {
        val iterator = toRemove.entries.iterator()
        while (iterator.hasNext()) {
            val (waypoint, condition) = iterator.next()
            if (condition.asBoolean) {
                WaypointManager.removeWaypoint(waypoint)
                iterator.remove()
            }
        }
    }

    fun removeSubGuessFromInternalState(pos: SboVec) {
        allGuesses.forEach { it.removeSubGuess(pos) }
    }

    fun removeArrowGuessFromSubGuess(pos: SboVec) {
        val target = pos.roundLocationToBlock()
        val toRemove = allGuesses.filter {
            it.getRemaining().any { guess -> guess.roundLocationToBlock() == target}
        }
        toRemove.forEach { it.removeAllWaypoints() }
        allGuesses.removeAll(toRemove.toSet())
    }

    private fun guessesAt(pos: SboVec): List<GuessEntry> {
        val search = pos.roundLocationToBlock()
        return allGuesses.filter {
            val guess = it.getCurrent().roundLocationToBlock()
            guess.x == search.x && guess.y == search.y && guess.z == search.z
        }
    }

    fun removeFromInternalState(pos: SboVec) {
        val containList = guessesAt(pos)
        containList.forEach { it.removeAllWaypoints() }
        allGuesses.removeAll(containList.toSet())
    }

    fun removeOrMoveFromInternalState(pos: SboVec) {
        val containList = guessesAt(pos)
        val toRemove = mutableSetOf<GuessEntry>()
        containList.forEach {
            if (!it.moveToNext()) {
                toRemove.add(it)
                it.removeAllWaypoints()
            }
        }
        allGuesses.removeAll(toRemove)
    }

    @SboEvent
    fun onReceiveParticle(event: ParticleReceiveEvent) {
        if (!Burrows.arrowGuess || !World.isInHub()) return

        val packet = event.packet
        if (!packet.isRelevant()) return

        val location = SboVec(packet.x, packet.y, packet.z)
        if (!location.isCloseToLastBurrow()) return

        val range = getArrowRange(packet.xDist, packet.yDist, packet.zDist) ?: return
        locations.add(location)
        range.processArrowDetection()
    }

    @SboEvent
    fun onTick(event: TickEvent) {
        if (!Burrows.arrowGuess || !World.isInHub() || allGuesses.isEmpty()) return
        checkMoveGuess()
    }

    @SboEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        if (!Burrows.arrowGuess) return

        val currentChain = event.currentBurrow
        val maxChain = event.maxBurrow

        if (currentChain != maxChain) {
            locations.clear()
        }
    }

    private fun detectArrow(): RaycastUtils.Ray? {
        val line = findline()
        if (line.isEmpty()) return null
        val candidate1 = line[1]
        val candidate2 = line[line.size - 2]
        val count1 = getPointsWithinDistance(candidate1)
        val count2 = getPointsWithinDistance(candidate2)

        if (!(count1 == COUNT_NEAR_BASE && count2 == COUNT_NEAR_TIP || count1 == COUNT_NEAR_TIP && count2 == COUNT_NEAR_BASE)
        ) return null

        val base: SboVec
        val tip: SboVec
        if (count1 == COUNT_NEAR_TIP) {
            base = line.last()
            tip = line.first()
        } else {
            base = line.first()
            tip = line.last()
        }

        val adjustedBase = base.down(1.5)
        val adjustedTip = tip.down(1.5)

        return RaycastUtils.Ray(adjustedBase, adjustedTip.minus(adjustedBase).normalize())
    }

    private data class LineCandidate(
        @JvmField val points: List<SboVec>,
        @JvmField val score: Double,
        @JvmField val support: Int
    )

    private data class LocationCell(@JvmField val x: Int, @JvmField val y: Int, @JvmField val z: Int) : Comparable<LocationCell> {
        private var cachedHashCode = -1

        override fun hashCode(): Int {
            var result = cachedHashCode
            if (result == -1) {
                result = x
                result = 31 * result + y
                result = 31 * result + z
                cachedHashCode = result
            }
            return result
        }

        override fun compareTo(other: LocationCell): Int {
            val xComparison = x.compareTo(other.x)
            if (xComparison != 0) return xComparison

            val yComparison = y.compareTo(other.y)
            return if (yComparison != 0) yComparison else z.compareTo(other.z)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LocationCell) return false

            if (x != other.x) return false
            if (y != other.y) return false

            // Don't compare cachedHashCode - equals() should return true even if hashCode was not called to compute the cached hash in one of the objects. The x, y and z must still be equal for it to return true.

            return z == other.z
        }
    }

    private data class IndexedLocation(@JvmField val point: SboVec, @JvmField val order: Int)

    /** Spatial broad phase for the small-radius neighbor lookups made while extending candidate lines. */
    private class LocationIndex(locations: Set<SboVec>) {
        private val cellSize = PARTICLE_DETECTION_TOLERANCE * 2.0
        private val cells = HashMap<LocationCell, MutableList<IndexedLocation>>()

        init {
            locations.forEachIndexed { order, point ->
                cells.getOrPut(cellFor(point)) { ArrayList() }.add(IndexedLocation(point, order))
            }
        }

        fun closestUnvisitedWithinTolerance(
            origin: SboVec,
            first: SboVec,
            second: SboVec,
            visited: Set<SboVec>
        ): SboVec? {
            var closest: IndexedLocation? = null
            var closestOrder = Int.MAX_VALUE
            var minDistSq = Double.MAX_VALUE

            val tolerance = PARTICLE_DETECTION_TOLERANCE
            val minX = floor(Math.nextDown(Math.nextDown(origin.x - tolerance) / cellSize)).toInt()
            val maxX = floor(Math.nextUp(Math.nextUp(origin.x + tolerance) / cellSize)).toInt()
            val minY = floor(Math.nextDown(Math.nextDown(origin.y - tolerance) / cellSize)).toInt()
            val maxY = floor(Math.nextUp(Math.nextUp(origin.y + tolerance) / cellSize)).toInt()
            val minZ = floor(Math.nextDown(Math.nextDown(origin.z - tolerance) / cellSize)).toInt()
            val maxZ = floor(Math.nextUp(Math.nextUp(origin.z + tolerance) / cellSize)).toInt()

            val abX = second.x - first.x
            val abY = second.y - first.y
            val abZ = second.z - first.z

            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        val bucket = cells[LocationCell(x, y, z)] ?: continue
                        for (candidate in bucket) {
                            val point = candidate.point
                            val distSq = origin.distanceSq(point)

                            if (distSq > PARTICLE_DETECTION_TOLERANCE_SQ || distSq > minDistSq) continue
                            if (distSq == minDistSq && candidate.order >= closestOrder) continue
                            if (point in visited) continue

                            val acX = point.x - first.x
                            val acY = point.y - first.y
                            val acZ = point.z - first.z

                            val crossX = abY * acZ - abZ * acY
                            val crossY = abZ * acX - abX * acZ
                            val crossZ = abX * acY - abY * acX

                            if (!(crossX * crossX + crossY * crossY + crossZ * crossZ < EPSILON)) continue

                            minDistSq = distSq
                            closest = candidate
                            closestOrder = candidate.order
                        }
                    }
                }
            }

            return closest?.point
        }

        private fun cellFor(point: SboVec) = LocationCell(
            floor(point.x / cellSize).toInt(),
            floor(point.y / cellSize).toInt(),
            floor(point.z / cellSize).toInt()
        )
    }

    private fun findline(): List<SboVec> {
        val locationIndex = LocationIndex(locations)
        val candidates = buildList {
            for (start in locations) {
                val line = mutableListOf<SboVec>()
                val visited = mutableSetOf<SboVec>()
                line.add(start)
                visited.add(start)

                if (extendLine(line, visited, locationIndex)) {
                    val immutable = line.toList()
                    add(LineCandidate(immutable, scoreLine(immutable), immutable.size))
                }
            }
        }

        return candidates
            .minWithOrNull(
                compareBy<LineCandidate> { it.score }
                    .thenByDescending { it.support }
            )
            ?.points
            .orEmpty()
    }

    private fun scoreLine(line: List<SboVec>): Double {
        if (line.size < 2) return Double.POSITIVE_INFINITY

        val origin = line.first()
        val direction = line.last().minus(origin).normalize()

        return line.sumOf { point ->
            val toPoint = point.minus(origin)
            val projection = direction.dotProduct(toPoint)
            val closest = origin.add(
                direction.x * projection,
                direction.y * projection,
                direction.z * projection
            )
            point.distanceTo(closest)
        }
    }

    private fun extendLine(
        line: MutableList<SboVec>,
        visited: MutableSet<SboVec>,
        locationIndex: LocationIndex,
    ): Boolean {
        while (line.size < SHAFT_LENGTH) {
            val first = line.first()
            val second = if (line.size > 1) line[1] else first
            val last = line.last()
            val nextLoc = locationIndex.closestUnvisitedWithinTolerance(last, first, second, visited) ?: return false

            line.add(nextLoc)
            visited.add(nextLoc)
        }

        return true
    }

    private fun getPointsWithinDistance(origin: SboVec): Int {
        val maxDistSq = PARTICLE_DETECTION_TOLERANCE * PARTICLE_DETECTION_TOLERANCE
        return locations.count { it != origin && it.distanceSq(origin) <= maxDistSq }
    }

    private fun findClosestValidBlockToRayNew(ray: RaycastUtils.Ray, range: IntRange): SboVec? {
        val bounds = HUB_BOUNDS
        if (!bounds.isInside(ray.origin)) return null

        val endPoint = RaycastUtils.intersectAABBWithRay(bounds, ray)?.second ?: return null
        val diff = endPoint.minus(ray.origin).toDoubleArray()
        val axisIndex = diff.withIndex()
            .filter { (_, value) -> abs(value) > 0.9 }
            .minByOrNull { (_, value) -> abs(value) }
            ?.index
            ?: return null

        val candidates = mutableMapOf<SboVec, Pair<Double, Double>>()

        val endPointArray = endPoint.toDoubleArray()
        val originArray = ray.origin.toDoubleArray()
        val directionArray = ray.direction.toDoubleArray()

        val iterations = abs(endPointArray[axisIndex] - originArray[axisIndex]).toInt()
        for (i in 1..iterations) {
            val axisValue = originArray[axisIndex] + i * sign(directionArray[axisIndex])
            val candidatePoint = RaycastUtils.findPointOnRay(ray, axisIndex, axisValue) ?: continue
            val candidateBlock = candidatePoint.roundLocationToBlock()
            if (!isBlockValid(candidateBlock)) continue
            val blockCenter = candidateBlock.add(0.5, 0.5, 0.5)
            val distanceToRay = RaycastUtils.findDistanceToRay(ray, blockCenter)

            val distanceFromOrigin = candidatePoint.distance(ray.origin)

            val scaledDistance = distanceToRay * 500000 / distanceFromOrigin

            candidates[candidateBlock] = scaledDistance to distanceFromOrigin
        }
        if (candidates.isEmpty()) return null

        val best = candidates.entries.minWithOrNull(
            compareBy<Map.Entry<SboVec, Pair<Double, Double>>> { it.value.first }
                .thenBy { it.value.second }
        ) ?: return null

        val bestScore = best.value.first
        val possibilities = candidates.filterValues { abs(it.first - bestScore) <= 1e-6 }
        val withinRange = possibilities.filterValues { it.second.toInt() in range }.map { it.key }

        val first = withinRange.firstOrNull()
        val hasOne = first != null

        if (hasOne) {
            val entry = GuessEntry(withinRange.toMutableList())

            if (allGuesses.any { it.isEquivalentTo(entry) }) {
                return first
            }

            allGuesses.add(entry)
            ChainTracker.onArrowGuess(ray.origin, withinRange)

            WaypointManager.addArrowGuess(entry.getCurrent())

            if (Burrows.showArrowSubGuesses) {
                entry.getRemaining().forEach {
                    WaypointManager.addArrowSubGuess(it)
                }
            }
        }

        if (Burrows.showTitleWhenFailure) {
            spadeTitleShown = if (!hasOne) {
                if (!spadeTitleShown) BurrowDetector.requestSpade("failure")
                true
            } else {
                false
            }
        }

        return first
    }

    private fun checkMoveGuess() {
        if (allGuesses.isEmpty()) return
        // Where the spade's burrow particles have shown around by now, see BurrowParticleClock
        val shownFrom = BurrowParticleClock.shownFrom()
        val burrowLocations = if (shownFrom == null) emptySet() else BurrowDetector.burrows.values.asSequence().map { it.waypoint?.pos ?: SboVec.ZERO }.toHashSet()
        val shownRangeSq = BurrowParticleClock.SHOWN_RANGE * BurrowParticleClock.SHOWN_RANGE
        val toRemove = mutableSetOf<GuessEntry>()

        for (guess in allGuesses) {
            val current = guess.getCurrent()
            if (!isBlockValid(current)) {
                if (!guess.moveToNext()) {
                    toRemove.add(guess)
                }
                continue
            }
            if (shownFrom != null) {
                val isKnownBurrow = current in burrowLocations
                if (!isKnownBurrow && current.distanceSq(shownFrom) <= shownRangeSq && !guess.moveToNext()) {
                    toRemove.add(guess)
                }
            }
        }

        allGuesses.removeAll(toRemove)
    }

    private fun getArrowRange(offsetX: Float, offsetY: Float, offsetZ: Float): IntRange? {
        if (offsetX == 0f && offsetY == 128f && offsetZ == 0f) return 0..117 //Yellow Close
        if (offsetX == 255f && offsetY == 255f && offsetZ == 0f) return 112..282 //Red Medium
        if (offsetX == 255f && offsetY == 0f && offsetZ == 0f) return 281..600 //Black Far
        return null
    }

    internal fun isBlockValid(pos: SboVec): Boolean {
        if (!HUB_BOUNDS.isInside(pos)) return false
        if (pos in invalidCache) return false

        if (!pos.isInLoadedChunk()) return true
        return isBlockTrulyValid(pos)
    }

    /** [isBlockValid] without adding to the invalid block cache, the spade guess checks many blocks per particle. */
    internal fun isBlockValidNoCache(pos: SboVec): Boolean {
        if (!HUB_BOUNDS.isInside(pos)) return false
        if (pos in invalidCache) return false
        if (!pos.isInLoadedChunk()) return true
        return isGroundBlock(pos)
    }

    private fun isGroundBlock(pos: SboVec): Boolean {
        val block = pos.getBlockAt()
        val isGround = block == Blocks.GRASS_BLOCK || block == Blocks.AIR && pos in recentClickedBlocks
        return isGround && pos.up().getBlockAt() == Blocks.AIR
    }

    private fun isBlockTrulyValid(pos: SboVec): Boolean {
        val valid = isGroundBlock(pos)

        // Second condition is for an edge case where you would mine an arrow guess block and your network goes off - recentClickedBlocks would stop having the block after 4 seconds, and since network went off the server will not be able to put grass block back in again. So we check that server connection is healthy before proceeding to cache it as invalid.
        if (!valid && System.nanoTime() - ServerStats.lastPacket < TimeUnit.SECONDS.toNanos(4L) && invalidCache.size < MAX_INVALID_CACHE_SIZE) {
            invalidCache.add(pos)

            if (invalidCache.size >= MAX_INVALID_CACHE_SIZE && !invalidCacheCapWarningSent) {
                invalidCacheCapWarningSent = true

                Chat.chat("§6[SBO] §eThe Arrow Guess invalid-block cache has reached its limit of $MAX_INVALID_CACHE_SIZE entries. This is harmless, but please report it to the developers so we can investigate whether some code is calling isBlockValid excessively or for too many unique blocks.")
            }
        }

        return valid
    }

    /** True if [pos] is inside [HUB_BOUNDS]. */
    fun isInHubBounds(pos: SboVec): Boolean = HUB_BOUNDS.isInside(pos)

    private fun AABB.isInside(vec: SboVec): Boolean {
        return vec.x > this.minX && vec.x <= this.maxX &&
                vec.y > this.minY && vec.y <= this.maxY &&
                vec.z > this.minZ && vec.z <= this.maxZ
    }

    private fun ClientboundLevelParticlesPacket.isRelevant() : Boolean {
        if (this.particle.type != ParticleTypes.DUST) return false
        if (this.count != 0) return false
        if (this.maxSpeed != 1.0f) return false
        val parameters = this.particle
        return parameters is DustParticleOptions
    }

    private fun SboVec.isCloseToLastBurrow(): Boolean = DianaEvents.lastWaypointClicked?.let { this.distanceTo(it) <= 7 } != false // null at startup then never null, pass condition if its null since we do not know, but it is probably close

    private fun IntRange.processArrowDetection(): IntRange {
        val arrow = detectArrow() ?: return this
        if(!recentFoundArrows.add(arrow)) return this

        locations.clear()
        findClosestValidBlockToRayNew(arrow, this)

        return this
    }
}
