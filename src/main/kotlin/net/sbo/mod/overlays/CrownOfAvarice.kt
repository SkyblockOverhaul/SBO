package net.sbo.mod.overlays

import net.minecraft.ChatFormatting.*
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin.mc
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
        .setCondition { true } // TODO: Setting to enable/disable in config, but for now always enabled
        .setExtraCondition { World.isInHub() }

    private var helmet: ItemStack? = null

    fun init() {
        overlay.init()
        updateLines()

        Register.onTick(20) {
            helmet = mc.player?.inventory?.getItem(39)
            updateLines()
        }
    }

    override fun generateLines(): List<OverlayTextLine> {
        return listOf(
            OverlayTextLine("$YELLOW${BOLD}Crown Of Avarice"),
            OverlayTextLine("$GRAY - ${GREEN}Current Coins: " + if (getCurrentCoins() != -1.0) "${AQUA}${formatNumber(getCurrentCoins())}" else "${RED}COA Not Equipped"),
            OverlayTextLine("$GRAY - ${GREEN}Coins/h: $AQUA${formatNumber(getCoinsPerHour())}"),
            OverlayTextLine("$GRAY - ${GREEN}Total Coins: $AQUA${formatNumber(getTotalCoins())}"),
            OverlayTextLine("$GRAY - ${GREEN}Profit/h: $AQUA${formatNumber(getProfitPerHour())}"),
        )
    }

    /** Profit/h from leveling COAs (price diff between 1b COA & clean COA) */
    private fun getProfitPerHour(): Double {
        if (getCoinsPerHour() == -1.0) return 0.0 // Avoid division by zero
        val maxCOAPrice = 2_000_000_000 // TODO: Use API endpoint once ready, don't forget to cache the value if needed
        val cleanCOAPrice = 500_000_000

        val profitPerCOA = maxCOAPrice - cleanCOAPrice
        val profitPerHour = if (getCoinsPerHour() == 0.0) 0.0 else profitPerCOA / (1_000_000_000 / getCoinsPerHour())
        return profitPerHour
    }

    /** Total coins made from leveling COAs this event */
    private fun getTotalCoins(): Int {
        return DataManager.dianaTrackerMayorData.items.CROWN_OF_AVARICE
    }

    /** Current COA's coins */
    private fun getCurrentCoins(item: ItemStack? = null): Double {
        val helmet = item ?: helmet
        if (!helmet.isCoA()) return -1.0

        val coa = ItemLookup(helmet!!)
        val coins = coa.cleanLoreList
            .find { it.contains("Coins Consumed: ") } // Grab the Coins Consumed line
            ?.split(": ")?.lastOrNull() // Grab the number
            ?.replace(",", "")?.toDoubleOrNull() ?: 0.0 // Format it

        return coins
    }

    private fun getCoinsPerHour(): Double {
        return if (DataManager.dianaTrackerMayorData.items.TIME == 0L) 0.0 else getTotalCoins().toDouble() / DataManager.dianaTrackerMayorData.items.TIME
    }

    /** Don't count when COA is maxed or it's not diana, etc... */
    fun addCoins(amount: Int, tracker: DianaTracker = DataManager.dianaTrackerMayorData, source: String) {
        if (!helmet.isCoA()) return

        val multiplier = if (getCurrentCoins(helmet) != 1_000_000_000.0) 5 else if (getCurrentCoins(helmet) == -1.0) 1 else 2 // 2x if max COA, 5x if clean COA, 1x if no COA

        tracker.items.CROWN_OF_AVARICE += amount * multiplier
        Chat.chat("§6[COA] §aAdded §6${formatNumber(amount)} §acoins to your Crown of Avarice! (x$multiplier) from $source")
        updateLines()
    }

    private fun ItemStack?.isCoA(): Boolean {
        if (this == null) return false

        return "Crown of Avarice" in ItemLookup(this).displayName
    }
}