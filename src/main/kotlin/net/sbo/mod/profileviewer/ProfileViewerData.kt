package net.sbo.mod.profileviewer

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import net.sbo.mod.partyfinder.OwnStats
import net.sbo.mod.partyfinder.PartyPlayer
import net.sbo.mod.partyfinder.PlayerStatsCache
import net.sbo.mod.partyfinder.api.GeneralStats
import net.sbo.mod.partyfinder.api.Networth
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.partyfinder.api.PlayerStats
import net.sbo.mod.partyfinder.api.TrackingData
import net.sbo.mod.utils.Player
import java.util.WeakHashMap

internal data class ViewedProfile(
    val uuid: String,
    val name: String,
    // category, subcategory ("" without), stat id
    val stats: Map<String, Map<String, Map<String, JsonElement>>>,
    val tracking: TrackingData? = null,
    val profile: String? = null,
    val dataAt: Long? = null,
    val refreshAt: Long = 0,
    val networth: Networth? = null,
    val skillAverage: Double? = null,
    val general: GeneralStats? = null,
    // category > subcategory > option > option value > stat: stats that change with an option, e.g. the Kuudra tier
    val byOption: Map<String, Map<String, Map<String, Map<String, Map<String, JsonElement>>>>> = emptyMap(),
    val items: PlayerItems? = null
) {
    constructor(player: PlayerStats, loadedAt: Long) : this(
        uuid = player.uuid,
        name = player.name,
        stats = player.stats,
        tracking = player.tracking,
        profile = player.profileName.ifBlank { null },
        dataAt = player.dataAt.takeIf { it > 0 },
        refreshAt = loadedAt + player.refreshIn * 1000L,
        networth = player.networth,
        skillAverage = player.general?.skillAverage ?: player.skillAverage,
        general = player.general,
        byOption = player.byOption,
        items = ProfileViewerApi.items(player.items)
    )

    fun anyStat(statId: String): JsonElement? = stats.values.asSequence().flatMap { it.values }.firstNotNullOfOrNull { it[statId] }

    val ironman: Boolean get() = (anyStat("ironman") as? JsonPrimitive)?.booleanOrNull == true
}

internal data class PerValue(val subType: String, val statId: String, val optionId: String)

internal object ViewerLayout {
    val HIDDEN_CATEGORIES = setOf("bestiary")
    val HIDDEN_STATS = setOf("frozenBlaze", "frozenWaterPungi", "safariTickets", "slayerLevel", "kuudraCompletions", "kuudraWave100")

    val TOP: Map<String, List<String>> = mapOf(
        "fishing" to listOf("trophyFisher", "trophyFishCaught", "bloodshotBelt")
    )

    val PER_VALUE: Map<String, List<PerValue>> = mapOf(
        "kuudra" to listOf(PerValue("waves", "kuudraHighestWave", "tier"))
    )

    val NO_SUBCATEGORIES = setOf("kuudra")
}

internal object ProfileViewerApi {
    const val MAX_OTHERS = 5

    private val unpacked = WeakHashMap<String, PlayerItems>()

    fun items(packed: String?): PlayerItems? {
        packed ?: return null
        synchronized(unpacked) { unpacked[packed]?.let { return it } }
        val items = PlayerItems.unpack(packed) ?: return null
        synchronized(unpacked) { return unpacked.getOrPut(packed) { items } }
    }

    private fun viewed(stats: PlayerStats) = ViewedProfile(stats, PlayerStatsCache.loadedAt(stats.uuid) ?: System.currentTimeMillis())

    fun cached(nameOrUuid: String): ViewedProfile? = PlayerStatsCache.cached(nameOrUuid)?.let(::viewed)

    /**
     * Always requests the stats from the backend, since it may have newer ones than the mod loaded earlier.
     * With fromCache, stats from the last 10 minutes are used instead.
     */
    fun player(name: String, refresh: Boolean, fromCache: Boolean = false, onError: (PfError) -> Unit, onDone: (ViewedProfile) -> Unit) {
        val own = name.equals(Player.getName() ?: Player.accountName(), ignoreCase = true) && OwnStats.uuid().isNotEmpty()
        if (own && refresh) PartyPlayer.reloadStats(onError) { onDone(viewed(it)) }
        else if (own) PlayerStatsCache.own(fromCache = fromCache, onError = onError) { onDone(viewed(it)) }
        else PlayerStatsCache.player(name, refresh, fromCache, items = true, onError = onError) { onDone(viewed(it)) }
    }

    fun party(uuids: List<String>, onError: (PfError) -> Unit, onDone: (List<ViewedProfile>) -> Unit) =
        PlayerStatsCache.players(uuids.take(MAX_OTHERS), onError) { players -> onDone(players.map(::viewed)) }
}
