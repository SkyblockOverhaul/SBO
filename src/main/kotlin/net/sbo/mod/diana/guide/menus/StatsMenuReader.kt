package net.sbo.mod.diana.guide.menus

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.TextUtil.removeFormatting
import net.sbo.mod.utils.events.TickScheduler.sleep
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.game.ItemLookup

/**
 * Reads the Magic Find of the player from the Hypixel stats menus while they are open.
 *
 * "Stats & Equipment" (/stats) has the total in Miscellaneous Stats. "Stats ➜ Magic Find" has the total and the biggest
 * flat sources. "Magic Find ➜ Flat Bonuses" has every source: one item per source with a "Value: +X★" line and
 * categories that list their sources as "+X★ Name" lines. The values are kept until the game closes.
 */
internal object StatsMenuReader {
    private const val MENU_ITEMS_DELAY_MS = 500L
    private val TOTAL = Regex("Magic Find ([0-9][0-9.,]*)$")
    private val ADDS_UP = Regex("^Adds up to: \\+([0-9][0-9.,]*)")
    private val VALUE = Regex("^Value: \\+([0-9][0-9.,]*)")
    private val SOURCE = Regex("^\\+([0-9][0-9.,]*)\\S*\\s+(.+)$")

    @Volatile
    var total: Double? = null
        private set

    /** Source name to Magic Find. Null until a Magic Find page was opened. */
    @Volatile
    var sources: Map<String, Double>? = null
        private set

    /** True when [sources] came from the Flat Bonuses page with every source and not only the biggest ones. */
    @Volatile
    var complete = false
        private set

    @Volatile
    var readAt: Long? = null
        private set

    /** The items arrive a moment after the menu opens. */
    @SboEvent
    fun onMenuOpen(event: GuiOpenEvent) {
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        val title = screen.title.string.removeFormatting()
        val page = when {
            title.startsWith("Stats & Equipment") -> Page.EQUIPMENT
            title.contains("Flat Bonuses") -> Page.FLAT_BONUSES
            title.startsWith("Stats") && title.endsWith("Magic Find") -> Page.MAGIC_FIND
            else -> return
        }
        sleep(MENU_ITEMS_DELAY_MS) { read(screen, page) }
    }

    private enum class Page { EQUIPMENT, MAGIC_FIND, FLAT_BONUSES }

    private fun read(screen: AbstractContainerScreen<*>, page: Page) {
        val own = mc.player?.inventory
        val items = screen.menu.slots.filter { it.container !== own && !it.item.isEmpty }.map { it.item }
        if (items.isEmpty()) return
        when (page) {
            Page.EQUIPMENT -> items.flatMap(::lore).firstNotNullOfOrNull { TOTAL.find(it)?.groupValues?.get(1) }?.let { total = number(it) }
            Page.MAGIC_FIND -> readMagicFind(items)
            Page.FLAT_BONUSES -> readFlatBonuses(items)
        }
        readAt = System.currentTimeMillis()
    }

    /** The Flat Bonuses item lists only the biggest sources, the rest is hidden behind "And 7 more...". */
    private fun readMagicFind(items: List<ItemStack>) {
        val lore = items.flatMap(::lore)
        lore.firstNotNullOfOrNull { ADDS_UP.find(it)?.groupValues?.get(1) }?.let { total = number(it) }
        if (complete) return
        sources = lore.mapNotNull(::source).toMap()
    }

    /**
     * One item per source when Group By Category is off. The category is the last line above the value like
     * "Attribute" or "Community Shop". It is kept in the key as "Name · Category" because two sources can both be
     * called "Magic Find".
     */
    private fun readFlatBonuses(items: List<ItemStack>) {
        val found = HashMap<String, Double>()
        items.forEach { stack ->
            val lore = lore(stack)
            val valueAt = lore.indexOfFirst { VALUE.containsMatchIn(it) }.takeIf { it >= 0 } ?: return@forEach
            val value = VALUE.find(lore[valueAt])?.groupValues?.get(1) ?: return@forEach
            val parts = lore.mapNotNull(::source)
            if (parts.isNotEmpty()) {
                found += parts
                return@forEach
            }
            val category = lore.take(valueAt).lastOrNull { it.isNotEmpty() }
            val name = name(stack.hoverName.string)
            found[if (category != null && category != name) "$name · $category" else name] = number(value)
        }
        if (found.isEmpty()) return
        sources = found
        complete = true
        total = found.values.sum()
    }

    private fun source(line: String): Pair<String, Double>? =
        SOURCE.find(line)?.let { name(it.groupValues[2]) to number(it.groupValues[1]) }

    private fun lore(stack: ItemStack): List<String> = ItemLookup(stack).cleanLoreList.map { it.trim() }

    /** Without the symbols in front like "★ " or "⚚ " and without color codes. */
    private fun name(text: String): String = text.removeFormatting().trim().dropWhile { !it.isLetterOrDigit() }.trim()

    private fun number(text: String): Double = text.replace(",", "").toDoubleOrNull() ?: 0.0
}
