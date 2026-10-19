package net.sbo.mod.utils

import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.DianaTracker
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.*
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.entity.DianaMobDeathEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.events.impl.guis.GuiCloseEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.game.ItemLookup
import net.sbo.mod.utils.game.ScoreBoard
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.http.Http
import net.sbo.mod.utils.http.SboApi
import net.sbo.mod.utils.time.TimeUtil
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.math.SboVec.Companion.toSboVec
import net.sbo.mod.utils.waypoint.WaypointManager.removeNearbyRareMobWaypointAt
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.reflect.full.memberProperties
import net.sbo.mod.utils.data.configs.diana.DianaItemsData
import net.sbo.mod.utils.data.configs.diana.DianaMobsData
import net.sbo.mod.utils.data.configs.npcSellValueMap
import net.sbo.mod.utils.data.configs.diana.DianaTracker as DianaTrackerDataClass

object Helper {
    private val MF_REGEX = Regex("""§b\(\+§b(\d+)""")

    private var lastLootShare: Long = 0L
    var allowSackTracking: Boolean = true
    var hasSpade: Boolean = false
    private var lastDianaMobDeath: Long = 0L
    private var lastInqDeath: Long = 0L
    private var lastKingDeath: Long = 0L
    private var lastSphinxDeath: Long = 0L
    private var lastMantiDeath: Long = 0L

    private var hasTrackedInq: Boolean = false
    private var hasTrackedKing: Boolean = false
    private var hasTrackedSphinx: Boolean = false
    private var hasTrackedManti: Boolean = false

    private var prevInv = mutableMapOf<String, Item>()
    private var priceDataAh: Map<String, Long> = emptyMap()
    private var lastAhFetch = 0L
    private var priceDataBazaar: HypixelBazaarResponse? = null
    private val ACTIVE_USERS_INTERVAL = TimeUnit.MINUTES.toNanos(4L)
    private var lastActiveUsersCount = 0L

    private val SBO_CALLBACK_THREAD: ExecutorService = Executors.newThreadPerTaskExecutor(Thread
            .ofVirtual()
            .name("sbo-callback-thread-", 1) // sbo-callback-thread-1, sbo-callback-thread-2 etc. starting from 1 (second parameter)
            .factory() // virtual threads are daemon by default
    )

    private fun onLootShare() {
        lastLootShare = System.nanoTime()
    }

    private fun notifyUserOfLs(mob: String) {
        if (Diana.assumeAllLS) {
            Chat.chat("§6[SBO] §eRegistered Lootshare $mob!", true)
        }
    }

    fun init() {
        Register.onChatMessageCancelable(Pattern.compile("^§e§lLOOT SHARE §fYou received .*? for assisting .*?!?$", Pattern.DOTALL)) { message, matchResult ->
            onLootShare()
            true
        }

        Register.onTick(20) { // maybe better way to register this
            hasSpade = playerHasItem("DEIFIC_SPADE") || playerHasItem("ARCHAIC_SPADE") || playerHasItem("ANCESTRAL_SPADE")
        }

        Register.onTick(20 * 60 * 5) {
            updateBazaarPrices()
        }

        Register.onTick(20 * 60 * 30) {
            updateAhPrices()
        }

        Register.onTick(20) {
            val now = System.nanoTime()
            if (now - lastActiveUsersCount > ACTIVE_USERS_INTERVAL && World.isInSkyblock()) {
                lastActiveUsersCount = now
                countActiveUser()
            }
        }

        /*Register.command("sbotestlschimdrop") {
            handleDianaMobDeath("Minos Inquisitor", 30.0f)
            DianaTracker.trackChatRngDrop("Enchanted Book (Chimera 1) §b(+§b566 ✯ Magic Find)")
        }

        Register.command("sbotestdrops") {
            DianaTracker.trackChatRngDrop("Daedalus Stick §b(+§b483 ✯ Magic Find)")
            DianaTracker.trackChatRngDrop("Minos Relic §b(+§b483 ✯ Magic Find)")
            DianaTracker.trackChatRngDrop("Crown of Greed")
            DianaTracker.trackChatRngDrop("Myth the Fish")
            DianaTracker.trackChatRngDrop("Daedalus Stick")
            DianaTracker.trackChatRngDrop("Shimmering Wool §b(+§b483 ✯ Magic Find)")
        }

        Register.command("sbotestbraideddrop") {
            DianaTracker.onBraidedDrop()
        }

        Register.command("sbotestchimdrop") {
            DianaTracker.trackChatRngDrop("Enchanted Book (Chimera 1) §b(+§b566 ✯ Magic Find)")
        }*/

        // Workaround to not timeout on the first requests since there is lot of work being done that steals CPU time from our HTTP threads during game startup. Wait after at least 1 second of virtual thread carrier threads being available, and runAsync's ForkJoinPool task queue to become empty to be able to run our task.
        sleep(1000) {
            CompletableFuture.runAsync {
                updateItemPriceInfo()
            }
        }
    }

