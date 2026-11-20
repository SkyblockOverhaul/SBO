package net.sbo.mod.diana.burrows

import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.TickEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.events.impl.packets.ParticleReceiveEvent
import net.sbo.mod.utils.game.InventoryUtils
import net.sbo.mod.utils.game.ServerStats
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.math.SboVec
import kotlin.math.abs
import kotlin.math.min

/**
 * When burrow particles show after taking out the spade.
 *
 * Hypixel looks at your hand every [CYCLE_TICKS] server ticks (not every 20). If the spade is in it, it shows the burrows
 * in range until the next look. A clock point is when the particles of a look come in on the client. Every start seen
 * right after taking out the spade sets the clock again, a start later than that is from walking into range.
 *
 * All burrows in range show at once, so after the first burrow particle of a spade use the rest showed too
 * ([SHOWN_SLACK_NANOS] after it to be sure). Without one, the clock says when they would have.
 *
 * The spade has to be out [LEAD_NANOS] before a clock point to be seen at its look, and the particles can come
 * [MARGIN_NANOS] late. Both are fixed, enough for pings up to about 300 ms.
 *
 * Particles only come for where the server saw you, so a place only counts a round trip after you were there, see
 * [shownFrom]: right after a teleport the burrows near the new place did not show yet.
 *
 * The clock goes by the server's ticks from its time packets ([ServerStats.serverTickAt]), so a laggy server keeps it on
 * time. Without time packets it goes by real time.
 */
object BurrowParticleClock {
    /**
     * How far around [shownFrom] every burrow showed its particles: they show up to 32 blocks away (measured from 31.8
     * on), and [shownFrom] can be up to a block from where the server saw you.
     */
    const val SHOWN_RANGE = 31.0

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
    /** How far you can move and still be where the server saw you, see [SHOWN_RANGE]. */
    private const val SAME_PLACE_SQ = 1.0
    /** Positions kept, more than [LEAD_NANOS] of ticks. */
    private const val POSITIONS = 32

    /** The last burrow particle, one after [QUIET_NANOS] without any is a start. */
    private var lastParticle = 0L
    /** The first burrow particle since the spade was taken out, older than that when none came yet. */
    private var firstShown = 0L
    /** A point on the clock near the last start seen, 0 when none is known. */
    private var clockPoint = 0L
    /** The server tick of [clockPoint], NaN when it isn't known. */
    private var clockTick = Double.NaN
    /** The last start that was off the clock, 0 when the last one was on it. */
    private var offClockStart = 0L

    /** Where you were at a tick, and when. */
    private class Position(val time: Long, val pos: SboVec)

    /** Your positions in the Hub, one per tick, the newest last. */
    private val positions = ArrayDeque<Position>(POSITIONS)

    /**
     * The clock for a spade taken out at some time, worked out once per [shownFrom].
     * @param point The time of a point on the clock.
     * @param period How long between two points.
     * @param known Whether there is a clock to go by.
     */
    private class Clock(val point: Long, val period: Long, val known: Boolean)

    /** The clock for a spade taken out at [spadeOut]. */
    private fun clock(spadeOut: Long): Clock {
        val onServerTicks = !clockTick.isNaN() && ServerStats.serverTickAt(System.nanoTime()) != null
        val point = if (clockPoint != 0L && onServerTicks) ServerStats.timeAtTick(clockTick) else clockPoint
        val period = if (onServerTicks) (CYCLE_TICKS * ServerStats.nanosPerTick).toLong() else PERIOD_NANOS
        // A real time point drifts off over time, server ticks don't
        val known = point != 0L && (onServerTicks || spadeOut - clockPoint <= TRUST_NANOS)
        return Clock(point, period, known)
    }

