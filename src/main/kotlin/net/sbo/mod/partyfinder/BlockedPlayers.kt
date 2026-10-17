package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.partyfinder.BlockedPlayer
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.SentCommandEvent
import net.sbo.mod.utils.http.Http
import net.sbo.mod.utils.http.Http.getString
import java.net.URLEncoder

/**
 * Players who can't ask to join the own parties and whose parties are hidden.
 * Hypixel's /block add and /block remove change the list too.
 */
object BlockedPlayers {
    private const val MOJANG_PROFILE = "https://api.mojang.com/users/profiles/minecraft/"
    private val BLOCK_COMMAND = Regex("""^/?block (add|remove) (\w{1,16})$""", RegexOption.IGNORE_CASE)

    private val config get() = DataManager.partyFinderConfigState

    fun list(): List<BlockedPlayer> = config.blockedPlayers.toList()

    fun isBlocked(uuid: String): Boolean = normalize(uuid).let { id -> config.blockedPlayers.any { it.uuid == id } }

    fun isBlockedName(name: String): Boolean = config.blockedPlayers.any { it.name.equals(name, ignoreCase = true) }

    /** Without [uuid] the name is looked up at Mojang first. */
    fun block(name: String, uuid: String? = null) {
        if (uuid == null) return lookup(name) { id, realName -> block(realName, id) }
        val id = normalize(uuid)
        config.blockedPlayers.removeAll { it.uuid == id }
        config.blockedPlayers.add(BlockedPlayer(id, name))
        config.save()
        PartyFinderManager.tell("§6[SBO] §eBlocked §b$name§e. They can no longer ask to join your parties.", true)
    }

    fun unblock(uuid: String) {
        val id = normalize(uuid)
        val player = config.blockedPlayers.firstOrNull { it.uuid == id } ?: return
        config.blockedPlayers.remove(player)
        config.save()
        PartyFinderManager.tell("§6[SBO] §eUnblocked §b${player.name}§e.", true)
    }

    @SboEvent
    fun onCommandSent(event: SentCommandEvent) {
        val match = BLOCK_COMMAND.find(event.content.trim()) ?: return
        val name = match.groupValues[2]
        mc.execute {
            if (match.groupValues[1].equals("add", ignoreCase = true)) block(name)
            else config.blockedPlayers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { unblock(it.uuid) }
        }
    }

    private fun lookup(name: String, onFound: (uuid: String, name: String) -> Unit) {
        Http.sendGetRequest(MOJANG_PROFILE + URLEncoder.encode(name, Charsets.UTF_8)).toJsonObject { profile ->
            val id = profile.getString("id") ?: return@toJsonObject
            mc.execute { onFound(id, profile.getString("name") ?: name) }
        }.error { error -> SBOKotlin.logger.warn("[SBO] Could not look up $name to block: ${error.message}") }
    }

    private fun normalize(uuid: String): String = uuid.replace("-", "").lowercase()
}
