package net.sbo.mod.test

import net.sbo.guilib.core.UiRoot
import net.sbo.guilib.core.css.Origin
import net.sbo.guilib.core.css.PseudoState
import net.sbo.guilib.core.css.Stylesheet
import net.sbo.guilib.core.dom.Element
import net.sbo.guilib.core.dom.VComponent
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.ComponentScope
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.chips
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.input
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.tabs
import net.sbo.guilib.core.layout.FontMetrics
import net.sbo.guilib.core.layout.TextMeasurer
import net.sbo.guilib.core.layout.TextStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Font
import java.awt.font.FontRenderContext

/** Exercises GuiLib's actual cascade, layout and hit testing without starting Minecraft.
 * Header fixtures retain the production hierarchy and labels; data/network hooks are omitted.
 * AWT uses the bundled Inter faces, but these checks do not replace in-game raster/scale QA.
 */
class UiPresentationTest {
    private val themes = listOf("sbo-dark", "light", "high-contrast", "minecraft")

    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { path }
    private fun sheet(path: String, origin: Origin = Origin.AUTHOR) =
        resource(path).bufferedReader().use { Stylesheet.parse(it.readText(), path, origin) }

    private val measurer = object : TextMeasurer {
        private val context = run { requireHeadlessReview(); FontRenderContext(null, true, true) }
        private val regular = resource("/assets/guilib/fonts/inter-regular.ttf").use { Font.createFont(Font.TRUETYPE_FONT, it) }
        private val bold = resource("/assets/guilib/fonts/inter-bold.ttf").use { Font.createFont(Font.TRUETYPE_FONT, it) }
        private fun font(style: TextStyle) = (if (style.bold) bold else regular).deriveFont(style.fontSize)
        override fun width(text: String, style: TextStyle) = font(style).getStringBounds(text, context).width.toFloat()
        override fun metrics(style: TextStyle): FontMetrics {
            val metrics = font(style).getLineMetrics("Ag", context)
            return FontMetrics(metrics.ascent, metrics.descent, metrics.height)
        }
    }

    private inner class Fixture(
        val width: Float = 640f,
        screen: String? = null,
        theme: String = "sbo-dark",
        content: ComponentScope.() -> Unit,
    ) {
        private var now = 0L
        val root = UiRoot(measurer, buildList {
            add(sheet("/assets/guilib/css/ua.css", Origin.USER_AGENT))
            add(sheet("/assets/sbo/ui/common.css"))
            if (screen != null) add(sheet("/assets/sbo/ui/$screen/$screen.css"))
            add(sheet("/assets/sbo/ui/themes/themes.css"))
        }, clock = { now })
        init {
            root.render(VComponent(component("Presentation fixture") { content() }, Unit, null))
            root.document.body.classList.add("sbo-theme-$theme")
            settle()
        }
        fun settle() { repeat(3) { now += 250; root.frame(width, 480f) } }
        fun element(selector: String) = requireNotNull(root.document.body.querySelector(selector)) { selector }
        fun all(selector: String) = root.document.body.querySelectorAll(selector)
        fun hover(element: Element) {
            val r = element.getBoundingClientRect()
            root.input.mouseMove(r.x + r.width / 2, r.y + r.height / 2)
            settle()
        }
        fun click(element: Element) {
            val r = element.getBoundingClientRect()
            root.input.mouseDown(r.x + r.width / 2, r.y + r.height / 2, 0)
            root.input.mouseUp(r.x + r.width / 2, r.y + r.height / 2, 0)
            settle()
        }
        fun key(value: String) { root.input.keyDown(value, 0); settle() }
    }

    @Test
    fun closeRemainsAnIconWithAStableClickableTargetInEveryTheme() {
        val screens = mapOf("config" to "cfg", "achievements" to "ach", "sounds" to "snd", "events" to "ev", "partyfinder" to "pf", "badge" to "bd", "cloud" to "cs")
        for ((screen, prefix) in screens) for (theme in themes) {
            var clicks = 0
            val f = Fixture(screen = screen, theme = theme) {
                div(className = "$prefix-window") {
                    header(className = "$prefix-header") {
                        span(className = "$prefix-title") { +"Title" }
                        button(className = "$prefix-close sbo-close", onClick = { clicks++ }) { +"×" }
                    }
                }
            }
            val close = f.element(".sbo-close")
            val initial = close.getBoundingClientRect()
            assertTrue(initial.width >= 18f && initial.height >= 18f, "$screen/$theme close target")
            assertEquals(0, close.style.backgroundColor ushr 24, "$screen/$theme default background")
            f.hover(close)
            assertEquals(0xFFEF8274.toInt(), close.style.color, "$screen/$theme hover icon")
            assertEquals(0, close.style.backgroundColor ushr 24, "$screen/$theme hover background")
            assertEquals(0f, close.style.borderTopWidth, "$screen/$theme border")
            assertTrue(close.style.boxShadow.isEmpty(), "$screen/$theme bevel must not cover close")
            assertNull(close.style.textShadow, "$screen/$theme close shadow")
            assertEquals(initial, close.getBoundingClientRect(), "$screen/$theme hover moved hitbox")
            f.click(close)
            assertEquals(1, clicks, "$screen/$theme click should reach close")
        }
    }

