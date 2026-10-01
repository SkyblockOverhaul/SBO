package net.sbo.mod.utils.data.configs.partyfinder

/** Filters and sorting of the party list, stored per party key ("kuudra/infernal"). */
data class PartyListFilter(
    var canJoin: Boolean = false,
    var notFull: Boolean = false,
    // Wanted party sizes, empty means all
    var sizes: MutableList<Int> = mutableListOf(),
    var minFreeSlots: Int = 0,
    // Party field id to wanted value, missing means all
    var options: MutableMap<String, String> = mutableMapOf(),
    // Roles the player wants to play, empty means all
    var roles: MutableList<String> = mutableListOf(),
    var sort: String = SORT_DEFAULT
) {
    /** Filters set in the filter dialog (not the quick filters and not the sorting). */
    fun dialogCount(): Int =
        listOf(sizes.isNotEmpty(), minFreeSlots > 0, options.isNotEmpty(), roles.isNotEmpty()).count { it }

    /** A copy with its own collections, so state changes never touch the saved config. */
    fun edited(block: PartyListFilter.() -> Unit): PartyListFilter =
        copy(sizes = sizes.toMutableList(), options = options.toMutableMap(), roles = roles.toMutableList()).apply(block)

    companion object {
        // Longest waiting first, so queuing again never moves a party up
        const val SORT_DEFAULT = "default"
        const val SORT_MOST_FREE = "mostFree"
        const val SORT_ALMOST_FULL = "almostFull"
        const val SORT_FEWEST_REQS = "fewestReqs"
    }
}
