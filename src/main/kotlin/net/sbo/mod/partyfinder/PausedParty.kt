package net.sbo.mod.partyfinder

import kotlinx.serialization.json.JsonElement
import net.sbo.mod.utils.data.configs.partyfinder.PartyDraft

/** A listing that is removed from the SBO queue as long as players who don't meet its requirements are in the party. */
internal class PausedParty(val draft: PartyDraft, val reqs: Map<String, JsonElement>, val options: Map<String, String>) {
    private val unfit = mutableSetOf<String>()

    val everyoneLeft: Boolean get() = unfit.isEmpty()

    fun add(name: String) {
        unfit += name.lowercase()
    }

    fun left(name: String) {
        unfit -= name.lowercase()
    }

    fun allLeft() = unfit.clear()
}
