package net.sbo.mod.diana.achievements

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.DianaMobDetect.RareDianaMob
import net.sbo.mod.overlays.DianaLoot.totalProfit
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.HypixelModApi.isOnHypixel
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DianaTrackerMayorData
import net.sbo.mod.utils.data.PartyPlayerStats
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.data.SboDataObject.achievementsData
import net.sbo.mod.utils.data.SboDataObject.dianaTrackerMayor
import net.sbo.mod.utils.data.SboDataObject.pastDianaEventsData
import net.sbo.mod.utils.data.SboDataObject.sboData
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.entity.EntitiyHitEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.game.ItemLookup
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

object AchievementManager {
    val rarityColorDict = mapOf(
        "Common" to "§f",
        "Uncommon" to "§a",
        "Rare" to "§9",
        "Epic" to "§5",
        "Legendary" to "§6",
        "Mythic" to "§d",
        "Divine" to "§b",
        "Celestial" to "§1",
        "Impossible" to "§4",
    )

    internal val achievements = mutableMapOf<Int, Achievement>()
    var achievementsUnlockedTotal = 0
    var achievementsUnlockedEvent = 0
    private val achievementQueue = ConcurrentLinkedQueue<Int>()
    private val isProcessingQueue = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun init() {
        Register.command("sbolockachievements") { args ->
            if (args.getOrNull(0) != "CONFIRM") {
                Chat.chat("§6[SBO] §eYou are about to reset all your achievements. Type §c/sbolockachievements CONFIRM §eto confirm")
                return@command
            }

            achievementsData.totalAchievements.clear()
            achievementsData.currentEventAchievements.clear()
            achievementsData.lastEventYear = -1

            SboDataObject.save("AchievementsData")
            Chat.chat("§6[SBO] §eAchievements locked")
        }

        Register.command("sbobacktrackachievements") {
            backTrackAchievements()
        }
        addAllAchievements()
        
        kingSoul()

        // Die to a Minos King
        // Register.onChatMessage()
    }

    fun addAchievement(id: Int, name: String, description: String, rarity: String, previousId: Int? = null, hidden: Boolean = false, repeatable: Boolean = true) {
        if (achievements.containsKey(id)) {
            throw RuntimeException("Duplicate achievement ID detected: $id. Achievement with this ID already exists: ${achievements[id]?.name}")
        }
        val achievement = Achievement(id, name, description, rarity, previousId, hidden, repeatable)
        achievements[id] = achievement
        achievement.loadState()
    }

    fun getAchievement(id: Int): Achievement? {
        return achievements[id]
    }

    fun unlockAchievement(id: Int) {
        val achievement = getAchievement(id) ?: return

        if (achievement.canBeUnlocked()) {
            achievementQueue.add(id)
            processQueue()
        }
    }

    private fun processQueue() {
        if (isProcessingQueue.compareAndSet(false, true)) {
            coroutineScope.launch {
                while (achievementQueue.isNotEmpty()) {
                    val id = achievementQueue.poll()
                    val achievement = getAchievement(id)
                    if (achievement != null && achievement.canBeUnlocked()) {
                        if (achievement.previousId != null && !getAchievement(achievement.previousId)?.isUnlocked()!!) {
                            achievementQueue.add(achievement.previousId)
                            achievementQueue.add(id)
                        } else {
                            achievement.unlock()
                            sleep(1000L)
                        }
                    }
                }
                isProcessingQueue.set(false)
            }
        }
    }

    fun backTrackAchievements() { // todo: needs rework because of new data structure
        Chat.chat("§6[SBO] §eBacktracking Achievements...")
        pastDianaEventsData.events.forEach { eventData ->
            trackAchievementsItem(eventData)
        }
        trackSince()
    }

    @SboEvent
    fun onEntityHit(event: EntitiyHitEvent) {
        if (event.entity !is Player) return
        val isRareMob = RareDianaMob.entries.any { event.entity.name.string.contains(it.display, ignoreCase = true) } && event.entity.uuid.version() != 4
        if (!isRareMob) return
        val item = event.player.mainHandItem
        val lookup = ItemLookup(item)
        if (lookup.displayName.contains("Shears", true) && event.entity.name.string.contains("King Minos")) unlockAchievement(92)
        if (lookup.displayName.contains("Core", true) && event.entity.name.string.contains("Manticore")) unlockAchievement(93)
    }


