package net.sbo.mod.utils

/**
 * Sends a request at most once per spacingMs, for a server that refuses requests which follow too fast.
 * Callers that ask while a request waits get the same answer, so this only fits requests whose answer is the same for everyone.
 * A refused request is sent once more, a second refusal is left to the caller.
 */
internal class SpacedRequests(
    private val spacingMs: Long,
    private val now: () -> Long,
    private val later: (delayMs: Long, action: () -> Unit) -> Unit,
    private val send: () -> Unit
) {
    private var lastSent: Long? = null
    private var waiting = false
    private var retried = false

    fun request() {
        if (waiting) return
        val wait = lastSent?.let { it + spacingMs - now() } ?: 0L
        if (wait <= 0L) return sendNow()
        waiting = true
        later(wait) {
            waiting = false
            sendNow()
        }
    }

    private fun sendNow() {
        lastSent = now()
        send()
    }

    /** Returns false when the retry was refused too. */
    fun refused(): Boolean {
        if (retried) {
            retried = false
            return false
        }
        retried = true
        lastSent = now()
        request()
        return true
    }

    fun answered() {
        retried = false
    }
}
