package net.sbo.mod.diana.guesses

import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.sbo.mod.SBOKotlin
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.collection.TimeLimitedSet
import net.sbo.mod.utils.events.DianaEvents
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.diana.BurrowDugEvent
import net.sbo.mod.utils.events.impl.game.TickEvent
import net.sbo.mod.utils.events.impl.packets.ParticleReceiveEvent
import net.sbo.mod.utils.game.InventoryUtils
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.game.ServerStats
import net.sbo.mod.utils.math.RaycastUtils
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.math.SboVec.Companion.toSboVec
import net.sbo.mod.utils.waypoint.Waypoint
import net.sbo.mod.utils.waypoint.WaypointManager
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
    private val PARTICLE_DETECTION_TOLERANCE_SQ = PARTICLE_DETECTION_TOLERANCE * PARTICLE_DETECTION_TOLERANCE
    private const val COUNT_NEAR_TIP = 4
    private const val COUNT_NEAR_BASE = 2
    private const val EPSILON = 1e-6
    private val HUB_BOUNDS_MIN = SboVec(-283.0, 60.0, -208.0)
    private val HUB_BOUNDS_MAX = SboVec(175.0, 105.0, 205.0)
    /** The Hub island, guesses outside of it are never valid. */
    val HUB_BOUNDS: AABB = AABB(HUB_BOUNDS_MIN.x, HUB_BOUNDS_MIN.y, HUB_BOUNDS_MIN.z, HUB_BOUNDS_MAX.x, HUB_BOUNDS_MAX.y, HUB_BOUNDS_MAX.z)

    private val recentFoundArrows = TimeLimitedSet<RaycastUtils.Ray>(18.seconds)

    private var spadeTitleShown = false

    private val locations = mutableSetOf<SboVec>()

    val recentClickedBlocks = TimeLimitedSet<SboVec>(4.seconds)

    val allGuesses = mutableListOf<GuessEntry>()

    private val toRemove = mutableMapOf<Waypoint, BooleanSupplier>()

    private val invalidCache = mutableSetOf<SboVec>()

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

    fun removeArrowGuessFromSubGuess(pos: SboVec) {
        val target = pos.roundLocationToBlock()

        val toRemove = allGuesses.filter {
            it.getRemaining().any { guess ->
                guess.roundLocationToBlock() == target
            }
        }

        toRemove.forEach {
            it.removeAllWaypoints()
        }

        allGuesses.removeAll(toRemove.toSet())
    }

    fun removeSubGuessFromInternalState(pos: SboVec) {
        allGuesses.forEach {
            it.removeSubGuess(pos)
        }
    }

    fun removeFromInternalState(pos: SboVec) {
        val target = pos.roundLocationToBlock()

        val containList = allGuesses.filter { guessEntry ->
            val current = guessEntry.getCurrent().roundLocationToBlock()

            current.x == target.x && current.y == target.y && current.z == target.z
        }

        containList.forEach { entry ->
            entry.removeAllWaypoints()
        }

        allGuesses.removeAll(containList.toSet())
    }

    fun removeOrMoveFromInternalState(pos: SboVec) {
        val target = pos.roundLocationToBlock()

        val containList = allGuesses.filter { guessEntry ->
            val current = guessEntry.getCurrent().roundLocationToBlock()

            current.x == target.x && current.y == target.y && current.z == target.z
        }

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
        if (!Diana.arrowGuess) return
        if (!World.isInHub()) return

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
        if (!Diana.arrowGuess) return
        if (!World.isInHub()) return
        if (allGuesses.isEmpty()) return
        checkMoveGuess()
    }

    @SboEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        if (!Diana.arrowGuess) return

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
        val points: List<SboVec>,
        val score: Double,
        val support: Int
    )

    private data class LocationCell(val x: Int, val y: Int, val z: Int) : Comparable<LocationCell> {
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
    }

    private data class IndexedLocation(val point: SboVec, val order: Int)

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
            val center = cellFor(origin)
            var closest: IndexedLocation? = null
            var minDistSq = Double.MAX_VALUE

            for (x in center.x - 1..center.x + 1) {
                for (y in center.y - 1..center.y + 1) {
                    for (z in center.z - 1..center.z + 1) {
                        val bucket = cells[LocationCell(x, y, z)] ?: continue
                        for (candidate in bucket) {
                            val point = candidate.point
                            if (point in visited) continue

                            val distSq = origin.distanceSq(point)
                            if (distSq > PARTICLE_DETECTION_TOLERANCE_SQ) continue
                            if (!isCollinear(first, second, point)) continue

                            val currentClosest = closest
                            if (distSq < minDistSq ||
                                (distSq == minDistSq && currentClosest != null && candidate.order < currentClosest.order)
                            ) {
                                minDistSq = distSq
                                closest = candidate
                            }
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
            val nextLoc = locationIndex.closestUnvisitedWithinTolerance(last, first, second, visited)
            if (nextLoc == null) return false

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

            WaypointManager.addArrowGuess(entry.getCurrent())

            if (Diana.showArrowSubGuesses) {
                entry.getRemaining().forEach {
                    WaypointManager.addArrowSubGuess(it)
                }
            }
        }

        if (Diana.showTitleWhenFailure) {
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
        val player = SBOKotlin.mc.player ?: return
        val hasSpade = InventoryUtils.isItemHeld("SPADE", 1.seconds) // 20 ticks
        val burrowLocations = BurrowDetector.burrows.values.asSequence().map { it.waypoint?.pos ?: SboVec.ZERO }.toHashSet()
        val playerPos = player.position().toSboVec()
        val toRemove = mutableSetOf<GuessEntry>()

        for (guess in allGuesses) {
            val current = guess.getCurrent()
            if (!isBlockValid(current)) {
                if (!guess.moveToNext()) {
                    toRemove.add(guess)
                }
                continue
            }
            if (hasSpade) {
                val isKnownBurrow = current in burrowLocations
                if (!isKnownBurrow && current.distanceSq(playerPos) <= 1024) { // 32 blocks
                    if (!guess.moveToNext()) {
                        toRemove.add(guess)
                    }
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


    private fun isCollinear(a: SboVec, b: SboVec, c: SboVec): Boolean {
        val abX = b.x - a.x
        val abY = b.y - a.y
        val abZ = b.z - a.z

        val acX = c.x - a.x
        val acY = c.y - a.y
        val acZ = c.z - a.z

        val crossX = abY * acZ - abZ * acY
        val crossY = abZ * acX - abX * acZ
        val crossZ = abX * acY - abY * acX

        return crossX * crossX +
               crossY * crossY +
               crossZ * crossZ < EPSILON
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
        if (!valid && System.nanoTime() - ServerStats.lastPacket < TimeUnit.SECONDS.toNanos(4L)) {
            invalidCache.add(pos)
        }

        return valid
    }

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

    private fun SboVec.isCloseToLastBurrow(): Boolean = DianaEvents.lastWaypointClicked?.let { this.distanceTo(it) <= 7 } ?: true // null at startup then never null, pass condition if its null since we do not know, but it is probably close

    private fun IntRange.processArrowDetection(): IntRange {
        val arrow = detectArrow() ?: return this
        if(!recentFoundArrows.add(arrow)) return this

        locations.clear()
        findClosestValidBlockToRayNew(arrow, this)

        return this
    }
}
