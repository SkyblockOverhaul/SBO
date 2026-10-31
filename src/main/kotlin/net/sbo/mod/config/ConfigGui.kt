package net.sbo.mod.config

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dom.createContext
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.aside
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.colorInput
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h1
import net.sbo.guilib.core.dsl.h2
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.input
import net.sbo.guilib.core.dsl.multiSelect
import net.sbo.guilib.core.dsl.numberInput
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.slider
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch
import net.sbo.guilib.core.dsl.textarea
import net.sbo.guilib.core.dsl.useEscapeBack
import net.sbo.guilib.fabric.GuiLib
import net.sbo.guilib.fabric.GuiLibScreen
import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

object ConfigGui {
    private val STYLES = listOf("sbo:ui/config/config.css", SboLook.STYLE)

    fun open(config: Config, title: String, onBack: (() -> Unit)? = null, whatsNew: WhatsNew? = null) {
        GuiLib.open(app(config, title, onBack, whatsNew), STYLES, title)
    }

    fun screen(config: Config, title: String, onBack: (() -> Unit)? = null, whatsNew: WhatsNew? = null): GuiLibScreen =
        GuiLib.screen(app(config, title, onBack, whatsNew), STYLES, title)

    private fun app(config: Config, title: String, onBack: (() -> Unit)?, whatsNew: WhatsNew?) =
        component(title) { App(AppProps(config, onBack, whatsNew?.takeIf { it.hits.isNotEmpty() })) }
}

private class Ui(
    val changed: () -> Unit,
    val show: (Category, ConfigElement) -> Unit,
    /** Shows [ConfigElement] wherever it is, used by the requirement links. */
    val jump: (ConfigElement) -> Unit,
    val collapsed: Set<String>,
    val toggle: (String) -> Unit,
    /** Opens [StringsEntry] in the big editor. */
    val edit: (StringsEntry) -> Unit,
)

private val UiContext = createContext<Ui?>(null, "ConfigUi")

private data class AppProps(val config: Config, val onBack: (() -> Unit)?, val whatsNew: WhatsNew?)

