package net.sbo.mod.partyfinder.gui

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import net.sbo.guilib.core.dom.Element
import net.sbo.guilib.core.dom.Rect
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.b
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.useEscapeBack
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.utils.data.DataManager
import kotlin.math.floor

internal data class OnboardingStep(
    val title: String,
    val text: String,
    val selector: String? = null,
    val page: String = "parties",
    val section: String? = null,
    // Content from the server (rules, online count) can show up late
    val waits: Boolean = false
)

internal val ONBOARDING_STEPS = listOf(
    OnboardingStep(
        "Party Types",
        "Every kind of party SBO knows. The number is how many parties are listed right now. Click a type to see its parties.",
        ".pf-sidebar"
    ),
    OnboardingStep(
        "Favorites",
        "The star pins a party type to Favorites at the top of this list. Drag favorites to change their order.",
        ".pf-side-scroll .pf-star"
    ),
    OnboardingStep(
        "Subcategories",
        "Pick one or All for every party of this type. The star next to them pins what you picked to Favorites.",
        ".pf-subs-row"
    ),
    OnboardingStep(
        "Quick Filters",
        "\"Can I join\" hides parties whose requirements you don't meet, \"Not full\" hides full parties.",
        ".pf-filters"
    ),
    OnboardingStep(
        "More Filters",
        "Filter by party size, free slots, wanted roles, party settings and minimum requirements. Your filters are kept per party type.",
        ".pf-filter-button"
    ),
    OnboardingStep(
        "Search",
        "Finds parties by a player name or a word in the note.",
        ".pf-search"
    ),
    OnboardingStep(
        "Refresh",
        "The list loads new parties by itself (see Settings). This button or F5 loads them right now.",
        ".pf-list-bar .pf-icon-button"
    ),
    OnboardingStep(
        "A Party",
        "This example party only exists during the tour. A party shows the leader, players and party size, and how long ago it was listed. Click the card to see the members, " +
            "click a member for all their stats. Right click to copy the note, hide or report the party.",
        ".pf-card"
    ),
    OnboardingStep(
        "Requirements",
        "What the leader asks for. Green: you meet it, red: you don't. + means at least this much, ~ means estimated. " +
            "Hover a value for details.",
        ".pf-reqs"
    ),
    OnboardingStep(
        "Join",
        "Asks the leader to invite you. If the leader has Auto Invite on and you meet the requirements, you are invited right away. " +
            "A grey button means you miss a requirement, hover it to see which.",
        ".pf-join"
    ),
    OnboardingStep(
        "Players Online",
        "How many players use SBO right now.",
        ".pf-online", waits = true
    ),
    OnboardingStep(
        "Pages",
        "Parties, Create Party, Rules and Settings. Next we look at creating your own party.",
        ".pf-nav"
    ),
    OnboardingStep(
        "Party Size",
        "How many players your party should have at most, you included.",
        ".pf-size-row", page = "create"
    ),
    OnboardingStep(
        "Requirements",
        "Players who don't meet them can't join. Leave a field empty or at Any for no requirement. " +
            "Your own value is on the right, so you see if you meet them yourself.",
        ".pf-fields", page = "create"
    ),
    OnboardingStep(
        "Note",
        "Two short lines everyone sees on your party, for example what you plan to do.",
        ".pf-note-input", page = "create"
    ),
    OnboardingStep(
        "List your Party",
        "You must be alone or the party leader. While it is listed, your party shows at the top next to the pages, " +
            "with a button to remove it. Your last input per party type is kept for next time.",
        ".pf-form-buttons", page = "create"
    ),
    OnboardingStep(
        "Rules",
        "Please read them once. Parties that break them can be reported with a right click on the party.",
        ".pf-rules", page = "rules", waits = true
    ),
    OnboardingStep(
        "Settings",
        "The party finder has its own settings in three sections.",
        ".pf-settings-tabs", page = "settings", section = "general"
    ),
    OnboardingStep(
        "General",
        "Auto Invite invites players who meet your requirements, Auto Requeue lists your party again when someone leaves. " +
            "Also how often the list refreshes, and reloading your own stats.",
        ".pf-form", page = "settings", section = "general"
    ),
    OnboardingStep(
        "Favorites",
        "Sort or remove favorites, open the party finder on your first favorite, or show favorites only once in the list.",
        ".pf-form", page = "settings", section = "favorites"
    ),
    OnboardingStep(
        "Look",
        "Themes (also for color blindness and high contrast, or your own theme files), the font and the window size. Global uses what you picked in the SBO settings for all windows.",
        ".pf-form", page = "settings", section = "look"
    ),
    OnboardingStep(
        "All set",
        "Open the party finder any time with /sbopf. Have fun finding your next party!"
    )
)

