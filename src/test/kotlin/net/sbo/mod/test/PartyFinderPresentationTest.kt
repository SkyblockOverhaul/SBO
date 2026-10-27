package net.sbo.mod.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class PartyFinderPresentationTest {
    @Test
    fun listAndInspectorStayWithinTheirAllocatedColumns() {
        for (theme in listOf("sbo-dark", "light", "high-contrast", "minecraft")) for (width in listOf(800f, 360f)) {
            val f = PartyFinderReviewFixture(width = width, inspector = true, theme = theme, queued = true)
            val stage = f.element(".pf-stage").getBoundingClientRect()
            assertTrue(stage.x >= 0 && stage.right <= width + 0.1f, "$theme/$width $stage")
            val panel = f.element(".pf-panel").getBoundingClientRect()
            for (row in f.all(".pf-panel-row")) {
                val b = row.getBoundingClientRect()
                for (selector in listOf(".pf-stat-label", ".pf-panel-value", ".pf-panel-need")) {
                    row.querySelector(selector)?.getBoundingClientRect()?.let {
                        assertTrue(it.x >= b.x - 0.1 && it.right <= b.right + 0.1, "$theme/$width $selector $it in $b")
                    }
                }
            }
            val close = f.element(".pf-panel .pf-close").getBoundingClientRect()
            assertTrue(close.x >= panel.x && close.right <= panel.right && close.y >= panel.y && close.bottom <= panel.bottom)
            val list = f.element(".pf-list").getBoundingClientRect()
            assertTrue(list.height > 90, "$theme/$width leaves a usable scroll viewport: $list")
            val reqs = f.all(".pf-req")
            assertEquals(9, reqs.size)
            assertTrue(reqs.all { it.style.borderTopWidth == 0f && (it.style.backgroundColor ushr 24) == 0 })
            assertNotEquals(reqs.first().style.color, f.element(".pf-req.bad").style.color)
            for (join in f.all(".pf-join")) {
                val card = f.all(".pf-card").first { it.contains(join) }.getBoundingClientRect()
                val b = join.getBoundingClientRect()
                assertTrue(b.x >= card.x && b.right <= card.right + 0.1, "$width join $b in $card")
            }
        }
    }

    @Test
    fun stableJoinHitboxAndMemberInspectionDoNotToggleTheirParty() {
        val f = PartyFinderReviewFixture()
        assertNotEquals(f.element(".pf-req.ok").style.color, f.element(".pf-req.bad").style.color)
        val join = f.all(".pf-join").first()
        val bounds = join.getBoundingClientRect()
        f.hover(join)
        assertEquals(bounds, join.getBoundingClientRect())
        f.click(join)
        assertEquals(1, f.joins)
        assertFalse(f.expanded)
        f.click(f.all(".pf-card").first())
        assertTrue(f.expanded)
        f.click(f.all(".pf-member").first())
        assertTrue(f.inspected)
        assertTrue(f.expanded)
        f.click(f.element(".pf-panel .pf-close"))
        assertFalse(f.inspected)
        assertTrue(f.expanded)
    }

    @Test
    fun searchKeepsFocusAndQueueActionsRemainReachable() {
        val f = PartyFinderReviewFixture(width = 360f, queued = true)
        val search = f.element(".pf-search")
        val bounds = search.getBoundingClientRect()
        assertEquals(0, search.style.borderTopColor ushr 24)
        assertNotEquals(f.element(".pf-window").style.backgroundColor, search.style.backgroundColor)
        f.root.document.keyboardModality = true
        search.focus(); f.root.input.charTyped("aster"); f.settle()
        assertTrue(search.style.outlineWidth > 0f)
        assertEquals(bounds, search.getBoundingClientRect())
        assertEquals("aster", f.currentSearch)
        assertEquals(search, f.root.document.focusedElement)
        f.click(f.element(".pf-queue .danger"))
        assertEquals(1, f.removed)
        f.click(f.element(".pf-icon-button"))
        assertEquals(1, f.refreshes)
        val empty = PartyFinderReviewFixture(empty = true, width = 360f)
        val message = empty.element(".pf-message").getBoundingClientRect()
        val list = empty.element(".pf-list").getBoundingClientRect()
        assertTrue(message.x >= list.x && message.right <= list.right && message.bottom <= list.bottom)
    }

    @Test
    fun exportReviewWhenRequested() {
        if (System.getenv("SBO_PF_REVIEW") != "1") return
        requireHeadlessReview()
        val out = PartyFinderReviewFixture.out
        Files.createDirectories(out)
        val measurements = StringBuilder("Synthetic GuiLib fixture; party API/actions inert; head textures/SVG omitted.\n")
        val tiles = mutableMapOf<String, HubReviewRaster.Tile>()
        for ((label, css) in listOf("before" to PartyFinderReviewFixture.beforeCss, "after" to PartyFinderReviewFixture.currentCss)) {
            for (state in listOf("populated", "queued-inspector", "narrow-inspector", "empty")) {
                val f = PartyFinderReviewFixture(css, width = if (state == "narrow-inspector") 360f else 800f,
                    height = if (state == "narrow-inspector") 400f else 480f,
                    empty = state == "empty", queued = state == "queued-inspector", inspector = state.endsWith("inspector"))
                HubReviewRaster.write(f, out.resolve("$label-$state.png"))
                val key = "$label-$state"
                tiles[key] = HubReviewRaster.Tile(key, out.resolve("$key.png"), f.element(".pf-stage").getBoundingClientRect())
                measurements.append("$label/$state " + listOf(".pf-window", ".pf-main", ".pf-list", ".pf-search", ".pf-panel")
                    .mapNotNull { s -> f.all(s).firstOrNull()?.let { s + "=" + it.getBoundingClientRect() } }.joinToString() + "\n")
            }
        }
        for (theme in listOf("light", "minecraft")) {
            val f = PartyFinderReviewFixture(theme = theme, inspector = true, queued = true)
            HubReviewRaster.write(f, out.resolve("after-$theme-inspector.png"))
        }
        Files.writeString(out.resolve("measurements.txt"), measurements)
        val order = listOf("before-populated" to "Before · populated", "after-populated" to "After · populated",
            "after-queued-inspector" to "After · queued + inspector", "after-narrow-inspector" to "After · narrow inspector (existing overlay)")
        HubReviewRaster.partyFinderCheckpoint(out.resolve("partyfinder-checkpoint.png"), order.map { (key, title) -> tiles.getValue(key).copy(label = title) })
    }
}
