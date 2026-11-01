package net.sbo.mod.diana.tracker

import net.sbo.mod.diana.achievements.AchievementManager.trackMagicFind
import net.sbo.mod.diana.achievements.AchievementManager.unlockAchievement
import net.sbo.mod.settings.categories.Drops
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.gotLootShareRecently
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.SoundHandler.playCustomSound
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.DataManager.dianaTrackerMayorData
import net.sbo.mod.utils.data.configs.sbo.SboData
import net.sbo.mod.utils.data.configs.sound.SoundSettingsData
import net.sbo.mod.utils.medal.MedalIntegration
import kotlin.reflect.KProperty1

/** Announces a drop in chat, as title and to the party, and tracks it. */
internal object DropAnnouncer {
    private val lootShareBlocklist = setOf("MYTH_THE_FISH", "BRAIDED_GRIFFIN_FEATHER")

    private val dropColors = mapOf(
        "MANTI_CORE" to "§c", "SHIMMERING_WOOL" to "§c", "MYTHOLOGICAL_DYE" to "§c", "MYTH_THE_FISH" to "§c",
        "CHIMERA" to "§d", "FATEFUL_STINGER" to "§d",
        "BRAIN_FOOD" to "§5", "MINOS_RELIC" to "§5", "BRAIDED_GRIFFIN_FEATHER" to "§5",
        "DAEDALUS_STICK" to "§6", "MYTHOS_FRAGMENT" to "§6",
        "CROWN_OF_GREED" to "§6", // TODO: Add Seperate LS tracker for LS Crown and clarify if LS or not in the message + party announce
        "HILT_OF_REVELATIONS" to "§9",
    )

    private val highestMagicFind = mapOf(
        "DAEDALUS_STICK" to SboData::highestStickMagicFind,
        "CHIMERA" to SboData::highestChimMagicFind,
        "MANTI_CORE" to SboData::highestCoreMagicFind,
        "SHIMMERING_WOOL" to SboData::highestWoolMagicFind,
        "FATEFUL_STINGER" to SboData::highestStingerMagicFind,
        "BRAIN_FOOD" to SboData::highestFoodMagicFind,
        "MINOS_RELIC" to SboData::highestRelicMagicFind,
    )

    private fun mfText(magicFind: Int) = if (magicFind > 0) " (+$magicFind ✯ Magic Find)" else ""

    fun playSound(sound: KProperty1<SoundSettingsData, String>, volume: KProperty1<SoundSettingsData, Float>) =
        playCustomSound(sound.get(DataManager.soundSettingsData), volume = volume.get(DataManager.soundSettingsData))

