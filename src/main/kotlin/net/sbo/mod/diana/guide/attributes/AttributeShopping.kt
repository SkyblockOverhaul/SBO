package net.sbo.mod.diana.guide.attributes

import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.world.inventory.ChestMenu
import net.sbo.guilib.core.dom.Element
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.fabric.GuiLibHud
import net.sbo.mod.diana.guide.GuideAttribute
import net.sbo.mod.diana.guide.GuideData
import net.sbo.mod.diana.guide.GuideGui
import net.sbo.mod.diana.guide.menus.MenuGuide
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiContentRenderEvent
import net.sbo.mod.utils.events.impl.guis.GuiMouseClickBefore
import org.lwjgl.glfw.GLFW

/**
 * Buying attribute shards on the bazaar. A click on an attribute card searches the bazaar for its shard with /bz.
 * While the bazaar is open a panel next to the menu lists every shard the guide still needs with the shards to the next
 * level and to level 10. A click on a line searches that shard. Next to the sign of Custom Amount a click on the next
 * or the max number types it into the sign. Closing the bazaar opens the guide on the Attributes tab again.
 *
 * The panel is drawn with GuiLib in the look of the tracker overlays. It gets no input of its own, so the hovered and
 * clicked line is found by the element ids of the last drawn frame.
 */
internal object AttributeShopping {
    private const val CHEST_WIDTH = 176
    private const val CHEST_BASE_HEIGHT = 114
    private const val SLOT_SIZE = 18
    private const val GAP = 4
    private const val SIGN_HALF_WIDTH = 52
    private const val SIGN_TOP = 40
    private const val CLEAR_PRESSES = 16
    private const val PANEL_WIDTH = 170
    private const val LINE_HEIGHT = 12
    private const val PANEL_EXTRA_HEIGHT = 56
    private const val SIGN_MENU_ROWS = 6
    private val PRIORITY = listOf("must", "should", "can")

    /** The attribute levels from the SBO profile. The guide window sets them when it loads the profile. */
    @Volatile
    var profileLevels: Map<String, Int>? = null

    private var shopping = false

    /** The shard searched last. Its line is marked. */
    private var focus: String? = null

    /** Made on first draw. GuiLib documents can not be made while the game is still starting. */
    private var hud: GuiLibHud? = null
    private var shownFor: Screen? = null
    private var rows: List<AttrRow> = emptyList()
    private var page = 0
    private var perPage = 1
    private var hovered: String? = null
    private var dirty = true
    private var drawnAt: Pair<Int, Int>? = null

    /** Closes the guide and searches the bazaar for the shard of [attr]. */
    fun start(attr: GuideAttribute) {
        shopping = true
        focus = attr.key
        shownFor = null
        page = -1
        MenuGuide.follow(search(attr)) {
            shopping = false
            shownFor = null
            drawnAt = null
            GuideGui.open("attributes")
        }
    }

    /** With "Shard" after the name the bazaar shows only the shard and not other items with that name. */
    private fun search(attr: GuideAttribute) = "bz ${attr.name} Shard"

    /** Every guide shard that can be bought and is not maxed. Must first then Should then Can. */
    private fun needed(): List<AttrRow> = attrRows(GuideData.guide, profileLevels)
        .filter { it.attr.bazaar != null && it.level != null && !it.maxed && (it.shards ?: 0) > 0 }
        .sortedBy { PRIORITY.indexOf(it.attr.priority) }

    /**
     * The lines are read again for every new screen so a bought level shows after the menu was read again. It is drawn
     * after the menu content and before the item tooltip, so tooltips stay on top.
     */
    @SboEvent
    fun onRender(event: GuiContentRenderEvent) {
        if (!shopping) return
        val screen = event.screen
        val sign = screen is AbstractSignEditScreen
        val (x, y) = position(screen) ?: return
        if (screen !== shownFor) {
            shownFor = screen
            rows = needed()
            perPage = ((menuHeight(screen) - PANEL_EXTRA_HEIGHT) / LINE_HEIGHT).coerceAtLeast(1)
            if (page < 0) page = (rows.indexOfFirst { it.attr.key == focus }.coerceAtLeast(0)) / perPage
            hovered = null
            dirty = true
        }
        val hud = hud ?: GuiLibHud(listOf("sbo:ui/dianaguide/shopping.css")).also { hud = it }
        val hover = if (drawnAt != null) idAt(hud, event.mouseX.toDouble() - x, event.mouseY.toDouble() - y) else null
        if (hover != hovered) {
            hovered = hover
            dirty = true
        }
        if (dirty) {
            hud.render { panel(sign) }
            dirty = false
        }
        hud.draw(event.context, x.toFloat(), y.toFloat())
        drawnAt = x to y
    }

    @SboEvent
    fun onClick(event: GuiMouseClickBefore) {
        if (!shopping || event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.screen !== shownFor) return
        val hud = hud ?: return
        val (x, y) = drawnAt ?: return
        val id = idAt(hud, event.mouseX - x, event.mouseY - y) ?: return
        if (id == "page:prev" || id == "page:next") {
            page += if (id == "page:next") 1 else -1
            dirty = true
            return
        }
        val key = id.substringAfter(':')
        val row = rows.firstOrNull { it.attr.key == key } ?: return
        val sign = event.screen as? AbstractSignEditScreen
        when {
            sign != null && id.startsWith("next:") -> row.next?.let { type(sign, it) }
            sign != null && id.startsWith("max:") -> row.shards?.let { type(sign, it) }
            sign == null && id.startsWith("row:") -> {
                focus = row.attr.key
                dirty = true
                Chat.command(search(row.attr))
            }
        }
    }

