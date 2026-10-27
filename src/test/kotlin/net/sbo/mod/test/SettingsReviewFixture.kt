package net.sbo.mod.test

import com.google.gson.JsonParser
import net.sbo.guilib.core.UiRoot
import net.sbo.guilib.core.css.Origin
import net.sbo.guilib.core.css.Stylesheet
import net.sbo.guilib.core.dom.Element
import net.sbo.guilib.core.dom.VComponent
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.*
import net.sbo.guilib.core.paint.PaintCommand
import java.nio.file.Files
import net.sbo.mod.config.settingsSectionHeader

/** Source-derived General data with isolated UI state. No DataManager, config saves or game hooks. */
internal class SettingsReviewFixture(
    css: String,
    override val width: Float = 640f,
    override val height: Float = 480f,
    theme: String = "sbo-dark",
    val mode: String = "general",
    val flatTemplate: Boolean = true,
) : ReviewFrame {
    data class Entry(val id: String, val name: String, val description: String, val kind: String,
        val section: String, val defaultOn: Boolean = false, val depends: String? = null)

    companion object {
        private val project = ReviewResources.project
        val currentCss: String get() = Files.readString(project.resolve("src/main/resources/assets/sbo/ui/config/config.css"))
        val baselineCss: String get() = Files.readString(project.resolve("tools/ui-review/baseline/config.css"))
        private val source = Files.readString(project.resolve("src/main/kotlin/net/sbo/mod/settings/categories/General.kt"))
        private val headings = Regex("""this.title = "([^"]+)"""").findAll(source).map { it.range.first to it.groupValues[1] }.toList()
        private fun section(at: Int) = headings.lastOrNull { it.first < at }?.second ?: "General"
        private fun literal(body: String, field: String): String {
            val value = Regex("this\\." + field + """ = Literal\("((?:\\.|[^"])*)"\)""").find(body)?.groupValues?.get(1) ?: return ""
            return JsonParser.parseString("\"" + value + "\"").asString
        }
        val entries: List<Entry> = buildList {
            Regex("""(?s)var (\w+) by ([^{]+)\{(.*?)\n    }""").findAll(source).forEach { match ->
                val declaration = match.groupValues[2]
                val body = match.groupValues[3]
                add(match.range.first to Entry(match.groupValues[1], literal(body, "name"), literal(body, "description"),
                    when { declaration.startsWith("boolean") -> "boolean"; declaration.startsWith("strings") -> "strings"; else -> "multi" },
                    section(match.range.first), declaration.startsWith("boolean(true)"),
                    Regex("""depends\(::(\w+)\)""").find(body)?.groupValues?.get(1)))
            }
            Regex("""(?s)button \{\s*title = "([^"]+)"\s*text = "([^"]+)"\s*description = "([^"]+)"""")
                .findAll(source).forEach { match -> add(match.range.first to Entry("move", match.groupValues[1], match.groupValues[3], "button", section(match.range.first))) }
        }.sortedBy { it.first }.map { it.second }
        val categories = listOf("General", "Diana", "Medal", "Party Commands", "Customization", "Themes", "QOL", "Debug")
        private val controls = listOf(
            Entry("integer", "Numeric input fixture", "Synthetic common-control example, not an added General setting.", "number", "Control fixtures"),
            Entry("slider", "Slider fixture", "Synthetic common-control example with a visible percentage value.", "slider", "Control fixtures"),
            Entry("text", "Long text fixture", "Synthetic input and a long explanation to check wrapping without squeezing other rows or moving the control column.", "text", "Control fixtures"),
        )
        private fun sheet(path: String, origin: Origin = Origin.AUTHOR) = ReviewResources.resource(path).bufferedReader().use {
            Stylesheet.parse(it.readText(), path, origin)
        }
    }

    override val fonts = ReviewFonts()
    private var now = 0L
    val root = UiRoot(fonts, listOf(sheet("/assets/guilib/css/ua.css", Origin.USER_AGENT),
        sheet("/assets/sbo/ui/common.css"), Stylesheet.parse(css, "config.css"), sheet("/assets/sbo/ui/themes/themes.css")), clock = { now })
    var queryValue = ""
        private set
    var numberValue = 12
        private set
    var sliderValue = 50
        private set
    private val app = component("Offline Settings General") {
        var query by useState(when (mode) { "search" -> "overlay"; "empty" -> "no-such-setting"; else -> "" })
        queryValue = query
        var collapsed by useState(if (mode == "collapsed") setOf("Overlays") else emptySet())
        var off by useState(if (mode == "off") setOf("Waypoints") else emptySet())
        var values by useState(entries.associate { it.id to (it.defaultOn && !(mode == "locked" && it.id == "hubChunkCache")) })
        var text by useState("Hub")
        var picks by useState(emptyList<String>())
        var number by useState(12)
        var volume by useState(50)
        numberValue = number
        sliderValue = volume
        var target by useState<String?>(null)
        val body = useElementRef()
        val searching = query.isNotBlank()
        useEscapeBack(searching) { query = "" }
        useEffect(searching) { body.current?.scrollTop = 0f }
        useEffect(target) {
            val id = target ?: return@useEffect
            setTimeout(60) {
                val container = body.current
                val row = container?.querySelector("#row-" + id)
                if (row != null) container.scrollTop += row.getBoundingClientRect().y - container.getBoundingClientRect().y - 24f
            }
            setTimeout(2000) { target = null }
        }
        fun jump(entry: Entry) {
            query = ""
            collapsed = collapsed - entry.section
            target = if (entry.section in off) "group-" + entry.section.replace(" ", "-") else entry.id
        }
        val hits = entries.filter { entry ->
            query.lowercase().split(' ').filter { it.isNotBlank() }.all {
                it in (entry.name + " " + entry.description + " " + entry.id + " General " + entry.section).lowercase()
            }
        }
        fun effectiveOn(id: String): Boolean {
            val entry = entries.first { it.id == id }
            return values[id] == true && entry.section !in off && (entry.depends?.let(::effectiveOn) != false)
        }
        fun NodeBuilder.row(entry: Entry, result: Boolean = false) {
            val locked = entry.depends?.let { !effectiveOn(it) } == true
            div(className = classNames("cfg-row", "locked" to locked, "highlighted" to (target == entry.id)), id = "row-" + entry.id) {
                div(className = "cfg-row-text") {
                    div(className = "cfg-row-name") { +entry.name }
                    if (result) span(className = "cfg-row-place", title = "Show it in its category", onClick = { jump(entry) }) {
                        +("General › " + entry.section + if (entry.section in off) " · off ↗" else " ↗")
                    }
                    div(className = "cfg-row-description") { div(className = "cfg-line") { +entry.description } }
                    if (locked) div(className = "cfg-row-needs") {
                        +"Needs: "
                        val required = entries.first { it.id == entry.depends }
                        span(className = "cfg-needs-link missing", title = "Show " + required.name, onClick = { jump(required) }) { +(required.name + " ON") }
                    }
                }
                div(className = "cfg-row-control") {
                    when (entry.kind) {
                        "boolean" -> switch(checked = values[entry.id] == true, disabled = locked, onChange = { values = values + (entry.id to it.checked) })
                        "button" -> button(className = "cfg-button sbo-button", onClick = {}) { +entry.name }
                        "multi" -> multiSelect(values = picks, className = "cfg-select", placeholder = "None", onChange = { picks = it }) {
                            listOf("NORMAL", "INQ", "MANTICORE", "KING", "SPHINX").forEach { option(it, it) }
                        }
                        "strings" -> div(className = "cfg-strings") {
                            textarea(className = "cfg-textarea", value = text, rows = 2, onInput = { text = it.value })
                            button(className = "cfg-strings-expand sbo-button", title = "Edit in a bigger field", onClick = {}) { +"⤢" }
                        }
                        "number" -> numberInput(value = number, min = 0, max = 999, className = "cfg-number", onChange = { number = it })
                        "slider" -> slider(value = volume, min = 0, max = 100, showValue = true, format = { "$it%" }, className = "cfg-slider", onChange = { volume = it })
                        "text" -> input(value = text, className = "cfg-text", onInput = { text = it.value })
                    }
                }
            }
        }
        div(className = "cfg-window") {
            header(className = "cfg-header") {
                button(className = "cfg-back", title = "Back", onClick = {}) { +"‹" }
                span(className = "cfg-title") { +"SBO 0.7.2 for MC 26.2" }
                div(className = "cfg-search") {
                    input(className = "cfg-search-input", value = query, placeholder = "Search all settings…", autoFocus = false, onInput = { query = it.value })
                    if (searching) button(className = "cfg-search-clear", title = "Clear the search", onClick = { query = "" }) { +"✕" }
                }
                button(className = "cfg-close sbo-close", title = "Close", onClick = {}) { +"×" }
            }
            div(className = "cfg-main") {
                aside(className = "cfg-sidebar") {
                    scroll(className = "cfg-categories guilib-autohide") {
                        categories.forEach { name ->
                            button(className = classNames("cfg-category", "active" to (!searching && name == "General"), "no-hits" to (searching && name != "General")),
                                onClick = { if (name == "General") query = "" }) {
                                span(className = "cfg-category-name") { +name }
                                if (searching && name == "General" && hits.isNotEmpty()) span(className = "cfg-count") { +hits.size.toString() }
                            }
                        }
                    }
                }
                scroll(className = "cfg-body guilib-autohide", ref = body) {
                    when {
                        searching && hits.isEmpty() -> div(className = "cfg-empty") { +("No settings match \"" + query.trim() + "\".") }
                        searching -> {
                            div(className = "cfg-results-info") { +(hits.size.toString() + " settings found") }
                            div(className = "cfg-result-group") {
                                h2(className = "cfg-result-category") { +"General" }
                                hits.forEach { row(it, true) }
                            }
                        }
                        mode == "controls" -> {
                            h1(className = "cfg-page-title") { +"Common control fixtures" }
                            controls.forEach { row(it) }
                        }
                        else -> {
                            h1(className = "cfg-page-title") { +"General" }
                            entries.groupBy { it.section }.forEach { (section, group) ->
                                div(className = classNames("cfg-sub", "collapsible" to (section !in off), "collapsed" to (section in collapsed || section in off), "off" to (section in off)),
                                    id = "row-group-" + section.replace(" ", "-")) {
                                    if (flatTemplate) settingsSectionHeader(section, section !in off,
                                        onCollapse = { if (section !in off) collapsed = if (section in collapsed) collapsed - section else collapsed + section },
                                        onEnabled = { off = if (it) off - section else off + section })
                                    else div(className = "cfg-sub-head") {
                                        fun toggleCollapse() { if (section !in off) collapsed = if (section in collapsed) collapsed - section else collapsed + section }
                                        h2(className = "cfg-sub-title", title = if (flatTemplate) "Collapse or expand $section" else null,
                                            tabIndex = if (flatTemplate && section !in off) 0 else null,
                                            onKeyDown = if (flatTemplate) { event ->
                                                if (event.key == "Enter" || event.key == " ") { event.preventDefault(); toggleCollapse() }
                                            } else null,
                                            onClick = { toggleCollapse() }) {
                                            span(className = "cfg-chevron"); +section
                                        }
                                        button(className = "cfg-sub-toggle", onClick = { off = if (section in off) off - section else off + section }) { +if (section in off) "OFF" else "ON" }
                                    }
                                }
                                if (section !in off && section !in collapsed) group.forEach { row(it) }
                            }
                        }
                    }
                }
            }
        }
    }
    init {
        root.render(VComponent(app, Unit, null))
        root.document.body.classList.add("sbo-theme-" + theme)
        if (flatTemplate) root.document.body.classList.add("cfg-template")
        settle()
    }
    override fun settle(): List<PaintCommand> {
        var frame = emptyList<PaintCommand>()
        repeat(5) { now += 200; frame = root.frame(width, height) }
        return frame
    }
    fun element(selector: String): Element = requireNotNull(root.document.body.querySelector(selector)) { selector }
    fun all(selector: String) = root.document.body.querySelectorAll(selector)
    fun click(element: Element) {
        val r = element.getBoundingClientRect()
        root.input.mouseDown(r.x + r.width / 2, r.y + r.height / 2, 0)
        root.input.mouseUp(r.x + r.width / 2, r.y + r.height / 2, 0)
        settle()
    }
    fun search(text: String) {
        root.document.body.querySelector(".cfg-search-clear")?.let { click(it) }
        element(".cfg-search-input").focus()
        root.input.charTyped(text)
        settle()
    }
    fun key(key: String) { root.input.keyDown(key, 0); root.input.keyUp(key, 0); settle() }
}
