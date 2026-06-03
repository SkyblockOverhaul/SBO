package net.sbo.mod.utils.game

import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.data.MayorResponse
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.http.Http
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import kotlin.math.floor

object Mayor {
    private const val SKYBLOCK_EPOCH = 1560276000L
    private const val SECONDS_PER_MINUTE = 0.8333333333333334
    private const val SECONDS_PER_MONTH = 37200.0
    private const val SECONDS_PER_DAY = 1200.0
    private const val SECONDS_PER_HOUR = 50.0
    private const val MONTHS_IN_YEAR = 12
    private const val DAYS_IN_MONTH = 31
    private const val ELECTION_DAY = 27
    private const val ELECTION_MONTH = 3

    private const val FALLBACK_MAYOR = "Diana"
    private const val FALLBACK_PERK = "Mythological Ritual"

    var dateMayorElected: Date? = null
    var newMayorAtDate: Date? = null
    var mayor: String? = null
    var perks: MutableSet<String> = mutableSetOf()
    var mayorApiError: Boolean = false
    var apiLastUpdated: Long? = null
    var minister: String? = null
    var ministerPerk: String? = null
    var skyblockDate: Date? = null
    var skyblockDateString: String = ""
    var refreshingMayor: Boolean = false
    var newMayor: Boolean = false
    var outDatedApi: Boolean = false
    var sbYear: Int = 0
    var mayorElectedYear = 0

    fun init() {
        refreshMayorData()
        Register.onTick(20 * 60) { refreshMayorData() }
    }

    private fun refreshMayorData() {
        skyblockDateString = calcSkyblockDate(System.currentTimeMillis())
        if (skyblockDateString.isEmpty()) return

        skyblockDate = convertStringToDate(skyblockDateString)
        updateMayorElection()
        updateSbYear()
        fetchMayorIfNeeded()
    }

    private fun updateMayorElection() {
        val newDate = newMayorAtDate ?: return
        if (newDate.time >= (skyblockDate?.time ?: return)) return

        newMayor = true
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val electionThisYear = date(ELECTION_DAY, ELECTION_MONTH, currentYear)

        if (electionThisYear.time > skyblockDate!!.time) {
            mayorElectedYear = currentYear - 1
            dateMayorElected = date(ELECTION_DAY, ELECTION_MONTH, currentYear - 1)
            newMayorAtDate = electionThisYear
        } else {
            mayorElectedYear = currentYear
            dateMayorElected = electionThisYear
            newMayorAtDate = date(ELECTION_DAY, ELECTION_MONTH, currentYear + 1)
        }
    }

    private fun updateSbYear() {
        Calendar.getInstance().apply {
            time = skyblockDate!!
            sbYear = get(Calendar.YEAR)
        }
    }

    private fun fetchMayorIfNeeded() {
        if (skyblockDate == null) return
        if (mayor != null && !mayorApiError && !newMayor && !outDatedApi) return
        if (refreshingMayor) return

        getMayor()
        newMayor = false
    }

    internal fun getMayor() {
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
        apiLastUpdated = response.lastUpdated
        mayor = response.mayor.name
        perks = response.mayor.perks.map { it.name }.toMutableSet()
        minister = response.mayor.minister?.name
        ministerPerk = response.mayor.minister?.perk?.name
        mayorApiError = false

        checkApiFreshness()
    }

    private fun checkApiFreshness() {
        val apiTimeStamp = apiLastUpdated ?: return
        val apiDate = convertStringToDate(calcSkyblockDate(apiTimeStamp))
        val electedDate = dateMayorElected

        outDatedApi = electedDate != null && apiDate.time < electedDate.time
        if (outDatedApi) applyFallback()
    }

    private fun handleApiError(message: String) {
        mayorApiError = true
        applyFallback()
        SBOKotlin.logger.error("API error: $message")
    }

    private fun applyFallback() {
        mayor = FALLBACK_MAYOR
        perks = mutableSetOf(FALLBACK_PERK)
    }

    private fun date(day: Int, month: Int, year: Int): Date =
        GregorianCalendar(year, month - 1, day).time

    private fun convertStringToDate(string: String): Date {
        val parts = string.split(".")
        return date(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }

    private fun calcSkyblockDate(date: Long): String {
        val unix = floor(date.toDouble() / 1000).toLong()
        var secondsSinceEpoch = unix - SKYBLOCK_EPOCH

        var year = 1
        var month = 1
        var day = 1
        var hour = 6

        val secondsPerYear = SECONDS_PER_MONTH * MONTHS_IN_YEAR

        val yearDiff = floor(secondsSinceEpoch / secondsPerYear).toInt()
        secondsSinceEpoch -= yearDiff * secondsPerYear.toLong()
        year += yearDiff

        val monthDiff = floor(secondsSinceEpoch / SECONDS_PER_MONTH).toInt() % 13
        secondsSinceEpoch -= monthDiff * SECONDS_PER_MONTH.toLong()
        month = (month + monthDiff).let { if (it == 0) 13 else it }

        val dayDiff = floor(secondsSinceEpoch / SECONDS_PER_DAY).toInt() % 32
        secondsSinceEpoch -= dayDiff * SECONDS_PER_DAY.toLong()
        day = (day + dayDiff).let { if (it == 0) DAYS_IN_MONTH else it }

        val hourDiff = floor(secondsSinceEpoch / SECONDS_PER_HOUR).toInt() % 24
        secondsSinceEpoch -= hourDiff * SECONDS_PER_HOUR.toLong()
        hour = (hour + hourDiff) % 24

        if (hour < 6) {
            day = if (day < DAYS_IN_MONTH) day + 1 else 1
            if (day == 1) month++
        }

        floor(secondsSinceEpoch / SECONDS_PER_MINUTE).toInt() % 60

        return "$day.$month.$year"
    }
}