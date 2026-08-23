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
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.http.SboApi
import java.util.concurrent.TimeUnit

object PartyPlayer {
    var stats: PartyPlayerStats = PartyPlayerStats()
    private var lastUpdate: Long = 0
    private var lastCacheBypass: Long = 0
    private var refreshing: Boolean = false

    private val CACHE_BYPASS_COOLDOWN = TimeUnit.SECONDS.toNanos(20)

    private fun cacheBypassReadyIn(): Long =
        TimeUnit.NANOSECONDS.toSeconds((lastCacheBypass + CACHE_BYPASS_COOLDOWN - System.nanoTime()).coerceAtLeast(0))

    fun init() {
        Register.command("sboreloadstats") {
            val waitSeconds = cacheBypassReadyIn()
            if (waitSeconds > 0) {
                Chat.chat("§6[SBO] §cPlease wait ${waitSeconds}s before reloading stats again.")
                return@command
            }
            getPartyPlayerStats(
                forceRefresh = true,
                onError = { error ->
                    Chat.chat("§6[SBO] §cFailed to reload stats: ${error.message}")
                }
            ) { stats ->
                Chat.chat("§6[SBO] §aPlayer stats reloaded: ${stats.name} (SB Level: ${stats.sbLvl})")
            }
        }

        Register.onChatMessage(
            Regex("^Switching to profile (.*)$"),
            noFormatting = true
        ) { _, _ ->
            sleep(2000) {
                load()
            }
        }
    }

    fun load() {
        getPartyPlayerStats(forceRefresh = true) { stats ->
            logger.info("[SBO] Player stats loaded: ${stats.name} (SB Level: ${stats.sbLvl})")
        }
    }

    /**
     * @param onError Called instead of [callback] when the request fails. Without it a failure
     * falls back to the last known stats, which is what the callers that only read stats want.
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

        val bypassCache = forceRefresh && cacheBypassReadyIn() == 0L
        if (bypassCache) lastCacheBypass = System.nanoTime()

        val fail = { error: Exception ->
            refreshing = false
            logger.error("Failed to fetch party player stats: $error")
            if (onError != null) onError(error) else callback(stats)
        }

        refreshing = true
        SboApi.playerInfo(Player.getName() ?: "", readCache = !bypassCache)
            .toJson<PlayerInfoResponse>(ignoreUnknownKeys = true) { response ->
                refreshing = false
                if (response.success) {
                    lastUpdate = System.nanoTime()
                    stats = response.playerInfo ?: PartyPlayerStats()
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
