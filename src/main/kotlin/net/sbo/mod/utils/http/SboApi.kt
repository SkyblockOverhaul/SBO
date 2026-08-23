package net.sbo.mod.utils.http

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sbo.mod.SBOKotlin.API_URL
import net.sbo.mod.utils.data.MembersRequest
import net.sbo.mod.utils.data.PartyRequest
import net.sbo.mod.utils.data.SboDataObject.sboData
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Thin client for the SBO backend.
 *
 * the SBO key travels in the `x-sbo-key` header
 */
object SboApi {
    private val json = Json { encodeDefaults = false }

    private fun headers(): Map<String, String> =
        sboData.sboKey.takeIf { it.isNotBlank() }?.let { mapOf("x-sbo-key" to it) } ?: emptyMap()

    private fun post(path: String, body: String = "{}"): HttpRequestHandle =
        Http.sendPostRequest("$API_URL$path", body, headers())

    private fun get(path: String): HttpRequestHandle =
        Http.sendGetRequest("$API_URL$path", headers())

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    fun createParty(request: PartyRequest): HttpRequestHandle =
        post("/createParty", json.encodeToString(request))

    fun updateQueuedParty(request: PartyRequest): HttpRequestHandle =
        post("/updateQueuedParty", json.encodeToString(request))

    fun unqueueParty(): HttpRequestHandle = post("/unqueueParty")

    fun refreshParty(): HttpRequestHandle = post("/refreshParty")

    fun partyInfo(members: List<String>, readCache: Boolean = true): HttpRequestHandle =
        post("/partyInfo", json.encodeToString(MembersRequest(members, readCache)))

    fun partyInfoByUuids(members: List<String>, readCache: Boolean = true): HttpRequestHandle =
        post("/partyInfoByUuids", json.encodeToString(MembersRequest(members, readCache)))

    fun countActiveUsers(): HttpRequestHandle = post("/countActiveUsers")

    fun playerInfo(player: String, readCache: Boolean = true): HttpRequestHandle =
        get("/playerInfo?player=${encode(player)}" + if (readCache) "" else "&readcache=false")

    fun playerInfoByUuid(uuid: String): HttpRequestHandle =
        get("/playerInfoByUuid?uuid=${encode(uuid)}")

    fun listParties(partyType: String): HttpRequestHandle =
        get("/listParties?partyType=${encode(partyType)}")

    fun activeUsers(): HttpRequestHandle = get("/activeUsers")

    fun ahItems(): HttpRequestHandle = get("/ahItems")
}