private val App = component<AppProps>("ConfigApp") { (config, onBack, whatsNew) ->
    val hasGeneral = config.elements.isNotEmpty()
    // null is the home page with every category below each other
    var page by useState<Category?>(null)
    // What's New is shown instead of the page while this is on
    var showNew by useState(whatsNew?.openFirst == true)
    // The count only marks it as not looked at yet
    var newSeen by useState(whatsNew?.seen == true)
    var query by useState("")
    var target by useState<ConfigElement?>(null)
    var editing by useState<StringsEntry?>(null)
    var collapsed by useState<Set<String>>(DataManager.sboData.collapsedSettings.toSet())
    val revision = useState(0)
    val body = useElementRef()
    useSboScale()
    useSboTheme()

    fun setCollapsed(keys: Set<String>) {
        collapsed = keys
        DataManager.sboData.collapsedSettings = keys.toMutableSet()
        DataManager.sboData.save()
    }

    fun show(category: Category, element: ConfigElement) {
        // The home page has it too, so that stays
        if (page != null) page = category
        showNew = false
        query = ""
        target = element
        // Jumping from the search into a collapsed sub opens it
        collapseKeyOf(category, element)?.let { setCollapsed(collapsed - it) }
    }

    val ui = useMemo(collapsed) {
        Ui(
            changed = {
                revision.update { it + 1 }
                config.save()
            },
            show = ::show,
            jump = { element ->
                categoryOf(config, element)?.let { category ->
                    // A setting in a switched off sub is hidden, so that goes to the sub instead
                    val offSub = (element as? ConfigEntry<*>)?.sub?.takeIf { !it.enabled }
                    show(category, offSub ?: element)
                }
            },
            collapsed = collapsed,
            toggle = { key -> setCollapsed(if (key in collapsed) collapsed - key else collapsed + key) },
            edit = { editing = it },
        )
    }
    val hits = useMemo(query, revision.value) { search(config, query) }
    val searching = query.isNotBlank()

    // Typing and dragging only save when the screen closes
    useEffect { onCleanup { config.save() } }
    useEscapeBack(searching) { query = "" }
    useEffect(page, searching, showNew) { body.current?.scrollTop = 0f }
    useEffect(showNew) {
        if (!showNew) return@useEffect
        whatsNew?.onOpened()
        newSeen = true
    }
    useEffect(target) {
        val element = target ?: return@useEffect
        // A few frames later the category page is laid out
        setTimeout(60) {
            val container = body.current ?: return@setTimeout
            val row = container.querySelector("#${rowId(element)}") ?: return@setTimeout
            container.scrollTop += row.getBoundingClientRect().y - container.getBoundingClientRect().y - 24f
        }
        setTimeout(2000) { target = null }
    }

    UiContext.Provider(ui) {
        div(className = "cfg-window") {
            header(className = "cfg-header") {
                if (onBack != null) {
                    button(className = "cfg-back", title = "Back", onClick = { onBack() }) { +"‹" }
                }
                span(className = "cfg-title") { +config.name }
                div(className = "cfg-search") {
                    input(
                        className = "cfg-search-input",
                        value = query,
                        placeholder = "Search all settings…",
                        autoFocus = false,
                        onInput = { query = it.value },
                    )
                    if (searching) {
                        button(className = "cfg-search-clear", title = "Clear the search", onClick = { query = "" }) { +"✕" }
                    }
                }
                button(className = "cfg-close", title = "Close", onClick = { GuiLib.close() }) { +"x" }
            }

            div(className = "cfg-main") {
                aside(className = "cfg-sidebar") {
                    scroll(className = "cfg-categories guilib-autohide") {
                        if (whatsNew != null) {
                            val classes = listOfNotNull("cfg-category", "home", "whats-new", "active".takeIf { showNew && !searching }).joinToString(" ")
                            button(className = classes, key = "cat:new", onClick = {
                                showNew = true
                                query = ""
                            }) {
                                span(className = "cfg-category-name") { +"What's New" }
                                if (!newSeen) span(className = "cfg-count") { +whatsNew.hits.size.toString() }
                            }
                        }
                        categoryLink("home", "All Settings", page == null && !showNew && !searching, hits.size, searching, home = true) {
                            page = null
                            showNew = false
                            query = ""
                        }
                        if (hasGeneral) {
                            categoryLink(config.id, "General", page === config && !showNew && !searching, hits.count { it.path.size == 1 }, searching) {
                                page = config
                                showNew = false
                                query = ""
                            }
                        }
                        config.children.forEach { category ->
                            val count = hits.count { it.path.getOrNull(1) === category }
                            categoryLink(category.id, category.name, page === category && !showNew && !searching, count, searching) {
                                page = category
                                showNew = false
                                query = ""
                            }
                        }
                    }
                }

                scroll(className = "cfg-body guilib-autohide", ref = body) {
                    when {
                        searching -> results(query, hits, revision.value)
                        showNew && whatsNew != null -> whatsNewPage(whatsNew, revision.value, ui)
                        page == null -> home(config, hasGeneral, revision.value, target, ui) { page = it }
                        page === config -> {
                            h1(className = "cfg-page-title") { +"General" }
                            elements(config, revision.value, target, ui)
                        }
                        else -> page?.let { categoryPage(it, revision.value, target, ui) }
                    }
                }
            }

            editing?.let { entry ->
                StringsEditor(EditorProps(entry) {
                    editing = null
                    ui.changed()
                })
            }
        }
    }
}

private fun NodeBuilder.categoryLink(
    id: String,
    label: String,
    active: Boolean,
    count: Int,
    searching: Boolean,
    home: Boolean = false,
    onClick: () -> Unit,
) {
    val classes = listOfNotNull(
        "cfg-category",
        "home".takeIf { home },
        "active".takeIf { active },
        "no-hits".takeIf { searching && count == 0 },
    ).joinToString(" ")
    button(className = classes, key = "cat:$id", onClick = { onClick() }) {
        span(className = "cfg-category-name") { +label }
        if (searching && count > 0) span(className = "cfg-count") { +count.toString() }
    }
}