internal data class OnboardingProps(
    val onShow: (page: String, section: String?) -> Unit,
    val onDone: () -> Unit,
    val intro: Boolean = true
)

private const val START_SCREEN = -1
private const val GAP = 6f
private const val MARGIN = 4f
private const val PADDING = 2f
private const val TIP_WIDTH = 190f
// The box's real height is only known after it was drawn once
private const val TIP_HEIGHT = 70f
// The page needs a moment after switching, before that the old page's elements would match
private const val SETTLE_MS = 100L
private const val CONTENT_WAIT_MS = 2000L

private data class Spot(val step: Int, val rect: Rect?)

internal val Onboarding = component<OnboardingProps>("Onboarding") { props ->
    var step by useState(if (props.intro) START_SCREEN else 0)
    // Keeps the last hole until the next step's element is found, so the dimming never jumps to the full screen
    var spot by useState(Spot(START_SCREEN, null))
    var tipHeight by useState(TIP_HEIGHT)
    val tipRef = useElementRef()
    val scrolledStep = useRef(-1)
    val stepStart = useRef(System.currentTimeMillis())
    val document = useDocument()

    useEffect {
        // Stored at once, so closing the window during the tour doesn't bring it back
        DataManager.sboData.pfOnboardingSeen = true
        DataManager.sboData.save()
        ONBOARDING_STEPS.getOrNull(step)?.let { props.onShow(it.page, it.section) }
    }

    fun finish() = props.onDone()
    // In the click instead of an effect, so page and step change in the same render
    fun go(next: Int) {
        if (next > ONBOARDING_STEPS.lastIndex) return finish()
        val shown = next.coerceAtLeast(0)
        stepStart.current = System.currentTimeMillis()
        ONBOARDING_STEPS[shown].let { props.onShow(it.page, it.section) }
        step = shown
    }
    useEscapeBack { finish() }

    // Layout changes (window size, loaded parties, scrolling) move the element, so its box is read again and again
    useInterval(50) {
        val current = ONBOARDING_STEPS.getOrNull(step) ?: return@useInterval
        val waited = System.currentTimeMillis() - stepStart.current
        if (waited < SETTLE_MS) return@useInterval
        // Elements hidden by CSS in small windows still match the selector
        val element = current.selector?.let { document.body.querySelector(it) }
            ?.takeIf { it.getBoundingClientRect().let { r -> r.width > 0 && r.height > 0 } }
        if (element == null) {
            if (!current.waits || waited > CONTENT_WAIT_MS) spot = Spot(step, null)
            return@useInterval
        }
        if (scrolledStep.current != step) {
            scrolledStep.current = step
            scrollIntoView(element)
        }
        spot = Spot(step, element.getBoundingClientRect())
        tipRef.current?.getBoundingClientRect()?.height?.takeIf { it > 0 }?.let { tipHeight = it }
    }

    val vw = document.viewportWidth
    val vh = document.viewportHeight
    val hole = spot.rect?.let { highlight(it, vw, vh) }
    // Without a hole the four dims meet in the middle, so they move smoothly into the next hole
    val dimmed = hole ?: Rect(vw / 2, vh / 2, 0f, 0f)

    portal {
        div(className = "pf-tour") {
            dim("top", 0f, 0f, vw, dimmed.y)
            dim("bottom", 0f, dimmed.bottom, vw, vh - dimmed.bottom)
            dim("left", 0f, dimmed.y, dimmed.x, dimmed.height)
            dim("right", dimmed.right, dimmed.y, vw - dimmed.right, dimmed.height)
            if (hole != null) div(className = "pf-tour-ring", style = box(hole.x, hole.y, hole.width, hole.height), key = "ring")

            val current = ONBOARDING_STEPS.getOrNull(step)
            if (current == null) {
                div(className = "pf-tour-start", key = "start") {
                    b(className = "pf-tour-title") { +"Welcome to the Party Finder" }
                    p(className = "pf-tour-text") {
                        +"A short tour shows how to find and join parties, how to list your own, and what the rules and settings are. It takes about two minutes."
                    }
                    div(className = "pf-tour-buttons") {
                        button(onClick = { finish() }) { +"Skip, I know it already" }
                        button(className = "primary", onClick = { go(0) }) { +"Start tour" }
                    }
                }
                return@div
            }
            if (spot.step != step) return@div

            val width = minOf(TIP_WIDTH, vw - 2 * MARGIN)
            div(className = "pf-tour-tip", ref = tipRef, style = tipPosition(hole, width, tipHeight, vw, vh), key = "tip:$step") {
                div(className = "pf-tour-head") {
                    b(className = "pf-tour-title") { +current.title }
                    span(className = "pf-tour-step") { +"${step + 1}/${ONBOARDING_STEPS.size}" }
                }
                p(className = "pf-tour-text") { +current.text }
                div(className = "pf-tour-buttons") {
                    button(className = "pf-small pf-tour-skip", onClick = { finish() }) { +"Skip" }
                    div(className = "pf-spacer")
                    button(className = "pf-small", disabled = step == 0, onClick = { go(step - 1) }) { +"Back" }
                    val last = step == ONBOARDING_STEPS.lastIndex
                    button(className = "pf-small primary", onClick = { go(step + 1) }) {
                        +(if (last) "Finish" else "Next")
                    }
                }
            }
        }
    }
}

