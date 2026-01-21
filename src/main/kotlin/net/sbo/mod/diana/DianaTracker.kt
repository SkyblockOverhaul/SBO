package net.sbo.mod.diana

import net.sbo.mod.SBOKotlin
import net.sbo.mod.diana.achievements.AchievementManager
import net.sbo.mod.diana.achievements.AchievementManager.trackMagicFind
import net.sbo.mod.diana.achievements.AchievementManager.unlockAchievement
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.overlays.DianaMobs
import net.sbo.mod.overlays.DianaStats
import net.sbo.mod.overlays.MagicFind
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.settings.categories.QOL
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.allowSackTracking
import net.sbo.mod.utils.Helper.checkDiana
import net.sbo.mod.utils.Helper.dianaMobDiedRecently
import net.sbo.mod.utils.Helper.gotLootShareRecently
import net.sbo.mod.utils.Helper.lastDianaMobDeath
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.game.Mayor
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.SoundHandler.playCustomSound
import net.sbo.mod.utils.game.World.isInSkyblock
import net.sbo.mod.utils.data.DianaTracker
import net.sbo.mod.utils.data.Item
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.data.SboDataObject.dianaTrackerMayor
import net.sbo.mod.utils.data.SboDataObject.dianaTrackerSession
import net.sbo.mod.utils.data.SboDataObject.dianaTrackerTotal
import net.sbo.mod.utils.data.SboDataObject.pastDianaEventsData
import net.sbo.mod.utils.data.SboDataObject.saveTrackerData
import net.sbo.mod.utils.data.SboDataObject.sboData
import java.util.regex.Pattern

object DianaTracker {
    private val otherDrops = listOf("ENCHANTED_ANCIENT_CLAW", "ANCIENT_CLAW", "ENCHANTED_GOLD")
    private val sackDrops = listOf("Enchanted Gold", "Ancient Claw", "Enchanted Ancient Claw")
    private val isMobOnCooldown: MutableMap<String, Boolean> = mutableMapOf()
    private val isItemOnCooldown: MutableMap<String, Boolean> = mutableMapOf()

    private val lootAnnouncerBuffer: MutableList<String> = mutableListOf()
    private var lootAnnouncerBool: Boolean = false
    private var allowScavTracking: Boolean = true

    fun init() {
        Register.command("sboresetsession") {
            dianaTrackerSession.reset().save()
            Chat.chat("§6[SBO] §aDiana session tracker has been reset.")
            DianaMobs.updateLines()
            DianaLoot.updateLines()
        }

        Register.command("sboresetmayortracker") {
            resetMayorTracker()
        }

        Register.command("sboresetstatstracker") {
            sboData.mobsSinceInq = 0
            sboData.inqsSinceChim = 0
            sboData.minotaursSinceStick = 0
            sboData.champsSinceRelic = 0
            sboData.inqsSinceLsChim = 0
            SboDataObject.save("SboData")
            SboTimerManager.timerSession.reset()
            DianaStats.updateLines()
        }

        Register.onChatMessageCancable(
            Pattern.compile("^§eThe election room is now closed\\. Clerk Seraphine is doing a final count of the votes\\.\\.\\.$", Pattern.DOTALL)
        ) { _, _ ->
            sleep(10000) {
                checkMayorTracker()
            }
            true
        }

        Register.onChatMessageCancable(
            Pattern.compile("^§6§lWow! §eYou dug out §6(.*?) coins§e!$")
        ) { _, _ ->
            allowScavTracking = false
            sleep(1000) {
                allowScavTracking = true
            }
            true
        }

        Register.onChatMessageCancable(Pattern.compile("(.*?) §efound a §cPhoenix §epet!(.*?)$", Pattern.DOTALL)) { message, matchResult ->
            if (QOL.phoenixAnnouncer) {
                Chat.chat("§6[SBO] §cGG §eFound a §cPhoenix §epet!")
                Helper.showTitle("§c§lPhoenix Pet!", "", 0, 25, 35)
            }
            val player = matchResult.group(1).removeFormatting().lowercase()
            if (!player.contains((Player.getName()?: "").lowercase())) return@onChatMessageCancable true
            sleep(1000) {
                if (isInSkyblock() && checkDiana() && dianaMobDiedRecently(3)) unlockAchievement(77) // phoenix pet
            }
            true
        }

        trackBurrowsWithChat()
        trackMobsWithChat()
        trackCoinsWithChat()
        trackTreasuresWithChat()
        trackRngDropsWithChat()
        trackShardsWithChat()
        trackMythTheFish()
    }

    fun trackWithPickuplog(item: Item) {
        sleep (1000) {
            if (Helper.getSecondsPassed(item.creation) > 5) return@sleep
//            if (!dianaMobDiedRecently(3)) return@sleep
            if (!checkDiana()) return@sleep
            when (item.itemId) {
                "HILT_OF_REVELATIONS" -> onRareDropFromMob("HILT_OF_REVELATIONS", false, false, false, 0)
                "CROWN_OF_GREED" -> onRareDropFromMob("CROWN_OF_GREED", false, false, false, 0)
            }
        }
    }

    fun trackWithPickuplogStackable(item: Item, amount: Int) {
        sleep (1000) {
            if (!dianaMobDiedRecently(3) && !gotLootShareRecently(3)) return@sleep
            if (!checkDiana()) return@sleep
            if (item.itemId in otherDrops) {
                trackItem(item.itemId, amount)
                return@sleep
            }
        }
    }

