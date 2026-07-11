package net.sbo.mod.utils.game

import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.data.MayorResponse
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.http.Http
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import kotlin.math.floor
import kotlin.math.round

object Mayor {
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
        Register.onTick(20 * 60) {
            refreshMayorData()
        }
    }

    private fun refreshMayorData() {
        skyblockDateString = calcSkyblockDate(System.currentTimeMillis())
        if (skyblockDateString.isNotEmpty()) {
            skyblockDate = convertStringToDate(skyblockDateString)
            if (newMayorAtDate == null || newMayorAtDate!!.time < skyblockDate!!.time) {
                newMayor = true
                val calendar = Calendar.getInstance()
                calendar.time = skyblockDate!!
                val currentYear = calendar.get(Calendar.YEAR)
                val compareDate = convertStringToDate("27.3.$currentYear")
                if (compareDate.time > skyblockDate!!.time) {
                    mayorElectedYear = currentYear - 1
                    dateMayorElected = convertStringToDate("27.3.${currentYear - 1}")
                    newMayorAtDate = convertStringToDate("27.3.$currentYear")
                } else {
                    mayorElectedYear = currentYear
                    dateMayorElected = convertStringToDate("27.3.$currentYear")
                    newMayorAtDate = convertStringToDate("27.3.${currentYear + 1}")
                }
            }
            val calendar = Calendar.getInstance()
            calendar.time = skyblockDate!!
            sbYear = calendar.get(Calendar.YEAR)
        }

        if (skyblockDate != null) {
            if ((mayor === null || mayorApiError || newMayor || outDatedApi) && !refreshingMayor) {
                getMayor()
                newMayor = false
            }
        }
    }

    internal fun getMayor() {
        mayor = null
        perks.clear()
        refreshingMayor = true

        Http.sendGetRequest("https://api.hypixel.net/resources/skyblock/election")
            .toJson<MayorResponse>(true) { response ->
                refreshingMayor = false
                if (response.success) {
                    apiLastUpdated = response.lastUpdated
                    mayor = response.mayor.name
                    perks = response.mayor.perks.map { it.name }.toMutableSet()
                    minister = response.mayor.minister?.name
                    ministerPerk = response.mayor.minister?.perk?.name
                    mayorApiError = false
                    apiLastUpdated?.let { apiTimeStamp ->
                        val apiDate = convertStringToDate(calcSkyblockDate(apiTimeStamp))
                        if (dateMayorElected == null || apiDate.time >= dateMayorElected!!.time) {
                            outDatedApi = false
                        } else {
                            outDatedApi = true
                            mayor = "Diana"
                            perks = mutableSetOf("Mythological Ritual")
                        }
                    }
                } else {
                    val errorMessage = response.error ?: "Unknown error"
                    SBOKotlin.logger.error("API error: $errorMessage")
                    mayorApiError = true
                    mayor = "Diana"
                    perks = mutableSetOf("Mythological Ritual")
                }
            }
            .error { error ->
                mayorApiError = true
                mayor = "Diana"
                perks = mutableSetOf("Mythological Ritual")
                SBOKotlin.logger.error("Error getting mayor from API: ${error.message}")
                refreshingMayor = false
            }
    }

    // ... (convertStringToDate und calcSkyblockDate bleiben unverändert)
    private fun convertStringToDate(string: String): Date {
        val parts = string.split(".")
        val day = parts[0].toInt()
        val month = parts[1].toInt() - 1
        val year = parts[2].toInt()
        return GregorianCalendar(year, month, day).time
    }

    private fun calcSkyblockDate(date: Long): String {
        val monthsInYear = 12
        val secondsPerMinute = 0.8333333333333334
        val secondsPerMonth = 37200.0

        val unix = floor(date.toDouble() / 1000).toLong()
        var secondsSinceLastLog = unix - 1560276000L

        var year = 1
        var month = 1
        var day = 1
        var hour = 6
        var minute = 0

        val secondsPerYear = secondsPerMonth * monthsInYear
        val secondsPerDay = 1200.0
        val secondsPerHour = 50.0

        val yearDiff = floor(secondsSinceLastLog / secondsPerYear).toInt()
        secondsSinceLastLog -= yearDiff * secondsPerYear.toLong()
        year += yearDiff

        val monthDiff = floor(secondsSinceLastLog / secondsPerMonth).toInt() % 13
        secondsSinceLastLog -= monthDiff * secondsPerMonth.toLong()
        month = (month + monthDiff) % 13
        if (month == 0) month = 13

        val dayDiff = floor(secondsSinceLastLog / secondsPerDay).toInt() % 32
        secondsSinceLastLog -= dayDiff * secondsPerDay.toLong()
        day = (day + dayDiff) % 32
        if (day == 0) day = 32

        val hourDiff = floor(secondsSinceLastLog / secondsPerHour).toInt() % 24
        secondsSinceLastLog -= hourDiff * secondsPerHour.toLong()
        hour = (hour + hourDiff) % 24

        if (hour < 6) {
            if (day < 31) {
                day += 1
            } else {
                day = 1
                month += 1
            }
        }

        val minuteDiff = floor(secondsSinceLastLog / secondsPerMinute).toInt() % 60
        secondsSinceLastLog -= minuteDiff * secondsPerMinute.toLong()
        minute = (minute + minuteDiff) % 60

        minute = (round(minute / 5.0) * 5).toInt() % 60

        return "$day.$month.$year"
    }
}