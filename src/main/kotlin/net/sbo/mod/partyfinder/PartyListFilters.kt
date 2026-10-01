package net.sbo.mod.partyfinder

import kotlinx.serialization.json.JsonNull
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.utils.data.configs.partyfinder.PartyListFilter

/** Applies the party list filters and sorting. The own party always stays on top. */
object PartyListFilters {
    fun apply(
        parties: List<PartyView>,
        filter: PartyListFilter,
        target: PartyTarget,
        me: MemberView?,
        myId: String?,
        search: String = ""
    ): List<PartyView> {
        val query = search.trim().lowercase()
        val visible = parties.filter { party ->
            party.id == myId || matches(party, filter, target, me, query)
        }
        val order = when (filter.sort) {
            PartyListFilter.SORT_OLDEST -> compareBy<PartyView> { it.createdAt }
            PartyListFilter.SORT_MOST_FREE -> compareByDescending<PartyView> { freeSlots(it) }.thenByDescending { it.createdAt }
            PartyListFilter.SORT_ALMOST_FULL -> compareBy<PartyView> { if (freeSlots(it) > 0) freeSlots(it) else Int.MAX_VALUE }.thenByDescending { it.createdAt }
            PartyListFilter.SORT_FEWEST_REQS -> compareBy<PartyView> { reqCount(it) }.thenByDescending { it.createdAt }
            else -> compareByDescending<PartyView> { it.createdAt }
        }
        return visible.sortedWith(compareByDescending<PartyView> { it.id == myId }.then(order))
    }

    fun freeSlots(party: PartyView): Int = (party.partySize - party.memberCount).coerceAtLeast(0)

    fun reqCount(party: PartyView): Int = party.reqs.values.count { it !is JsonNull }

    private fun matches(party: PartyView, filter: PartyListFilter, target: PartyTarget, me: MemberView?, query: String): Boolean {
        if (filter.notFull && ReqMatcher.isFull(party)) return false
        if (filter.canJoin && me != null && ReqMatcher.checkJoin(party, target, me).isNotEmpty()) return false
        if (filter.sizes.isNotEmpty() && party.partySize !in filter.sizes) return false
        if (freeSlots(party) < filter.minFreeSlots) return false
        if (filter.withNote && party.note.isBlank()) return false
        for ((id, wanted) in filter.options) {
            val option = target.options.firstOrNull { it.id == id } ?: continue
            if ((party.options[id] ?: option.default) != wanted) return false
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
