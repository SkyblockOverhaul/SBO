package net.sbo.mod.overlays

import net.minecraft.ChatFormatting.*
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.overlay.CHAT_SCREEN_FILTER
import net.sbo.mod.utils.overlay.CRAFTING_PLAYER_INVENTORY_FILTER
import net.sbo.mod.utils.overlay.DirtyFlushableOverlay
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.sbo.mod.utils.Helper.formatNumber
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.diana.DianaTracker
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.ItemLookup
import net.sbo.mod.utils.game.World

object CrownOfAvarice : DirtyFlushableOverlay() {
    override val overlay = Overlay("Diana Crown Of Avarice", 10f, 10f, allowedScreens = listOf(CRAFTING_PLAYER_INVENTORY_FILTER, CHAT_SCREEN_FILTER))
        .setCondition { Diana.coaTracker != Diana.Tracker.OFF }
        .setExtraCondition { World.isInHub() }

    private var helmet: ItemStack? = null
    private var primaryCoaUuid: String? = null
    private var cachedCoaCoins: Long = -1L // Coins of the PRIMARY CoA only. -1 means that primary CoA isn't equipped
    private var tracker: DianaTracker? = null
        get() = when (Diana.coaTracker) {
            Diana.Tracker.TOTAL -> DataManager.dianaTrackerTotalData
            Diana.Tracker.EVENT -> DataManager.dianaTrackerMayorData
            Diana.Tracker.SESSION -> DataManager.dianaTrackerSessionData
            else -> null
        }

    fun init() {
        overlay.init()
        updateLines()

        Register.onTick(50) { // Update every 2.5s
            helmet = mc.player?.inventory?.getItem(39)
            val worn = if (helmet.isCoA()) ItemLookup(helmet!!) else null

            // Load the saved primary UUID
            if (primaryCoaUuid == null && DataManager.sboData.currentCoaUuid.isNotEmpty()) {
                primaryCoaUuid = DataManager.sboData.currentCoaUuid
            }

            // Set the first detected CoA as the primary one if we don't have a saved one yet
            if (primaryCoaUuid == null && worn != null) {
                primaryCoaUuid = worn.uuid
                DataManager.sboData.currentCoaUuid = worn.uuid

                // Baseline for ALL trackers, so the starting balance is never counted as profit
                val startCoins = getCurrentCoins(helmet)
                listOf(
                    DataManager.dianaTrackerTotalData,
                    DataManager.dianaTrackerMayorData,
                    DataManager.dianaTrackerSessionData
                ).forEach { it.items.CURRENT_COA_START_COINS = startCoins }

                Chat.chat("§6[SBO] §aYour worn CoA was automatically set as your primary CoA. It's used to track your coins and profit/h.")
                Chat.chat("§6[SBO] §7Wearing a different CoA? Use §e/setprimarycoa §7to switch.")
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
                    }
                }
            }

            updateLines()
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

    override fun generateLines(): List<OverlayTextLine> {
        return listOf(
            OverlayTextLine("$YELLOW${BOLD}Crown Of Avarice"),
            OverlayTextLine("$GRAY - ${GREEN}Current Coins: " + if (cachedCoaCoins != -1L) "${AQUA}${formatNumber(cachedCoaCoins)}" else "${RED}Primary COA Not Equipped"),
            OverlayTextLine("$GRAY - ${GREEN}Coins/h: $AQUA${formatNumber(getCoinsPerHour())}"),
            OverlayTextLine("$GRAY - ${GREEN}Total Coins: $AQUA${formatNumber(getTotalCoins())}"),
            OverlayTextLine("$GRAY - ${GREEN}Profit/h: $AQUA${formatNumber(getProfitPerHour())}"),
        )
    }

    /** Profit/h from leveling COAs (price diff between 1b COA & clean COA) */
    private fun getProfitPerHour(): Double {
        val maxCOAPrice = 2_000_000_000 // TODO: Use API endpoint once ready, don't forget to cache the value if needed
        val cleanCOAPrice = 500_000_000

        val profitPerCOA = maxCOAPrice - cleanCOAPrice
        val profitPerHour = if (getCoinsPerHour() == 0.0) 0.0 else profitPerCOA / (1_000_000_000 / getCoinsPerHour())
        return profitPerHour
    }

    /** Total coins made from leveling COAs this event */
    private fun getTotalCoins(): Long {
        return tracker?.items?.COA_COINS ?: 0L
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

    private fun ItemStack?.isCoA(): Boolean {
        if (this == null) return false

        return "Crown of Avarice" in ItemLookup(this).displayName
    }
}