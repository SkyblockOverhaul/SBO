package net.sbo.mod.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.math.abs

class HubPresentationTest {
    @Test
    fun reviewRuntimeRequiresHeadlessBeforeAwtUse() {
        requireHeadlessReview()
        assertEquals("true", System.getProperty("java.awt.headless"))
    }
    @Test
    fun commandsOpenAndCloseWithKeyboardActivation() {
        val f = HubReviewFixture(HubReviewFixture.currentCss)
        repeat(8) { f.key("Tab") }
        assertTrue(f.all(".hub-tile")[6].isFocused)
        f.key("Enter")
        assertTrue(f.commandsOpen)
        assertTrue(f.element(".hub-close-small").isFocused)
        f.key("Enter")
        assertFalse(f.commandsOpen)
        assertTrue(f.all(".hub-tile")[6].isFocused)
    }

    @Test
    fun approvedHubGeometryAndBottomEdgeHoverStayStable() {
        for ((width, height) in listOf(640f to 360f, 360f to 240f)) {
            val before = HubReviewFixture(HubReviewFixture.baselineCss, width, height)
            val after = HubReviewFixture(HubReviewFixture.currentCss, width, height)
            val oldWindow = before.element(".hub-window").getBoundingClientRect()
            val window = after.element(".hub-window").getBoundingClientRect()
            assertEquals(oldWindow.width, window.width, .001f)
            assertTrue(window.x >= 0 && window.right <= width && window.y >= 0 && window.bottom <= height)
            for (selector in listOf(".hub-title", ".hub-tiles")) {
                val old = before.element(selector).getBoundingClientRect()
                val current = after.element(selector).getBoundingClientRect()
                assertEquals(old.width, current.width, .001f, selector)
                assertEquals(old.height, current.height, .001f, selector)
                assertEquals(old.x - oldWindow.x, current.x - window.x, .1f, selector)
                assertEquals(old.y - oldWindow.y, current.y - window.y, .1f, selector)
            }
            val tiles = after.all(".hub-tile")
            assertEquals(9, tiles.size)
            tiles.forEachIndexed { i, tile ->
                val bounds = tile.getBoundingClientRect()
                val previous = before.all(".hub-tile")[i].getBoundingClientRect()
                assertEquals(previous.width, bounds.width, .001f)
                assertEquals(previous.height, bounds.height, .001f)
                after.hover(tile, bottom = true)
                repeat(4) { after.settle(); assertEquals(bounds, tile.getBoundingClientRect()); assertTrue(tile.contains(after.root.input.hovered)) }
            }
            val footerY = after.all(".hub-card").map { it.getBoundingClientRect().y }
            assertEquals(1, footerY.distinct().size, "Footer groups share one row")
            val footer = after.element(".hub-columns").getBoundingClientRect()
            for (link in after.all(".hub-columns button")) {
                val b = link.getBoundingClientRect()
                assertTrue(b.x >= footer.x - .1 && b.right <= footer.right + .1 && b.bottom <= footer.bottom + .1,
                    "Every upstream credit remains visible: $b in $footer")
            }
        }
    }

    @Test
    fun borderlessActionsShowKeyboardFocusWithoutChangingLayout() {
        val f = HubReviewFixture(HubReviewFixture.currentCss)
        f.key("Tab")
        assertTrue(f.element(".hub-close").isFocused)
        f.key("Tab")
        assertTrue(f.all(".hub-tile").first().isFocused)
        for (selector in listOf(".hub-tile", ".hub-link", ".hub-donate", ".hub-credit", ".hub-close")) {
            val el = f.element(selector)
            val bounds = el.getBoundingClientRect()
            f.root.document.keyboardModality = true
            el.focus()
            f.settle()
            assertTrue(el.style.outlineWidth > 0f, "$selector needs a visible focus indicator")
            assertEquals(bounds, el.getBoundingClientRect())
        }
    }

    @Test
    fun commandsAlignAndScrollToEveryDescriptionThenRestoreFocus() {
        for ((width, height) in listOf(640f to 360f, 360f to 240f)) {
            val f = HubReviewFixture(HubReviewFixture.currentCss, width, height)
            f.openCommands()
            assertTrue(f.commandsOpen)
            assertTrue(f.element(".hub-close-small").isFocused, "Opening Commands focuses Close")
            assertEquals(HubReviewFixture.commands.size, f.all(".hub-command").size)
            val close = f.element(".hub-close-small").getBoundingClientRect()
            assertTrue(close.width >= 18f && close.height >= 18f)
            val title = f.element(".hub-commands-header .hub-card-title").getBoundingClientRect()
            assertTrue(abs(title.x - f.element(".hub-command-name").getBoundingClientRect().x) < 0.1f, "Header and command text share a left edge")
            assertTrue(abs(title.y + title.height / 2 - close.y - close.height / 2) < 0.1f, "Title and close share a center")
            val list = f.element(".hub-commands-list")
            assertTrue(list.maxScrollTop > 0f)
            list.scrollTop = list.maxScrollTop
            f.settle()
            assertTrue(f.all(".hub-command-description").last().getBoundingClientRect().bottom <= list.getBoundingClientRect().bottom + 0.1f)
            // The scroll viewport is also keyboard-focusable; tabbing may visit it as well as Close.
            repeat(4) {
                f.key("Tab")
                assertTrue(f.element(".hub-commands").contains(f.root.document.focusedElement), "Focus remains inside Commands: ${f.root.document.focusedElement?.describe()}")
            }
            repeat(4) {
                f.key("Tab", shift = true)
                assertTrue(f.element(".hub-commands").contains(f.root.document.focusedElement), "Shift+Tab remains inside Commands")
            }
            f.root.document.keyboardModality = true
            f.element(".hub-close-small").focus()
            f.settle()
            assertTrue(f.element(".hub-close-small").style.outlineWidth > 0f)
            f.key("Escape")
            assertFalse(f.commandsOpen)
            assertTrue(f.all(".hub-tile")[6].isFocused, "Closing the modal restores its launcher focus")
        }
    }