// The settings added since the last version, with switches for all of them at once
private fun NodeBuilder.whatsNewPage(whatsNew: WhatsNew, revision: Int, ui: Ui) {
    h1(className = "cfg-page-title") { +whatsNew.title }
    if (whatsNew.text.isNotEmpty()) description(whatsNew.text, "cfg-page-description")
    val toggles = whatsNew.hits.mapNotNull { it.element as? ConfigEntry<*> }.filter(::switchable)
    if (toggles.isNotEmpty()) {
        div(className = "cfg-new-actions") {
            button(className = "cfg-button", title = "Turn every new feature below on", onClick = {
                toggles.forEach { switchTo(it, true) }
                ui.changed()
            }) { +"Enable all" }
            button(className = "cfg-button", title = "Turn every new feature below off", onClick = {
                toggles.forEach { switchTo(it, false) }
                ui.changed()
            }) { +"Disable all" }
        }
    }
    whatsNew.extra?.let { Row(RowProps(it, revision, false), key = "new:extra") }
    whatsNew.hits.groupBy { (it.element as ConfigEntry<*>).since }.forEach { (since, group) ->
        div(className = "cfg-result-group", key = "new:$since") {
            h2(className = "cfg-result-category") { +"New in $since" }
            group.forEach { hit ->
                Row(RowProps(hit.element, revision, false, hit), key = "new:${System.identityHashCode(hit.element)}")
            }
        }
    }
}

// Every category below each other, so all settings can be scrolled through at once; a title opens the category alone
private fun NodeBuilder.home(config: Config, hasGeneral: Boolean, revision: Int, target: ConfigElement?, ui: Ui, open: (Category) -> Unit) {
    if (hasGeneral) {
        div(className = "cfg-home-section", key = "home:${config.id}") {
            h1(className = "cfg-page-title cfg-home-title", title = "Show only General", onClick = { _ -> open(config) }) { +"General" }
            elements(config, revision, target, ui)
        }
    }
    config.children.forEach { category ->
        div(className = "cfg-home-section", key = "home:${category.id}") {
            h1(className = "cfg-page-title cfg-home-title", title = "Show only ${category.name}", onClick = { _ -> open(category) }) { +category.name }
            if (category.description.isNotEmpty()) description(category.description, "cfg-page-description")
            elements(category, revision, target, ui)
            category.children.forEach { child ->
                h2(className = "cfg-child-category", key = "child:${child.id}") { +child.name }
                elements(child, revision, target, ui)
            }
        }
    }
}

private fun NodeBuilder.categoryPage(category: Category, revision: Int, target: ConfigElement?, ui: Ui) {
    h1(className = "cfg-page-title") { +category.name }
    if (category.description.isNotEmpty()) description(category.description, "cfg-page-description")
    elements(category, revision, target, ui)
    category.children.forEach { child ->
        h2(className = "cfg-child-category", key = "child:${child.id}") { +child.name }
        elements(child, revision, target, ui)
    }
}

private fun NodeBuilder.elements(category: Category, revision: Int, target: ConfigElement?, ui: Ui) {
    var hidden = false
    category.elements.forEachIndexed { i, element ->
        when (element) {
            is Sub -> {
                hidden = false
                if (!element.condition()) return@forEachIndexed
                val key = collapseKey(category, element)
                SubHeader(SubHeaderProps(element, element.enabled, key, ui), key = "${category.id}:sub:$i")
                hidden = element.toggleable && (!element.enabled || key in ui.collapsed)
            }
            is Button -> if (!hidden && element.condition()) Row(RowProps(element, revision, element === target), key = "${category.id}:btn:$i")
            is ConfigEntry<*> -> if (!hidden) Row(RowProps(element, revision, element === target), key = "${category.id}:${element.id}")
        }
    }
}

