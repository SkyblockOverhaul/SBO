package net.sbo.mod.profileviewer

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import net.sbo.mod.partyfinder.OwnStats
import net.sbo.mod.partyfinder.PlayerStatsCache
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.partyfinder.api.PlayerStats
import net.sbo.mod.partyfinder.api.TrackingData
import net.sbo.mod.utils.Player

internal data class ViewedProfile(
    val uuid: String,
    val name: String,
    // category, subcategory ("" without), stat id
    val stats: Map<String, Map<String, Map<String, JsonElement>>>,
    val tracking: TrackingData? = null,
    val profile: String? = null,
    val dataAt: Long? = null,
    val refreshAt: Long = 0,
    val networth: Double? = null,
    val skillAverage: Double? = null
) {
    constructor(player: PlayerStats, loadedAt: Long) : this(
        uuid = player.uuid,
        name = player.name,
        stats = player.stats,
        tracking = player.tracking,
        profile = player.profileName.ifBlank { null },
        dataAt = player.dataAt.takeIf { it > 0 },
        refreshAt = loadedAt + player.refreshIn * 1000L,
        skillAverage = player.skillAverage
    )

    // For stats like the SkyBlock level that are the same everywhere
    fun general(statId: String): JsonElement? = stats.values.asSequence().flatMap { it.values }.firstNotNullOfOrNull { it[statId] }

    val ironman: Boolean get() = (general("ironman") as? JsonPrimitive)?.booleanOrNull == true
}

internal data class CompareParty(val partyType: String, val subType: String, val reqs: Map<String, JsonElement>) {
    constructor(party: PartyView) : this(party.partyType, party.subType, party.reqs)
}

internal object ProfileViewerApi {
    const val MAX_OTHERS = 5

    private fun viewed(stats: PlayerStats) = ViewedProfile(stats, PlayerStatsCache.loadedAt(stats.uuid) ?: System.currentTimeMillis())

    fun cached(nameOrUuid: String): ViewedProfile? = PlayerStatsCache.cached(nameOrUuid)?.let(::viewed)

    // Your own stats go through the same request as in the party finder
    fun player(name: String, refresh: Boolean, onError: (PfError) -> Unit, onDone: (ViewedProfile) -> Unit) {
        val own = name.equals(Player.getName() ?: Player.accountName(), ignoreCase = true) && OwnStats.uuid().isNotEmpty()
        if (own) PlayerStatsCache.own(refresh, onError) { onDone(viewed(it)) }
        else PlayerStatsCache.player(name, refresh, onError) { onDone(viewed(it)) }
    }

    fun party(uuids: List<String>, onError: (PfError) -> Unit, onDone: (List<ViewedProfile>) -> Unit) =
        PlayerStatsCache.players(uuids.take(MAX_OTHERS), onError) { players -> onDone(players.map(::viewed)) }
}
