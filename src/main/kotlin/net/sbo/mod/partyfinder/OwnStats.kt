package net.sbo.mod.partyfinder

import net.sbo.mod.partyfinder.api.CheckBody
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.MojangAuth
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
        val readcache = if (force) false else null
        // Until the Mojang login ran, by name like /partyInfo, so opening the GUI does not log in
        val body = if (MojangAuth.hasSession()) {
            CheckBody(target.partyType, target.subType, listOf(uuid), options = sent, readcache = readcache)
        } else {
            CheckBody(target.partyType, target.subType, names = listOf(Player.getName() ?: Player.accountName()), options = sent, readcache = readcache)
        }
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
}
