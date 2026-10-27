package net.sbo.mod.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import net.sbo.guilib.core.dom.Rect
import kotlin.math.abs

class PartyFinderFormsPresentationTest {
    @Test
    fun createControlsAndOwnValuesRemainInsideRowsAtBothWidths() {
        for (theme in listOf("sbo-dark", "minecraft")) for (width in listOf(800f, 360f)) {
            val f = PartyFinderFormFixture(width = width, theme = theme)
            for (row in f.all(".pf-field")) {
                val bounds = row.getBoundingClientRect()
                for (selector in listOf(".pf-field-input", ".pf-own", ".guilib-number", "select")) {
                    for (e in row.querySelectorAll(selector)) {
                        val b = e.getBoundingClientRect()
                        // Wide/narrow alternatives have a zero box when hidden.
                        if (b.width > 0) assertTrue(b.x >= bounds.x - .1f && b.right <= bounds.right + .1f, "$width $selector $b in $bounds")
                    }
                }
            }
            f.scrollTo("#section-note")
            val note = f.element(".pf-note-input").getBoundingClientRect()
            val form = f.element(".pf-create-form").getBoundingClientRect()
            assertTrue(note.x >= form.x && note.right <= form.right)
            f.frame.click(f.element("#form-reset"))
            assertNull(f.numeric)
            assertTrue(f.selectedItems.isEmpty())
        }
    }

    @Test
    fun formDropdownKeyboardAndUnavailableActionRemainUsable() {
        val f = PartyFinderFormFixture(width = 360f)
        f.scrollTo("#field-rarity")
        val select = f.element("#field-rarity select")
        assertEquals(0, select.style.borderTopColor ushr 24)
        f.frame.root.document.keyboardModality = true
        select.focus(); f.frame.settle()
        assertTrue(select.style.outlineWidth > 0)
        f.frame.key(" ")
        assertFalse(f.all(".guilib-select-menu").isEmpty())
        f.frame.key("Escape")
        assertTrue(f.all(".guilib-select-menu").isEmpty())
        val disabled = PartyFinderFormFixture(width = 360f, edit = true, unavailable = true, empty = true)
        disabled.scrollTo("#form-submit")
        disabled.frame.click(disabled.element("#form-submit"))
        assertEquals(0, disabled.submitted)
        assertNull(disabled.numeric)
        assertTrue(disabled.all(".pf-own").isEmpty(), "The missing-own-data specimen must not display a comparison")
    }

    @Test
    fun filterControlsFitAndExistingEscapeDismissalRemainsAvailable() {
        for (width in listOf(800f, 360f)) {
            val f = PartyFinderFormFixture(width = width, filters = true)
            val modal = f.element(".pf-filter-dialog").getBoundingClientRect()
            assertTrue(modal.x >= 0 && modal.right <= width + .1)
            for (row in f.all(".pf-filter-row")) {
                val b = row.getBoundingClientRect()
                for (e in row.querySelectorAll(".pf-filter-input")) {
                    val field = e.getBoundingClientRect()
                    assertTrue(field.x >= b.x && field.right <= b.right + .1, "$width $field in $b")
                    val label = row.querySelector(".pf-filter-label")!!.getBoundingClientRect()
                    assertTrue(abs((field.y + field.height / 2) - (label.y + label.height / 2)) < .1f,
                        "Control centers on the complete label/help block at $width")
                }
            }
            f.scrollTo("#filter-sort")
            f.element("#filter-sort select").focus(); f.frame.key(" ")
            assertFalse(f.all(".guilib-select-menu").isEmpty())
            f.frame.key("Escape")
            assertTrue(f.all(".guilib-select-menu").isEmpty())
            // GuiLib currently dismisses the filter modal with the open select on Escape.
            // Characterize that existing behavior; this presentation pass does not alter it.
            assertFalse(f.modalOpen)
        }
    }

