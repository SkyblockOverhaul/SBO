package net.sbo.mod.diana.guesses

import net.minecraft.core.particles.ParticleTypes
import net.sbo.mod.SBOKotlin
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.diana.burrows.ChainTracker
import net.sbo.mod.settings.categories.Burrows
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.PlaySoundEvent
import net.sbo.mod.utils.events.impl.game.PlayerInteractEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.events.impl.packets.ParticleReceiveEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.game.ItemLookup
import net.sbo.mod.utils.math.PolynomialFitter
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.math.SboVec.Companion.toSboVec
import net.sbo.mod.utils.waypoint.WaypointManager
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * Guesses the burrow the spade points at from its dripping lava trail.
 *
 * The trail is a cubic Bezier curve:
 * - P0 is the first particle, 1 block out from the eyes along the look
 * - P1 is P0 + 4 * look + 3 blocks up
 * - P2 is P3 + 3 blocks up
 * - P3 is the top center of the burrow
 *
 * Particle i is at t = i / n, where n = 0.52518 * |P3 - P0| + 0.179.
 *
 * From the second particle on the burrow is the only unknown. Every block near the solved end that a burrow can be on
 * is tried (see [canHold]), and the one whose curve fits the particles best is used. Measured in game this gives the
 * exact block from 5 particles on, and the exact block or the one next to it from 2 particles (0.2 seconds after using
 * the spade). With 2 particles the curve can fit 2 or 3 places, the first harp note (higher the farther the burrow)
 * rules some of them out and the farthest one is used.
 *
 * A trail that stops after [SHORT_TRAIL] particles ends that close to the burrow (within about 5 blocks), so the guess is
 * solved again with that step count when its curve fits better.
 *
 * Full trails (10+ particles) only use the cubic fit when the curve found no block. The curve guess is kept otherwise,
 * the cubic fit can end on a burrow next to the one the spade points at.
 */
object PreciseGuessBurrow {
    private var particleLocations = mutableListOf<SboVec>()
    private var lastLavaParticle: Long = 0
    private var lastPointTime: Long = 0

    private var lastGuessTime: Long = 0
    private var throwLook: SboVec? = null
    private var throwEye: SboVec? = null
    private var firstNote: Float? = null

    /** The guess of the current spade use, moved as more particles come in. */
    private var throwGuess: SboVec? = null
    /** Whether [throwGuess]'s waypoint was made by this spade use, a guess that was already there is left when it moves. */
    private var ownsThrowGuess = false
    private var shortTrailChecked = false

    private val SPADE_WINDOW = TimeUnit.MILLISECONDS.toNanos(3000L)
    /** Particles come every 0.1 seconds, a particle much later is from another spade use the game did not report. */
    private val POINT_GAP = TimeUnit.MILLISECONDS.toNanos(300L)
    /** A second spade use this soon, before the first trail started, still shows the first trail. */
    private val DOUBLE_CLICK = TimeUnit.MILLISECONDS.toNanos(300L)

    private const val FULL_TRAIL = 10
    private const val SHORT_TRAIL = 3
    private const val N_PER_BLOCK = 0.52518
    private const val N_BASE = 0.179
    private const val N_SPREAD = 0.12
    private const val POINT_ERROR = 0.002
    private const val PICK_SIDE = 3
    private const val PICK_UP = 2

    // The step counts tried for the curve: N_MIN, N_MIN + N_STEP, ... up to 200
    private const val N_MIN = 5.0
    private const val N_STEP = 0.01
    private const val N_COUNT = 19501

    // Per step count, the sums over the particles summed so far: the curve end is (sx, sy, sz) / den and
    // the residual at it is saa - |(sx, sy, sz)|^2 / den, so each new particle is one pass over the step counts
    private val sx = DoubleArray(N_COUNT)
    private val sy = DoubleArray(N_COUNT)
    private val sz = DoubleArray(N_COUNT)
    private val den = DoubleArray(N_COUNT)
    private val saa = DoubleArray(N_COUNT)
    private val scores = DoubleArray(N_COUNT)
    /** Particles in the sums, particle 0 is P0 itself. */
    private var summed = 1

