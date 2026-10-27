package net.sbo.mod.test

import net.sbo.guilib.core.UiRoot
import net.sbo.guilib.core.css.Origin
import net.sbo.guilib.core.css.Stylesheet
import net.sbo.guilib.core.dom.Element
import net.sbo.guilib.core.dom.VComponent
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.*
import net.sbo.guilib.core.paint.PaintCommand
import java.nio.file.Files
import net.sbo.mod.config.useSettingsPickerFocus
import net.sbo.mod.config.settingsDescription
import net.sbo.mod.config.settingsSectionHeader

/** Real GuiLib controls with source-derived labels/ranges/options and isolated disposable state. */
internal class DianaReviewFixture(
    css: String,
    override val width: Float = 640f,
    override val height: Float = 480f,
    theme: String = "sbo-dark",
    val mode: String = "expanded",
    val section: String? = null,
    val pickerFocus: Boolean = true,
    val lightContrast: Boolean = true,
    val categoryName: String = "Diana",
    categoryEntries: List<DianaReviewData.Entry>? = null,
    val categoryGroups: List<DianaReviewData.Group> = DianaReviewData.groups,
    val categoryDescription: String = "",
    val valueOverrides: Map<String, String> = emptyMap(),
    val externalEntries: List<DianaReviewData.Entry> = emptyList(),
    val collapsedGroups: Set<String> = emptySet(),
    val offGroups: Set<String> = emptySet(),
    val sectionSwitches: Boolean = true,
) : ReviewFrame {
    companion object {
        val currentCss get() = SettingsReviewFixture.currentCss
        val baselineCss get() = Files.readString(ReviewResources.project.resolve("tools/ui-review/baseline/diana-config.css"))
        private fun sheet(path: String, origin: Origin = Origin.AUTHOR) = ReviewResources.resource(path).bufferedReader().use {
            Stylesheet.parse(it.readText(), path, origin)
        }
    }
    override val fonts = ReviewFonts()
    private var now = 0L
    val root = UiRoot(fonts, listOf(sheet("/assets/guilib/css/ua.css", Origin.USER_AGENT),
        sheet("/assets/sbo/ui/common.css"), Stylesheet.parse(css, "config.css"), sheet("/assets/sbo/ui/themes/themes.css")), clock = { now })
    var currentValues = emptyMap<String, String>()
        private set
    var currentLines = emptyMap<String, List<String>>()
        private set
    var queryValue = ""
        private set
    val data = categoryEntries ?: if (mode == "pickers") DianaReviewData.pickerEntries else DianaReviewData.entries
    private val dependencies = data + externalEntries

    private val app = component("Offline Diana settings") {
        if (pickerFocus) useSettingsPickerFocus()
        var query by useState(if (mode == "search") "tracker" else "")
        queryValue = query
        var collapsed by useState(collapsedGroups + if (mode == "collapsed") setOf("Diana Burrows") else emptySet())
        var off by useState(offGroups + if (mode == "off") setOf("Diana Burrows Preferences") else emptySet())
        var values by useState(dependencies.associate { it.id to if (mode == "dependent" && it.id in setOf("coaTracker", "lootTracker", "receiveRareMob", "shareRareMob")) {
            if (it.kind == "enum") "OFF" else "false"
        } else it.initial } + valueOverrides)
        var lines by useState(data.filter { it.kind == "strings" }.associate { it.id to
            if (mode == "strings") listOf("&6{amount} × {price}", "&b{mf}% Magic Find") else listOf(it.initial) })
        currentValues = values
        currentLines = lines
        var editing by useState<String?>(null)
        var target by useState<String?>(null)
        val body = useElementRef()
        val searching = query.isNotBlank()
        useEscapeBack(searching) { query = "" }
        useEffect(searching) { body.current?.scrollTop = 0f }
        useEffect(target) {
            val id = target ?: return@useEffect
            setTimeout(60) { scrollTo(id, false) }
            setTimeout(2000) { target = null }
        }
        fun jump(entry: DianaReviewData.Entry) {
            query = ""
            collapsed = collapsed - entry.section
            target = if (entry.section in off) "group-" + entry.section.replace(" ", "-") else entry.id
        }
        fun effectiveOn(id: String, visited: Set<String> = emptySet()): Boolean {
            check(id !in visited) { "Cyclic source fixture dependency: $id" }
            val entry = dependencies.first { it.id == id }
            val enabled = values[id] !in listOf("false", "OFF", null)
            if (entry.kind != "boolean") return enabled
            return enabled && entry.section !in off && entry.requirements.all { requirement ->
                val checks = requirement.conditions.map { condition ->
                    val on = effectiveOn(condition.id, visited + id)
                    if (condition.state == "OFF") !on else on
                }
                if (requirement.any) checks.any { it } else checks.all { it }
            }
        }
        fun conditionMet(condition: DianaReviewData.Condition): Boolean {
            val enabled = effectiveOn(condition.id)
            return if (condition.state == "OFF") !enabled else enabled
        }
        fun NodeBuilder.description(text: String, className: String) {
            settingsDescription(text, className, lightContrast && theme == "light")
        }
        fun NodeBuilder.row(entry: DianaReviewData.Entry, result: Boolean = false) {
            val locked = entry.requirements.any { requirement ->
                if (requirement.any) requirement.conditions.none(::conditionMet) else requirement.conditions.any { !conditionMet(it) }
            }
            div(className = classNames("cfg-row", "locked" to locked, "highlighted" to (target == entry.id)), id = "row-" + entry.id) {
                div(className = "cfg-row-text") {
                    div(className = "cfg-row-name") { +entry.name }
                    if (result) span(className = "cfg-row-place", onClick = { jump(entry) }) { +(categoryName + " › " + entry.section + " ↗") }
                    description(entry.description, "cfg-row-description")
                    if (locked) div(className = "cfg-row-needs") {
                        +"Needs: "
                        entry.requirements.forEachIndexed { i, requirement ->
                            if (i > 0) +", "
                            if (requirement.any && requirement.conditions.size > 1) +"one of "
                            requirement.conditions.forEachIndexed { j, condition ->
                                if (j > 0) +(if (requirement.any) " or " else ", ")
                                val required = dependencies.first { it.id == condition.id }
                                span(className = "cfg-needs-link " + if (conditionMet(condition)) "ok" else "missing", onClick = { if (required in data) jump(required) }) {
                                    +(required.name + " " + condition.state)
                                }
                            }
                        }
                    }
                }
                div(className = "cfg-row-control") {
                    when (entry.kind) {
                        "boolean" -> switch(checked = values[entry.id] == "true", disabled = locked, onChange = { values = values + (entry.id to it.checked.toString()) })
                        "int" -> numberInput(value = values.getValue(entry.id).toInt(), min = entry.minimum?.toInt() ?: Int.MIN_VALUE,
                            max = entry.maximum?.toInt() ?: Int.MAX_VALUE, className = "cfg-number", onChange = { values = values + (entry.id to it.toString()) })
                        "double" -> numberInput(value = values.getValue(entry.id).toDouble(), min = -Double.MAX_VALUE, max = Double.MAX_VALUE,
                            step = 0.1, className = "cfg-number", onChange = { values = values + (entry.id to it.toString()) })
                        "slider" -> slider(value = values.getValue(entry.id).toInt(), min = entry.minimum!!.toInt(), max = entry.maximum!!.toInt(),
                            showValue = true, className = "cfg-slider", onChange = { values = values + (entry.id to it.toString()) })
                        "float-slider" -> slider(value = values.getValue(entry.id).toFloat(), min = entry.minimum!!.toFloat(), max = entry.maximum!!.toFloat(),
                            step = 0.02f, showValue = true, format = { "%.2f".format(java.util.Locale.ROOT, it) }, className = "cfg-slider", onChange = { values = values + (entry.id to it.toString()) })
                        "enum", "choice" -> select(value = values.getValue(entry.id), className = "cfg-select", onChange = { values = values + (entry.id to it.value) }) {
                            entry.options.forEach { option(it, entry.optionLabels[it] ?: it, title = entry.optionDescriptions[it]) }
                        }
                        "select" -> multiSelect(values = values.getValue(entry.id).split(',').filter { it.isNotBlank() }, className = "cfg-select", placeholder = "None",
                            onChange = { values = values + (entry.id to it.joinToString(",")) }) { entry.options.forEach { option(it, it) } }
                        "strings" -> div(className = "cfg-strings") {
                            textarea(className = "cfg-textarea", value = lines.getValue(entry.id).joinToString("\n"), rows = 2,
                                onInput = { lines = lines + (entry.id to it.value.split("\n")) })
                            button(className = "cfg-strings-expand sbo-button", title = "Edit in a bigger field", onClick = { editing = entry.id }) { +"⤢" }
                        }
                        "button" -> button(className = "cfg-button sbo-button", onClick = {}) { +entry.buttonText }
                        "rgb", "rgba" -> colorInput(value = values.getValue(entry.id).toInt(), alpha = entry.kind == "rgba",
                            onChange = { values = values + (entry.id to it.toString()) })
                    }
                }
            }
        }
        val hits = data.filter { entry -> query.lowercase().split(' ').all { it in (entry.name + " " + entry.description + " " + entry.id + " " + entry.section).lowercase() } }
        div(className = "cfg-window") {
            header(className = "cfg-header") {
                button(className = "cfg-back", title = "Back", onClick = {}) { +"‹" }
                span(className = "cfg-title") { +"SBO 0.7.2 for MC 26.2" }
                div(className = "cfg-search") {
                    input(className = "cfg-search-input", value = query, placeholder = "Search all settings…", onInput = { query = it.value })
                    if (searching) button(className = "cfg-search-clear", title = "Clear the search", onClick = { query = "" }) { +"✕" }
                }
                button(className = "cfg-close sbo-close", title = "Close", onClick = {}) { +"×" }
            }
            div(className = "cfg-main") {
                aside(className = "cfg-sidebar") {
                    scroll(className = "cfg-categories guilib-autohide") {
                        SettingsReviewFixture.categories.forEach { name ->
                            button(className = classNames("cfg-category", "active" to (!searching && name == categoryName)), onClick = { if (name == categoryName) query = "" }) {
                                span(className = "cfg-category-name") { +name }
                            }
                        }
                    }
                }
                scroll(className = "cfg-body guilib-autohide", ref = body) {
                    when {
                        searching -> {
                            div(className = "cfg-results-info") { +(hits.size.toString() + " settings found") }
                            div(className = "cfg-result-group") { h2(className = "cfg-result-category") { +categoryName }; hits.forEach { row(it, true) } }
                        }
                        mode == "pickers" -> {
                            h1(className = "cfg-page-title") { +"Shared picker fixtures" }
                            data.forEach { row(it) }
                        }
                        else -> {
                            h1(className = "cfg-page-title") { +categoryName }
                            if (categoryDescription.isNotBlank()) description(categoryDescription, "cfg-page-description")
                            data.groupBy { it.section }.forEach { (section, group) ->
                                if (section.isNotBlank()) {
                                div(className = classNames("cfg-sub", "collapsible" to (section !in off), "collapsed" to (section in collapsed || section in off), "off" to (section in off)),
                                    id = "row-group-" + section.replace(" ", "-")) {
                                    if (sectionSwitches) settingsSectionHeader(section, section !in off,
                                        onCollapse = { if (section !in off) collapsed = if (section in collapsed) collapsed - section else collapsed + section },
                                        onEnabled = { off = if (it) off - section else off + section })
                                    else div(className = "cfg-sub-head") {
                                        h2(className = "cfg-sub-title", onClick = { if (section !in off) collapsed = if (section in collapsed) collapsed - section else collapsed + section }) {
                                            span(className = "cfg-chevron"); +section
                                        }
                                        button(className = "cfg-sub-toggle", onClick = { off = if (section in off) off - section else off + section }) { +if (section in off) "OFF" else "ON" }
                                    }
                                    val explanation = categoryGroups.first { it.name == section }.description
                                    if (explanation.isNotBlank()) description(explanation, "cfg-sub-description")
                                }
                                }
                                if (section !in off && section !in collapsed) group.forEach { row(it) }
                            }
                        }
                    }
                }
            }
            editing?.let { id ->
                val entry = data.first { it.id == id }
                OfflineStringsEditor(StringsReviewProps(entry, lines.getValue(id), lightContrast && theme == "light",
                    { lines = lines + (id to it.split("\n")) }, { editing = null }))
            }
        }
    }
    init {
        root.render(VComponent(app, Unit, null))
        root.document.body.classList.add("sbo-theme-" + theme)
        settle()
        section?.let { scrollTo("group-" + it.replace(" ", "-")); settle() }
    }
    override fun settle(): List<PaintCommand> {
        var frame = emptyList<PaintCommand>()
        repeat(5) { now += 200; frame = root.frame(width, height) }
        return frame
    }
    fun element(selector: String): Element = requireNotNull(root.document.body.querySelector(selector)) { selector }
    fun all(selector: String) = root.document.body.querySelectorAll(selector)
    fun scrollTo(id: String, settleFrame: Boolean = true) {
        val container = root.document.body.querySelector(".cfg-body") ?: return
        val row = root.document.body.querySelector("#row-" + id) ?: return
        container.scrollTop += row.getBoundingClientRect().y - container.getBoundingClientRect().y - 4f
        if (settleFrame) settle()
    }
    fun click(element: Element) {
        val r = element.getBoundingClientRect()
        root.input.mouseDown(r.x + r.width / 2, r.y + r.height / 2, 0)
        root.input.mouseUp(r.x + r.width / 2, r.y + r.height / 2, 0)
        settle()
    }
    fun key(key: String) { root.input.keyDown(key, 0); root.input.keyUp(key, 0); settle() }
    fun open(selector: String) { scrollTo(selector.substringAfter("#row-").substringBefore(' ')); click(element(selector)) }
}

private data class StringsReviewProps(val entry: DianaReviewData.Entry, val lines: List<String>, val light: Boolean,
    val change: (String) -> Unit, val close: () -> Unit)

// Match production's child component so its Escape handler belongs to the editor lifecycle.
private val OfflineStringsEditor = component<StringsReviewProps>("Offline strings editor") { p ->
    useEscapeBack { p.close() }
    div(className = "cfg-editor") {
        div(className = "cfg-editor-header") {
            span(className = "cfg-editor-title") { +p.entry.name }
            button(className = "cfg-editor-done sbo-button", onClick = { p.close() }) { +"Done" }
        }
        settingsDescription(p.entry.description, "cfg-row-description", p.light)
        textarea(className = "cfg-editor-text", value = p.lines.joinToString("\n"), rows = 12, autoFocus = true,
            onInput = { p.change(it.value) })
    }
}
