package net.sbo.mod.diana.burrows

import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.Burrows
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.TickEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.game.InventoryUtils
import net.sbo.mod.utils.game.ServerStats
import net.sbo.mod.utils.math.SboVec
import kotlin.math.abs
import kotlin.math.min

/**
 * When burrow particles show after taking out the spade. Hypixel looks at your hand every [CYCLE_TICKS] server ticks
 * (not every 20) and shows the burrows in range till the next look if the spade is in it. The particles of a look come
 * in a while after it, the clock points are when they come in. Every start seen right after taking out the spade sets
 * the clock again, walking into range shows them right away. All burrows in range show at once, so the first burrow
 * particle after taking out the spade means the rest showed too ([SHOWN_SLACK_NANOS] after it to be sure), without
 * one the clock says when they would have.
 *
 * The spade has to be out [LEAD_NANOS] before a point to be seen at its look and the particles can come [MARGIN_NANOS]
 * late. Both are fixed, enough for pings up to about 300 ms.
 *
 * Particles only come for where the server saw you, so a place only counts a round trip after you were there, see
 * [shownFrom]: right after a teleport the burrows near the new place did not show yet.
 *
 * The clock goes by the server's ticks from its time packets ([ServerStats.serverTickAt]), a laggy server keeps it on
 * time. Without them, or with [Burrows.serverTickParticleClock] off, it goes by real time.
 */
object BurrowParticleClock {
    /** Server ticks between two looks at your hand, fit on the time packets of 130 spade uses. */
    private const val CYCLE_TICKS = 22
    /** The same in real time, measured over the 130 spade uses in four sessions: 1.101 to 1.103 seconds. */
    private const val PERIOD_NANOS = 1_102_000_000L
    /** How long before a clock point the spade has to be out to be seen: a round trip, so it covers high pings. */
    private const val LEAD_NANOS = 550_000_000L
    /** How late particles can come after their clock point. */
    private const val MARGIN_NANOS = 300_000_000L
    /** How long after the first burrow particle of a spade use the others in range showed, 57 of 57 came with it. */
    private const val SHOWN_SLACK_NANOS = 200_000_000L
    /** No burrow particle for this long before one makes it a start. */
    private const val QUIET_NANOS = 300_000_000L
    /** How long a real time clock point is trusted, the period is only known to about a millisecond. */
    private const val TRUST_NANOS = 180_000_000_000L
    /** A start this far off the clock is lag, it only moves the clock when the next start is off the same way. */
    private const val OFF_CLOCK_NANOS = 80_000_000L
    /** How far you can move and still be where the server saw you, the search radius is a block short of the range. */
    private const val SAME_PLACE_SQ = 1.0
    /** Positions kept, more than [LEAD_NANOS] of ticks. */
    private const val POSITIONS = 32

    private var lastParticle = 0L
    /** The first burrow particle since the spade was taken out, older than that when none came yet. */
    private var firstShown = 0L
    /** A point on the clock near the last start seen, 0 when none is known. */
    private var clockPoint = 0L
    /** The server tick of [clockPoint], NaN when it isn't known. */
    private var clockTick = Double.NaN
    /** The last start that was off the clock, 0 when the last one was on it. */
    private var offClockStart = 0L
    private val positionTimes = LongArray(POSITIONS)
    private val positionX = DoubleArray(POSITIONS)
    private val positionY = DoubleArray(POSITIONS)
    private val positionZ = DoubleArray(POSITIONS)
    /** Positions kept since the last world change, the newest is at index (positionCount - 1) % [POSITIONS]. */
    private var positionCount = 0

    /** Called for every burrow particle in the Hub. */
    fun onBurrowParticle() {
        val now = System.nanoTime()
        val quiet = now - lastParticle > QUIET_NANOS
        lastParticle = now
        val spadeOut = InventoryUtils.heldSince("SPADE") ?: return
        if (firstShown < spadeOut) firstShown = now
        if (!quiet) return
        // Only a start right after taking out the spade is on the clock, a later one is from walking into range
        if (now - spadeOut > PERIOD_NANOS + LEAD_NANOS + MARGIN_NANOS) return
        val point = point()
        if (point == 0L || !onServerTicks() && now - clockPoint > TRUST_NANOS) {
            setClockPoint(now)
            offClockStart = 0L
            return
        }
        val error = offBy(now, point)
        if (abs(error) <= OFF_CLOCK_NANOS) {
            // Each start is a little early or late, the clock only follows it a quarter of the way
            setClockPoint(now - error + error / 4)
            offClockStart = 0L
        } else {
            val last = offClockStart
            val again = last != 0L && abs(offBy(now, last)) <= OFF_CLOCK_NANOS
            if (again) setClockPoint(now)
            offClockStart = if (again) 0L else now
        }
    }

