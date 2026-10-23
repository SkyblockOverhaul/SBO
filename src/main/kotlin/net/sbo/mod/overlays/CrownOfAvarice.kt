package net.sbo.mod.overlays

import net.minecraft.world.item.ItemStack
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.item
import net.sbo.guilib.core.dsl.span
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.overlay.CHAT_SCREEN_FILTER
import net.sbo.mod.utils.overlay.CRAFTING_PLAYER_INVENTORY_FILTER
import net.sbo.mod.utils.overlay.GuiLibOverlay
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.sbo.mod.utils.Helper.formatNumber
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.diana.DianaTracker
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiCloseEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.game.ItemLookup
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.overlay.isCraftingScreenOpen
import java.util.Locale

object CrownOfAvarice {
    private const val MAX_COINS = 1_000_000_000L
    private const val RATE_REFRESH_MS = 5_000L

    // Both looks share one name, so they share the saved position; only the chosen one is shown
    private const val OVERLAY_NAME = "Diana Crown Of Avarice"

    // The new look, built with GuiLib (sbo:ui/coa/coa.css), a click on the view badge in the inventory changes the view
    private val overlay = GuiLibOverlay(
        OVERLAY_NAME, 10f, 10f,
        allowedScreens = listOf(CRAFTING_PLAYER_INVENTORY_FILTER, CHAT_SCREEN_FILTER),
        stylesheets = listOf("sbo:ui/coa/coa.css")
    )
        .setCondition { Diana.coaTracker != Diana.Tracker.OFF && !Diana.isLegacy(Diana.TrackerOverlay.COA) }
        .setExtraCondition { World.isInHub() }

    // The legacy look: Minecraft text lines like the other overlays, slightly better performance
    private val legacyOverlay = Overlay(OVERLAY_NAME, 10f, 10f, allowedScreens = listOf(CRAFTING_PLAYER_INVENTORY_FILTER, CHAT_SCREEN_FILTER))
        .setCondition { Diana.coaTracker != Diana.Tracker.OFF && Diana.isLegacy(Diana.TrackerOverlay.COA) }
        .setExtraCondition { World.isInHub() }

    private val changeView = OverlayUtils.createClickableTextLine(
        text = "§eChange View",
        hoverText = "§e§nChange View",
        onClick = {
            Diana.coaTracker = Diana.coaTracker.next()
            updateLines()
        }
    )

    private var maxCOAPrice: Long = 0L
    private var cleanCOAPrice: Long = 0L

    var helmet: ItemStack? = null
    // The last CoA seen on your head, the header has no icon until then
    private var icon: ItemStack? = null
    var primaryCoaUuid: String? = null
    private var cachedCoaCoins: Long = -1L // Coins of the PRIMARY CoA only. -1 means that primary CoA isn't equipped
    private var tracker: DianaTracker? = null
        get() = when (Diana.coaTracker) {
            Diana.Tracker.TOTAL -> DataManager.dianaTrackerTotalData
            Diana.Tracker.EVENT -> DataManager.dianaTrackerMayorData
            Diana.Tracker.SESSION -> DataManager.dianaTrackerSessionData
            else -> null
        }

    // What the tick in init last saw, to notice changes
    private var lastHelmet: ItemStack? = null
    private var lastTrackerOn = false
    private var lastAlphaBlocked = false
    private var lastRateTime = 0L

    // Set by updateLines, the overlay is refreshed on the next tick: never while the game is still starting, and
    // several calls in one tick refresh it once
    private var dirty = true
    // Handled on the tick, the look setting can also change while the config loads
    private var lookChanged = false
    private var shownView: View? = null

    // On the Alpha Network without its separate data, tracking would write alpha coins into the main data
    private val alphaBlocked: Boolean get() = HypixelModApi.isAlpha && !DataManager.alphaActive

