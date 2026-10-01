package net.sbo.mod.partyfinder

import net.sbo.mod.partyfinder.api.CheckBody
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.events.Register
import java.util.concurrent.ConcurrentHashMap

/** The player's own stats per party target, for "can I join?". Kept 10 minutes. */
object OwnStats {
    private const val MAX_AGE_MS = 10 * 60 * 1000L

    private val cache = ConcurrentHashMap<String, Pair<Long, MemberView>>()

    fun init() {
        Register.onChatMessage(Regex("^Your profile was changed to: "), noFormatting = true) { _, _ -> clear() }
    }

    fun clear() = cache.clear()

    fun cached(target: PartyTarget): MemberView? = cache[target.key]?.second

    /** [force] skips the backend cache too, which has a cooldown there. */
    fun get(target: PartyTarget, force: Boolean = false, onError: (PfError) -> Unit, callback: (MemberView) -> Unit) {
        val hit = cache[target.key]
        if (!force && hit != null && System.currentTimeMillis() - hit.first < MAX_AGE_MS) {
            callback(hit.second)
            return
        }
        val uuid = Player.getUUIDString().replace("-", "")
        if (uuid.isEmpty()) {
            onError(PfError(PfError.INVALID_REQUEST, "Not in a world"))
            return
        }
        PartyFinderApi.checkMembers(
            CheckBody(target.partyType, target.subType, listOf(uuid), readcache = if (force) false else null),
            onError = onError
        ) { data ->
            val me = data.members.firstOrNull()
            if (me == null) {
                onError(PfError(PfError.NO_PROFILE, "No stats found"))
            } else {
                cache[target.key] = System.currentTimeMillis() to me
                callback(me)
            }
        }
    }
}
