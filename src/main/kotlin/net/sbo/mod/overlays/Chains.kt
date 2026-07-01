package net.sbo.mod.overlays

import net.minecraft.ChatFormatting
import net.minecraft.ChatFormatting.*
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.overlay.DirtyFlushableOverlay
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.utils.waypoint.WaypointManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.diana.guesses.ArrowGuessBurrow

object Chains : DirtyFlushableOverlay() {
    override val overlay = Overlay("Diana Chains", 10f, 10f).setCondition { Diana.ongoingChainsDisplay && (Helper.checkDiana() || Helper.hasSpade) }

    private var failureTimes = 0

    fun init() {
        overlay.init()
        updateLines()

        Register.onTick(20) {
            updateLines()
        }
    }

    private fun colorBasedOnAmount(amount: Int): ChatFormatting =
        when {
            amount >= 7 -> GREEN
            amount >= 5 -> GOLD
            amount >= 3 -> YELLOW
            amount >= 2 -> RED
            else -> DARK_RED
        }

    override fun generateLines(): List<OverlayTextLine> {
        val chains = BurrowDetector.chains
        val chainsColor = colorBasedOnAmount(chains)

        val knownW = WaypointManager.getWaypointsOfType("burrow").filter { !it.hidden && it.text != "Start" }.size
        val arrowW = WaypointManager.getWaypointsOfType("arrow").filter { !it.hidden && !it.inaccurateArrow }.size

        val waypoints = knownW + arrowW
        val waypointsColor = colorBasedOnAmount(waypoints)

        val arrows = ArrowGuessBurrow.allGuesses.size
        val known = BurrowDetector.burrows.filter { it.value.type != "Start" }.size

        val internalState = arrows + known
        val internalColor = colorBasedOnAmount(internalState)

        val waypointStateMatches = knownW == known && arrowW == arrows
        val chainMatches = chains == internalState

        val allMatches = chainMatches && waypointStateMatches

        val solverIssue = !allMatches && waypointStateMatches && chains > internalState
        val cleanupIssue = !allMatches && !solverIssue && waypointStateMatches

        val mismatch = !allMatches && !solverIssue && !cleanupIssue
        val issue = solverIssue || cleanupIssue || mismatch

        val status = when {
            allMatches ->
                "${GREEN}Fine"

            solverIssue ->
                "${YELLOW}More chains started than there are waypoints - possible issue with solver or waypoint addition"

            cleanupIssue ->
                "${GOLD}More waypoints than chains started - possible issue with waypoint cleanup"

            else ->
                "${DARK_RED}Internal state mismatch! Please report this!"
        }

        val lines = mutableListOf<OverlayTextLine>()

        lines.add(OverlayTextLine("$YELLOW${BOLD}Diana Chains"))

        // TODO: sometimes max is 8 due to close burrow detection, but it does not held up, 7 seems to be most common. maybe update when confirmed fully
        lines.add(OverlayTextLine("$GRAY - ${GREEN}Started: $chainsColor$chains/7-8"))
        lines.add(OverlayTextLine("$GRAY - ${AQUA}Waypoints: $waypointsColor$waypoints/7-8 ($knownW known, $arrowW arrow)"))

        // TODO: probably remove the issue and mismatch detection entirely at some point after we're fully sure the mod works all properly
        val debug = Debug.debugMessages
        if (issue || mismatch || debug) { // hide for less verbosity unless theres an issue or debug enabled
            failureTimes++

            // Only start showing for consistent issues lasting more than 4 seconds, otherwise this can flicker at times when a new burrow is being detected
            if (failureTimes >= 4 || debug) {
                if (issue || debug) {
                    lines.add(OverlayTextLine(""))
                    lines.add(OverlayTextLine("$GRAY - ${LIGHT_PURPLE}Status: $status"))
                }
                if (mismatch || debug) {
                    lines.add(OverlayTextLine("$GRAY - ${YELLOW}Internal State: $internalColor$internalState/7-8 ($known known, $arrows arrow)"))
                    lines.add(OverlayTextLine(""))
                    lines.add(OverlayTextLine("$GRAY - ${DARK_AQUA}Run /sbodebugburrows to add waypoints at internal state locations"))
                }
            }
        } else {
            failureTimes = 0
        }

        return lines
    }
}