    @Test
    fun themesStillReachWindowHeadingsAndNativeFields() {
        val expected = mapOf(
            "sbo-dark" to (0xFF1E1F22.toInt() to 0xFFFFFFFF.toInt()),
            "light" to (0xFFF2F3F5.toInt() to 0xFF000000.toInt()),
            "high-contrast" to (0xFF000000.toInt() to 0xFFFFFFFF.toInt()),
            "minecraft" to (0xFF2A2420.toInt() to 0xFFFFFFFF.toInt()),
        )
        for ((theme, colors) in expected) {
            val f = Fixture(screen = "config", theme = theme) {
                div(className = "cfg-window") {
                    header(className = "cfg-header") { span(className = "cfg-title") { +"Settings" } }
                    input(id = "field", value = "Editable", onChange = {})
                    button(className = "sbo-button", id = "action") { +"Open" }
                }
            }
            assertEquals(colors.first, f.element(".cfg-window").style.backgroundColor, theme)
            assertEquals(colors.second, f.element(".cfg-title").style.color, theme)
            assertTrue(f.element("#field").style.borderTopWidth > 0f, "$theme field boundary")
            if (theme == "minecraft") {
                assertEquals(listOf("minecraft"), f.element(".cfg-title").style.fontFamily)
                assertTrue(f.element("#action").style.boxShadow.isNotEmpty(), "Minecraft regular buttons retain their bevel")
            }
        }
    }

    @Test
    fun partyFinderTitleTabsStatusAndCloseAlignWithoutOverflow() {
        for (width in listOf(320f, 480f, 640f)) for (queued in listOf(false, true)) {
            val f = Fixture(width, "partyfinder") {
                div(className = "pf-stage") {
                    div(className = "pf-window") {
                        header(className = "pf-header") {
                            span(className = "pf-title") { +"Party Finder" }
                            tabs(value = "parties", onChange = {}, className = "pf-nav") {
                                tab("parties", "Parties"); tab("create", if (queued) "Edit Party" else "Create Party"); tab("rules", "Rules"); tab("settings", "Settings")
                            }
                            div(className = "pf-spacer")
                            div(className = "pf-online") { span(className = "pf-online-dot"); span { +"1234" } }
                            if (queued) div(className = "pf-queue") {
                                span(className = "pf-queue-text") { +"Master Mode Floor VII 5/5" }
                                button(className = "pf-small danger") { +"Remove" }
                            }
                            button(className = "pf-close sbo-close") { +"×" }
                        }
                    }
                }
            }
            val title = f.element(".pf-title")
            centerAligned(title, f.element(".pf-online"), "$width status")
            centerAligned(title, f.element(".pf-close"), "$width close")
            val tabs = f.all(".guilib-tab")
            tabs.drop(1).forEach { centerAligned(tabs.first(), it, "$width tab labels") }
            if (width > 520f) centerAligned(title, tabs.first(), "$width tabs and title")
            else assertTrue(tabs.first().getBoundingClientRect().y >= title.getBoundingClientRect().bottom, "$width tabs wrap below the title")
            contained(f.element(".pf-header"), listOf(title, f.element(".pf-online"), f.element(".pf-close")) + tabs)
            if (queued) contained(f.element(".pf-header"), listOf(f.element(".pf-queue"), f.element(".pf-small")))
        }
    }

