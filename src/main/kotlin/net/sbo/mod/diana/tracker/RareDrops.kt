package net.sbo.mod.diana.tracker

import net.sbo.mod.diana.achievements.AchievementManager.unlockAchievement
import net.sbo.mod.settings.categories.Drops
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.gotLootShareRecently
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.DataManager.dianaTrackerMayorData
import net.sbo.mod.utils.data.DataManager.sboData
import net.sbo.mod.utils.data.configs.sbo.SboData
import net.sbo.mod.utils.data.configs.sound.SoundSettingsData
import net.sbo.mod.utils.medal.MedalIntegration
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

/** The drops from the RARE DROP messages and digging, with their since counters, b2b streaks and clips. */
internal object RareDrops {
    /** The drops of the rare mobs that also count lootshares. */
    class RareDrop(
        val name: String,
        /** All of these are in the drop message. */
        val match: List<String>,
        val customKey: String,
        val sinceText: String,
        val droppedBy: String,
        val since: KMutableProperty1<SboData, Int>,
        val lsSince: KMutableProperty1<SboData, Int>,
        val b2b: KMutableProperty1<SboData, Boolean>,
        val lsB2b: KMutableProperty1<SboData, Boolean>,
        val sound: KProperty1<SoundSettingsData, String>,
        val volume: KProperty1<SoundSettingsData, Float>,
        val clip: () -> Unit,
        val b2bAch: Int? = null,
        val b2b2bAch: Int? = null,
        val lsB2bAch: Int? = null,
        val lsB2b2bAch: Int? = null,
    ) {
        val itemId = name.toItemId()
    }

    val rareDrops = listOf(
        RareDrop("Shimmering Wool", listOf("Shimmering Wool"), "wool", "Shimmering Wool", "King Minos",
            SboData::kingSinceWool, SboData::kingSinceLsWool, SboData::b2bWool, SboData::b2bWoolLs,
            SoundSettingsData::woolSound, SoundSettingsData::woolVolume, { MedalIntegration.saveWoolClip() }, b2bAch = 81, lsB2bAch = 82),
        RareDrop("Manti-core", listOf("Manti-core"), "core", "Manti-core", "Manticores",
            SboData::mantiSinceCore, SboData::mantiSinceLsCore, SboData::b2bCore, SboData::b2bCoreLs,
            SoundSettingsData::coreSound, SoundSettingsData::coreVolume, { MedalIntegration.saveMantiCoreClip() }, b2bAch = 94, lsB2bAch = 95),
        RareDrop("Fateful Stinger", listOf("Fateful Stinger"), "stinger", "Fateful Stinger", "Manticores",
            SboData::mantiSinceStinger, SboData::mantiSinceLsStinger, SboData::b2bStinger, SboData::b2bStingerLs,
            SoundSettingsData::stingerSound, SoundSettingsData::stingerVolume, { MedalIntegration.saveFatefulStingerClip() }),
        RareDrop("Chimera", listOf("Enchanted Book", "Chimera"), "chimera", "a Chimera", "Inquisitors",
            SboData::inqsSinceChim, SboData::inqsSinceLsChim, SboData::b2bChim, SboData::b2bChimLs,
            SoundSettingsData::chimSound, SoundSettingsData::chimVolume, { MedalIntegration.saveChimeraClip() }, b2bAch = 1, b2b2bAch = 2, lsB2bAch = 66, lsB2b2bAch = 67),
        RareDrop("Brain Food", listOf("Brain Food"), "brain food", "Brain Food", "Sphinx",
            SboData::sphinxSinceFood, SboData::sphinxSinceLsFood, SboData::b2bFood, SboData::b2bFoodLs,
            SoundSettingsData::bfSound, SoundSettingsData::bfVolume, { MedalIntegration.saveBrainFoodClip() }, b2bAch = 97, b2b2bAch = 98, lsB2bAch = 99, lsB2b2bAch = 100),
    )

    /** Drops that only get tracked, a clip and the misc drop sound. */
    private val miscDrops: List<Pair<String, () -> Unit>> = listOf(
        "Washed-up Souvenir" to { MedalIntegration.saveWashedUpSouvenirClip() },
        "Dwarf Turtle Shelmet" to { MedalIntegration.saveDwarfTurtleShelmetClip() },
        "Crochet Tiger Plushie" to { MedalIntegration.saveCrochetTigerPlushieClip() },
        "Antique Remedies" to { MedalIntegration.saveAntiqueRemediesClip() },
        "Cretan Urn" to { MedalIntegration.saveCretanUrnClip() },
    )