    fun init() {
        overlay.init()
        legacyOverlay.init()
        // The view badge in the top right changes the view
        overlay.onClick(".coa-mode") {
            Diana.coaTracker = Diana.coaTracker.next()
            updateLines()
        }
        updateLines()
        updateCoaPrice()

        Register.onTick(20 * 60 * 30) { // Refresh every 30mins
            updateCoaPrice()
        }

        // The helmet slot holds a new ItemStack whenever the server changes it (equipping, more coins in the lore,
        // world change), so comparing the reference each tick finds every change without reading the lore.
        // Rates (/h, maxed in) move with the playtime, they are refreshed every few seconds while it runs.
        Register.onTick(1) {
            val stack = mc.player?.inventory?.getItem(39)
            val trackerOn = tracker != null
            val blocked = alphaBlocked
            // Turning the tracker on or the alpha data coming in or going reads the worn CoA again
            if (stack !== lastHelmet || trackerOn != lastTrackerOn || blocked != lastAlphaBlocked) {
                lastHelmet = stack
                lastTrackerOn = trackerOn
                lastAlphaBlocked = blocked
                onHelmetChanged(stack)
            }
            val time = tracker?.items?.TIME ?: 0L
            if (time < lastRateTime || time - lastRateTime >= RATE_REFRESH_MS) {
                lastRateTime = time
                updateLines()
            }
            if (dirty) refresh()
        }

        Register.command("setprimarycoa") {
            helmet = mc.player?.inventory?.getItem(39)
            if (helmet == null) {
                Chat.chat("§6[SBO] §cFailed to detect your helmet")
                return@command
            }

            if (!helmet.isCoA()) {
                Chat.chat("§6[SBO] §cYour helmet is not a Crown of Avarice")
                return@command
            }

            if (tracker == null) {
                Chat.chat("§6[SBO] §cPlease enable your CoA tracker (Settings -> Diana -> CoA Tracker) first")
                return@command
            }

            if (alphaBlocked) {
                Chat.chat("§6[SBO] §cThe CoA tracker only tracks on the Alpha Network with Debug -> Separate Alpha Data on")
                return@command
            }

            // Set primary CoA & currentCoins to the new CoA's values
            val uuid = ItemLookup(helmet!!).uuid
            DataManager.sboData.currentCoaUuid = uuid
            primaryCoaUuid = uuid

            val startCoins = getCurrentCoins(helmet)
            cachedCoaCoins = startCoins

            // Reset the start coins on all trackers
            listOf(
                DataManager.dianaTrackerTotalData,
                DataManager.dianaTrackerMayorData,
                DataManager.dianaTrackerSessionData
            ).forEach { it.items.CURRENT_COA_START_COINS = startCoins }

            Chat.chat("§6[SBO] §aYour primary CoA has been set to this CoA")

            updateLines()
        }
    }

    // Reads the worn CoA and counts its gained coins, only when the helmet changed (see the tick in init)
    private fun onHelmetChanged(stack: ItemStack?) {
        helmet = stack
        val worn = if (helmet.isCoA()) ItemLookup(helmet!!) else null
        // A new copy only when the item changed, the overlay re-renders when the icon is a different stack
        if (worn != null && icon.let { it == null || !ItemStack.isSameItemSameComponents(it, helmet!!) }) icon = helmet!!.copy()

        // The Alpha Network has copies of your items with the same UUID but other coins: counting them into the main
        // data would break the baselines of the real CoA, so it is only tracked there with the separate alpha data
        if (alphaBlocked) {
            cachedCoaCoins = -1L
            updateLines()
            return
        }

        // Load the saved primary UUID
        if (primaryCoaUuid == null && DataManager.sboData.currentCoaUuid.isNotEmpty()) {
            primaryCoaUuid = DataManager.sboData.currentCoaUuid
        }

        // Set the first detected CoA as the primary one if we don't have a saved one yet
        if (primaryCoaUuid == null && worn != null && tracker != null) {
            primaryCoaUuid = worn.uuid
            DataManager.sboData.currentCoaUuid = worn.uuid

            // Baseline for ALL trackers, so the starting balance is never counted as profit
            val startCoins = getCurrentCoins(helmet)
            listOf(
                DataManager.dianaTrackerTotalData,
                DataManager.dianaTrackerMayorData,
                DataManager.dianaTrackerSessionData
            ).forEach { it.items.CURRENT_COA_START_COINS = startCoins }

            Chat.chat("§6[SBO] §aYour worn CoA was automatically set as your primary CoA. It's used to track your coins and profit/h.", true)
            Chat.chat("§6[SBO] §7Wearing a different CoA? Use §e/setprimarycoa §7to switch.", true)
        }

        // Only trust the coins if the CoA on our head is the primary one.
        cachedCoaCoins = if (worn != null && worn.uuid == primaryCoaUuid) {
            getCurrentCoins(helmet)
        } else {
            -1L
        }

        // Only count gains while the PRIMARY CoA is worn
        if (tracker != null && cachedCoaCoins != -1L) {
            // Update all trackers so Total/Event/Session stay in sync when you switch the setting
            listOf(
                DataManager.dianaTrackerTotalData,
                DataManager.dianaTrackerMayorData,
                DataManager.dianaTrackerSessionData
            ).forEach { t ->
                // -1 = baseline never set (fresh/reset tracker): set it without counting anything
                if (t.items.CURRENT_COA_START_COINS < 0) {
                    t.items.CURRENT_COA_START_COINS = cachedCoaCoins
                    return@forEach
                }

                val gained = cachedCoaCoins - t.items.CURRENT_COA_START_COINS
                if (gained > 0) {
                    t.items.COA_COINS += gained
                    t.items.CURRENT_COA_START_COINS = cachedCoaCoins // move the baseline forward
                } else if (gained < 0) {
                    // Coins consumed only grow, so fewer means another copy of the CoA (the Alpha Network copies
                    // the profile): start over from it without counting anything
                    t.items.CURRENT_COA_START_COINS = cachedCoaCoins
                }
            }
        }

        updateLines()
    }

