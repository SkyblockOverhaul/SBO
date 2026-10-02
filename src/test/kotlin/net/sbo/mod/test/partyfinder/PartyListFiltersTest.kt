package net.sbo.mod.test.partyfinder

import kotlinx.serialization.json.JsonPrimitive
import net.sbo.mod.partyfinder.PartyCategories
import net.sbo.mod.partyfinder.PartyListFilters
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.api.CategoriesData
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.partyfinder.api.WantedRoles
import net.sbo.mod.utils.data.configs.partyfinder.PartyListFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PartyListFiltersTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun loadCategories() {
            val body = PartyListFiltersTest::class.java.getResource("/partyfinder/categories.json")!!.readText()
            PartyCategories.use(PartyFinderApi.parse<CategoriesData>(body, 200).getOrThrow())
        }

        private val kuudra: PartyTarget get() = PartyCategories.target("kuudra", "infernal")!!

        private fun party(
            id: String,
            size: Int = 4,
            members: Int = 1,
            createdAt: Long = 0,
            note: String = "",
            roles: List<String> = emptyList(),
            options: Map<String, String> = emptyMap(),
            reqs: Int = 0,
            name: String = id
        ) = PartyView(
            id = id,
            partyType = "kuudra",
            subType = "infernal",
            partySize = size,
            memberCount = members,
            note = note,
            createdAt = createdAt,
            reqs = (0 until reqs).associate { "stat$it" to JsonPrimitive(1) },
            options = options,
            roles = WantedRoles(roles),
            members = listOf(MemberView(uuid = id, name = name))
        )

        private fun ids(parties: List<PartyView>, filter: PartyListFilter, search: String = "", myId: String? = null) =
            PartyListFilters.apply(parties, filter, kuudra, null, myId, search).map { it.id }
    }

    @Test
    fun filtersBySizeFreeSpotsAndSearch() {
        val parties = listOf(
            party("a", size = 2, members = 1, note = "chill run"),
            party("b", size = 4, members = 3),
            party("c", size = 4, members = 1, name = "RolexDE")
        )
        assertEquals(listOf("a"), ids(parties, PartyListFilter(sizes = mutableListOf(2))))
        assertEquals(listOf("c"), ids(parties, PartyListFilter(minFreeSlots = 2)))
        assertEquals(listOf("c"), ids(parties, PartyListFilter(), search = "rolex"))
        assertEquals(listOf("a"), ids(parties, PartyListFilter(), search = "CHILL"))
    }

    @Test
    fun filtersByOptionsAndRoles() {
        val parties = listOf(
            party("a", options = mapOf("ironman" to "only"), roles = listOf("dps")),
            party("b", roles = listOf("support")),
            party("c")
        )
        assertEquals(listOf("a"), ids(parties, PartyListFilter(options = mutableMapOf("ironman" to "only"))))
        // Parties without the field count as its default (anyone)
        assertEquals(listOf("b", "c"), ids(parties, PartyListFilter(options = mutableMapOf("ironman" to "any"))).sorted())
        // Parties without wanted roles take everyone
        assertEquals(listOf("a", "c"), ids(parties, PartyListFilter(roles = mutableListOf("dps"))).sorted())
    }

    @Test
    fun sortsAndKeepsOwnPartyOnTop() {
        val parties = listOf(
            party("old", createdAt = 1, members = 3, reqs = 2),
            party("new", createdAt = 3, members = 1, reqs = 0),
            party("mid", createdAt = 2, members = 4, reqs = 1),
            party("me", createdAt = 0, members = 4)
        )
        // Longest waiting first, also for old saved sorts
        assertEquals(listOf("me", "old", "mid", "new"), ids(parties, PartyListFilter(), myId = "me"))
        assertEquals(listOf("me", "old", "mid", "new"), ids(parties, PartyListFilter(sort = "newest"), myId = "me"))
        assertEquals(listOf("me", "old", "mid", "new"), ids(parties, PartyListFilter(sort = "fewestReqs"), myId = "me"))
        assertEquals(listOf("me", "new", "old", "mid"), ids(parties, PartyListFilter(sort = PartyListFilter.SORT_MOST_FREE), myId = "me"))
        assertEquals(listOf("me", "old", "new", "mid"), ids(parties, PartyListFilter(sort = PartyListFilter.SORT_ALMOST_FULL), myId = "me"))
        // The own party ignores filters
        assertEquals(listOf("me"), ids(parties, PartyListFilter(minFreeSlots = 5), myId = "me"))
    }

    @Test
    fun filtersByWhatPartiesAskFor() {
        fun asking(id: String, vararg reqs: Pair<String, kotlinx.serialization.json.JsonElement>) =
            party(id).copy(reqs = reqs.toMap())
        val diana = PartyCategories.target("diana")!!
        val parties = listOf(
            asking("high", "tracking" to JsonPrimitive(50), "griffin" to JsonPrimitive("MYTHIC")),
            asking("low", "tracking" to JsonPrimitive(20), "griffin" to JsonPrimitive("RARE")),
            asking("none")
        )
        fun dianaIds(filter: PartyListFilter) = PartyListFilters.apply(parties, filter, diana, null, null).map { it.id }
        assertEquals(listOf("high", "low"), dianaIds(PartyListFilter(reqs = mutableMapOf("tracking" to "20"))))
        assertEquals(listOf("high"), dianaIds(PartyListFilter(reqs = mutableMapOf("tracking" to "40"))))
        assertEquals(listOf("high"), dianaIds(PartyListFilter(reqs = mutableMapOf("griffin" to "LEGENDARY"))))
        assertEquals(listOf("high", "low", "none"), dianaIds(PartyListFilter()))
        // Item lists are not offered
        assertEquals(listOf("tracking", "griffin", "dianaKills", "sbLevel", "magicalPower", "bph"), PartyListFilters.filterableReqs(diana).map { it.stat })
        assertEquals(false, PartyListFilters.filterableReqs(PartyCategories.target("kuudra", "infernal")!!).any { it.type == "anyOf" })

        val bacte = PartyCategories.target("rift", "bacte")!!
        val rift = listOf(asking("charm", "livingTimecharm" to JsonPrimitive(true)), asking("free"))
        assertEquals(listOf("charm"), PartyListFilters.apply(rift, PartyListFilter(reqs = mutableMapOf("livingTimecharm" to "true")), bacte, null, null).map { it.id })
        assertEquals(1, PartyListFilter(reqs = mutableMapOf("livingTimecharm" to "true")).dialogCount())
    }
}