    fun trackWithSacksMessage(itemName: String, amount: Int) {
        if (!allowSackTracking) return
        if (!checkDiana()) return
        val item = itemName.replace("Ingot", "").trim()
        if (sackDrops.contains(item)) {
            trackItem(item, amount)
        }
    }

    fun trackScavengerCoins(amount: Long) {
        if (amount <= 0) return
        if (!dianaMobDiedRecently(4)) return
        if (!checkDiana()) return
        if (amount > 150000 || !allowScavTracking) return
        trackItem("SCAVENGER_COINS", amount.toInt())
        trackItem("COINS", amount.toInt())
    }

    fun trackMobsWithChat() {
        Register.onChatMessageCancable(Pattern.compile("(.*?) §eYou dug (.*?)§2(.*?)§e!(.*?)$", Pattern.DOTALL)) { message, matchResult ->
            val mob = matchResult.group(3)
            if (isMobOnCooldown.getOrDefault(mob, false)) return@onChatMessageCancable !QOL.dianaMessageHider
            when (mob) {
                "King Minos" -> {
                    DianaMobDetect.onRareSpawn(mob)
                    trackMob(mob, 1)

                    sboData.kingSinceWool += 1
                    if (Diana.sendSinceMessage) {
                        val timeSinceKing = Helper.formatTime(dianaTrackerTotal.items.TIME - sboData.lastKingDate)
                        if (sboData.lastKingDate != 0L) {
                            Chat.chat("§6[SBO] §eTook §c${sboData.mobsSinceKing} §eMobs and §c$timeSinceKing §eto get a King!")
                        } else {
                            Chat.chat("§6[SBO] §eTook §c${sboData.mobsSinceKing} §eMobs to get a King!")
                        }
                    }
                    sboData.lastKingDate = dianaTrackerTotal.items.TIME

                    if (sboData.b2bKing && sboData.mobsSinceKing == 1) {
                        Chat.chat("§6[SBO] §cb2b2b King Minos!")
                        unlockAchievement(117) // b2b2b king
                    }
                    if (sboData.mobsSinceKing == 1 && !sboData.b2bKing) {
                        Chat.chat("§6[SBO] §cb2b King Minos!")
                        unlockAchievement(87) // b2b king
                        sboData.b2bKing = true
                    }
                    if (sboData.kingSinceWool >= 2) sboData.b2bWool = false
                    if (sboData.kingSinceWool >= 2) sboData.b2bWoolLs = false
                    sboData.mobsSinceKing = 0
                }
                "Manticore" -> {
                    DianaMobDetect.onRareSpawn(mob)
                    trackMob(mob, 1)

                    sboData.mantiSinceCore += 1
                    sboData.mantiSinceStinger += 1

                    if (Diana.sendSinceMessage) {
                        val timeSinceManti = Helper.formatTime(dianaTrackerTotal.items.TIME - sboData.lastMantiDate)
                        if (sboData.lastMantiDate != 0L) {
                            Chat.chat("§6[SBO] §eTook §c${sboData.mobsSinceManti} §eMobs and §c$timeSinceManti §eto get a Manticore!")
                        } else {
                            Chat.chat("§6[SBO] §eTook §c${sboData.mobsSinceManti} §eMobs to get a Manticore!")
                        }
                    }
                    sboData.lastMantiDate = dianaTrackerTotal.items.TIME

                    if (sboData.b2bManti && sboData.mobsSinceManti == 1) {
                        Chat.chat("§6[SBO] §cb2b2b Manticore!")
                        unlockAchievement(110)
                    }
                    if (sboData.mobsSinceManti == 1 && !sboData.b2bManti) {
                        Chat.chat("§6[SBO] §cb2b Manticore!")
                        unlockAchievement(109) // b2b manti
                        sboData.b2bManti = true
                    }
                    if (sboData.mantiSinceCore >= 2) sboData.b2bCore = false
                    if (sboData.mantiSinceStinger >= 2) sboData.b2bStinger = false
                    sboData.mobsSinceManti = 0
                }
                "Minos Inquisitor" -> {
                    DianaMobDetect.onRareSpawn(mob)
                    trackMob(mob, 1)

                    sboData.inqsSinceChim += 1

                    if (Diana.sendSinceMessage) {
                        val timeSinceInq = Helper.formatTime(dianaTrackerTotal.items.TIME - sboData.lastInqDate)
                        if (sboData.lastInqDate != 0L) {
                            Chat.chat("§6[SBO] §eTook §c${sboData.mobsSinceInq} §eMobs and §c$timeSinceInq §eto get an Inquis!")
                        } else {
                            Chat.chat("§6[SBO] §eTook §c${sboData.mobsSinceInq} §eMobs to get an Inquis!")
                        }
                    }
                    sboData.lastInqDate = dianaTrackerTotal.items.TIME

                    if (sboData.b2bInq && sboData.mobsSinceInq == 1) {
                        Chat.chat("§6[SBO] §cb2b2b Inquisitor!")
                        unlockAchievement(7) // b2b2b inq
                    }
                    if (sboData.mobsSinceInq == 1 && !sboData.b2bInq) {
                        Chat.chat("§6[SBO] §cb2b Inquisitor!")
                        unlockAchievement(6) // b2b inq
                        sboData.b2bInq = true
                    }
                    if (sboData.inqsSinceChim >= 2) sboData.b2bChim = false
                    sboData.mobsSinceInq = 0
                }
                "Sphinx" -> {
                    DianaMobDetect.onRareSpawn(mob)
                    trackMob(mob, 1)

                    sboData.sphinxSinceFood += 1

                    if (Diana.sendSinceMessage) {
                        val timeSinceSphinx = Helper.formatTime(dianaTrackerTotal.items.TIME - sboData.lastSphinxDate)
                        if (sboData.lastSphinxDate != 0L) {
                            Chat.chat("§6[SBO] §eTook §c${sboData.mobsSinceSphinx} §eMobs and §c$timeSinceSphinx §eto get a Sphinx!")
                        } else {
                            Chat.chat("§6[SBO] §eTook §c${sboData.mobsSinceSphinx} §eMobs to get a Sphinx!")
                        }
                    }
                    sboData.lastSphinxDate = dianaTrackerTotal.items.TIME

                    if (sboData.b2bSphinx && sboData.mobsSinceSphinx == 1) {
                        Chat.chat("§6[SBO] §cb2b2b Sphinx!")
                        unlockAchievement(108)
                    }
                    if (sboData.mobsSinceSphinx == 1 && !sboData.b2bSphinx) {
                        Chat.chat("§6[SBO] §cb2b Sphinx!")
                        unlockAchievement(107)
                        sboData.b2bSphinx = true
                    }
                    if (sboData.sphinxSinceFood >= 2) sboData.b2bFood = false
                    sboData.mobsSinceSphinx = 0
                }
                "Minos Champion" -> {
                    sboData.champsSinceRelic += 1
                    trackMob(mob, 1)
                }
                "Minotaur" -> {
                    sboData.minotaursSinceStick += 1
                    if (sboData.minotaursSinceStick >= 2) sboData.b2bStick = false
                    trackMob(mob, 1)
                }
                "Gaia Construct" -> trackMob(mob, 1)
                "Harpy" -> trackMob(mob, 1)
                "Cretan Bull" -> trackMob(mob, 1)
                "Stranded Nymph" -> trackMob(mob, 1)
                "Siamese Lynxes" -> trackMob(mob, 1)
                "Minos Hunter" -> trackMob(mob, 1)
            }
            SboDataObject.save("SboData")
            !QOL.dianaMessageHider
        }
    }

