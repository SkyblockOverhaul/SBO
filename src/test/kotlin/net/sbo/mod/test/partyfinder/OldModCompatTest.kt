package net.sbo.mod.test.partyfinder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Plays mod 0.5.1 to 0.6.0 against a local backend: same requests, and every answer is read with
 * a frozen copy of their data classes and JSON settings, so a field the old mods cannot read fails here.
 * Skipped unless SBO_TEST_KEY is set, see PartyFinderLiveTest.
 */
class OldModCompatTest {
    private val api = System.getenv("SBO_TEST_API") ?: "http://localhost:3000"
    private val key = System.getenv("SBO_TEST_KEY").orEmpty()
    private val client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()

    private val leader = "33cd429790564c91a7579f56a3739431" // D4rkswift, owns the key
    private val member = "6099000dfd1e4153acc1d206f1a93433" // Emxa
    private val joiner = "504b14df86ea44c1b04f54f747f47982" // HotMenFeet

    // SboApi and HttpRequestHandle.toJson of the old mods
    private val requestJson = Json { encodeDefaults = false }
    private val responseJson = Json { ignoreUnknownKeys = true }

    private fun send(method: String, path: String, body: String? = null): String {
        val request = HttpRequest.newBuilder(URI.create("$api$path")).header("x-sbo-key", key)
            .header("X-SBO-Version", "0.6.0")
        if (method == "POST") request.header("Content-Type", "application/json")
        request.method(method, if (method == "POST") HttpRequest.BodyPublishers.ofString(body ?: "{}") else HttpRequest.BodyPublishers.noBody())
        val response = client.send(request.build(), HttpResponse.BodyHandlers.ofString())
        assertTrue(response.statusCode() in 200..299, "$path answered HTTP ${response.statusCode()}: ${response.body()}")
        return response.body()
    }

    private inline fun <reified T> read(text: String): T = responseJson.decodeFromString<T>(text)

    private fun success(text: String): Boolean = read<JsonObject>(text)["Success"]?.jsonPrimitive?.booleanOrNull ?: false

    private fun list(type: String): List<OldParty> =
        read<OldGetAllParties>(send("GET", "/listParties?partyType=${URLEncoder.encode(type, Charsets.UTF_8)}")).parties

    @Test
    fun oldModsStillWork() {
        assumeTrue(key.isNotEmpty(), "SBO_TEST_KEY not set")

        val diana = OldPartyRequest(listOf(leader, member), OldReqs(lvl = 100, kills = 0, eman9 = true, looting5 = true), "Diana", "old mod", 4)
        val created = read<OldPartyAddResponse>(send("POST", "/createParty", requestJson.encodeToString(diana)))
        assertTrue(created.success, "create: ${created.error}")
        assertEquals(4, created.partySize)
        assertNotNull(created.partyReqs)

        val party = list("Diana").firstOrNull { it.leader == leader }
        assertNotNull(party, "old list shows the party")
        assertEquals(2, party!!.partyMembersCount)
        assertEquals(100, party.reqs.lvl)
        assertTrue(party.reqs.eman9 && party.reqs.looting5, "eman9 and looting5 stay for old mods")
        assertEquals("old mod", party.note)
        assertTrue(party.partyInfo.all { it.name.isNotEmpty() && it.sbLvl > 0 })

        val updated = read<OldPartyUpdateResponse>(send("POST", "/updateQueuedParty", requestJson.encodeToString(diana.copy(uuids = listOf(leader, member, joiner)))))
        assertTrue(updated.success, "update: ${updated.error}")
        assertEquals(3, list("Diana").first { it.leader == leader }.partyMembersCount)

        assertTrue(success(send("POST", "/refreshParty")), "refresh")

        val tooHigh = read<OldPartyAddResponse>(send("POST", "/createParty", requestJson.encodeToString(diana.copy(reqs = OldReqs(lvl = 99999)))))
        assertFalse(tooHigh.success)
        assertFalse(tooHigh.error.isNullOrBlank(), "old mods show the error text")

        assertTrue(success(send("POST", "/unqueueParty")), "unqueue")
        assertTrue(list("Diana").none { it.leader == leader })

        val custom = read<OldPartyAddResponse>(send("POST", "/createParty", requestJson.encodeToString(OldPartyRequest(listOf(leader), OldReqs(mp = 1), "Custom", "", 3))))
        assertTrue(custom.success, "custom: ${custom.error}")
        assertTrue(list("Custom").any { it.leader == leader })
        assertTrue(success(send("POST", "/unqueueParty")))

        val byUuid = read<OldPartyInfo>(send("POST", "/partyInfoByUuids", requestJson.encodeToString(OldMembersRequest(listOf(leader, member)))))
        assertTrue(byUuid.success)
        assertEquals(2, byUuid.partyInfo.size)
        val byName = read<OldPartyInfo>(send("POST", "/partyInfo", requestJson.encodeToString(OldMembersRequest(listOf("D4rkswift")))))
        assertTrue(byName.partyInfo.single().name.equals("D4rkswift", ignoreCase = true))

        val player = read<OldPlayerInfoResponse>(send("GET", "/playerInfo?player=D4rkswift"))
        assertTrue(player.success && player.playerInfo?.uuid == leader)

        send("POST", "/countActiveUsers")
        assertNotNull(read<JsonObject>(send("GET", "/activeUsers"))["activeUsers"]?.jsonPrimitive?.intOrNull)
    }

