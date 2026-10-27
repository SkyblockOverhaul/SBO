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

/** Offline shell/list/inspector fixture matching production DOM classes and hierarchy.
 * Synthetic parties and stats; no DataManager, API, clipboard, game, or queue actions.
 * Head textures and SVG images are intentionally not reproduced by the rasterizer.
 */
internal class PartyFinderReviewFixture(
    css: String = currentCss,
    override val width: Float = 800f,
    override val height: Float = 480f,
    theme: String = "sbo-dark",
    private val empty: Boolean = false,
    private val queued: Boolean = false,
    inspector: Boolean = false,
    initialPage: String = "parties",
    private val pageContent: (NodeBuilder.() -> Unit)? = null,
    private val overlayContent: (NodeBuilder.() -> Unit)? = null,
    private val showTargetToolbar: Boolean = true,
) : ReviewFrame {
    companion object {
        val currentCss get() = Files.readString(ReviewResources.project.resolve("src/main/resources/assets/sbo/ui/partyfinder/partyfinder.css"))
        val beforeCss get() = Files.readString(ReviewResources.project.resolve("tools/ui-review/baseline/partyfinder-before.css"))
        val out get() = ReviewResources.project.resolve("build/ui-review/partyfinder-list")
        private fun sheet(path: String, origin: Origin = Origin.AUTHOR) = ReviewResources.resource(path).bufferedReader().use {
            Stylesheet.parse(it.readText(), path, origin)
        }
    }
    override val fonts = ReviewFonts()
    private var now = 0L
    val root = UiRoot(fonts, listOf(sheet("/assets/guilib/css/ua.css", Origin.USER_AGENT),
        sheet("/assets/sbo/ui/common.css"), Stylesheet.parse(css, "partyfinder.css"),
        sheet("/assets/sbo/ui/themes/themes.css")), clock = { now })
    var joins = 0; private set
    var removed = 0; private set
    var refreshes = 0; private set
    var currentSearch = ""; private set
    var inspected = inspector; private set
    var expanded = inspector; private set
    private val app = component("Offline Party Finder review") {
        var panel by useState(inspector)
        var open by useState(inspector)
        var search by useState("")
        var page by useState(initialPage)
        var floor by useState("VII")
        var filters by useState(emptyList<String>())
        inspected = panel; expanded = open; currentSearch = search
        div(className = classNames("pf-stage", "with-panel" to panel)) {
            div(className = "pf-window") {
                header(className = "pf-header") {
                    span(className = "pf-title") { +"Party Finder" }
                    tabs(value = page, onChange = { page = it }, className = "pf-nav") {
                        tab("parties", "Parties"); tab("create", if (queued) "Edit Party" else "Create Party")
                        tab("rules", "Rules"); tab("settings", "Settings")
                    }
                    div(className = "pf-spacer")
                    div(className = "pf-online", title = "Players online in Party Finder") {
                        span(className = "pf-online-dot"); span { +"1234" }
                    }
                    if (queued) div(className = "pf-queue") {
                        span(className = "pf-queue-text", onClick = { floor = "VII" }) { +"Master Mode Floor VII 4/5" }
                        button(className = "sbo-button pf-small danger", onClick = { removed++ }) { +"Remove" }
                    }
                    button(className = "pf-close sbo-close", title = "Close", onClick = {}) { +"×" }
                }
                div(className = "pf-body") {
                    aside(className = "pf-sidebar") {
                        scroll(className = "pf-side-scroll guilib-autohide") {
                            div(className = "pf-side-title") { +"Favorites" }
                            sideItem("Master Mode VII", "7", true)
                            div(className = "pf-side-title") { +"Party Types" }
                            listOf("Catacombs" to "18", "Master Mode" to "7", "Kuudra" to "12", "Diana" to "4", "Slayers" to "6", "Fishing" to "0").forEach {
                                sideItem(it.first, it.second, it.first == "Master Mode")
                            }
                        }
                        div(className = "pf-side-item pf-support") { span(className = "pf-side-label") { +"Support SBO" } }
                    }
                    main(className = classNames("pf-main", "pf-main-list guilib-autohide" to (pageContent == null))) {
                        if (showTargetToolbar) div(className = "pf-toolbar") {
                            h2(className = "pf-heading") { +"Master Mode" }
                            div(className = "pf-subs-row") {
                                tabs(value = floor, onChange = { floor = it }, variant = "pills", className = "pf-subs") {
                                    listOf("All", "I", "II", "III", "IV", "V", "VI", "VII").forEach { tab(it, it) }
                                }
                                span(className = "pf-sub-star") { +"☆" }
                            }
                        }
                        if (pageContent != null) pageContent.invoke(this) else {
                        div(className = "pf-list-bar") {
                            chips(values = filters, onChange = { filters = it }, className = "pf-filters") {
                                option("canJoin", "Can I join"); option("notFull", "Not full")
                            }
                            div(className = "guilib-chip pf-filter-button", title = "More filters and sorting") { +"Filters" }
                            input(value = search, onChange = { search = it.value }, placeholder = "Search", className = "pf-search", title = "Search a player name or a note")
                            span(className = "pf-count") { +(if (empty) "0 parties" else "3 parties") }
                            div(className = "pf-spacer")
                            button(className = "sbo-button pf-icon-button", title = "Refresh the list (F5)", onClick = { refreshes++ }) { +"↻" }
                        }
                        scroll(className = "pf-list guilib-autohide") {
                            if (empty) div(className = "pf-message") {
                                p { +"There are no Master Mode Floor VII parties right now. Create one under \"Create Party\"." }
                            } else {
                                party("Aster", "4/5", "Fast S+ runs, please know your role. No downtime between runs.", true, open, queued, false,
                                    { open = !open }, { joins++ }, { panel = true })
                                party("VeryLongPlayer16", "3/5", "Chill learning runs. Looking for a tank and a healer.", false, false, false, false,
                                    {}, { joins++ }, {})
                                party("Birch", "5/5", "Full party — finishing a run.", null, false, false, true,
                                    {}, {}, {})
                            }
                        }
                        div(className = "pf-legend") {
                            span(className = "pf-legend-ok") { +"Green: meets requirements" }; +" · "
                            span(className = "pf-legend-bad") { +"Red: requirements not met" }
                        }
                        }
                    }
                }
            }
            if (panel) aside(className = "pf-panel") {
                div(className = "pf-panel-head") {
                    div(className = "pf-head")
                    b(className = "pf-panel-name") { +"VeryLongPlayer16" }
                    button(className = "pf-close sbo-close", title = "Close", onClick = { panel = false }) { +"×" }
                }
                div(className = "pf-panel-sub") { +"Leader, Master Mode Floor VII" }
                scroll(className = "pf-panel-stats guilib-autohide") {
                    listOf("Catacombs" to "50", "Skyblock Level" to "381", "Class Level" to "49", "Secrets Found" to "58,321", "Magical Power" to "1,482",
                        "Master Mode Floor VII Completions" to "1,234", "Fastest S+" to "5:42", "Selected Class" to "Tank", "Terminator" to "Owned",
                        "Wither Blade" to "Missing", "Gear Score" to "Estimated 9,842", "Dungeon Equipment" to "Ancient Cloak, Implosion Belt, Gauntlet of Contagion").forEachIndexed { i, (label, value) ->
                        div(className = "pf-panel-row") {
                            tooltip("Synthetic stat label details", className = "pf-tip") {
                                span(className = "pf-stat-label") { +label; img("sbo:ui/partyfinder/info.svg", className = "pf-icon pf-info") }
                            }
                            tooltip("Synthetic stat details", className = "pf-tip") {
                                span(className = classNames("pf-panel-value", "missing" to (value == "Missing"), "estimated" to value.startsWith("Estimated"))) { +value }
                            }
                            if (i < 4) div(className = "pf-panel-need") { +"Party needs: ${listOf("48+", "300+", "45+", "30,000+")[i]}" }
                        }
                    }
                }
            }
        }
        overlayContent?.invoke(this)
    }
    private fun NodeBuilder.sideItem(label: String, count: String, active: Boolean) {
        div(className = classNames("pf-side-item", "active" to active)) {
            span(className = "pf-side-label") { +label }; span(className = "pf-side-count") { +count }; span(className = "pf-star") { +"☆" }
        }
    }
    private fun NodeBuilder.party(name: String, size: String, note: String, meets: Boolean?, expanded: Boolean, mine: Boolean, full: Boolean,
        onToggle: () -> Unit, onJoin: () -> Unit, onInspect: () -> Unit) {
        div(className = "pf-card-anchor") {
            div(className = classNames("pf-card", "mine" to mine, "full" to full, "expanded" to expanded), onClick = { onToggle() }) {
                div(className = "pf-card-head") {
                    div(className = "pf-head"); b(className = "pf-leader") { +name }
                    if (mine) span(className = "pf-tag mine") { +"Your party" }
                    span(className = "pf-tag") { +size }; div(className = "pf-spacer"); span(className = "pf-age") { +"2m ago" }
                    if (!mine) button(className = "sbo-button pf-join primary", disabled = full || meets == false,
                        onClick = { it.stopPropagation(); onJoin() }) { +"Join" }
                }
                div(className = "pf-card-tags") { span(className = "pf-tag option") { +"Run: S+" }; span(className = "pf-tag option") { +"Language: English" } }
                div(className = "pf-note") { +note }
                div(className = "pf-reqs") {
                    listOf("Catacombs" to "48+", "Skyblock Level" to "300+", "Secrets Found" to "30,000+").forEach { (label, value) ->
                        span(className = classNames("pf-req", "ok" to (!mine && meets == true), "bad" to (!mine && meets == false))) {
                            span(className = "pf-req-label") { +"$label: " }; +value
                        }
                    }
                }
                div(className = "pf-roles") {
                    +"Looking for: "; span(className = "pf-role-slot taken") { +"✓ Berserk" }; span(className = "pf-role-slot") { +"Tank" }
                }
                collapse(open = expanded) {
                    div(className = "pf-members", onClick = { it.stopPropagation() }) {
                        listOf(name, "VeryLongPlayer16", "Cedar", "Dusk").forEachIndexed { i, member ->
                            div(className = "pf-member-anchor") {
                                div(className = classNames("pf-member", "inspected" to (i == 1)), onClick = { onInspect() }) {
                                    span(className = "pf-member-name") { +member }; span(className = "pf-member-role") { +" · Tank" }
                                    div(className = "pf-member-stats") {
                                        listOf("Catacombs: 50", "Secrets: 58,321", "Class: 49").forEach { stat ->
                                            tooltip("Synthetic member stat details", className = "pf-tip") { span(className = "pf-stat") { +stat } }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    init { root.render(VComponent(app, Unit, null)); root.document.body.classList.add("sbo-theme-$theme"); settle() }
    override fun settle(): List<PaintCommand> {
        var commands = emptyList<PaintCommand>()
        repeat(5) { now += 250; commands = root.frame(width, height) }
        return commands
    }
    fun element(selector: String): Element = requireNotNull(root.document.body.querySelector(selector)) { selector }
    fun all(selector: String) = root.document.body.querySelectorAll(selector)
    fun click(e: Element) { val b = e.getBoundingClientRect(); root.input.mouseDown(b.x + b.width / 2, b.y + b.height / 2, 0); root.input.mouseUp(b.x + b.width / 2, b.y + b.height / 2, 0); settle() }
    fun hover(e: Element) { val b = e.getBoundingClientRect(); root.input.mouseMove(b.x + b.width / 2, b.y + b.height / 2); settle() }
    fun key(value: String) { root.input.keyDown(value, 0); root.input.keyUp(value, 0); settle() }
}