    @Test
    fun exportCurrentUnitWhenRequested() {
        val unit = System.getenv("SBO_PF_FORMS_REVIEW") ?: return
        requireHeadlessReview()
        val out = PartyFinderFormFixture.out
        Files.createDirectories(out)
        val measurements = StringBuilder()
        val tiles = mutableMapOf<String, HubReviewRaster.Tile>()
        for ((label, css) in listOf("before" to PartyFinderFormFixture.beforeCss, "after" to PartyFinderReviewFixture.currentCss)) {
            if (unit in listOf("create", "all")) for (width in listOf(800f, 360f)) {
                val f = PartyFinderFormFixture(css, width, if (width < 400) 400f else 480f)
                val name = "$label-create-" + if (width < 400) "narrow" else "normal"
                HubReviewRaster.write(f.frame, out.resolve("$name.png"))
                tiles[name] = HubReviewRaster.Tile(name, out.resolve("$name.png"), f.element(".pf-stage").getBoundingClientRect())
                measurements.append(name + " " + f.all(".pf-field").map { it.getBoundingClientRect() } + "\n")
            }
            if (unit in listOf("filters", "all")) for (width in listOf(800f, 360f)) {
                val f = PartyFinderFormFixture(css, width, if (width < 400) 400f else 480f, filters = true)
                val name = "$label-filters-" + if (width < 400) "narrow" else "normal"
                HubReviewRaster.write(f.frame, out.resolve("$name.png"))
                tiles[name] = HubReviewRaster.Tile(name, out.resolve("$name.png"), f.element(".pf-filter-dialog").getBoundingClientRect())
                measurements.append(name + " " + f.all(".pf-filter-row").map { it.getBoundingClientRect() } + "\n")
            }
            if (unit in listOf("create", "all")) {
                val f = PartyFinderReviewFixture(css)
                HubReviewRaster.write(f, out.resolve("$label-list.png"))
                tiles["$label-list"] = HubReviewRaster.Tile(label, out.resolve("$label-list.png"), f.element(".pf-stage").getBoundingClientRect())
            }
        }
        if (unit in listOf("create", "all")) {
            val edit = PartyFinderFormFixture(width = 360f, height = 400f, edit = true)
            edit.scrollTo("#section-roles")
            HubReviewRaster.write(edit.frame, out.resolve("after-edit-bottom.png"))
            tiles["after-edit-bottom"] = HubReviewRaster.Tile("Narrow Edit", out.resolve("after-edit-bottom.png"), edit.element(".pf-stage").getBoundingClientRect())
            val disabled = PartyFinderFormFixture(width = 360f, height = 400f, unavailable = true, empty = true)
            HubReviewRaster.write(disabled.frame, out.resolve("after-create-disabled-empty.png"))
        }
        Files.writeString(out.resolve("$unit-measurements.txt"), measurements)
        if (unit == "all") {
            val a = tiles.getValue("before-filters-normal").bounds
            val b = tiles.getValue("after-filters-normal").bounds
            val x = minOf(a.x, b.x); val y = minOf(a.y, b.y)
            val filterBounds = Rect(x, y, maxOf(a.right, b.right) - x, maxOf(a.bottom, b.bottom) - y)
            val order = listOf("after-list" to "List · plain requirements", "after-edit-bottom" to "Narrow Edit · roles, note and actions",
                "before-create-normal" to "Create · before", "after-create-normal" to "Create · after",
                "before-filters-normal" to "Filters · before", "after-filters-normal" to "Filters · after")
            HubReviewRaster.partyFinderCheckpoint(out.resolve("partyfinder-forms-checkpoint.png"), order.map { (key, title) ->
                tiles.getValue(key).copy(label = title, bounds = if (key.contains("filters")) filterBounds else tiles.getValue(key).bounds)
            })
            HubReviewRaster.write(PartyFinderFormFixture(theme = "light").frame, out.resolve("after-create-light.png"))
            val menu = PartyFinderFormFixture(width = 360f, height = 400f, filters = true)
            menu.scrollTo("#filter-sort"); menu.frame.click(menu.element("#filter-sort select"))
            HubReviewRaster.write(menu.frame, out.resolve("after-filters-open.png"))
            Files.writeString(out.resolve("inventory.txt"), "Source-schema specimens, synthetic definitions (not server category inventory).\n" +
                "Create/Edit: min numeric and labelled min; flag; rarity; anyOf/multiselect, Any/All, item tier; single/segmented option; own role and wanted roles; note, Reset/Create/Update; unavailable/empty.\n" +
                "Filters: sizes, free spots, single/multiple options, requirements, flag, roles, sort, Reset/Done; existing Escape dismissal.\n" +
                "Production note limits: ${PartyFinderFormFixture.noteMaxLength} characters, ${PartyFinderFormFixture.noteMaxLines} lines.\n" +
                "No live API, drafts, queue, own-stat loading, permissions or native rendering exercised.\n")
        }
    }
}
