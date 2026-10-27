package net.sbo.mod.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.math.abs

class SettingsPresentationTest {
    private val themes = listOf("sbo-dark", "light", "high-contrast", "minecraft")

    @Test
    fun normalRowsKeepOneControlColumnAndAlignedTextGutters() {
        for (theme in themes) {
            val f = SettingsReviewFixture(SettingsReviewFixture.currentCss, theme = theme)
            val columns = f.all(".cfg-row-control").map { it.getBoundingClientRect() }
            assertTrue(columns.all { abs(it.width - 160f) < 0.1f })
            assertTrue(columns.all { abs(it.x - columns.first().x) < 0.1f })
            f.all(".cfg-row").forEach { row ->
                val text = row.querySelector(".cfg-row-text")!!.getBoundingClientRect()
                val control = row.querySelector(".cfg-row-control")!!.getBoundingClientRect()
                assertTrue(text.right <= control.x - 7.9f, theme + " label/control gap")
            }
            assertEquals(f.element(".cfg-page-title").getBoundingClientRect().x + 5f,
                f.element(".cfg-row-name").getBoundingClientRect().x, 0.1f, theme + " title inner gutter")
            val stress = SettingsReviewFixture(SettingsReviewFixture.currentCss, theme = theme, mode = "controls")
            assertTrue(stress.all(".cfg-row-control").all { abs(it.getBoundingClientRect().width - 160f) < 0.1f })
        }
    }

    @Test
    fun narrowSearchGetsItsOwnRowAndFieldsStayWithinTheirRows() {
        for (theme in themes) {
            val f = SettingsReviewFixture(SettingsReviewFixture.currentCss, 360f, 400f, theme)
            val header = f.element(".cfg-header").getBoundingClientRect()
            val title = f.element(".cfg-title").getBoundingClientRect()
            val search = f.element(".cfg-search").getBoundingClientRect()
            val close = f.element(".cfg-close").getBoundingClientRect()
            assertTrue(search.y >= title.bottom, theme + " search below title")
            assertTrue(search.width >= header.width - 17f, theme + " full search width")
            assertTrue(close.right <= header.right && close.width >= 18f)
            assertTrue(f.element(".cfg-back").getBoundingClientRect().height >= 18f)
            for (row in f.all(".cfg-row")) {
                val box = row.getBoundingClientRect()
                val text = row.querySelector(".cfg-row-text")!!.getBoundingClientRect()
                val control = row.querySelector(".cfg-row-control")!!.getBoundingClientRect()
                assertTrue(text.right <= control.x - 7.9f, theme + " beside text")
                assertEquals(text.y + text.height / 2, control.y + control.height / 2, 0.2f, theme + " full-block center")
                assertTrue(control.right <= box.right && control.x >= box.x, theme + " contained control")
            }
        }
    }

    @Test
    fun searchTypingClearNoResultsAndEscapeKeepGeneralReachable() {
        val f = SettingsReviewFixture(SettingsReviewFixture.currentCss)
        f.search("overlay")
        assertEquals("overlay", f.queryValue)
        assertEquals(3, f.all(".cfg-row").size)
        val clear = f.element(".cfg-search-clear")
        assertTrue(clear.getBoundingClientRect().width >= 18f)
        f.click(clear)
        assertEquals("", f.queryValue)
        assertEquals(14, f.all(".cfg-row").size)
        f.search("no-such-setting")
        assertTrue(f.all(".cfg-row").isEmpty())
        assertNotNull(f.element(".cfg-empty"))
        // GuiLib first leaves the focused input; the next Escape clears the search.
        f.key("Escape")
        assertFalse(f.element(".cfg-search-input").isFocused)
        assertEquals("no-such-setting", f.queryValue)
        f.key("Escape")
        assertEquals("", f.queryValue)
        assertEquals(14, f.all(".cfg-row").size)
    }

    @Test
    fun searchJumpReopensCollapsedSectionAndKeepsContext() {
        val f = SettingsReviewFixture(SettingsReviewFixture.currentCss, mode = "collapsed")
        assertEquals(11, f.all(".cfg-row").size)
        f.search("bobber")
        assertEquals(1, f.all(".cfg-row-place").size)
        f.click(f.element(".cfg-row-place"))
        assertEquals("", f.queryValue)
        assertEquals(14, f.all(".cfg-row").size)
        val row = f.element("#row-bobberOverlay")
        val viewport = f.element(".cfg-body").getBoundingClientRect()
        assertTrue(row.getBoundingClientRect().y >= viewport.y)
        assertTrue(row.getBoundingClientRect().bottom <= viewport.bottom)
        assertTrue("highlighted" in row.styleClasses)
    }

