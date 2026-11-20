package net.sbo.mod.utils.game

import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.packets.LoginReceiveEvent
import net.sbo.mod.utils.events.impl.packets.SetTimeReceiveEvent
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

object ServerStats {
    private var prevTime = 0L
    private var averageTps = 20f
    private const val TPS_HISTORY = 20
    private val tpsLog = FloatArray(TPS_HISTORY)
    private var tpsIndex = 0
    private var tpsCount = 0

    var lastPacket = 0L

    /** A time packet this much off the server tick from the ones before is a jump, the tick is set to it. */
    private const val TICK_JUMP = 5.0
    /** Without a time packet for this long the server tick isn't known (they come every 20 ticks). */
    private val TICK_STALE_NANOS = TimeUnit.SECONDS.toNanos(3L)
    /** The server tick at [tickBaseTime], NaN before a time packet came. */
    private var tickBase = Double.NaN
    /** When the last time packet came. */
    private var tickBaseTime = 0L
    /** The game time of the last time packet, -1 before one came. */
    private var lastGameTime = -1L
    /** How long a server tick takes, from the time packets. */
    var nanosPerTick = TimeUnit.MILLISECONDS.toNanos(50L).toDouble()
        private set

    private fun addTpsSample(tps: Float) {
        tpsLog[tpsIndex] = tps
        tpsIndex = (tpsIndex + 1) % TPS_HISTORY
        if (tpsCount < TPS_HISTORY) tpsCount++
    }

    /**
     * Output format and sample size matches Odin's. https://github.com/odtheking/Odin/pull/124
     */
    fun getTpsString(): String {
        if (tpsCount == 0) return "Current: ${"%.1f".format(Locale.ROOT, averageTps)}"

        var max = tpsLog[0]
        var min = tpsLog[0]
        var total = 0f
        for (i in 0..<tpsCount) {
            val tps = tpsLog[i]
            max = maxOf(max, tps)
            min = minOf(min, tps)
            total += tps
        }

        return "Current: ${"%.1f".format(Locale.ROOT, averageTps)} (max/min/avg) ${"%.1f".format(Locale.ROOT, max)}/${"%.1f".format(Locale.ROOT, min)}/${"%.1f".format(Locale.ROOT, total / tpsCount)}"
    }

    /**
     * The server's game tick at [time], from the time packets: the game tick they bring and the ticks since at their pace.
     * @param time A System.nanoTime().
     * @return The tick with its fraction, null without a recent time packet.
     */
    fun serverTickAt(time: Long): Double? {
        if (tickBase.isNaN() || time - lastPacket > TICK_STALE_NANOS) return null
        return tickBase + (time - tickBaseTime) / nanosPerTick
    }

    /** The System.nanoTime() of a server [tick], see [serverTickAt]. */
    fun timeAtTick(tick: Double): Long = tickBaseTime + ((tick - tickBase) * nanosPerTick).toLong()

    /**
     * Time packets come a bit early or late, the server tick only follows them a quarter of the way, and the pace of a tick
     * an eighth. A repeated game time is skipped, a jump (new server, lag) sets the tick to it.
     */
    private fun followServerTick(gameTime: Long, currentTime: Long) {
        if (gameTime == lastGameTime) return
        val predicted = serverTickAt(currentTime)
        if (predicted == null || abs(gameTime - predicted) > TICK_JUMP) {
            tickBase = gameTime.toDouble()
        } else {
            val ticks = gameTime - lastGameTime
            if (ticks in 10..40) nanosPerTick += ((currentTime - tickBaseTime).toDouble() / ticks - nanosPerTick) / 8
            tickBase = predicted + (gameTime - predicted) / 4
        }
        tickBaseTime = currentTime
        lastGameTime = gameTime
    }

    @SboEvent
    fun onSetTimeReceive(event: SetTimeReceiveEvent) {
        val currentTime = System.nanoTime()
        followServerTick(event.packet.gameTime, currentTime)
        lastPacket = currentTime
        if (prevTime != 0L) {
            val deltaTime = currentTime - prevTime
            averageTps = (TimeUnit.MILLISECONDS.toNanos(20000L).toFloat() / max(1, deltaTime)).coerceIn(0f, 20f)
            addTpsSample(averageTps)
        }
        prevTime = currentTime
    }

    @SboEvent
    fun onLoginReceive(event: LoginReceiveEvent) {
        averageTps = 20f
        prevTime = 0L
        tickBase = Double.NaN
        tickBaseTime = 0L
        lastGameTime = -1L
        tpsIndex = 0
        tpsCount = 0
    }
}
