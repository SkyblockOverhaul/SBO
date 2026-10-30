package net.sbo.mod.diana.burrows

import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.math.SboVec
import java.util.concurrent.TimeUnit

/**
 * Which step of its chain each known burrow is, out of the chain's total from the dig message.
 *
 * Digging step k of a chain shows an arrow at step k + 1 of the same chain, and the spade points at it until the next
 * dig. So an arrow guess is step k + 1 of the dig it starts from (the nearest dig within [ARROW_ORIGIN_DISTANCE] blocks
 * of the arrow, in the last [LINK_NANOS]), and a spade guess made before the next dig is step k + 1 of that dig.
 * A burrow found by its particles takes the step of the guess it replaces: the nearest one within [MATCH_DISTANCE]
 * blocks, as arrow guesses can be that far off.
 */
object ChainTracker {
    private const val MATCH_DISTANCE = 10.0
    /** The arrow shows up at the dug burrow, SBO reads it within 7 blocks of it. */
    private const val ARROW_ORIGIN_DISTANCE = 8.0
    private val LINK_NANOS = TimeUnit.SECONDS.toNanos(5)
    /** Chains run out after 30 minutes without a dig. */
    private val EXPIRE_NANOS = TimeUnit.MINUTES.toNanos(30)

    class Step(val positions: List<SboVec>, val step: Int, val max: Int, val time: Long) {
        val isChainEnd: Boolean get() = step == max
    }

    private val steps = mutableListOf<Step>()

    private class Dig(val pos: SboVec, val step: Int, val max: Int, val time: Long)

    /** Recent digs whose arrow did not show yet. */
    private val digsWaitingForArrow = mutableListOf<Dig>()
    private var lastDigStep = 0
    private var lastDigMax = 0
    /** The step added for the current spade use, replaced while its guess moves. */
    private var spadeStep: Step? = null

    /**
     * Called when a burrow is dug.
     * @param current Its step in the chain.
     * @param max The chain's total.
     * @param pos Where it was, if known.
     */
    fun onBurrowDug(current: Int, max: Int, pos: SboVec?) {
        val now = System.nanoTime()
        steps.removeAll { now - it.time > EXPIRE_NANOS }
        if (pos != null) nearest(pos)?.let { steps.remove(it) }
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
        add(candidates, "arrow", dig.step + 1, dig.max)
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
        spadeStep = add(listOf(pos), "spade", lastDigStep + 1, lastDigMax)
    }

    /** After a lobby swap the spade points at the nearest burrow, so its guesses are no longer the last chain's next step. */
    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        lastDigStep = 0
        spadeStep = null
    }

    /** The step of the burrow at or guessed at [pos], or null when it is not known. */
    fun stepAt(pos: SboVec): Step? = nearest(pos)

    fun isChainEnd(pos: SboVec): Boolean = nearest(pos)?.isChainEnd == true

    fun clear() {
        steps.clear()
        lastDigStep = 0
        lastDigMax = 0
        spadeStep = null
        digsWaitingForArrow.clear()
    }

    private fun add(positions: List<SboVec>, source: String, stepNumber: Int, max: Int): Step {
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