    // AH prices load once at startup and then every 30 minutes, so a failed first load is retried on the next join
    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (priceDataAh.isEmpty() && System.currentTimeMillis() - lastAhFetch > 60_000) updateAhPrices()
    }

    @SboEvent
    fun onDianaMobDeath(event: DianaMobDeathEvent) {
        handleDianaMobDeath(event.name, event.entity.distanceTo(mc.player!!), event.entity.blockPosition().toSboVec())
    }

    private fun handleDianaMobDeath(name: String, dist: Float, pos: SboVec) {
        val nearby = dist <= 30
        val last = DianaTracker.lastSpawnedMob
        val lsOverride = Diana.assumeAllLS && nearby && (last == null || !name.contains(last)) // we need to check if dying mob is not spawned by user by comparing to last spawned mob to avoid counting self-mob as lootshare
        when {
            name.contains("Minos Inquisitor") -> {
                announceDianaMobKillTime("Minos Inquisitor", nearby)
                removeNearbyRareMobWaypointAt(pos)
                if (lsOverride) onLootShare() // makes the gotLootShareRecently condition below always pass
                if (gotLootShareRecently() && !hasTrackedInq) {
                    hasTrackedInq = true
                    notifyUserOfLs("Minos Inquisitor")
                    DianaTracker.trackItem("MINOS_INQUISITOR_LS", 1)
                    sleep(2000) {
                        hasTrackedInq = false
                    }
                }
                lastInqDeath = System.nanoTime()
            }
            name.contains("King Minos") -> {
                announceDianaMobKillTime("King Minos", nearby)
                removeNearbyRareMobWaypointAt(pos)
                if (lsOverride) onLootShare() // makes the gotLootShareRecently condition below always pass
                if (gotLootShareRecently() && !hasTrackedKing) {
                    hasTrackedKing = true
                    notifyUserOfLs("King Minos")
                    DianaTracker.trackItem("KING_MINOS_LS", 1)
                    sleep(2000) {
                        hasTrackedKing = false
                    }
                }
                lastKingDeath = System.nanoTime()
            }
            name.contains("Sphinx") -> {
                announceDianaMobKillTime("Sphinx", nearby)
                removeNearbyRareMobWaypointAt(pos)
                if (gotLootShareRecently() && !hasTrackedSphinx) {
                    hasTrackedSphinx = true
                    notifyUserOfLs("Sphinx")
                    DianaTracker.trackItem("SPHINX_LS", 1)
                    sleep(2000) {
                        hasTrackedSphinx = false
                    }
                }
                lastSphinxDeath = System.nanoTime()
            }
            name.contains("Manticore") -> {
                announceDianaMobKillTime("Manticore", nearby)
                removeNearbyRareMobWaypointAt(pos)
                if (lsOverride) onLootShare() // makes the gotLootShareRecently condition below always pass
                if (gotLootShareRecently() && !hasTrackedManti) {
                    hasTrackedManti = true
                    notifyUserOfLs("Manticore")
                    DianaTracker.trackItem("MANTICORE_LS", 1)
                    sleep(2000) {
                        hasTrackedManti = false
                    }
                }
                lastMantiDeath = System.nanoTime()
            }
        }

        if (nearby) {
            allowSackTracking = true
            lastDianaMobDeath = System.nanoTime()
        }
    }

    private fun announceDianaMobKillTime(mob: String, nearby: Boolean) {
        val elapsedMillis = DianaTracker.consumeRareMobSpawnTime(mob)
        if (nearby && Diana.sendKillTimeMessage && elapsedMillis >= 0L) {
            Chat.chat("§6[SBO] §eTook §6${formatTime(elapsedMillis, decimalSeconds = true)} §eto kill the $mob!")
        }
    }

    @SboEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        sleep(200) {
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

    private val PLAYER_NAME = Regex("^[A-Za-z0-9_]{1,16}$")
    private val NON_ALPHANUMERIC = Regex("[^a-zA-Z0-9_]")

    /** A valid Minecraft name, so putting it into a command can't add anything else. */
    fun isPlayerName(name: String): Boolean = PLAYER_NAME.matches(name)

    fun getPlayerName(player: String): String {
        var name = player
        val num = name.indexOf(']')
        if (num != -1) {
            name = name.substring(num + 2)
        }
        name = name.removeFormatting()
        name = name.replace(NON_ALPHANUMERIC, "")
        return name.trim()
    }

    /**
     * Calculate percentage of one property to another.
     * If [mobName] is provided, it calculates the percentage of [propertyName] from [items] to [mobName] from [mobs].
     * If [mobName] is null, it calculates the percentage of [propertyName] from [mobs] to total mobs.
     */
    fun calcPercentOneReflectively(items: DianaItemsData, mobs: DianaMobsData, propertyName: String, mobName: String? = null): String {
        return if (mobName != null) {
            calcPercentOne(getItemCount(items, propertyName), getMobCount(mobs, mobName))
        } else {
            calcPercentOne(getMobCount(mobs, propertyName), mobs.TOTAL_MOBS)
        }
    }

    private fun getItemCount(items: DianaItemsData, propertyName: String): Int =
        ITEM_COUNT_GETTERS[propertyName]?.invoke(items)
            ?: (ITEM_PROPERTIES[propertyName]?.call(items) as? Int)
            ?: 0

    private fun getMobCount(mobs: DianaMobsData, propertyName: String): Int =
        MOB_COUNT_GETTERS[propertyName]?.invoke(mobs)
            ?: (MOB_PROPERTIES[propertyName]?.call(mobs) as? Int)
            ?: 0

    private val ITEM_PROPERTIES = DianaItemsData::class.memberProperties.associateBy { it.name }
    private val MOB_PROPERTIES = DianaMobsData::class.memberProperties.associateBy { it.name }

    private val ITEM_COUNT_GETTERS: Map<String, (DianaItemsData) -> Int> = mapOf(
        "GRIFFIN_FEATHER" to { it.GRIFFIN_FEATHER }, "MYTHOS_FRAGMENT" to { it.MYTHOS_FRAGMENT },
        "CROWN_OF_GREED" to { it.CROWN_OF_GREED }, "WASHED_UP_SOUVENIR" to { it.WASHED_UP_SOUVENIR },
        "SHIMMERING_WOOL" to { it.SHIMMERING_WOOL }, "SHIMMERING_WOOL_LS" to { it.SHIMMERING_WOOL_LS },
        "MANTI_CORE" to { it.MANTI_CORE }, "MANTI_CORE_LS" to { it.MANTI_CORE_LS },
        "CHIMERA" to { it.CHIMERA }, "CHIMERA_LS" to { it.CHIMERA_LS },
        "BRAIN_FOOD" to { it.BRAIN_FOOD }, "BRAIN_FOOD_LS" to { it.BRAIN_FOOD_LS },
        "FATEFUL_STINGER" to { it.FATEFUL_STINGER }, "FATEFUL_STINGER_LS" to { it.FATEFUL_STINGER_LS },
        "BRAIDED_GRIFFIN_FEATHER" to { it.BRAIDED_GRIFFIN_FEATHER }, "DAEDALUS_STICK" to { it.DAEDALUS_STICK },
        "CRETAN_URN" to { it.CRETAN_URN }, "DWARF_TURTLE_SHELMET" to { it.DWARF_TURTLE_SHELMET },
        "ANTIQUE_REMEDIES" to { it.ANTIQUE_REMEDIES }, "CROCHET_TIGER_PLUSHIE" to { it.CROCHET_TIGER_PLUSHIE },
        "ENCHANTED_ANCIENT_CLAW" to { it.ENCHANTED_ANCIENT_CLAW }, "ANCIENT_CLAW" to { it.ANCIENT_CLAW },
        "MINOS_RELIC" to { it.MINOS_RELIC }, "ENCHANTED_GOLD" to { it.ENCHANTED_GOLD },
        "HILT_OF_REVELATIONS" to { it.HILT_OF_REVELATIONS }, "KING_MINOS_SHARD" to { it.KING_MINOS_SHARD },
        "SPHINX_SHARD" to { it.SPHINX_SHARD }, "MINOTAUR_SHARD" to { it.MINOTAUR_SHARD },
        "CRETAN_BULL_SHARD" to { it.CRETAN_BULL_SHARD }, "HARPY_SHARD" to { it.HARPY_SHARD },
        "MYTHOLOGICAL_DYE" to { it.MYTHOLOGICAL_DYE }, "MYTH_THE_FISH" to { it.MYTH_THE_FISH }
    )

    private val MOB_COUNT_GETTERS: Map<String, (DianaMobsData) -> Int> = mapOf(
        "KING_MINOS" to { it.KING_MINOS }, "MANTICORE" to { it.MANTICORE },
        "MINOS_INQUISITOR" to { it.MINOS_INQUISITOR }, "SPHINX" to { it.SPHINX },
        "MINOS_CHAMPION" to { it.MINOS_CHAMPION }, "MINOTAUR" to { it.MINOTAUR },
        "GAIA_CONSTRUCT" to { it.GAIA_CONSTRUCT }, "HARPY" to { it.HARPY },
        "CRETAN_BULL" to { it.CRETAN_BULL }, "STRANDED_NYMPH" to { it.STRANDED_NYMPH },
        "SIAMESE_LYNXES" to { it.SIAMESE_LYNXES }, "MINOS_HUNTER" to { it.MINOS_HUNTER },
        "TOTAL_MOBS" to { it.TOTAL_MOBS },
        "MINOS_INQUISITOR_LS" to { it.MINOS_INQUISITOR_LS }, "KING_MINOS_LS" to { it.KING_MINOS_LS },
        "MANTICORE_LS" to { it.MANTICORE_LS }, "SPHINX_LS" to { it.SPHINX_LS }
    )

    fun calcPercentOne(itemCount: Int, mobCount: Int): String {
        val result = if (mobCount <= 0) {
            0.0
        } else {
            itemCount.toDouble() / mobCount * 100
        }

        return PERCENT_FORMAT.format(result)
    }

    private val PERCENT_FORMAT = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US)).apply {
        roundingMode = RoundingMode.HALF_UP
    }
    private val NUMBER_FORMAT = DecimalFormat("#,###")
    private val BILLION_FORMAT = DecimalFormat("0.00b")
    private val MILLION_FORMAT = DecimalFormat("0.0m")
    private val THOUSAND_FORMAT = DecimalFormat("0.0k")
    private val INTEGER_FORMAT = DecimalFormat("0")

    fun formatNumber(number: Number?, withCommas: Boolean = false): String {
        val num = number?.toDouble() ?: 0.0

        return if (withCommas) {
            NUMBER_FORMAT.format(num)
        } else {
            when {
                num >= 1_000_000_000 -> BILLION_FORMAT.format(num / 1_000_000_000)
                num >= 1_000_000 -> MILLION_FORMAT.format(num / 1_000_000)
                num >= 1_000 -> THOUSAND_FORMAT.format(num / 1_000)
                else -> INTEGER_FORMAT.format(num)
            }
        }
    }

    fun formatTime(milliseconds: Long, decimalSeconds: Boolean = false): String {
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
            if (decimalSeconds) {
                builder.append("${"%.1f".format(Locale.ROOT, milliseconds / 1000.0)}s")
            } else {
                builder.append("${seconds}s")
            }
        }

        return builder.toString().trim()
    }

    /**
     * Adapted from SkyHanni code, uses performant StringBuilder and native String operations rather than using a Regex to remove all formatting characters and the characters coming directly after formatting characters.
     */
    fun String.removeFormatting(): String {
        val length = length
        if (length == 0) return ""

        var nextFormattingSequence = indexOf('§')
        if (nextFormattingSequence == -1) return this

        val cleanedString = StringBuilder(length - 1)
        var readIndex = 0

        while (nextFormattingSequence != -1) {
            cleanedString.append(this, readIndex, nextFormattingSequence)

            readIndex = minOf(nextFormattingSequence + 2, length)
            nextFormattingSequence = indexOf('§', readIndex)
        }

        cleanedString.append(this, readIndex, length)
        return cleanedString.toString()
    }

    fun Component.removeFormatting(): String = this.string.removeFormatting()

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

    private fun getCursorItemStack(): ItemStack? {
        val handler = mc.player?.containerMenu ?: return null
        return handler.carried
    }

    fun readPlayerInv(): Map<String, Item> {
        val inventory = Player.getPlayerInventory()?.getNonEquipmentItems() ?: return emptyMap<String, Item>()
        val invItems = mutableMapOf<String, Item>()

        if (getCursorItemStack()?.count != 0) return prevInv

        for ((slot, stack) in inventory.withIndex()) {
            if (slot == 8) continue // Skip SB Star
            val stack: ItemStack = inventory[slot]

            if (!stack.isEmpty) {
                var id: String
                var item: Item
                val lookup = ItemLookup(stack)
                val sbId = lookup.sbId
                var isChimera = false
                if (sbId == "ENCHANTED_BOOK") {
                    val lore = lookup.cleanLoreList
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
                        lookup.uuid,
                        lookup.displayName,
                        lookup.timestamp,
                        stack.count
                    )
                    id = if (item.itemUUID != "") item.itemUUID else item.itemId
                } else {
                    item = Item(
                        "CHIMERA",
                        lookup.uuid,
                        "§d§lChimera",
                        lookup.timestamp,
                        stack.count
                    )
                    id = "CHIMERA"
                }

                val existing = invItems.putIfAbsent(id, item)

                if (existing != null) {
                    existing.count += item.count
                }
            }
        }
        prevInv = invItems
        return invItems
    }

    fun toUpperSnakeCase(input: String): String = input.replace("-", " ").split(" ").joinToString("_") { it.uppercase() }

    /**
     * Returns the number of seconds since an epoch timestamp.
     *
     * The timestamp must originate from System.currentTimeMillis() or another
     * Unix epoch source (e.g. Hypixel item NBT timestamp).
     *
     * For nanoTime, use {@link #getSecondsPassedSinceNano(Long)}
     */
    fun getSecondsPassed(timestamp: Long): Long = (TimeUtil.currentTimeMillis() - timestamp) / 1000

    private fun getSecondsPassedSinceNano(timestamp: Long): Long = (System.nanoTime() - timestamp) / TimeUnit.SECONDS.toNanos(1L)

    private fun playerHasItem(sbId: String): Boolean {
        val inventory = Player.getPlayerInventory() ?: return false
        return inventory.getNonEquipmentItems().any { stack -> !stack.isEmpty && ItemLookup(stack).sbId == sbId }
    }

    fun showTitle(title: String?, subtitle: String?, fadeIn: Int, time: Int, fadeOut: Int, overwrite: Boolean = true) {
        val currentDurationTicks = mc.gui.titleTime
        val currentTitle = mc.gui.title?.string
        val currentSubtitle = mc.gui.subtitle?.string

        if (overwrite || time >= currentDurationTicks) {
            mc.gui.setTimes(fadeIn, time, fadeOut)
        }

        if (title != null && (overwrite || currentTitle.isNullOrEmpty())) {
            mc.gui.setTitle(Component.nullToEmpty(title))
        }

        if (subtitle != null && (overwrite || currentSubtitle.isNullOrEmpty())) {
            mc.gui.setSubtitle(Component.nullToEmpty(subtitle))
        }
    }

    fun checkCustomDropMessage(dropName: String, magicFind: Int, isLootshare: Boolean, amountOverride: Int? = null): Pair<Boolean, String> {
        val info = getDropInfo(dropName) ?: return false to ""

        if (!info.isEnabled) return false to ""

        val resultText = info.template
            .replace("{amount}", (amountOverride ?: info.totalAmount).toString())
            .replace("{percentage}", "%.2f".format(info.percentage) + "%")
            .replace("{mf}", if (magicFind > 0) "$magicFind" else "")
            .replace("{lstext}", if (isLootshare) " (LS)" else "")
            .replace("{price}", getItemPriceFormatted(info.itemId))
            .replace('&', '§')
            .replace("{since}", getSinceDrop(dropName, isLootshare))
            .replace("+ ✯ Magic Find ", "") // prevent nonsense magic find when hypixel doesn't put it into the message (mob killed by someone else)

        return true to resultText
    }

    data class DropInfo(
        val template: String,
        val isEnabled: Boolean,
        val totalAmount: Int,
        val itemId: String,
        private val mobCount: Int,
        private val dropCount: Int
    ) {
        val percentage: Double
            get() = if (mobCount > 0) dropCount.toDouble() / mobCount * 100 else 0.0
    }

    private fun getCustomMessage(message: Array<out String>): String =
        message.firstOrNull { it.isNotBlank() }?.trim() ?: ""

    private fun getSinceDrop(dropName: String, isLootshare: Boolean): String {
        val data = DataManager.sboData
        return when (dropName.lowercase()) {
            "wool" -> if (isLootshare) data.kingSinceLsWool.toString() else data.kingSinceWool.toString()
            "core" -> if (isLootshare) data.mantiSinceLsCore.toString() else data.mantiSinceCore.toString()
            "stinger" -> if (isLootshare) data.mantiSinceLsCore.toString() else data.mantiSinceCore.toString()
            "chimera" -> if (isLootshare) data.inqsSinceLsChim.toString() else data.inqsSinceChim.toString()
            "brain food" -> if (isLootshare) data.sphinxSinceLsFood.toString() else data.sphinxSinceFood.toString()
            else -> "ErrorGettingSince"
        }
    }
  
    private fun getDropInfo(dropName: String): DropInfo? {
        val tracker = DataManager.dianaTrackerMayorData
        val items = tracker.items
        val mobs = tracker.mobs

        return when (dropName.lowercase()) {
            "chimera" -> {
                val message = getCustomMessage(Diana.customChimeraMessage)
                DropInfo(message, message.isNotEmpty(), items.CHIMERA + items.CHIMERA_LS, "CHIMERA", mobs.MINOS_INQUISITOR, items.CHIMERA)
            }

            "core" -> {
                val message = getCustomMessage(Diana.customManticoreMessage)
                DropInfo(message, message.isNotEmpty(), items.MANTI_CORE + items.MANTI_CORE_LS, "MANTI_CORE", mobs.MANTICORE, items.MANTI_CORE)
            }

            "stinger" -> {
                val message = getCustomMessage(Diana.customFatefulStingerMessage)
                DropInfo(message, message.isNotEmpty(), items.FATEFUL_STINGER + items.FATEFUL_STINGER_LS, "FATEFUL_STINGER", mobs.MANTICORE, items.FATEFUL_STINGER)
            }

            "brain food" -> {
                val message = getCustomMessage(Diana.customBrainFoodMessage)
                DropInfo(message, message.isNotEmpty(), items.BRAIN_FOOD + items.BRAIN_FOOD_LS, "BRAIN_FOOD", mobs.SPHINX, items.BRAIN_FOOD)
            }

            "wool" -> {
                val message = getCustomMessage(Diana.customShimmeringWoolMessage)
                DropInfo(message, message.isNotEmpty(), items.SHIMMERING_WOOL + items.SHIMMERING_WOOL_LS, "SHIMMERING_WOOL", mobs.KING_MINOS, items.SHIMMERING_WOOL)
            }

            else -> null
        }
    }

    fun getSpawnMessage(message: String, mob: String): String {
        val mobs = DataManager.dianaTrackerMayorData.mobs
        val items = DataManager.dianaTrackerMayorData.items
        val sboData = DataManager.sboData

        val kingPercent = calcPercentOne(mobs.KING_MINOS, mobs.TOTAL_MOBS)
        val manticorePercent = calcPercentOne(mobs.MANTICORE, mobs.TOTAL_MOBS)
        val inqPercent = calcPercentOne(mobs.MINOS_INQUISITOR, mobs.TOTAL_MOBS)
        val sphinxPercent = calcPercentOne(mobs.SPHINX, mobs.TOTAL_MOBS)

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

    fun toTitleCase(input: String): String = input.lowercase().replaceFirstChar { char -> char.uppercase() }

    fun getMagicFind(mf: String): Int {
        val mfMatch = MF_REGEX.find(mf)
        if (mfMatch != null) {
            return mfMatch.groupValues[1].toIntOrNull() ?: 0
        }
        return 0
    }

    /** Tells the backend this player is online, it counts everyone from the last 5 minutes. */
    private fun countActiveUser() {
        SboApi.countActiveUsers()
            .result { response ->
                if (!response.isSuccessful) {
                    SBOKotlin.logger.error("Failed to count active players: ${response.code} ${response.message}")
                }
            }
            .error { exception ->
                SBOKotlin.logger.error("Error while counting active players", exception)
            }
    }

    private fun updateItemPriceInfo() {
        updateBazaarPrices()
        updateAhPrices()
    }

    private fun updateBazaarPrices() {
        Http.sendGetRequest("https://api.hypixel.net/skyblock/bazaar?product")
            .toJson<HypixelBazaarResponse>(true) {
                priceDataBazaar = it
                DianaLoot.updateLines()
            }.error { error ->
                SBOKotlin.logger.error("Unexpected error while fetching Bazaar item prices", error)
            }
    }

    private fun updateAhPrices() {
        lastAhFetch = System.currentTimeMillis()
        SboApi.ahItems()
            .toJson<List<Map<String, Map<String, Long>>>>(true) { json ->
                priceDataAh = json.flatMap { it.entries }.associate { it.key to it.value["price"]!! }
                DianaLoot.updateLines()
            }.error { error ->
                SBOKotlin.logger.error("Unexpected error while fetching AH item prices", error)
            }
    }


    fun getItemPrice(sbId: String, amount: Int = 1): Long {
        val id = when {
            sbId == "CHIMERA" -> "ENCHANTMENT_ULTIMATE_CHIMERA_1"
            sbId.endsWith("_SHARD") || sbId.endsWith("_DYE") -> {
                val suffix = sbId.substringAfterLast('_')   // "SHARD"
                val name = sbId.substringBeforeLast('_')   // "WITHER"
                "${suffix}_$name"
            }
            else -> sbId
        }

        val bzProduct = priceDataBazaar?.products?.get(id)
        val bazaarPrice = if (Diana.bazaarSettingDiana == Diana.SettingDiana.INSTASELL) {
            bzProduct?.quick_status?.sellPrice ?: 0.0
        } else {
            bzProduct?.quick_status?.buyPrice ?: 0.0
        }

        val ahPrice = priceDataAh[id]?.toDouble() ?: 0.0
        val npcPrice = npcSellValueMap[id]?.toDouble() ?: 0.0

        val preferNpc =
            Diana.ironmanOverrides || Diana.npcPriceOverrides && (sbId == "CRETAN_URN" || sbId == "DWARF_TURTLE_SHELMET" || sbId == "ANTIQUE_REMEDIES" || sbId == "WASHED_UP_SOUVENIR" || sbId == "CROCHET_TIGER_PLUSHIE" || sbId == "HILT_OF_REVELATIONS")
        val bestMarketPrice = kotlin.math.max(bazaarPrice, ahPrice)
        val bestUnitPrice = if (npcPrice > bestMarketPrice || preferNpc) npcPrice else bestMarketPrice

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
    fun gotLootShareRecently(timeframe: Long = 2): Boolean = getSecondsPassedSinceNano(lastLootShare) <= timeframe

    fun dianaMobDiedRecently(seconds: Long = 2): Boolean = getSecondsPassedSinceNano(lastDianaMobDeath) <= seconds

    internal fun getBurrowsPerHr(tracker: DianaTrackerDataClass, timer: SboTimerManager.SBOTimer): Double {
        val hours = timer.getHourTime()
        if (hours <= 0.0) return 0.0
        val totalBurrows = tracker.items.TOTAL_BURROWS.toDouble()
        val burrowsPerHr = totalBurrows / hours
        return BigDecimal.valueOf(burrowsPerHr).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    internal fun getMobsPerHr(tracker: DianaTrackerDataClass, timer: SboTimerManager.SBOTimer): Double {
        val hours = timer.getHourTime()
        if (hours <= 0.0) return 0.0
        val totalMobs = tracker.mobs.TOTAL_MOBS.toDouble()
        val mobsPerHr = totalMobs / hours
        return BigDecimal.valueOf(mobsPerHr).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    // TODO: When the Alpha Network update is pushed to main by Hypixel, remove BASE_CHANCES_PERCENT and rename BASE_CHANCES_PERCENT_NEW to BASE_CHANCES_PERCENT.
    private val BASE_CHANCES_PERCENT = mapOf(
        "epic" to mapOf("stick" to 0.08, "relic" to 0.02),
        "legendary" to mapOf("chim" to 1.0, "stick" to 0.12, "relic" to 0.03, "food" to 0.25),
        "mythic" to mapOf("chim" to 1.25, "stick" to 0.16, "relic" to 0.04, "food" to 0.5, "wool" to 0.2, "core" to 0.2, "stinger" to 0.5)
    )

    private val BASE_CHANCES_PERCENT_NEW = mapOf(
        "epic" to mapOf("stick" to 0.05, "relic" to 0.05),
        "legendary" to mapOf("chim" to 2.0, "stick" to 0.1, "relic" to 0.1, "food" to 0.5),
        "mythic" to mapOf("chim" to 2.5, "stick" to 0.15, "relic" to 0.15, "food" to 1.0, "wool" to 1.0, "core" to 1.0, "stinger" to 1.5)
    )

    // TODO: When the Alpha Network update is pushed to main by Hypixel, remove getChance() and rename getChanceNew() to getChance().
    fun getChance(mf: Int, looting: Int, rarity: String, lootshare: Boolean = false): Map<String, Double> {
        val baseChances = BASE_CHANCES_PERCENT[rarity.lowercase().trim()]
            ?: emptyMap()

        val mfMultiplier = 1.0 + mf / 100.0
        val factor = if (lootshare) mfMultiplier / 5.0 else mfMultiplier * (1.0 + looting * 0.15)
        return baseChances.mapValues { (_, chance) -> chance / 100.0 * factor }
    }

    // TODO: When the Alpha Network update is pushed to main by Hypixel, this becomes getChance().
    fun getChanceNew(mf: Int, rarity: String, lootshare: Boolean = false): Map<String, Double> {
        val baseChances = BASE_CHANCES_PERCENT_NEW[rarity.lowercase().trim()]
            ?: emptyMap()

        return calculateDropChances(baseChances, mf, lootshare)
    }

    private fun calculateDropChances(baseChances: Map<String, Double>, mf: Int, lootshare: Boolean): Map<String, Double> {
        val multiplier = 1.0 + mf / 100.0
        val factor = if (lootshare) multiplier / 5.0 else multiplier

        return baseChances.mapValues { (_, chance) -> chance / 100.0 * factor }
    }

    fun formatChances(chance: Double, label: String): String {
        val percent = String.format("%.2f", chance * 100)
        val fraction = " §7(§b1/${(1 / chance).roundToInt()}§7)"
        return "§eChance: §b$percent%$fraction $label"
    }

    fun formatMagicFind(mf: Int): String = "§7[MF:$mf]"

    fun formatMagicFindAndLooting(mf: Int, looting: Int): String = " §7[MF:$mf] [L:$looting]"

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