    fun trackAchievementsItem(tracker: DianaTrackerMayorData) {
        if (!isOnHypixel) return
        val itemsData = tracker.items
        val mobsData = tracker.mobs
        val time = itemsData.TIME
        val totalBurrows = itemsData.TOTAL_BURROWS
        val totalChimera = itemsData.CHIMERA + itemsData.CHIMERA_LS
        val totalWools = itemsData.SHIMMERING_WOOL + itemsData.SHIMMERING_WOOL_LS
        val totalBrainFood = itemsData.BRAIN_FOOD + itemsData.BRAIN_FOOD_LS
        val totalCores = itemsData.MANTI_CORE + itemsData.MANTI_CORE_LS
        val daedalusStickCount = itemsData.DAEDALUS_STICK
        val chimeraLsCount = itemsData.CHIMERA_LS

        when {
            totalBurrows >= 25000 -> unlockAchievement(22)
            totalBurrows >= 20000 -> unlockAchievement(21)
            totalBurrows >= 15000 -> unlockAchievement(20)
            totalBurrows >= 10000 -> unlockAchievement(19)
            totalBurrows >= 5000 -> unlockAchievement(18)
        }

        when {
            time >= 86400000L * 3 -> unlockAchievement(27)
            time >= 86400000L * 2 -> unlockAchievement(26)
            time >= 86400000L -> unlockAchievement(25)
            time >= 3600000L * 10 -> unlockAchievement(24)
            time >= 3600000L -> unlockAchievement(23)
        }

        if (itemsData.MINOS_RELIC >= 1) unlockAchievement(16)

        when {
            daedalusStickCount >= 7 -> unlockAchievement(8)
            daedalusStickCount >= 1 -> unlockAchievement(14)
        }

        when {
            totalChimera >= 64 -> unlockAchievement(125)
            totalChimera >= 32 -> unlockAchievement(11)
            totalChimera >= 16 -> unlockAchievement(9)
            totalChimera >= 1 -> unlockAchievement(12)
        }

        when {
            chimeraLsCount >= 32 -> unlockAchievement(126)
            chimeraLsCount >= 16 -> unlockAchievement(10)
            chimeraLsCount >= 1 -> unlockAchievement(13)
        }

        if (time >= 18000000L) { // 5 hours
            val burrows = tracker.items.TOTAL_BURROWS.toDouble()
            val burrowsPerHour = (burrows / (time.toDouble() / 3600000.0)).toInt()
            when {
                burrowsPerHour >= 600 -> unlockAchievement(72)
                burrowsPerHour >= 500 -> unlockAchievement(71)
                burrowsPerHour >= 450 -> unlockAchievement(70)
                burrowsPerHour >= 400 -> unlockAchievement(69)
                burrowsPerHour >= 350 -> unlockAchievement(68)
            }
        }

        if (daedalusStickCount >= 1 && totalChimera >= 2) unlockAchievement(73)
        if (daedalusStickCount >= 1 && itemsData.MINOS_RELIC >= 2) unlockAchievement(74)
        if (daedalusStickCount >= 1 && itemsData.MANTI_CORE + itemsData.MANTI_CORE_LS >= 2) unlockAchievement(78)

        when {
            totalWools >= 3 -> unlockAchievement(79)
            totalWools >= 1 -> unlockAchievement(83)
        }

        if (itemsData.SHIMMERING_WOOL_LS >= 1) unlockAchievement(111)

        if (itemsData.MANTI_CORE_LS >= 1) unlockAchievement(114)
        if (totalCores >= 1) unlockAchievement(113)

        if (totalBrainFood >= 5) unlockAchievement(85)

        if (itemsData.MYTHOLOGICAL_DYE >= 1) unlockAchievement(86)

        if (mobsData.KING_MINOS >= 1) unlockAchievement(116)
        if (mobsData.SPHINX >= 1) unlockAchievement(115)
        if (mobsData.MANTICORE >= 1) unlockAchievement(112)

        if (totalProfit(dianaTrackerMayor) >= 1_000_000_000L) unlockAchievement(84)
        trackCOA()
    }

