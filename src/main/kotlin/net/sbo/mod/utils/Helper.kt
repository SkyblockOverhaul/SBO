package net.sbo.mod.utils

import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.DianaTracker
import net.sbo.mod.utils.data.DianaTracker as DianaTrackerDataClass
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.data.DianaItemsData
import net.sbo.mod.utils.data.DianaMobsData
import net.sbo.mod.utils.data.npcSellValueMap
import net.sbo.mod.utils.data.HypixelBazaarResponse
import net.sbo.mod.utils.data.Item
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.entity.DianaMobDeathEvent
import net.sbo.mod.utils.events.impl.guis.GuiCloseEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.game.ItemUtils
import net.sbo.mod.utils.game.Mayor
import net.sbo.mod.utils.game.ScoreBoard
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.http.Http
import net.sbo.mod.utils.waypoint.WaypointManager.onLootshare
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.reflect.full.memberProperties
import java.text.DecimalFormat
import java.util.Locale
import java.util.regex.Pattern
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object Helper {
    var lastLootShare: Long = 0L
    var allowSackTracking: Boolean = true
    var hasSpade: Boolean = false
    var lastDianaMobDeath: Long = 0L
    var lastInqDeath: Long = 0L
    var lastKingDeath: Long = 0L
    var lastSphinxDeath: Long = 0L
    var lastMantiDeath: Long = 0L
    var currentScreen: Screen? = null
    var lastCocoon: Long = 0L

    private var hasTrackedInq: Boolean = false
    private var hasTrackedKing: Boolean = false
    private var hasTrackedSphinx: Boolean = false
    private var hasTrackedManti: Boolean = false

    private var prevInv = mutableMapOf<String, Item>()
    private var priceDataAh: Map<String, Long> = emptyMap()
    private var priceDataBazaar: HypixelBazaarResponse? = null

    private val SBO_CALLBACK_THREAD: ExecutorService = Executors.newThreadPerTaskExecutor(Thread
            .ofVirtual()
            .name("sbo-callback-thread-", 1) // sbo-callback-thread-1, sbo-callback-thread-2 etc. starting from 1 (second parameter)
            .factory() // virtual threads are daemon by default
    )

    fun init() {
        Register.onChatMessageCancable(Pattern.compile("^§e§lLOOT SHARE §fYou received loot for assisting (.*?)$", Pattern.DOTALL)) { message, matchResult ->
            onLootshare()
            lastLootShare = System.currentTimeMillis()
            true
        }

        Register.onTick(20) { // maybe better way to register this
            hasSpade = playerHasItem("DEIFIC_SPADE") || playerHasItem("ARCHAIC_SPADE") || playerHasItem("ANCESTRAL_SPADE")
        }

        Register.onTick(20 * 60 * 5) {
            updateItemPriceInfo()
        }
        updateItemPriceInfo()
    }

    @SboEvent
    fun onDianaMobDeath(event: DianaMobDeathEvent) {
        val dist = event.entity.distanceTo(mc.player!!)
        when {
            event.name.contains("Minos Inquisitor") -> {
                if (getSecondsPassed(lastLootShare) < 2 && !hasTrackedInq) {
                    hasTrackedInq = true
                    DianaTracker.trackItem("MINOS_INQUISITOR_LS", 1)
                    sleep(2000) {
                        hasTrackedInq = false
                    }
                }
                lastInqDeath = System.currentTimeMillis()
            }
            event.name.contains("King Minos") -> {
                if (getSecondsPassed(lastLootShare) < 2 && !hasTrackedKing) {
                    hasTrackedKing = true
                    DianaTracker.trackItem("KING_MINOS_LS", 1)
                    sleep(2000) {
                        hasTrackedKing = false
                    }
                }
                lastKingDeath = System.currentTimeMillis()
            }
            event.name.contains("Sphinx") -> {
                if (getSecondsPassed(lastLootShare) < 2 && !hasTrackedSphinx) {
                    hasTrackedSphinx = true
                    DianaTracker.trackItem("SPHINX_LS", 1)
                    sleep(2000) {
                        hasTrackedSphinx = false
                    }
                }
                lastSphinxDeath = System.currentTimeMillis()
            }
            event.name.contains("Manticore") -> {
                if (getSecondsPassed(lastLootShare) < 2 && !hasTrackedManti) {
                    hasTrackedManti = true
                    DianaTracker.trackItem("MANTICORE_LS", 1)
                    sleep(2000) {
                        hasTrackedManti = false
                    }
                }
                lastMantiDeath = System.currentTimeMillis()
            }
        }

        if (dist <= 30) {
            allowSackTracking = true
            lastDianaMobDeath = System.currentTimeMillis()
        }
    }

    @SboEvent
    fun onGuiClose(event: GuiCloseEvent) {
        sleep(200) {
            currentScreen = null
        }
    }

    @SboEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        sleep(200) {
            currentScreen = event.screen
            if (event.screen.title.string == "Sack of Sacks") allowSackTracking = false
        }
    }

    /**
     * Sleeps for the specified number of milliseconds and then executes the callback.
     * This is done in a separate thread to avoid blocking the main thread.
     *
     * @param milliseconds The number of milliseconds to sleep.
     * @param callback The function to execute after sleeping.
     */
    fun sleep(milliseconds: Long, callback: () -> Unit) {
        SBO_CALLBACK_THREAD.execute {
            Thread.sleep(milliseconds)
            callback()
        }
    }

    fun getPlayerName(player: String): String {
        var name = player
        val num = name.indexOf(']')
        if (num != -1) {
            name = name.substring(num + 2)
        }
        name = name.replace(Regex("§[0-9a-fk-or]"), "")
        name = name.replace(Regex("[^a-zA-Z0-9_]"), "")
        return name.trim()
    }
    
    /**
     * Calculate percentage of one property to another.
     * If [mobName] is provided, it calculates the percentage of [propertyName] from [items] to [mobName] from [mobs].
     * If [mobName] is null, it calculates the percentage of [propertyName] from [mobs] to total mobs.
     */
    fun calcPercentOne(items: DianaItemsData, mobs: DianaMobsData, propertyName: String, mobName: String? = null): String {
        val result: Double = if (mobName != null) {
            val itemCount = items::class.memberProperties.firstOrNull { it.name == propertyName }
                ?.call(items) as? Int ?: 0
            val mobCount = mobs::class.memberProperties.firstOrNull { it.name == mobName }
                ?.call(mobs) as? Int ?: 0

            if (mobCount <= 0) 0.0
            else (itemCount.toDouble() / mobCount.toDouble() * 100)
        } else {
            val mobCount = mobs::class.memberProperties.firstOrNull { it.name == propertyName }
                ?.call(mobs) as? Int ?: 0
            val totalMobsCount = mobs.TOTAL_MOBS

            if (totalMobsCount <= 0) 0.0
            else (mobCount.toDouble() / totalMobsCount.toDouble() * 100)
        }
        return "%.2f".format(Locale.US, result)
    }

    fun formatNumber(number: Number?, withCommas: Boolean = false): String {
        val num = number?.toDouble() ?: 0.0

        if (withCommas) {
            // Format with commas
            val formatter = DecimalFormat("#,###")
            return formatter.format(num)
        } else {
            // Format with suffixes (k, m, b)
            return when {
                num >= 1_000_000_000 -> "%.2fb".format(num / 1_000_000_000)
                num >= 1_000_000 -> "%.1fm".format(num / 1_000_000)
                num >= 1_000 -> "%.1fk".format(num / 1_000)
                else -> "%.0f".format(num)
            }
        }
    }

    fun formatTime(milliseconds: Long): String {
        if (milliseconds <= 0) {
            return "0s"
        }

        val totalSeconds = (milliseconds / 1000).toInt()
        val totalMinutes = totalSeconds / 60
        val totalHours = totalMinutes / 60
        val days = totalHours / 24
        val hours = totalHours % 24
        val minutes = totalMinutes % 60
        val seconds = totalSeconds % 60

        val builder = StringBuilder()

        if (days > 0) {
            builder.append("${days}d ")
        }
        if (hours > 0 || days > 0) {
            builder.append("${hours}h ")
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            builder.append("${minutes}m ")
        }
        if (builder.isEmpty()) {
            builder.append("${seconds}s")
        }

        return builder.toString().trim()
    }

    private val COLOR_REGEX: Regex = Regex("§.")

    fun String.removeFormatting(): String {
        return this.replace(COLOR_REGEX, "")
    }

    fun Component.removeFormatting(): String {
        return this.string.replace(COLOR_REGEX, "")
    }

    fun matchLvlToColor(lvl: Int): String {
        return when {
            lvl >= 480 -> "§4$lvl"
            lvl >= 440 -> "§c$lvl"
            lvl >= 400 -> "§6$lvl"
            lvl >= 360 -> "§5$lvl"
            lvl >= 320 -> "§d$lvl"
            lvl >= 280 -> "§9$lvl"
            lvl >= 240 -> "§3$lvl"
            lvl >= 200 -> "§b$lvl"
            else -> "§7$lvl"
        }
    }

    fun getNumberColor(number: Int, range: Int): String {
        return when (number) {
            range -> "§c$number"
            range - 1 -> "§6$number"
            else -> "§9$number"
        }
    }

    fun getGriffinItemColor(item: String?): String {
        if (item.isNullOrEmpty()) return ""
        val name = item.replace("PET_ITEM_", "").replace("_", " ").replaceFirstChar { it.uppercase() }
        return when (name) {
            "Four Eyed Fish" -> "§5$name"
            "Dwarf Turtle Shelmet" -> "§a$name"
            "Crochet Tiger Plushie" -> "§5$name"
            "Antique Remedies" -> "§5$name"
            "Lucky Clover" -> "§a$name"
            "Minos Relic" -> "§5$name"
            else -> "§7$name"
        }
    }

    fun getRarity(item: String): String {
        return when (item.lowercase().trim()) {
            "common" -> "§f$item"
            "uncommon" -> "§a$item"
            "rare" -> "§9$item"
            "epic" -> "§5$item"
            "legendary" -> "§6$item"
            "mythic" -> "§d$item"
            else -> "§7$item"
        }
    }

    fun matchDianaKillsToColor(kills: Int): String {
        return when {
            kills >= 200_000 -> "§6${formatNumber(kills, true)}"
            kills >= 150_000 -> "§e${formatNumber(kills, true)}"
            kills >= 100_000 -> "§c${formatNumber(kills, true)}"
            kills >= 75_000 -> "§d${formatNumber(kills, true)}"
            kills >= 50_000 -> "§9${formatNumber(kills, true)}"
            kills >= 25_000 -> "§a${formatNumber(kills, true)}"
            kills >= 10_000 -> "§2${formatNumber(kills, true)}"
            else -> "§7${formatNumber(kills, true)}"
        }
    }

    fun getPurse(): Long {
        val lines = ScoreBoard.getLines()
        if (lines.isEmpty()) return 0L
        val purseLine = lines.find { it.contains("Purse: ") }
        return if (purseLine != null) {
            val purseValue = purseLine.substringAfter("Purse: ")
            val numericValue = purseValue.split(" ")[0]
            numericValue.replace(",", "").toLongOrNull() ?: 0L
        } else {
            0L
        }
    }

    fun getCursorItemStack(): ItemStack? {
        val handler = mc.player?.containerMenu ?: return null
        return handler.carried
    }

    fun readPlayerInv(): MutableMap<String, Item> {
        val inventory = Player.getPlayerInventory()
        val invItems = mutableMapOf<String, Item>()

        if (getCursorItemStack()?.count != 0) return prevInv

        for (slot in 0 until (inventory.size - 5)) {
            if (slot == 8) continue // Skip SB Star
            val stack: ItemStack = inventory[slot]

            if (!stack.isEmpty) {
                val customData = stack.get(DataComponents.CUSTOM_DATA)
                var id: String
                var item: Item
                val nbt = customData?.copyTag()
                val sbId = ItemUtils.getSBID(customData)
                // print for debugging the lore lines
                var isChimera = false
                if (sbId == "ENCHANTED_BOOK") {
                    val lore = ItemUtils.getLoreList(stack)
                    for (line in lore) {
                        if (line.contains("Chimera")) {
                            isChimera = true
                            break
                        }
                    }
                }

                if (!isChimera) {
                    item = Item(
                        sbId,
                        ItemUtils.getUUID(customData),
                        ItemUtils.getDisplayName(stack),
                        ItemUtils.getTimestamp(customData),
                        stack.count
                    )
                    id = if (item.itemUUID != "") item.itemUUID else item.itemId
                } else {
                    item = Item(
                        "CHIMERA",
                        ItemUtils.getUUID(customData),
                        "§d§lChimera",
                        ItemUtils.getTimestamp(customData),
                        stack.count
                    )
                    id = "CHIMERA"
                }

                if (invItems.containsKey(id)) {
                    invItems[id]?.count += item.count
                } else {
                    invItems[id] = item
                }
            }
        }
        prevInv = invItems
        return invItems
    }

    fun timestampToDate(timestamp: Long): String {
        if (timestamp <= 0) return "Unknown"
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(date)
    }

    fun toUpperSnakeCase(input: String): String {
        return input.replace("-", " ").split(" ").joinToString("_") { it.uppercase() }
    }

    fun getSecondsPassed(timestamp: Long): Long {
        return (System.currentTimeMillis() - timestamp) / 1000
    }

    fun playerHasItem(sbId: String): Boolean {
        val inv = Player.getPlayerInventory()
        for (i in inv.indices) {
            val stack = inv[i]
            if (!stack.isEmpty && ItemUtils.getSBID(stack.get(DataComponents.CUSTOM_DATA)) == sbId) {
                return true
            }
        }
        return false
    }

    private fun hasMythologicalRitualActive(): Boolean = Mayor.mayor == "Jerry" || Mayor.mayor == "Aura" || Mayor.ministerPerk == "Mythological Ritual" || Mayor.perks.contains("Mythological Ritual")

    fun checkDiana(): Boolean = Debug.itsAlwaysDiana || hasSpade && hasMythologicalRitualActive() && World.getWorld() == "Hub"

    fun getGuiName(): String {
        return currentScreen?.title?.string ?: ""
    }

    fun showTitle(title: String?, subtitle: String?, fadeIn: Int, time: Int, fadeOut: Int) {
        mc.gui.apply {
            setTimes(fadeIn, time, fadeOut)
            if (title != null)
                setTitle(Component.nullToEmpty(title))
            if (subtitle != null)
                setSubtitle(Component.nullToEmpty(subtitle))
        }
    }

    fun checkCustomDropMessage(dropName: String, magicFind: Int): Pair<Boolean, String> {
        val info = getDropInfo(dropName) ?: return Pair(false, "")

        if (!info.isEnabled) return Pair(false, "")

        val resultText = info.template
            .replace("{amount}", info.totalAmount.toString())
            .replace("{percentage}", "%.2f".format(info.percentage) + "%")
            .replace("{mf}", if (magicFind > 0) "$magicFind" else "")
            .replace('&', '§')
            .replace("+ ✯ Magic Find ", "") // prevent nonsense magic find when hypixel doesn't put it into the message (mob killed by someone else)

        return Pair(true, resultText)
    }

    data class DropInfo(
        val template: String,
        val isEnabled: Boolean,
        val totalAmount: Int,
        val mobCount: Int,
        val dropCount: Int
    ) {
        val percentage: Double
            get() = if (mobCount > 0) (dropCount.toDouble() / mobCount) * 100 else 0.0
    }

    private fun getDropInfo(dropName: String): DropInfo? {
        val tracker = SboDataObject.dianaTrackerMayor
        val items = tracker.items
        val mobs = tracker.mobs

        return when (dropName.lowercase()) {
            "chimera" -> DropInfo(Diana.customChimMessage[0].trim(), Diana.chimMessageBool, items.CHIMERA + items.CHIMERA_LS, mobs.MINOS_INQUISITOR, items.CHIMERA)
            "core" -> DropInfo(Diana.customCoreMessage[0].trim(), Diana.coreMessageBool, items.MANTI_CORE + items.MANTI_CORE_LS, mobs.MANTICORE, items.MANTI_CORE)
            "stinger" -> DropInfo(Diana.customStingerMessage[0].trim(), Diana.stingerMessageBool, items.FATEFUL_STINGER + items.FATEFUL_STINGER_LS, mobs.MANTICORE, items.FATEFUL_STINGER)
            "brain food" -> DropInfo(Diana.customBfMessage[0].trim(), Diana.bfMessageBool, items.BRAIN_FOOD + items.BRAIN_FOOD_LS, mobs.SPHINX, items.BRAIN_FOOD)
            "wool" -> DropInfo(Diana.customWoolMessage[0].trim(), Diana.woolMessageBool, items.SHIMMERING_WOOL + items.SHIMMERING_WOOL_LS, mobs.KING_MINOS, items.SHIMMERING_WOOL)
            else -> null
        }
    }

    fun getSpawnMessage(message: String, mob: String): String {
        val mobs = SboDataObject.dianaTrackerMayor.mobs
        val items = SboDataObject.dianaTrackerMayor.items
        val sboData = SboDataObject.sboData

        val kingPercent = calcPercentOne(items, mobs, "KING_MINOS")
        val manticorePercent = calcPercentOne(items, mobs, "MANTICORE")
        val inqPercent = calcPercentOne(items, mobs, "MINOS_INQUISITOR")
        val sphinxPercent = calcPercentOne(items, mobs, "SPHINX")

        when (mob.lowercase()) {
            "minos inquisitor", "inq" -> {
                val since = sboData.mobsSinceInq
                return message.replace("{since}", since.toString()).replace("{chance}", inqPercent)
            }

            "king minos", "king" -> {
                val since = sboData.mobsSinceKing
                return message.replace("{since}", since.toString()).replace("{chance}", kingPercent)
            }

            "sphinx" -> {
                val since = sboData.mobsSinceSphinx
                return message.replace("{since}", since.toString()).replace("{chance}", sphinxPercent)
            }

            "manticore", "manti" -> {
                val since = sboData.mobsSinceManti
                return message.replace("{since}", since.toString()).replace("{chance}", manticorePercent)
            }
            else -> return ""
        }
    }

    fun toTitleCase(input: String): String {
        return input.lowercase().replaceFirstChar { char -> char.uppercase() }
    }

    fun getMagicFind(mf: String): Int {
        val mfMatch = Regex("""§b\(\+§b(\d+)""").find(mf)
        if (mfMatch != null) {
            val mfValue = mfMatch.groupValues[1].toIntOrNull() ?: 0
            return mfValue
        }
        return 0
    }

    fun updateItemPriceInfo() {
        Http.sendGetRequest("https://api.skyblockoverhaul.com/ahItems")
            .toJson<List<Map<String, Map<String, Long>>>>(true) { json ->
                priceDataAh = json.flatMap { it.entries }.associate { it.key to it.value["price"]!! }
                DianaLoot.updateLines()
            }.error { error ->
                if (priceDataAh.isEmpty()) {
                    // no price data available - notify user
                    Chat.chat("§6[SBO] §4Unexpected error while fetching AH item prices: $error")
                } else {
                    // if a previous request succeeded and this request failed, it might be temporary and we still
                    // have some price data even if outdated. so only log to logs
                    SBOKotlin.logger.error("Unexpected error while fetching AH item prices", error)
                }
            }
        Http.sendGetRequest("https://api.hypixel.net/skyblock/bazaar?product")
            .toJson<HypixelBazaarResponse>(true) {
                priceDataBazaar = it
                DianaLoot.updateLines()
            }.error { error ->
                if (priceDataBazaar == null) {
                    // no price data available - notify user
                    Chat.chat("§6[SBO] §4Unexpected error while fetching Bazaar item prices: $error")
                } else {
                    // if a previous request succeeded and this request failed, it might be temporary and we still
                    // have some price data even if outdated. so only log to logs
                    SBOKotlin.logger.error("Unexpected error while fetching Bazaar item prices", error)
                }
            }
    }


    fun getItemPrice(sbId: String, amount: Int = 1): Long {
        val id = when {
            sbId == "CHIMERA" -> "ENCHANTMENT_ULTIMATE_CHIMERA_1"
            sbId.endsWith("_SHARD") || sbId.endsWith("_DYE") -> {
                val suffix = sbId.substringAfterLast('_')   // "SHARD"
                val name = sbId.substringBeforeLast('_')   // "WITHER"
                "${suffix}_${name}"
            }
            else -> sbId
        }

        val bzProduct = priceDataBazaar?.products?.get(id)
        val bazaarPrice = if (Diana.bazaarSettingDiana == Diana.SettingDiana.INSTASELL) {
            bzProduct?.quick_status?.sellPrice
        } else {
            bzProduct?.quick_status?.buyPrice
        }

        if (bazaarPrice != null && bazaarPrice > 0.0) {
            return (bazaarPrice * amount).roundToLong()
        }

        val ahPrice = priceDataAh[id]?.toDouble() ?: 0.0
        val npcPrice = npcSellValueMap[id]?.toDouble() ?: 0.0

        val bestUnitPrice = if (npcPrice > ahPrice) npcPrice else ahPrice

        return (bestUnitPrice * amount).roundToLong()
    }

    fun getItemPriceFormatted(sbId: String, amount: Int = 1): String {
        val price = getItemPrice(sbId, amount)
        return formatNumber(price)
    }

    /**
     * Checks if the player has received loot share recently.
     * @param timeframe The timeframe in seconds to check against. Default is 2 seconds.
     */
    fun gotLootShareRecently(timeframe: Long = 2): Boolean {
        return getSecondsPassed(lastLootShare) <= timeframe
    }

    fun dianaMobDiedRecently(seconds: Long = 2): Boolean {
        return getSecondsPassed(lastDianaMobDeath) <= seconds
    }

    fun getBurrowsPerHr(tracker: DianaTrackerDataClass, timer: SboTimerManager.SBOTimer): Double {
        val hours = timer.getHourTime()
        if (hours <= 0.01) return 0.0
        val totalBurrows = tracker.items.TOTAL_BURROWS.toDouble()
        val burrowsPerHr = totalBurrows / hours
        return BigDecimal(burrowsPerHr).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun getMobsPerHr(tracker: DianaTrackerDataClass, timer: SboTimerManager.SBOTimer): Double {
        val hours = timer.getHourTime()
        if (hours <= 0.01) return 0.0
        val totalMobs = tracker.mobs.TOTAL_MOBS.toDouble()
        val mobsPerHr = totalMobs / hours
        return BigDecimal(mobsPerHr).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun getChance(mf: Int, looting: Int,rarity: String, lootshare: Boolean = false): Map<String, Double> {
        val baseChances: Map<String, Double> = when (rarity.lowercase().trim()) {
            "epic" -> mapOf("stick" to 0.0004, "relic" to 0.0002)
            "legendary" -> mapOf("chim" to 0.01, "stick" to 0.0006, "relic" to 0.0003, "food" to 0.0025)
            "mythic" -> mapOf("chim" to 0.0125, "stick" to 0.0008, "relic" to 0.0004, "food" to 0.005, "wool" to 0.002, "core" to 0.002, "stinger" to 0.005)
            else -> mapOf("chim" to 0.0, "stick" to 0.0, "relic" to 0.0, "food" to 0.0, "wool" to 0.0, "core" to 0.0, "stinger" to 0.0)
        }
        val multiplier = 1 + mf / 100.0
        if (lootshare) {
            val factor = multiplier / 5
            return baseChances.mapValues { it.value * factor }
        }
        val lootingMultiplier = 1 + looting * 0.15
        return baseChances.mapValues { it.value * multiplier * lootingMultiplier }
    }

    fun formatChances(chance: Double, label: String): String {
        val percent = String.format("%.2f", chance * 100)
        val fraction = " §7(§b1/${(1 / chance).roundToInt()}§7)"
        return "§eChance: §b$percent%$fraction $label"
    }

    fun getMagicFindAndLooting(mf: Int, looting: Int): String {
        return " §7[MF:$mf] [L:$looting]"
    }

    fun getKillsFromLore(stack: ItemStack?): Int {
        if (stack == null || stack.isEmpty) return 0

        val linesList: List<Component> = stack.get(DataComponents.LORE)?.lines ?: listOf()

        for (lineText in linesList) {
            val line = lineText.string
            if (line.startsWith("Kills: ")) {
                val numberString = line.substringAfter("Kills: ")
                    .replace(",", "")
                    .trim()

                return numberString.toIntOrNull() ?: 0
            }
        }

        return 0
    }
}