// enabled is part of the props so the header redraws when the sub is switched
private data class SubHeaderProps(val sub: Sub, val enabled: Boolean, val key: String, val ui: Ui)

// A sub without a title only separates, there is nothing to click or switch
private val SubHeader = component<SubHeaderProps>("ConfigSub") { (sub, enabled, key, ui) ->
    val collapsed = sub.toggleable && key in ui.collapsed
    val classes = listOfNotNull(
        "cfg-sub",
        "collapsible".takeIf { sub.toggleable && enabled },
        "collapsed".takeIf { collapsed || !enabled },
        "off".takeIf { !enabled },
    ).joinToString(" ")
    div(className = classes, id = rowId(sub)) {
        if (sub.toggleable) {
            div(className = "cfg-sub-head") {
                h2(className = "cfg-sub-title", onClick = if (enabled) { _ -> ui.toggle(key) } else null) {
                    span(className = "cfg-chevron")
                    +sub.title
                }
                // A small pill, a switch here looked like one more setting
                button(
                    className = "cfg-sub-toggle",
                    title = if (enabled) "Turn off everything in ${sub.title}" else "Turn ${sub.title} back on",
                    onClick = {
                        sub.setEnabled(!enabled)
                        ui.changed()
                    },
                ) { +(if (enabled) "ON" else "OFF") }
            }
        }
        if (sub.description.isNotEmpty()) description(sub.description, "cfg-sub-description")
    }
}

// By title, so it stays collapsed when settings are added or moved
private fun collapseKey(category: Category, sub: Sub) = "${category.id}/${sub.title.stripFormatting()}"

private fun collapseKeyOf(category: Category, element: ConfigElement): String? {
    for (owner in listOf(category) + category.children) {
        var current: Sub? = null
        for (e in owner.elements) {
            if (e is Sub) current = e.takeIf { it.title.isNotEmpty() && it.condition() }
            if (e === element) return current?.let { collapseKey(owner, it) }
        }
    }
    return null
}

private fun NodeBuilder.results(query: String, hits: List<SearchHit>, revision: Int) {
    if (hits.isEmpty()) {
        div(className = "cfg-empty") { +"No settings match \"${query.trim()}\"." }
        return
    }
    div(className = "cfg-results-info") { +(if (hits.size == 1) "1 setting found" else "${hits.size} settings found") }
    hits.groupBy { it.category }.forEach { (category, group) ->
        div(className = "cfg-result-group", key = "group:${category.id}") {
            h2(className = "cfg-result-category") { +placeName(group.first().path) }
            group.forEach { hit ->
                Row(RowProps(hit.element, revision, false, hit), key = "hit:${category.id}:${System.identityHashCode(hit.element)}")
            }
        }
    }
}

private fun placeName(path: List<Category>) = if (path.size == 1) "General" else path.drop(1).joinToString(" › ") { it.name }

private fun NodeBuilder.description(text: String, className: String) {
    div(className = className) {
        text.lines().forEach { line -> div(className = "cfg-line") { +line.ifEmpty { " " } } }
    }
}

private fun rowId(element: ConfigElement) = "cfg-row-${System.identityHashCode(element)}"

private data class RowProps(
    val element: ConfigElement,
    val revision: Int,
    val highlighted: Boolean,
    val hit: SearchHit? = null,
)

