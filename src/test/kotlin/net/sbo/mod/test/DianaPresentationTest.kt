package net.sbo.mod.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.math.abs

class DianaPresentationTest {
    private val themes = listOf("sbo-dark", "light", "high-contrast", "minecraft")
    @Test
    fun inventoryComesFromDeclarationsWithoutInitializingSettings() {
        requireHeadlessReview()
        val data = DianaReviewData.entries
        assertTrue(data.all { it.name.isNotBlank() && it.description.isNotBlank() }, data.filter { it.name.isBlank() || it.description.isBlank() }.toString())
        assertEquals(Regex("(?m)^    var ").findAll(java.nio.file.Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/settings/categories/Diana.kt"))).count() + data.count { it.kind == "button" }, data.size)
        assertEquals(9, DianaReviewData.groups.size)
        assertEquals(3, data.count { it.kind == "button" })
        assertEquals(10, data.count { it.kind == "strings" })
        assertEquals(6, data.count { it.kind == "select" })
        assertEquals(listOf("OFF", "TOTAL", "EVENT", "SESSION"), data.first { it.id == "mobTracker" }.options)
        assertEquals(15.0, data.first { it.id == "afkTimeout" }.minimum)
        assertTrue(data.first { it.id == "arrowGuess" }.description.contains("\n"))
        assertFalse(data.any { it.kind in listOf("rgb", "rgba") })
        println("Diana inventory: " + data.groupingBy { it.kind }.eachCount())
    }

    @Test
    fun allGroupsAndRowsRemainPresent() {
        val f = DianaReviewFixture(DianaReviewFixture.currentCss)
        assertEquals(DianaReviewData.entries.size, f.all(".cfg-row").size)
        assertEquals(9, f.all(".cfg-sub").size)
    }

    @Test
    fun switchTracksAlignWithLabelTextInsideStableEighteenPixelTargets() {
        for (theme in themes) {
            val f = DianaReviewFixture(DianaReviewFixture.currentCss, theme = theme)
            val label = f.element("#row-spadeGuess .cfg-row-text").getBoundingClientRect()
            val target = f.element("#row-spadeGuess .guilib-switch")
            val bounds = target.getBoundingClientRect()
            val track = f.element("#row-spadeGuess .guilib-switch-track").getBoundingClientRect()
            assertEquals(18f, bounds.height, 0.1f)
            assertEquals(11f, track.height, 0.1f)
            assertTrue(abs(label.y + label.height / 2 - track.y - track.height / 2) <= 1.1f, theme)
            f.root.input.mouseMove(bounds.x + 1, bounds.bottom - 0.2f)
            f.settle()
            assertEquals(bounds, target.getBoundingClientRect())
            f.click(target)
            assertEquals("false", f.currentValues["spadeGuess"])
        }
    }

    @Test
    fun everyDianaControlFitsItsCellAcrossThemesAndWidths() {
        for (theme in themes) for (width in listOf(640f, 360f)) {
            val f = DianaReviewFixture(DianaReviewFixture.currentCss, width, 480f, theme, "strings")
            for (row in f.all(".cfg-row")) {
                val cell = row.querySelector(".cfg-row-control")!!.getBoundingClientRect()
                assertEquals(if (width > 460f) 160f else 76f, cell.width, 0.1f)
                for (selector in listOf(".cfg-select", ".cfg-number", ".guilib-slider-field", ".cfg-strings", ".cfg-textarea", ".cfg-strings-expand")) {
                    for (control in row.querySelectorAll(selector)) {
                        val box = control.getBoundingClientRect()
                        assertTrue(box.x >= cell.x - 0.1f && box.right <= cell.right + 0.1f, theme + " $width $selector: $box in $cell")
                    }
                }
            }
        }
    }

    @Test
    fun multilineStringsPreserveNewlinesAndExpandedEditorEscape() {
        for (width in listOf(640f, 360f)) {
        val f = DianaReviewFixture(DianaReviewFixture.currentCss, width = width, mode = "strings")
        f.scrollTo("customChimeraMessage")
        if (System.getenv("SBO_UPSTREAM_REVIEW") == "1") HubReviewRaster.write(f,
            ReviewResources.project.resolve("build/ui-review/upstream-integration/strings-${width.toInt()}.png"))
        val input = f.element("#row-customChimeraMessage .cfg-textarea")
        input.focus()
        f.key("End")
        f.root.input.charTyped(" fixture")
        f.settle()
        assertTrue(f.currentLines.getValue("customChimeraMessage").any { it.endsWith(" fixture") })
        val expand = f.element("#row-customChimeraMessage .cfg-strings-expand")
        assertEquals(18f, expand.getBoundingClientRect().width, 0.1f)
        assertEquals(18f, expand.getBoundingClientRect().height, 0.1f)
        f.root.document.keyboardModality = true
        expand.focus(); f.key("Enter")
        assertTrue(f.element(".cfg-editor-text").isFocused)
        f.key("End"); f.key("Enter")
        f.root.input.charTyped("last line"); f.settle()
        assertEquals("last line", f.currentLines.getValue("customChimeraMessage").last())
        if (System.getenv("SBO_UPSTREAM_REVIEW") == "1") HubReviewRaster.write(f,
            ReviewResources.project.resolve("build/ui-review/upstream-integration/editor-${width.toInt()}.png"))
        // GuiLib first blurs a focused textarea; the next Escape runs the editor's back handler.
        f.key("Escape")
        assertFalse(f.element(".cfg-editor-text").isFocused)
        assertFalse(f.all(".cfg-editor").isEmpty())
        f.key("Escape")
        assertTrue(f.all(".cfg-editor").isEmpty())
        f.click(f.element("#row-customChimeraMessage .cfg-strings-expand"))
        f.click(f.element(".cfg-editor-done"))
        assertTrue(f.all(".cfg-editor").isEmpty())
        assertEquals("last line", f.currentLines.getValue("customChimeraMessage").last())
        }
    }

    @Test
    fun enumAndMultiSelectKeepKeyboardEscapeAndSelectedValues() {
        val f = DianaReviewFixture(DianaReviewFixture.currentCss)
        f.open("#row-mobTracker .cfg-select")
        assertEquals(4, f.all(".guilib-option").size)
        f.key("ArrowDown"); f.key("Enter")
        assertEquals("TOTAL", f.currentValues["mobTracker"])
        assertTrue(f.all(".guilib-select-menu").isEmpty())
        assertTrue(f.element("#row-mobTracker .cfg-select").isFocused)
        f.open("#row-allowedWarps .cfg-select")
        assertEquals(7, f.all(".guilib-option").size)
        f.click(f.all(".guilib-option")[2])
        assertTrue("CRYPT" in f.currentValues.getValue("allowedWarps").split(','))
        assertEquals(1, f.all(".guilib-select-menu").size)
        assertTrue(f.element("#row-allowedWarps .cfg-select").isFocused)
        f.key("Escape")
        assertTrue(f.all(".guilib-select-menu").isEmpty())
        assertTrue(f.element("#row-allowedWarps .cfg-select").isFocused)
    }

    @Test
    fun baselineMouseSelectionDemonstratesLostMultiselectEscape() {
        val f = DianaReviewFixture(DianaReviewFixture.baselineCss, pickerFocus = false)
        f.open("#row-allowedWarps .cfg-select")
        f.click(f.all(".guilib-option")[2])
        assertFalse(f.element("#row-allowedWarps .cfg-select").isFocused)
        repeat(2) { f.key("Escape") }
        assertEquals(1, f.all(".guilib-select-menu").size)
    }

    @Test
    fun narrowMinecraftCategoryDoesNotLeaveAnOrphanLetter() {
        val f = DianaReviewFixture(DianaReviewFixture.currentCss, 360f, 400f, "minecraft")
        assertTrue(f.settle().filterIsInstance<net.sbo.guilib.core.paint.PaintCommand.Text>().any { it.text == "Customization" })
    }

    @Test
    fun lightFormattedDescriptionsKeepReadableColorMeaning() {
        fun luminance(argb: Int): Double {
            fun channel(shift: Int): Double {
                val value = ((argb ushr shift) and 255) / 255.0
                return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
            }
            return channel(16) * 0.2126 + channel(8) * 0.7152 + channel(0) * 0.0722
        }
        val f = DianaReviewFixture(DianaReviewFixture.currentCss, theme = "light")
        assertFalse(f.all(".cfg-format-a").isEmpty())
        assertFalse(f.all(".cfg-format-b").isEmpty())
        for (color in listOf("a", "b", "c", "6")) for (run in f.all(".cfg-format-$color")) {
            val foreground = luminance(run.style.color)
            for (background in listOf(0xFFF2F3F5.toInt(), 0xFFFFFFFF.toInt())) {
                val surface = luminance(background)
                val contrast = (maxOf(foreground, surface) + 0.05) / (minOf(foreground, surface) + 0.05)
                assertTrue(contrast >= 4.5, "Formatted $color contrast=$contrast")
            }
        }
        val dark = DianaReviewFixture(DianaReviewFixture.currentCss)
        assertTrue(dark.all(".cfg-format-a, .cfg-format-b").isEmpty())
    }

    @Test
    fun rgbAlphaAndChoicePopoversStayInsideViewportAndPreserveEscape() {
        for (theme in themes) for (width in listOf(640f, 360f)) {
            val f = DianaReviewFixture(DianaReviewFixture.currentCss, width, 400f, theme, "pickers")
            for (id in listOf("rgb", "rgba")) {
                f.open("#row-$id .guilib-color-input")
                val popup = f.element(".guilib-color-popover").getBoundingClientRect()
                assertTrue(popup.x >= 0 && popup.y >= 0 && popup.right <= width + 0.1 && popup.bottom <= 400.1, "$theme $width: $popup")
                assertEquals(if (id == "rgba") 1 else 0, f.all(".guilib-cp-alpha").size)
                if (id == "rgba") {
                    val old = f.currentValues[id]
                    val alpha = f.element(".guilib-cp-alpha").getBoundingClientRect()
                    f.root.input.mouseDown(alpha.x + alpha.width * 0.25f, alpha.y + alpha.height / 2, 0)
                    f.root.input.mouseUp(alpha.x + alpha.width * 0.25f, alpha.y + alpha.height / 2, 0)
                    f.settle()
                    assertNotEquals(old, f.currentValues[id])
                }
                f.key("Escape")
                assertTrue(f.all(".guilib-color-popover").isEmpty())
                assertTrue(f.element("#row-$id .guilib-color-input").isFocused)
            }
            f.open("#row-choice .cfg-select")
            f.key("ArrowDown"); f.key("Enter")
            assertEquals("Long descriptive choice label", f.currentValues["choice"])
        }
    }

    @Test
    fun groupAndDependencyStatesRetainAllContentAndJumpContext() {
        val collapsed = DianaReviewFixture(DianaReviewFixture.currentCss, mode = "collapsed")
        assertEquals(DianaReviewData.entries.count { it.section != "Diana Burrows" }, collapsed.all(".cfg-row").size)
        collapsed.click(collapsed.element(".cfg-sub.collapsed .cfg-sub-title"))
        assertEquals(DianaReviewData.entries.size, collapsed.all(".cfg-row").size)
        val off = DianaReviewFixture(DianaReviewFixture.currentCss, mode = "off")
        assertEquals(DianaReviewData.entries.count { it.section != "Diana Burrows Preferences" }, off.all(".cfg-row").size)
        off.scrollTo("group-Diana-Burrows-Preferences")
        off.click(off.element(".cfg-sub.off .cfg-section-switch"))
        assertEquals(DianaReviewData.entries.size, off.all(".cfg-row").size)
        val dependent = DianaReviewFixture(DianaReviewFixture.currentCss, mode = "dependent")
        val row = dependent.element("#row-combineLootLines")
        assertTrue("locked" in row.styleClasses)
        assertEquals(1, row.querySelectorAll(".cfg-needs-link").size)
        dependent.scrollTo("combineLootLines")
        dependent.click(dependent.element("#row-combineLootLines .cfg-needs-link"))
        assertTrue("highlighted" in dependent.element("#row-lootTracker").styleClasses)
    }

    @Test
    fun actualIntegerDecimalAndSliderControlsKeepTheirRanges() {
        val f = DianaReviewFixture(DianaReviewFixture.currentCss)
        f.scrollTo("afkTimeout")
        f.click(f.all("#row-afkTimeout .cfg-number button").last())
        assertEquals("61", f.currentValues["afkTimeout"])
        f.scrollTo("hpAlert")
        f.click(f.all("#row-hpAlert .cfg-number button").last())
        assertEquals(0.1, f.currentValues.getValue("hpAlert").toDouble(), 0.0001)
        f.scrollTo("warpDiff")
        f.element("#row-warpDiff .cfg-slider").focus(); f.key("ArrowRight")
        assertEquals("23", f.currentValues["warpDiff"])
    }

    @Test
    fun exportDianaReviewWhenRequested() {
        if (System.getenv("SBO_DIANA_REVIEW") != "1") return
        requireHeadlessReview()
        val version = Regex("versions/([^/]+)/").find(javaClass.protectionDomain.codeSource.location.path)?.groupValues?.get(1) ?: "current"
        val out = ReviewResources.project.resolve("build/ui-review/settings-diana/" + version)
        val measurements = StringBuilder("Explicit headless JVM; source-derived Diana data; synthetic isolated values/actions. Not native screenshots.\n")
        val normalStates = listOf("expanded", "warp", "tracker", "announcer", "timings", "waypoints", "rare", "other", "collapsed", "off", "dependent", "strings", "enum-open", "multi-open", "picker-rgb", "picker-alpha", "picker-choice")
        val sections = mapOf("warp" to "Diana Warp", "tracker" to "Diana Tracker", "announcer" to "Diana Announcer", "timings" to "Rare Mob Title Timings", "waypoints" to "Diana Waypoints", "rare" to "Rare Mobs", "other" to "Other", "strings" to "Diana Announcer", "dependent" to "Diana Tracker", "enum-open" to "Diana Tracker", "multi-open" to "Diana Warp")
        val themes = if (System.getenv("SBO_DIANA_QUICK") == "1") listOf("sbo-dark") else listOf("sbo-dark", "light", "high-contrast", "minecraft")
        for (theme in themes) {
            val states = normalStates
            for ((label, css) in listOf("before" to DianaReviewFixture.baselineCss, "after" to DianaReviewFixture.currentCss)) {
                for ((size, dimensions) in listOf("normal" to (640f to 480f), "constrained" to (360f to 400f))) {
                    for (state in states) {
                        val mode = when { state.startsWith("picker-") -> "pickers"; state in listOf("expanded", "collapsed", "off", "dependent", "strings") -> state; else -> "expanded" }
                        val f = DianaReviewFixture(css, dimensions.first, dimensions.second, theme, mode, sections[state],
                            pickerFocus = label == "after", lightContrast = label == "after", sectionSwitches = label == "after")
                        when (state) {
                            "strings" -> f.scrollTo("customChimeraMessage")
                            "off" -> f.scrollTo("group-Diana-Burrows-Preferences")
                            "dependent" -> f.scrollTo("combineLootLines")
                            "enum-open" -> f.open("#row-mobTracker .cfg-select")
                            "multi-open" -> f.open("#row-allowedWarps .cfg-select")
                            "picker-rgb" -> f.open("#row-rgb .guilib-color-input")
                            "picker-alpha" -> f.open("#row-rgba .guilib-color-input")
                            "picker-choice" -> f.open("#row-choice .cfg-select")
                        }
                        val name = "$label-$size-$state"
                        HubReviewRaster.write(f, out.resolve(theme + "/" + name + ".png"))
                        measurements.append(theme + "/" + name + " fields=" + f.all(".cfg-row-control").take(3).map { it.getBoundingClientRect() } +
                            " menus=" + (f.all(".guilib-select-menu") + f.all(".guilib-color-popover")).map { it.getBoundingClientRect() } + "\n")
                    }
                }
            }
            for (state in states) HubReviewRaster.comparison(out.resolve(theme), state)
        }
        Files.writeString(out.resolve("measurements.txt"), measurements)
        Files.writeString(out.resolve("inventory.txt"), DianaReviewData.groups.joinToString("\n\n") { group ->
            group.name + "\n" + group.description + "\n" + DianaReviewData.entries.filter { it.section == group.name }.joinToString("\n") {
                it.id + " | " + it.kind + " | " + it.name + " | " + it.initial + " | " + it.options + " | " + it.requirements
            }
        })
    }
}
