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
            val value = party.options[id] ?: option.default
            // A field with several values matches when the party picked the wanted one
            if (if (option.multiple) wanted !in option.picks(value) else value != wanted) return false
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
