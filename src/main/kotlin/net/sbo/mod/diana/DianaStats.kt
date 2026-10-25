package net.sbo.mod.diana

import jdk.jfr.Percentage
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.configs.diana.DianaTracker
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

// TODO Once majority of people are on 0.7.2 or above, change the party message to send with [SBO] prefix in front.
object DianaStats {
    private val STATS_PATTERN: Pattern = Pattern.compile(
        "§9Party §8> (.*?)§f: (?:\\[SBO\\] )?Playtime: (.*?) - Profit: (.*?) - (.*?) - Burrows: (.*?) \\((.*?)\\) - Mobs: (.*?) \\((.*?)\\) - Inquisitors: (.*?) \\((.*?)\\) - LS Inqs: (.*?) - Chimeras: (.*?) \\((.*?)\\) - LS: (.*?) \\((.*?)\\) - Sticks: (.*?) \\((.*?)\\) - Relics: (.*?) \\((.*?)\\)(.*?)",
        Pattern.DOTALL
    )

    private val STATS_PATTERN_TWO: Pattern = Pattern.compile(
        "§9Party §8> (.*?)§f: (?:\\[SBO\\] )?" +
            "Pt:([^-]+)-" +                       // 2 playtime
            "Pr:(.+?)/(.+?)/h-" +                 // 3 profit, 4 profit/h
            "B:(.+?)/(.+?)-" +                    // 5 burrows, 6 burrows/h
            "M:(\\d+)/(.+?)-" +                   // 7 mobs, 8 mobs/h
            "(?:I:(\\d+)/(.+?)-)?" +              // 9 inqs, 10 inq %
            "(?:LSI:(\\d+)-)?" +                  // 11 LS inqs
            "(?:S:(\\d+)/(.+?)-)?" +              // 12 sphinx, 13 %
            "(?:K:(\\d+)/(.+?)-)?" +              // 14 kings, 15 %
            "(?:M:(\\d+)/(.+?)-)?" +              // 16 mantis, 17 %
            "(?:C(\\d+)/(.+?)-)?" +               // 18 chimeras, 19 %
            "(?:LS: (\\d+)/\\((.+?)\\)-)?" +      // 20 LS chimeras, 21 %
            "(?:S(\\d+)/(.+?)-)?" +               // 22 sticks, 23 %
            "(?:R(\\d+)/(.+?)-)?" +               // 24 relics, 25 %
            "(?:C(\\d+)/(.+?)-)?" +               // 26 cores, 27 %
            "(?:W(\\d+)/(.+?)-)?" +               // 28 wools, 29 %
            "(?:D(\\d+)/(.+?))?-?$",              // 30 dyes, 31 %
        Pattern.DOTALL
    )

