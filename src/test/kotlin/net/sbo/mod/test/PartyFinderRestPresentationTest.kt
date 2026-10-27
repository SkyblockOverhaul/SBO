package net.sbo.mod.test

import net.sbo.mod.partyfinder.gui.ONBOARDING_STEPS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.math.abs

class PartyFinderRestPresentationTest {
    @Test fun rulesContentAndFailureActionsFitBothSizes() {
        for (width in listOf(800f, 360f)) {
            val f = PartyFinderRestFixture("rules", width = width)
            assertEquals(6, f.frame.all(".pf-rule").size)
            val form = f.frame.element(".pf-form").getBoundingClientRect()
            for (rule in f.frame.all(".pf-rule")) {
                val b = rule.getBoundingClientRect(); assertTrue(b.x >= form.x && b.right <= form.right)
            }
            val error = PartyFinderRestFixture("rules-error", width = width)
            error.frame.click(error.frame.element(".pf-message button")); assertEquals(1, error.actionCount)
            assertFalse(PartyFinderRestFixture("rules-loading", width = width).frame.all(".pf-message").isEmpty())
        }
    }
    @Test fun settingsControlsCenterOnTheirFullTextAndRemainContained() {
        assertEquals(11, PartyFinderRestFixture.settings.size)
        for (view in listOf("settings-general", "settings-favorites", "settings-look")) for (width in listOf(800f, 360f)) {
            val f = PartyFinderRestFixture(view, width = width)
            for (row in f.frame.all(".pf-setting")) {
                val label = row.querySelector(".pf-setting-text")!!.getBoundingClientRect()
                val control = (row.querySelector(".guilib-switch") ?: row.querySelector("select") ?: row.querySelector(".guilib-segmented") ?: row.querySelector("button"))!!
                val b = control.getBoundingClientRect(); val r = row.getBoundingClientRect()
                assertTrue(b.x >= r.x && b.right <= r.right + .1, "$view/$width $b in $r")
                assertTrue(abs(label.y + label.height / 2 - b.y - b.height / 2) < .1, "$view/$width full text/control center")
            }
        }
    }
    @Test fun allTourCopyFitsMeasuredTipsAndEscapeStillDismisses() {
        assertEquals(22, ONBOARDING_STEPS.size)
        for (step in ONBOARDING_STEPS.indices) {
            val f = PartyFinderRestFixture("tour-tip", width = 360f, height = 240f, stepIndex = step)
            val b = f.frame.element(".pf-tour-tip").getBoundingClientRect()
            assertTrue(b.x >= 0 && b.right <= 360.1f && b.y >= 0 && b.bottom <= 240.1f, "Tour $step: $b")
            assertEquals(3, f.frame.all(".pf-tour-tip button").size)
        }
        val f = PartyFinderRestFixture("tour-missing", width = 360f, height = 240f)
        f.frame.key("Escape"); assertTrue(f.dismissed)
    }
    @Test fun roleReportAndContextPopupTargetsRemainAvailable() {
        for (view in listOf("join", "change-role", "report", "report-sending")) {
            val f = PartyFinderRestFixture(view, width = 360f, height = 400f)
            val b = f.frame.element(".pf-dialog").getBoundingClientRect()
            assertTrue(b.x >= 0 && b.right <= 360 && b.y >= 0 && b.bottom <= 400)
            f.frame.click(f.frame.element("#rest-submit")); assertEquals(0, f.actionCount)
            f.frame.key("Escape"); assertTrue(f.dismissed)
        }
        for (view in listOf("menu-own", "menu-other", "menu-member")) {
            val f = PartyFinderRestFixture(view, width = 360f, height = 400f)
            f.openMenu()
            val b = f.frame.element(".guilib-menu").getBoundingClientRect()
            assertTrue(b.x >= 0 && b.right <= 360 && b.y >= 0 && b.bottom <= 400, "$view $b")
            f.frame.key("Escape"); assertTrue(f.frame.all(".guilib-menu").isEmpty())
        }
        val selected = PartyFinderRestFixture("join", roleChoice = "berserk")
        selected.frame.click(selected.frame.element("#rest-submit")); assertEquals(1, selected.actionCount)
        assertTrue(selected.dismissed)
    }
    @Test fun exportSelectedUnitWhenRequested() {
        val unit = System.getenv("SBO_PF_REST_REVIEW") ?: return
        requireHeadlessReview()
        val out = PartyFinderRestFixture.out; Files.createDirectories(out)
        val views = when (unit) {
            "rules" -> listOf("rules", "rules-error")
            "settings" -> listOf("settings-general", "settings-look", "settings-favorites", "settings-favorites-empty", "settings-general-empty")
            "tour" -> listOf("tour-welcome", "tour-tip", "tour-missing")
            "dialogs" -> listOf("join", "change-role", "report", "report-sending", "menu-own", "menu-member", "menu-other")
            "all" -> listOf("rules", "rules-error", "settings-general", "settings-look", "settings-favorites", "settings-favorites-empty", "settings-general-empty", "tour-welcome", "tour-tip", "tour-missing", "join", "change-role", "report", "report-sending", "menu-own", "menu-member", "menu-other")
            else -> error(unit)
        }
        val tiles = mutableListOf<HubReviewRaster.Tile>()
        for (view in views) {
            val narrow = view !in listOf("rules", "settings-general", "settings-favorites", "change-role", "menu-own")
            val f = PartyFinderRestFixture(view, width = if (narrow) 360f else 800f, height = if (view.startsWith("tour")) 240f else if (narrow) 400f else 480f,
                roleChoice = if (view == "join") "berserk" else null)
            if (view.startsWith("menu")) f.openMenu()
            val path = out.resolve("after-$view.png"); HubReviewRaster.write(f.frame, path)
            val bounds = (f.frame.all(".guilib-menu").firstOrNull() ?: f.frame.all(".pf-dialog").firstOrNull())?.getBoundingClientRect()
                ?: if (view.startsWith("tour")) net.sbo.guilib.core.dom.Rect(0f, 0f, f.width, f.height) else f.frame.element(".pf-stage").getBoundingClientRect()
            if (view in listOf("rules", "settings-general", "settings-look", "settings-favorites", "tour-tip", "join", "report", "menu-member")) tiles.add(HubReviewRaster.Tile(view, path, bounds))
            if (view in listOf("settings-general", "settings-look", "settings-favorites", "report")) {
                val before = PartyFinderRestFixture(view, css = PartyFinderRestFixture.beforeCss, width = f.width, height = f.height)
                HubReviewRaster.write(before.frame, out.resolve("before-$view.png"))
            }
        }
        if (unit == "all") {
            HubReviewRaster.partyFinderCheckpoint(out.resolve("partyfinder-rest-checkpoint.png"), tiles)
            for (view in listOf("rules", "settings-general")) HubReviewRaster.write(PartyFinderRestFixture(view, width = 360f, height = 400f).frame, out.resolve("after-$view-narrow.png"))
            HubReviewRaster.write(PartyFinderRestFixture("settings-look", width = 360f, height = 400f, theme = "light").frame, out.resolve("after-settings-look-light.png"))
        }
    }
}