    /** Every burrow particle in the Hub, with or without Close Burrow Detection. */
    @SboEvent
    fun onParticleReceive(event: ParticleReceiveEvent) {
        if (!World.isInHub() || ParticleTypes.getParticleType(event.packet) == null) return
        val now = System.nanoTime()
        val quiet = now - lastParticle > QUIET_NANOS
        lastParticle = now
        val spadeOut = InventoryUtils.heldSince("SPADE") ?: return
        if (firstShown < spadeOut) firstShown = now
        if (!quiet) return
        // Only a start right after taking out the spade is on the clock, a later one is from walking into range
        if (now - spadeOut > PERIOD_NANOS + LEAD_NANOS + MARGIN_NANOS) return
        val clock = clock(spadeOut)
        if (!clock.known) {
            setClockPoint(now)
            offClockStart = 0L
            return
        }
        val error = offBy(now, clock.point, clock.period)
        if (abs(error) <= OFF_CLOCK_NANOS) {
            // Each start is a little early or late, the clock only follows it a quarter of the way
            setClockPoint(now - error + error / 4)
            offClockStart = 0L
        } else {
            val last = offClockStart
            val again = last != 0L && abs(offBy(now, last, clock.period)) <= OFF_CLOCK_NANOS
            if (again) setClockPoint(now)
            offClockStart = if (again) 0L else now
        }
    }

    private fun setClockPoint(time: Long) {
        clockPoint = time
        clockTick = ServerStats.serverTickAt(time) ?: Double.NaN
    }

    /** How far [time] is from the nearest point of a clock going through [point], from minus to plus half a [period]. */
    private fun offBy(time: Long, point: Long, period: Long): Long =
        Math.floorMod(time - point + period / 2, period) - period / 2

    /** When the clock says the particles of a spade taken out at [spadeOut] started, they're due [MARGIN_NANOS] later. */
    private fun clockStart(spadeOut: Long): Long {
        val clock = clock(spadeOut)
        // Without a clock: the point after taking out the spade can be a whole period and the lead away
        if (!clock.known) return spadeOut + clock.period + LEAD_NANOS
        val cycles = Math.floorDiv(spadeOut + LEAD_NANOS - clock.point + clock.period - 1, clock.period)
        return clock.point + cycles * clock.period
    }

    /** When the particles of a spade taken out at [spadeOut] are due: [clockStart] or the first burrow particle. */
    private fun dueTime(spadeOut: Long, clockStart: Long): Long {
        val due = clockStart + MARGIN_NANOS
        return if (firstShown >= spadeOut) min(due, firstShown + SHOWN_SLACK_NANOS) else due
    }

    /**
     * Where you were a round trip ([LEAD_NANOS]) ago, when the server saw you there while the particles of the held spade
     * showed: every burrow within [SHOWN_RANGE] of it has shown by now. Null when there is no such place.
     */
    fun shownFrom(): SboVec? {
        val spadeOut = InventoryUtils.heldSince("SPADE") ?: return null
        val clockStart = clockStart(spadeOut)
        val now = System.nanoTime()
        if (now < dueTime(spadeOut, clockStart)) return null
        // The particles started at the clock point or with the first one
        val shownSince = if (firstShown >= spadeOut) min(clockStart, firstShown) else clockStart
        for (i in positions.indices.reversed()) {
            val position = positions[i]
            if (now - position.time < LEAD_NANOS) continue
            if (position.time >= shownSince) return position.pos
            // From before the particles started: it counts if you stayed there till they did, the server saw you there
            for (j in i + 1..<positions.size) {
                if (position.pos.distanceSq(positions[j].pos) > SAME_PLACE_SQ) return null
                if (positions[j].time >= shownSince) break
            }
            return position.pos
        }
        return null
    }

    @SboEvent
    fun onTick(event: TickEvent) {
        if (!World.isInHub()) return
        val player = mc.player ?: return
        if (positions.size == POSITIONS) positions.removeFirst()
        positions.addLast(Position(System.nanoTime(), SboVec(player.x, player.y, player.z)))
    }

    /** A new server can have its clock somewhere else. */
    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        clockPoint = 0L
        clockTick = Double.NaN
        offClockStart = 0L
        lastParticle = 0L
        firstShown = 0L
        positions.clear()
    }
}
