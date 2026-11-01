package net.sbo.mod.diana.tracker

import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.overlays.DianaMobs
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.DataManager.dianaTrackerMayorData
import net.sbo.mod.utils.data.DataManager.pastDianaEventsData
import net.sbo.mod.utils.game.Mayor

/** Starts a new mayor tracker when a new Diana got elected, the old one goes to the past events. */
object MayorReset {
    /** Fix up event year if it was left as default year somehow. */
    private fun fixYear() {
        if (dianaTrackerMayorData.year == 0) {
            dianaTrackerMayorData.year = Mayor.mayorElectedYear
            DataManager.save(DataManager::dianaTrackerMayorData)
        }
    }

    fun check() {
        fixYear()
        // Only reset if Skyblock Year is higher than the current event's year.
        if (dianaTrackerMayorData.year >= Mayor.mayorElectedYear) return
        reset()
    }

    internal fun reset(notifyEvenIfAllZero: Boolean = false) {
        // Also called from /sboresetmayortracker, so not only through check
        fixYear()

        val allZero = Counters.isEmpty(dianaTrackerMayorData)
        if (!allZero) {
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
