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
import net.sbo.guilib.core.dsl.useEscapeBack
import net.sbo.guilib.fabric.GuiLib
import net.sbo.guilib.fabric.GuiLibScreen
import net.sbo.mod.guis.look.SboLook
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

    fun open(config: Config, title: String, onBack: (() -> Unit)? = null) {
        GuiLib.open(app(config, title, onBack), STYLES, title)
    }

    fun screen(config: Config, title: String, onBack: (() -> Unit)? = null): GuiLibScreen =
        GuiLib.screen(app(config, title, onBack), STYLES, title)

    private fun app(config: Config, title: String, onBack: (() -> Unit)?) =
        component(title) { App(AppProps(config, onBack)) }
}

private class Ui(
    val changed: () -> Unit,
    val show: (Category, ConfigElement) -> Unit,
)

private val UiContext = createContext<Ui?>(null, "ConfigUi")

private data class AppProps(val config: Config, val onBack: (() -> Unit)?)

private val App = component<AppProps>("ConfigApp") { (config, onBack) ->
    val hasGeneral = config.elements.isNotEmpty()
    var page by useState<Category>(if (hasGeneral) config else config.subcategories.firstOrNull() ?: config)
    var query by useState("")
    var target by useState<ConfigElement?>(null)
    val revision = useState(0)
    val body = useElementRef()
    useSboScale()
    useSboTheme()

    val ui = useMemo {
        Ui(
            changed = {
                revision.update { it + 1 }
                config.save()
            },
            show = { category, element ->
                page = category
                query = ""
                target = element
            },
        )
    }
    val hits = useMemo(query, revision.value) { search(config, query) }
    val searching = query.isNotBlank()

    // Typing and dragging only save when the screen closes
    useEffect { onCleanup { config.save() } }
    useEscapeBack(searching) { query = "" }
    useEffect(page, searching) { body.current?.scrollTop = 0f }
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
                button(className = "cfg-close", title = "Close", onClick = { GuiLib.close() }) { +"✕" }
            }

            div(className = "cfg-main") {
                aside(className = "cfg-sidebar") {
                    scroll(className = "cfg-categories guilib-autohide") {
                        if (hasGeneral) {
                            categoryLink(config, "General", page === config && !searching, hits.count { it.path.size == 1 }, searching) {
                                page = config
                                query = ""
                            }
                        }
                        config.subcategories.forEach { category ->
                            val count = hits.count { it.path.getOrNull(1) === category }
                            categoryLink(category, category.name, page === category && !searching, count, searching) {
                                page = category
                                query = ""
                            }
                        }
                    }
                }

                scroll(className = "cfg-body guilib-autohide", ref = body) {
                    when {
                        searching -> results(query, hits, revision.value)
                        page === config -> {
                            h1(className = "cfg-page-title") { +"General" }
                            elements(config, revision.value, target)
                        }
                        else -> categoryPage(page, revision.value, target)
                    }
                }
            }
        }
    }
}

private fun NodeBuilder.categoryLink(
    category: Category,
    label: String,
    active: Boolean,
    count: Int,
    searching: Boolean,
    onClick: () -> Unit,
) {
    val classes = listOfNotNull(
        "cfg-category",
        "active".takeIf { active },
        "no-hits".takeIf { searching && count == 0 },
    ).joinToString(" ")
    button(className = classes, key = "cat:${category.id}", onClick = { onClick() }) {
        span(className = "cfg-category-name") { +label }
        if (searching && count > 0) span(className = "cfg-count") { +count.toString() }
    }
}

private fun NodeBuilder.categoryPage(category: Category, revision: Int, target: ConfigElement?) {
    h1(className = "cfg-page-title") { +category.name }
    if (category.description.isNotEmpty()) description(category.description, "cfg-page-description")
    elements(category, revision, target)
    category.subcategories.forEach { sub ->
        h2(className = "cfg-subcategory", key = "sub:${sub.id}") { +sub.name }
        elements(sub, revision, target)
    }
}

private fun NodeBuilder.elements(category: Category, revision: Int, target: ConfigElement?) {
    category.elements.forEachIndexed { i, element ->
        when (element) {
            is Separator -> if (element.condition()) {
                div(className = "cfg-section", key = "${category.id}:sep:$i") {
                    if (element.title.isNotEmpty()) h2(className = "cfg-section-title") { +element.title }
                    if (element.description.isNotEmpty()) description(element.description, "cfg-section-description")
                }
            }
            is Button -> if (element.condition()) Row(RowProps(element, revision, element === target), key = "${category.id}:btn:$i")
            is ConfigEntry<*> -> Row(RowProps(element, revision, element === target), key = "${category.id}:${element.id}")
        }
    }
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

    val classes = listOfNotNull("cfg-row", "highlighted".takeIf { highlighted }).joinToString(" ")
    div(className = classes, id = rowId(element)) {
        div(className = "cfg-row-text") {
            div(className = "cfg-row-name") { +rowName(element) }
            if (hit != null) {
                val place = listOfNotNull(placeName(hit.path), hit.section?.title?.takeIf { it.isNotEmpty() }).joinToString(" › ")
                span(className = "cfg-row-place", title = "Show it in its category", onClick = { ui.show(hit.category, element) }) {
                    +"$place ↗"
                }
            }
            val text = rowDescription(element)
            if (text.isNotEmpty()) description(text, "cfg-row-description")
        }
        div(className = "cfg-row-control") {
            when (element) {
                is Button -> button(className = "cfg-button", onClick = {
                    element.onClick()
                    ui.changed()
                }) { +(element.text ?: "Open") }
                is ConfigEntry<*> -> control(element, ui, refresh)
                is Separator -> {}
            }
        }
    }
}

private fun rowName(element: ConfigElement) = when (element) {
    is Button -> element.title
    is ConfigEntry<*> -> element.name
    is Separator -> element.title
}

private fun rowDescription(element: ConfigElement) = when (element) {
    is Button -> element.description
    is ConfigEntry<*> -> element.description
    is Separator -> element.description
}

private fun NodeBuilder.control(entry: ConfigEntry<*>, ui: Ui, refresh: () -> Unit) {
    when (entry) {
        is BooleanEntry -> switch(checked = entry.value, onChange = {
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
        is EnumEntry<*> -> {
            select(value = entry.value.name, className = "cfg-select", onChange = { event ->
                entry.setName(event.value)
                ui.changed()
            }) {
                entry.constants.forEach { option(it.name, it.toString()) }
            }
        }
        is SelectEntry<*> -> {
            multiSelect(values = entry.value.map { it.name }, className = "cfg-select", placeholder = "None", onChange = { names ->
                entry.setNames(names)
                ui.changed()
            }) {
                entry.constants.forEach { option(it.name, it.toString()) }
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

private fun NodeBuilder.strings(entry: StringsEntry, ui: Ui, refresh: () -> Unit) {
    div(className = "cfg-strings") {
        entry.value.forEachIndexed { i, line ->
            div(className = "cfg-strings-line", key = i) {
                input(className = "cfg-text", value = line, onInput = {
                    entry.set(entry.value.copyOf().also { lines -> lines[i] = it.value })
                    refresh()
                })
                if (entry.value.size > 1) {
                    button(className = "cfg-strings-remove", title = "Remove this line", onClick = {
                        entry.set(entry.value.filterIndexed { j, _ -> j != i }.toTypedArray())
                        ui.changed()
                    }) { +"✕" }
                }
            }
        }
        button(className = "cfg-strings-add", onClick = {
            entry.set(entry.value + "")
            ui.changed()
        }) { +"+ Add line" }
    }
}