// One requirement met and one not, so the requirements step shows both colors
internal fun tourParty(target: PartyTarget, own: MemberView?): PartyView {
    val shown = target.subTargets().first()
    val reqs = shown.reqs.filter { it.type == "min" }.take(2).mapIndexed { i, def ->
        val have = floor((own?.stats?.get(def.stat) as? JsonPrimitive)?.doubleOrNull ?: 0.0).toLong()
        def.stat to (JsonPrimitive(if (i == 0) have else have * 2 + 1) as JsonElement)
    }.toMap()
    val leader = MemberView(TOUR_LEADER, "ExampleLeader")
    return PartyView(
        id = TOUR_LEADER,
        partyType = shown.partyType,
        subType = shown.sub?.id ?: "",
        partySize = shown.maxSize,
        memberCount = 2,
        note = "Example party for the tour",
        createdAt = System.currentTimeMillis() - 3 * 60_000L,
        reqs = reqs,
        members = listOf(leader, MemberView("00000000000000000000000000000002", "ExampleMember"))
    )
}

private const val TOUR_LEADER = "00000000000000000000000000000001"

private fun highlight(r: Rect, vw: Float, vh: Float): Rect {
    val x = (r.x - PADDING).coerceAtLeast(0f)
    val y = (r.y - PADDING).coerceAtLeast(0f)
    return Rect(x, y, (r.right + PADDING).coerceAtMost(vw) - x, (r.bottom + PADDING).coerceAtMost(vh) - y)
}

private fun tipPosition(hole: Rect?, width: Float, height: Float, vw: Float, vh: Float): String {
    fun clampX(x: Float) = x.coerceIn(MARGIN, (vw - width - MARGIN).coerceAtLeast(MARGIN))
    fun clampY(y: Float) = y.coerceIn(MARGIN, (vh - height - MARGIN).coerceAtLeast(MARGIN))
    val (left, top) = when {
        hole == null -> clampX((vw - width) / 2) to clampY((vh - height) / 2)
        vh - hole.bottom >= height + GAP + MARGIN -> clampX(hole.x) to hole.bottom + GAP
        hole.y >= height + GAP + MARGIN -> clampX(hole.x) to hole.y - GAP - height
        vw - hole.right >= width + GAP + MARGIN -> hole.right + GAP to clampY(hole.y)
        hole.x >= width + GAP + MARGIN -> hole.x - GAP - width to clampY(hole.y)
        else -> clampX((vw - width) / 2) to vh - height - MARGIN
    }
    return "left: ${left}px; top: ${top}px; width: ${width}px;"
}

private fun box(x: Float, y: Float, width: Float, height: Float) =
    "left: ${x}px; top: ${y}px; width: ${width.coerceAtLeast(0f)}px; height: ${height.coerceAtLeast(0f)}px;"

private fun NodeBuilder.dim(key: String, x: Float, y: Float, width: Float, height: Float) {
    div(className = "pf-tour-dim", style = box(x, y, width, height), key = key)
}

private fun scrollIntoView(element: Element) {
    var parent = element.parent
    while (parent != null && parent.maxScrollTop <= 0f) parent = parent.parent
    val container = parent ?: return
    val view = container.getBoundingClientRect()
    val r = element.getBoundingClientRect()
    val delta = when {
        r.y < view.y -> r.y - view.y - MARGIN
        r.bottom > view.bottom -> minOf(r.bottom - view.bottom + MARGIN, r.y - view.y - MARGIN)
        else -> return
    }
    container.scrollTop = (container.scrollTop + delta).coerceIn(0f, container.maxScrollTop)
}
