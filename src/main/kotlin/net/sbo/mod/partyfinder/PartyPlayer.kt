package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.diana.achievements.AchievementManager.trackWithCheckPlayer
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.partyfinder.api.PlayerStats
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.AchievementStats
import net.sbo.mod.utils.data.AchievementStatsResponse
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.DataManager.sboData
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.TabList
import net.sbo.mod.utils.http.SboApi
import java.util.concurrent.TimeUnit

object PartyPlayer {
    var stats: AchievementStats = AchievementStats()
    private var lastCacheBypass: Long = 0
    private var refreshing: Boolean = false

    private val CACHE_BYPASS_COOLDOWN = TimeUnit.SECONDS.toNanos(20)

    private fun currentProfile(): String? = TabList.findInfo("Profile: ")?.trim()?.takeIf { it.isNotEmpty() }

    private fun nameOutdated(info: AchievementStats?): Boolean {
        val returned = info?.name?.takeIf { it.isNotEmpty() } ?: return false
        val current = Player.getName()?.takeIf { it.isNotEmpty() } ?: return false
        return !current.equals(returned, ignoreCase = true)
    }

    private fun profileChanged(profile: String?): Boolean {
        val cached = sboData.lastStatsProfile.takeIf { it.isNotEmpty() } ?: return false
        if (profile == null) return false
        return !profile.equals(cached, ignoreCase = true)
    }

    private fun rememberProfile(profile: String?) {
        if (profile == null || profile.equals(sboData.lastStatsProfile, ignoreCase = true)) return
        sboData.lastStatsProfile = profile
        DataManager.save(DataManager::sboData)
    }

    private fun cacheBypassReadyIn(): Long =
        TimeUnit.NANOSECONDS.toSeconds((lastCacheBypass + CACHE_BYPASS_COOLDOWN - System.nanoTime()).coerceAtLeast(0))

    fun init() {
        Register.command("sboreloadstats") {
            reloadStats(onError = { Chat.chat("§6[SBO] §cCould not reload your stats: ${ProblemText.error(it)}") }) { stats ->
                val wait = reloadWait(stats)
                if (wait != null) Chat.chat("§6[SBO] §e$wait")
                else Chat.chat("§6[SBO] §aPlayer stats reloaded: ${stats.name} (SB Level: ${stats.general?.sbLevel ?: 0})")
            }
        }

        Register.onChatMessage(
            Regex("^Your profile was changed to: (\\S+)"),
            noFormatting = true
        ) { _, match ->
            val profile = match.groupValues[1]
            sleep(2000) {
                load(profile)
            }
        }
    }

    fun load(profile: String? = currentProfile()) {
        if (refreshing) return
        fetch(
            readCache = !(profileChanged(profile) && cacheBypassReadyIn() == 0L),
            profile = profile,
            onError = { error -> logger.warn("[SBO] Player stats not loaded: ${error.message}") }
        ) { stats ->
            logger.info("[SBO] Player stats loaded: ${stats.name} (SB Level: ${stats.sbLvl})")
        }
    }

    /** Loads all stats fresh from Hypixel API. Within 5 minutes of the last refresh the backend sends what it has cached. */
    fun reloadStats(onError: (PfError) -> Unit, onDone: (PlayerStats) -> Unit) {
        PlayerStatsCache.own(refresh = true, onError = onError) { stats ->
            load()
            onDone(stats)
        }
    }

    /** The message when the force reload is still on cooldown, null when it went through. */
    fun reloadWait(stats: PlayerStats): String? {
        val minutes = (System.currentTimeMillis() - stats.dataAt) / 60_000
        if (minutes < 1 || stats.refreshIn <= 0) return null
        val wait = if (stats.refreshIn >= 60) "${stats.refreshIn / 60}m ${stats.refreshIn % 60}s" else "${stats.refreshIn}s"
        return "Your stats are from $minutes min ago. SBO loads them from Hypixel at most every 5 minutes, try again in $wait."
    }

    private fun fetch(
        readCache: Boolean,
        profile: String?,
        onError: ((Exception) -> Unit)? = null,
        callback: (AchievementStats) -> Unit
    ) {
        val fail = { error: Exception ->
            refreshing = false
            logger.error("Failed to fetch party player stats: $error")
            if (onError != null) onError(error) else callback(stats)
        }

        val name = Player.getName()?.takeIf { it.isNotBlank() }
        if (name == null) {
            fail(Exception("No player name available"))
            return
        }

        if (!readCache) lastCacheBypass = System.nanoTime()
        refreshing = true
        SboApi.achievementStats(name, readCache = readCache)
            .toJson<AchievementStatsResponse>(ignoreUnknownKeys = true) { response ->
                refreshing = false
                if (response.success) {
                    if (readCache && nameOutdated(response.stats) && cacheBypassReadyIn() == 0L) {
                        fetch(readCache = false, profile = profile, onError = onError, callback = callback)
                        return@toJson
                    }
                    stats = response.stats ?: AchievementStats()
                    rememberProfile(profile)
                    if (stats.sbLvl == -1) {
                        Chat.chat("§6[SBO] §cYour stats are not available, please try again later.")
                    } else {
                        trackWithCheckPlayer(stats)
                    }
                    callback(stats)
                } else {
                    fail(Exception(response.error ?: "Unknown error"))
                }
            }
            .error { error -> fail(error) }
    }
}
