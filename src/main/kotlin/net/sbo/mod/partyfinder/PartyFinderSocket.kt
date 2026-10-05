package net.sbo.mod.partyfinder

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.MojangAuth
import net.sbo.mod.utils.SboKey
import net.sbo.mod.utils.events.Register
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.net.http.WebSocketHandshakeException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap

/**
 * Join requests over the SBO socket (`/pf/ws`) instead of `/msg`. Only connected while needed: while
 * the own party is listed, and while waiting for an answer plus two minutes. The backend checks the
 * joiner, so the leader gets requests that already passed.
 */
object PartyFinderSocket {
    private const val PING_MS = 30_000L
    private const val KEEP_OPEN_MS = 2 * 60_000L
    // Without "sent" by then the request goes out by /msg instead
    private const val SENT_WAIT_MS = 10_000L
    private const val CONNECT_WAIT_MS = 10_000L
    private const val FIRST_RETRY_MS = 2_000L
    private const val MAX_RETRY_MS = 60_000L
    // The backend closes with this when the same account connected somewhere else
    private const val REPLACED = 4000

    /** A join request of this player, [onResult] runs on the client thread with the status and the whole answer. */
    private class Pending(val onResult: (String, JsonObject) -> Unit, val sentAt: Long) {
        @Volatile var sent = false
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(CONNECT_WAIT_MS)).build()
    private val lock = Any()

    @Volatile private var socket: WebSocket? = null
    private var connecting = false
    private val waitingForOpen = mutableListOf<(WebSocket?) -> Unit>()
    @Volatile private var keepOpenUntil = 0L
    @Volatile private var nextAttempt = 0L
    private var retryDelay = FIRST_RETRY_MS
    private var lastPing = 0L
    private var sending: CompletableFuture<*> = CompletableFuture.completedFuture(null)
    private val pending = ConcurrentHashMap<String, Pending>()

    fun init() {
        Register.onTick(20) { tick() }
    }

    private fun needed(): Boolean = PartyFinderManager.inQueue || System.currentTimeMillis() < keepOpenUntil

    private fun tick() {
        val now = System.currentTimeMillis()
        // No "sent" in time: the request goes out by /msg
        pending.entries.removeIf { (_, request) ->
            val late = !request.sent && now - request.sentAt > SENT_WAIT_MS
            if (late) request.onResult("leader_offline", JsonObject(emptyMap()))
            late || now - request.sentAt > KEEP_OPEN_MS
        }
        val ws = socket
        if (!needed()) {
            if (ws != null) close()
            return
        }
        if (ws == null) {
            if (now >= nextAttempt) withSocket { }
            return
        }
        if (now - lastPing >= PING_MS) {
            lastPing = now
            send(buildJsonObject { put("type", "ping") })
        }
    }

    /** Asks the leader of [partyId] for an invite. Statuses: sent, invited, declined, failed, leader_offline. */
    fun requestJoin(partyId: String, role: String?, onResult: (String, JsonObject) -> Unit) {
        keepOpenUntil = System.currentTimeMillis() + KEEP_OPEN_MS
        withSocket { ws ->
            if (ws == null) return@withSocket SBOKotlin.mc.execute { onResult("leader_offline", JsonObject(emptyMap())) }
            val requestId = UUID.randomUUID().toString()
            pending[requestId] = Pending(onResult, System.currentTimeMillis())
            send(buildJsonObject {
                put("type", "join")
                put("requestId", requestId)
                put("partyId", partyId)
                role?.let { put("role", it) }
            })
        }
    }

    /** Tells the joiner behind [requestId] what the leader did. */
    fun answer(requestId: String, invited: Boolean) {
        send(buildJsonObject {
            put("type", "answer")
            put("requestId", requestId)
            put("status", if (invited) "invited" else "declined")
        })
    }

    /** Runs [then] with the open socket, connects first if needed; null when that failed. */
    private fun withSocket(then: (WebSocket?) -> Unit) {
        socket?.let { return then(it) }
        val start = synchronized(lock) {
            waitingForOpen += then
            (!connecting).also { connecting = true }
        }
        if (start) connect()
    }

    private fun opened(ws: WebSocket?) {
        val callbacks = synchronized(lock) {
            connecting = false
            if (ws != null) {
                socket = ws
                retryDelay = FIRST_RETRY_MS
                lastPing = System.currentTimeMillis()
            } else {
                nextAttempt = System.currentTimeMillis() + retryDelay
                retryDelay = minOf(retryDelay * 2, MAX_RETRY_MS)
            }
            waitingForOpen.toList().also { waitingForOpen.clear() }
        }
        callbacks.forEach { it(ws) }
    }

    private fun connect() {
        MojangAuth.withSession(onFail = { opened(null) }) { session ->
            val uri = URI.create(SBOKotlin.API_URL.replaceFirst("http", "ws") + "/pf/ws")
            client.newWebSocketBuilder()
                .connectTimeout(Duration.ofMillis(CONNECT_WAIT_MS))
                .header("x-sbo-key", SboKey.get())
                .header("x-sbo-session", session)
                .buildAsync(uri, Listener())
                .whenComplete { ws, error ->
                    if (error != null) {
                        // A refused login: the next try logs in again
                        if ((error.cause as? WebSocketHandshakeException)?.response?.statusCode() == 401) MojangAuth.forget()
                        SBOKotlin.logger.warn("[SBO] Party finder socket could not connect: ${error.cause?.message ?: error.message}")
                    }
                    opened(ws)
                }
        }
    }

    private fun close() {
        val ws = synchronized(lock) { socket.also { socket = null } } ?: return
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Not needed").exceptionally { null }
    }

    // One send at a time, the JDK socket refuses overlapping sends
    private fun send(message: JsonObject) {
        val ws = socket ?: return
        synchronized(lock) {
            sending = sending.handle { _, _ -> null }.thenCompose { ws.sendText(message.toString(), true) }
        }
    }

    private fun closed(ws: WebSocket, code: Int) {
        synchronized(lock) {
            if (socket !== ws) return
            socket = null
            // Another game with this account took over, don't fight over the connection
            nextAttempt = System.currentTimeMillis() + if (code == REPLACED) MAX_RETRY_MS else retryDelay
        }
    }

    private fun handle(text: String) {
        val message = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        val requestId = message["requestId"]?.jsonPrimitive?.contentOrNull ?: return
        when (message["type"]?.jsonPrimitive?.contentOrNull) {
            "join_request" -> {
                val name = message["name"]?.jsonPrimitive?.contentOrNull ?: return
                val uuid = message["uuid"]?.jsonPrimitive?.contentOrNull ?: return
                val role = message["role"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull
                SBOKotlin.mc.execute { PartyFinderManager.onSocketJoinRequest(requestId, uuid, name, role) }
            }
            "join_result" -> {
                val request = pending[requestId] ?: return
                val status = message["status"]?.jsonPrimitive?.contentOrNull ?: return
                if (status == "sent") request.sent = true else pending.remove(requestId)
                // An answer keeps the socket open a bit longer for the next one
                keepOpenUntil = maxOf(keepOpenUntil, System.currentTimeMillis() + KEEP_OPEN_MS)
                SBOKotlin.mc.execute { request.onResult(status, message) }
            }
        }
    }

    private class Listener : WebSocket.Listener {
        private val text = StringBuilder()

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            text.append(data)
            if (last) {
                val message = text.toString()
                text.setLength(0)
                handle(message)
            }
            webSocket.request(1)
            return null
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
            closed(webSocket, statusCode)
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            SBOKotlin.logger.warn("[SBO] Party finder socket error: ${error.message}")
            closed(webSocket, 0)
        }
    }
}