    @Test
    fun closeTargetsStayQuietAndClickableAcrossThemes() {
        for (theme in listOf("sbo-dark", "light", "high-contrast", "minecraft")) {
            val f = HubReviewFixture(HubReviewFixture.currentCss, theme = theme)
            f.openCommands()
            val close = f.element(".hub-close-small")
            val bounds = close.getBoundingClientRect()
            f.hover(close, bottom = true)
            assertEquals(bounds, close.getBoundingClientRect(), theme)
            assertEquals(0xFFEF8274.toInt(), close.style.color, theme)
            assertEquals(0, close.style.backgroundColor ushr 24, theme)
            assertTrue(close.style.boxShadow.isEmpty(), theme)
            assertNull(close.style.textShadow, theme)
            f.click(close)
            assertFalse(f.commandsOpen, theme)
        }
    }

    @Test
    fun reviewBaselineAndCurrentGeometry() {
        for ((label, css) in listOf("before" to HubReviewFixture.baselineCss, "after" to HubReviewFixture.currentCss)) {
            val f = HubReviewFixture(css, manageCommandsFocus = label == "after")
            assertEquals(listOf("Settings", "Party Finder", "Events", "Achievements", "Sounds", "Move Overlays", "Commands", "Cloud Sync", "SBO Badge"), HubReviewFixture.tiles.map { it.first })
            assertEquals(32, HubReviewFixture.commands.size)
            println("$label hub=${f.element(".hub-window").getBoundingClientRect()} close=${f.element(".hub-close").getBoundingClientRect()}")
            f.openCommands()
            println("$label commands=${f.element(".hub-commands").getBoundingClientRect()} header=${f.element(".hub-commands-header").getBoundingClientRect()} close=${f.element(".hub-close-small").getBoundingClientRect()} list=${f.element(".hub-commands-list").getBoundingClientRect()}")
        }
    }

    @Test
    fun exportReviewImagesWhenRequested() {
        if (System.getenv("SBO_UI_REVIEW") != "1") return
        val version = Regex("versions/([^/]+)/").find(javaClass.protectionDomain.codeSource.location.path)?.groupValues?.get(1) ?: "current"
        val out = HubReviewFixture.project.resolve("build/ui-review/hub-commands/$version")
        val measurements = StringBuilder("Synthetic GuiLib source fixtures. Not in-game screenshots.\nNative item slots blank; Minecraft font approximated; shader shadows omitted.\n")
        for ((label, css) in listOf("before" to HubReviewFixture.baselineCss, "after" to HubReviewFixture.currentCss)) {
            for ((size, dimensions) in listOf("normal" to (640f to 360f), "constrained" to (360f to 240f))) {
                for (state in listOf("default", "hover", "focus", "supporter", "commands", "commands-focus", "commands-bottom")) {
                    val f = HubReviewFixture(css, dimensions.first, dimensions.second, unlocked = state == "supporter", manageCommandsFocus = label == "after")
                    when (state) {
                        "hover" -> f.hover(f.all(".hub-tile").first(), bottom = true)
                        "focus" -> { f.key("Tab"); f.key("Tab") }
                        "commands", "commands-focus", "commands-bottom" -> {
                            f.openCommands()
                            if (state == "commands-focus") {
                                repeat(2) { f.key("Tab") }
                            }
                            if (state == "commands-bottom") { f.element(".hub-commands-list").scrollTop = 10000f; f.settle() }
                        }
                    }
                    val name = "$label-$size-$state"
                    HubReviewRaster.write(f, out.resolve("$name.png"))
                    measurements.append("$name window=${f.element(".hub-window").getBoundingClientRect()}")
                    if (f.commandsOpen) measurements.append(" dialog=${f.element(".hub-commands").getBoundingClientRect()} list=${f.element(".hub-commands-list").getBoundingClientRect()} close=${f.element(".hub-close-small").getBoundingClientRect()}")
                    measurements.append('\n')
                }
            }
        }
        Files.writeString(out.resolve("measurements.txt"), measurements)
        for (state in listOf("default", "commands", "focus")) HubReviewRaster.comparison(out, state)
    }
}
