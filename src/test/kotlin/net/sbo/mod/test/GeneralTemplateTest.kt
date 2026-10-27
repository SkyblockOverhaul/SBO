package net.sbo.mod.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import net.sbo.guilib.core.paint.PaintCommand

class GeneralTemplateTest {
    private val css get() = SettingsReviewFixture.currentCss

    @Test
    fun genuineSectionSwitchKeepsEnableAndCollapseSeparate() {
        val f = SettingsReviewFixture(css, flatTemplate = true)
        assertTrue(f.all(".cfg-sub-toggle").isEmpty())
        assertEquals(5, f.all(".cfg-section-switch").size)
        val selector = "#row-group-Overlays .cfg-section-switch"
        val toggle = f.element(selector)
        val bounds = toggle.getBoundingClientRect()
        assertTrue(bounds.width >= 24f && bounds.height >= 18f)
        val track = toggle.querySelector(".guilib-switch-track")!!.getBoundingClientRect()
        val rowTrack = f.element("#row-bobberOverlay .guilib-switch-track").getBoundingClientRect()
        val thumb = toggle.querySelector(".guilib-switch-thumb")!!.getBoundingClientRect()
        val rowThumb = f.element("#row-bobberOverlay .guilib-switch-thumb").getBoundingClientRect()
        assertEquals(rowTrack.width, track.width, 0.01f)
        assertEquals(rowTrack.height, track.height, 0.01f)
        assertEquals(rowThumb.width, thumb.width, 0.01f)
        assertEquals(rowThumb.height, thumb.height, 0.01f)
        assertNotNull(toggle.querySelector(".guilib-switch-label"))
        f.root.document.keyboardModality = true
        toggle.focus(); f.settle()
        assertTrue(toggle.style.outlineWidth > 0f)
        f.key(" ")
        assertEquals(11, f.all(".cfg-row").size)
        assertFalse("checked" in f.element(selector).styleClasses)
        f.element(selector).focus(); f.key(" ")
        assertEquals(14, f.all(".cfg-row").size)
        val heading = f.element("#row-group-Overlays .cfg-sub-title")
        heading.focus(); f.key("Enter")
        assertEquals(11, f.all(".cfg-row").size)
        assertTrue("checked" in f.element(selector).styleClasses)
        heading.focus(); f.key("Enter")
        assertEquals(14, f.all(".cfg-row").size)
        assertEquals(bounds, f.element(selector).getBoundingClientRect())
    }

    @Test
    fun flatPreviewRetainsInputsAndFitsNormalAndNarrowViewports() {
        for (width in listOf(640f, 360f)) {
            val f = SettingsReviewFixture(css, width, 400f, flatTemplate = true)
            assertEquals(0, f.element(".cfg-sidebar").style.backgroundColor ushr 24)
            assertEquals(0, f.element(".cfg-category.active").style.backgroundColor ushr 24)
            assertEquals(14, f.all(".cfg-row").size)
            val search = f.element(".cfg-search-input")
            val before = search.getBoundingClientRect()
            assertEquals(0, search.style.borderTopColor ushr 24)
            assertNotEquals(f.element(".cfg-window").style.backgroundColor, search.style.backgroundColor)
            f.root.document.keyboardModality = true
            search.focus(); f.settle()
            assertTrue(search.style.outlineWidth > 0f)
            assertEquals(before, search.getBoundingClientRect())
            f.root.input.charTyped("overlay"); f.settle()
            assertEquals("overlay", f.queryValue)
            f.click(f.element(".cfg-search-clear"))
            assertEquals("", f.queryValue)
            val select = f.element(".cfg-select")
            assertEquals(0, select.style.borderTopColor ushr 24)
            assertEquals(search.style.backgroundColor, select.style.backgroundColor)
            assertTrue(f.element(".cfg-sub-head").style.borderBottomWidth > 0f)
            f.root.document.keyboardModality = true
            select.focus(); f.settle()
            assertTrue(select.style.outlineWidth > 0f)
            for (target in f.all(".cfg-section-switch")) {
                val b = target.getBoundingClientRect()
                assertTrue(b.x >= 0 && b.right <= width)
                f.root.input.mouseMove(b.right - 0.1f, b.bottom - 0.1f); f.settle()
                assertEquals(b, target.getBoundingClientRect())
            }
        }
    }

    @Test
    fun filledSelectKeepsMouseSelectionEscapeAndStableBounds() {
        val f = SettingsReviewFixture(css, flatTemplate = true)
        val select = f.element(".cfg-select")
        val bounds = select.getBoundingClientRect()
        f.click(select)
        assertEquals(1, f.all(".guilib-select-menu").size)
        f.click(f.all(".guilib-option").first())
        assertEquals(1, f.all(".guilib-option.selected").size)
        select.focus(); f.key("Escape")
        assertTrue(f.all(".guilib-select-menu").isEmpty())
        assertEquals(bounds, select.getBoundingClientRect())
        val locked = SettingsReviewFixture(css, mode = "locked", flatTemplate = true)
        assertFalse(locked.all(".cfg-row.locked").isEmpty())
    }

