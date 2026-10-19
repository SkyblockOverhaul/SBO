package net.sbo.mod.diana

import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.DianaMobDetect.RareDianaMob
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayExamples
import net.sbo.mod.utils.overlay.OverlayTextLine
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * While a rare mob is around, lists the party with who lootshared it: red until they sent the
 * "[SBO] Lootshared ..." party message from [LootshareDisplay], aqua after. Whoever shared the
 * mob's coords in party chat is shown as the spawner, they always get the loot.
 */
object LootshareParty {
    // Messages up to this long before the mob showed up still count, the spawn share usually comes first
    private val MESSAGE_LEAD_NS = TimeUnit.SECONDS.toNanos(30L)
    // How long the overlay stays after the last rare mob is gone
    private val LINGER_NS = TimeUnit.SECONDS.toNanos(10L)

    private val partyChatRegex = Regex("^§9[^§]+ §[0-9a-fk-or]> (.*?)§[0-9a-fk-or]*: ?(.*)$")
    private val lootsharedRegex = Regex("""^\[SBO] Lootshared .+!$""")
    // The rare mob share from DianaMobDetect.onRareSpawn: "x: 1, y: 2, z: 3 | King Minos"
    private val spawnShareRegex = Regex("""x: -?\d+, y: -?\d+, z: -?\d+ \| (.+)$""")

    private val overlay = Overlay(name = "lootshareParty", x = 10f, y = 60f, exampleView = OverlayExamples.lootsharePartyExample)
        .setCondition { Diana.lootsharePartyOverlay }

    private var memberUuids: List<String> = emptyList()
    private val names = mutableMapOf<String, String>()
    // Lowercase player name to when they sent the message
    private val lootshared = mutableMapOf<String, Long>()
    private val spawners = mutableMapOf<String, Long>()
    private var startNanos = 0L
    private var lastMobNanos = 0L

    fun init() {
        overlay.init()

        HypixelModApi.onPartyInfo { _, _, members -> memberUuids = members }

        Register.onChatMessage(partyChatRegex) { _, match ->
            val sender = Helper.getPlayerName(match.groupValues[1]).lowercase()
            val text = match.groupValues[2].removeFormatting().trim()
            val now = System.nanoTime()
            when {
                lootsharedRegex.matches(text) -> lootshared[sender] = now
                RareDianaMob.fromName(spawnShareRegex.find(text)?.groupValues?.get(1) ?: "") != null -> spawners[sender] = now
            }
        }

        Register.onTick(5) { update() }
    }

    private fun update() {
        val now = System.nanoTime()
        val mobs = LootshareDisplay.rareMobsAround
        if (mobs.isNotEmpty()) {
            if (lastMobNanos == 0L) startNanos = now
            lastMobNanos = now
        } else if (lastMobNanos != 0L && now - lastMobNanos > LINGER_NS) {
            lastMobNanos = 0L
            lootshared.clear()
            spawners.clear()
        }

        if (lastMobNanos == 0L || !Diana.lootsharePartyOverlay || !(PartyFinderManager.isInParty || Debug.lootshareDisplayTest)) {
            overlay.clearLines()
            return
        }

        val header = mobs.distinct().joinToString("§7, §d") { it.display }.ifEmpty { "gone" }
        val lines = mutableListOf(OverlayTextLine("§6Lootshare §7- §d$header"))
        memberNames().forEach { name -> lines += OverlayTextLine(memberLine(name, now)) }
        overlay.setLines(lines)
    }

    private fun memberLine(name: String, now: Long): String {
        val key = name.lowercase()
        val isSelf = key == Player.getName()?.lowercase()
        return when {
            (isSelf && LootshareDisplay.ownMobAround) || spawners.isRecent(key, now) -> "§d$name §7(spawned)"
            (isSelf && LootshareDisplay.lootsharedAny) || lootshared.isRecent(key, now) -> "§b$name ✔"
            else -> "§c$name ✖"
        }
    }

    private fun Map<String, Long>.isRecent(key: String, now: Long): Boolean {
        val sent = this[key] ?: return false
        return sent >= startNanos - MESSAGE_LEAD_NS && sent <= now
    }

    // Members that are not in your lobby have no tab list entry and are left out until they are
    private fun memberNames(): List<String> {
        val own = Player.getName()
        val party = memberUuids.mapNotNull { uuid -> names[uuid] ?: lookupName(uuid)?.also { names[uuid] = it } }
        return (listOfNotNull(own) + party).distinctBy { it.lowercase() }
    }

    private fun lookupName(uuid: String): String? {
        val id = runCatching { UUID.fromString(uuid) }.getOrNull() ?: return null
        return mc.connection?.getPlayerInfo(id)?.profile?.name()
    }
}