private val Row = component<RowProps>("ConfigRow") { (element, _, highlighted, hit) ->
    val refresh = useForceUpdate()
    val ui = useContext(UiContext) ?: return@component

    // Locked while a setting from depends(...) is off
    val locked = element is ConfigEntry<*> && !element.requirementsMet
    val classes = listOfNotNull("cfg-row", "highlighted".takeIf { highlighted }, "locked".takeIf { locked }).joinToString(" ")
    div(className = classes, id = rowId(element)) {
        div(className = "cfg-row-text") {
            div(className = "cfg-row-name") { +rowName(element) }
            if (hit != null) {
                val place = listOfNotNull(placeName(hit.path), hit.sub?.title?.takeIf { it.isNotEmpty() }).joinToString(" › ")
                // The setting is hidden on the page while its sub is off, so that jumps to the sub instead
                val offSub = hit.sub?.takeIf { !it.enabled }
                span(className = "cfg-row-place", title = "Show it in its category", onClick = { ui.show(hit.category, offSub ?: element) }) {
                    +(if (offSub != null) "$place · off ↗" else "$place ↗")
                }
            }
            val text = rowDescription(element)
            if (text.isNotEmpty()) description(text, "cfg-row-description")
            // Only while it can't be used, otherwise it's just noise
            if (locked) needs(element, ui)
        }
        div(className = "cfg-row-control") {
            when (element) {
                is Button -> button(className = "cfg-button", onClick = {
                    element.onClick()
                    ui.changed()
                }) { +(element.text ?: "Open") }
                is ConfigEntry<*> -> control(element, ui, refresh, locked)
                is Sub -> {}
            }
        }
    }
}

// Every required setting links to itself, green when it is as needed and red when not
private fun NodeBuilder.needs(entry: ConfigEntry<*>, ui: Ui) {
    div(className = "cfg-row-needs") {
        +"Needs: "
        entry.requirements.forEachIndexed { i, requirement ->
            if (i > 0) +", "
            val conditions = requirement.conditions.mapNotNull { condition -> condition.entry?.let { condition to it } }
            if (requirement.any && conditions.size > 1) +"one of "
            conditions.forEachIndexed { j, (condition, required) ->
                if (j > 0) +(if (requirement.any) " or " else ", ")
                val state = when {
                    condition.met -> "ok"
                    // In a met "one of" the others don't need to change
                    requirement.met -> "idle"
                    else -> "missing"
                }
                val label = listOf(required.name, condition.state).filter { it.isNotEmpty() }.joinToString(" ")
                span(className = "cfg-needs-link $state", title = "Show ${required.name}", onClick = { ui.jump(required) }) { +label }
            }
        }
    }
}

private fun categoryOf(category: Category, element: ConfigElement): Category? =
    if (element in category.elements) category else category.children.firstNotNullOfOrNull { categoryOf(it, element) }

private fun rowName(element: ConfigElement) = when (element) {
    is Button -> element.title
    is ConfigEntry<*> -> element.name
    is Sub -> element.title
}

private fun rowDescription(element: ConfigElement) = when (element) {
    is Button -> element.description
    is ConfigEntry<*> -> element.description
    is Sub -> element.description
}

