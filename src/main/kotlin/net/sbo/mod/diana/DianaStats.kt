package net.sbo.mod.diana

import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DianaTracker
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.events.Register
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object DianaStats {
    val STATS_PATTERN: Pattern = Pattern.compile(
        "§9Party §8> (.*?)§f: Playtime: (.*?) - Profit: (.*?) - (.*?) - Burrows: (.*?) \\((.*?)\\) - Mobs: (.*?) \\((.*?)\\) - Inquisitors: (.*?) \\((.*?)\\) - LS Inqs: (.*?) - Chimeras: (.*?) \\((.*?)\\) - LS: (.*?) \\((.*?)\\) - Sticks: (.*?) \\((.*?)\\) - Relics: (.*?) \\((.*?)\\)(.*?)",
        Pattern.DOTALL
    )

    fun registerReplaceStatsMessage() {
        Register.onChatMessageCancelable(
            STATS_PATTERN
        ) { message, matcher ->
            val statsMessage = ArrayList<String>()
            statsMessage.add("§9Party §8> ${matcher.group(1)}§f:")
            statsMessage.add("§ePlaytime: §b${matcher.group(2)}")
            statsMessage.add("§aBurrows: §b${matcher.group(5)} §7(${matcher.group(6)}/h)")
            statsMessage.add("§aMobs: §b${matcher.group(7)} §7(${matcher.group(8)}/h)")
            statsMessage.add("§dInquisitors: §b${matcher.group(9)} §7(${matcher.group(10)}) §6LS: §b${matcher.group(11)}")
            statsMessage.add("§dChimeras: §b${matcher.group(12)} §7(${matcher.group(13)}) §6LS: §b${matcher.group(14)} §7(${matcher.group(15)})")
            statsMessage.add("§6Sticks: §b${matcher.group(16)} §7(${matcher.group(17)})")
            statsMessage.add("§5Relics: §b${matcher.group(18)} §7(${matcher.group(19)})")
            statsMessage.add("§6Profit: §b${matcher.group(3)} §7(${matcher.group(4)})")
            Chat.chat(statsMessage.joinToString("\n"))
            false
        }
    }

    fun getPlayerStats(total: Boolean? = false): PlayerStats {
        val tracker: DianaTracker = when (total) {
            true -> SboDataObject.dianaTrackerTotal
            false -> SboDataObject.dianaTrackerMayor
            else -> SboDataObject.dianaTrackerSession
        }

        val timer: SboTimerManager.SBOTimer = when (total) {
            true -> SboTimerManager.timerTotal
            false -> SboTimerManager.timerMayor
            else -> SboTimerManager.timerSession
        }

        val playtime = tracker.items.TIME
        val playTimeHrs = playtime.toDouble() / TimeUnit.HOURS.toMillis(1)

        val burrowsPerHour = Helper.getBurrowsPerHr(tracker, timer)
        val mobsPerHour = if (playTimeHrs > 0) tracker.mobs.TOTAL_MOBS.toDouble() / playTimeHrs else 0.0

        val totalValue = DianaLoot.totalProfit(tracker)
        val profit = listOf(
            Helper.formatNumber(totalValue),
            Diana.bazaarSettingDiana.toString(),
            Helper.formatNumber(totalValue / playTimeHrs)
        )
        val stats = PlayerStats(
            playtime = Helper.formatTime(playtime),
            profit = profit,
            burrows = Helper.formatNumber(tracker.items.TOTAL_BURROWS),
            burrowsPerHour = "%.2f".format(Locale.US, burrowsPerHour),
            totalMobs = Helper.formatNumber(tracker.mobs.TOTAL_MOBS),
            mobsPerHour = "%.2f".format(Locale.US, mobsPerHour),
            inquisitors = tracker.mobs.MINOS_INQUISITOR,
            inqPercentage = "${Helper.calcPercentOne(tracker.items, tracker.mobs, "MINOS_INQUISITOR")}%",
            lsInqs = Helper.formatNumber(tracker.mobs.MINOS_INQUISITOR_LS, withCommas = true),
            chimeraDrops = tracker.items.CHIMERA,
            chimeraDropRate = "${Helper.calcPercentOne(tracker.items, tracker.mobs, "CHIMERA", "MINOS_INQUISITOR")}%",
            chimeraLSDrops = tracker.items.CHIMERA_LS,
            chimeraLSDropRate = "${"%.2f".format(Locale.US, if (tracker.mobs.MINOS_INQUISITOR_LS > 0) tracker.items.CHIMERA_LS.toDouble() / tracker.mobs.MINOS_INQUISITOR_LS.toDouble() * 100.0 else 0.0)}%",
            sticksDropped = tracker.items.DAEDALUS_STICK,
            stickDropRate = "${Helper.calcPercentOne(tracker.items, tracker.mobs, "DAEDALUS_STICK", "MINOTAUR")}%",
            relicsDropped = tracker.items.MINOS_RELIC,
            relicDropRate = "${Helper.calcPercentOne(tracker.items, tracker.mobs, "MINOS_RELIC", "MINOS_CHAMPION")}%"
        )
        return stats
    }

    fun sendPlayerStats(total: Boolean? = false) {
        val stats = getPlayerStats(total)
        val statsMessage = buildString {
            append("Playtime: ${stats.playtime} - ")
            append("Profit: ${stats.profit[0]} - ${stats.profit[2]}/h - ")
            append("Burrows: ${stats.burrows} (${stats.burrowsPerHour}/h) - ")
            append("Mobs: ${stats.totalMobs} (${stats.mobsPerHour}/h) - ")
            append("Inquisitors: ${stats.inquisitors} (${stats.inqPercentage}) - ")
            append("LS Inqs: ${stats.lsInqs} - ")
            append("Chimeras: ${stats.chimeraDrops} (${stats.chimeraDropRate}) - LS: ${stats.chimeraLSDrops} (${stats.chimeraLSDropRate}) - ")
            append("Sticks: ${stats.sticksDropped} (${stats.stickDropRate}) - ")
            append("Relics: ${stats.relicsDropped} (${stats.relicDropRate})")
        }

        Chat.pc(statsMessage)
    }

    data class PlayerStats(
        val playtime: String,
        val profit: List<String>,
        val burrows: String,
        val burrowsPerHour: String,
        val totalMobs: String,
        val mobsPerHour: String,
        val inquisitors: Int,
        val inqPercentage: String,
        val lsInqs: String,
        val chimeraDrops: Int,
        val chimeraDropRate: String,
        val chimeraLSDrops: Int,
        val chimeraLSDropRate: String,
        val sticksDropped: Int,
        val stickDropRate: String,
        val relicsDropped: Int,
        val relicDropRate: String
    )
}
