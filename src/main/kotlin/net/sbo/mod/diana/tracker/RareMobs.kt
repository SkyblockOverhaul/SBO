package net.sbo.mod.diana.tracker

import net.sbo.mod.diana.DianaMobDetect
import net.sbo.mod.diana.achievements.AchievementManager.unlockAchievement
import net.sbo.mod.settings.categories.Drops
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager.dianaTrackerTotalData
import net.sbo.mod.utils.data.DataManager.sboData
import net.sbo.mod.utils.data.configs.sbo.SboData
import kotlin.reflect.KMutableProperty1

/** Mob spawns: the since counters and b2b streaks of the rare mobs. */
internal object RareMobs {
    private class RareMob(
        val b2bName: String,
        val sinceName: String,
        val mobsSince: KMutableProperty1<SboData, Int>,
        val lastDate: KMutableProperty1<SboData, Long>,
        val b2b: KMutableProperty1<SboData, Boolean>,
        val b2bAch: Int,
        val b2b2bAch: Int,
        /** Counts the spawn for the mob's drops. */
        val countForDrops: SboData.() -> Unit,
    )

    private val rareMobs = mapOf(
        "King Minos" to RareMob("King Minos", "a King", SboData::mobsSinceKing, SboData::lastKingDate, SboData::b2bKing, b2bAch = 87, b2b2bAch = 117) {
            kingSinceWool += 1
            if (kingSinceWool >= 2) { b2bWool = false; b2bWoolLs = false }
        },
        "Manticore" to RareMob("Manticore", "a Manticore", SboData::mobsSinceManti, SboData::lastMantiDate, SboData::b2bManti, b2bAch = 109, b2b2bAch = 110) {
            mantiSinceCore += 1
            mantiSinceStinger += 1
            if (mantiSinceCore >= 2) b2bCore = false
            if (mantiSinceStinger >= 2) b2bStinger = false
        },
        "Minos Inquisitor" to RareMob("Inquisitor", "a Minos Inquisitor", SboData::mobsSinceInq, SboData::lastInqDate, SboData::b2bInq, b2bAch = 6, b2b2bAch = 7) {
            inqsSinceChim += 1
            if (inqsSinceChim >= 2) b2bChim = false
        },
        "Sphinx" to RareMob("Sphinx", "a Sphinx", SboData::mobsSinceSphinx, SboData::lastSphinxDate, SboData::b2bSphinx, b2bAch = 107, b2b2bAch = 108) {
            sphinxSinceFood += 1
            if (sphinxSinceFood >= 2) b2bFood = false
        },
    )
    private val plainMobs = setOf("Gaia Construct", "Harpy", "Cretan Bull", "Stranded Nymph", "Siamese Lynxes", "Minos Hunter")

    fun countSpawn(mob: String) {
        when (mob) {
            in rareMobs -> onRareSpawn(mob, rareMobs.getValue(mob))
            "Minos Champion" -> {
                sboData.champsSinceRelic += 1
                Tracker.trackMob(mob, 1)
            }
            "Minotaur" -> {
                sboData.minotaursSinceStick += 1
                if (sboData.minotaursSinceStick >= 2) sboData.b2bStick = false
                Tracker.trackMob(mob, 1)
            }
            in plainMobs -> Tracker.trackMob(mob, 1)
            else -> Chat.chat("§6[SBO] §cUnknown diana mob spawned: $mob. Please report this.")
        }
    }

    /** Every tracked mob counts towards the next rare mob, the second mob since the last one ends its b2b streak. */
    fun countMob(amount: Int) {
        for (rare in rareMobs.values) {
            rare.mobsSince.value += amount
            if (rare.mobsSince.value >= 2) rare.b2b.value = false
        }
    }

    private fun onRareSpawn(mob: String, rare: RareMob) {
        DianaMobDetect.onRareSpawn(mob)
        Tracker.trackMob(mob, 1)
        sboData.(rare.countForDrops)()

        val mobsSince = rare.mobsSince.value
        if (Drops.sendSinceMessage) {
            val lastDate = rare.lastDate.value
            val time = if (lastDate != 0L) " and §c${Helper.formatTime(dianaTrackerTotalData.items.TIME - lastDate)} §e" else " "
            Chat.chat("§6[SBO] §eTook §c$mobsSince §eMobs${time}to get ${rare.sinceName}!")
        }
        rare.lastDate.value = dianaTrackerTotalData.items.TIME

        announceB2b(rare.b2bName, mobsSince, rare.b2b, rare.b2bAch, rare.b2b2bAch)
        rare.mobsSince.value = 0
    }

    /** "b2b" when the counter is at 1, "b2b2b" when the one before was a b2b already. */
    fun announceB2b(name: String, since: Int, b2b: KMutableProperty1<SboData, Boolean>, b2bAch: Int?, b2b2bAch: Int?) {
        if (since != 1) return
        if (b2b.value) {
            Chat.chat("§6[SBO] §cb2b2b $name!")
            b2b2bAch?.let { unlockAchievement(it) }
        } else {
            Chat.chat("§6[SBO] §cb2b $name!")
            b2bAch?.let { unlockAchievement(it) }
            b2b.value = true
        }
    }
}