    fun onRareDrop(item: String, showMessageOrTitle: Boolean, trackLootshare: Boolean, magicFind: Int, amount: Int = 1, actuallyRare: Boolean = true, enforceCooldown: Boolean = true) {
        val itemId = item.toItemId()

        if (Tracker.isItemOnCooldown(itemId) && enforceCooldown) return

        val mfPrefix = mfText(magicFind)
        val isCoG = itemId == "CROWN_OF_GREED"

        if (isCoG || itemId == "HILT_OF_REVELATIONS") {
            // Hilt comes from the pickup log and the rare drop message, Crown of Greed only from the pickup log. The sound, clip and party message are here so whichever comes first plays them and the cooldown check above stops the second.
            playSound(SoundSettingsData::miscDropSound, SoundSettingsData::miscDropVolume)

            if (isCoG) {
                MedalIntegration.saveCrownOfGreedClip()
                // CoG is no longer a treasure and instead dropped by Minos King; worth announcing
                announceLootToParty("Crown of Greed", "Crown of Greed$mfPrefix", amount = dianaTrackerMayorData.items.CROWN_OF_GREED + 1) // we didn't call trackItem yet, so add + 1
            } else {
                MedalIntegration.saveHiltOfRevelationsClip()
            }
        }

        val color = dropColors[itemId]
        val hidePrice = Helper.getItemPrice(itemId, amount) == 0L
        val price = "§6${Helper.getItemPriceFormatted(itemId, amount)} coins"

        val ls = gotLootShareRecently() && itemId !in lootShareBlocklist
        val lsText = if (ls) " (LS)" else ""

        // -1: not counted in the message, or no separate lootshare count
        val rawLsCount = if (color != null) Counters.mayorCount("${itemId}_LS") ?: -1 else -1
        val realLsCount = if (rawLsCount != -1) rawLsCount + if (ls) 1 else 0 else -1 // we didn't call trackItem yet, avoids being #0

        val rawCount = if (color != null) Counters.mayorCount(itemId) ?: -1 else -1
        val realCount = if (rawCount != -1) rawCount + if (!ls || rawLsCount == -1) 1 else 0 else -1 // we didn't call trackItem yet, avoids being #0

        val count = when {
            realCount == -1 -> ""
            realLsCount != -1 && ls -> " Total #${realCount + realLsCount} LS #$realLsCount"
            realLsCount != -1 -> " #${realCount + realLsCount}"
            else -> " #$realCount"
        }
        val totalCount = if (realLsCount != -1) realCount + realLsCount else realCount

        if (Drops.lootAnnouncerChat && showMessageOrTitle) {
            val customKey = RareDrops.rareDrops.firstOrNull { it.itemId == itemId }?.customKey
            val customMsg = customKey?.let { Helper.checkCustomDropMessage(it, magicFind, ls, totalCount) } ?: (false to "")

            if (customMsg.first) {
                Chat.chat(customMsg.second)
            } else {
                Chat.chat("§6[SBO] §lRARE DROP! §r${color ?: "§f"}$item§b$mfPrefix§d$lsText§e$count${if (hidePrice) "" else "§6 (+$price)"}")
            }
        }

        if (Drops.lootAnnouncerScreen && showMessageOrTitle && actuallyRare && (!isCoG || Drops.announceCrownOfGreed)) {
            Helper.showTitle("${color ?: "§f"}§l$item$lsText!", if (hidePrice) null else price, 0, 25, 35)
        }

        if (ls) {
            Chat.chat("§6[SBO] §cLootshared a $item!")
            when (itemId) {
                "DAEDALUS_STICK" -> unlockAchievement(15)
                "CHIMERA" -> unlockAchievement(13)
            }
        } else {
            highestMagicFind[itemId]?.let { if (magicFind > it.value) it.value = magicFind }
            trackMagicFind(magicFind, itemId == "CHIMERA")
        }

        Tracker.trackItem(if (trackLootshare && ls) itemId + "_LS" else itemId, amount, enforceCooldown)
    }

    /** The party message for a drop, the custom drop message instead if there is one. */
    fun announceDrop(name: String, customMsg: Pair<Boolean, String>, magicFind: Int, amount: Int, lsAmount: Int = -1) {
        if (customMsg.first) {
            announceLootToParty(name, customMsg.second, true)
        } else {
            announceLootToParty(name, name + mfText(magicFind), amount = amount, lsAmount = lsAmount)
        }
    }

    fun announceLootToParty(item: String, customMsg: String? = null, replaceDropMessage: Boolean = false, amount: Int = -1, lsAmount: Int = -1) {
        if (!Drops.lootAnnouncerParty) return
        var msg = customMsg?.removeFormatting() ?: Helper.toTitleCase(item.replace("_LS", "").replace("_", " "))

        if (!replaceDropMessage) {
            val itemId = item.toItemId()
            val price = Helper.getItemPriceFormatted(itemId)
            val priceStr = if (price != "0") " (+$price coins)" else ""
            val ls = gotLootShareRecently() && itemId !in lootShareBlocklist

            if (ls) msg += " (LS)"
            if (amount != -1) {
                msg += if (ls && lsAmount != -1) " Total #$amount LS #$lsAmount" else " #$amount"
            }
            msg = "[SBO] RARE DROP! $msg$priceStr"
        }

        Chat.pc(msg)
    }
}
