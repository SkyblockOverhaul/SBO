package net.sbo.mod.utils.game

import net.sbo.mod.SBOKotlin
import net.minecraft.world.scores.*
import java.lang.String.CASE_INSENSITIVE_ORDER
import net.sbo.mod.utils.events.Register

object ScoreBoard {
    private val COMPARATOR: Comparator<PlayerScoreEntry> = Comparator.comparing { obj: PlayerScoreEntry -> obj.value() }
        .reversed()
        .thenComparing({ obj: PlayerScoreEntry -> obj.owner() }, CASE_INSENSITIVE_ORDER)

    private val FORMATTING_REGEX: Regex = "§[^a-f0-9]".toRegex()

    private var linesDirty = true
    private var titleDirty = true

    private var lastLines: List<String> = emptyList()
    private var lastTitle: String = "Unknown Scoreboard" // dummy

    init {
        Register.onTick(1) {
            linesDirty = true
            titleDirty = true
        }
    }

    /**
     * Retrieves the lines from the scoreboard sidebar.
     * It returns a list of formatted strings representing the scoreboard entries.
     * Each entry is stripped of any formatting codes.
     * @return A list of formatted strings representing the scoreboard entries.
     */
    fun getLines(): List<String> {
        if (linesDirty) {
            lastLines = fetchLines()
            linesDirty = false
        }

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
                ).string
            }
            .map { decoratedText ->
                decoratedText.replace(FORMATTING_REGEX, "")
            }
            .toList()
            .asReversed()
    }

    fun getTitle(): String {
        if (titleDirty) {
            lastTitle = fetchTitle()
            titleDirty = false
        }

        return lastTitle
    }

    private fun fetchTitle(): String {
        val scoreboard = SBOKotlin.mc.level?.scoreboard ?: return "Unknown Scoreboard"
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return "No Objective"

        return objective.displayName.string
    }
}
