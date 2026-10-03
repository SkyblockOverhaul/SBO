package net.sbo.mod.partyfinder

import kotlinx.serialization.json.JsonPrimitive
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyOption
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.partyfinder.api.ReqDef
import net.sbo.mod.utils.data.configs.partyfinder.PartyListFilter

/** Applies the party list filters and sorting. The own party always stays on top. */
object PartyListFilters {
    private val REQ_TYPES = setOf("min", "rarity", "flag")
    private const val ANY = "any"

    fun apply(
        parties: List<PartyView>,
        filter: PartyListFilter,
        target: PartyTarget,
        me: MemberView?,
        myId: String?,
        search: String = "",
        // Own stats for one party: its subcategory in the list of all of them, its tier for slayer
        meFor: (PartyView, PartyTarget) -> MemberView? = { _, _ -> me }
    ): List<PartyView> {
        val query = search.trim().lowercase()
        val visible = parties.filter { party ->
            val partyTarget = target.forParty(party)
            party.id == myId || matches(party, filter, partyTarget, meFor(party, partyTarget), query)
        }
        // Ties always go to the party that waits longest, old saved sorts fall back to that too
        val order = when (filter.sort) {
            PartyListFilter.SORT_MOST_FREE -> compareByDescending<PartyView> { freeSlots(it) }
            PartyListFilter.SORT_ALMOST_FULL -> compareBy<PartyView> { if (freeSlots(it) > 0) freeSlots(it) else Int.MAX_VALUE }
            else -> compareBy<PartyView> { 0 }
        }
        return visible.sortedWith(compareByDescending<PartyView> { it.id == myId }.then(order).thenBy { it.createdAt })
    }

    /** Party fields the filter dialog offers. */
    fun filterableOptions(target: PartyTarget): List<PartyOption> = target.options

    /** Fields with many values or several picks, the filter dialog offers several values for them. */
    fun filtersSeveral(option: PartyOption): Boolean = option.multiple || option.values.size > 3

    /** Values the filter dialog offers for a field; "any" is left out, such parties always match. */
    fun filterValues(option: PartyOption) = option.values.filter { !filtersSeveral(option) || it.id != ANY }

    /** Whether the party's value of [option] matches the [wanted] filter value (a comma list for several values). */
    fun optionMatches(party: PartyView, option: PartyOption, wanted: String): Boolean {
        val value = party.options[option.id] ?: option.default
        if (!filtersSeveral(option)) return value == wanted
        val picked = option.picks(wanted) - ANY
        if (picked.isEmpty() || value == ANY) return true
        // No picks on a field with several values means all of them, like the bosses
        val partyPicks = if (option.multiple) option.picks(value).ifEmpty { option.values.map { it.id } } else listOf(value)
        return partyPicks.any { it in picked }
    }

    /** Requirements the filter dialog offers, item lists are left out. */
    fun filterableReqs(target: PartyTarget): List<ReqDef> = target.reqs.filter { it.type in REQ_TYPES }

    /** Whether the party asks for at least [wanted]; parties that don't ask for the stat never do. */
    fun asksAtLeast(party: PartyView, def: ReqDef, wanted: String): Boolean {
        val need = if (def.type == "min") wanted.toDoubleOrNull()?.let(::JsonPrimitive) ?: return true else JsonPrimitive(wanted)
        return ReqMatcher.meets(def.type, party.reqs[def.stat], need)
    }

    fun freeSlots(party: PartyView): Int = (party.partySize - party.memberCount).coerceAtLeast(0)

    private fun matches(party: PartyView, filter: PartyListFilter, target: PartyTarget, me: MemberView?, query: String): Boolean {
        if (filter.notFull && ReqMatcher.isFull(party)) return false
        if (filter.canJoin && me != null && ReqMatcher.checkJoin(party, target, me).isNotEmpty()) return false
        if (filter.sizes.isNotEmpty() && party.partySize !in filter.sizes) return false
        if (freeSlots(party) < filter.minFreeSlots) return false
        val options = filterableOptions(target)
        for ((id, wanted) in filter.options) {
            val option = options.firstOrNull { it.id == id } ?: continue
            if (!optionMatches(party, option, wanted)) return false
        }
        val reqs = filterableReqs(target)
        for ((stat, wanted) in filter.reqs) {
            val def = reqs.firstOrNull { it.stat == stat } ?: continue
            if (!asksAtLeast(party, def, wanted)) return false
        }
        // Parties without wanted roles take everyone
        if (filter.roles.isNotEmpty() && party.roles.wanted.isNotEmpty() && party.roles.wanted.none { it in filter.roles }) return false
        if (query.isNotEmpty()) {
            val names = party.members.map { it.name.lowercase() }
            if (query !in party.note.lowercase() && names.none { query in it }) return false
        }
        return true
    }
}