    @Test
    fun rowControlsCenterOnTheWholeTitleAndDescriptionBlock() {
        val report = StringBuilder("Logical bounds: controls centered beside complete title+description blocks.\n")
        for (width in listOf(640f, 360f)) {
            val f = SettingsReviewFixture(css, width, 400f, flatTemplate = true)
            for (row in f.all(".cfg-row")) {
                val text = row.querySelector(".cfg-row-text")!!.getBoundingClientRect()
                val control = row.querySelector(".cfg-row-control")!!.getBoundingClientRect()
                val textCenter = text.y + text.height / 2
                val controlCenter = control.y + control.height / 2
                assertEquals(textCenter, controlCenter, 0.2f)
                assertTrue(text.right <= control.x - 7.9f)
                assertTrue(control.right <= row.getBoundingClientRect().right + 0.1f)
                report.append("width=$width ${row.id} text=$text control=$control centers=$textCenter/$controlCenter\n")
            }
            assertEquals(14, f.all(".cfg-row-description").size)
            val divider = f.element("#row-group-Waypoints .cfg-sub-head").getBoundingClientRect()
            val dropdown = f.element("#row-hideOwnWaypoints .cfg-select").getBoundingClientRect()
            val gap = dropdown.y - divider.bottom
            assertTrue(gap >= 4f, "Clear divider/dropdown gap: $gap")
            report.append("width=$width divider→dropdown=$gap\n")
            val title = f.element("#row-assumeMuted .cfg-row-name").getBoundingClientRect()
            val description = f.element("#row-assumeMuted .cfg-row-description").getBoundingClientRect()
            assertTrue(description.height > title.height, "Wrapped description contributes to alignment")
        }
        val out = ReviewResources.project.resolve("build/ui-review/general-template")
        Files.createDirectories(out)
        Files.writeString(out.resolve("row-centering-measurements.txt"), report)
    }

    @Test
    fun sectionHeadingsAttachMoreCloselyToTheirOwnContent() {
        fun measure(f: SettingsReviewFixture): Pair<Float, Float> {
            val paint = f.settle().filterIsInstance<PaintCommand.Text>()
            val heading = paint.first { it.text == "Overlays" }
            val first = paint.first { it.text == "Bobber Overlay" }
            val next = paint.first { it.text == "Waypoints" }
            val lastText = f.element("#row-move .cfg-row-text").getBoundingClientRect()
            val lastControl = f.element("#row-move .cfg-row-control").getBoundingClientRect()
            return (first.y - heading.y - f.fonts.metrics(heading.style).normalLineHeight) to
                (next.y - maxOf(lastText.bottom, lastControl.bottom))
        }
        val approved = Files.readString(ReviewResources.project.resolve("tools/ui-review/baseline/settings-before-adoption.css")) + "\n" +
            Files.readString(ReviewResources.project.resolve("tools/ui-review/baseline/general-template-spacing-v2.css"))
        val report = StringBuilder("Logical pixels; actual painted text positions plus nested wrapper/content bounds.\n")
        for (width in listOf(640f, 360f)) {
            val before = SettingsReviewFixture(approved, width, 400f, flatTemplate = true)
            val after = SettingsReviewFixture(css, width, 400f, flatTemplate = true)
            val old = measure(before); val revised = measure(after)
            report.append("width=$width before: heading→content=${old.first}, content→next-heading=${old.second}\n")
            report.append("width=$width after: heading→content=${revised.first}, content→next-heading=${revised.second}\n")
            for ((label, f) in listOf("before" to before, "after" to after)) for (selector in listOf("#row-group-Overlays", "#row-group-Overlays .cfg-sub-head", "#row-group-Overlays .cfg-sub-title", "#row-bobberOverlay", "#row-bobberOverlay .cfg-row-name", "#row-move", "#row-group-Waypoints")) {
                report.append("$width $label $selector ${f.element(selector).getBoundingClientRect()}\n")
            }
            assertTrue(revised.first <= 12f, report.toString())
            assertTrue(revised.second in 16f..23f, report.toString())
            assertTrue(revised.second > revised.first + 6f, report.toString())
        }
        val out = ReviewResources.project.resolve("build/ui-review/general-template")
        Files.createDirectories(out)
        Files.writeString(out.resolve("spacing-measurements.txt"), report)
        println(report)
    }

    @Test
    fun exportSingleGeneralFeedbackPreviewWhenRequested() {
        if (System.getenv("SBO_GENERAL_TEMPLATE") != "1") return
        requireHeadlessReview()
        val out = ReviewResources.project.resolve("build/ui-review/general-template")
        Files.createDirectories(out)
        val original = ReviewResources.project.resolve("build/ui-review/settings-general/26.2-fabric/sbo-dark/after-normal-general.png")
        val before = out.resolve("before-normal.png")
        if (!Files.exists(before)) Files.copy(original, before)
        HubReviewRaster.write(SettingsReviewFixture(css, flatTemplate = true), out.resolve("general-template-revised.png"))
        HubReviewRaster.write(SettingsReviewFixture(css, 360f, 400f, flatTemplate = true), out.resolve("after-constrained.png"))
    }
}
