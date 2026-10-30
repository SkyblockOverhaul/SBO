package net.sbo.mod.overlays

import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.span
import net.sbo.mod.settings.categories.Trackers
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.overlay.CHAT_SCREEN_FILTER
import net.sbo.mod.utils.overlay.CRAFTING_PLAYER_INVENTORY_FILTER
import net.sbo.mod.utils.overlay.DirtyFlushableOverlay
import net.sbo.mod.utils.overlay.GuiLibOverlay
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.sbo.mod.utils.overlay.isCraftingScreenOpen

/**
 * A Diana overlay with two looks: the new one built with GuiLib (sbo:ui/tracker/tracker.css) and, when picked in
 * "Tracker Legacy Look", Minecraft text lines from [generateLines]. Both share one name, so they share the saved
 * position; only the chosen one is shown.
 * The new look is only laid out again when its [view] changed.
 * By default it is shown with a spade in the hub, [extraCondition] changes that (it is ignored in /sboguis).
 */
abstract class DianaOverlay<V : Any>(
    name: String,
    private val look: Trackers.TrackerLook,
    shown: () -> Boolean,
    x: Float = 10f,
    y: Float = 10f,
    allowedScreens: List<(screen: Screen?) -> Boolean> = SCREENS,
    extraCondition: () -> Boolean = { Helper.hasSpade && World.isInHub() },
    exampleLines: List<OverlayTextLine> = emptyList()
) : DirtyFlushableOverlay() {
    final override val overlay = Overlay(name, x, y, allowedScreens = allowedScreens, exampleView = exampleLines)
        .setCondition { shown() && Trackers.isLegacy(look) }
        .setExtraCondition(extraCondition)

    private val modern = GuiLibOverlay(name, x, y, allowedScreens = allowedScreens, stylesheets = listOf("sbo:ui/tracker/tracker.css"))
        .setCondition { shown() && !Trackers.isLegacy(look) }
        .setExtraCondition(extraCondition)

    private var shownView: V? = null
    // Handled on the next flush, the look settings can also change while the config loads
    private var lookChanged = false

    protected fun initOverlays() {
        overlay.init()
        modern.init()
        modern.onElementClick(::elementClicked)
        modern.tooltip(::tooltip)
    }

    /** Called when a look setting changes: the other look takes over at the same saved position. */
    fun onLookChanged() {
        lookChanged = true
        updateLines()
    }

    override fun isShown(): Boolean = overlay.checkCondition() || modern.checkCondition()

    override fun flush() {
        if (lookChanged) {
            lookChanged = false
            overlay.loadPosition()
            modern.loadPosition()
            shownView = null
        }
        if (Trackers.isLegacy(look)) {
            overlay.setLines(generateLines())
            return
        }
        val view = view()
        // Re-rendering means a full GuiLib layout pass, so only when something shown changed
        if (view == shownView) return
        shownView = view
        modern.setContent { content(view) }
    }

    /** Everything the new look shows, as data: equal views look the same. */
    protected abstract fun view(): V

    protected abstract fun NodeBuilder.content(view: V)

    /** A click in the inventory or chat on the element with [id] in the new look. */
    protected open fun elementClicked(id: String) {}

    /** The tooltip of the element with [id] in the new look, none for null. */
    protected open fun tooltip(id: String): String? = null

    /** Hides or shows the tracker line [key] again, only from the inventory where hidden lines are listed. */
    protected fun toggleHidden(key: String) {
        if (!isCraftingScreenOpen()) return
        val hideList = DataManager.sboData.hideTrackerLines
        if (key in hideList) hideList.remove(key) else hideList.add(key)
        updateLines()
    }

    /** The line [keys] in the "Custom" sort of tracker [orderName]: the saved ones first, the ones it doesn't have yet in their default order after. */
    protected fun customOrder(orderName: String, keys: List<String>): List<String> {
        val saved = DataManager.sboData.trackerOrder[orderName].orEmpty().filter { it in keys }.distinct()
        return saved + keys.filter { it !in saved }
    }

    /** [lines] in [sort], ties keep the default order. */
    protected fun <T> sortLines(lines: List<T>, sort: Trackers.LineSort, orderName: String, key: (T) -> String, amount: (T) -> Int): List<T> =
        when (sort) {
            Trackers.LineSort.DEFAULT -> lines
            Trackers.LineSort.HIGHEST -> lines.sortedByDescending(amount)
            Trackers.LineSort.LOWEST -> lines.sortedBy(amount)
            Trackers.LineSort.CUSTOM -> customOrder(orderName, lines.map(key)).let { order -> lines.sortedBy { order.indexOf(key(it)) } }
        }

    /**
     * A click on a move arrow of [rows] moves that line of tracker [orderName] past the next [shown] line above or
     * below it (only the shown ones, so every click visibly moves it). False for other elements.
     */
    protected fun moveClicked(id: String, orderName: String, keys: List<String>, shown: () -> List<String>): Boolean {
        val up = id.startsWith("up:")
        if (!up && !id.startsWith("down:")) return false
        val key = id.substringAfter(':')
        val lines = shown()
        val index = lines.indexOf(key)
        val neighbour = lines.getOrNull(if (up) index - 1 else index + 1)
        if (index < 0 || neighbour == null) return true
        val order = customOrder(orderName, keys).toMutableList()
        order.remove(key)
        order.add(order.indexOf(neighbour) + if (up) 0 else 1, key)
        DataManager.sboData.trackerOrder[orderName] = order
        updateLines()
        return true
    }

    /** The rows, with ▲ ▼ to move them in the custom order when [reorder]; rows with the same [key] move together. */
    protected fun NodeBuilder.rows(rows: List<Row>, reorder: Boolean, key: (Row) -> String? = { it.id }) {
        val keys = rows.mapNotNull(key).distinct()
        rows.forEach { row ->
            val line = key(row)
            if (!reorder || line == null) row(row)
            else row(row) {
                moveArrow("▲", "up:$line", line != keys.first())
                moveArrow("▼", "down:$line", line != keys.last())
            }
        }
    }

    // Without [enabled] it keeps its place but can't be clicked
    private fun NodeBuilder.moveArrow(arrow: String, id: String, enabled: Boolean) {
        span(className = if (enabled) "tracker-move" else "tracker-move off", id = if (enabled) id else null) { +arrow }
    }

    /** One line of the new look; [id] makes it clickable and hoverable as "line:<id>". */
    data class Row(
        val id: String?,
        val name: String,
        val color: ChatFormatting?,
        val value: String,
        val extra: String? = null,
        val price: String? = null,
        val hidden: Boolean = false,
        val valueClass: String? = null
    )

    /** The outer box, with the font and background settings. */
    protected fun NodeBuilder.tracker(accent: String, children: NodeBuilder.() -> Unit) {
        val classes = buildString {
            append("tracker ").append(accent)
            if (Trackers.trackerFont == Trackers.TrackerFont.INTER) append(" inter")
            if (!Trackers.hasBackground(look)) append(" no-bg")
        }
        div(className = classes) {
            children()
            if (showLookNotice()) lookNotice("tracker")
        }
    }

    protected fun NodeBuilder.head(title: String, badges: NodeBuilder.() -> Unit = {}) {
        div(className = "tracker-head") {
            span(className = "tracker-title") { +title }
            badges()
        }
    }

    /** A small label in the head, clickable (with [id]) in the inventory. */
    protected fun NodeBuilder.badge(text: String, id: String? = null, clickable: Boolean = false, extraClass: String? = null) {
        val classes = buildString {
            append("tracker-badge")
            if (clickable) append(" clickable")
            if (extraClass != null) append(' ').append(extraClass)
        }
        span(className = classes, id = if (clickable) id else null) { +text }
    }

    /** One line, [controls] are added at its end. */
    protected fun NodeBuilder.row(row: Row, controls: NodeBuilder.() -> Unit = {}) {
        div(className = if (row.hidden) "tracker-row hidden" else "tracker-row", id = row.id?.let { "line:$it" }) {
            span(className = row.color?.let { "tracker-name ${colorClass(it)}" } ?: "tracker-name") { +row.name }
            span(className = row.valueClass?.let { "tracker-value $it" } ?: "tracker-value") { +row.value }
            if (row.extra != null) span(className = "tracker-extra") { +row.extra }
            if (row.price != null) span(className = "tracker-price") { +row.price }
            controls()
        }
    }

    protected fun NodeBuilder.separator() {
        div(className = "tracker-sep")
    }

    protected fun NodeBuilder.hint(text: String) {
        div(className = "tracker-hint") { +text }
    }

    companion object {
        val SCREENS = listOf(CHAT_SCREEN_FILTER, CRAFTING_PLAYER_INVENTORY_FILTER)

        // Minecraft's color codes as classes, e.g. "mc-light_purple"
        fun colorClass(color: ChatFormatting) = "mc-${color.name.lowercase()}"

        /** True until the player answered the new look notice, see [lookNotice]. */
        fun showLookNotice(): Boolean = !DataManager.sboData.trackerLookAnswered

        /** The id of a row element, null for other elements. */
        fun rowId(id: String): String? = id.removePrefix("line:").takeIf { it.length != id.length }
    }
}

fun NodeBuilder.lookNotice(prefix: String) {
    div(className = "$prefix-notice") {
        div(className = "$prefix-notice-title") { +"New tracker look!" }
        div(className = "$prefix-notice-line") {
            span(className = "$prefix-notice-cmd") { +"/sbolegacylook yes" }
            span { +"old look back" }
        }
        div(className = "$prefix-notice-line") {
            span(className = "$prefix-notice-cmd") { +"/sbolegacylook no" }
            span { +"keep this one" }
        }
    }
}
