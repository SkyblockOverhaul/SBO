package net.sbo.mod.diana.guesses

import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.waypoint.WaypointManager
import net.sbo.mod.diana.burrows.BurrowDetector

class GuessEntry(val guesses: List<SboVec>) {
    private var currentIndex = 0

    fun getCurrent(): SboVec = guesses[currentIndex]

    fun contains(vec: SboVec): Boolean = guesses.contains(vec)

    fun getRemaining(): List<SboVec> = guesses.subList(currentIndex + 1, guesses.size)

    fun isEquivalentTo(other: GuessEntry): Boolean = guesses.subList(currentIndex, guesses.size) == other.guesses.subList(other.currentIndex, other.guesses.size)

    fun removeAllWaypoints() {
        WaypointManager.removeWaypointAt(getCurrent(), "arrow")

        getRemaining().forEach {
            WaypointManager.removeWaypointAt(it, "subGuess")
        }
    }

    fun moveToNext(): Boolean {
        val current = getCurrent()
        val currentWaypoint = WaypointManager.getWaypointAt(current, "arrow")

        if (currentWaypoint != null) {
            if (currentWaypoint.userInteractedWith) {
                // If the user interacted with the waypoint, we queue it to be removed when we get the "You dug out a Griffin Burrow!" chat message (dug 2 times).
                BurrowDetector.queueRemoval(currentWaypoint) { currentWaypoint.timesDug != 1 }
            } else {
                // Otherwise, we directly remove it (most likely a waypoint that was in a wrong position, so moveToNext was called before player can do any interaction, since this is checked every tick)
                WaypointManager.removeWaypoint(currentWaypoint)
            }
        }

        val nextIndex = currentIndex + 1

        if (nextIndex in guesses.indices) {
            val next = guesses[nextIndex]
            currentIndex = nextIndex

            WaypointManager.removeArrowSubGuess(next)
            WaypointManager.addArrowGuess(next)

            return true
        }

        return false
    }
}
