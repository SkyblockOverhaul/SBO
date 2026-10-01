package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.partyfinder.api.BphReport
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.StatsReportBody
import net.sbo.mod.utils.SboKey
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.Mayor
import java.math.BigDecimal
import java.math.RoundingMode

/** Sends stats only the mod can measure to the backend. For now burrows per hour from the Diana mayor tracker. */
object StatReporter {
    private const val INTERVAL_TICKS = 20 * 60 * 15
    private const val MIN_HOURS = 1.0
    private const val MAX_BPH = 1500.0

    private var lastSent: BphReport? = null

    fun init() {
        Register.onTick(INTERVAL_TICKS) { report() }
    }

    fun report() {
        if (!SboKey.get().startsWith("sbo") || Mayor.mayorElectedYear == 0) return
        val tracker = DataManager.dianaTrackerMayorData
        // The mayor tracker resets when Diana is elected again, until then it holds the last event
        val current = tracker.year != 0 && tracker.year >= Mayor.mayorElectedYear
        val next = bphReport(tracker.items.TOTAL_BURROWS.toLong(), SboTimerManager.timerMayor.getHourTime(), current) ?: return
        if (next == lastSent) return
        PartyFinderApi.reportStats(
            StatsReportBody(next),
            onError = { logger.warn("[SBO] Burrows per hour not reported: ${it.code} ${it.message}") }
        ) { lastSent = next }
    }

    /** Null when there is nothing worth sending: less than an hour, no burrows or an impossible value. */
    fun bphReport(burrows: Long, hours: Double, current: Boolean): BphReport? {
        if (hours < MIN_HOURS || burrows <= 0) return null
        val value = BigDecimal.valueOf(burrows / hours).setScale(2, RoundingMode.HALF_UP).toDouble()
        if (value > MAX_BPH) return null
        return BphReport(value, if (current) "current" else "lastEvent", hours, burrows)
    }
}
