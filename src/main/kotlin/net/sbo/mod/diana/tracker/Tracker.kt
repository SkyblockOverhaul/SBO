package net.sbo.mod.diana.tracker

import net.sbo.mod.diana.RareMobKillTime
import net.sbo.mod.diana.achievements.AchievementManager
import net.sbo.mod.diana.achievements.AchievementManager.unlockAchievement
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.overlays.DianaMobs
import net.sbo.mod.overlays.DianaStats
import net.sbo.mod.overlays.MagicFind
import net.sbo.mod.settings.categories.Drops
import net.sbo.mod.settings.categories.General
import net.sbo.mod.settings.categories.Trackers
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.allowSackTracking
import net.sbo.mod.utils.Helper.dianaMobDiedRecently
import net.sbo.mod.utils.Helper.gotLootShareRecently
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.Item
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.DataManager.dianaTrackerMayorData
import net.sbo.mod.utils.data.DataManager.dianaTrackerSessionData
import net.sbo.mod.utils.data.DataManager.dianaTrackerTotalData
import net.sbo.mod.utils.data.DataManager.saveTrackerData
import net.sbo.mod.utils.data.DataManager.sboData
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.GameCloseEvent
import net.sbo.mod.utils.game.World.isInSkyblock
import net.sbo.mod.utils.http.Http
import net.sbo.mod.utils.time.TimeUtil
import java.util.regex.Pattern

/** The Diana tracker: reads the chat, pickup log and sacks and counts into the mayor, session and total tracker. */
object Tracker {
    private val otherDrops = listOf("ENCHANTED_ANCIENT_CLAW", "ANCIENT_CLAW", "ENCHANTED_GOLD")
    private val sackDrops = listOf("Enchanted Gold", "Ancient Claw", "Enchanted Ancient Claw")
    private val isMobOnCooldown: MutableMap<String, Boolean> = mutableMapOf()
    private val isItemOnCooldown: MutableMap<String, Boolean> = mutableMapOf()

    private var allowScavTracking: Boolean = true

    var lastSpawnedMob: String? = null
    var lastSpawnedMobTime: Long = 0L

    internal var lastBurrow: BURROW_TYPE = BURROW_TYPE.START
        private set

    // "Minos Hunter" intentionally skipped until its tracker entry exists.
    private val shardIds = mapOf(
        "King Minos" to "KING_MINOS_SHARD",
        "Sphinx" to "SPHINX_SHARD",
        "Minotaur" to "MINOTAUR_SHARD",
        "Cretan Bull" to "CRETAN_BULL_SHARD",
        "Harpy" to "HARPY_SHARD",
    )

    internal fun isItemOnCooldown(itemId: String) = isItemOnCooldown.getOrDefault(itemId, false)

