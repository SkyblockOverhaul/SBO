package net.sbo.mod.utils.game

import net.sbo.mod.SBOKotlin
import net.minecraft.world.scores.*
import java.lang.String.CASE_INSENSITIVE_ORDER

object ScoreBoard {
    private val COMPARATOR: Comparator<PlayerScoreEntry> = Comparator.comparing { obj: PlayerScoreEntry -> obj.value() }
        .reversed()
        .thenComparing({ obj: PlayerScoreEntry -> obj.owner() }, CASE_INSENSITIVE_ORDER)

    /**
     * Retrieves the lines from the scoreboard sidebar.
     * It returns a list of formatted strings representing the scoreboard entries.
     * Each entry is stripped of any formatting codes.
     * @return A list of formatted strings representing the scoreboard entries.
     */
    fun getLines(): List<String> {
        val scoreboard = SBOKotlin.mc.level?.getScoreboard() ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()

        return scoreboard.listPlayerScores(objective)
            .filter { entry -> !entry.isHidden }
            .sortedWith(COMPARATOR)
            .take(15)
            .map { entry ->
                PlayerTeam.formatNameForTeam(
                    scoreboard.getPlayersTeam(entry.owner()),
                    entry.ownerName()
                ).string
            }
            .map { decoratedText ->
                decoratedText.replace("§[^a-f0-9]".toRegex(), "")
            }
            .asReversed()
    }

    fun getTitle(): String {
        val scoreboard = SBOKotlin.mc.level?.getScoreboard() ?: return "Unknown Scoreboard"
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return "No Objective"
        return objective.displayName.string
    }
}