    fun trackCoinsWithChat() {
        Register.onChatMessageCancable(Pattern.compile("^§6§lWow! §eYou dug out §6(.*?) coins§e!$", Pattern.DOTALL)) { message, matchResult ->
            val coins = matchResult.group(1).replace(",", "").toIntOrNull() ?: 0
            if (coins > 0) trackItem("COINS", coins)
            true
        }
    }

    fun trackTreasuresWithChat() {
        Register.onChatMessageCancable(Pattern.compile("^§6§lRARE DROP! §eYou dug out a (.*?)§e!$", Pattern.DOTALL)) { message, matchResult ->
            val drop = matchResult.group(1).drop(2)
            when (drop) {
                "Griffin Feather" -> trackItem(drop, 1)
                "Mythos Fragment" -> trackItem(drop, 1)
                "Braided Griffin Feather" -> trackItem(drop, 1) // todo: add since message
            }
            true
        }
    }

    fun trackRngDropsWithChat() {
        Register.onChatMessageCancable(Pattern.compile("^§6§lRARE DROP! (.*?)$", Pattern.DOTALL)) { message, matchResult ->
            if (!checkDiana()) return@onChatMessageCancable true
            val drop = matchResult.group(1)
            val isLootShare = gotLootShareRecently(2)

            val magicfind = Helper.getMagicFind(drop)
            var mfPrefix = ""
            if (magicfind > 0) mfPrefix = " (+$magicfind ✯ Magic Find)"

            when {
                drop.contains("Shimmering Wool") -> { // todo: add achievements for wool
                    playCustomSound(Customization.woolSound[0], Customization.woolVolume)
                    onRareDropFromMob("Shimmering Wool", true, true, true, magicfind)
                    if (!isLootShare) {
                        // normal wool
                        if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.kingSinceWool} §eKing Minos to get Shimmering Wool!")

                        if (sboData.b2bWool && sboData.kingSinceWool == 1) {
                            Chat.chat("§6[SBO] §cb2b2b Shimmering Wool!")
                        }
                        if (sboData.kingSinceWool == 1 && !sboData.b2bWool) {
                            Chat.chat("§6[SBO] §cb2b Shimmering Wool!")
                            unlockAchievement(81) // b2b wool
                            sboData.b2bWool = true
                        }
                        sboData.kingSinceWool = 0
                    } else {
                        // lootshare wool
                        if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.kingSinceLsWool} §eKing Minos to lootshare Shimmering Wool!")

                        sleep(200) {
                            if (sboData.b2bWoolLs && sboData.kingSinceLsWool == 1) {
                                Chat.chat("§6[SBO] §cb2b2b Lootshare Shimmering Wool!")
                            }
                            if (sboData.kingSinceLsWool == 1 && !sboData.b2bWoolLs) {
                                Chat.chat("§6[SBO] §cb2b Lootshare Shimmering Wool!")
                                unlockAchievement(82) // b2b ls wool
                                sboData.b2bWoolLs = true
                            }
                            sboData.kingSinceLsWool = 0
                        }
                    }

                    val customMsg = Helper.checkCustomDropMessage("wool", magicfind)
                    if (customMsg.first) {
                        announceLootToParty("Shimmering Wool!", customMsg.second, true)
                    } else {
                        announceLootToParty("Shimmering Wool!", "Shimmering Wool!$mfPrefix")
                    }

                }
                drop.contains("Manti-core") -> { // todo: add achievements for core
                    playCustomSound(Customization.coreSound[0], Customization.coreVolume)
                    onRareDropFromMob("Manti-core", true, true, true, magicfind)
                    if (!isLootShare) {
                        // normal core
                        if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.mantiSinceCore} §eManticores to get Manti-core!")

                        if (sboData.b2bCore && sboData.mantiSinceCore == 1) {
                            Chat.chat("§6[SBO] §cb2b2b Manti-core!")
                        }
                        if (sboData.mantiSinceCore == 1 && !sboData.b2bCore) {
                            Chat.chat("§6[SBO] §cb2b Manti-core!")
                            unlockAchievement(94)
                            sboData.b2bCore = true
                        }
                        sboData.mantiSinceCore = 0
                    } else {
                        // lootshare core
                        if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.mantiSinceLsCore} §eManticores to lootshare Manti-core!")

                        sleep(200) {
                            if (sboData.b2bCoreLs && sboData.mantiSinceLsCore == 1) {
                                Chat.chat("§6[SBO] §cb2b2b Lootshare Manti-core!")
                            }
                            if (sboData.mantiSinceLsCore == 1 && !sboData.b2bCoreLs) {
                                Chat.chat("§6[SBO] §cb2b Lootshare Manti-core!")
                                unlockAchievement(95)
                                sboData.b2bCoreLs = true
                            }
                            sboData.mantiSinceLsCore = 0
                        }
                    }

