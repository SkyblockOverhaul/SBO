package net.sbo.mod.diana.guesses

import net.minecraft.core.particles.ParticleTypes
import net.sbo.mod.SBOKotlin
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.settings.categories.Diana
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
 * The spade's lava trail is a cubic Bezier curve:
 *  - P0 = 1 block out from your eyes along your look (the first particle),
 *  - P1 = P0 + 4 * look + 3 up,
 *  - P2 = P3 + 3 up,
 *  - P3 = the burrow's top centre,
 * with particle i at t = i / n, where n = 0.52518 * |P3 - P0| + 0.179.
 *
 * So from the second particle on, the burrow is the only unknown. Each block near the solved end that a burrow can be on
 * is tried ([canHold]: grass with air above, and on [BurrowSpots] if a list is given), and the one whose curve fits the
 * particles best wins. Measured in game: exact block from 5 particles on,
 * exact or the block beside it from 2 (0.2 s after the click). With only 2 particles the curve can fit 2 or 3 places;
 * the first harp note (higher the farther the burrow) rules some out, then the farthest is taken.
 *
 * Full trails (10+ particles) keep the cubic-fit guess, which ends on the burrow exactly.
 */
object PreciseGuessBurrow {
    private var particleLocations = mutableListOf<SboVec>()
    private var lastLavaParticle: Long = 0
    private var lastPointTime: Long = 0

    private var lastGuessTime: Long = 0
    private var throwLook: SboVec? = null
    private var throwEye: SboVec? = null
    private var firstNote: Float? = null

    // This throw's waypoint: moved as the guess improves, not added again
    private var throwGuess: SboVec? = null

    private val SPADE_WINDOW = TimeUnit.MILLISECONDS.toNanos(3000L)
    // Particles come every 0.1 s: one much later belongs to another throw (a click the game didn't report)
    private val POINT_GAP = TimeUnit.MILLISECONDS.toNanos(300L)
    // A second click this soon, before the first's trail started: the server draws the first one
    private val DOUBLE_CLICK = TimeUnit.MILLISECONDS.toNanos(300L)

