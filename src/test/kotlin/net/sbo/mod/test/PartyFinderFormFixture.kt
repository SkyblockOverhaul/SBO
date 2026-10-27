package net.sbo.mod.test

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.*
import java.nio.file.Files

/** Presentation-only specimens for the dynamic Create/Edit and filter control families.
 * Hierarchy, classes, strings and widget types follow CreatePage/PartiesPage. Synthetic
 * backend definitions deliberately combine long labels and requirement kinds; no live app,
 * saved drafts, OwnStats, PartyFinderManager or network callbacks are initialized.
 */
internal class PartyFinderFormFixture(
    css: String = PartyFinderReviewFixture.currentCss,
    width: Float = 800f,
    height: Float = 480f,
    theme: String = "sbo-dark",
    private val filters: Boolean = false,
    private val edit: Boolean = false,
    private val unavailable: Boolean = false,
    private val empty: Boolean = false,
) {
    companion object {
        val beforeCss get() = Files.readString(ReviewResources.project.resolve("tools/ui-review/baseline/partyfinder-forms-before.css"))
        val out get() = ReviewResources.project.resolve("build/ui-review/partyfinder-forms")
        private val managerSource = Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/partyfinder/PartyFinderManager.kt"))
        val noteMaxLength = Regex("const val NOTE_MAX_LENGTH = (\\d+)").find(managerSource)!!.groupValues[1].toInt()
        val noteMaxLines = Regex("const val NOTE_MAX_LINES = (\\d+)").find(managerSource)!!.groupValues[1].toInt()
    }
    var numeric: Double? = if (empty) null else 300.0; private set
    var selectedItems: List<String> = if (empty) emptyList() else listOf("term", "armor"); private set
    var matchMode = "any"; private set
    var noteValue = ""; private set
    var submitted = 0; private set
    var modalOpen = filters; private set
    var sortValue = "default"; private set
    private val form = component("Offline PF Create/Edit controls") {
        var size by useState(5)
        var minimum by useState(numeric)
        var required by useState(false)
        var rarity by useState(if (empty) "" else "legendary")
        var items by useState(selectedItems)
        var mode by useState("any")
        var tier by useState("fiery")
        var option by useState("splus")
        var labelled by useState(if (empty) "" else "1000")
        var ownRole by useState("tank")
        var roles by useState(listOf("berserk"))
        var note by useState(if (edit) "Fast runs, please know your role." else "")
        numeric = minimum; selectedItems = items; matchMode = mode; noteValue = note
        scroll(className = "pf-form pf-create-form guilib-autohide") {
            if (unavailable) div(className = "pf-banner") { +"These parties can only be created while the event is running." }
            h3(className = "pf-section", id = "section-size") { +"Party size" }
            div(className = "pf-size-row") { numberInput(value = size, onChange = { size = it }, min = 2, max = 5); span(className = "pf-muted") { +(if (size == 5) "full party" else "$size players") } }
            p(className = "pf-hint") { +"Master Mode: Floor VII parties can have up to 5 players." }
            h3(className = "pf-section", id = "section-requirements") { +"Requirements" }
            p(className = "pf-hint") { +"Players who don't meet these can't join. Leave a field empty or at \"Any\" for no requirement. Your own value is shown on the right." }
            div(className = "pf-fields") {
                field("number", "Skyblock Level", if (empty) null else "You: 381", "ok") {
                    numberInput(value = minimum, onChange = { minimum = it }, allowEmpty = true, min = 1.0, max = 500.0, decimals = 1)
                }
                field("labelled", "Master Mode Floor VII Completions", if (empty) null else "× You: 824", "bad") {
                    select(value = labelled, onChange = { labelled = it.value }) { option("", "Any"); option("1000", "1,000 or better"); option("2000", "2,000 or better") }
                }
                field("flag", "Wither Blade", if (empty) null else "You: Owned", if (required) "ok" else "") {
                    checkbox(checked = required, onChange = { required = it.checked }, label = "Required")
                }
                field("rarity", "Pet Rarity", if (empty) null else "You: Legendary", "ok") {
                    select(value = rarity, onChange = { rarity = it.value }) {
                        option("", "Any"); option("rare", "Rare or better"); option("legendary", "Legendary or better"); option("mythic", "Mythic")
                    }
                }
                field("items", "Weapons and equipment with a minimum tier", if (empty) null else "You: Terminator, Hyperion", "ok", true) {
                    div(className = "pf-any-of") {
                        multiSelect(values = items, onChange = { items = it }, placeholder = "Any") {
                            option("term", "Terminator"); option("blade", "Wither Blade"); option("armor", "Crimson Armor")
                        }
                        if (items.size > 1) segmented(value = mode, onChange = { mode = it }, className = "pf-match") {
                            option("any", "Any of these"); option("all", "All of these")
                        }
                        if ("armor" in items) div(className = "pf-tier-row") {
                            span { +"Crimson Armor, at least " }
                            select(value = tier, onChange = { tier = it.value }) { option("", "Basic"); option("fiery", "Fiery"); option("infernal", "Infernal") }
                        }
                    }
                }
            }
            h3(className = "pf-section", id = "section-options") { +"Party settings" }
            div(className = "pf-fields") {
                field("option", "Run goal") {
                    div(className = "pf-wide-only") { segmented(value = option, onChange = { option = it }) { option("any", "Any"); option("splus", "S+"); option("practice", "Practice") } }
                    div(className = "pf-narrow-only") { select(value = option, onChange = { option = it.value }) { option("any", "Any"); option("splus", "S+"); option("practice", "Practice") } }
                }
            }
            h3(className = "pf-section", id = "section-roles") { +"Wanted roles" }
            p(className = "pf-hint") { +"Optional. If you pick roles, everyone who wants to join has to choose one of them." }
            div(className = "pf-fields") { field("own-role", "Your role") {
                select(value = ownRole, onChange = { ownRole = it.value }) { option("", "No role"); option("tank", "Tank"); option("berserk", "Berserk"); option("healer", "Healer") }
            } }
            p(className = "pf-hint") { +"Optional. Your role takes its own place in the party. The roles below are the places for the players you are looking for." }
            chips(values = roles, onChange = { roles = it }) { option("tank", "Tank", disabled = ownRole == "tank"); option("berserk", "Berserk"); option("healer", "Healer") }
            h3(className = "pf-section", id = "section-note") { +"Note" }
            p(className = "pf-hint") { +"Write here what the fields above don't cover, like the sea creature you hunt or how many runs you plan." }
            textarea(value = note, onChange = { note = it.value }, placeholder = "Write a short note for your party", rows = noteMaxLines, maxLength = noteMaxLength, maxLines = noteMaxLines, className = "pf-note-input")
            p(className = "pf-hint") { +"${note.length}/$noteMaxLength. Letters, numbers, spaces and , . ! ? - _ + / : ' ( ) only, other characters are removed." }
            div(className = "pf-form-buttons") {
                button(className = "sbo-button", id = "form-reset", onClick = { minimum = null; items = emptyList(); roles = emptyList(); note = "" }) { +"Reset" }
                tooltip("You must be alone or the party leader. Everyone already in your party has to meet the requirements too.", className = "pf-tip") {
                    button(className = "sbo-button primary", id = "form-submit", disabled = unavailable, onClick = { submitted++ }) { +(if (edit) "Update party" else "Create party") }
                }
            }
            p(className = "pf-hint pf-rules-hint") { +"By listing a party you agree to the "; span(className = "pf-link") { +"rules" }; +"." }
        }
    }
    private fun NodeBuilder.field(id: String, label: String, own: String? = null, status: String = "", wide: Boolean = false, control: NodeBuilder.() -> Unit) {
        div(className = classNames("pf-field", "wide" to wide), id = "field-$id") {
            tooltip("Synthetic stat explanation", className = "pf-tip") { span(className = "pf-stat-label pf-field-label") { +label; img("sbo:ui/partyfinder/info.svg", className = "pf-icon pf-info") } }
            div(className = "pf-field-input") { control() }
            if (own != null) span(className = "pf-own $status", title = own) { +own }
        }
    }
    private val filter = component("Offline PF filters") {
        var open by useState(true)
        var sizes by useState(if (empty) emptyList<String>() else listOf("5"))
        var free by useState<Int?>(if (empty) null else 1)
        var goal by useState(if (empty) "" else "splus")
        var minimum by useState<Double?>(if (empty) null else 48.0)
        var roles by useState(if (empty) emptyList<String>() else listOf("tank"))
        var languages by useState(if (empty) emptyList<String>() else listOf("en"))
        var required by useState(false)
        var sort by useState("default")
        modalOpen = open; sortValue = sort; numeric = minimum
        modal(open = open, onClose = { open = false }, className = "pf-dialog pf-filter-dialog") {
            h3 { +"Filters" }
            p(className = "pf-hint") { +"Only parties that match everything you set here are shown. Your own party is always shown." }
            div(className = "pf-filter-list") {
                filterRow("sizes", "Party size", "Only parties of these sizes. Pick none to see all sizes.") {
                    chips(values = sizes, onChange = { sizes = it }) { option("2", "Duo"); option("3", "Trio"); option("4", "4 players"); option("5", "Full party") }
                }
                filterRow("free", "Free spots", "Only parties with at least this many free spots.") { numberInput(value = free, onChange = { free = it }, allowEmpty = true, min = 1, max = 4) }
                filterRow("goal", "Run goal", "Only parties with one of these. Parties set to \"Any\" are always shown.") {
                    select(value = goal, onChange = { goal = it.value }) { option("", "Show all"); option("splus", "S+"); option("practice", "Practice") }
                }
                filterRow("languages", "Language", "Pick none to show all languages.") {
                    multiSelect(values = languages, onChange = { languages = it }, placeholder = "Show all") {
                        option("en", "English"); option("de", "German"); option("fr", "French")
                    }
                }
                div(className = "pf-filter-section") { div { +"Requirements" }; div(className = "pf-hint") { +"Only parties that ask for at least this much. Parties that don't ask for it are hidden." } }
                filterRow("minimum", "Master Mode Floor VII Completions", null) { numberInput(value = minimum, onChange = { minimum = it }, allowEmpty = true, min = 1.0, max = 50000.0, decimals = 1) }
                filterRow("flag", "Wither Blade", null) { checkbox(checked = required, onChange = { required = it.checked }, label = "Party requires it") }
                filterRow("roles", "My roles", "Only parties that look for one of these roles. Parties that don't ask for roles take everyone.") {
                    chips(values = roles, onChange = { roles = it }) { option("tank", "Tank"); option("berserk", "Berserk"); option("healer", "Healer") }
                }
                filterRow("sort", "Sort by", "Parties that wait longest come first, also when two parties are equal.") {
                    select(value = sort, onChange = { sort = it.value }) { option("default", "Default"); option("free", "Most free spots"); option("full", "Almost full first") }
                }
            }
            div(className = "pf-dialog-buttons") {
                button(className = "sbo-button", id = "filter-reset", onClick = { sizes = emptyList(); free = null; goal = ""; languages = emptyList(); minimum = null; roles = emptyList(); sort = "default" }) { +"Reset filters" }
                button(className = "sbo-button primary", id = "filter-done", onClick = { open = false }) { +"Done" }
            }
        }
    }
    private fun NodeBuilder.filterRow(id: String, label: String, hint: String?, control: NodeBuilder.() -> Unit) {
        div(className = "pf-filter-row", id = "filter-$id") {
            div(className = "pf-filter-label") { div { +label }; if (hint != null) div(className = "pf-hint") { +hint } }
            div(className = "pf-filter-input") { control() }
        }
    }
    val frame = PartyFinderReviewFixture(css, width, height, theme, queued = edit, initialPage = if (filters) "parties" else "create",
        pageContent = if (filters) null else ({ form(Unit) }), overlayContent = if (filters) ({ filter(Unit) }) else null)
    fun element(selector: String) = frame.element(selector)
    fun all(selector: String) = frame.all(selector)
    fun scrollTo(selector: String) {
        val container = element(if (filters) ".pf-filter-dialog" else ".pf-create-form")
        container.scrollTop += element(selector).getBoundingClientRect().y - container.getBoundingClientRect().y - 5f
        frame.settle()
    }
}
