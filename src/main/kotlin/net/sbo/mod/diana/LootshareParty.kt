package net.sbo.mod.diana

import net.minecraft.ChatFormatting
import net.minecraft.ChatFormatting.AQUA
import net.minecraft.ChatFormatting.LIGHT_PURPLE
import net.minecraft.ChatFormatting.RED
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.DianaMobDetect.RareDianaMob
import net.sbo.mod.overlays.DianaOverlay
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.overlay.CHAT_SCREEN_FILTER
import net.sbo.mod.utils.overlay.OverlayEditScreen
import net.sbo.mod.utils.overlay.OverlayExamples
import net.sbo.mod.utils.overlay.OverlayTextLine
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * While a rare mob is around, lists the party with who lootshared it: red until they sent the
 * "[SBO] Lootshared ..." party message from [LootshareDisplay], aqua after. Whoever shared the
 * mob's coords in party chat is shown as the spawner, they always get the loot.
 */
object LootshareParty : DianaOverlay<LootshareParty.View>("lootshareParty", Diana.TrackerOverlay.LOOTSHARE, { Diana.lootsharePartyOverlay },
    y = 60f,
    allowedScreens = listOf(CHAT_SCREEN_FILTER),
    extraCondition = { true },
    exampleLines = OverlayExamples.lootsharePartyExample
) {
    // Messages up to this long before the mob showed up still count, the spawn share usually comes first
    private val MESSAGE_LEAD_NS = TimeUnit.SECONDS.toNanos(30L)
    // How long the overlay stays after the last rare mob is gone
    private val LINGER_NS = TimeUnit.SECONDS.toNanos(10L)

    private val partyChatRegex = Regex("^§9[^§]+ §[0-9a-fk-or]> (.*?)§[0-9a-fk-or]*: ?(.*)$")
    private val lootsharedRegex = Regex("""^\[SBO] Lootshared .+!$""")
    // The rare mob share from DianaMobDetect.onRareSpawn: "x: 1, y: 2, z: 3 | King Minos"
    private val spawnShareRegex = Regex("""x: -?\d+, y: -?\d+, z: -?\d+ \| (.+)$""")

    private var memberUuids: List<String> = emptyList()
    private val names = mutableMapOf<String, String>()
    // Lowercase player name to when they sent the message
    private val lootshared = mutableMapOf<String, Long>()
    private val spawners = mutableMapOf<String, Long>()
    private var startNanos = 0L
    private var lastMobNanos = 0L

    private enum class Status { SPAWNED, SHARED, MISSING }

    // What update() last found, empty while the overlay is hidden
    private var shownMobs: List<RareDianaMob>? = null
    private var members: List<Pair<String, Status>> = emptyList()

    fun init() {
        initOverlays()

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
            shownMobs = null
            members = emptyList()
        } else {
            shownMobs = mobs.distinct()
            members = memberNames().map { it to memberStatus(it, now) }
        }
        updateLines()
    }

    private fun memberStatus(name: String, now: Long): Status {
        val key = name.lowercase()
        val isSelf = key == Player.getName()?.lowercase()
        return when {
            (isSelf && LootshareDisplay.ownMobAround) || spawners.isRecent(key, now) -> Status.SPAWNED
            (isSelf && LootshareDisplay.lootsharedAny) || lootshared.isRecent(key, now) -> Status.SHARED
            else -> Status.MISSING
        }
    }

    override fun generateLines(): List<OverlayTextLine> {
        val mobs = shownMobs ?: return emptyList()
        val header = mobs.joinToString("§7, §d") { it.display }.ifEmpty { "gone" }
        val lines = mutableListOf(OverlayTextLine("§6Lootshare §7- §d$header"))
        members.forEach { (name, status) ->
            lines += OverlayTextLine(when (status) {
                Status.SPAWNED -> "§d$name §7(spawned)"
                Status.SHARED -> "§b$name ✔"
                Status.MISSING -> "§c$name ✖"
            })
        }
        return lines
    }

    data class View(val mobs: String, val rows: List<Row>)

    override fun view(): View {
        // An example in /sboguis while no rare mob is around, so it can be placed
        val mobs = shownMobs ?: return if (mc.screen is OverlayEditScreen) EXAMPLE else View("", emptyList())
        return View(mobs.joinToString(", ") { it.display }.ifEmpty { "Gone" }, members.map { (name, status) -> memberRow(name, status) })
    }

    private fun memberRow(name: String, status: Status): Row {
        val (color: ChatFormatting, value: String) = when (status) {
            Status.SPAWNED -> LIGHT_PURPLE to "Spawned"
            Status.SHARED -> AQUA to "✔"
            Status.MISSING -> RED to "✖"
        }
        return Row(null, name, color, value, valueClass = colorClass(color))
    }

    private val EXAMPLE = View("King Minos", listOf(
        memberRow("Saotzuri", Status.SPAWNED),
        memberRow("Friend", Status.SHARED),
        memberRow("OtherFriend", Status.MISSING)
    ))

    override fun NodeBuilder.content(view: View) {
        if (view.rows.isEmpty() && view.mobs.isEmpty()) return
        tracker("lootshare") {
            head("Lootshare") { badge(view.mobs, extraClass = "mob") }
            view.rows.forEach { row(it) }
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
