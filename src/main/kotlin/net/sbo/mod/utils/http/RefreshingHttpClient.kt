// TODO: Remove RefreshingHttpClient and its call-sites once all supported Minecraft versions
// require Java 29+ (the next LTS after Java 25, which includes the HTTP/2 connection leak fix).

// Adapted from https://github.com/SkyblockerMod/Skyblocker/pull/2806

package net.sbo.mod.utils.http

import net.sbo.mod.utils.events.Register
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * An HTTP client that is periodically replaced to work around JDK-8326498.
 *
 * Refreshing is automatically disabled on Java 26 and newer, where the fix is
 * included. Old clients are shut down asynchronously after replacement,
 * allowing their in-flight requests to finish.
 *
 * @param refreshIntervalTicks How often to replace the client, or null to disable refreshing.
 * @param configure The configuration applied whenever a new client is created.
 */
class RefreshingHttpClient(
    refreshIntervalTicks: Int? = DEFAULT_REFRESH_INTERVAL_TICKS,
    private val configure: HttpClient.Builder.() -> Unit
) {
    private val lock = ReentrantReadWriteLock()

    private var current = createClient()

    init {
        refreshIntervalTicks?.let { interval ->
            require(interval > 0)

            if (Runtime.version().feature() < 26) {
                Register.onTick(interval) { refresh() }
            }
        }
    }

    /**
     * Runs [action] with the current client.
     *
     * The read lock prevents refresh from shutting down the client until
     * [action] has finished submitting its operation. Multiple callers can
     * use the client concurrently.
     */
    fun <T> useClient(action: (HttpClient) -> T): T =
        lock.read { action(current) }

    private fun createClient(): HttpClient =
        HttpClient.newBuilder()
            .apply(configure)
            .build()

    private fun refresh() {
        val old = lock.write {
            val replacement = createClient()
            current.also { current = replacement }
        }

        shutdownExecutor.execute {
            shutdown(old)
        }
    }

    private fun shutdown(client: HttpClient) {
        client.shutdown()

        var interrupted = false

        while (!client.isTerminated) {
            try {
                client.awaitTermination(Duration.ofMinutes(5))
            } catch (_: InterruptedException) {
                if (!interrupted) {
                    interrupted = true
                    client.shutdownNow()
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt()
        }
    }

    companion object {
        const val DEFAULT_REFRESH_INTERVAL_TICKS = 60 * 20 * 20

        private val shutdownExecutor = Executors.newVirtualThreadPerTaskExecutor()
    }
}

