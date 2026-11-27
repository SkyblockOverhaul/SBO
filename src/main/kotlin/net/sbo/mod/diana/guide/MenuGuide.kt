package net.sbo.mod.diana.guide

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.Slot
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.TextUtil.removeFormatting
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.TickEvent
import net.sbo.mod.utils.events.impl.guis.GuiPostRenderEvent
import net.sbo.mod.utils.game.ItemLookup
import kotlin.math.PI
import kotlin.math.sin

/**
 * A way through Hypixel menus to a page the guide reads.
 *
 * @property command the command that opens the first menu without the slash
 * @property steps per menu title the name of the item to click next
 * @property target the title of the page at the end
 * @property offToggles buttons on the target page that should be off. They are highlighted while their lore says
 * "Enabled: ON"
 */
internal class MenuPath(
    val command: String,
    val steps: List<Pair<String, String>>,
    val target: String,
    val offToggles: List<String> = emptyList()
)

/**
 * Shows the player the way to a menu page. The button in the guide runs the command and then the item to click next is
 * outlined on every page until the target page is open. SBO never clicks for the player. Hypixel builds the pages on
 * the server so they can not be opened directly.
 */
internal object MenuGuide {
    private const val TIMEOUT_MS = 2 * 60 * 1000L
    private const val CHEST_WIDTH = 176
    private const val CHEST_BASE_HEIGHT = 114
    private const val SLOT_SIZE = 18
    private const val OUTLINE = 0xFF55FF55.toInt()
    private const val FILL_RGB = 0x55FF55
    private const val FILL_MIN_ALPHA = 0x30
    private const val FILL_MAX_ALPHA = 0x90
    private const val PULSE_MS = 900.0

    val MAGIC_FIND = MenuPath(
        "stats",
        listOf("Stats & Equipment" to "Miscellaneous Stats", "Your Stats Breakdown" to "Magic Find", "Magic Find" to "Flat Bonuses"),
        "Flat Bonuses",
        listOf("Group By Category", "Flatten Stats Menu")
    )

    val AUTOPET = MenuPath("pets", listOf("Pets" to "Autopet"), "Autopet")

    private const val FIRST_MENU_WAIT_MS = 5000L
    private const val PAGE_SWITCH_MS = 500L

    private var active: MenuPath? = null
    private var startedAt = 0L
    private var menuSeen = false
    private var leftAt = 0L

    /** Closes the guide and opens the first menu of [path]. */
    fun start(path: MenuPath) {
        active = path
        startedAt = System.currentTimeMillis()
        menuSeen = false
        leftAt = 0L
        GuiLib.close()
        Chat.command(path.command)
    }

    /**
     * The way ends when the player leaves the menus. Otherwise a later /stats or /pets typed by hand would still be
     * highlighted. Before the first menu arrives the server gets a few seconds to open it. A short moment without a
     * menu between two pages does not end it.
     */
    @SboEvent
    fun onTick(event: TickEvent) {
        active ?: return
        val now = System.currentTimeMillis()
        if (mc.screen is AbstractContainerScreen<*>) {
            menuSeen = true
            leftAt = 0L
            return
        }
        if (menuSeen && leftAt == 0L) leftAt = now
        val left = menuSeen && now - leftAt > PAGE_SWITCH_MS
        val waitedTooLong = !menuSeen && now - startedAt > FIRST_MENU_WAIT_MS
        if (left || waitedTooLong) active = null
    }

    @SboEvent
    fun onRender(event: GuiPostRenderEvent) {
        val path = active ?: return
        if (System.currentTimeMillis() - startedAt > TIMEOUT_MS) {
            active = null
            return
        }
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        val title = screen.title.string.removeFormatting()
        val own = mc.player?.inventory
        val items = screen.menu.slots.filter { it.container !== own && !it.item.isEmpty }
        fun named(name: String) = items.filter { it.item.hoverName.string.removeFormatting().contains(name) }
        if (title.contains(path.target)) {
            val on = path.offToggles.flatMap(::named).filter { slot ->
                ItemLookup(slot.item).cleanLoreList.any { it.contains("Enabled: ON") }
            }
            if (on.isEmpty()) active = null
            on.forEach { highlight(event, screen, it) }
            return
        }
        val name = path.steps.lastOrNull { title.contains(it.first) }?.second ?: return
        named(name).firstOrNull()?.let { highlight(event, screen, it) }
    }

    private fun highlight(event: GuiPostRenderEvent, screen: AbstractContainerScreen<*>, slot: Slot) {
        val rows = (screen.menu as? ChestMenu)?.rowCount ?: return
        val left = (screen.width - CHEST_WIDTH) / 2 + slot.x
        val top = (screen.height - CHEST_BASE_HEIGHT - rows * SLOT_SIZE) / 2 + slot.y
        highlight(event, left - 1, top - 1, left + 17, top + 17)
    }

    /** A translucent fill that pulses over the slot and a 2 pixel frame around it, so it stands out on any menu. */
    private fun highlight(event: GuiPostRenderEvent, x0: Int, y0: Int, x1: Int, y1: Int) {
        val ctx = event.context
        val pulse = (sin(System.currentTimeMillis() / PULSE_MS * 2 * PI) + 1) / 2
        val alpha = (FILL_MIN_ALPHA + pulse * (FILL_MAX_ALPHA - FILL_MIN_ALPHA)).toInt()
        ctx.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, (alpha shl 24) or FILL_RGB)
        ctx.fill(x0 - 1, y0 - 1, x1 + 1, y0 + 1, OUTLINE)
        ctx.fill(x0 - 1, y1 - 1, x1 + 1, y1 + 1, OUTLINE)
        ctx.fill(x0 - 1, y0 - 1, x0 + 1, y1 + 1, OUTLINE)
        ctx.fill(x1 - 1, y0 - 1, x1 + 1, y1 + 1, OUTLINE)
    }
}
