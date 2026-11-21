package net.sbo.mod.diana.burrows

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.sbo.mod.SBOKotlin
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.collection.TimeLimitedSet
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.GameCloseEvent
import net.sbo.mod.utils.http.Http.getBoolean
import net.sbo.mod.utils.http.SboApi
import net.sbo.mod.utils.hubcache.HubIsland
import net.sbo.mod.utils.math.SboVec
import kotlin.math.floor
import kotlin.time.Duration.Companion.hours

/**
 * Sends the blocks this player dug a particle-confirmed burrow from to the SBO backend, which adds them to one count
 * per block (no player data) to map where burrows spawn. Only while the backend collects, it is switched on for one
 * Diana mayor term; nothing is kept on disk.
 */
object BurrowMap {
    private const val SEND_TICKS = 20 * 60
    /** BURROW_UPLOAD_MAX_DIGS of the backend. */
    private const val MAX_PER_UPLOAD = 30
    private const val MAX_QUEUED = 300
    /** A "not collecting" answer is asked again after this, so collection can start while the game runs. */
    private const val RECHECK_MS = 15 * 60 * 1000L
    /** A burrow lives far shorter; its block is released earlier when it is dug, see [forget]. */
    private val COUNTED_FOR = 1.hours
    private const val MAX_FAILURES = 3

    @Serializable
    private data class Dig(val x: Int, val y: Int, val z: Int)

    @Serializable
    private data class Upload(val digs: List<Dig>)

    private enum class State { UNKNOWN, ASKING, ON, OFF }

    private val json = Json
    private var state = State.UNKNOWN
    private var checkedAt = 0L
    private var sending = false
    private var failures = 0
    private val queue = ArrayDeque<Dig>()
    /** Blocks counted, so the same burrow seen again (lobby switch, walking away and back) isn't counted twice. */
    private val counted = TimeLimitedSet<Dig>(COUNTED_FOR)

    fun init() {
        Register.onTick(SEND_TICKS) { send() }
    }

    /** Queues a burrow its particles confirmed, or says why it isn't (for the debug message). */
    fun record(pos: SboVec): String? {
        if (!Debug.sendBurrowSpots) return "Send Burrow Spots is off"
        val dig = digOf(pos)
        if (dig.y !in 0..255 || !HubIsland.contains(dig.x shr 4, dig.z shr 4)) return "outside the Hub island"
        if (dig in counted) return "already counted"

        val now = System.currentTimeMillis()
        when (state) {
            State.UNKNOWN -> askCollecting()
            State.OFF -> if (now - checkedAt >= RECHECK_MS) askCollecting() else return "the server is not collecting"
            State.ASKING, State.ON -> {}
        }
        if (queue.size >= MAX_QUEUED) return "queue full"
        queue.addLast(dig)
        counted.add(dig)
        return null
    }

    /** The burrow on this block was dug or is gone, a burrow there later is a new one. */
    fun forget(pos: SboVec) {
        counted.remove(digOf(pos))
    }

    private fun digOf(pos: SboVec) = Dig(floor(pos.x).toInt(), floor(pos.y).toInt(), floor(pos.z).toInt())

    @SboEvent
    fun onGameClose(event: GameCloseEvent) {
        send()
    }

    private fun askCollecting() {
        state = State.ASKING
        SboApi.burrowsCollecting()
            .toJsonObject { body -> SBOKotlin.mc.execute { answered(body.getBoolean("collecting")) } }
            .error { SBOKotlin.mc.execute { answered(false) } }
    }

    private fun answered(collecting: Boolean) {
        state = if (collecting) State.ON else State.OFF
        checkedAt = System.currentTimeMillis()
        if (!collecting) queue.clear()
    }

    private fun send() {
        // Turned off with digs still queued: they stay on this client
        if (!Debug.sendBurrowSpots) return queue.clear()
        if (state != State.ON || sending || queue.isEmpty()) return
        val batch = List(minOf(MAX_PER_UPLOAD, queue.size)) { queue.removeFirst() }
        sending = true
        SboApi.burrowUpload(json.encodeToString(Upload(batch)))
            .toJsonObject { body ->
                SBOKotlin.mc.execute {
                    sending = false
                    failures = 0
                    // Collection ended, or the key was refused: stop until the next check
                    if (!body.getBoolean("Success") || !body.getBoolean("collecting")) answered(false)
                }
            }
            .error {
                SBOKotlin.mc.execute {
                    sending = false
                    // Backend down or refusing the batch: stop until the next check instead of sending it every minute
                    if (++failures >= MAX_FAILURES) {
                        failures = 0
                        answered(false)
                    } else if (state == State.ON) {
                        // Offline or the cooldown, tried again with the next send
                        batch.asReversed().forEach { if (queue.size < MAX_QUEUED) queue.addFirst(it) }
                    }
                }
            }
    }
}
