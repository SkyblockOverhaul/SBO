package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin.API_URL
import net.sbo.mod.diana.achievements.AchievementManager.trackWithCheckPlayer
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.PartyInfo
import net.sbo.mod.utils.data.PartyPlayerStats
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.http.Http

object PartyPlayer {
    var stats: PartyPlayerStats = PartyPlayerStats()
    private var lastUpdate: Long = 0
    private var cooldown: Long = 0
    private var refreshing: Boolean = false

    fun init() {
        Register.command("sboreloadstats") {
            if (System.currentTimeMillis() - cooldown < 2 * 60 * 1000) { // if cooldown is 2 min
                Chat.chat("§6[SBO] §cPlease wait before reloading stats again.")
                return@command
            } else {
                cooldown = System.currentTimeMillis()
                getPartyPlayerStats(true) { stats ->
                    Chat.chat("§6[SBO] §aPlayer stats reloaded: ${stats.name} (SB Level: ${stats.sbLvl})")
                }
            }
        }

        Register.onChatMessage(
            Regex("^Switching to profile (.*)$"),
            true
        ) { _, _ ->
            sleep(2000) {
                load()
            }
        }
    }

    fun load() {
        getPartyPlayerStats(true) { stats ->
            Chat.chat("§6[SBO] §aPlayer stats loaded: ${stats.name} (SB Level: ${stats.sbLvl})")
        }
    }

    fun getPartyPlayerStats(forceRefresh: Boolean = false, callback: (PartyPlayerStats) -> Unit) {
        if (forceRefresh || System.currentTimeMillis() - lastUpdate > 10 * 60 * 1000) { // 10 minutes
            if (refreshing) {
                callback(stats)
                return
            }
            refreshing = true
            Http.sendGetRequest("$API_URL/partyInfoByUuids?uuids=${Player.getUUIDString().replace("-", "")}&readcache=false")
                .toJson<PartyInfo>(true) { response ->
                    refreshing = false
                    if (response.success) {
                        lastUpdate = System.currentTimeMillis()
                        stats = response.partyInfo.firstOrNull() ?: PartyPlayerStats()
                        if (stats.sbLvl == -1) {
                            Chat.chat("§6[SBO] §cYour stats are not available, please try again later.")
                        } else {
                            trackWithCheckPlayer(stats)
                        }
                        callback(stats)
                    }
                }
                .error { error ->
                    refreshing = false
                    println("[SBO] Failed to fetch party player stats: $error")
                    callback(stats)
                }

        } else {
            callback(stats)
            return
        }
    }
}