    @Test
    fun achievementAndEventHeadersKeepActionsVisibleOnNarrowWindows() {
        for (width in listOf(320f, 480f, 640f)) {
            val achievements = Fixture(width, "achievements") {
                div(className = "ach-window") {
                    header(className = "ach-header") {
                        div(className = "ach-heading") {
                            span(className = "ach-title") { +"Achievements" }
                            div(className = "ach-progress") {
                                span(className = "ach-progress-text") { +"125/256 · 48.8%" }
                                div(className = "ach-bar") { div(className = "ach-bar-fill", style = "width: 48.8%") }
                            }
                        }
                        select(value = "auto", className = "ach-scale-select") { option("auto", "Size: Auto") }
                        button(className = "ach-close sbo-close") { +"×" }
                    }
                }
            }
            contained(achievements.element(".ach-header"), listOf(".ach-title", ".ach-progress-text", ".ach-scale-select", ".ach-close").map(achievements::element))
            centerAligned(achievements.element(".ach-scale-select"), achievements.element(".ach-close"), "$width achievement actions")
            val events = Fixture(width, "events") {
                div(className = "ev-window") {
                    header(className = "ev-header") {
                        div(className = "ev-heading") {
                            button(className = "sbo-button ev-back") { +"‹ Back" }
                            span(className = "ev-title") { +"Current Event · Year 467" }
                            button(className = "sbo-button ev-nav") { +"‹" }
                            button(className = "sbo-button ev-nav") { +"›" }
                        }
                        div(className = "ev-spacer")
                        div(className = "ev-header-actions") {
                            button(className = "sbo-button ev-copy") { +"Copy" }
                            select(value = "auto", className = "ev-scale-select") { option("auto", "Size: Auto") }
                            button(className = "sbo-close") { +"×" }
                        }
                    }
                }
            }
            contained(events.element(".ev-header"), listOf(".ev-title", ".ev-back", ".ev-copy", ".ev-scale-select", ".sbo-close").map(events::element) + events.all(".ev-nav"))
            centerAligned(events.element(".ev-copy"), events.element(".sbo-close"), "$width event actions")
        }
    }

    @Test
    fun sharedActionsDoNotPaintOverNativeSelectionOrKeyboardFocus() {
        val f = Fixture {
            var selected by useState("a")
            div {
                tabs(value = selected, onChange = { selected = it }) { tab("a", "First"); tab("b", "Second") }
                segmented(value = selected, onChange = { selected = it }) { option("a", "First"); option("b", "Second") }
                chips(values = listOf("a")) { option("a", "Selected"); option("b", "Available") }
                input(id = "field", value = "Editable", onChange = {})
                button(className = "sbo-button", id = "action") { +"Open" }
            }
        }
        val segment = f.element(".guilib-segment.selected")
        assertEquals(0, segment.style.backgroundColor ushr 24, "Native segment must reveal the selection indicator")
        assertTrue(f.element(".guilib-segment-indicator").style.backgroundColor ushr 24 > 0)
        assertNotEquals(f.element(".guilib-chip.selected").style.backgroundColor, f.element(".guilib-chip:not(.selected)").style.backgroundColor)
        val second = f.all(".guilib-tab")[1]
        f.click(second)
        assertTrue(f.all(".guilib-tab")[1].classList.contains("active"))
        assertTrue(f.all(".guilib-segment")[1].classList.contains("selected"))
        f.key("ArrowLeft")
        assertTrue(f.all(".guilib-tab")[0].classList.contains("active"))
        assertTrue(f.all(".guilib-tab")[0].hasState(PseudoState.FOCUS_VISIBLE))
        val field = f.element("#field")
        val initialBorder = field.style.borderTopColor
        f.click(field)
        assertNotEquals(initialBorder, field.style.borderTopColor, "Native focused input boundary remains visible")
        f.key("Tab")
        val action = f.element("#action")
        assertTrue(action.hasState(PseudoState.FOCUS_VISIBLE))
        assertTrue(action.style.outlineWidth > 0f, "Flat action has a keyboard focus outline")
        assertFalse(action.style.backgroundColor ushr 24 == 0)
    }

    private fun centerAligned(a: Element, b: Element, label: String) {
        val ar = a.getBoundingClientRect(); val br = b.getBoundingClientRect()
        assertEquals(ar.y + ar.height / 2, br.y + br.height / 2, 0.6f, label)
    }

    private fun contained(parent: Element, elements: List<Element>) {
        val outer = parent.getBoundingClientRect()
        for (element in elements) {
            val r = element.getBoundingClientRect()
            assertTrue(r.width > 0f && r.height > 0f, "${element.className} has a visible target")
            assertTrue(r.x >= outer.x - 0.6f && r.right <= outer.right + 0.6f && r.y >= outer.y - 0.6f && r.bottom <= outer.bottom + 0.6f,
                "${element.className} $r exceeds header $outer")
        }
    }
}
