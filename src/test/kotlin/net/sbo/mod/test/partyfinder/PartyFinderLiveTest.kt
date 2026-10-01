package net.sbo.mod.test.partyfinder

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import net.sbo.mod.partyfinder.PartyCategories
import net.sbo.mod.partyfinder.ReqMatcher
import net.sbo.mod.partyfinder.api.BphReport
import net.sbo.mod.partyfinder.api.CategoriesData
import net.sbo.mod.partyfinder.api.CheckBody
import net.sbo.mod.partyfinder.api.CheckData
import net.sbo.mod.partyfinder.api.PartiesData
import net.sbo.mod.partyfinder.api.PartyBody
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.partyfinder.api.StatsReportBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Runs against a local backend with real profiles. Skipped unless SBO_TEST_KEY is set:
 * `pnpm dev:api`, then `pnpm pf:devkey` in the backend, then
 * `SBO_TEST_KEY=<key> ./gradlew :26.1.2-fabric:test --tests '*PartyFinderLiveTest'`.
 */
class PartyFinderLiveTest {
    private val api = System.getenv("SBO_TEST_API") ?: "http://localhost:3000"
    private val key = System.getenv("SBO_TEST_KEY").orEmpty()
    private val client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()

    private val leader = "33cd429790564c91a7579f56a3739431" // D4rkswift, owns the key
    private val joiners = listOf(
        "6099000dfd1e4153acc1d206f1a93433", // Emxa
        "88ab893d058c4f7ead8274a8385a7043", // RolexDE
        "504b14df86ea44c1b04f54f747f47982" // HotMenFeet
    )

    private inline fun <reified T> call(method: String, path: String, body: String? = null, withKey: Boolean = true): Result<T> {
        val request = HttpRequest.newBuilder(URI.create("$api$path"))
        if (withKey) request.header("x-sbo-key", key)
        if (body != null) request.header("Content-Type", "application/json")
        request.method(method, if (body == null) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofString(body))
        val response = client.send(request.build(), HttpResponse.BodyHandlers.ofString())
        return PartyFinderApi.parse<T>(response.body(), response.statusCode())
    }

    private inline fun <reified T> post(path: String, body: Any): T {
        val text = when (body) {
            is PartyBody -> PartyFinderApi.json.encodeToString(body)
            is CheckBody -> PartyFinderApi.json.encodeToString(body)
            is StatsReportBody -> PartyFinderApi.json.encodeToString(body)
            else -> "{}"
        }
        return call<T>("POST", path, text).getOrThrow()
    }

    @Test
    fun ownStatsByNameWithoutKey() {
        assumeTrue(key.isNotEmpty(), "SBO_TEST_KEY not set")
        // Like /partyInfo: names work without a key, uuids don't (no uuid probing for bots)
        val byName = call<CheckData>("POST", "/pf/members/check",
            PartyFinderApi.json.encodeToString(CheckBody("diana", names = listOf("D4rkswift"))), withKey = false).getOrThrow()
        assertEquals(leader, byName.members.single().uuid)
        val byUuid = call<CheckData>("POST", "/pf/members/check",
            PartyFinderApi.json.encodeToString(CheckBody("diana", uuids = listOf(leader))), withKey = false)
        assertEquals("INVALID_KEY", (byUuid.exceptionOrNull() as? PartyFinderApi.PfException)?.error?.code)
    }

    @Test
    fun fullRoundTripAndSameChecksAsBackend() {
        assumeTrue(key.isNotEmpty(), "SBO_TEST_KEY not set")
        val categories = call<CategoriesData>("GET", "/pf/categories").getOrThrow()
        PartyCategories.use(categories)

        for ((type, sub) in listOf("diana" to "", "kuudra" to "infernal", "fishing" to "lava", "slayer" to "voidgloom", "mining" to "mineshaft")) {
            val target = PartyCategories.target(type, sub)!!
            // Requirements copied from the leader's own stats, so the others fail some of them
            val me = post<CheckData>("/pf/members/check", CheckBody(type, sub, listOf(leader))).members.single()
            val reqs = target.reqs.mapNotNull { def ->
                val have = me.stats[def.stat] ?: return@mapNotNull null
                val need: JsonElement = when (def.type) {
                    "min" -> (have as? JsonPrimitive)?.doubleOrNull?.takeIf { it > 0 }?.let { JsonPrimitive(it) } ?: return@mapNotNull null
                    "flag" -> if (have == JsonPrimitive(true)) have else return@mapNotNull null
                    "rarity" -> if (have is JsonPrimitive && have != JsonNull) have else return@mapNotNull null
                    "anyOf" -> (have as? JsonArray)?.takeIf { it.isNotEmpty() }?.let { owned ->
                        buildJsonArray {
                            owned.forEach { item ->
                                val obj = item as JsonObject
                                add(buildJsonObject {
                                    put("id", obj["id"]!!)
                                    obj["tier"]?.let { put("minTier", it) }
                                })
                            }
                        }
                    } ?: return@mapNotNull null
                    else -> return@mapNotNull null
                }
                def.stat to need
            }.toMap()
            val options = if (type == "slayer") mapOf("purpose" to "trading", "tier" to "4") else emptyMap()

            val party = post<PartyView>(
                "/pf/parties",
                PartyBody(type, sub, categories.version, listOf(leader), target.maxSize, "live test", reqs, options)
            )
            assertEquals(1, party.memberCount, "$type: created")
            assertTrue(call<PartiesData>("GET", "/pf/parties?partyType=$type&subType=$sub").getOrThrow().parties.any { it.id == leader })

            val check = post<CheckData>("/pf/members/check", CheckBody(type, sub, joiners, partyId = leader))
            val local = check.members.flatMap { ReqMatcher.check(it.name, it.stats, party.reqs, target, party.options) }
            assertEquals(
                check.problems.map { it.name to it.stat }.sortedBy { it.toString() },
                local.map { it.name to it.stat }.sortedBy { it.toString() },
                "$type/$sub: mod and backend disagree"
            )
            println("$type/$sub: ${reqs.size} reqs, ${check.problems.size} problems, " +
                check.problems.groupBy { it.name }.map { (n, p) -> "$n ${p.map { it.stat }}" })
        }

        assertTrue(call<JsonElement>("POST", "/pf/parties/refresh", "{}").isSuccess)
        assertTrue(call<JsonElement>("POST", "/pf/stats/report", PartyFinderApi.json.encodeToString(StatsReportBody(BphReport(500.0, "current", 2.0, 1000)))).isSuccess)
        val badBph = call<JsonElement>("POST", "/pf/stats/report", PartyFinderApi.json.encodeToString(StatsReportBody(BphReport(900.0, "current", 2.0, 1000))))
        assertEquals("INVALID_REQUEST", (badBph.exceptionOrNull() as PartyFinderApi.PfException).error.code)
        assertTrue(call<JsonElement>("POST", "/pf/parties/remove", "{}").isSuccess)
        val gone = call<JsonElement>("POST", "/pf/parties/refresh", "{}")
        assertEquals("PARTY_NOT_FOUND", (gone.exceptionOrNull() as PartyFinderApi.PfException).error.code)
    }
}
