package net.sbo.mod.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.sbo.mod.SBOKotlin
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.http.SboApi

/** Mojang login like a server join, proves the SBO key belongs to the playing account. The token only goes to Mojang. */
object MojangAuth {
    const val SESSION_REQUIRED = "SESSION_REQUIRED"
    const val KEY_NOT_YOURS = "KEY_NOT_YOURS"
    const val MOJANG_BUSY = "MOJANG_BUSY"
    const val CHECK_FAILED_TEXT = "SBO could not check your Minecraft account with Mojang. Please try again in a minute."
    const val KEY_FAILED_TEXT = "SBO could not set up your key. Please try again in a minute."
    private const val SET_UP_TEXT = "SBO checked with Mojang that this is your Minecraft account. Your SBO key was set up automatically."

    // Mojang allows 6 joins per 30 s and the player needs them for servers, so no retry loop
    private const val RETRY_AFTER_MS = 60_000L

    class Failure(val code: String, val message: String)

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    // Only in memory, never written to a config or file
    private var session: String? = null
    private var sessionAccount: String? = null
    private var running = false
    private val waiting = mutableListOf<(String?, Failure?) -> Unit>()
    private var lastFailure: Failure? = null
    private var failedAt = 0L

    fun current(): String? = synchronized(lock) { session?.takeIf { sessionAccount == Player.accountUuid() } }

    fun hasSession(): Boolean = current() != null

    fun forget() = synchronized(lock) {
        session = null
        sessionAccount = null
    }

    /** Logs in first when needed. Callbacks run on any thread. */
    fun withSession(onFail: (Failure) -> Unit, block: (String) -> Unit) {
        current()?.let { return block(it) }
        var recent: Failure? = null
        var start = false
        synchronized(lock) {
            recent = lastFailure?.takeIf { System.currentTimeMillis() - failedAt < RETRY_AFTER_MS }
            if (recent == null) {
                waiting += { s, failure -> if (s != null) block(s) else onFail(failure!!) }
                start = !running
                running = true
            }
        }
        recent?.let { return onFail(it) }
        if (start) login()
    }

    private fun done(newSession: String?, failure: Failure?) {
        val callbacks = synchronized(lock) {
            running = false
            lastFailure = failure
            if (failure != null) failedAt = System.currentTimeMillis()
            waiting.toList().also { waiting.clear() }
        }
        callbacks.forEach { it(newSession, failure) }
    }

    private fun fail(failure: Failure) {
        SBOKotlin.logger.warn("[SBO] Mojang login failed: ${failure.code} ${failure.message}")
        done(null, failure)
    }

    private fun login() {
        val account = Player.accountUuid()
        SboApi.post("/auth/mojang/start")
            .result { response ->
                val data = dataOf(response.body?.string().orEmpty()) ?: return@result
                val serverId = data["serverId"]?.jsonPrimitive?.contentOrNull ?: return@result fail(Failure(MOJANG_BUSY, CHECK_FAILED_TEXT))
                if (!joinMojang(serverId)) return@result fail(Failure(MOJANG_BUSY, CHECK_FAILED_TEXT))
                finish(account, serverId)
            }
            .error { fail(Failure("NETWORK", it.message ?: "server not reachable")) }
    }

    private fun finish(account: String, serverId: String) {
        val body = buildJsonObject {
            put("name", SBOKotlin.mc.user.name)
            put("serverId", serverId)
        }
        SboApi.post("/auth/mojang/finish", body.toString())
            .result { response ->
                val data = dataOf(response.body?.string().orEmpty()) ?: return@result
                val key = data["key"]?.jsonPrimitive?.contentOrNull
                val newSession = data["session"]?.jsonPrimitive?.contentOrNull
                val uuid = data["uuid"]?.jsonPrimitive?.contentOrNull
                // Never take a key for another account, nor keep one after switching accounts
                if (key == null || newSession == null || uuid != account || Player.accountUuid() != account) {
                    return@result fail(Failure(MOJANG_BUSY, CHECK_FAILED_TEXT))
                }
                synchronized(lock) {
                    session = newSession
                    sessionAccount = account
                }
                if (SboKey.get() == key) return@result done(newSession, null)
                // The key is written before anyone waiting sends a request with it
                SBOKotlin.mc.execute {
                    SboKey.set(key)
                    tell(SET_UP_TEXT)
                    done(newSession, null)
                }
            }
            .error { fail(Failure("NETWORK", it.message ?: "server not reachable")) }
    }

    /** Null after reporting the failure. */
    private fun dataOf(text: String): JsonObject? {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        if (root?.get("success")?.jsonPrimitive?.contentOrNull == "true") {
            return root["data"] as? JsonObject ?: JsonObject(emptyMap())
        }
        val error = root?.get("error") as? JsonObject
        val code = error?.get("code")?.jsonPrimitive?.contentOrNull
        val message = error?.get("message")?.jsonPrimitive?.contentOrNull.orEmpty()
        // A ban keeps its own text, everything else is "try again later"
        fail(if (code == "INVALID_KEY") Failure(code, message) else Failure(MOJANG_BUSY, CHECK_FAILED_TEXT))
        return null
    }

    private fun joinMojang(serverId: String): Boolean = try {
        val user = SBOKotlin.mc.user
        SBOKotlin.mc.services().sessionService().joinServer(user.profileId, user.accessToken, serverId)
        true
    } catch (e: Exception) {
        SBOKotlin.logger.warn("[SBO] Mojang join failed: ${e.message}")
        false
    }

    private fun tell(text: String) {
        val gui = PartyFinderManager.listener
        if (gui != null) gui(true, text) else Chat.chat("§6[SBO] §a$text")
    }
}
