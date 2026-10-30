package net.sbo.mod.overlays

import net.minecraft.ChatFormatting
import net.minecraft.ChatFormatting.*
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.div
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.diana.guesses.ArrowGuessBurrow
import net.sbo.mod.settings.categories.Burrows
import net.sbo.mod.settings.categories.Trackers
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.overlay.CHAT_SCREEN_FILTER
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.sbo.mod.utils.waypoint.WaypointManager

object Chains : DianaOverlay<Chains.View>("Diana Chains", Trackers.TrackerLook.CHAINS, { Burrows.ongoingChainsDisplay },
    allowedScreens = listOf(CHAT_SCREEN_FILTER)
) {
    private var failureTimes = 0

    fun init() {
        initOverlays()
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

    private enum class Status(val color: ChatFormatting, val text: String) {
        FINE(GREEN, "Fine"),
        SOLVER(YELLOW, "More chains started than there are waypoints - possible issue with solver or waypoint addition"),
        CLEANUP(GOLD, "More waypoints than chains started - possible issue with waypoint cleanup"),
        MISMATCH(DARK_RED, "Internal state mismatch!")
    }

    /** What both looks show. Counts how long an issue lasts, so it is read once per update. */
    private class State(
        val chains: Int,
        val knownW: Int,
        val knownWStart: Int,
        val arrowW: Int,
        val arrows: Int,
        val known: Int,
        val status: Status,
        // Status and internal state lines, only for consistent issues or in debug mode
        val showStatus: Boolean,
        val showInternal: Boolean
    ) {
        val waypoints get() = knownW + arrowW
        val internalState get() = arrows + known
    }

    private fun state(): State {
        val chains = BurrowDetector.chains

        val burrowWayps = WaypointManager.getWaypointsOfType("burrow")

        val knownW = burrowWayps.filter { !it.hidden && it.text != "Start" }.size
        val knownWStart = burrowWayps.filter { it.text == "Start" }.size

        val arrowW = WaypointManager.getWaypointsOfType("arrow").filter { !it.hidden }.size

        val arrows = ArrowGuessBurrow.allGuesses.size
        val known = BurrowDetector.burrows.filter { it.value.type != "Start" }.size

        val internalState = arrows + known

        val waypointStateMatches = knownW == known && arrowW == arrows
        val chainMatches = chains == internalState

        val allMatches = chainMatches && waypointStateMatches

        val solverIssue = !allMatches && waypointStateMatches && chains > internalState
        val cleanupIssue = !allMatches && !solverIssue && waypointStateMatches

        val mismatch = !allMatches && !solverIssue && !cleanupIssue
        val issue = solverIssue || cleanupIssue || mismatch

        val status = when {
            allMatches -> Status.FINE
            solverIssue -> Status.SOLVER
            cleanupIssue -> Status.CLEANUP
            else -> Status.MISMATCH
        }

        val debug = Debug.debugOnlyMessages
        var showStatus = false
        // TODO: probably remove the issue and mismatch detection entirely at some point after we're fully sure the mod works all properly
        if (!Burrows.simpleChainsOverlayMode) {
            if (issue || debug) { // hide for less verbosity unless theres an issue
                failureTimes++

                // Only start showing for consistent issues lasting more than 4 seconds, otherwise this can flicker at times when a new burrow is being detected
                showStatus = failureTimes >= 4 || debug
            } else {
                failureTimes = 0
            }
        }

        return State(chains, knownW, knownWStart, arrowW, arrows, known, status, showStatus, showStatus && (mismatch || debug))
    }

    data class View(val rows: List<Row>, val warnings: List<String>)

    override fun view(): View {
        val state = state()
        val rows = mutableListOf<Row>()
        val warnings = mutableListOf<String>()
        if (!Burrows.simpleChainsOverlayMode) {
            rows += Row(null, "Started", GREEN, "${state.chains}/${state.chains.coerceAtLeast(7)}", "${state.knownWStart} pending start", valueClass = colorClass(colorBasedOnAmount(state.chains)))
            rows += Row(null, "Waypoints", AQUA, "${state.waypoints}/${state.waypoints.coerceAtLeast(7)}", "${state.knownW} known, ${state.arrowW} arrow", valueClass = colorClass(colorBasedOnAmount(state.waypoints)))
            if (state.showStatus) {
                // Only the fine status is short enough for a row, the issues wrap below it
                rows += Row(null, "Status", LIGHT_PURPLE, if (state.status == Status.FINE) state.status.text else "Issue", valueClass = colorClass(state.status.color))
                if (state.status != Status.FINE) warnings += state.status.text
            }
            if (state.showInternal) {
                rows += Row(null, "Internal State", YELLOW, "${state.internalState}/7-8", "${state.known} known, ${state.arrows} arrow", valueClass = colorClass(colorBasedOnAmount(state.internalState)))
                warnings += "Run /sbodebugburrows to add waypoints at internal state locations that don't have one"
            }
        } else {
            // don't confuse user with known/arrow and mismatches etc., simple overlay mode
            if (state.knownWStart >= 1) {
                rows += Row(null, "Pending Start", GREEN, "${state.knownWStart}", valueClass = colorClass(colorBasedOnAmount(state.knownWStart)))
            }
            rows += Row(null, "Active", AQUA, "${state.waypoints}", valueClass = colorClass(colorBasedOnAmount(state.waypoints)))
        }
        return View(rows, warnings)
    }

    override fun NodeBuilder.content(view: View) {
        tracker("chains") {
            head("Diana Chains")
            view.rows.forEach { row(it) }
            view.warnings.forEach { div(className = "tracker-warn") { +it } }
        }
    }

    override fun generateLines(): List<OverlayTextLine> {
        val state = state()
        val chains = state.chains
        val chainsColor = colorBasedOnAmount(chains)
        val waypoints = state.waypoints
        val waypointsColor = colorBasedOnAmount(waypoints)
        val internalColor = colorBasedOnAmount(state.internalState)

        val lines = mutableListOf<OverlayTextLine>()

        lines.add(OverlayTextLine("$YELLOW${BOLD}Diana Chains"))

        val chainsMax = chains.coerceAtLeast(7)
        val waypointsMax = waypoints.coerceAtLeast(7)

        if (!Burrows.simpleChainsOverlayMode) {
            lines.add(OverlayTextLine("$GRAY - ${GREEN}Started: $chainsColor$chains/$chainsMax (${state.knownWStart} pending start)"))
            lines.add(OverlayTextLine("$GRAY - ${AQUA}Waypoints: $waypointsColor$waypoints/$waypointsMax (${state.knownW} known, ${state.arrowW} arrow)"))

            if (state.showStatus) {
                lines.add(OverlayTextLine(""))
                lines.add(OverlayTextLine("$GRAY - ${LIGHT_PURPLE}Status: ${state.status.color}${state.status.text}"))
                if (state.showInternal) {
                    lines.add(OverlayTextLine("$GRAY - ${YELLOW}Internal State: $internalColor${state.internalState}/7-8 (${state.known} known, ${state.arrows} arrow)"))
                    lines.add(OverlayTextLine(""))
                    lines.add(OverlayTextLine("$GRAY - ${DARK_AQUA}Run /sbodebugburrows to add waypoints at internal state locations that don't have one"))
                }
            }
        } else {
            // don't confuse user with known/arrow and mismatches etc., simple overlay mode
            if (state.knownWStart >= 1) {
                lines.add(OverlayTextLine("$GRAY - ${GREEN}Pending Start: ${colorBasedOnAmount(state.knownWStart)}${state.knownWStart}"))
            }
            lines.add(OverlayTextLine("$GRAY - ${AQUA}Active: $waypointsColor$waypoints"))
        }

        return lines
    }
}
