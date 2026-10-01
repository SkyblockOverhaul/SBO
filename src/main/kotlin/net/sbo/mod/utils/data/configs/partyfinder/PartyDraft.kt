package net.sbo.mod.utils.data.configs.partyfinder

/**
 * A party the player wants to queue, also stored per (sub)category as the last used input.
 * [reqs] holds each requirement value as JSON text by stat id, e.g. `"tracking" to "50"`.
 */
data class PartyDraft(
    var partyType: String = "",
    var subType: String = "",
    var partySize: Int = 6,
    var note: String = "",
    var reqs: MutableMap<String, String> = mutableMapOf(),
    var options: MutableMap<String, String> = mutableMapOf(),
    var wantedRoles: MutableList<String> = mutableListOf()
) {
    /** Same key as `PartyTarget.key`. */
    val key: String get() = if (subType.isEmpty()) partyType else "$partyType/$subType"
}
