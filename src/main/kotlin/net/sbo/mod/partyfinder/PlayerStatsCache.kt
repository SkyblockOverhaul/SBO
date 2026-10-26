package net.sbo.mod.partyfinder

import net.sbo.mod.partyfinder.api.OwnStatsBody
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.partyfinder.api.PlayerStats
import net.sbo.mod.partyfinder.api.ViewerStatsBody
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SboKey
import java.util.concurrent.ConcurrentHashMap

/** Players with every category, shared by the party finder and the profile viewer. Kept 10 minutes. */
object PlayerStatsCache {
    private const val MAX_AGE_MS = 10 * 60 * 1000L
    private const val FIRST_PAUSE_MS = 30_000L
    private const val MAX_PAUSE_MS = 5 * 60 * 1000L

    // withItems means it was asked with items, the stats still have none when the inventory API is off
    private class Entry(val stats: PlayerStats, val at: Long, val withItems: Boolean)

    private class Failure(val error: PfError, val at: Long, val count: Int) {
        // 30 seconds, then 1, 2, 4 and at most 5 minutes
        val pause: Long get() = minOf(FIRST_PAUSE_MS shl (count - 1).coerceAtMost(4), MAX_PAUSE_MS)
    }

    // lowercase names and uuids
    private val players = ConcurrentHashMap<String, Entry>()
    private val failures = ConcurrentHashMap<String, Failure>()
    // So the same player is only asked for once at a time
    private val waiting = HashMap<String, MutableList<Pair<(PfError) -> Unit, (PlayerStats) -> Unit>>>()

    private fun key(nameOrUuid: String) = nameOrUuid.trim().lowercase().replace("-", "")

    private fun entry(nameOrUuid: String): Entry? =
        players[key(nameOrUuid)]?.takeIf { System.currentTimeMillis() - it.at < MAX_AGE_MS }

    fun cached(nameOrUuid: String, items: Boolean = false): PlayerStats? = entry(nameOrUuid)?.takeIf { !items || it.withItems }?.stats

    fun loadedAt(nameOrUuid: String): Long? = entry(nameOrUuid)?.at

    fun forget(nameOrUuid: String) {
        val removed = players.remove(key(nameOrUuid)) ?: return
        players.remove(key(removed.stats.uuid))
        players.remove(key(removed.stats.name))
    }

    private fun paused(key: String): PfError? =
        failures[key]?.takeIf { System.currentTimeMillis() - it.at < it.pause }?.error

    /**
     * By uuid with an SBO key, otherwise by name, so opening a GUI never forces a Mojang login.
     * Always with the items, so the profile viewer can show own profile without requesting the items.
     */
    fun own(refresh: Boolean = false, fromCache: Boolean = true, onError: (PfError) -> Unit, onDone: (PlayerStats) -> Unit) {
        val uuid = OwnStats.uuid()
        if (uuid.isEmpty()) return onError(PfError(PfError.INVALID_REQUEST, "Not in a world"))
        val name = Player.getName() ?: Player.accountName()
        val byKey = SboKey.get().isNotBlank()
        val body = OwnStatsBody(
            uuid = uuid.takeIf { byKey }, name = name.takeIf { !byKey }, refresh = refresh.takeIf { it }, items = true
        )
        load(if (byKey) uuid else name, refresh, fromCache, true, onError, onDone) { done, fail -> PartyFinderApi.ownStats(body, fail, done) }
    }

    fun player(
        name: String, refresh: Boolean = false, fromCache: Boolean = true, items: Boolean = false,
        onError: (PfError) -> Unit, onDone: (PlayerStats) -> Unit
    ) {
        load(name, refresh, fromCache, items, onError, onDone) { done, fail ->
            val body = ViewerStatsBody(names = listOf(name), refresh = refresh.takeIf { it }, items = items.takeIf { it })
            PartyFinderApi.viewerStats(body, fail) { data ->
                data.players.firstOrNull()?.let(done) ?: fail(data.failed.firstOrNull()?.error ?: PfError(PfError.NO_PROFILE, "No stats found"))
            }
        }
    }

    /** Up to 5 players by uuid in one request. Players loaded in the last 10 minutes or failing right now are not asked for. */
    fun players(uuids: List<String>, onError: (PfError) -> Unit, onDone: (List<PlayerStats>) -> Unit) {
        val ids = uuids.map(::key).distinct()
        val missing = ids.filter { cached(it) == null && paused(it) == null }
        if (missing.isEmpty()) return onDone(ids.mapNotNull(::cached))
        PartyFinderApi.viewerStats(ViewerStatsBody(uuids = missing), onError = { error ->
            missing.forEach { failed(it, error) }
            onError(error)
        }) { data ->
            data.players.forEach { store(it, withItems = false) }
            data.failed.forEach { failed(key(it.uuid), it.error) }
            onDone(ids.mapNotNull(::cached))
        }
    }

    private fun load(
        nameOrUuid: String,
        refresh: Boolean,
        fromCache: Boolean,
        items: Boolean,
        onError: (PfError) -> Unit,
        onDone: (PlayerStats) -> Unit,
        request: (done: (PlayerStats) -> Unit, fail: (PfError) -> Unit) -> Unit
    ) {
        val key = key(nameOrUuid)
        if (!refresh) {
            if (fromCache) cached(key, items)?.let { return onDone(it) }
            paused(key)?.let { return onError(it) }
        }
        // A request without items cannot answer one that wants them
        val waitKey = if (items) "$key:items" else key
        synchronized(waiting) {
            waiting[waitKey]?.let {
                it += onError to onDone
                return
            }
            waiting[waitKey] = mutableListOf(onError to onDone)
        }
        fun finish(each: (Pair<(PfError) -> Unit, (PlayerStats) -> Unit>) -> Unit) {
            synchronized(waiting) { waiting.remove(waitKey) }.orEmpty().forEach(each)
        }
        request({ stats ->
            store(stats, items)
            finish { it.second(stats) }
        }, { error ->
            // A failed refresh still has the old stats, only failed loads wait before the next try
            if (!refresh) failed(key, error)
            finish { it.first(error) }
        })
    }

    private fun store(stats: PlayerStats, withItems: Boolean) {
        val entry = Entry(stats, System.currentTimeMillis(), withItems)
        listOf(key(stats.uuid), key(stats.name)).forEach {
            players[it] = entry
            failures.remove(it)
        }
    }

    private fun failed(key: String, error: PfError) {
        failures[key] = Failure(error, System.currentTimeMillis(), (failures[key]?.count ?: 0) + 1)
    }
}
