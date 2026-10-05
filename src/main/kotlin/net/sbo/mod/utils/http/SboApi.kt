package net.sbo.mod.utils.http

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.API_URL
import net.sbo.mod.utils.data.CloudUploadRequest
import net.sbo.mod.utils.data.MembersRequest
import net.sbo.mod.utils.MojangAuth
import net.sbo.mod.utils.SboKey
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Client for the SBO backend.
 *
 * the SBO key travels in the `x-sbo-key` header, the mod version in the `X-SBO-Version` header,
 * the Mojang login session in the `x-sbo-session` header (only for calls that act on the player's data)
 */
object SboApi {
    private val json = Json { encodeDefaults = false }
    
    private val VERSION_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9.+_-]{0,31}")

    private var cachedVersion: String? = null

    private fun modVersion(): String? {
        cachedVersion?.let { return it }
        val version = runCatching { SBOKotlin.version }.getOrNull()
            ?.takeIf { VERSION_PATTERN.matches(it) } ?: return null
        cachedVersion = version
        return version
    }

    private fun headers(): Map<String, String> = buildMap {
        SboKey.get().takeIf { it.isNotBlank() }?.let { put("x-sbo-key", it) }
        modVersion()?.let { put("X-SBO-Version", it) }
    }

    internal fun post(path: String, body: String = "{}"): HttpRequestHandle =
        Http.sendPostRequest("$API_URL$path", body, headers())

    internal fun get(path: String): HttpRequestHandle =
        Http.sendGetRequest("$API_URL$path", headers())

    internal fun authedPost(path: String, body: String = "{}"): HttpRequestHandle =
        authed { headers -> Http.sendPostRequest("$API_URL$path", body, headers) }

    internal fun authedGet(path: String): HttpRequestHandle =
        authed { headers -> Http.sendGetRequest("$API_URL$path", headers) }

    /** One new login and retry on a missing session or wrong key; failures come back as a normal answer in both formats. */
    private fun authed(send: (Map<String, String>) -> HttpRequestHandle): HttpRequestHandle {
        val outer = HttpRequestHandle()
        fun attempt(retried: Boolean) {
            if (SboKey.get().isBlank()) MojangAuth.forget()
            MojangAuth.withSession(onFail = { outer.complete(failure(it.code, it.message)) }) { session ->
                send(headers() + ("x-sbo-session" to session))
                    .result { response ->
                        val text = response.body?.string().orEmpty()
                        val (code, message) = errorOf(text)
                        val wrongKey = code == MojangAuth.KEY_NOT_YOURS || (code == "INVALID_KEY" && message?.startsWith("Key is banned") != true)
                        when {
                            !retried && (code == MojangAuth.SESSION_REQUIRED || wrongKey) -> {
                                MojangAuth.forget()
                                if (code == MojangAuth.KEY_NOT_YOURS) SboKey.clear()
                                attempt(true)
                            }
                            code == MojangAuth.SESSION_REQUIRED -> outer.complete(failure(code, MojangAuth.CHECK_FAILED_TEXT))
                            wrongKey -> outer.complete(failure(code!!, MojangAuth.KEY_FAILED_TEXT))
                            else -> outer.complete(response.copy(body = ResponseBody(text.byteInputStream())))
                        }
                    }
                    .error { outer.fail(it) }
            }
        }
        attempt(false)
        return outer
    }

    private fun errorOf(text: String): Pair<String?, String?> {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null to null
        val error = root["error"] as? JsonObject
        val code = root["Code"]?.jsonPrimitive?.contentOrNull ?: error?.get("code")?.jsonPrimitive?.contentOrNull
        val message = root["Error"]?.jsonPrimitive?.contentOrNull ?: error?.get("message")?.jsonPrimitive?.contentOrNull
        return code to message
    }

    private fun failure(code: String, message: String): HttpResponse {
        val body = buildJsonObject {
            put("success", false)
            putJsonObject("error") {
                put("code", code)
                put("message", message)
            }
            put("Success", false)
            put("Error", message)
            put("Code", code)
        }
        return HttpResponse(200, "OK", ResponseBody(body.toString().byteInputStream()))
    }

    internal fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    fun partyInfo(members: List<String>, readCache: Boolean = true): HttpRequestHandle =
        post("/partyInfo", json.encodeToString(MembersRequest(members, readCache)))

    fun partyInfoByUuids(members: List<String>, readCache: Boolean = true): HttpRequestHandle =
        post("/partyInfoByUuids", json.encodeToString(MembersRequest(members, readCache)))

    fun countActiveUsers(): HttpRequestHandle = post("/countActiveUsers")

    fun playerInfo(player: String, readCache: Boolean = true): HttpRequestHandle =
        get("/playerInfo?player=${encode(player)}" + if (readCache) "" else "&readcache=false")

    fun playerInfoByUuid(uuid: String): HttpRequestHandle =
        get("/playerInfoByUuid?uuid=${encode(uuid)}")

    fun activeUsers(): HttpRequestHandle = get("/activeUsers")

    fun ahItems(): HttpRequestHandle = get("/ahItems")

    fun cloudStatus(): HttpRequestHandle = authedGet("/cloudSync")

    fun cloudDownload(slot: String): HttpRequestHandle = authedGet("/cloudSync/${encode(slot)}")

    fun cloudUpload(slot: String, request: CloudUploadRequest): HttpRequestHandle =
        authedPost("/cloudSync/${encode(slot)}", json.encodeToString(request))

    fun cloudDelete(slot: String): HttpRequestHandle = authedPost("/cloudSync/${encode(slot)}/delete")
}