    /** Shows the current numbers, called whenever they change. */
    fun updateLines() {
        dirty = true
    }

    private fun refresh() {
        dirty = false
        if (lookChanged) {
            lookChanged = false
            overlay.loadPosition()
            legacyOverlay.loadPosition()
            shownView = null
        }
        val view = currentView()
        // Re-rendering means a full GuiLib layout pass, so only when something shown changed
        if (view == shownView) return
        shownView = view
        if (Diana.isLegacy(Diana.TrackerOverlay.COA)) legacyOverlay.setLines(legacyLines(view)) else overlay.setContent { content(view) }
    }

    /** Called when the look setting changes: the other overlay takes over on the next tick, at the same saved position. */
    fun onLookChanged() {
        lookChanged = true
        updateLines()
    }


    /** Everything the overlay shows, as text: equal views look the same. */
    private data class View(
        val icon: ItemStack?,
        val mode: String,
        val inter: Boolean,
        val background: Boolean,
        val fillWidth: String,
        val percent: String,
        val maxed: Boolean,
        val coins: String,
        val earned: String,
        val earnedPerHour: String,
        val profit: String,
        val profitPerHour: String,
        val maxedIn: String,
        val notEquipped: Boolean,
        val alpha: Boolean,
        val inventory: Boolean
    )

    private fun currentView(): View {
        val coins = shownCoins()
        val progress = if (coins >= 0L) (coins.toDouble() / MAX_COINS).coerceIn(0.0, 1.0) else 0.0
        return View(
            icon = icon,
            mode = Helper.toTitleCase(Diana.coaTracker.name),
            inter = Diana.trackerFont == Diana.TrackerFont.INTER,
            background = Diana.hasBackground(Diana.TrackerOverlay.COA),
            // Whole tenths of a percent are finer than a pixel of the bar
            fillWidth = "%.1f".format(Locale.ROOT, progress * 100),
            percent = if (coins >= 0L) "${"%.1f".format(Locale.ROOT, progress * 100)}%" else "-",
            maxed = coins >= MAX_COINS,
            coins = if (coins >= 0L) formatNumber(coins) else "-",
            earned = formatNumber(getTotalCoins()),
            earnedPerHour = "${formatNumber(getCoinsPerHour())}/h",
            profit = formatNumber(getProfit()),
            profitPerHour = "${formatNumber(getProfitPerHour())}/h",
            maxedIn = maxedInText(coins),
            notEquipped = cachedCoaCoins == -1L,
            alpha = alphaBlocked,
            inventory = isCraftingScreenOpen()
        )
    }

    private fun legacyLines(view: View): List<OverlayTextLine> {
        val lines = mutableListOf(
            OverlayTextLine("§e§lCrown Of Avarice §7(§e${view.mode}§7)"),
            OverlayTextLine("§7 - §aCoins: §b${view.coins} §7/ 1B §8(${view.percent})"),
            OverlayTextLine("§7 - §aEarned: §b${view.earned} §7[§b${view.earnedPerHour}§7]"),
            OverlayTextLine("§7 - §aProfit: §b${view.profit} §7[§b${view.profitPerHour}§7]"),
            OverlayTextLine("§7 - §aMaxed In: §b${view.maxedIn}")
        )
        if (view.alpha) lines += OverlayTextLine("§7 - §cNot tracked on Alpha (Separate Alpha Data off)")
        else if (view.notEquipped) lines += OverlayTextLine("§7 - §cPrimary CoA not equipped")
        if (view.inventory) lines.add(0, changeView)
        return lines
    }

