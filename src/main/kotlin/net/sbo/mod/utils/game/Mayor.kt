package net.sbo.mod.utils.game

import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.data.MayorResponse
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.http.Http

object Mayor {
    /** Early Spring 1st, Year 1 */
    private const val SKYBLOCK_EPOCH_MILLIS = 1560275700000L
    private const val DAY_MILLIS = 20 * 60 * 1000L
    private const val DAYS_IN_MONTH = 31
    private const val DAYS_IN_YEAR = DAYS_IN_MONTH * 12
    private const val ELECTION_DAY = 27
    private const val ELECTION_MONTH = 3
    /** Late Spring 27th as a 0 based day of the year, the new mayor is in office from this day on */
    private const val ELECTION_DAY_OF_YEAR = (ELECTION_MONTH - 1) * DAYS_IN_MONTH + ELECTION_DAY - 1

    private const val FALLBACK_MAYOR = "Diana"
    private const val FALLBACK_PERK = "Mythological Ritual"

    private var mayor: String? = null
    private var perks: MutableSet<String> = mutableSetOf()
    private var mayorApiError: Boolean = false
    private var minister: String? = null
    private var ministerPerk: String? = null
    private var refreshingMayor: Boolean = false
    private var outDatedApi: Boolean = false
    private var fetchedForYear = 0

    /** The SkyBlock year of the election the current mayor won, worked out from the server time on every read so it can't go stale. */
    val mayorElectedYear: Int
        get() = electedYearAt(Http.serverTimeMillis())

    fun init() {
        refreshMayorData()
        Register.onTick(20 * 60) { refreshMayorData() }
    }

    /** The year of the election whose mayor was in office at [millis]. Plain day counting, java.util.Calendar follows the locale (th_TH gives Buddhist years) and the time zone. */
    fun electedYearAt(millis: Long): Int {
        val day = Math.floorDiv(millis - SKYBLOCK_EPOCH_MILLIS, DAY_MILLIS)
        val year = Math.floorDiv(day, DAYS_IN_YEAR.toLong()).toInt() + 1
        val dayOfYear = Math.floorMod(day, DAYS_IN_YEAR.toLong()).toInt()
        return if (dayOfYear >= ELECTION_DAY_OF_YEAR) year else year - 1
    }

    private fun refreshMayorData() {
        val year = mayorElectedYear
        val newMayor = year != fetchedForYear
        if (mayor != null && !mayorApiError && !newMayor && !outDatedApi) return
        if (refreshingMayor) return

        fetchedForYear = year
        getMayor()
    }

    private fun getMayor() {
        mayor = null
        perks.clear()
        refreshingMayor = true

        Http.sendGetRequest("https://api.hypixel.net/resources/skyblock/election")
            .toJson<MayorResponse>(true) { response ->
                refreshingMayor = false
                if (!response.success) {
                    handleApiError(response.error ?: "Unknown error")
                    return@toJson
                }
                processMayorResponse(response)
            }
            .error { error ->
                handleApiError(error.message ?: "Request failed")
            }
    }

    private fun processMayorResponse(response: MayorResponse) {
        mayor = response.mayor.name
        perks = response.mayor.perks.map { it.name }.toMutableSet()
        minister = response.mayor.minister?.name
        ministerPerk = response.mayor.minister?.perk?.name
        mayorApiError = false

        // Hypixel caches the response, one from before the last election still has the old mayor
        outDatedApi = electedYearAt(response.lastUpdated) < mayorElectedYear
        if (outDatedApi) applyFallback()
    }

    private fun handleApiError(message: String) {
        refreshingMayor = false
        mayorApiError = true
        applyFallback()
        SBOKotlin.logger.error("API Mayor error: $message")
    }

    private fun applyFallback() {
        mayor = FALLBACK_MAYOR
        perks = mutableSetOf(FALLBACK_PERK)
    }
}
