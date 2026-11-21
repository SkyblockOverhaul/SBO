package net.sbo.mod.diana.tracker

import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.overlays.DianaMobs
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.DataManager.dianaTrackerMayorData
import net.sbo.mod.utils.data.DataManager.pastDianaEventsData
import net.sbo.mod.utils.game.Mayor

/** Starts a new mayor tracker when an election ends (Late Spring 27th), the old one goes to the past events. */
object MayorReset {
    /** Fix up event year if it was left as default year somehow. */
    private fun fixYear() {
        if (dianaTrackerMayorData.year == 0) {
            dianaTrackerMayorData.year = Mayor.mayorElectedYear
            DataManager.save(DataManager::dianaTrackerMayorData)
        }
    }

    /** The election year of the last tracked drop, it can't get stuck like a stored year (clock was ahead once, old locale bug). */
    private fun trackedYear(): Int? = dianaTrackerMayorData.lastTrackedAt.takeIf { it > 0 }?.let(Mayor::electedYearAt)

    fun check() {
        fixYear()
        val current = Mayor.mayorElectedYear
        val tracked = trackedYear()
        if (tracked == null) {
            // Files from before lastTrackedAt: any other stored year resets, a year ahead would block every reset until it is reached
            if (dianaTrackerMayorData.year != current) reset()
            return
        }
        // A later year than now means the time is off right now, the next drop stamps the right time again
        if (tracked > current) return
        if (tracked < current) {
            reset()
            return
        }
        if (dianaTrackerMayorData.year != current) {
            dianaTrackerMayorData.year = current
            DataManager.save(DataManager::dianaTrackerMayorData)
        }
    }

    internal fun reset(notifyEvenIfAllZero: Boolean = false) {
        // Also called from /sboresetmayortracker, so not only through check
        fixYear()

        val allZero = Counters.isEmpty(dianaTrackerMayorData)
        if (!allZero) {
            // Past events go by the year of their last drop, the stored one can be wrong
            trackedYear()?.let { dianaTrackerMayorData.year = it }
            // If any of the mob or item counters are non-zero, save this event to past event data.
            pastDianaEventsData.events += dianaTrackerMayorData.snapshot()
            DataManager.save(DataManager::pastDianaEventsData)
        }

        dianaTrackerMayorData.reset()
        dianaTrackerMayorData.year = Mayor.mayorElectedYear
        dianaTrackerMayorData.resetCount += 1
        DataManager.save(DataManager::dianaTrackerMayorData)

        SboTimerManager.timerMayor.reset()
        SboTimerManager.timerCoaMayor.reset()
        SboTimerManager.pauseAllActiveTimers()

        DianaMobs.updateLines()
        DianaLoot.updateLines()

        // Notify the user only if the event-tracker had some non-zero data, or if notifyEvenIfAllZero was passed as true (called from /sboresetmayortracker).
        if (!allZero || notifyEvenIfAllZero) {
            Chat.chat("§6[SBO] §aDiana mayor tracker has been reset.")
        }
    }
}
