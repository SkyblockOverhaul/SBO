package net.sbo.mod.diana.guide

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.a
import net.sbo.guilib.core.dsl.b
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.footer
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.tabs
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.guide.attributes.AttributeMenuReader
import net.sbo.mod.diana.guide.attributes.AttributeShopping
import net.sbo.mod.diana.guide.attributes.attributesTab
import net.sbo.mod.diana.guide.tabs.loadoutTab
import net.sbo.mod.diana.guide.tabs.magicFindTab
import net.sbo.mod.diana.guide.tabs.petRulesTab
import net.sbo.mod.diana.guide.tabs.techniqueTab
import net.sbo.mod.diana.guide.tabs.trackingTab
import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.gui.StatView
import net.sbo.mod.profileviewer.DisplayItemStacks
import net.sbo.mod.profileviewer.PlayerItems
import net.sbo.mod.profileviewer.ProfileViewerApi
import net.sbo.mod.profileviewer.ViewedProfile
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.events.Register

/**
 * The Diana guide window (`/sboguide`). It shows the gear of the player next to the inhouse Diana Guide.
 * Pet rules, tracking and the written technique chapter have their own tabs.
 * The advice comes from [GuideData] and the items of the player from [GearScan].
 */
object GuideGui {
    private val STYLES = listOf("sbo:ui/dianaguide/dianaguide.css", SboLook.STYLE)

    private val TABS = listOf(
        "loadout" to "Loadout",
        "mf" to "Magic Find",
        "attributes" to "Attributes",
        "pets" to "Pet Rules",
        "tracking" to "Tracking",
        "technique" to "Technique",
    )

    /** The tab the next open starts on. */
    private var startTab = "loadout"

    /** Opens the window on [tab]. Must run on the client thread. */
    fun open(tab: String = "loadout") {
        startTab = tab
        GuiLib.open(App, STYLES, title = "SBO Diana Guide")
    }

    fun register() {
        Register.command("sboguide") { mc.schedule { open() } }
        AttributeMenuReader.register()
    }

    private val App = component("DianaGuide") {
        val guide = GuideData.guide
        var chapter by useState(startTab)
        var profile by useState<PlayerItems?>(null)
        var attributeLevels by useState<Map<String, Int>?>(null)
        var loading by useState(true)
        var error by useState<String?>(null)
        var loadedAt by useState<Long?>(null)
        var reload by useState(0)
        var stacksBuilt by useState(0)
        val now = useState(System.currentTimeMillis())
        useSboScale()
        useSboTheme("dianaguide")
        useInterval(30_000) { now.set(System.currentTimeMillis()) }

        useEffect(reload) {
            loading = true
            error = null
            val name = Player.getName() ?: Player.accountName()
            ProfileViewerApi.player(name, refresh = false, fromCache = reload == 0, onError = { e ->
                error = ProblemText.error(e)
                loading = false
            }) { loaded ->
                profile = loaded.items
                attributeLevels = readAttributeLevels(loaded)
                AttributeShopping.profileLevels = attributeLevels
                loadedAt = System.currentTimeMillis()
                loading = false
            }
        }
        useEffect(profile) {
            val items = profile ?: return@useEffect
            DisplayItemStacks.prepare(items.all(), items.stored()) { stacksBuilt++ }
        }

        val scanned = useMemo(profile, stacksBuilt, reload) { GearScan.scan(guide.slots, profile) }

        div(className = "dg-window") {
            header(className = "dg-header") {
                div(className = "dg-head-row") {
                    span(className = "dg-title") { +"Diana Guide" }
                    div(className = "dg-spacer")
                    span(className = "dg-read") { +readText(loading, loadedAt, now.value) }
                    button(
                        className = if (loading) "dg-icon-button loading" else "dg-icon-button",
                        disabled = loading,
                        title = "Read your gear and SBO profile again",
                        onClick = { reload++ }
                    ) { img(src = "${StatView.ICONS}/refresh.svg", className = "dg-icon") }
                    button(className = "dg-close", title = "Close", onClick = { GuiLib.close() }) { +"x" }
                }
                tabs(value = chapter, onChange = { chapter = it }, className = "dg-tabs") {
                    TABS.forEach { (value, label) -> tab(value, label) }
                }
            }

            scroll(className = "dg-main guilib-autohide") {
                if (!GearScan.onSkyblock()) banner("You are not on SkyBlock. Armor and inventory come from your SBO profile and can be a few minutes old.")
                error?.let { message ->
                    div(className = "dg-banner bad") {
                        span(className = "dg-banner-text") { +"Could not load your SBO profile: $message Equipment and pets show as unknown." }
                        button(disabled = loading, onClick = { reload++ }) { +"Retry" }
                    }
                }
                when (chapter) {
                    "loadout" -> loadoutTab(guide, scanned)
                    "mf" -> magicFindTab(guide, scanned, now.value)
                    "attributes" -> attributesTab(guide, attributeLevels, now.value)
                    "pets" -> petRulesTab(guide, now.value)
                    "tracking" -> trackingTab(guide)
                    else -> techniqueTab(guide)
                }
            }

            footer(className = "dg-footer") {
                span(className = "dg-footer-text") {
                    +"Based on the "
                    b { +guide.credit.guide }
                    +" by "
                    b { +guide.credit.author }
                    +", with support from "
                    b { +guide.credit.support }
                }
                link("Open guide", guide.credit.url)
            }
        }
    }

    private fun NodeBuilder.banner(text: String) {
        div(className = "dg-banner") { span(className = "dg-banner-text") { +text } }
    }

    /** The `dianaAttributes` breakdown of the Diana stats. Null when the backend did not send it. */
    private fun readAttributeLevels(loaded: ViewedProfile): Map<String, Int>? {
        val levels = loaded.stats["diana"]?.get("")?.get("dianaAttributes") as? JsonObject ?: return null
        return levels.mapNotNull { (key, value) -> (value as? JsonPrimitive)?.intOrNull?.let { key to it } }.toMap()
    }

    private fun readText(loading: Boolean, loadedAt: Long?, now: Long): String = when {
        loading -> "Reading..."
        loadedAt == null -> "Profile not read"
        else -> "Read ${ago(now - loadedAt)}"
    }
}

/** How long ago something was read like "just now" or "5 min ago". */
internal fun ago(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60} h ago"
    }
}

/** A link that opens in the browser. The `a` of GuiLib does not navigate by itself. */
internal fun NodeBuilder.link(label: String, url: String) {
    a(className = "dg-link", title = url, onClick = { SBOKotlin.openInBrowser(url) }) { +"$label ↗" }
}
