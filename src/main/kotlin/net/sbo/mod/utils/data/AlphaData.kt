package net.sbo.mod.utils.data

import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.overlays.CrownOfAvarice
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.overlays.DianaMobs
import net.sbo.mod.overlays.DianaStats
import net.sbo.mod.overlays.MagicFind
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register

/**
 * Debug "Separate Alpha Data": on the Alpha Network the gameplay data (trackers, stats, achievements, past events)
 * switches to its own copy, see [DataManager.switchAlphaData], so testing there never changes the real data.
 */
object AlphaData {
    private var checkQueued = false

    /**
     * Called when the server environment or the setting changes: switches the data on the next tick (client thread),
     * with a one-time tick task, so nothing runs per tick while nothing changes.
     */
    fun check() {
        // Packets and disconnects can come from other threads, the tick tasks are client thread only
        mc.execute {
            if (checkQueued) return@execute
            checkQueued = true
            Register.onTick(1) { unregister ->
                unregister()
                checkQueued = false
                switchIfNeeded()
            }
        }
    }

    private fun switchIfNeeded() {
        val alpha = Debug.separateAlphaData && HypixelModApi.isAlpha
        if (alpha == DataManager.alphaActive) return

        SboTimerManager.beforeTrackerSwap()
        DataManager.switchAlphaData(alpha)
        SboTimerManager.afterTrackerSwap()

        DianaStats.updateLines()
        MagicFind.updateLines()
        DianaMobs.updateLines()
        DianaLoot.updateLines()
        CrownOfAvarice.updateLines()

        if (alpha) Chat.chat("§6[SBO] §eAlpha Network: using separate tracker data, your main data stays untouched.")
        else Chat.chat("§6[SBO] §eSwitched back to your main tracker data.")
    }
}