    private fun NodeBuilder.content(view: View) {
        val classes = buildString {
            append("coa")
            if (view.inter) append(" inter")
            if (!view.background) append(" no-bg")
        }
        div(className = classes) {
            div(className = "coa-head") {
                view.icon?.let { item(it, className = "coa-icon", decorations = false) }
                span(className = "coa-title") { +"Crown of Avarice" }
                span(className = if (view.inventory) "coa-mode clickable" else "coa-mode") { +view.mode }
            }
            div(className = "coa-progress") {
                div(className = if (view.maxed) "coa-bar maxed" else "coa-bar") {
                    div(className = "coa-fill", style = "width: ${view.fillWidth}%")
                }
                span(className = "coa-percent") { +view.percent }
            }
            row("Coins", view.coins, "/ 1B")
            row("Earned", view.earned, view.earnedPerHour)
            row("Profit", view.profit, view.profitPerHour, "profit")
            row("Maxed in", view.maxedIn)
            if (view.alpha) div(className = "coa-warn") { +"Not tracked on Alpha (Separate Alpha Data off)" }
            else if (view.notEquipped) div(className = "coa-warn") { +"Primary CoA not equipped" }
            if (view.inventory) div(className = "coa-hint") { +"Click the view to change it" }
        }
    }

    private fun NodeBuilder.row(label: String, value: String, extra: String? = null, valueClass: String? = null) {
        div(className = "coa-row") {
            span(className = "coa-label") { +label }
            span(className = if (valueClass != null) "coa-value $valueClass" else "coa-value") { +value }
            if (extra != null) span(className = "coa-extra") { +extra }
        }
    }

    /** Coins of the worn primary CoA, or the last known ones while it is not worn, -1 if unknown. */
    private fun shownCoins(): Long {
        if (cachedCoaCoins != -1L) return cachedCoaCoins
        return tracker?.items?.CURRENT_COA_START_COINS ?: -1L
    }

    private fun maxedInText(coins: Long): String {
        if (coins >= MAX_COINS) return "Maxed!"
        val hours = maxedIn(coins)
        // No estimate until something was earned in this view
        return if (hours.isFinite()) Helper.formatTime((hours * 3_600_000).toLong()) else "-"
    }

    /** Profit/h = total CoA profit / hours played */
    fun getProfitPerHour(): Double {
        val hours = (tracker?.items?.TIME ?: 0L) / 3_600_000.0
        return if (hours > 0.0) getProfit() / hours else 0.0
    }

    /** Total profit from leveling COAs with this tracker */
    fun getProfit(): Double {
        if (cleanCOAPrice <= 0L || maxCOAPrice <= 0L) updateCoaPrice()

        val profitPerCOA = maxCOAPrice - cleanCOAPrice
        return profitPerCOA * getTotalCoins() / MAX_COINS.toDouble()
    }

    /** Total coins made from leveling COAs with this tracker */
    private fun getTotalCoins(): Long {
        return tracker?.items?.COA_COINS ?: 0L
    }

    // Infinite while nothing was earned yet
    private fun maxedIn(coins: Long): Double {
        return (MAX_COINS - coins.coerceAtLeast(0L)) / getCoinsPerHour()
    }

    /** Current COA's coins, or -1 if the item isn't a CoA */
    private fun getCurrentCoins(item: ItemStack? = null): Long {
        val helmet = item ?: helmet
        if (!helmet.isCoA()) return -1L

        return ItemLookup(helmet!!).cleanLoreList
            .find { it.contains("Coins Consumed: ") } // Grab the Coins Consumed line
            ?.split(": ")?.lastOrNull() // Grab the number
            ?.replace(",", "")?.toLongOrNull() ?: 0L // Format it
    }

    private fun getCoinsPerHour(): Double {
        val ms = tracker?.items?.TIME ?: 0L
        if (ms <= 0L) return 0.0

        return getTotalCoins() / (ms / 3_600_000.0)
    }

    fun ItemStack?.isCoA(): Boolean {
        if (this == null) return false

        return "Crown of Avarice" in ItemLookup(this).displayName
    }

    private fun updateCoaPrice() {
        cleanCOAPrice = Helper.getItemPrice("CROWN_OF_AVARICE", 1)
        maxCOAPrice = Helper.getItemPrice("CROWN_OF_AVARICE_MAXED", 1)
    }

    // The click hint only shows in the inventory
    @SboEvent
    fun onGuiClose(event: GuiCloseEvent) {
        if (CRAFTING_PLAYER_INVENTORY_FILTER(event.screen)) updateLines()
    }

    @SboEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (CRAFTING_PLAYER_INVENTORY_FILTER(event.screen)) updateLines()
    }
}
