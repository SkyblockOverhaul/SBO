package net.sbo.mod.test

import com.google.gson.JsonParser
import net.sbo.guilib.core.dom.Rect
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.*
import net.sbo.mod.partyfinder.api.ReportReason
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.gui.ONBOARDING_STEPS
import net.sbo.mod.partyfinder.gui.highlight
import net.sbo.mod.partyfinder.gui.tipPosition
import java.nio.file.Files

/** Offline specimens; source strings/control hierarchy, synthetic rules/players/favorites.
 * Never renders the live Settings/Rules/Onboarding components or performs their effects.
 * Tour copy and pure positioning functions are shared; route waits/timers remain native QA.
 */
internal class PartyFinderRestFixture(
    val view: String,
    css: String = PartyFinderReviewFixture.currentCss,
    val width: Float = 800f,
    val height: Float = 480f,
    theme: String = "sbo-dark",
    stepIndex: Int = 7,
    roleChoice: String? = null,
) {
    companion object {
        val out get() = ReviewResources.project.resolve("build/ui-review/partyfinder-rest")
        val beforeCss get() = Files.readString(ReviewResources.project.resolve("tools/ui-review/baseline/partyfinder-rest-before.css"))
        private val source = Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/partyfinder/gui/SettingsPage.kt"))
        private val literal = "\"(?:\\\\.|[^\"\\\\])*\""
        val settings = Regex("settingRow\\(\\s*($literal),\\s*($literal)").findAll(source).associate {
            JsonParser.parseString(it.groupValues[1]).asString to JsonParser.parseString(it.groupValues[2]).asString
        }
    }
    var actionCount = 0; private set
    var selectedRole: String? = null; private set
    var reportBlocked = true; private set
    var tourStep = stepIndex; private set
    var dismissed = false; private set
    private var measureTip: (Float) -> Unit = {}
    private val app = component("Offline PF remaining pages") {
        var on by useState(true)
        var themeChoice by useState("global")
        var fontChoice by useState("inter")
        var scaleChoice by useState("global")
        var refresh by useState("30")
        var favoriteOrder by useState(listOf("Master Mode: Floor VII", "Diana: Burrows and Inquisitors", "Kuudra: Infernal"))
        val section = view.substringAfter("settings-").substringBefore('-')
        when {
            view.startsWith("rules") -> when (view) {
                "rules-loading" -> div(className = "pf-message") { p { +"Loading the rules..." } }
                "rules-error" -> div(className = "pf-message") { p { +"Could not load the rules. Connection unavailable." }; button(className = "sbo-button", onClick = { actionCount++ }) { +"Try again" } }
                else -> scroll(className = "pf-form pf-rules guilib-autohide") {
                    h3(className = "pf-section") { +"Party Finder Rules" }
                    p(className = "pf-hint") { +"Keep parties honest, useful and welcoming. These rules apply to party notes and the way you use Party Finder." }
                    listOf("Be respectful" to "Do not harass other players or post offensive notes. Treat party members with respect, including when a run does not go as planned.",
                        "Describe your party honestly" to "Use requirements and notes that match the run you intend to play. Do not create misleading parties or invite players for a different activity.",
                        "No advertising" to "Do not advertise unrelated services, websites or communities in party notes.",
                        "No selling carries" to "Party Finder is for finding players. Do not use listings to sell carries or other services.",
                        "Keep listings relevant" to "Remove a party when you no longer need players. Pick the party type, roles and requirements that describe what you are doing.",
                        "Report real problems" to "Reports are reviewed by moderators. Give enough detail for them to understand what happened.").forEachIndexed { i, (title, text) ->
                        div(className = "pf-rule") { b(className = "pf-rule-title") { +"${i + 1}. $title." }; span { +" $text" } }
                    }
                    p(className = "pf-hint pf-rules-outro") { +"Moderators may remove listings that break the rules." }
                }
            }
            view.startsWith("settings") -> {
                div(className = "pf-toolbar") { tabs(value = section, onChange = {}, variant = "pills", className = "pf-subs pf-settings-tabs") {
                    tab("general", "General"); tab("favorites", "Favorites"); tab("look", "Look")
                } }
                scroll(className = "pf-form pf-settings-form guilib-autohide") {
                    when (section) {
                        "look" -> {
                            setting("Theme") { select(value = themeChoice, onChange = { themeChoice = it.value }, className = "pf-theme-select") {
                                option("global", "Global (SBO Dark)")
                                SettingsSweepData.read("Themes").entries.first { it.id == "theme" }.let { entry -> entry.options.forEach { option(it, entry.optionLabels.getValue(it)) } }
                            } }
                            p(className = "pf-hint pf-theme-info") { +SettingsSweepData.read("Themes").entries.first { it.id == "theme" }.optionDescriptions.getValue("sbo-dark") }
                            setting("Your own themes") { button(className = "sbo-button", onClick = { actionCount++ }) { +"Open theme folder" } }
                            setting("Recombobulated items") { switch(checked = on, onChange = { on = it.checked }) }
                            setting("Font") { select(value = fontChoice, onChange = { fontChoice = it.value }) { option("inter", "Inter"); option("minecraft", "Minecraft"); option("nunito", "Nunito"); option("jetbrains-mono", "JetBrains Mono") } }
                            setting("Size") { select(value = scaleChoice, onChange = { scaleChoice = it.value }, className = "pf-scale-select") {
                                option("global", "Global (Auto)"); option("auto", "Auto"); listOf("1", "1.5", "2", "2.5", "3", "4").forEach { option(it, it) }
                            } }
                        }
                        "favorites" -> {
                            setting("Start with favorites") { switch(checked = on, onChange = { on = it.checked }) }
                            setting("Hide favorites in Party Types") { switch(checked = on, onChange = { on = it.checked }) }
                            if (view.endsWith("empty")) p(className = "pf-hint") { +"No favorites yet. Click the star next to a party type to add one." }
                            else {
                                p(className = "pf-hint") { +"Drag to change the order. The first one opens when \"Start with favorites\" is on." }
                                sortableList(favoriteOrder, key = { it }, onReorder = { favoriteOrder = it }, className = "pf-fav-settings") { key, _ ->
                                    div(className = "pf-fav-row") { img("sbo:ui/partyfinder/grip.svg", className = "pf-icon pf-grip"); span(className = "pf-fav-name") { +key }; button(className = "sbo-button pf-small", onClick = { favoriteOrder = favoriteOrder - key }) { +"Remove" } }
                                }
                            }
                        }
                        else -> {
                            h3(className = "pf-section") { +"Your party" }
                            setting("Auto invite") { switch(checked = on, onChange = { on = it.checked }) }
                            setting("Auto requeue") { switch(checked = on, onChange = { on = it.checked }) }
                            h3(className = "pf-section") { +"Blocked players" }
                            p(className = "pf-hint") { +"Players on this list can't join your parties. Their join requests are declined automatically." }
                            if (view.endsWith("empty")) p(className = "pf-hint") { +"No blocked players yet. Right-click a player in the party list to block them, or use /block add <name>." }
                            else div(className = "pf-fav-row") { div(className = "pf-head"); span(className = "pf-fav-name") { +"VeryLongPlayer16" }; button(className = "sbo-button pf-small", onClick = { actionCount++ }) { +"Unblock" } }
                            h3(className = "pf-section") { +"Party list" }
                            setting("Refresh automatically") { segmented(value = refresh, onChange = { refresh = it }) { option("0", "Off"); option("30", "30 s"); option("60", "60 s") } }
                            h3(className = "pf-section", id = "rest-stats") { +"Your stats" }
                            p(className = "pf-hint") { +"Reload your stats after you got new gear or leveled up. ${ProblemText.OWN_RELOAD_HINT}" }
                            button(className = "sbo-button", disabled = view.endsWith("reloading"), onClick = { actionCount++ }) { +"Reload my stats" }
                            h3(className = "pf-section") { +"Help" }
                            setting("Tour") { button(className = "sbo-button", onClick = { actionCount++ }) { +"Start tour" } }
                        }
                    }
                }
            }
            view.startsWith("menu") -> contextMenu(menu = {
                header("VeryLongPlayer16")
                when (view) {
                    "menu-own" -> { item("Copy note", disabled = true) {}; item("Copy leader name") {}; item("Edit party") {}; separator(); item("Remove from queue", danger = true) { actionCount++ } }
                    "menu-member" -> { item("Show all stats") {}; item("Copy name") {}; item("Change role…") {}; separator(); item("Make party leader") {}; item("Make moderator") {}; item("Kick from party", danger = true) {}; item("Kick and block", danger = true) { actionCount++ } }
                    else -> { item("Copy note") {}; item("Copy leader name") {}; item("Join party", disabled = true) {}; separator(); item("Hide this party") {}; item("Block player") {}; item("Report party…", danger = true) { actionCount++ } }
                }
            }, className = "rest-menu-anchor") { button(className = "sbo-button", id = "rest-menu-target", style = "position: absolute; right:12px; bottom:12px;") { +"VeryLongPlayer16" } }
        }
    }
    private fun NodeBuilder.setting(title: String, control: NodeBuilder.() -> Unit) {
        div(className = "pf-setting") { div(className = "pf-setting-text") { div(className = "pf-setting-title") { +title }; div(className = "pf-hint") { +settings.getValue(title) } }; control() }
    }
    private val overlay = component("Offline PF dialogs and tour") {
        val document = useDocument()
        var open by useState(true)
        var role by useState<String?>(if (view == "change-role") "tank" else roleChoice)
        var reason by useState(if (view.startsWith("report")) "other" else "")
        var details by useState(if (view == "report-sending") "A detailed synthetic report." else "Bad")
        var sending by useState(view == "report-sending")
        var step by useState(stepIndex)
        var intro by useState(view == "tour-welcome")
        var tipHeight by useState(70f)
        measureTip = { tipHeight = it }
        dismissed = !open; selectedRole = role; tourStep = step
        if (view.startsWith("tour")) {
            useEscapeBack { open = false }
            if (open) portal {
                div(className = "pf-tour") {
                    val target = ONBOARDING_STEPS.getOrNull(step)?.selector?.let { document.body.querySelector(it) }
                        ?.getBoundingClientRect()?.takeIf { it.width > 0 && it.height > 0 }
                    val hole = if (intro || view == "tour-missing") null else target?.let { highlight(it, width, height) }
                    val d = hole ?: Rect(width / 2, height / 2, 0f, 0f)
                    listOf(Rect(0f,0f,width,d.y), Rect(0f,d.bottom,width,height-d.bottom), Rect(0f,d.y,d.x,d.height), Rect(d.right,d.y,width-d.right,d.height)).forEach {
                        div(className = "pf-tour-dim", style = "left:${it.x}px;top:${it.y}px;width:${it.width}px;height:${it.height}px;")
                    }
                    if (intro) div(className = "pf-tour-start") {
                        b(className = "pf-tour-title") { +"Welcome to the Party Finder" }
                        p(className = "pf-tour-text") { +"A short tour shows how to find and join parties, how to list your own, and what the rules and settings are. It takes about two minutes." }
                        div(className = "pf-tour-buttons") { button(className = "sbo-button", onClick = { open = false }) { +"Skip, I know it already" }; button(className = "sbo-button primary", onClick = { intro = false; step = 0 }) { +"Start tour" } }
                    } else {
                        val current = ONBOARDING_STEPS[step]
                        if (hole != null) div(className = "pf-tour-ring", style = "left:${hole.x}px;top:${hole.y}px;width:${hole.width}px;height:${hole.height}px;")
                        div(className = "pf-tour-tip", style = tipPosition(hole, minOf(190f, width - 8), tipHeight, width, height)) {
                            div(className = "pf-tour-head") { b(className = "pf-tour-title") { +current.title }; span(className = "pf-tour-step") { +"${step+1}/${ONBOARDING_STEPS.size}" } }
                            p(className = "pf-tour-text") { +current.text }
                            div(className = "pf-tour-buttons") {
                                button(className = "sbo-button pf-small pf-tour-skip", onClick = { open = false }) { +"Skip" }; div(className = "pf-spacer")
                                button(className = "sbo-button pf-small", disabled = step == 0, onClick = { step-- }) { +"Back" }
                                button(className = "sbo-button pf-small primary", onClick = { if (step == ONBOARDING_STEPS.lastIndex) open = false else step++ }) { +(if (step == ONBOARDING_STEPS.lastIndex) "Finish" else "Next") }
                            }
                        }
                    }
                }
            }
        } else if (view in listOf("join", "change-role") || view.startsWith("report")) {
            val report = view.startsWith("report")
            val tooShort = report && reason == "other" && details.trim().length < ReportReason.MIN_OTHER_DETAILS
            reportBlocked = reason.isEmpty() || tooShort || sending
            modal(open = open, onClose = { open = false }, className = "pf-dialog" + if (report) " pf-report-dialog" else "") {
                h3 { +(if (report) "Report VeryLongPlayer16's party" else if (view == "join") "Join VeryLongPlayer16" else "Change the role of VeryLongPlayer16") }
                if (report) p(className = "pf-hint") {
                    +"Moderators on the SBO Discord see your report together with your name and the party. Please only report parties that really break the "
                    span(className = "pf-link", onClick = { actionCount++ }) { +"rules" }; +"."
                } else if (view == "join") p(className = "pf-hint") { +"This party looks for these roles. Pick the one you want to play." }
                if (report) {
                    radioGroup(value = reason, onChange = { reason = it }, vertical = true) { ReportReason.entries.forEach { option(it.id, it.label) } }
                    textarea(value = details, onChange = { details = it.value }, placeholder = "Describe the problem in a few words", rows = 3, maxLength = ReportReason.MAX_DETAILS, className = "pf-report-input")
                    if (tooShort) p(className = "pf-hint") { +"Please write at least ${ReportReason.MIN_OTHER_DETAILS} characters, so the moderators know what happened." }
                } else radioGroup(value = role, onChange = { role = it }, vertical = true) {
                    option("tank", if (view == "join") "Tank (taken)" else "Tank", disabled = view == "join"); option("berserk", "Berserk"); option("healer", "Healer")
                    if (view == "change-role") option("", "No role")
                }
                div(className = "pf-dialog-buttons") {
                    button(className = "sbo-button", onClick = { open = false }) { +"Cancel" }
                    button(className = "sbo-button primary", id = "rest-submit", disabled = if (report) reportBlocked else role == null || (view == "change-role" && role == "tank"), onClick = { actionCount++; if (report) sending = true else open = false }) { +(if (report) "Send report" else if (view == "join") "Send join request" else "Save") }
                }
            }
        }
    }
    val frame = PartyFinderReviewFixture(css, width, height, theme, initialPage = if (view.startsWith("rules")) "rules" else if (view.startsWith("settings")) "settings" else "parties",
        pageContent = if (view.startsWith("rules") || view.startsWith("settings") || view.startsWith("menu")) ({ app(Unit) }) else null,
        overlayContent = if (view.startsWith("tour") || view in listOf("join", "change-role") || view.startsWith("report")) ({ overlay(Unit) }) else null,
        showTargetToolbar = !(view.startsWith("rules") || view.startsWith("settings")))
    init { if (view.startsWith("tour") && view != "tour-welcome") repeat(2) { frame.all(".pf-tour-tip").firstOrNull()?.let { measureTip(it.getBoundingClientRect().height); frame.settle() } } }
    fun openMenu() { val b = frame.element("#rest-menu-target").getBoundingClientRect(); frame.root.input.mouseDown(b.right-1, b.bottom-1, 2); frame.root.input.mouseUp(b.right-1, b.bottom-1, 2); frame.settle() }
    fun scrollTo(selector: String) { val c = frame.element(".pf-form"); c.scrollTop += frame.element(selector).getBoundingClientRect().y - c.getBoundingClientRect().y - 4; frame.settle() }
}
