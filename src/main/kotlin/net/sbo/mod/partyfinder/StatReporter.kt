package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.partyfinder.api.BphReport
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.partyfinder.api.StatsReportBody
import net.sbo.mod.utils.SboKey
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.Mayor
import kotlin.random.Random

/** Sends stats only the mod can measure to the backend. For now the totals of the Diana mayor tracker. */
object StatReporter {
    private const val INTERVAL_TICKS = 20 * 60 * 15
    private const val STARTUP_DELAY_TICKS = 20 * 30
    // Spreads the reports, so players who start together don't all report together
    private const val MAX_JITTER_TICKS = 20 * 60

    @Volatile
    private var lastSent: BphReport? = null
    @Volatile
    private var outdatedShown = false

    internal fun init() {
        // Once shortly after the start, so short sessions report too
        Register.onTick(STARTUP_DELAY_TICKS + Random.nextInt(MAX_JITTER_TICKS)) { unregister ->
            unregister()
            report()
        }
        Register.onTick(INTERVAL_TICKS + Random.nextInt(MAX_JITTER_TICKS)) { report() }
    }

    internal fun report() {
        // Burrows on the Alpha Network don't count
        if (!SboKey.get().startsWith("sbo") || Mayor.mayorElectedYear == 0 || DataManager.alphaActive) return
        val tracker = DataManager.dianaTrackerMayorData
        // Until the first burrow after an election the tracker still holds the last event, which was already reported
        if (tracker.year != Mayor.mayorElectedYear) return
        val playtimeMs = SboTimerManager.timerMayor.getElapsedMillis()
        if (tracker.items.TOTAL_BURROWS <= 0 && playtimeMs <= 0) return
        val mods = BphGuard.foreignMods()
        val next = BphReport(
            burrows = tracker.items.TOTAL_BURROWS.toLong(),
            playtimeMs = playtimeMs,
            mayorYear = tracker.year,
            resetCount = tracker.resetCount,
            modified = mods.isNotEmpty(),
            edited = tracker.edited,
            mods = mods,
        )
        if (next == lastSent) return
        PartyFinderApi.reportStats(StatsReportBody(next), onError = { error ->
            if (error.code == PfError.MOD_OUTDATED) showOutdated()
            else logger.warn("[SBO] Burrows per hour not reported: ${error.code} ${error.message}")
        }) { lastSent = next }
    }

    private fun showOutdated() {
        if (outdatedShown) return
        outdatedShown = true
        Chat.chat("§6[SBO] §eYour burrows per hour no longer count for party finder requirements with this SBO version. Update SBO to keep them counting.")
    }
}