    /** The drop from a "RARE DROP!" message. */
    fun onChatDrop(drop: String) {
        val isLootShare = gotLootShareRecently()
        val magicFind = Helper.getMagicFind(drop)
        val rareDrop = rareDrops.firstOrNull { rare -> rare.match.all { drop.contains(it) } }
        val miscDrop = miscDrops.firstOrNull { drop.contains(it.first) }

        when {
            rareDrop != null -> onMobRareDrop(rareDrop, magicFind, isLootShare)

            drop.contains("Enchanted Book") -> return // a book other than Chimera

            drop.contains("Daedalus Stick") -> {
                DropAnnouncer.playSound(SoundSettingsData::stickSound, SoundSettingsData::stickVolume)
                MedalIntegration.saveDaedalusStickClip()
                DropAnnouncer.onRareDrop("Daedalus Stick", showMessageOrTitle = true, trackLootshare = false, magicFind = magicFind)
                val customMsg = Helper.checkCustomDropMessage("Daedalus Stick", magicFind, false)

                if (Drops.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.minotaursSinceStick} §eMinotaurs to get a Daedalus Stick!")
                RareMobs.announceB2b("Daedalus Stick", sboData.minotaursSinceStick, SboData::b2bStick, b2bAch = 3, b2b2bAch = 4)
                sboData.minotaursSinceStick = 0

                DropAnnouncer.announceDrop("Daedalus Stick", customMsg, magicFind, amount = dianaTrackerMayorData.items.DAEDALUS_STICK)
            }

            drop.contains("Minos Relic") -> {
                MedalIntegration.saveMinosRelicClip()
                DropAnnouncer.playSound(SoundSettingsData::relicSound, SoundSettingsData::relicVolume)
                DropAnnouncer.onRareDrop("Minos Relic", showMessageOrTitle = true, trackLootshare = false, magicFind = magicFind)
                val customMsg = Helper.checkCustomDropMessage("Minos Relic", magicFind, isLootShare)

                if (Drops.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.champsSinceRelic} §eChampions to get a Minos Relic!")
                if (sboData.champsSinceRelic == 1) {
                    Chat.chat("§6[SBO] §cb2b Minos Relic!")
                    unlockAchievement(5) // b2b relic
                }
                if (isLootShare) {
                    Chat.chat("§6[SBO] §cLootshared a Minos Relic!")
                    unlockAchievement(17) // relic ls
                }
                sboData.champsSinceRelic = 0

                DropAnnouncer.announceDrop("Minos Relic", customMsg, magicFind, amount = dianaTrackerMayorData.items.MINOS_RELIC)
            }

            miscDrop != null -> {
                miscDrop.second()
                DropAnnouncer.onRareDrop(miscDrop.first, showMessageOrTitle = false, trackLootshare = false, magicFind = magicFind)
                DropAnnouncer.playSound(SoundSettingsData::miscDropSound, SoundSettingsData::miscDropVolume)
            }

            // sound played by onRareDrop for hilt
            drop.contains("Hilt of Revelations") -> DropAnnouncer.onRareDrop("Hilt of Revelations", showMessageOrTitle = false, trackLootshare = false, magicFind = magicFind)
        }
        DataManager.save(DataManager::sboData)
    }

    /** Wool, Manti-core, Fateful Stinger, Chimera and Brain Food: since message and b2b, separately for lootshares. */
    private fun onMobRareDrop(drop: RareDrop, magicFind: Int, isLootShare: Boolean) {
        drop.clip()
        DropAnnouncer.playSound(drop.sound, drop.volume)
        DropAnnouncer.onRareDrop(drop.name, showMessageOrTitle = true,
            trackLootshare = true,
            magicFind = magicFind,
            // lootsharing from own inquisitor is possible, which allows to drop 2 chimeras from a single inquisitor of yours, so we need to not early return if isItemOnCooldown
            enforceCooldown = drop.name != "Chimera"
        )

        val customMsg = Helper.checkCustomDropMessage(drop.customKey, magicFind, isLootShare)

        if (!isLootShare) {
            if (Drops.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${drop.since.value} §e${drop.droppedBy} to get ${drop.sinceText}!")
            RareMobs.announceB2b(drop.name, drop.since.value, drop.b2b, drop.b2bAch, drop.b2b2bAch)
            if (drop.name == "Chimera" && sboData.b2bChim && sboData.b2bInq) unlockAchievement(75) // b2b chim from b2b inq
            drop.since.value = 0
        } else {
            if (Drops.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${drop.lsSince.value} §e${drop.droppedBy} to lootshare ${drop.sinceText}!")
            sleep(200) {
                RareMobs.announceB2b("Lootshare ${drop.name}", drop.lsSince.value, drop.lsB2b, drop.lsB2bAch, drop.lsB2b2bAch)
                drop.lsSince.value = 0
            }
        }

        val lsAmount = Counters.mayorCount(drop.itemId + "_LS") ?: 0
        DropAnnouncer.announceDrop(drop.name, customMsg, magicFind, amount = (Counters.mayorCount(drop.itemId) ?: 0) + lsAmount, lsAmount = lsAmount)
    }

    fun onBraidedFeather() {
        // TODO: add since message
        MedalIntegration.saveBraidedFeatherClip()
        DropAnnouncer.onRareDrop("Braided Griffin Feather", showMessageOrTitle = true, trackLootshare = false, magicFind = 0)
        DropAnnouncer.announceLootToParty("Braided Griffin Feather", "Braided Griffin Feather", amount = dianaTrackerMayorData.items.BRAIDED_GRIFFIN_FEATHER)
    }

    fun onMythologicalDye() {
        MedalIntegration.saveMythologicalDyeClip()
        DropAnnouncer.onRareDrop("Mythological Dye", showMessageOrTitle = true, trackLootshare = false, magicFind = 0)
    }

    fun onMythTheFish() {
        MedalIntegration.saveMythTheFishClip()
        DropAnnouncer.onRareDrop("Myth the Fish", showMessageOrTitle = true, trackLootshare = false, magicFind = 0)
        DropAnnouncer.announceLootToParty("Myth the Fish", "Myth the Fish", amount = dianaTrackerMayorData.items.MYTH_THE_FISH)

        if (Tracker.lastSpawnedMob == "Sphinx" && Tracker.lastBurrow == Tracker.BurrowType.MOB) unlockAchievement(119)
    }
}