    fun init() {
        Commands.register()

        // Only a compare of two years, the overlays switch to the new event right when the election ends instead of on the next drop
        Register.onTick(20) {
            if (isInSkyblock()) MayorReset.check()
        }

        Register.onChatMessageCancelable(
            Pattern.compile("^§6§lWow! §eYou dug out §6(.*?) coins§e!$")
        ) { _, matchResult ->
            lastBurrow = BURROW_TYPE.TREASURE
            allowScavTracking = false
            sleep(1000) {
                allowScavTracking = true
            }
            val coins = matchResult.group(1).replace(",", "").toIntOrNull() ?: 0
            if (coins > 0) trackItem("COINS", coins)
            true
        }

        Register.onChatMessageCancelable(Pattern.compile("(.*?) §efound a §cPhoenix §epet!(.*?)$", Pattern.DOTALL)) { _, matchResult ->
            val player = matchResult.group(1).removeFormatting().lowercase()
            if (!player.contains((Player.getName().orEmpty()).lowercase())) return@onChatMessageCancelable true
            if (General.phoenixAnnouncer) {
                Chat.chat("§6[SBO] §cGG §eFound a §cPhoenix §epet!")
                Helper.showTitle("§c§lPhoenix Pet!", "", 0, 25, 35)
            }
            sleep(1000) {
                if (isInSkyblock() && dianaMobDiedRecently(3)) unlockAchievement(77) // phoenix pet
            }
            true
        }

        Register.onChatMessageCancelable(Pattern.compile("^§eYou (.*?) Griffin [Bb]urrow(.*?)$", Pattern.DOTALL)) { _, matchResult ->
            if (matchResult.group(2).removeFormatting().contains("(1/")) lastBurrow = BURROW_TYPE.START // The first one is always Start
            trackItem("TOTAL_BURROWS", 1)
            if (Trackers.fourEyedFish) {
                val fishCoins = if (lastBurrow != BURROW_TYPE.START) 4000 else 2000
                trackItem("FISH_COINS", fishCoins)
                trackItem("COINS", fishCoins)
            }
            General.HiddenMessage.DIANA !in General.hideMessages
        }

        Register.onChatMessageCancelable(Pattern.compile("(.*?) §eYou dug (.*?)§2(.*?)§e!(.*?)$", Pattern.DOTALL)) { _, matchResult ->
            lastBurrow = BURROW_TYPE.MOB
            val mob = matchResult.group(3)
            if (!isMobOnCooldown.getOrDefault(mob, false)) trackMobOnSpawnAndSave(mob)
            General.HiddenMessage.DIANA !in General.hideMessages
        }

        Register.onChatMessageCancelable(Pattern.compile("^§6§lRARE DROP! §eYou dug out a (.*?)§e!$", Pattern.DOTALL)) { _, matchResult ->
            if (!(lastBurrow == BURROW_TYPE.MOB && lastSpawnedMob == "Sphinx")) lastBurrow = BURROW_TYPE.TREASURE
            when (val drop = matchResult.group(1).drop(2)) {
                "Griffin Feather", "Mythos Fragment" -> trackItem(drop, 1)
                "Braided Griffin Feather" -> RareDrops.onBraidedFeather()
            }
            true
        }

        Register.onChatMessageCancelable(Pattern.compile("^§6§lRARE DROP! (.*?)$", Pattern.DOTALL)) { _, matchResult ->
            RareDrops.onChatDrop(matchResult.group(1))
            true
        }

        Register.onChatMessageCancelable(
            Pattern.compile("^§d§lWOW! (.*?) §6found a §2Mythological Dye.*$", Pattern.DOTALL)
        ) { _, matchResult ->
            val player = matchResult.group(1).removeFormatting().lowercase().trim()
            if (player.contains(Player.getName()?.lowercase()?.trim().orEmpty())) RareDrops.onMythologicalDye()
            true
        }

        Register.onChatMessageCancelable(
            Pattern.compile(
                "^§b§lCHARM! §7You charmed the §c§2(.*?) §7and received §a(\\d+) §[0-9a-f](.*?) Shards?§7!$",
                Pattern.DOTALL
            )
        ) { _, matchResult ->
            trackShard(matchResult.group(3), matchResult.group(2))
            true
        }

        Register.onChatMessageCancelable(
            Pattern.compile(
                "^§e§lLOOT SHARE §fYou received §b(\\d+) §[0-9a-f](.*?) §fShards for assisting §b(.*?)§f!$",
                Pattern.DOTALL
            )
        ) { _, matchResult ->
            trackShard(matchResult.group(2), matchResult.group(1))
            true
        }

        Register.onChatMessage(Regex("^(.*?) §eYou just dug out(.*?)$")) { _, matchResult ->
            if (matchResult.groupValues[2].contains("Myth the Fish")) RareDrops.onMythTheFish()
        }
    }

    // Reset before DataManager's normal-priority close listener performs its blocking save.
    @SboEvent(SboEvent.Priority.HIGH)
    fun onGameClose(event: GameCloseEvent) {
        if (Trackers.resetSessionOnGameRestart) {
            DianaLoot.resetSession()
        }
    }

    fun trackWithPickuplog(item: Item) {
        val createdAt = item.creation
        val secondsPassedSinceCreation = Helper.getSecondsPassed(createdAt)

        if (secondsPassedSinceCreation <= 6) {
            trackWithPickuplog(item.itemId)
            return
        }

        if (Debug.debugOnlyMessages) {
            val timeStatus = when {
                TimeUtil.isSynchronized -> "NTP synchronized, uncertainty≈${TimeUtil.synchronizationUncertaintyMillis}ms"
                TimeUtil.synchronizationFailed -> "NTP synchronization FAILED"
                else -> "NTP synchronization pending"
            }

            Chat.chat(
                "SBO(debug): not tracking item with creation older than 6 seconds. " +
                    "secondsPassedSinceCreation=$secondsPassedSinceCreation," +
                    "createdAt=$createdAt," +
                    "time=$timeStatus"
            )
        }
    }

