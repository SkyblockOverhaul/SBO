package net.sbo.mod.partyfinder

import net.sbo.mod.partyfinder.api.CheckBody
import net.sbo.mod.partyfinder.api.CheckScope
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SboKey
import net.sbo.mod.utils.events.Register
import java.util.concurrent.ConcurrentHashMap

/** The player's own stats per party target, for "can I join?". Kept 10 minutes. */
object OwnStats {
    private const val MAX_AGE_MS = 10 * 60 * 1000L

    private val cache = ConcurrentHashMap<String, Pair<Long, MemberView>>()

    fun uuid(): String = Player.getUUIDString().replace("-", "").ifEmpty { Player.accountUuid() }

    fun init() {
        Register.onChatMessage(Regex("^Your profile was changed to: "), noFormatting = true) { _, _ -> clear() }
    }

    fun clear() = cache.clear()

    /** Party fields the stats depend on (e.g. the slayer tier), missing ones at their default. Empty for most targets. */
    fun statOptions(target: PartyTarget, options: Map<String, String> = emptyMap()): Map<String, String> =
        target.options.filter { it.affectsStats }.associate { it.id to (options[it.id] ?: it.default) }

    private fun keyOf(target: PartyTarget, options: Map<String, String>): String {
        val picked = statOptions(target, options)
        return if (picked.isEmpty()) target.key else target.key + "?" + picked.entries.joinToString("&") { "${it.key}=${it.value}" }
    }

    fun cached(target: PartyTarget, options: Map<String, String> = emptyMap()): MemberView? = cache[keyOf(target, options)]?.second

    /** [options] are party fields, only the ones stats depend on are sent. [force] skips the backend cache too, which has a cooldown there. */
    fun get(
        target: PartyTarget,
        options: Map<String, String> = emptyMap(),
        force: Boolean = false,
        onError: (PfError) -> Unit,
        callback: (MemberView) -> Unit
    ) {
        val key = keyOf(target, options)
        val sent = statOptions(target, options).takeIf { it.isNotEmpty() }
        val hit = cache[key]
        if (!force && hit != null && System.currentTimeMillis() - hit.first < MAX_AGE_MS) {
            callback(hit.second)
            return
        }
        val uuid = uuid()
        if (uuid.isEmpty()) {
            onError(PfError(PfError.INVALID_REQUEST, "Not in a world"))
            return
        }
        val body = bodyFor(uuid, target, sent, readcache = if (force) false else null)
        PartyFinderApi.checkMembers(
            body,
            onError = onError
        ) { data ->
            val me = data.members.firstOrNull()
            if (me == null) {
                onError(PfError(PfError.NO_PROFILE, "No stats found"))
            } else {
                cache[key] = System.currentTimeMillis() to me
                callback(me)
            }
        }
    }

    /** Loads your own stats for every subcategory of "All" with a single request. */
    fun getAll(targets: List<PartyTarget>, onError: (PfError) -> Unit, callback: (PartyTarget, MemberView) -> Unit) {
        val now = System.currentTimeMillis()
        val fresh = targets.associateWith { cache[keyOf(it, emptyMap())]?.takeIf { hit -> now - hit.first < MAX_AGE_MS }?.second }
        fresh.forEach { (target, me) -> if (me != null) callback(target, me) }
        val missing = targets.filter { fresh[it] == null }
        if (missing.size <= 1) {
            missing.forEach { target -> get(target, onError = onError) { callback(target, it) } }
            return
        }
        val uuid = uuid()
        if (uuid.isEmpty()) {
            onError(PfError(PfError.INVALID_REQUEST, "Not in a world"))
            return
        }
        val body = bodyFor(uuid, missing.first(), null, null).copy(subTypes = missing.map { it.subType })
        PartyFinderApi.checkMembers(body, onError = onError) { data ->
            val at = System.currentTimeMillis()
            missing.forEach { target ->
                val me = data.bySubType[target.subType]?.firstOrNull() ?: return@forEach onError(PfError(PfError.NO_PROFILE, "No stats found"))
                cache[keyOf(target, emptyMap())] = at to me
                callback(target, me)
            }
        }
    }

    /** Loads your own stats for several parties with a single request, each party can have its own options like the slayer tier. They all have to be the same party type. */
    fun getMany(
        wanted: List<Pair<PartyTarget, Map<String, String>>>,
        onError: (PartyTarget, Map<String, String>, PfError) -> Unit,
        callback: (PartyTarget, Map<String, String>, MemberView) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val missing = wanted.distinctBy { (target, options) -> keyOf(target, options) }.filter { (target, options) ->
            val hit = cache[keyOf(target, options)]?.takeIf { now - it.first < MAX_AGE_MS }?.second
            if (hit != null) callback(target, options, hit)
            hit == null
        }
        if (missing.size <= 1) {
            missing.forEach { (target, options) -> get(target, options, onError = { onError(target, options, it) }) { callback(target, options, it) } }
            return
        }
        val uuid = uuid()
        if (uuid.isEmpty()) {
            missing.forEach { (target, options) -> onError(target, options, PfError(PfError.INVALID_REQUEST, "Not in a world")) }
            return
        }
        val scopes = missing.map { (target, options) -> CheckScope(target.subType, statOptions(target, options).takeIf { it.isNotEmpty() }) }
        val body = bodyFor(uuid, missing.first().first, null, null).copy(scopes = scopes)
        PartyFinderApi.checkMembers(body, onError = { e -> missing.forEach { (target, options) -> onError(target, options, e) } }) { data ->
            val at = System.currentTimeMillis()
            missing.forEachIndexed { i, (target, options) ->
                val me = data.byScope.getOrNull(i)?.firstOrNull()
                    ?: return@forEachIndexed onError(target, options, PfError(PfError.NO_PROFILE, "No stats found"))
                cache[keyOf(target, options)] = at to me
                callback(target, options, me)
            }
        }
    }

    // Without an SBO key we look the player up by name, so opening the GUI never forces a Mojang login
    private fun bodyFor(uuid: String, target: PartyTarget, options: Map<String, String>?, readcache: Boolean?): CheckBody =
        if (SboKey.get().isNotBlank()) {
            CheckBody(target.partyType, target.subType, listOf(uuid), options = options, readcache = readcache)
        } else {
            CheckBody(target.partyType, target.subType, names = listOf(Player.getName() ?: Player.accountName()), options = options, readcache = readcache)
        }
}