    fun registerReplaceStatsMessage() {
        // Keep old pattern for compatibility with older versions
        Register.onChatMessageCancelable(
            STATS_PATTERN
        ) { message, matcher ->
            val statsMessage = ArrayList<String>()
            statsMessage.add("§9Party §8> ${matcher.group(1)}§f:")
            statsMessage.add("§ePlaytime: §b${matcher.group(2)}")
            statsMessage.add("§aBurrows: §b${matcher.group(5)} §7(${matcher.group(6)})")
            statsMessage.add("§aMobs: §b${matcher.group(7)} §7(${matcher.group(8)})")
            statsMessage.add("§dInquisitors: §b${matcher.group(9)} §7(${matcher.group(10)}) §6LS: §b${matcher.group(11)}")
            statsMessage.add("§dChimeras: §b${matcher.group(12)} §7(${matcher.group(13)}) §6LS: §b${matcher.group(14)} §7(${matcher.group(15)})")
            statsMessage.add("§6Sticks: §b${matcher.group(16)} §7(${matcher.group(17)})")
            statsMessage.add("§5Relics: §b${matcher.group(18)} §7(${matcher.group(19)})")
            statsMessage.add("§6Profit: §b${matcher.group(3)} §7(${matcher.group(4)})")
            Chat.chat(statsMessage.joinToString("\n"))
            false
        }

        Register.onChatMessageCancelable(STATS_PATTERN_TWO) { _, matcher ->
            // Appends a line only if the optional count group matched
            fun ArrayList<String>.optional(countGroup: Int, build: (count: String, pct: String) -> String) {
                val count = matcher.group(countGroup) ?: return
                add(build(count, matcher.group(countGroup + 1)))
            }

            val statsMessage = ArrayList<String>().apply {
                add("§9Party §8> ${matcher.group(1)}§f:")
                add("§ePlaytime: §b${matcher.group(2)}")
                add("§aBurrows: §b${matcher.group(5)} §7(${matcher.group(6)}/h)")
                add("§aMobs: §b${matcher.group(7)} §7(${matcher.group(8)}/h)")

                // Inquisitors (+ LS inqs on the same line, like the old format)
                val lsInqs = matcher.group(11)
                optional(9) { c, p -> "§dInquisitors: §b$c §7($p)" + (lsInqs?.let { " §6LS: §b$it" } ?: "") }
                if (matcher.group(9) == null && lsInqs != null) add("§dLS Inqs: §b$lsInqs")

                optional(12) { c, p -> "§dSphinx: §b$c §7($p)" }
                optional(14) { c, p -> "§6Kings: §b$c §7($p)" }
                optional(16) { c, p -> "§aMantis: §b$c §7($p)" }
                optional(18) { c, p -> "§dChimeras: §b$c §7($p)" }
                optional(20) { c, p -> "§dLS Chimeras: §b$c §7($p)" }
                optional(22) { c, p -> "§6Sticks: §b$c §7($p)" }
                optional(24) { c, p -> "§5Relics: §b$c §7($p)" }
                optional(26) { c, p -> "§bCores: §b$c §7($p)" }
                optional(28) { c, p -> "§fWools: §b$c §7($p)" }
                optional(30) { c, p -> "§dDyes: §b$c §7($p)" }

                add("§6Profit: §b${matcher.group(3)} §7(${matcher.group(4)}/h)")
            }

            Chat.chat(statsMessage.joinToString("\n"))
            false
        }
    }

