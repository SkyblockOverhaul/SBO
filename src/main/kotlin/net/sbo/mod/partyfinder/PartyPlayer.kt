package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.achievements.AchievementManager.trackWithCheckPlayer
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.PartyPlayerStats
import net.sbo.mod.utils.data.PlayerInfoResponse
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.data.SboDataObject.sboData
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.TabList
import net.sbo.mod.utils.http.SboApi
import java.util.concurrent.TimeUnit

object PartyPlayer {
    var stats: PartyPlayerStats = PartyPlayerStats()
    private var lastUpdate: Long = 0
    private var lastCacheBypass: Long = 0
    private var refreshing: Boolean = false

    private val CACHE_BYPASS_COOLDOWN = TimeUnit.SECONDS.toNanos(20)

    private fun currentProfile(): String? = TabList.findInfo("Profile: ")?.trim()?.takeIf { it.isNotEmpty() }

    /** True when the name the API returned is not the one we are playing on. */
    private fun nameOutdated(info: PartyPlayerStats?): Boolean {
        val returned = info?.name?.takeIf { it.isNotEmpty() } ?: return false
        val current = Player.getName()?.takeIf { it.isNotEmpty() } ?: return false
        return !current.equals(returned, ignoreCase = true)
    }

    /** True when [profile] is not the one the stored stats belong to. */
    private fun profileChanged(profile: String?): Boolean {
        val cached = sboData.lastStatsProfile.takeIf { it.isNotEmpty() } ?: return false
        if (profile == null) return false
        return !profile.equals(cached, ignoreCase = true)
    }

    private fun rememberProfile(profile: String?) {
        if (profile == null || profile.equals(sboData.lastStatsProfile, ignoreCase = true)) return
        sboData.lastStatsProfile = profile
        SboDataObject.save("SboData")
    }

    private fun cacheBypassReadyIn(): Long =
        TimeUnit.NANOSECONDS.toSeconds((lastCacheBypass + CACHE_BYPASS_COOLDOWN - System.nanoTime()).coerceAtLeast(0))

    fun init() {
        Register.command("sboreloadstats") {
            val waitSeconds = cacheBypassReadyIn()
            if (waitSeconds > 0) {
                Chat.chat("§6[SBO] §cPlease wait ${waitSeconds}s before reloading stats again.")
                return@command
            }
            reloadStats(
                onError = { error ->
                    Chat.chat("§6[SBO] §cFailed to reload stats: ${error.message}")
                }
            ) { stats ->
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
     * Refetches with readCache=false, for /sboreloadstats.
     * @param onError Called on failure instead of [callback], which otherwise gets the old stats.
     */
    fun reloadStats(
        onError: ((Exception) -> Unit)? = null,
        callback: (PartyPlayerStats) -> Unit
    ) {
        if (refreshing) {
            callback(stats)
            return
        }
        fetch(readCache = false, profile = currentProfile(), onError = onError, callback = callback)
    }

    /**
     * Reads through the API cache, unless the profile or the name changed.
     * @param forceRefresh Skips the 10 minute freshness check.
     * @param onError Called on failure instead of [callback], which otherwise gets the old stats.
     */
    fun getPartyPlayerStats(
        forceRefresh: Boolean = false,
        onError: ((Exception) -> Unit)? = null,
        callback: (PartyPlayerStats) -> Unit
    ) {
        if (!forceRefresh && System.nanoTime() - lastUpdate <= TimeUnit.MILLISECONDS.toNanos(10 * 60 * 1000)) { // 10 minutes
            callback(stats)
            return
        }
        if (refreshing) {
            callback(stats)
            return
        }

        val profile = currentProfile()
        fetch(!(profileChanged(profile) && cacheBypassReadyIn() == 0L), profile, onError, callback)
    }

    private fun fetch(
        readCache: Boolean,
        profile: String?,
        onError: ((Exception) -> Unit)? = null,
        callback: (PartyPlayerStats) -> Unit
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
        SboApi.playerInfo(name, readCache = readCache)
            .toJson<PlayerInfoResponse>(ignoreUnknownKeys = true) { response ->
                refreshing = false
                if (response.success) {
                    if (readCache && nameOutdated(response.playerInfo) && cacheBypassReadyIn() == 0L) {
                        fetch(readCache = false, profile = profile, onError = onError, callback = callback)
                        return@toJson
                    }
                    lastUpdate = System.nanoTime()
                    stats = response.playerInfo ?: PartyPlayerStats()
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