    private const val FULL_TRAIL = 10
    private const val N_PER_BLOCK = 0.52518
    private const val N_BASE = 0.179
    private const val N_SPREAD = 0.12
    private const val POINT_ERROR = 0.002
    private const val PICK_SIDE = 3
    private const val PICK_UP = 2

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (!Diana.spadeGuess) return
        this.particleLocations.clear()
        throwGuess = null
    }

    @SboEvent
    fun onReceiveParticle(event: ParticleReceiveEvent) {
        val packet = event.packet
        if (!Diana.spadeGuess || !World.isInHub()) return
        if (packet.particle.type != ParticleTypes.DRIPPING_LAVA || packet.count != 2 || packet.maxSpeed != -0.5f) return
        val currLoc = SboVec(packet.x, packet.y, packet.z)
        val now = System.nanoTime()
        this.lastLavaParticle = now
        if (now - lastGuessTime > SPADE_WINDOW) return

        if (this.particleLocations.isNotEmpty()) {
            val distToLast = this.particleLocations.last().distanceTo(currLoc)
            if (distToLast > 3 || distToLast == 0.0) return
            if (now - lastPointTime > POINT_GAP) return
        }
        this.particleLocations.add(currLoc)
        lastPointTime = now
        if (this.particleLocations.size < 2) return

        val guess = (if (particleLocations.size >= FULL_TRAIL) fullTrailGuess() else earlyGuess()) ?: return
        moveGuess(guess)
    }

    @SboEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (!Diana.spadeGuess || !World.isInHub() || firstNote != null) return
        if (System.nanoTime() - lastGuessTime > SPADE_WINDOW) return
        val sound = event.sound
        if (sound.identifier.path != "block.note_block.harp") return
        val eye = SBOKotlin.mc.player?.eyePosition?.toSboVec() ?: return
        if (eye.distanceTo(sound.x, sound.y, sound.z) > 48) return
        firstNote = sound.pitch
    }

    @SboEvent
    fun onUseSpade(event: PlayerInteractEvent) {
        if (!Diana.spadeGuess || !World.isInHub()) return
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
        if (this.particleLocations.isEmpty() && now - lastGuessTime < DOUBLE_CLICK) return
        BurrowDetector.pendingUseSpadeTitle = null
        this.particleLocations.clear()
        lastGuessTime = now
        throwLook = player.getViewVector(1f).toSboVec().normalize()
        throwEye = player.eyePosition.toSboVec()
        firstNote = null
        throwGuess = null
    }

    // One waypoint per throw: moved when a later particle changes the block
    private fun moveGuess(pos: SboVec) {
        if (pos == throwGuess) return
        throwGuess?.let { WaypointManager.removeWaypointAt(it, "guess") }
        throwGuess = pos
        WaypointManager.addSpadeGuess(pos)
    }

    // A block a burrow can be on: grass with air above (or not loaded yet), and on the burrow list if there is one
    private fun canHold(block: SboVec) = BurrowSpots.isPossible(block) && ArrowGuessBurrow.isBlockValidNoCache(block)

    // ---- 2 to 9 particles: the curve ----

    private fun earlyGuess(): SboVec? {
        val p0 = particleLocations[0]
        // The server's look, from the first particle (1 block out from your eyes): aimed at a block, it uses the
        // previous tick's turn, which a quick flick after a warp makes far off from the client's
        val eye = throwEye
        val look = if (eye != null && kotlin.math.abs(p0.distanceTo(eye) - 1.0) < 0.05) (p0 - eye).normalize()
            else throwLook ?: return null
        val p1 = p0 + look * 4.0 + SboVec(0.0, 3.0, 0.0)
        // Each place the curve can end, best first; the first with a block a burrow can be on near it
        for (end in curveEnds(p0, p1)) {
            pickBlock(p0, p1, end)?.let { return it }
        }
        return null
    }

    // For each step count n the end is linear in the points: solve it, and score how well the points and n agree
    private fun curveEnds(p0: SboVec, p1: SboVec): List<SboVec> {
        val points = particleLocations
        val fits = ArrayList<Pair<Double, SboVec>>()
        var n = 5.0
        while (n < 200) {
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
        val best = listOfNotNull(fits.minByOrNull { it.first }?.second)
        if (points.size >= 3) return best

        // 2 particles: every good fit (a local best) the first note allows, farthest first
        val good = (1 until fits.size - 1).filter {
            fits[it].first <= fits[it - 1].first && fits[it].first <= fits[it + 1].first && fits[it].first < 25
        }.map { fits[it].second }
        if (good.isEmpty()) return best
        val note = firstNote
        val allowed = if (note == null) good else good.filter { noteAllows(note, it.distanceTo(p0)) }.ifEmpty { good }
        return allowed.sortedByDescending { it.distanceTo(p0) }
    }

    // The first harp note is about 30.87 + 0.0214 * distance (in 63rds of pitch), rounded: 20 blocks' slack each side
    private fun noteAllows(pitch: Float, distance: Double): Boolean {
        val q = (pitch * 63.0).roundToInt()
        val lo = (q - 0.5 - 30.87) / 0.0214 - 20
        val hi = (q + 0.5 - 30.87) / 0.0214 + 20
        return distance in lo..hi
    }

    private fun residual(p0: SboVec, p1: SboVec, p3: SboVec, n: Double): Double {
        // Plain doubles: this runs tens of thousands of times per particle
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

    // How badly a curve ending at [p3] fits the particles, the step count free near what its distance says
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

    // Of the blocks near [end] a burrow can be on, the one whose curve fits best (its block position), or null
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

    // ---- 10+ particles: the cubic fit, onto its block (or the nearest one beside it a burrow can be on) ----

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