    private fun getPlayerStats(total: Boolean? = false): PlayerStats {
        val tracker: DianaTracker = when (total) {
            true -> DataManager.dianaTrackerTotalData
            false -> DataManager.dianaTrackerMayorData
            else -> DataManager.dianaTrackerSessionData
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
        return PlayerStats(
            playtime = Helper.formatTime(playtime),
            profit = profit,
            burrows = Helper.formatNumber(tracker.items.TOTAL_BURROWS),
            burrowsPerHour = "%.2f".format(Locale.US, burrowsPerHour),
            totalMobs = tracker.mobs.TOTAL_MOBS,
            mobsPerHour = "%.2f".format(Locale.US, mobsPerHour),
            inquisitors = tracker.mobs.MINOS_INQUISITOR,
            inqPercentage = "${Helper.calcPercentOne(tracker.mobs.MINOS_INQUISITOR, tracker.mobs.TOTAL_MOBS)}%",
            lsInqs = tracker.mobs.MINOS_INQUISITOR_LS,
            sphinx = tracker.mobs.SPHINX,
            sphinxPercentage = "${Helper.calcPercentOne(tracker.mobs.SPHINX, tracker.mobs.TOTAL_MOBS)}%",
            mantis = tracker.mobs.MANTICORE,
            mantisPercentage = "${Helper.calcPercentOne(tracker.mobs.MANTICORE, tracker.mobs.TOTAL_MOBS)}%",
            kings = tracker.mobs.KING_MINOS,
            kingPercentage = "${Helper.calcPercentOne(tracker.mobs.KING_MINOS, tracker.mobs.TOTAL_MOBS)}%",
            chimeraDrops = tracker.items.CHIMERA,
            chimeraDropRate = "${Helper.calcPercentOne(
                tracker.items.CHIMERA,
                tracker.mobs.MINOS_INQUISITOR
            )}%",
            chimeraLSDrops = tracker.items.CHIMERA_LS,
            chimeraLSDropRate = "${
                "%.2f".format(
                    Locale.US,
                    if (tracker.mobs.MINOS_INQUISITOR_LS > 0) tracker.items.CHIMERA_LS.toDouble() / tracker.mobs.MINOS_INQUISITOR_LS.toDouble() * 100.0 else 0.0
                )
            }%",
            sticksDropped = tracker.items.DAEDALUS_STICK,
            stickDropRate = "${Helper.calcPercentOne(
                tracker.items.DAEDALUS_STICK,
                tracker.mobs.MINOTAUR
            )}%",
            relicsDropped = tracker.items.MINOS_RELIC,
            relicDropRate = "${Helper.calcPercentOne(
                tracker.items.MINOS_RELIC,
                tracker.mobs.MINOS_CHAMPION
            )}%",
            coresDropped = tracker.items.MANTI_CORE,
            coreDropRate = "${Helper.calcPercentOne(
                tracker.items.MANTI_CORE,
                tracker.mobs.MANTICORE
            )}%",
            woolsDropped = tracker.items.SHIMMERING_WOOL,
            woolDropRate = "${Helper.calcPercentOne(
                tracker.items.SHIMMERING_WOOL,
                tracker.mobs.KING_MINOS
            )}%",
            dyesDropped = tracker.items.MYTHOLOGICAL_DYE,
            dyeDropRate = "${Helper.calcPercentOne(
                tracker.items.MYTHOLOGICAL_DYE,
                tracker.mobs.TOTAL_MOBS
            )}%"
        )
    }

    fun sendPlayerStats(total: Boolean? = false) {
        val stats = getPlayerStats(total)
        val statsMessage = buildString {
            append("Pt:${stats.playtime}-") // Playtime
            append("Pr:${stats.profit[0]}/${stats.profit[2]}/h-") // Profit & Profit/h
            append("B:${stats.burrows}/${stats.burrowsPerHour}-") // Burrows & Burrows/h
            append("M:${stats.totalMobs}/${stats.mobsPerHour}-") // Mobs & Mobs/h

            // Only shows intresting stats to avoid cluttering the chat with 0s especially for players without the mythic griffin
            if (stats.inquisitors > 0) append("I:${stats.inquisitors}/${stats.inqPercentage}-") // Inquisitors & Inquisitor %
            if (stats.lsInqs > 0) append("LSI:${stats.lsInqs}-") // LS Inquisitors
            if (stats.sphinx > 0) append("S:${stats.sphinx}/${stats.sphinxPercentage}-") // Sphinx & Sphinx %
            if (stats.kings > 0) append("K:${stats.kings}/${stats.kingPercentage}-") // Kings & King %
            if (stats.mantis > 0) append("M:${stats.mantis}/${stats.mantisPercentage}-") // Mantis & Mantis %
            if (stats.chimeraDrops > 0) append("C${stats.chimeraDrops}/${stats.chimeraDropRate}-") // Chimeras & Chimera %
            if (stats.chimeraLSDrops > 0) append("LS: ${stats.chimeraLSDrops}/(${stats.chimeraLSDropRate})-") // LS Chimeras & LS Chimera %
            if (stats.sticksDropped > 0) append("S${stats.sticksDropped}/${stats.stickDropRate}-") // Sticks & Stick %
            if (stats.relicsDropped > 0) append("R${stats.relicsDropped}/${stats.relicDropRate}-") // Relics & Relic %
            if (stats.coresDropped > 0) append("C${stats.coresDropped}/${stats.coreDropRate}-") // Cores & Core %
            if (stats.woolsDropped > 0) append("W${stats.woolsDropped}/${stats.woolDropRate}-") // Wools & Wool %
            if (stats.dyesDropped > 0) append("D${stats.dyesDropped}/${stats.dyeDropRate}") // Dyes & Dye %
        }

        Chat.pc(statsMessage)
    }

    data class PlayerStats(
        val playtime: String,
        val profit: List<String>,
        val burrows: String,
        val burrowsPerHour: String,
        val totalMobs: Int,
        val mobsPerHour: String,
        val inquisitors: Int,
        val inqPercentage: String,
        val sphinx: Int,
        val sphinxPercentage: String,
        val mantis: Int,
        val mantisPercentage: String,
        val kings: Int,
        val kingPercentage: String,
        val lsInqs: Int,
        val chimeraDrops: Int,
        val chimeraDropRate: String,
        val chimeraLSDrops: Int,
        val chimeraLSDropRate: String,
        val sticksDropped: Int,
        val stickDropRate: String,
        val relicsDropped: Int,
        val relicDropRate: String,
        val coresDropped: Int,
        val coreDropRate: String,
        val woolsDropped: Int,
        val woolDropRate: String,
        val dyesDropped: Int,
        val dyeDropRate: String
    )
}