    private fun setClockPoint(time: Long) {
        clockPoint = time
        clockTick = (if (Burrows.serverTickParticleClock) ServerStats.serverTickAt(time) else null) ?: Double.NaN
    }

    /** Whether the clock goes by server ticks right now, see [BurrowParticleClock]. */
    private fun onServerTicks(): Boolean =
        Burrows.serverTickParticleClock && !clockTick.isNaN() && ServerStats.serverTickAt(System.nanoTime()) != null

    /** The time of a point on the clock. */
    private fun point(): Long = if (clockPoint != 0L && onServerTicks()) ServerStats.timeAtTick(clockTick) else clockPoint

    /** How long between two points on the clock. */
    private fun period(): Long = if (onServerTicks()) (CYCLE_TICKS * ServerStats.nanosPerTick).toLong() else PERIOD_NANOS

    /** How far [time] is from the nearest point of a clock going through [point], from minus to plus half a period. */
    private fun offBy(time: Long, point: Long): Long {
        val period = period()
        return Math.floorMod(time - point + period / 2, period) - period / 2
    }

    /** When the particles of the held spade are due, null when no spade is held. */
    fun dueTime(): Long? {
        val spadeOut = InventoryUtils.heldSince("SPADE") ?: return null
        val due = clockDue(spadeOut)
        return if (firstShown >= spadeOut) min(due, firstShown + SHOWN_SLACK_NANOS) else due
    }

    /** When the clock says the particles of a spade taken out at [spadeOut] are due. */
    private fun clockDue(spadeOut: Long): Long {
        val point = point()
        val period = period()
        // Without a clock: the point after taking out the spade can be a whole period and the lead away
        if (point == 0L || !onServerTicks() && spadeOut - clockPoint > TRUST_NANOS) {
            return spadeOut + period + LEAD_NANOS + MARGIN_NANOS
        }
        val cycles = Math.floorDiv(spadeOut + LEAD_NANOS - point + period - 1, period)
        return point + cycles * period + MARGIN_NANOS
    }

    /** Whether the spade is held and the burrow particles in range of it would show by now. */
    fun particlesShown(): Boolean = dueTime()?.let { System.nanoTime() >= it } == true

    /**
     * Where you were a round trip ([LEAD_NANOS]) ago, when the server saw you there while the particles of the held spade
     * showed: every burrow in range of it has shown by now. Null when there is no such place.
     */
    fun shownFrom(): SboVec? {
        val spadeOut = InventoryUtils.heldSince("SPADE") ?: return null
        val due = dueTime() ?: return null
        val now = System.nanoTime()
        if (now < due) return null
        // The particles started at the clock point (its due time is that much later to be sure) or with the first one
        var shownSince = clockDue(spadeOut) - MARGIN_NANOS
        if (firstShown >= spadeOut) shownSince = min(shownSince, firstShown)
        for (back in 0..<min(positionCount, POSITIONS)) {
            val i = (positionCount - 1 - back) % POSITIONS
            if (now - positionTimes[i] < LEAD_NANOS) continue
            if (positionTimes[i] >= shownSince) return position(i)
            // From before the particles started: it counts if you stayed there till they did, the server saw you there
            for (newer in back - 1 downTo 0) {
                val j = (positionCount - 1 - newer) % POSITIONS
                if (distanceSq(i, j) > SAME_PLACE_SQ) return null
                if (positionTimes[j] >= shownSince) break
            }
            return position(i)
        }
        return null
    }

    private fun position(i: Int) = SboVec(positionX[i], positionY[i], positionZ[i])

    private fun distanceSq(i: Int, j: Int): Double {
        val x = positionX[i] - positionX[j]
        val y = positionY[i] - positionY[j]
        val z = positionZ[i] - positionZ[j]
        return x * x + y * y + z * z
    }

    @SboEvent
    fun onTick(event: TickEvent) {
        val player = mc.player ?: return
        val i = positionCount % POSITIONS
        positionTimes[i] = System.nanoTime()
        positionX[i] = player.x
        positionY[i] = player.y
        positionZ[i] = player.z
        positionCount++
    }

    /** A new server can have its clock somewhere else. */
    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        clockPoint = 0L
        clockTick = Double.NaN
        offClockStart = 0L
        lastParticle = 0L
        firstShown = 0L
        positionCount = 0
    }
}