    /** The current page and the number of pages for [perPage] lines per page. */
    private fun pageOf(perPage: Int): Pair<Int, Int> {
        val count = ((rows.size + perPage - 1) / perPage).coerceAtLeast(1)
        page = page.coerceIn(0, count - 1)
        return page to count
    }

    /**
     * As many lines as fit beside the menu with its inventory. The sign uses the height of a big chest. With more than
     * one page the last page is filled with empty lines so the page buttons do not move.
     */
    private fun NodeBuilder.panel(sign: Boolean) {
        val (current, count) = pageOf(perPage)
        val shown = rows.drop(current * perPage).take(perPage)
        div(className = "shop") {
            div(className = "shop-head") {
                span(className = "shop-title") { +"Attribute Shards" }
                span(className = "shop-col") { +"Next" }
                span(className = "shop-col") { +"Max" }
                span(className = "shop-col coins") { +"Coins" }
            }
            if (rows.isEmpty()) div(className = "shop-hint") { +"Every guide shard is maxed." }
            shown.forEach { row -> line(row, sign) }
            if (count > 1) repeat(perPage - shown.size) { i ->
                div(className = "shop-row empty", key = "empty$i") {
                    span(className = "shop-name") { +"-" }
                    span(className = "shop-col") { +"-" }
                }
            }
            if (count > 1) div(className = "shop-pages") {
                span(className = "shop-page" + (if (current == 0) " off" else "") + (if (hovered == "page:prev") " hover" else ""), id = "page:prev") { +"‹" }
                span(className = "shop-page-text") { +"${current + 1} / $count" }
                span(className = "shop-page" + (if (current == count - 1) " off" else "") + (if (hovered == "page:next") " hover" else ""), id = "page:next") { +"›" }
            }
            div(className = "shop-hint") {
                +if (sign) "Click Next or Max to type it in." else "Click a shard to search it."
            }
        }
    }

    private fun NodeBuilder.line(row: AttrRow, sign: Boolean) {
        val key = row.attr.key
        val rowId = "row:$key"
        val classes = "shop-row" + (if (key == focus) " focus" else "") + (if (!sign && hovered == rowId) " hover" else "")
        div(className = classes, id = if (sign) null else rowId, key = key) {
            span(className = "shop-name r-${row.attr.rarity}") { +((if (key == focus) "▶ " else "") + row.attr.name) }
            number("next:$key", row.next, sign)
            number("max:$key", row.shards, sign)
            span(className = "shop-col coins") { +(row.coins?.let { shortCoins(it.toDouble()) } ?: "-") }
        }
    }

    /** On the sign the numbers are the click targets, so they get an id and a frame. */
    private fun NodeBuilder.number(id: String, value: Int?, sign: Boolean) {
        val classes = "shop-col" + (if (sign) " pick" else "") + (if (sign && hovered == id) " hover" else "")
        span(className = classes, id = if (sign) id else null) { +(value?.toString() ?: "-") }
    }

    /** The id of the innermost element with one at that point of the panel, from the last drawn frame. */
    private fun idAt(hud: GuiLibHud, x: Double, y: Double): String? {
        if (x < 0 || y < 0 || x > hud.width || y > hud.height) return null
        var element: Element? = hud.root.painter.hitTest(x.toFloat(), y.toFloat())
        while (element != null) {
            element.id?.let { return it }
            element = element.parent
        }
        return null
    }

    /** The first line of the sign is cleared and the amount typed in like the player would. */
    private fun type(sign: AbstractSignEditScreen, amount: Int) {
        repeat(CLEAR_PRESSES) { sign.keyPressed(KeyEvent(GLFW.GLFW_KEY_BACKSPACE, 0, 0)) }
        amount.toString().forEach { sign.charTyped(CharacterEvent(it.code)) }
    }

    /** The height of the menu with the player inventory. A sign has none, it gets the height of a big chest. */
    private fun menuHeight(screen: Screen): Int {
        val rows = ((screen as? AbstractContainerScreen<*>)?.menu as? ChestMenu)?.rowCount ?: SIGN_MENU_ROWS
        return CHEST_BASE_HEIGHT + rows * SLOT_SIZE
    }

    /**
     * Right of the menu or left of it when there is no room. Next to the sign on its right. The screen position fields
     * are protected, so the menu corner is worked out the same way the screen does it.
     */
    private fun position(screen: Screen): Pair<Int, Int>? {
        if (screen is AbstractSignEditScreen) return screen.width / 2 + SIGN_HALF_WIDTH + GAP to SIGN_TOP
        val chest = screen as? AbstractContainerScreen<*> ?: return null
        val rows = (chest.menu as? ChestMenu)?.rowCount ?: return null
        val left = (screen.width - CHEST_WIDTH) / 2
        val top = (screen.height - CHEST_BASE_HEIGHT - rows * SLOT_SIZE) / 2
        val right = left + CHEST_WIDTH + GAP
        val x = if (right + PANEL_WIDTH <= screen.width) right else (left - PANEL_WIDTH - GAP).coerceAtLeast(0)
        return x to top
    }
}
