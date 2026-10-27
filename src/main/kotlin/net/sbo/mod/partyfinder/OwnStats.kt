package net.sbo.mod.partyfinder

import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.events.Register

/** The player's own stats per party target, for "can I join?". They come from PlayerStatsCache, every category in one request. */
object OwnStats {
    fun uuid(): String = Player.getUUIDString().replace("-", "").ifEmpty { Player.accountUuid() }

    fun init() {
        Register.onChatMessage(Regex("^Your profile was changed to: "), noFormatting = true) { _, _ -> clear() }
    }

    fun clear() = PlayerStatsCache.forget(uuid())

    /** Party fields the stats depend on (e.g. the slayer tier), missing ones at their default. Empty for most targets. */
    fun statOptions(target: PartyTarget, options: Map<String, String> = emptyMap()): Map<String, String> =
        target.options.filter { it.affectsStats }.associate { it.id to (options[it.id] ?: it.default) }

    fun cached(target: PartyTarget, options: Map<String, String> = emptyMap()): MemberView? =
        PlayerStatsCache.cached(uuid())?.member(target.partyType, target.subType, statOptions(target, options))

    private fun noStats() = PfError(PfError.NO_PROFILE, "No stats found")

    /** With force the backend loads from Hypixel again, at most every 5 minutes. */
    fun get(
        target: PartyTarget,
        options: Map<String, String> = emptyMap(),
        force: Boolean = false,
        onError: (PfError) -> Unit,
        callback: (MemberView) -> Unit
    ) {
        PlayerStatsCache.own(force, onError) { stats ->
            stats.member(target.partyType, target.subType, statOptions(target, options))?.let(callback) ?: onError(noStats())
        }
    }

    /** Your own stats for every subcategory of "All". */
    fun getAll(targets: List<PartyTarget>, onError: (PfError) -> Unit, callback: (PartyTarget, MemberView) -> Unit) {
        PlayerStatsCache.own(onError = onError) { stats ->
            targets.forEach { target ->
                stats.member(target.partyType, target.subType, statOptions(target))?.let { callback(target, it) } ?: onError(noStats())
            }
        }
    }

    /** Your own stats for several parties, each party can have its own fields like the slayer tier. */
    fun getMany(
        wanted: List<Pair<PartyTarget, Map<String, String>>>,
        onError: (PartyTarget, Map<String, String>, PfError) -> Unit,
        callback: (PartyTarget, Map<String, String>, MemberView) -> Unit
    ) {
        PlayerStatsCache.own(onError = { error -> wanted.forEach { (target, options) -> onError(target, options, error) } }) { stats ->
            wanted.forEach { (target, options) ->
                stats.member(target.partyType, target.subType, statOptions(target, options))?.let { callback(target, options, it) }
                    ?: onError(target, options, noStats())
            }
        }
    }
}
