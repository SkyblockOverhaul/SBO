package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.diana.achievements.AchievementManager.trackWithCheckPlayer
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
            reloadStats(onError = { message -> Chat.chat("§6[SBO] §c$message") }) { stats ->
                Chat.chat("§6[SBO] §aPlayer stats reloaded: ${stats.name} (SB Level: ${stats.sbLvl})")
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

    /**
     * Loads your stats fresh from Hypixel, used by /sboreloadstats and the party finder button.
     * This refreshes the cached stats in the backend and in the client.
     * @param onError Gets a message for the player when it is too soon or the request fails.
     */
    fun reloadStats(onError: (String) -> Unit, onDone: (AchievementStats) -> Unit) {
        val waitSeconds = cacheBypassReadyIn()
        if (waitSeconds > 0) {
            onError("Please wait ${waitSeconds}s before reloading your stats again.")
            return
        }
        val done = { stats: AchievementStats ->
            OwnStats.clear()
            onDone(stats)
        }
        if (refreshing) {
            done(stats)
            return
        }
        fetch(
            readCache = false,
            profile = currentProfile(),
            onError = { error -> onError("Could not reload your stats: ${error.message}") },
            callback = done
        )
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
