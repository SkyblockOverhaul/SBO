package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.partyfinder.api.ProfileBody
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SboKey
import net.sbo.mod.utils.events.Register

/** The player's own stats per party target, for "can I join?". They come from PlayerStatsCache, every category in one request. */
object OwnStats {
    // The profile Hypixel named after the last join or switch, null before the first one
    private var profileId: String? = null

    fun uuid(): String = Player.getUUIDString().replace("-", "").ifEmpty { Player.accountUuid() }

    fun init() {
        // Hypixel names the profile after every SkyBlock join and every switch. Its API only shows a switch
        // after a while, so the mod sends the profile ID to the SBO server, which then knows about the switch right away.
        Register.onChatMessage(Regex("^Profile ID: ([0-9a-fA-F-]{32,36})$"), noFormatting = true) { _, match ->
            onProfileId(match.groupValues[1])
        }
    }

    private fun onProfileId(id: String) {
        if (id.equals(profileId, ignoreCase = true)) return
        val switched = profileId != null
        profileId = id
        val reload = {
            if (switched) {
                clear()
                PartyPlayer.load()
            }
        }
        if (SboKey.get().isBlank()) return reload()
        PartyFinderApi.reportProfile(ProfileBody(id), onError = { error ->
            logger.warn("[SBO] Profile not sent: ${error.message}")
            reload()
        }) { reload() }
    }

    fun clear() = PlayerStatsCache.forget(uuid())

    /** Party fields the stats depend on (e.g. the slayer tier), missing ones at their default. Empty for most targets. */
    fun statOptions(target: PartyTarget, options: Map<String, String> = emptyMap()): Map<String, String> =
        target.options.asSequence().filter { it.affectsStats }.associate { it.id to (options[it.id] ?: it.default) }

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
        PlayerStatsCache.own(force, onError = onError) { stats ->
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
        PlayerStatsCache.own(onError = { error -> wanted.forEach { [target, options] -> onError(target, options, error) } }) { stats ->
            wanted.forEach { [target, options] ->
                stats.member(target.partyType, target.subType, statOptions(target, options))?.let { callback(target, options, it) }
                    ?: onError(target, options, noStats())
            }
        }
    }
}