    private fun trackWithPickuplog(itemId: String) {
        when (itemId) {
            "HILT_OF_REVELATIONS" -> DropAnnouncer.onRareDrop("Hilt of Revelations", showMessageOrTitle = Drops.hiltDropMessage,
                trackLootshare = false,
                magicFind = 0,
                actuallyRare = false // makes it show the chat message but not the title
            )
            "CROWN_OF_GREED" -> DropAnnouncer.onRareDrop("Crown of Greed", showMessageOrTitle = true,
                trackLootshare = false,
                magicFind = 0
            )
        }
    }

    fun trackWithPickuplogStackable(item: Item, amount: Int) {
        sleep (1000) {
            if (!dianaMobDiedRecently(3) && !gotLootShareRecently(3)) return@sleep
            if (item.itemId in otherDrops) {
                trackItem(item.itemId, amount)
                return@sleep
            }
        }
    }

    fun trackWithSacksMessage(itemName: String, amount: Int) {
        if (!allowSackTracking) return
        if (!dianaMobDiedRecently(30)) return
        val item = itemName.replace("Ingot", "").trim()
        if (sackDrops.contains(item)) {
            trackItem(item, amount)
        }
    }

    fun trackScavengerCoins(amount: Long) {
        if (amount <= 0) return
        if (!dianaMobDiedRecently(4)) return
        if (amount > 150000 || !allowScavTracking) return
        trackItem("SCAVENGER_COINS", amount.toInt())
        trackItem("COINS", amount.toInt())
    }

    private fun trackShard(shard: String, amount: String) {
        shardIds[shard.removeFormatting()]?.let { trackItem(it, amount.toIntOrNull() ?: 0) }
    }

    fun trackMobOnSpawnAndSave(mob: String, fromCocoon: Boolean = false) {
        lastSpawnedMob = mob
        lastSpawnedMobTime = System.nanoTime()
        RareMobKillTime.onMobSpawn(mob, fromCocoon)
        RareMobs.countSpawn(mob)
        DataManager.save(DataManager::sboData)
    }

    internal fun trackMob(item: String, amount: Int) {
        if (isMobOnCooldown.getOrDefault(item, false)) return
        isMobOnCooldown[item] = true

        trackItem(item, amount)
        trackItem("TOTAL_MOBS", amount)
        RareMobs.countMob(amount)
        DataManager.save(DataManager::sboData)

        sleep(500) {
            isMobOnCooldown[item] = false
        }
    }

    fun trackItem(item: String, amount: Int, enforceCooldown: Boolean = true) {
        val itemId = item.toItemId()

        if (isItemOnCooldown(itemId) && enforceCooldown) return
        isItemOnCooldown[itemId] = true

        MayorReset.check()
        val itemName = Helper.toUpperSnakeCase(item)
        when (itemName) {
            "MINOS_INQUISITOR_LS" -> sboData.inqsSinceLsChim += 1
            "KING_MINOS_LS" -> sboData.kingSinceLsWool += 1
            "MANTICORE_LS" -> {
                sboData.mantiSinceLsCore += 1
                sboData.mantiSinceLsStinger += 1
            }
            "SPHINX_LS" -> sboData.sphinxSinceLsFood += 1
        }

        Counters.add(dianaTrackerMayorData, itemName, amount)
        dianaTrackerMayorData.lastTrackedAt = Http.serverTimeMillis()
        Counters.add(dianaTrackerSessionData, itemName, amount)
        Counters.add(dianaTrackerTotalData, itemName, amount)
        saveTrackerData()
        DianaStats.updateLines()
        MagicFind.updateLines()
        DianaMobs.updateLines()
        DianaLoot.updateLines()
        SboTimerManager.updateAllActivity()
        AchievementManager.trackAchievementsItem(dianaTrackerMayorData)
        AchievementManager.trackSince()

        sleep(500) {
            isItemOnCooldown[itemId] = false
        }
    }

    enum class BURROW_TYPE {
        START, MOB, TREASURE
    }
}