    @Test
    fun newModPartiesStayOutOfOldList() {
        assumeTrue(key.isNotEmpty(), "SBO_TEST_KEY not set")
        // Old and new mods have separate queues
        val body = """{"partyType":"diana","uuids":["$leader"],"partySize":5,"reqs":{"tracking":5,"griffin":"RARE"},"note":"new mod"}"""
        assertTrue(send("POST", "/pf/parties", body).contains("\"success\":true"))
        assertTrue(list("Diana").none { it.leader == leader })
        list("Custom")
        send("POST", "/pf/parties/remove")
    }
}

// Frozen copy of the party classes in utils/data/DataClasses.kt of mod 0.5.1 to 0.6.0 (identical in all three)

@Serializable
private data class OldGetAllParties(
    @SerialName("Success") val success: Boolean = false,
    @SerialName("Parties") val parties: List<OldParty> = emptyList()
)

@Serializable
private data class OldPartyInfo(
    @SerialName("Success") val success: Boolean = false,
    @SerialName("PartyInfo") val partyInfo: List<OldPartyPlayerStats> = emptyList(),
)

@Serializable
private data class OldPlayerInfoResponse(
    @SerialName("Success") val success: Boolean = false,
    @SerialName("PlayerInfo") val playerInfo: OldPartyPlayerStats? = null,
    @SerialName("Error") val error: String? = null
)

@Serializable
private data class OldPartyAddResponse(
    @SerialName("Success") val success: Boolean = false,
    @SerialName("Message") val message: String? = null,
    @SerialName("PartyInfo") val partyInfo: List<OldPartyPlayerStats>? = null,
    @SerialName("PartyReqs") val partyReqs: OldReqs? = null,
    @SerialName("PartySize") val partySize: Int? = null,
    @SerialName("Error") val error: String? = null
)

@Serializable
private data class OldPartyUpdateResponse(
    @SerialName("Success") val success: Boolean = false,
    @SerialName("Message") val message: String? = null,
    @SerialName("PartyReqs") val partyReqs: OldReqs? = null,
    @SerialName("PartySize") val partySize: Int? = null,
    @SerialName("Error") val error: String? = null
)

@Serializable
private data class OldPartyRequest(
    val uuids: List<String>,
    val reqs: OldReqs,
    val partyType: String = "Diana",
    val note: String = "",
    val partySize: Int = 6
)

@Serializable
private data class OldMembersRequest(
    val members: List<String>,
    val readcache: Boolean = true
)

@Serializable
private data class OldParty(
    @SerialName("partyinfo") val partyInfo: List<OldPartyPlayerStats>,
    val reqs: OldReqs,
    val leader: String,
    @SerialName("partymembers") val partyMembersCount: Int,
    val leaderName: String,
    val note: String,
    val partySize: Int
)

@Serializable
private data class OldPartyPlayerStats(
    val name: String = "",
    val sbLvl: Int = -1,
    val eman9: Boolean = false,
    val looting5daxe: Boolean = false,
    val emanLvl: Int = 0,
    val warnings: List<String> = emptyList(),
    val uuid: String = "",
    val clover: Boolean = false,
    val daxeLootingLvl: Int = 0,
    val daxeChimLvl: Int = 0,
    val invApi: Boolean = false,
    val magicalPower: Int = 0,
    val enrichments: Int = 0,
    val missingEnrichments: Int = 0,
    val griffinRarity: String = "",
    val griffinItem: JsonPrimitive? = null,
    val killLeaderboard: Int = 999999,
    val mythosKills: Int = 0
)

@Serializable
private data class OldReqs(
    val lvl: Int = -1,
    val kills: Int = 0,
    val eman9: Boolean = false,
    val looting5: Boolean = false,
    val mp: Int = 0
)