    fun trackSince() {
        if (!isOnHypixel) return

        when {
            sboData.mobsSinceInq >= 1000 -> unlockAchievement(33)
            sboData.mobsSinceInq >= 500 -> unlockAchievement(32)
            sboData.mobsSinceInq >= 250 -> unlockAchievement(31)
        }

        when {
            sboData.inqsSinceChim >= 100 -> unlockAchievement(37)
            sboData.inqsSinceChim >= 60 -> unlockAchievement(36)
            sboData.inqsSinceChim >= 30 -> unlockAchievement(35)
            sboData.inqsSinceChim >= 15 -> unlockAchievement(34)
        }

        if (sboData.minotaursSinceStick >= 200) unlockAchievement(29)

        when {
            sboData.champsSinceRelic >= 3000 -> unlockAchievement(65)
            sboData.champsSinceRelic >= 1000 -> unlockAchievement(30)
        }
    }

    fun trackMagicFind(magicFind: Int, chimera: Boolean = false) {
        if (!isOnHypixel) return
        if (magicFind == 0) return

        when {
            magicFind >= 600 -> unlockAchievement(42)
            magicFind >= 500 -> unlockAchievement(41)
            magicFind >= 400 -> unlockAchievement(40)
            magicFind >= 300 -> unlockAchievement(39)
        }

        if (chimera) {
            when {
                magicFind < 100 -> unlockAchievement(43)
                magicFind < 200 -> unlockAchievement(44)
            }
        }
    }

    @SboEvent
    fun trackBeKills(event: GuiOpenEvent) {
        Helper.sleep(200) {
            val screen = event.screen
            if (screen !is AbstractContainerScreen<*>) return@sleep
            if (!event.screen.title.string.contains("Mythological Creatur", ignoreCase = true)) return@sleep
            val slots = screen.menu.slots

            val bullKills = Helper.getKillsFromLore(slots[10].item)
            val gaiaKills = Helper.getKillsFromLore(slots[11].item)
            val harpyKills = Helper.getKillsFromLore(slots[12].item)
            val kingKills = Helper.getKillsFromLore(slots[13].item)
            val mantiKills = Helper.getKillsFromLore(slots[14].item)
            val champKills = Helper.getKillsFromLore(slots[15].item)
            val hunterKills = Helper.getKillsFromLore(slots[16].item)
            val inqKills = Helper.getKillsFromLore(slots[19].item)
            val minoKills = Helper.getKillsFromLore(slots[20].item)
            val catKills = Helper.getKillsFromLore(slots[21].item)
            val sphinxKills = Helper.getKillsFromLore(slots[22].item)
            val nymphKills = Helper.getKillsFromLore(slots[23].item)

            val allMaxed = listOf(
                gaiaKills to 50, inqKills to 45, minoKills to 46,
                champKills to 47, hunterKills to 48, catKills to 49,
                nymphKills to 101, bullKills to 102, harpyKills to 103,
                sphinxKills to 104, mantiKills to 105, kingKills to 106
            ).all { (kills, id) ->
                val isMaxed = when (id) {
                    45, 104 -> kills >= 500
                    50, 49, 102, 103, 101, 48 -> kills >= 3000
                    47, 46 -> kills >= 1000
                    105, 106 -> kills >= 100
                    else -> false
                }
                if (isMaxed) unlockAchievement(id)
                isMaxed
            }
            if (allMaxed) unlockAchievement(51)
        }
    }

    @SboEvent
    fun trackCarnivalPerks(event: GuiOpenEvent) {
        Helper.sleep(200) {
            val screen = event.screen
            if (screen !is AbstractContainerScreen<*>) return@sleep
            if (!event.screen.title.string.contains("Mythological Ritual", ignoreCase = true)) return@sleep
            val slots = screen.menu.slots

            val dmgSlot = slots[11]
            val coinsSlot = slots[12]
            val mfSlot = slots[14]
            val trackingSlot = slots[15]

            if (dmgSlot.item.hoverName.removeFormatting().contains("Storied Stinger V") && coinsSlot.item.hoverName.removeFormatting().contains("Deadly Greed V") && mfSlot.item.hoverName.removeFormatting().contains("Diana's Favor III") && trackingSlot.item.hoverName.removeFormatting().contains("Elusive Hunter II")) { // does not guarantee its maxed since it shows next tier instead of current if it's not maxed, but we still need the check so that we don't trigger achievement for maxing unrelated perks
                for (upgrade in listOf(dmgSlot, coinsSlot, mfSlot, trackingSlot)) {
                    val lookup = ItemLookup(upgrade.item)
                    var hasUnlocked = false
                    for (loreLine in lookup.loreList) {
                        if (loreLine.removeFormatting().contains("UNLOCKED")) {
                            hasUnlocked = true
                            break
                        }
                    }
                    if (!hasUnlocked) {
                        return@sleep // not maxed
                    }
                }

                // if we reach here all should be maxed
                unlockAchievement(120)
            }
        }
    }