    fun init() {
        Register.onTick(1) { checkShortTrail() }
    }

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (!Burrows.spadeGuess) return
        this.particleLocations.clear()
        throwGuess = null
        ownsThrowGuess = false
        resetFits()
    }

    @SboEvent
    fun onReceiveParticle(event: ParticleReceiveEvent) {
        val packet = event.packet
        if (!Burrows.spadeGuess || !World.isInHub()) return
        if (packet.particle.type != ParticleTypes.DRIPPING_LAVA || packet.count != 2 || packet.maxSpeed != -0.5f) return
        val currLoc = SboVec(packet.x, packet.y, packet.z)
        val now = System.nanoTime()
        this.lastLavaParticle = now
        if (now - lastGuessTime > SPADE_WINDOW) return
        if (!Burrows.curveSpadeGuess) {
            oldGuess(currLoc)
            return
        }

        if (this.particleLocations.isNotEmpty()) {
            val distToLast = this.particleLocations.last().distanceTo(currLoc)
            if (distToLast > 3 || distToLast == 0.0) return
            if (now - lastPointTime > POINT_GAP) return
        }
        this.particleLocations.add(currLoc)
        lastPointTime = now
        if (this.particleLocations.size < 2) return

        val guess = when {
            particleLocations.size < FULL_TRAIL -> earlyGuess()
            throwGuess == null -> fullTrailGuess()
            else -> null
        } ?: return
        moveGuess(guess)
    }

    @SboEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (!Burrows.spadeGuess || !World.isInHub() || firstNote != null) return
        if (System.nanoTime() - lastGuessTime > SPADE_WINDOW) return
        val sound = event.sound
        if (sound.identifier.path != "block.note_block.harp") return
        val eye = SBOKotlin.mc.player?.eyePosition?.toSboVec() ?: return
        if (eye.distanceTo(sound.x, sound.y, sound.z) > 48) return
        firstNote = sound.pitch
    }

    @SboEvent
    fun onUseSpade(event: PlayerInteractEvent) {
        if (!Burrows.spadeGuess || !World.isInHub()) return
        val action = event.action
        if (action != "useItem" && action != "useBlock") return
        val player = SBOKotlin.mc.player
        val item = player?.mainHandItem
        if (item?.isEmpty == true) return
        if (item == null) return
        val lookup = ItemLookup(item)
        val sbId = lookup.sbId
        if (sbId != "DEIFIC_SPADE" && sbId != "ARCHAIC_SPADE" && sbId != "ANCESTRAL_SPADE") return
        val now = System.nanoTime()
        if (now - this.lastLavaParticle < TimeUnit.MILLISECONDS.toNanos(200L)) {
            event.isCanceled = true
            return
        }
        if (Burrows.curveSpadeGuess && this.particleLocations.isEmpty() && now - lastGuessTime < DOUBLE_CLICK) return
        BurrowDetector.pendingUseSpadeTitle = null
        this.particleLocations.clear()
        lastGuessTime = now
        throwLook = player.getViewVector(1f).toSboVec().normalize()
        throwEye = player.eyePosition.toSboVec()
        firstNote = null
        throwGuess = null
        ownsThrowGuess = false
        shortTrailChecked = false
        resetFits()
    }

    /** The spade guess from before the curve guess, a cubic fit of the particles, used with [Burrows.curveSpadeGuess] off. */
    private fun oldGuess(currLoc: SboVec) {
        if (this.particleLocations.isEmpty()) {
            this.particleLocations.add(currLoc)
            return
        }

        val distToLast = this.particleLocations.last().distanceTo(currLoc)
        if (distToLast > 3 || distToLast == 0.0) return
        this.particleLocations.add(currLoc)

        val guessPosition = this.guessBurrowLocation() ?: return
        val guess = guessPosition.down(0.5).roundLocationToBlock()
        WaypointManager.addSpadeGuess(guess)
        ChainTracker.onSpadeGuess(guess)
    }

    /**
     * Moves the spade guess waypoint of the current spade use to a new block.
     * @param pos The block the burrow is guessed on.
     */
    private fun moveGuess(pos: SboVec) {
        if (pos == throwGuess) return
        if (ownsThrowGuess) throwGuess?.let { WaypointManager.removeWaypointAt(it, "guess") }
        throwGuess = pos
        val existed = WaypointManager.waypointExists("guess", pos).first
        WaypointManager.addSpadeGuess(pos)
        ownsThrowGuess = !existed && WaypointManager.waypointExists("guess", pos).first
        ChainTracker.onSpadeGuess(pos)
    }

    /** Whether a burrow can be on the block: grass with air above (or not loaded yet), and on [BurrowSpots] if it has a list. */
    private fun canHold(block: SboVec) = BurrowSpots.isPossible(block) && ArrowGuessBurrow.isBlockValidNoCache(block)

    /** The first two control points of the curve, P0 and P1, or null without a look. */
    private fun curveStart(): Pair<SboVec, SboVec>? {
        val p0 = particleLocations[0]
        // Use the look the server used, from the first particle. When the spade is used on a block, the server uses the
        // rotation from the tick before, which is far off from the client's after turning quickly
        val eye = throwEye
        val look = if (eye != null && abs(p0.distanceTo(eye) - 1.0) < 0.05) (p0 - eye).normalize()
            else throwLook ?: return null
        return p0 to p0 + look * 4.0 + SboVec(0.0, 3.0, 0.0)
    }

    /** The guess for 2 to 9 particles, from the curve. */
    private fun earlyGuess(): SboVec? {
        val (p0, p1) = curveStart() ?: return null
        // Try each place the curve can end, best first, until one has a block a burrow can be on near it
        for (end in curveEnds(p0, p1)) {
            pickBlock(p0, p1, end)?.let { return it }
        }
        return null
    }

    /** Solves the guess again once the trail stopped after [SHORT_TRAIL] particles. */
    private fun checkShortTrail() {
        if (!Burrows.curveSpadeGuess || shortTrailChecked || particleLocations.size != SHORT_TRAIL) return
        if (System.nanoTime() - lastPointTime <= POINT_GAP) return
        shortTrailChecked = true
        shortTrailGuess()?.let { moveGuess(it) }
    }

    /** The guess for a trail that ended early: the step count is below its particle count, if that curve fits better. */
    private fun shortTrailGuess(): SboVec? {
        val (p0, p1) = curveStart() ?: return null
        val count = particleLocations.size.toDouble()
        val short = curveFits(p0, p1, count - 1, count).minByOrNull { it.first } ?: return null
        updateFits(p0, p1)
        if (scores[bestFit()] < short.first) return null
        return pickBlock(p0, p1, short.second)
    }

    /**
     * The places the curve can end, best first. For a given step count n the end is linear in the particles, so it is
     * solved for every n and scored by how well the particles and n fit.
     */
    private fun curveEnds(p0: SboVec, p1: SboVec): List<SboVec> {
        updateFits(p0, p1)
        val best = listOf(fitEnd(bestFit()))
        if (particleLocations.size >= 3) return best

        // With 2 particles, every good fit (a local best) that the first note allows, farthest first
        val good = ArrayList<SboVec>()
        for (k in 1 until N_COUNT - 1) {
            val score = scores[k]
            if (score <= scores[k - 1] && score <= scores[k + 1] && score < 25) good.add(fitEnd(k))
        }
        if (good.isEmpty()) return best
        val note = firstNote
        val allowed = if (note == null) good else good.filter { noteAllows(note, it.distanceTo(p0)) }.ifEmpty { good }
        return allowed.sortedByDescending { it.distanceTo(p0) }
    }

    private fun resetFits() {
        sx.fill(0.0)
        sy.fill(0.0)
        sz.fill(0.0)
        den.fill(0.0)
        saa.fill(0.0)
        summed = 1
    }

    /**
     * Adds the particles not in the sums yet and scores every step count from [N_MIN], lower is better: how far the
     * particles are from the curve and how far the step count is from what the curve's length gives.
     * @param p0 The first control point.
     * @param p1 The second control point.
     */
    private fun updateFits(p0: SboVec, p1: SboVec) {
        while (summed < particleLocations.size) {
            val i = summed++
            val q = particleLocations[i]
            for (k in 0 until N_COUNT) {
                val t = i / (N_MIN + k * N_STEP)
                val u = 1 - t
                val a = u * u * u
                val b = 3 * u * u * t
                val c3 = 3 * u * t * t
                val c = c3 + t * t * t
                val ax = q.x - a * p0.x - b * p1.x
                val ay = q.y - a * p0.y - b * p1.y - 3 * c3
                val az = q.z - a * p0.z - b * p1.z
                sx[k] += c * ax
                sy[k] += c * ay
                sz[k] += c * az
                den[k] += c * c
                saa[k] += ax * ax + ay * ay + az * az
            }
        }
        for (k in 0 until N_COUNT) {
            val d = den[k]
            val residual = saa[k] - (sx[k] * sx[k] + sy[k] * sy[k] + sz[k] * sz[k]) / d
            val dx = sx[k] / d - p0.x
            val dy = sy[k] / d - p0.y
            val dz = sz[k] / d - p0.z
            val off = (N_PER_BLOCK * sqrt(dx * dx + dy * dy + dz * dz) + N_BASE - (N_MIN + k * N_STEP)) / N_SPREAD
            scores[k] = residual / (POINT_ERROR * POINT_ERROR) + off * off
        }
    }

    /** The step count index with the lowest score. */
    private fun bestFit(): Int {
        var best = 0
        for (k in 1 until N_COUNT) if (scores[k] < scores[best]) best = k
        return best
    }

    /** The curve end for step count index [k], made only for the ones used. */
    private fun fitEnd(k: Int) = SboVec(sx[k] / den[k], sy[k] / den[k], sz[k] / den[k])

    /**
     * The curve end for every step count from [minN] to [maxN] with its score, lower is better. Used for the few step
     * counts of a short trail, [updateFits] scores the full range.
     * @param p0 The first control point.
     * @param p1 The second control point.
     */
    private fun curveFits(p0: SboVec, p1: SboVec, minN: Double, maxN: Double): List<Pair<Double, SboVec>> {
        val points = particleLocations
        val fits = ArrayList<Pair<Double, SboVec>>()
        var n = minN
        while (n <= maxN) {
            var nx = 0.0
            var ny = 0.0
            var nz = 0.0
            var den = 0.0
            for (i in 1 until points.size) {
                val t = i / n
                val u = 1 - t
                val c = 3 * u * t * t + t * t * t
                val a = u * u * u
                val b = 3 * u * u * t
                val q = points[i]
                nx += c * (q.x - a * p0.x - b * p1.x)
                ny += c * (q.y - a * p0.y - b * p1.y - 9 * u * t * t)
                nz += c * (q.z - a * p0.z - b * p1.z)
                den += c * c
            }
            val p3 = SboVec(nx / den, ny / den, nz / den)
            val off = (N_PER_BLOCK * p3.distanceTo(p0) + N_BASE - n) / N_SPREAD
            fits.add(residual(p0, p1, p3, n) / (POINT_ERROR * POINT_ERROR) + off * off to p3)
            n += 0.01
        }
        return fits
    }

    /** The first harp note is about 30.87 + 0.0214 * distance (in 63rds of pitch) rounded, checked with 20 blocks of slack. */
    private fun noteAllows(pitch: Float, distance: Double): Boolean {
        val q = (pitch * 63.0).roundToInt()
        val lo = (q - 0.5 - 30.87) / 0.0214 - 20
        val hi = (q + 0.5 - 30.87) / 0.0214 + 20
        return distance in lo..hi
    }

    /** The squared distance of the particles from the curve ending at [p3] with step count [n]. */
    private fun residual(p0: SboVec, p1: SboVec, p3: SboVec, n: Double): Double {
        // Plain doubles since this runs tens of thousands of times per particle
        var res = 0.0
        for (i in 1 until particleLocations.size) {
            val t = i / n
            val u = 1 - t
            val a = u * u * u
            val b = 3 * u * u * t
            val c = 3 * u * t * t
            val d = t * t * t
            val q = particleLocations[i]
            val x = a * p0.x + b * p1.x + (c + d) * p3.x - q.x
            val y = a * p0.y + b * p1.y + (c + d) * p3.y + 3 * c - q.y
            val z = a * p0.z + b * p1.z + (c + d) * p3.z - q.z
            res += x * x + y * y + z * z
        }
        return res
    }

    /** How badly the curve ending at [p3] fits the particles, with the step count free around what its distance gives. */
    private fun fitScore(p0: SboVec, p1: SboVec, p3: SboVec): Double {
        val n0 = N_PER_BLOCK * p3.distanceTo(p0) + N_BASE
        var best = Double.MAX_VALUE
        var n = n0 - 0.6
        while (n <= n0 + 0.6) {
            val off = (n0 - n) / N_SPREAD
            best = min(best, residual(p0, p1, p3, n) / (POINT_ERROR * POINT_ERROR) + off * off)
            n += 0.02
        }
        return best
    }

    /** Of the blocks near [end] a burrow can be on, the one whose curve fits the particles best. */
    private fun pickBlock(p0: SboVec, p1: SboVec, end: SboVec): SboVec? {
        val cx = floor(end.x)
        val cy = (end.y - 1).roundToInt().toDouble()
        val cz = floor(end.z)
        var best: SboVec? = null
        var bestScore = Double.MAX_VALUE
        for (dx in -PICK_SIDE..PICK_SIDE) for (dz in -PICK_SIDE..PICK_SIDE) for (dy in -PICK_UP..PICK_UP) {
            val block = SboVec(cx + dx, cy + dy, cz + dz)
            if (!canHold(block)) continue
            val score = fitScore(p0, p1, block.add(0.5, 1.0, 0.5))
            if (score < bestScore) {
                bestScore = score
                best = block
            }
        }
        return best
    }

    /** The guess for 10+ particles: the cubic fit's block, or the closest block next to it a burrow can be on. */
    private fun fullTrailGuess(): SboVec? {
        val end = guessBurrowLocation() ?: return null
        val block = end.down(0.5).roundLocationToBlock()
        if (canHold(block)) return block
        var best: SboVec? = null
        for (dx in -1..1) for (dz in -1..1) for (dy in -1..1) {
            val b = block.add(dx, dy, dz)
            if (!canHold(b)) continue
            if (best == null || b.add(0.5, 1.0, 0.5).distanceSq(end) < best.add(0.5, 1.0, 0.5).distanceSq(end)) best = b
        }
        return best ?: block
    }

    private fun guessBurrowLocation(): SboVec? {
        if (this.particleLocations.size < 4) return null
        val fitters = List(3) { PolynomialFitter(3) }

        this.particleLocations.forEachIndexed { index, location ->
            val x = index.toDouble()
            location.toDoubleArray().forEachIndexed { i, value ->
                fitters[i].addPoint(x, value)
            }
        }

        val coefficients = fitters.map { it.fit() }
        val startPointDerivative = SboVec.fromArray(
            coefficients.map { evaluateDerivative(it, 0.0) }
        )

        val pitch = this.getPitchFromDerivative(startPointDerivative)
        val controlPointDistance = sqrt(24 * sin(pitch - PI) + 25)
        val t = 3 * controlPointDistance / startPointDerivative.length()
        val result = coefficients.map { coeff ->
            evaluatePolynomial(coeff, t)
        }
        return SboVec.fromArray(result)
    }

    private fun getPitchFromDerivative(derivative: SboVec): Double {
        val xzLength = sqrt(derivative.x.pow(2) + derivative.z.pow(2))
        val pitchRadians = -atan2(derivative.y, xzLength)

        var guessPitch = pitchRadians
        var windowMin = -PI / 2
        var windowMax = PI / 2

        repeat(100) {
            val resultPitch = atan2(sin(guessPitch) - 0.75, cos(guessPitch))

            if (resultPitch == pitchRadians) {
                return@getPitchFromDerivative guessPitch
            }

            if (resultPitch < pitchRadians) {
                windowMin = guessPitch
            } else {
                windowMax = guessPitch
            }
            guessPitch = (windowMin + windowMax) / 2
        }
        return guessPitch
    }

    private fun evaluatePolynomial(coefficients: List<Double>, t: Double): Double {
        var result = 0.0
        for (coefficient in coefficients.asReversed()) {
            result = result * t + coefficient
        }
        return result
    }

    private fun evaluateDerivative(coefficients: List<Double>, t: Double): Double {
        var result = 0.0
        val derivativeCoefficients = coefficients
            .mapIndexedNotNull { index, coefficient ->
                if (index == 0) null else coefficient * index
            }

        for (coefficient in derivativeCoefficients.asReversed()) {
            result = result * t + coefficient
        }

        return result
    }
}
