package net.sbo.mod.test.partyfinder

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sbo.mod.partyfinder.PartyCategories
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.ReqMatcher
import net.sbo.mod.partyfinder.api.CategoriesData
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.partyfinder.api.WantedRoles
import net.sbo.mod.partyfinder.gui.StatView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PartyFinderLogicTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun loadCategories() {
            val resource = PartyFinderLogicTest::class.java.getResource("/partyfinder/categories.json")
            assumeTrue(resource != null, "partyfinder/categories.json not present (fetch it from GET /pf/categories)")
            val body = resource!!.readText()
            PartyCategories.use(PartyFinderApi.parse<CategoriesData>(body, 200).getOrThrow())
        }

        private fun target(type: String, sub: String = ""): PartyTarget = PartyCategories.target(type, sub)!!

        private fun n(value: Number): JsonElement = JsonPrimitive(value)

        private fun items(vararg owned: Pair<String, String?>): JsonElement = buildJsonArray {
            owned.forEach { (id, tier) -> add(buildJsonObject { put("id", id); tier?.let { put("tier", it) } }) }
        }
    }

    @Test
    fun readsTheRealContract() {
        val data = PartyCategories.data!!
        assertEquals(
            listOf("diana", "fishing", "mining", "kuudra", "bestiary", "rift", "safari", "slayer", "custom"),
            data.categories.map { it.id }
        )
        assertEquals(14.0, PartyCategories.stat("tracking")?.unverifiedUpTo)
        assertEquals("Adept", PartyCategories.stat("trophyFisher")?.valueLabels?.get(2))
    }

    @Test
    fun mergesSubcategories() {
        val shaft = target("mining", "mineshaft")
        assertEquals(4, shaft.maxSize)
        assertEquals(listOf("ironman", "shaft"), shaft.options.map { it.id })

        val lava = target("fishing", "lava")
        assertEquals(6, lava.maxSize)
        assertTrue(lava.reqs.map { it.stat }.containsAll(listOf("fishingLevel", "trophyFisher", "jawbusKills")))

        assertEquals("water", target("fishing", "nope").subType)
        assertEquals("diana", target("DIANA").key)
        assertEquals(4, target("kuudra", "infernal").maxSize)
        assertEquals(6, target("kuudra", "infernal").roles.size)
    }

    @Test
    fun readsErrorsWithProblems() {
        val body = """{"success":false,"error":{"code":"REQS_NOT_MET","message":"x",
            "problems":[{"name":"Emxa","stat":"tracking","have":null,"need":50}]}}"""
        val error = (PartyFinderApi.parse<JsonElement>(body, 200).exceptionOrNull() as PartyFinderApi.PfException).error
        assertEquals("REQS_NOT_MET", error.code)
        assertEquals(JsonNull, error.problems.single().have)
        assertEquals("Tracking: needs at least 50, has no data", ProblemText.describe(error.problems.single(), target("diana")))

        val rateLimited = PartyFinderApi.parse<JsonElement>("""{"success":false,"error":{"code":"RATE_LIMITED","message":"slow"}}""", 429)
        assertEquals("RATE_LIMITED", (rateLimited.exceptionOrNull() as PartyFinderApi.PfException).error.code)
        assertTrue(PartyFinderApi.parse<JsonElement>("""{"success":true,"data":null}""", 200).isSuccess)
    }

    @Test
    fun matchesEachRequirementType() {
        assertTrue(ReqMatcher.meets("min", n(50), n(50)))
        assertFalse(ReqMatcher.meets("min", n(49.5), n(50)))
        assertFalse(ReqMatcher.meets("min", JsonNull, n(1)))
        assertTrue(ReqMatcher.meets("flag", JsonPrimitive(true), JsonPrimitive(true)))
        assertFalse(ReqMatcher.meets("flag", JsonNull, JsonPrimitive(true)))
        assertTrue(ReqMatcher.meets("rarity", JsonPrimitive("MYTHIC"), JsonPrimitive("LEGENDARY")))
        assertFalse(ReqMatcher.meets("rarity", JsonPrimitive("EPIC"), JsonPrimitive("LEGENDARY")))
        assertFalse(ReqMatcher.meets("rarity", JsonNull, JsonPrimitive("COMMON")))

        val terrorFiery = buildJsonArray { add(buildJsonObject { put("id", "TERROR"); put("minTier", "FIERY") }) }
        assertTrue(ReqMatcher.meets("anyOf", items("TERROR" to "INFERNAL"), terrorFiery))
        assertFalse(ReqMatcher.meets("anyOf", items("TERROR" to "BURNING"), terrorFiery))
        assertFalse(ReqMatcher.meets("anyOf", items("TERROR" to null), terrorFiery))
        assertTrue(ReqMatcher.meets("anyOf", items("AURORA" to null), buildJsonArray { add(JsonPrimitive("AURORA")) }))
        assertFalse(ReqMatcher.meets("anyOf", JsonNull, buildJsonArray { add(JsonPrimitive("AURORA")) }))

        // { "match": "all" } needs every pick, like the backend
        val allOf = buildJsonObject {
            put("match", "all")
            put("picks", buildJsonArray { add(buildJsonObject { put("id", "TERROR"); put("minTier", "FIERY") }); add(JsonPrimitive("AURORA")) })
        }
        assertTrue(ReqMatcher.meets("anyOf", items("TERROR" to "INFERNAL", "AURORA" to null), allOf))
        assertFalse(ReqMatcher.meets("anyOf", items("TERROR" to "INFERNAL"), allOf))
        assertFalse(ReqMatcher.meets("anyOf", items("TERROR" to "HOT", "AURORA" to null), allOf))
    }

    @Test
    fun checksJoinLikeTheBackend() {
        val kuudra = target("kuudra", "infernal")
        val party = PartyView(
            id = "leader", partyType = "kuudra", subType = "infernal", partySize = 4, memberCount = 2,
            reqs = mapOf("magicalPower" to n(1300), "kuudraCompletions" to n(100)),
            options = mapOf("ironman" to "none"),
            roles = WantedRoles(listOf("dps", "stunner"))
        )
        val me = MemberView(
            uuid = "me", name = "RolexDE",
            stats = mapOf("magicalPower" to n(1855), "kuudraCompletions" to n(20), "ironman" to JsonPrimitive(true))
        )
        val problems = ReqMatcher.checkJoin(party, kuudra, me, role = "support")
        assertEquals(listOf("kuudraCompletions", "ironman", "role"), problems.map { it.stat })
        assertEquals("this party is not for Ironman players", ProblemText.describe(problems[1], kuudra))
        assertEquals("pick one of these roles: DPS, Stunner", ProblemText.describe(problems[2], kuudra))
        assertTrue(ReqMatcher.checkJoin(party, kuudra, me.copy(stats = me.stats + ("kuudraCompletions" to n(100)) + ("ironman" to JsonPrimitive(false)))).isEmpty())
        assertFalse(ReqMatcher.isFull(party))
    }

    @Test
    fun slayerTradingChecksEveryone() {
        val party = PartyView(
            id = "leader", partyType = "slayer", subType = "voidgloom", partySize = 4,
            reqs = mapOf("slayerLevel" to n(9)), options = mapOf("ironman" to "any")
        )
        val me = MemberView(uuid = "me", name = "x", stats = mapOf("slayerLevel" to n(1)))
        assertEquals(1, ReqMatcher.checkJoin(party, target("slayer", "voidgloom"), me).size)
    }

    @Test
    fun formatsValues() {
        assertEquals("Adept", ProblemText.value("trophyFisher", n(2)))
        assertEquals("1,855", ProblemText.value("magicalPower", n(1855)))
        assertEquals("42.5", ProblemText.value("tracking", n(42.5)))
        assertEquals("Legendary", ProblemText.value("griffin", JsonPrimitive("LEGENDARY")))
        assertEquals("Fiery Terror Armor", ProblemText.value("kuudraArmor", items("TERROR" to "FIERY")))
        val armor = JsonArray(listOf(buildJsonObject { put("id", "TERROR"); put("minTier", "FIERY") }))
        val problem = net.sbo.mod.partyfinder.api.Problem("x", "kuudraArmor", JsonNull, armor)
        assertNotNull(target("kuudra", "basic").req("kuudraArmor"))
        assertEquals(
            "Armor: needs one of Terror Armor (Fiery or better), has none of them",
            ProblemText.describe(problem, target("kuudra", "basic"))
        )
    }

    @Test
    fun topValuesHaveNothingBetter() {
        val def = target("kuudra", "infernal").req("kuudraArmor")!!
        fun armor(tier: String) = JsonArray(listOf(buildJsonObject { put("id", "CRIMSON"); put("minTier", tier) }))
        assertEquals("Infernal Crimson Armor", StatView.need(def, armor("INFERNAL")))
        assertEquals("Crimson Armor (Fiery or better)", StatView.need(def, armor("FIERY")))
        assertEquals("Crimson Armor", StatView.need(def, armor("BASIC")))
        assertEquals("Mythic", ProblemText.rarityNeed("MYTHIC"))
        assertEquals("Legendary or better", ProblemText.rarityNeed("LEGENDARY"))
    }

    @Test
    fun petsCanAskForARarity() {
        val def = target("kuudra", "infernal").req("kuudraPet")!!
        assertEquals("minRarity", def.choices.first { it.id == "ENDER_DRAGON" }.minimumField)
        // Golden Dragon only exists as Legendary, nothing to choose
        assertNull(def.choices.first { it.id == "GOLDEN_DRAGON" }.minimumField)

        val legendary = JsonArray(listOf(buildJsonObject { put("id", "ENDER_DRAGON"); put("minRarity", "LEGENDARY") }))
        fun pet(rarity: String?) = buildJsonArray { add(buildJsonObject { put("id", "ENDER_DRAGON"); rarity?.let { put("rarity", it) } }) }
        assertTrue(ReqMatcher.meets("anyOf", pet("LEGENDARY"), legendary))
        assertFalse(ReqMatcher.meets("anyOf", pet("EPIC"), legendary))
        assertFalse(ReqMatcher.meets("anyOf", pet(null), legendary))
        assertTrue(ReqMatcher.meets("anyOf", pet("EPIC"), buildJsonArray { add(JsonPrimitive("ENDER_DRAGON")) }))

        assertEquals("Legendary Ender Dragon", StatView.need(def, legendary))
        assertEquals("Legendary Ender Dragon", ProblemText.value("kuudraPet", pet("LEGENDARY")))
        val fishing = target("fishing", "water").req("fishingPet")!!
        val flyingFish = JsonArray(listOf(buildJsonObject { put("id", "FLYING_FISH"); put("minRarity", "EPIC") }))
        assertEquals("Flying Fish (Epic or better)", StatView.need(fishing, flyingFish))
    }

    @Test
    fun noteKeepsTwoLines() {
        assertEquals("a\nb c d", PartyFinderManager.limitNoteLines("a\nb\nc\r\nd"))
        assertEquals("one line", PartyFinderManager.limitNoteLines("one line"))
        assertEquals("a\nb c", PartyFinderManager.checkPartyNote("a\nb\nc"))
        assertEquals("unique + sparkling", PartyFinderManager.checkPartyNote("unique + sparkling"))
    }
}