    val COA_MF_PATTERN = Pattern.compile("\\+([0-9]*\\.?[0-9]+)✯ Magic Find ✿")

    fun trackCOA() {
        val helmet = mc.player?.inventory?.getItem(39) ?: ItemStack.EMPTY
        val lookup = ItemLookup(helmet)
        if (!lookup.displayName.contains("Crown of Avarice")) return

        unlockAchievement(121)

        val mf = lookup.loreList
            .map { it.removeFormatting() }
            .getValueFromLine(COA_MF_PATTERN)
            .toDouble()

        when (mf) {
            25.0 -> unlockAchievement(124)
            22.5 -> unlockAchievement(123)
            20.0 -> unlockAchievement(122)
        }
    }

    private fun List<String>.getValueFromLine(regex: Pattern): String {
        for (line in this) {
            val matcher = regex.matcher(line)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return ""
    }

    fun trackWithCheckPlayer(playerInfo: PartyPlayerStats) {
        if (playerInfo.eman9) unlockAchievement(56)

        when {
            playerInfo.mythosKills >= 150000 -> unlockAchievement(61)
            playerInfo.mythosKills >= 100000 -> unlockAchievement(60)
            playerInfo.mythosKills >= 50000 -> unlockAchievement(59)
            playerInfo.mythosKills >= 25000 -> unlockAchievement(58)
            playerInfo.mythosKills >= 10000 -> unlockAchievement(57)
        }

        when {
            playerInfo.killLeaderboard <= 10 -> unlockAchievement(64)
            playerInfo.killLeaderboard <= 50 -> unlockAchievement(63)
            playerInfo.killLeaderboard <= 100 -> unlockAchievement(62)
        }
    }

    @SboEvent
    fun checkDaxeEnchants(event: WorldChangeEvent) {
        Helper.sleep(2000) {
            var chimV = false
            var lootingV = false
            var divineGift3 = false
            val player = mc.player ?: return@sleep

            for (slot in 0..8) {
                val stack: ItemStack = player.inventory.getItem(slot)
                if (stack.isEmpty) continue

                val displayName: String = stack.hoverName.string.lowercase()
                if (!displayName.contains("daedalus")) continue

                val customData: CustomData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                val nbt: CompoundTag = customData.copyTag()

                val enchants: CompoundTag = nbt.getCompound("enchantments").orElse(CompoundTag())
                if (enchants.isEmpty) continue

                if (enchants.getInt("ultimate_chimera").orElse(0) == 5) chimV = true
                if (enchants.getInt("looting").orElse(0) == 5) lootingV = true
                if (enchants.getInt("divine_gift").orElse(0) == 3) divineGift3 = true
                break
            }
            if (chimV) unlockAchievement(52)
            if (lootingV) unlockAchievement(53)
            if (divineGift3) unlockAchievement(54)
            if (chimV && lootingV && divineGift3) unlockAchievement(55)
        }
    }

    fun kingSoul() {
        Register.onChatMessage(Regex("^§aYou added a §2Empyrean King Minos §7Lv1750 §asoul to your §9Summoning Ring§a!$")) { message, matchResult ->
            if (matchResult.groupValues[0].contains("King Minos")) unlockAchievement(118)
        }
    }

    fun addAllAchievements() {
        // inquisitor
        addAchievement(1, "Back-to-Back Chimera", "Get 2 Chimera in a row", "Mythic")
        addAchievement(2, "b2b2b Chimera", "Get 3 Chimera in a row", "Divine")
        addAchievement(66, "Back-to-Back LS Chimera", "Get 2 Lootshare Chimera in a row", "Divine")
        addAchievement(67, "b2b2b LS Chimera", "Get 3 Lootshare Chimera in a row", "Impossible", 66)
        addAchievement(6, "Inquisitor Double Trouble", "Get 2 Inquisitors in a row", "Epic")
        addAchievement(7, "b2b2b Inquisitor", "Get 3 Inquisitors in a row", "Divine")
        addAchievement(75, "Back-to-Back Goat", "Get b2b chimera from b2b inquisitor", "Impossible", hidden = true)

        addAchievement(12, "First Chimera", "Get your first Chimera", "Epic", repeatable = false)
        addAchievement(9, "Chimera V", "Get 16 chimera in one event", "Mythic", 12)
        addAchievement(11, "Chimera VI", "Get 32 Chimera in one event", "Divine", 9)
        addAchievement(125, "Chimera VII", "Get 64 Chimera in one event", "Celestial", 11)
        addAchievement(13, "First lootshare Chimera", "Lootshare your first Chimera", "Legendary")
        addAchievement(10, "Tf?", "Get 16 lootshare Chimera in one event", "Divine", 13)
        addAchievement(126, "Wtf?", "Get 32 lootshare Chimera in one event", "Celestial", 10)

        // Minotaur
        addAchievement(14, "First Stick", "Get your first Stick", "Uncommon", repeatable = false)
        addAchievement(8, "Can i make a ladder now?", "Get 7 Sticks in one event", "Epic", 14)
        addAchievement(3, "Back-to-Back Stick", "Get 2 Sticks in a row", "Divine")
        addAchievement(15, "1/6250", "Lootshare a Stick (1/6250)", "Impossible", hidden = true)

        // Champion
        addAchievement(16, "First Relic", "Get your first Relic", "Epic", repeatable = false)
        addAchievement(17, "1/25000", "Lootshare a Relic (1/25000)", "Impossible", hidden = true)
        addAchievement(5, "Back-to-Back Relic", "Get 2 Relics in a row", "Impossible")

        // Sphinx
        addAchievement(115, "What do i click?", "Get your first Sphinx", "Uncommon", repeatable = false)
        addAchievement(107, "Back-to-Back Sphinx", "Get 2 Sphinx in a row", "Epic")
        addAchievement(108, "b2b2b Sphinx", "Get 3 Sphinx in a row", "Mythic", 107)
        addAchievement(97, "Back-to-Back Brain Food", "Get 2 Brain Food in a row", "Legendary")
        addAchievement(98, "b2b2b Brain Food", "Get 3 Brain Food in a row", "Impossible")
        addAchievement(99, "b2b ls Brain Food", "Lootshare 2 Brain Food in a row", "Mythic")
        addAchievement(100, "b2b2b ls Brain Food", "Lootshare 3 Brain Food in a row", "Celestial")
        addAchievement(85, "Might get some braincells back", "Get 5 brain food in one event", "Legendary")

        // Manticore
        addAchievement(112, "Here, Kitty Kitty… OH NO.", "Spawn your first Manticore", "Epic", repeatable = false)
        addAchievement(109, "b2b Manticore", "Spawn 2 Manticores in a row", "Divine")
        addAchievement(110, "b2b2b Manticore", "Spawn 3 Manticores in a row", "Celestial")
        addAchievement(113, "First core", "Drop your first Manti-core", "Mythic")
        addAchievement(94, "Back-to-Back core", "Drop 2 Manti-cores in a row", "Celestial")
        addAchievement(114, "First Lootshare core", "Lootshare your first Manti-core", "Legendary")
        addAchievement(95, "Back-to-Back Lootshare core", "Lootshare 2 Manti-cores in a row", "Impossible")

        // King Minos
        addAchievement(116, "Why do i hear boss music?", "get your first King Minos", "Legendary", repeatable = false)
        addAchievement(87, "b2b King Minos", "Get 2 King Minos in a row", "Celestial")
        addAchievement(117, "b2b2b King Minos", "Get 3 King Minos in a row", "Impossible")
        addAchievement(83, "First Wool", "Get your first Wool", "Mythic")
        addAchievement(81, "Back-to-Back Wool", "Get 2 Wools in a row", "Impossible")
        addAchievement(111, "First Lootshare Wool", "Lootshare your first Wool", "Divine")
        addAchievement(82, "Back-to-Back Lootshare Wool", "Lootshare 2 Wools in a row", "Impossible")
//        addAchievement(88, "Let your party cope!", "Die to your Minos King", "Rare") // TODO

        // Burrows
        addAchievement(18, "Where the grind begins", "Get 5k burrows in one event", "Common")
        addAchievement(19, "Touch some grass", "Get 10k burrows in one event", "Uncommon", 18)
        addAchievement(20, "Please go outside", "Get 15k burrows in one event", "Epic", 19)
        addAchievement(21, "Digging your own grave", "Get 20k burrows in one event", "Legendary", 20)
        addAchievement(22, "Are you mentally stable?", "Get 25k burrows in one event", "Mythic", 21)

        // Playtime
        addAchievement(23, "So this is Diana?", "1 hour of playtime in one event", "Common")
        addAchievement(24, "Is this really fun?", "10 hours of playtime in one event", "Uncommon", 23)
        addAchievement(25, "No shower for me", "1 day of playtime in one event", "Rare", 24)
        addAchievement(26, "Are you okay?", "2 days of playtime in one event", "Epic", 25)
        addAchievement(27, "Sleep is downtime!", "3 days of playtime in one event", "Legendary", 26)

        // Since
        addAchievement(29, "lf Stick", "200 Minotaur since Stick", "Common")
        addAchievement(30, "lf Relic", "1000 Champions since Relic", "Uncommon")
        addAchievement(65, "Where is my Relic?", "3000 champions since Relic", "Mythic", 30)

        addAchievement(31, "lf Inquisitor", "250 mobs since Inquisitor", "Common")
        addAchievement(32, "You have legi Griffin right?", "500 mobs since Inquisitor", "Rare", 31)
        addAchievement(33, "Why do you still play?", "1000 mobs since Inquisitor", "Legendary", 32)

        addAchievement(34, "lf Chimera", "15 Inquisitors since Chimera", "Common")
        addAchievement(35, "So where is my Chimera?", "30 inquisitors since Chimera", "Epic", 34)
        addAchievement(36, "I am done", "60 Inquisitors since Chimera", "Legendary", 35)
        addAchievement(37, "No more Diana", "100 Inquisitors since Chimera", "Divine", 36)

        // Download SBO
        addAchievement(38, "Real Diana non", "Download SBO", "Divine", repeatable = false)

        // Magic Find
        addAchievement(39, "Fortune seeker", "Get a Diana drop with 300 Magic Find", "Uncommon")
        addAchievement(40, "Blessed by fortune", "Get a Diana drop with 400 Magic Find", "Epic", 39)
        addAchievement(41, "Greed knows no bounds", "Get a Diana drop with 500 Magic Find", "Mythic", 40)
        addAchievement(42, "The principle of luck", "Get a Diana drop with 600 Magic Find", "Divine", 41)
        addAchievement(44, "Magic Find is overrated", "Drop a Chimera, under 200 Magic Find", "Epic")
        addAchievement(43, "I don't need Magic Find", "Drop a Chimera, under 100 Magic Find", "Legendary", 44)

        // Bestiary
        addAchievement(45, "Inquisitor Slayer", "Max the Inquisitor Bestiary", "Epic", repeatable = false)
        addAchievement(46, "Minotaur Slayer", "Max the Minotaur Bestiary", "Legendary", repeatable = false)
        addAchievement(47, "Champion Slayer", "Max the Champion Bestiary", "Epic", repeatable = false)
        addAchievement(48, "Hunter Slayer", "Max the Hunter Bestiary", "Epic", repeatable = false)
        addAchievement(49, "Lynx Slayer", "Max the Siamese Lynx Bestiary", "Epic", repeatable = false)
        addAchievement(50, "Gaia Slayer", "Max the Gaia Bestiary", "Legendary", repeatable = false)
        addAchievement(101, "Nymph Slayer", "Max the Nymph Bestiary", "Epic", repeatable = false)
        addAchievement(102, "Cretan Bull Slayer", "Max the Cretan Bull Bestiary", "Epic", repeatable = false)
        addAchievement(103, "Harpy Slayer", "Max the Harpy Bestiary", "Epic", repeatable = false)
        addAchievement(104, "Sphinx Slayer", "Max the Sphinx Bestiary", "Legendary", repeatable = false)
        addAchievement(105, "Manticore Slayer", "Max the Manticore Bestiary", "Mythic", repeatable = false)
        addAchievement(106, "King Minos Slayer", "Max the King Minos Bestiary", "Mythic", repeatable = false)
        addAchievement(51, "Time to get on the leaderboard", "Max all Diana Bestiaries", "Mythic", hidden = true, repeatable = false)

        // Daedalus Axe
        addAchievement(52, "Daedalus Mastery: Chimera V", "Chimera V on Daedalus Axe", "Legendary", repeatable = false)
        addAchievement(53, "Daedalus Mastery: Looting V", "Looting V on Daedalus Axe", "Legendary", repeatable = false)
        addAchievement(54, "Daedalus Mastery: Divine Gift III", "Divine Gift III on Daedalus Axe", "Legendary", repeatable = false)
        addAchievement(55, "Looking Clean", "Get max Divine Gift, Chimera, Looting", "Mythic", hidden = true, repeatable = false)
        addAchievement(56, "Now you can't complain", "Obtain Enderman Slayer 9", "Epic", hidden = true, repeatable = false)

        // Kills
        addAchievement(57, "Oh look maxed Crest", "Kill 10k Diana Mobs", "Rare", repeatable = false)
        addAchievement(58, "Keep the grind going", "Kill 25k Diana Mobs", "Epic", 57, repeatable = false)
        addAchievement(59, "I am not addicted", "Kill 50k Diana Mobs", "Legendary", 58, repeatable = false)
        addAchievement(60, "100k gang", "Kill 100k Diana Mobs", "Mythic", 59, repeatable = false)
        addAchievement(61, "The grind never stops", "Kill 150k Diana Mobs", "Divine", 60, true, repeatable = false)

        // Leaderboard
        addAchievement(62, "Mom look i am on the leaderboard", "Top 100 on the kills leaderboard", "Legendary", repeatable = false)
        addAchievement(63, "So this is what addiction feels like", "Top 50 on the kills leaderboard", "Mythic", 62, repeatable = false)
        addAchievement(64, "Diana is my life", "Top 10 on the kills leaderboard", "Divine", 63, repeatable = false)

        // Burrows per hour
        addAchievement(68, "Dedicated Digger", "Get 350 burrows/hour (5h playtime)", "Uncommon")
        addAchievement(69, "Burrow Enthusiast", "Get 400 burrows/hour (5h playtime)", "Epic", 68)
        addAchievement(70, "Shovel Expert", "Get 450 burrows/hour (5h playtime)", "Legendary", 69)
        addAchievement(71, "Burrow Maniac", "Get 500 burrows/hour (5h playtime)", "Divine", 70)
        addAchievement(72, "Nice macro!", "Get 600 burrows/hour (5h playtime)", "Impossible", 71, true)

        // Other
        addAchievement(73, "Can I craft a Chimera sword now?", "Get 1 stick & 2 chimeras in 1 event", "Epic")
        addAchievement(74, "Can I craft a Relic sword now?", "Get 1 stick & 2 relics in 1 event", "Legendary")
        addAchievement(78, "Can I craft a Core sword now?", "Get 1 stick & 2 manti-cores in 1 event", "Divine")
        addAchievement(79, "Can I craft a Shimmering bed now?", "Get 3 shimmering wool in 1 event", "Celestial", 83)

        addAchievement(86, "It could look better", "Get a Mythological Dye", "Epic")

        addAchievement(84, "Those coins gotta be heavy?", "Make 1b profit in 1 event", "Legendary")

//        addAchievement(89, "Ever heard of gr*ss?", "Get top 100 king bestiary", "Legendary", repeatable = false) // TODO
//        addAchievement(90, "Get a j*b", "Get top 10 king bestiary", "Mythic", repeatable = false) // TODO
//        addAchievement(91, "King of the Hill", "Get top 1 king bestiary", "Impossible", repeatable = false) // TODO

        addAchievement(92, "Why am I not getting a wool???", "Hit a king with a shear", "Uncommon", hidden = true, repeatable = false)
        addAchievement(93, "Why are you doing this?", "Hit a Manticore with 'core' in item name", "Uncommon", hidden = true, repeatable = false)
        addAchievement(118, "No wool? Sell his soul to the devil!", "Get a King's soul", "Epic", hidden = true, repeatable = false)

        addAchievement(119, "Knowledge is Power", "Get Myth the Fish from answering Sphinx question correct", "Mythic", hidden = true)
        addAchievement(120, "Max Diana Carnival", "Get all diana carnival perks maxed out", "Legendary", repeatable = false)

        addAchievement(77, "From the ashes", "Drop a Phoenix pet from a Diana mob", "Impossible", hidden = true)

        addAchievement(121, "Capitalism on top!", "Get COA", "Rare", repeatable = false)
        addAchievement(122, "Inflation speedrun any%", "Get a 10m coins COA", "Epic", 121, repeatable = false)
        addAchievement(123, "Already? Damn that was fast!", "Get a 100m coins COA", "Legendary", 122, repeatable = false)
        addAchievement(124, "All of that just for 2.5 mf", "Get a 1b coins COA", "Mythic", 123, repeatable = false)
    }
}