    @Test
    fun offAndDependencyStatesRetainTheirControlsAndLinks() {
        val off = SettingsReviewFixture(SettingsReviewFixture.currentCss, mode = "off")
        assertEquals(12, off.all(".cfg-row").size)
        assertEquals(1, off.all(".cfg-sub.off").size)
        off.click(off.element(".cfg-sub.off .cfg-section-switch"))
        assertEquals(14, off.all(".cfg-row").size)
        val locked = SettingsReviewFixture(SettingsReviewFixture.currentCss, mode = "locked")
        assertFalse(locked.all(".cfg-row.locked").isEmpty())
        assertFalse(locked.all(".cfg-needs-link").isEmpty())
        assertNotNull(locked.element("#row-chunkCacheAreas .cfg-strings"))
        assertTrue("locked" in locked.element("#row-chunkCacheRenderHubIslandOnly").styleClasses)
    }

    @Test
    fun numericAndSliderKeyboardValuesStillChangeInsideStableCells() {
        val f = SettingsReviewFixture(SettingsReviewFixture.currentCss, mode = "controls")
        val bounds = f.all(".cfg-row-control").map { it.getBoundingClientRect() }
        val buttons = f.all(".cfg-number button")
        assertEquals(2, buttons.size)
        f.click(buttons.last())
        assertEquals(13, f.numberValue)
        f.element(".cfg-slider").focus()
        f.key("ArrowRight")
        assertTrue(f.sliderValue > 50)
        assertEquals(bounds, f.all(".cfg-row-control").map { it.getBoundingClientRect() })
    }

    @Test
    fun headerAndSidebarActionsKeepVisibleFocusAndStableHoverBounds() {
        val f = SettingsReviewFixture(SettingsReviewFixture.currentCss, mode = "search")
        for (selector in listOf(".cfg-back", ".cfg-search-clear", ".cfg-category")) {
            val element = f.element(selector)
            val bounds = element.getBoundingClientRect()
            f.root.document.keyboardModality = true
            element.focus()
            f.settle()
            assertTrue(element.style.outlineWidth > 0f, selector)
            val rect = element.getBoundingClientRect()
            f.root.input.mouseMove(rect.x + rect.width / 2, rect.bottom - 0.2f)
            f.settle()
            assertEquals(bounds, element.getBoundingClientRect())
        }
    }
    @Test
    fun generalFixtureUsesEverySourceEntryWithoutGameHooks() {
        requireHeadlessReview()
        assertEquals(14, SettingsReviewFixture.entries.size)
        assertTrue(SettingsReviewFixture.entries.all { it.name.isNotBlank() && it.description.isNotBlank() })
        assertEquals(listOf("Overlays", "Waypoints", "Accessibility", "Achievements", "Chunk Cache"), SettingsReviewFixture.entries.map { it.section }.distinct())
        val f = SettingsReviewFixture(SettingsReviewFixture.currentCss)
        assertEquals(SettingsReviewFixture.entries.size, f.all(".cfg-row").size)
        println("General rows: " + SettingsReviewFixture.entries.map { it.name })
    }

    @Test
    fun exportSettingsReviewWhenRequested() {
        if (System.getenv("SBO_SETTINGS_REVIEW") != "1") return
        requireHeadlessReview()
        val version = Regex("versions/([^/]+)/").find(javaClass.protectionDomain.codeSource.location.path)?.groupValues?.get(1) ?: "current"
        val out = ReviewResources.project.resolve("build/ui-review/settings-general/" + version)
        val measurements = StringBuilder("Explicit headless JVM; source-derived General labels and synthetic values. Not native screenshots.\n")
        for (theme in listOf("sbo-dark", "light", "high-contrast", "minecraft")) {
            val modes = if (theme == "sbo-dark") listOf("general", "bottom", "search", "empty", "collapsed", "off", "locked", "controls") else listOf("general", "search", "controls")
            for ((label, css) in listOf("before" to SettingsReviewFixture.baselineCss, "after" to SettingsReviewFixture.currentCss)) {
                for ((size, dimensions) in listOf("normal" to (640f to 480f), "constrained" to (360f to 400f))) {
                    for (mode in modes) {
                        val f = SettingsReviewFixture(css, dimensions.first, dimensions.second, theme, if (mode == "bottom") "general" else mode, flatTemplate = label == "after")
                        if (mode == "bottom" || mode == "locked") { f.element(".cfg-body").scrollTop = f.element(".cfg-body").maxScrollTop; f.settle() }
                        val name = "$label-$size-$mode"
                        HubReviewRaster.write(f, out.resolve(theme + "/" + name + ".png"))
                        measurements.append(theme + "/" + name + " header=" + f.element(".cfg-header").getBoundingClientRect() +
                            " search=" + f.element(".cfg-search").getBoundingClientRect() +
                            " columns=" + f.all(".cfg-row-control").take(4).map { it.getBoundingClientRect() } + "\n")
                    }
                }
            }
            for (state in listOf("general", "search", "controls")) HubReviewRaster.comparison(out.resolve(theme), state)
        }
        Files.writeString(out.resolve("measurements.txt"), measurements)
    }
}
