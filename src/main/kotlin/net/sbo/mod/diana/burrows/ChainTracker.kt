package net.sbo.mod.diana.burrows

import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.TickEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.waypoint.WaypointManager
import java.util.concurrent.TimeUnit

/**
 * Which step of its chain each known burrow and guess is, out of the chain's total from the dig message. Used for the
 * chain end color and to show a chain end treasure burrow as dug out with one click.
 *
 * Digging step k of a chain shows an arrow at step k + 1 of the same chain, and the spade points at it until the next
 * dig. So an arrow guess is step k + 1 of the dig it starts from (the nearest dig within [ARROW_ORIGIN_DISTANCE] blocks
 * of the arrow, in the last [LINK_NANOS]), and a spade guess made before the next dig is step k + 1 of that dig.
 * A burrow found by its particles takes the step of the guess it replaces: the nearest one within [MATCH_DISTANCE]
 * blocks, as arrow guesses can be that far off.
 *
 * Only one chain runs at a time and every step is added after the dig before it, so all steps are dropped on the next dig
 * (the ones not dug were wrong guesses, or from a chain that ran out). A step is also dropped once no guess or burrow is
 * left near it (a wrong guess that was removed, or a burrow removed by its smoke).
 */
object ChainTracker {
    private const val MATCH_DISTANCE = 10.0
    /** The arrow shows up at the dug burrow, SBO reads it within 7 blocks of it. */
    private const val ARROW_ORIGIN_DISTANCE = 8.0
    private val LINK_NANOS = TimeUnit.SECONDS.toNanos(5)
    /** Chains run out after 30 minutes without a dig. */
    private val EXPIRE_NANOS = TimeUnit.MINUTES.toNanos(30)
    private const val PRUNE_TICKS = 20

    class Step(val positions: List<SboVec>, val step: Int, val max: Int, val time: Long) {
        val isChainEnd: Boolean get() = step == max

        fun distanceTo(pos: SboVec): Double = positions.minOf { it.distanceTo(pos) }
    }

    private val steps = mutableListOf<Step>()

    private class Dig(val pos: SboVec, val step: Int, val max: Int, val time: Long)

    /** Recent digs whose arrow did not show yet. */
    private val digsWaitingForArrow = mutableListOf<Dig>()
    private var lastDigStep = 0
    private var lastDigMax = 0
    /** The step added for the current spade use, replaced while its guess moves. */
    private var spadeStep: Step? = null
    private var ticks = 0

    /**
     * Called when a burrow is dug.
     * @param current Its step in the chain.
     * @param max The chain's total.
     * @param pos The block dug, if known.
     */
    fun onBurrowDug(current: Int, max: Int, pos: SboVec?) {
        val now = System.nanoTime()
        // The chain moved on, finished or a new one started: the steps not dug were wrong guesses
        steps.clear()
        lastDigStep = current
        lastDigMax = max
        spadeStep = null
        digsWaitingForArrow.removeAll { now - it.time > LINK_NANOS }
        if (pos != null && current < max) digsWaitingForArrow.add(Dig(pos, current, max, now))
    }

    /**
     * Called when an arrow guess is made.
     * @param origin Where the arrow starts, at the burrow that was dug.
     * @param candidates The blocks along the arrow the burrow can be on, best first.
     */
    fun onArrowGuess(origin: SboVec, candidates: List<SboVec>) {
        if (candidates.isEmpty()) return
        val now = System.nanoTime()
        digsWaitingForArrow.removeAll { now - it.time > LINK_NANOS }
        val dig = digsWaitingForArrow.filter { it.pos.distanceTo(origin) <= ARROW_ORIGIN_DISTANCE }.minByOrNull { it.pos.distanceTo(origin) } ?: return
        digsWaitingForArrow.remove(dig)
        // A spade guess for the same burrow made before the arrow showed is replaced
        spadeStep?.takeIf { s -> candidates.any { it.distanceTo(s.positions[0]) <= MATCH_DISTANCE } }?.let {
            steps.remove(it)
            spadeStep = null
        }
        add(candidates, dig.step + 1, dig.max)
    }

    /**
     * Called every time the spade guess changes.
     * @param pos The burrow it points at.
     */
    fun onSpadeGuess(pos: SboVec) {
        // Only after a dig that did not finish its chain does the spade point at that chain's next burrow
        if (lastDigStep == 0 || lastDigStep >= lastDigMax) return
        spadeStep?.let { steps.remove(it) }
        spadeStep = null
        if (nearest(pos) != null) return
        spadeStep = add(listOf(pos), lastDigStep + 1, lastDigMax)
    }

    /** After a lobby swap the spade points at the nearest burrow, so its guesses are no longer the last chain's next step. */
    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        lastDigStep = 0
        spadeStep = null
    }

    @SboEvent
    fun onTick(event: TickEvent) {
        if (steps.isEmpty() || ++ticks % PRUNE_TICKS != 0) return
        val now = System.nanoTime()
        val waypoints = WaypointManager.getAllGuessesAndBurrows()
        steps.removeAll { step ->
            now - step.time > EXPIRE_NANOS || waypoints.none { step.distanceTo(it.pos) <= MATCH_DISTANCE }
        }
        if (spadeStep != null && spadeStep !in steps) spadeStep = null
    }

    /**
     * Whether the waypoint at [pos] is the end of its chain.
     * @param type The waypoint's type. Burrows are often close together, so of the known burrows only the one closest to
     * the chain end step is it.
     */
    fun isChainEnd(pos: SboVec, type: String): Boolean {
        val step = nearest(pos)?.takeIf { it.isChainEnd } ?: return false
        if (type != "burrow") return true
        val distance = step.distanceTo(pos)
        return WaypointManager.getWaypointsOfType("burrow").none { step.distanceTo(it.pos) < distance }
    }

    fun clear() {
        steps.clear()
        lastDigStep = 0
        lastDigMax = 0
        spadeStep = null
        digsWaitingForArrow.clear()
    }

    private fun add(positions: List<SboVec>, stepNumber: Int, max: Int): Step {
        val step = Step(positions, stepNumber, max, System.nanoTime())
        steps.add(step)
        return step
    }

    private fun nearest(pos: SboVec): Step? {
        var best: Step? = null
        var bestDistance = MATCH_DISTANCE
        for (step in steps) {
            for (p in step.positions) {
                val distance = p.distanceTo(pos)
                if (distance <= bestDistance) {
                    bestDistance = distance
                    best = step
                }
            }
        }
        return best
    }
}