private fun NodeBuilder.control(entry: ConfigEntry<*>, ui: Ui, refresh: () -> Unit, locked: Boolean) {
    when (entry) {
        is BooleanEntry -> switch(checked = entry.value, disabled = locked, onChange = {
            entry.set(it.checked)
            ui.changed()
        })
        is NumberEntry<*> -> number(entry, ui, refresh)
        is ColorEntry -> colorInput(value = entry.value, alpha = entry.allowAlpha, onChange = {
            entry.set(it)
            ui.changed()
        })
        is StringEntry -> input(className = "cfg-text", value = entry.value, onInput = {
            entry.set(it.value)
            refresh()
        })
        is StringsEntry -> strings(entry, ui, refresh)
        is ChoiceEntry -> {
            val options = entry.options()
            select(value = entry.value, className = "cfg-select", onChange = { event ->
                entry.set(event.value)
                ui.changed()
            }) {
                options.forEach { option(it.id, it.label, title = it.description.ifEmpty { null }) }
            }
        }
        is OptionEntry<*> -> {
            select(value = entry.value.name, className = "cfg-select", onChange = { event ->
                entry.setName(event.value)
                ui.changed()
            }) {
                entry.constants.forEach { option(it.name, it.optionLabel, title = it.optionDescription.ifEmpty { null }) }
            }
        }
        is OptionsEntry<*> -> {
            multiSelect(values = entry.value.map { it.name }, className = "cfg-select", placeholder = "None", onChange = { names ->
                entry.setNames(names)
                ui.changed()
            }) {
                entry.constants.forEach { option(it.name, it.optionLabel, title = it.optionDescription.ifEmpty { null }) }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun NodeBuilder.number(entry: NumberEntry<*>, ui: Ui, refresh: () -> Unit) {
    val range = entry.range
    when (val value = entry.value) {
        is Int -> {
            val e = entry as NumberEntry<Int>
            val r = range as ClosedRange<Int>?
            if (r != null && e.slider) {
                slider(value = value, min = r.start, max = r.endInclusive, showValue = true, className = "cfg-slider",
                    onChange = { e.set(it); refresh() }, onChangeEnd = { ui.changed() })
            } else {
                numberInput(value = value, min = r?.start ?: Int.MIN_VALUE, max = r?.endInclusive ?: Int.MAX_VALUE,
                    className = "cfg-number", onChange = { e.set(it); ui.changed() })
            }
        }
        is Float, is Double -> {
            val min = range?.start?.toDouble()
            val max = range?.endInclusive?.toDouble()
            val step = if (min != null && max != null) niceStep(max - min) else 0.1
            fun set(v: Double) = when (value) {
                is Float -> (entry as NumberEntry<Float>).set(v.toFloat())
                else -> (entry as NumberEntry<Double>).set(v)
            }
            if (min != null && max != null && entry.slider) {
                slider(value = value.toFloat(), min = min.toFloat(), max = max.toFloat(), step = step.toFloat(), showValue = true,
                    format = { format(it.toDouble(), step) }, className = "cfg-slider",
                    onChange = { set(it.toDouble()); refresh() }, onChangeEnd = { ui.changed() })
            } else {
                numberInput(value = value.toDouble(), min = min ?: -Double.MAX_VALUE, max = max ?: Double.MAX_VALUE, step = step,
                    className = "cfg-number", onChange = { set(it); ui.changed() })
            }
        }
        else -> span(className = "cfg-unsupported") { +value.toString() }
    }
}

private fun niceStep(span: Double): Double {
    if (span <= 0) return 0.1
    val raw = span / 100
    val power = 10.0.pow(floor(log10(raw)))
    val factor = listOf(1.0, 2.0, 5.0, 10.0).first { it * power >= raw - 1e-12 }
    return BigDecimal.valueOf(factor * power).round(MathContext(1)).toDouble()
}

private fun format(value: Double, step: Double): String {
    val decimals = BigDecimal.valueOf(step).stripTrailingZeros().scale().coerceAtLeast(0)
    return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toPlainString()
}

// One line of the field is one entry of the setting
private fun NodeBuilder.strings(entry: StringsEntry, ui: Ui, refresh: () -> Unit) {
    div(className = "cfg-strings") {
        textarea(className = "cfg-textarea", value = entry.value.joinToString("\n"), rows = 2, onInput = {
            entry.set(it.value.split("\n").toTypedArray())
            refresh()
        })
        button(className = "cfg-strings-expand", title = "Edit in a bigger field", onClick = { ui.edit(entry) }) { +"⤢" }
    }
}

private data class EditorProps(val entry: StringsEntry, val close: () -> Unit)

// Covers the whole window, Escape or Done goes back
private val StringsEditor = component<EditorProps>("ConfigStringsEditor") { (entry, close) ->
    val refresh = useForceUpdate()
    useEscapeBack { close() }
    div(className = "cfg-editor") {
        div(className = "cfg-editor-header") {
            span(className = "cfg-editor-title") { +entry.name }
            button(className = "cfg-editor-done", onClick = { close() }) { +"Done" }
        }
        if (entry.description.isNotEmpty()) description(entry.description, "cfg-row-description")
        textarea(className = "cfg-editor-text", value = entry.value.joinToString("\n"), rows = 12, autoFocus = true, onInput = {
            entry.set(it.value.split("\n").toTypedArray())
            refresh()
        })
    }
}
