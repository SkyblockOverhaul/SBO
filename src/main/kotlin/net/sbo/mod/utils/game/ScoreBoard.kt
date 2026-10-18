package net.sbo.mod.utils.game

import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerScoreEntry
import net.minecraft.world.scores.PlayerTeam
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.Helper.removeFormatting
import java.lang.String.CASE_INSENSITIVE_ORDER

object ScoreBoard {
    private val COMPARATOR: Comparator<PlayerScoreEntry> = Comparator.comparing { obj: PlayerScoreEntry -> obj.value() }
        .reversed()
        .thenComparing({ obj: PlayerScoreEntry -> obj.owner() }, CASE_INSENSITIVE_ORDER)

    private var linesDirty = true
    private var titleDirty = true

    private var lastLines: List<String> = emptyList()
    private var lastTitle: String = "Unknown Scoreboard" // dummy

    init {
        // Packet bursts only set dirty flags; rebuild each cache at most once per tick.
        Register.onTick(1) {
            if (linesDirty) {
                linesDirty = false
                lastLines = fetchLines()
            }
            if (titleDirty) {
                titleDirty = false
                lastTitle = fetchTitle()
            }
        }
    }

    fun markLinesDirty() {
        linesDirty = true
    }

    fun markTitleDirty() {
        titleDirty = true
    }

    fun markDirty() {
        markLinesDirty()
        markTitleDirty()
    }

    /**
     * Retrieves the lines from the scoreboard sidebar.
     * It returns a list of formatted strings representing the scoreboard entries.
     * Each entry is stripped of any formatting codes.
     * @return A list of formatted strings representing the scoreboard entries.
     */
    fun getLines(): List<String> {
        return lastLines
    }

    private fun fetchLines(): List<String> {
        val scoreboard = SBOKotlin.mc.level?.scoreboard ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()

        return scoreboard.listPlayerScores(objective)
            .asSequence()
            .filter { entry -> !entry.isHidden }
            .sortedWith(COMPARATOR)
            .take(15)
            .map { entry ->
                PlayerTeam.formatNameForTeam(
                    scoreboard.getPlayersTeam(entry.owner()),
                    entry.ownerName()
                ).string.removeFormatting()
            }
            .toList()
            .asReversed()
    }

    fun getTitle(): String {
        return lastTitle
    }

    private fun fetchTitle(): String {
        val scoreboard = SBOKotlin.mc.level?.scoreboard ?: return "Unknown Scoreboard"
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return "No Objective"

        return objective.displayName.string
    }
}
