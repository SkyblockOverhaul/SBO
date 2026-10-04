package net.sbo.mod.partyfinder.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.http.HttpRequestHandle
import net.sbo.mod.utils.http.SboApi

/**
 * Client for the `/pf` party finder endpoints.
 *
 * Every answer is `{ success, data }` or `{ success: false, error }`, also on HTTP 4xx/5xx,
 * so the error code survives. Callbacks run on the HTTP thread.
 */
/** Tells a banned player once per game session why their key is banned. */
object BanNotice {
    @Volatile
    private var shown = false

    fun check(error: PfError) {
        if (shown || error.code != PfError.INVALID_KEY || !error.message.startsWith("Key is banned")) return
        shown = true
        Chat.chat("§6[SBO] §c${ProblemText.error(error)}")
    }
}

object PartyFinderApi {
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    fun categories(onError: (PfError) -> Unit, onSuccess: (CategoriesData) -> Unit) =
        SboApi.get("/pf/categories").handle(onError, onSuccess)

    fun parties(partyType: String, subType: String = "", onError: (PfError) -> Unit, onSuccess: (List<PartyView>) -> Unit) {
        val sub = if (subType.isEmpty()) "" else "&subType=${SboApi.encode(subType)}"
        SboApi.get("/pf/parties?partyType=${SboApi.encode(partyType)}$sub")
            .handle<PartiesData>(onError) { onSuccess(it.parties) }
    }

    fun counts(onError: (PfError) -> Unit, onSuccess: (Map<String, Int>) -> Unit) {
        SboApi.get("/pf/parties/counts").handle<PartyCounts>(onError) { onSuccess(it.counts) }
    }

    fun createParty(body: PartyBody, onError: (PfError) -> Unit, onSuccess: (PartyView) -> Unit) =
        SboApi.post("/pf/parties", json.encodeToString(body)).handle(onError, onSuccess)

    fun updateParty(body: PartyBody, onError: (PfError) -> Unit, onSuccess: (PartyView) -> Unit) =
        SboApi.post("/pf/parties/update", json.encodeToString(body)).handle(onError, onSuccess)

    fun removeParty(onError: (PfError) -> Unit, onSuccess: () -> Unit) =
        SboApi.post("/pf/parties/remove").handle<JsonElement>(onError) { onSuccess() }

    fun refreshParty(onError: (PfError) -> Unit, onSuccess: () -> Unit) =
        SboApi.post("/pf/parties/refresh").handle<JsonElement>(onError) { onSuccess() }

    fun checkMembers(body: CheckBody, onError: (PfError) -> Unit, onSuccess: (CheckData) -> Unit) =
        SboApi.post("/pf/members/check", json.encodeToString(body)).handle(onError, onSuccess)

    fun rules(onError: (PfError) -> Unit, onSuccess: (RulesData) -> Unit) =
        SboApi.get("/pf/rules").handle(onError, onSuccess)

    fun reportParty(body: PartyReportBody, onError: (PfError) -> Unit, onSuccess: () -> Unit) =
        SboApi.post("/pf/parties/report", json.encodeToString(body)).handle<JsonElement>(onError) { onSuccess() }

    fun reportStats(body: StatsReportBody, onError: (PfError) -> Unit, onSuccess: () -> Unit) =
        SboApi.post("/pf/stats/report", json.encodeToString(body)).handle<JsonElement>(onError) { onSuccess() }

    /** Reads a `/pf` answer; [onSuccess] gets `data` (JsonNull for calls without data). */
    @PublishedApi
    internal inline fun <reified T> HttpRequestHandle.handle(
        crossinline onError: (PfError) -> Unit,
        crossinline onSuccess: (T) -> Unit
    ) {
        result { response ->
            val result = try {
                parse<T>(response.body?.string().orEmpty(), response.code)
            } catch (e: Exception) {
                Result.failure<T>(PfException(PfError(PfError.BAD_RESPONSE, e.message ?: "Unreadable answer")))
            }
            result.fold(
                onSuccess = { onSuccess(it) },
                onFailure = {
                    val error = (it as? PfException)?.error ?: PfError(PfError.BAD_RESPONSE, it.message ?: "")
                    BanNotice.check(error)
                    onError(error)
                }
            )
        }
        error { e -> onError(PfError(PfError.NETWORK, e.message ?: e.javaClass.simpleName)) }
    }

    @PublishedApi
    internal inline fun <reified T> parse(body: String, httpCode: Int): Result<T> {
        if (body.isBlank()) return Result.failure(PfException(PfError(PfError.BAD_RESPONSE, "Empty answer (HTTP $httpCode)")))
        val root = json.parseToJsonElement(body) as? JsonObject
            ?: return Result.failure(PfException(PfError(PfError.BAD_RESPONSE, "Unexpected answer (HTTP $httpCode)")))
        if (root["success"]?.jsonPrimitive?.booleanOrNull != true) {
            val error = root["error"]?.let { json.decodeFromJsonElement<PfError>(it) }
                ?: PfError(PfError.BAD_RESPONSE, "HTTP $httpCode")
            return Result.failure(PfException(error))
        }
        return Result.success(json.decodeFromJsonElement<T>(root["data"] ?: JsonNull))
    }

    class PfException(val error: PfError) : Exception("${error.code}: ${error.message}")
}