                    val customMsg = Helper.checkCustomDropMessage("core", magicfind)
                    if (customMsg.first) {
                        announceLootToParty("Manti-core!", customMsg.second, true)
                    } else {
                        announceLootToParty("Manti-core!", "Manti-core!$mfPrefix")
                    }

                }
                drop.contains("Fateful Stinger") -> { // todo: add achievements for stinger
                    playCustomSound(Customization.stingerSound[0], Customization.stingerVolume)
                    onRareDropFromMob("Fateful Stinger", true, true, true, magicfind)
                    if (!isLootShare) {
                        // normal stinger
                        if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.mantiSinceStinger} §eManticores to get Fateful Stinger!")

                        if (sboData.b2bStinger && sboData.mantiSinceStinger == 1) {
                            Chat.chat("§6[SBO] §cb2b2b Fateful Stinger!")
                        }
                        if (sboData.mantiSinceStinger == 1 && !sboData.b2bStinger) {
                            Chat.chat("§6[SBO] §cb2b Fateful Stinger!")
                            sboData.b2bStinger = true
                        }
                        sboData.mantiSinceStinger = 0
                    } else {
                        // lootshare stinger
                        if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.mantiSinceLsStinger} §eManticores to lootshare Fateful Stinger!")

                        sleep(200) {
                            if (sboData.b2bStingerLs && sboData.mantiSinceLsStinger == 1) {
                                Chat.chat("§6[SBO] §cb2b2b Lootshare Fateful Stinger!")
                            }
                            if (sboData.mantiSinceLsStinger == 1 && !sboData.b2bStingerLs) {
                                Chat.chat("§6[SBO] §cb2b Lootshare Fateful Stinger!")
                                sboData.b2bStingerLs = true
                            }
                            sboData.mantiSinceLsStinger = 0
                        }
                    }

                    val customMsg = Helper.checkCustomDropMessage("Stinger", magicfind)
                    if (customMsg.first) {
                        announceLootToParty("Fateful Stinger!", customMsg.second, true)
                    } else {
                        announceLootToParty("Fateful Stinger!", "Fateful Stinger!$mfPrefix")
                    }

                }
                drop.contains("Enchanted Book") -> {
                    if (!drop.contains("Chimera")) return@onChatMessageCancable true

                    playCustomSound(Customization.chimSound[0], Customization.chimVolume)
                    onRareDropFromMob("Chimera", true, false, true, magicfind)

                    if (!isLootShare) {
                        // normal chimera
                        if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.inqsSinceChim} §eInquisitors to get a Chimera!")

                        if (sboData.b2bChim && sboData.inqsSinceChim == 1) {
                            Chat.chat("§6[SBO] §cb2b2b Chimera!")
                            unlockAchievement(2) // b2b2b chim
                        }
                        if (sboData.inqsSinceChim == 1 && !sboData.b2bChim) {
                            Chat.chat("§6[SBO] §cb2b Chimera!")
                            sboData.b2bChim = true
                            unlockAchievement(1) // b2b chim
                        }
                        if (sboData.b2bChim && sboData.b2bInq) {
                            unlockAchievement(75) // b2b chim from b2b inq
                        }
                        sboData.inqsSinceChim = 0
                    } else {
                        // lootshare chim
                        if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.inqsSinceLsChim} §eInquisitors to lootshare a Chimera!")

                        sleep(200) {
                            if (sboData.b2bChimLs && sboData.inqsSinceLsChim == 1) {
                                Chat.chat("§6[SBO] §cb2b2b Lootshare Chimera!")
                                unlockAchievement(67) // b2b2b chim ls
                            }
                            if (sboData.inqsSinceLsChim == 1 && !sboData.b2bChimLs) {
                                Chat.chat("§6[SBO] §cb2b Lootshare Chimera!")
                                sboData.b2bChimLs = true
                                unlockAchievement(66) // b2b chim ls
                            }
                            sboData.inqsSinceLsChim = 0
                        }
                    }

                    val customChimMsg = Helper.checkCustomDropMessage("Chimera", magicfind)
                    if (customChimMsg.first) {
                        announceLootToParty("Chimera!", customChimMsg.second, true)
                    } else {
                        announceLootToParty("Chimera!", "Chimera!$mfPrefix")
                    }
                }
                drop.contains("Brain Food") -> { // todo: add achievements for food
                    playCustomSound(Customization.bfSound[0], Customization.bfVolume)
                    onRareDropFromMob("Brain Food", true, true, true, magicfind)
                    if (!isLootShare) {
                        // normal brain food
                        if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.sphinxSinceFood} §eSphinx to get Brain Food!")

                        if (sboData.b2bFood && sboData.sphinxSinceFood == 1) {
                            Chat.chat("§6[SBO] §cb2b2b Brain Food!")
                            unlockAchievement(98) // b2b2b food
                        }
                        if (sboData.sphinxSinceFood == 1 && !sboData.b2bFood) {
                            Chat.chat("§6[SBO] §cb2b Brain Food!")
                            unlockAchievement(97) // b2b food
                            sboData.b2bInq = true
                        }
                        sboData.sphinxSinceFood = 0
                    } else {
                        // lootshare brain food
                        if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.sphinxSinceFood} §eSphinx to lootshare Brain Food!")

                        sleep(200) {
                            if (sboData.b2bFoodLs && sboData.sphinxSinceFood == 1) {
                                Chat.chat("§6[SBO] §cb2b2b Lootshare Brain Food!")
                                unlockAchievement(100) // b2b2b ls food
                            }
                            if (sboData.sphinxSinceFood == 1 && !sboData.b2bFoodLs) {
                                Chat.chat("§6[SBO] §cb2b Lootshare Brain Food!")
                                unlockAchievement(99) // b2b ls food
                                sboData.b2bFoodLs = true
                            }
                            sboData.sphinxSinceLsFood = 0
                        }
                    }

                    val customMsg = Helper.checkCustomDropMessage("Brain Food", magicfind)
                    if (customMsg.first) {
                        announceLootToParty("Brain Food!", customMsg.second, true)
                    } else {
                        announceLootToParty("Brain Food!", "Brain Food!$mfPrefix")
                    }
                }
                drop.contains("Daedalus Stick") -> {
                    playCustomSound(Customization.stickSound[0], Customization.stickVolume)
                    onRareDropFromMob("Daedalus Stick", true, true, false, magicfind)

                    if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.minotaursSinceStick} §eMinotaurs to get a Daedalus Stick!")

                    if (sboData.b2bStick && sboData.minotaursSinceStick == 1) {
                        Chat.chat("§6[SBO] §cb2b2b Daedalus Stick!")
                        unlockAchievement(4) // b2b2b stick
                    }
                    if (sboData.minotaursSinceStick == 1 && !sboData.b2bStick) {
                        Chat.chat("§6[SBO] §cb2b Daedalus Stick!")
                        sboData.b2bStick = true
                        unlockAchievement(3) // b2b stick
                    }
                    sboData.minotaursSinceStick = 0
                }
                drop.contains("Minos Relic") -> {
                    playCustomSound(Customization.relicSound[0], Customization.relicVolume)
                    onRareDropFromMob("Minos Relic", true, true, false, magicfind)

                    if (Diana.sendSinceMessage) Chat.chat("§6[SBO] §eTook §c${sboData.champsSinceRelic} §eChampions to get a Minos Relic!")

                    if (sboData.champsSinceRelic == 1) {
                        Chat.chat("§6[SBO] §cb2b Minos Relic!")
                        unlockAchievement(5) // b2b relic
                    }
                    if (isLootShare) {
                        Chat.chat("§6[SBO] §cLootshared a Minos Relic!")
                        unlockAchievement(17) // relic ls
                    }
                    sboData.champsSinceRelic = 0
                }
                drop.contains("Crown of Greed") -> onRareDropFromMob("Crown of Greed", false, false, false, magicfind)
                drop.contains("Washed-up Souvenir") -> onRareDropFromMob("Washed-up Souvenir", false, false, false, magicfind)
                drop.contains("Dwarf Turtle Shelmet") -> onRareDropFromMob("Dwarf Turtle Shelmet", false, false, false, magicfind)
                drop.contains("Crochet Tiger Plushie") -> onRareDropFromMob("Crochet Tiger Plushie", false, false, false, magicfind)
                drop.contains("Antique Remedies") -> onRareDropFromMob("Antique Remedies", false, false, false, magicfind)
                drop.contains("Cretan Urn") -> onRareDropFromMob("Cretan Urn", false, false, false, magicfind)
                drop.contains("Hilt of Revelations") -> onRareDropFromMob("Hilt of Revelations", false, false, false, magicfind)
            }
            SboDataObject.save("SboData")
            true
        }

        Register.onChatMessageCancable(Pattern.compile("^§d§lWOW! (.*?) §6found a §2Mythological Dye§6!$", Pattern.DOTALL)) { message, matchResult ->
            val player = matchResult.group(1).removeFormatting().lowercase().trim()
            if (player.contains(Player.getName()?.lowercase()?.trim()?: "")) {
                onRareDropFromMob("Mythological Dye", true, true, false, 0)
            }
            true
        }
    }

    // todo: show title and announce to party based on value of the item
    fun onRareDropFromMob(item: String, title: Boolean, partyAnnounce: Boolean, trackLootshare: Boolean, magicFind: Int, amount: Int = 1) {
        if (isItemOnCooldown.getOrDefault(item, false)) return
        val itemId = item.uppercase().replace(" ", "_").replace("-", "_")
        var mfPrefix = ""
        if (magicFind > 0) mfPrefix = " (+$magicFind ✯ Magic Find)"

        if (Diana.lootAnnouncerScreen && title) {
            val subTitle = if (Diana.lootAnnouncerPrice) "§6${Helper.getItemPriceFormatted(itemId, amount)} coins" else ""
            when (itemId) {
                "MANTI_CORE", "SHIMMERING_WOOL", "MYTHOLOGICAL_DYE"  -> {
                    Helper.showTitle("§c§l$item!", subTitle, 0, 25, 35)
                }
                "CHIMERA", "FATEFUL_STINGER" -> {
                    Helper.showTitle("§d§l$item!", subTitle, 0, 25, 35)
                }
                "BRAIN_FOOD", "MINOS_RELIC", "BRAIDED_GRIFFIN_FEATHER" -> {
                    Helper.showTitle("§5§l$item!", subTitle, 0, 25, 35)
                }
                "DAEDALUS_STICK", "MYTHOS_FRAGMENT" -> {
                    Helper.showTitle("§6§l$item!", subTitle, 0, 25, 35)
                }
            }
        }

        val isLootShare = gotLootShareRecently(2)
        if (isLootShare) {
            Chat.chat("§6[SBO] §cLootshared a $item!")
            when (itemId) {
                "DAEDALUS_STICK" -> unlockAchievement(15)
                "CHIMERA" -> unlockAchievement(16)
            }
        } else {
            when (itemId) {
                "DAEDALUS_STICK" -> if (magicFind > sboData.highestStickMagicFind) sboData.highestStickMagicFind = magicFind
                "CHIMERA" -> if (magicFind > sboData.highestChimMagicFind) sboData.highestChimMagicFind = magicFind
                "MATI_CORE" -> if (magicFind > sboData.highestCoreMagicFind) sboData.highestCoreMagicFind = magicFind
                "SHIMMERING_WOOL" -> if (magicFind > sboData.highestWoolMagicFind) sboData.highestWoolMagicFind = magicFind
                "FABLED_STINGER" -> if (magicFind > sboData.highestStingerMagicFind) sboData.highestStingerMagicFind = magicFind
                "BRAIN_FOOD" -> if (magicFind > sboData.highestFoodMagicFind) sboData.highestFoodMagicFind = magicFind
            }

            trackMagicFind(magicFind, itemId == "CHIMERA")
        }

        if (trackLootshare && isLootShare) {
            trackItem(itemId + "_LS", amount)
        } else {
            trackItem(itemId, amount)
        }
    }

    fun trackBurrowsWithChat() {
        Register.onChatMessageCancable(Pattern.compile("^§eYou (.*?) Griffin [Bb]urrow(.*?)$", Pattern.DOTALL)) { message, matchResult ->
            val burrow = matchResult.group(2).removeFormatting()
            trackItem("TOTAL_BURROWS", 1)
            if (Diana.fourEyedFish) {
                if (burrow.contains("(2/4)") || burrow.contains("(3/4)")) {
                    trackItem("FISH_COINS", 4000)
                    trackItem("COINS", 4000)
                } else {
                    trackItem("FISH_COINS", 2000)
                    trackItem("COINS", 2000)
                }
            }
            !QOL.dianaMessageHider
        }
    }

    fun announceLootToParty(item: String, customMsg: String? = null, replaceDropMessage: Boolean = false) {
        if (!Diana.lootAnnouncerParty) return
        var msg = Helper.toTitleCase(item.replace("_LS", "").replace("_", " "))
        if (customMsg != null) msg = customMsg.removeFormatting()

        if (replaceDropMessage) {
            if (customMsg != null) Chat.chat(customMsg)
            Chat.command("pc $msg")
        } else {
            lootAnnouncerBuffer.add(msg)
            if (!lootAnnouncerBool) {
                lootAnnouncerBool = true
                sleep(1500) {
                    sendLootAnnouncement()
                    lootAnnouncerBool = false
                }
            }
        }
    }

    fun sendLootAnnouncement() {
        if (lootAnnouncerBuffer.isEmpty()) return
        val msg = lootAnnouncerBuffer.joinToString(", ")
        lootAnnouncerBuffer.clear()
        Chat.command("pc [SBO] RARE DROP! $msg")
    }

    fun getB2BMessage(itemName: String, streak: Int): String? { // not used yet
        if (streak <= 1) return null

        val prettyName = Helper.toTitleCase(itemName.replace("_", " "))
        val streakText = "b" + "2b".repeat(streak - 1)

        return "§6[SBO] §c$streakText $prettyName!"
    }

    fun checkMayorTracker() {
        if (dianaTrackerMayor.year == 0) {
            dianaTrackerMayor.year = Mayor.mayorElectedYear
            dianaTrackerMayor.save()
            return
        }

        if (dianaTrackerMayor.year >= Mayor.mayorElectedYear) return
        var allZero = true
        for (item in dianaTrackerMayor.mobs::class.java.declaredFields) {
            item.isAccessible = true
            if (item.get(dianaTrackerMayor.mobs) is Int) {
                if (item.getInt(dianaTrackerMayor.mobs) > 0) {
                    allZero = false
                    break
                }
            }
        }
        resetMayorTracker(allZero)
    }

    internal fun resetMayorTracker(check: Boolean = false) {
        if (!check) {
            pastDianaEventsData.events += dianaTrackerMayor.snapshot()
            SboDataObject.save("PastDianaEventsData")
        }
        dianaTrackerMayor.reset()
        dianaTrackerMayor.year = Mayor.mayorElectedYear
        dianaTrackerMayor.save()
        SboTimerManager.timerMayor.reset()
        SboTimerManager.activeTimers.forEach { it.pause() }
        DianaMobs.updateLines()
        DianaLoot.updateLines()
        Chat.chat("§6[SBO] §aDiana mayor tracker has been reset.")
    }

    fun trackShardsWithChat() {
        Register.onChatMessageCancable(Pattern.compile("^(.*?) You charmed a (.*?) and captured (.*?) Shards §7from it.$", Pattern.DOTALL)) { message, matchResult ->
            val shard = matchResult.group(2).removeFormatting()
            val amount = matchResult.group(3).removeFormatting().toIntOrNull() ?: 0
            when (shard) {
                "King Minos" -> trackItem("KING_MINOS_SHARD", amount)
                "Sphinx" -> trackItem("SPHINX_SHARD", amount)
                "Minotaur" -> trackItem("MINOTAUR_SHARD", amount)
                "Cretan Bull" -> trackItem("CRETAN_BULL_SHARD", amount)
                "Harpy" -> trackItem("HARPY_SHARD", amount)
            }
            true
        }

        Register.onChatMessageCancable(Pattern.compile("^(.*?) You charmed a (.*?) and captured its §9Shard§7.$", Pattern.DOTALL)) { message, matchResult ->
            val shard = matchResult.group(2).removeFormatting()
            val amount = 1
            when (shard) {
                "King Minos" -> trackItem("KING_MINOS_SHARD", amount)
                "Sphinx" -> trackItem("SPHINX_SHARD", amount)
                "Minotaur" -> trackItem("MINOTAUR_SHARD", amount)
                "Cretan Bull" -> trackItem("CRETAN_BULL_SHARD", amount)
                "Harpy" -> trackItem("HARPY_SHARD", amount)
            }
            true
        }

        Register.onChatMessageCancable(Pattern.compile("^§aYou caught (.*?) (.*?) §aShards(.*?)$", Pattern.DOTALL)) { message, matchResult ->
            val shard = matchResult.group(2).removeFormatting()
            val amount = matchResult.group(1).removeFormatting().replace("x", "").trim().toIntOrNull() ?: 0
            when (shard) {
                "King Minos" -> trackItem("KING_MINOS_SHARD", amount)
                "Sphinx" -> trackItem("SPHINX_SHARD", amount)
                "Minotaur" -> trackItem("MINOTAUR_SHARD", amount)
                "Cretan Bull" -> trackItem("CRETAN_BULL_SHARD", amount)
                "Harpy" -> trackItem("HARPY_SHARD", amount)
            }
            true
        }

        Register.onChatMessageCancable(Pattern.compile("^§aYou caught a (.*?) §aShard!$", Pattern.DOTALL)) { message, matchResult ->
            val shard = matchResult.group(1).removeFormatting()
            val amount = 1
            when (shard) {
                "King Minos" -> trackItem("KING_MINOS_SHARD", amount)
                "Sphinx" -> trackItem("SPHINX_SHARD", amount)
                "Minotaur" -> trackItem("MINOTAUR_SHARD", amount)
                "Cretan Bull" -> trackItem("CRETAN_BULL_SHARD", amount)
                "Harpy" -> trackItem("HARPY_SHARD", amount)
            }
            true
        }
    }

    fun trackMythTheFish() {
        Register.onChatMessage(Regex("^(.*?) §eYou just dug out(.*?)$")) { message, matchResult ->
            if (matchResult.groupValues[1].contains("Myth the Fish")) {
                onRareDropFromMob("Myth the Fish", false, true, false, 0)
                unlockAchievement(119)
            }
        }
    }

    fun trackMob(item: String, amount: Int) {
        if (isMobOnCooldown.getOrDefault(item, false)) return
        isMobOnCooldown[item] = true

        trackItem(item, amount)
        trackItem("TOTAL_MOBS", amount)
        sboData.mobsSinceInq += amount
        if (sboData.mobsSinceInq >= 2) sboData.b2bInq = false
        sboData.mobsSinceKing += amount
        if (sboData.mobsSinceKing >= 2) sboData.b2bKing = false
        sboData.mobsSinceManti += amount
        if (sboData.mobsSinceManti >= 2) sboData.b2bManti = false
        sboData.mobsSinceSphinx += amount
        if (sboData.mobsSinceSphinx >= 2) sboData.b2bSphinx = false
        SboDataObject.save("SboData")

        sleep(500) {
            isMobOnCooldown[item] = false
        }
    }

    fun trackItem(item: String, amount: Int, fromInq: Boolean = false) {
        if (isItemOnCooldown.getOrDefault(item, false)) return
        isItemOnCooldown[item] = true

        checkMayorTracker()
        val itemName = Helper.toUpperSnakeCase(item)
        if (itemName == "MINOS_INQUISITOR_LS") sboData.inqsSinceLsChim += 1
        if (itemName == "KING_MINOS_LS") sboData.kingSinceLsWool += 1
        if (itemName == "MANTICORE_LS") {
            sboData.mantiSinceLsCore += 1
            sboData.mantiSinceLsStinger += 1
        }
        if (itemName == "SPHINX_LS") sboData.sphinxSinceLsFood += 1

        trackOne(dianaTrackerMayor, itemName, amount, fromInq)
        trackOne(dianaTrackerSession, itemName, amount, fromInq)
        trackOne(dianaTrackerTotal, itemName, amount, fromInq)
        saveTrackerData()
        DianaStats.updateLines()
        MagicFind.updateLines()
        DianaMobs.updateLines()
        DianaLoot.updateLines()
        SboTimerManager.updateAllActivity()
        AchievementManager.trackAchievementsItem(dianaTrackerMayor)
        AchievementManager.trackSince()

        sleep(500) {
            isItemOnCooldown[item] = false
        }
    }

    fun trackOne(tracker: DianaTracker, item: String, amount: Int, fromInq: Boolean = false) {
        when (item) {
            // SHARDS
            "KING_MINOS_SHARD" -> tracker.items.KING_MINOS_SHARD += amount
            "SPHINX_SHARD" -> tracker.items.SPHINX_SHARD += amount
            "MINOTAUR_SHARD" -> tracker.items.MINOTAUR_SHARD += amount
            "CRETAN_BULL_SHARD" -> tracker.items.CRETAN_BULL_SHARD += amount
            "HARPY_SHARD" -> tracker.items.HARPY_SHARD += amount

            // ITEMS
            "BRAIDED_GRIFFIN_FEATHER" -> tracker.items.BRAIDED_GRIFFIN_FEATHER += amount // todo: title, since und party message // drops from treasure burrow
            "FATEFUL_STINGER" -> tracker.items.FATEFUL_STINGER += amount
            "FATEFUL_STINGER_LS" -> tracker.items.FATEFUL_STINGER_LS += amount
            "MANTI_CORE" -> tracker.items.MANTI_CORE += amount
            "MANTI_CORE_LS" -> tracker.items.MANTI_CORE_LS += amount
            "SHIMMERING_WOOL" -> tracker.items.SHIMMERING_WOOL += amount
            "SHIMMERING_WOOL_LS" -> tracker.items.SHIMMERING_WOOL_LS += amount
            "BRAIN_FOOD" -> tracker.items.BRAIN_FOOD += amount
            "BRAIN_FOOD_LS" -> tracker.items.BRAIN_FOOD_LS += amount

            "MYTHOLOGICAL_DYE" -> tracker.items.MYTHOLOGICAL_DYE += amount

            "CRETAN_URN" -> tracker.items.CRETAN_URN += amount
            "MYTHOS_FRAGMENT" -> tracker.items.MYTHOS_FRAGMENT += amount
            "HILT_OF_REVELATIONS" -> tracker.items.HILT_OF_REVELATIONS += amount
            "MYTH_THE_FISH" -> tracker.items.MYTH_THE_FISH += amount

            "COINS" -> tracker.items.COINS += amount
            "GRIFFIN_FEATHER" -> tracker.items.GRIFFIN_FEATHER += amount
            "CROWN_OF_GREED" -> tracker.items.CROWN_OF_GREED += amount
            "WASHED_UP_SOUVENIR" -> tracker.items.WASHED_UP_SOUVENIR += amount
            "CHIMERA" -> tracker.items.CHIMERA += amount
            "CHIMERA_LS" -> tracker.items.CHIMERA_LS += amount
            "DAEDALUS_STICK" -> tracker.items.DAEDALUS_STICK += amount
            "DWARF_TURTLE_SHELMET" -> tracker.items.DWARF_TURTLE_SHELMET += amount
            "CROCHET_TIGER_PLUSHIE" -> tracker.items.CROCHET_TIGER_PLUSHIE += amount
            "ANTIQUE_REMEDIES" -> tracker.items.ANTIQUE_REMEDIES += amount
            "ENCHANTED_ANCIENT_CLAW" -> tracker.items.ENCHANTED_ANCIENT_CLAW += amount
            "ANCIENT_CLAW" -> tracker.items.ANCIENT_CLAW += amount
            "MINOS_RELIC" -> tracker.items.MINOS_RELIC += amount
            "ENCHANTED_GOLD" -> tracker.items.ENCHANTED_GOLD += amount
            "SCAVENGER_COINS" -> tracker.items.SCAVENGER_COINS += amount
            "FISH_COINS" -> tracker.items.FISH_COINS += amount
            "TOTAL_BURROWS" -> tracker.items.TOTAL_BURROWS += amount
            // MOBS
            "KING_MINOS" -> tracker.mobs.KING_MINOS += amount
            "KING_MINOS_LS" -> tracker.mobs.KING_MINOS_LS += amount
            "MANTICORE" -> tracker.mobs.MANTICORE += amount
            "MANTICORE_LS" -> tracker.mobs.MANTICORE_LS += amount
            "MINOS_INQUISITOR" -> tracker.mobs.MINOS_INQUISITOR += amount
            "MINOS_INQUISITOR_LS" -> tracker.mobs.MINOS_INQUISITOR_LS += amount
            "SPHINX" -> tracker.mobs.SPHINX += amount
            "SPHINX_LS" -> tracker.mobs.SPHINX_LS += amount
            "MINOS_CHAMPION" -> tracker.mobs.MINOS_CHAMPION += amount
            "MINOTAUR" -> tracker.mobs.MINOTAUR += amount
            "GAIA_CONSTRUCT" -> tracker.mobs.GAIA_CONSTRUCT += amount
            "HARPY" -> tracker.mobs.HARPY += amount
            "CRETAN_BULL" -> tracker.mobs.CRETAN_BULL += amount
            "STRANDED_NYMPH" -> tracker.mobs.STRANDED_NYMPH += amount
            "SIAMESE_LYNXES" -> tracker.mobs.SIAMESE_LYNXES += amount
            "MINOS_HUNTER" -> tracker.mobs.MINOS_HUNTER += amount
            "TOTAL_MOBS" -> tracker.mobs.TOTAL_MOBS += amount
        }
    }
}
