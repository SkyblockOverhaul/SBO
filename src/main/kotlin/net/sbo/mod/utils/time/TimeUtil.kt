package net.sbo.mod.utils.time

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Provides a wall-clock timestamp driven by a monotonic clock and synchronized
 * against an NTP server.
 *
 * The system clock is used only as an initial reference until NTP synchronization
 * completes. After synchronization, System.nanoTime() is used to advance the
 * corrected NTP time, so subsequent system clock changes cannot affect the result.
 */
object TimeUtil {

    private const val NTP_SERVER = "time.cloudflare.com"
    private const val NTP_PORT = 123

    private const val NTP_PACKET_SIZE = 48
    private const val NTP_TIMEOUT_MILLIS = 5_000

    private const val NTP_EPOCH_OFFSET_SECONDS = 2_208_988_800L

    private val EXECUTOR = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SBO Time Synchronization").apply {
            isDaemon = true
        }
    }

    private val started = AtomicBoolean(false)

    @Volatile
    private var ntpSynchronized = false

    @Volatile
    private var ntpSynchronizationFailed = false

    @Volatile
    private var baseNanoTime = System.nanoTime()

    @Volatile
    private var baseTimeMillis = System.currentTimeMillis()

    /**
     * Estimated network uncertainty of the successful NTP synchronization
     * in milliseconds.
     *
     * This is approximately half the measured round-trip time and is only
     * an estimate, not a guaranteed error bound.
     */
    @Volatile
    var synchronizationUncertaintyMillis: Long = Long.MAX_VALUE
        private set

    /**
     * Whether NTP synchronization has successfully completed.
     */
    val isSynchronized: Boolean
        get() = ntpSynchronized

    /**
     * Whether the NTP synchronization attempt has completed unsuccessfully.
     */
    val synchronizationFailed: Boolean
        get() = ntpSynchronizationFailed

    /**
     * Starts the one-time NTP synchronization.
     *
     * This method returns immediately. The network request runs asynchronously.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }

        EXECUTOR.execute {
            synchronize()
        }
    }

    /**
     * Returns the current Unix time in milliseconds.
     *
     * Before NTP synchronization completes, this uses the local system clock
     * only as its initial reference.
     *
     * Once NTP synchronization succeeds, only System.nanoTime() is used to
     * advance the corrected time, so subsequent system clock changes cannot
     * affect the result.
     */
    fun currentTimeMillis(): Long {
        return baseTimeMillis +
            (System.nanoTime() - baseNanoTime) / 1_000_000L
    }

    private fun synchronize() {
        try {
            val result = queryNtpServer()

            val localMidpointNanos =
                result.localSendNanoTime +
                    (result.localReceiveNanoTime - result.localSendNanoTime) / 2L

            val localMidpointMillis =
                localTimeAtNano(localMidpointNanos)

            val correctedMillis =
                localMidpointMillis + result.offsetMillis

            /*
             * Establish the NTP-corrected wall-clock reference at the
             * corresponding monotonic-clock point.
             */
            baseNanoTime = localMidpointNanos
            baseTimeMillis = correctedMillis

            synchronizationUncertaintyMillis =
                result.roundTripMillis / 2L

            ntpSynchronized = true
        } catch (_: Exception) {
            ntpSynchronizationFailed = true
        }
    }

    private fun queryNtpServer(): NtpResult {
        val address = InetAddress.getByName(NTP_SERVER)

        DatagramSocket().use { socket ->
            socket.soTimeout = NTP_TIMEOUT_MILLIS

            val packet = ByteArray(NTP_PACKET_SIZE)

            // NTP version 4, client mode.
            packet[0] = 0x23

            val request = DatagramPacket(
                packet,
                packet.size,
                address,
                NTP_PORT
            )

            // T1: client request transmit time.
            val localSendNanoTime = System.nanoTime()

            socket.send(request)

            val responsePacket = DatagramPacket(
                packet,
                packet.size
            )

            socket.receive(responsePacket)

            // T4: client response receive time.
            val localReceiveNanoTime = System.nanoTime()

            if (responsePacket.length < NTP_PACKET_SIZE) {
                throw IllegalStateException("Invalid NTP response")
            }

            // T2: server request receive time.
            val serverReceiveMillis =
                readNtpTimestamp(packet, 32)

            // T3: server response transmit time.
            val serverTransmitMillis =
                readNtpTimestamp(packet, 40)

            val localSendMillis =
                localTimeAtNano(localSendNanoTime)

            val localReceiveMillis =
                localTimeAtNano(localReceiveNanoTime)

            /*
             * Standard NTP clock-offset calculation:
             *
             * offset = ((T2 - T1) + (T3 - T4)) / 2
             */
            val offsetMillis = (
                (serverReceiveMillis - localSendMillis) +
                    (serverTransmitMillis - localReceiveMillis)
                ) / 2L

            val roundTripMillis =
                (localReceiveNanoTime - localSendNanoTime) / 1_000_000L

            return NtpResult(
                offsetMillis = offsetMillis,
                roundTripMillis = roundTripMillis,
                localSendNanoTime = localSendNanoTime,
                localReceiveNanoTime = localReceiveNanoTime
            )
        }
    }

    private fun readNtpTimestamp(
        packet: ByteArray,
        offset: Int
    ): Long {
        val seconds = readUnsignedInt(packet, offset)
        val fraction = readUnsignedInt(packet, offset + 4)

        val unixSeconds =
            seconds - NTP_EPOCH_OFFSET_SECONDS

        val millis =
            (fraction * 1_000L) / 0x1_0000_0000L

        return unixSeconds * 1_000L + millis
    }

    private fun readUnsignedInt(
        packet: ByteArray,
        offset: Int
    ): Long {
        return (
            (packet[offset].toLong() and 0xFF) shl 24 or
                ((packet[offset + 1].toLong() and 0xFF) shl 16) or
                ((packet[offset + 2].toLong() and 0xFF) shl 8) or
                (packet[offset + 3].toLong() and 0xFF)
            )
    }

    private fun localTimeAtNano(nanoTime: Long): Long {
        return baseTimeMillis +
            (nanoTime - baseNanoTime) / 1_000_000L
    }

    private data class NtpResult(
        val offsetMillis: Long,
        val roundTripMillis: Long,
        val localSendNanoTime: Long,
        val localReceiveNanoTime: Long
    )
}
