package net.sbo.mod.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.math.abs

class SettingsSweepTest {
    private val selected get() = System.getenv("SBO_SETTINGS_CATEGORY") ?: "Medal"
    private fun category() = SettingsSweepData.read(selected)

    @Test
    fun sourceInventoryContainsEveryDeclaredEntryAndGroup() {
        requireHeadlessReview()
        val c = category()
        val expected = mapOf("Medal" to 20, "PartyCommands" to 9, "Customization" to 23, "Themes" to 4, "QOL" to 6, "Debug" to 7)
        assertEquals(expected.getValue(selected), c.entries.size)
        assertTrue(c.entries.all { it.name.isNotBlank() && it.description.isNotBlank() }, c.entries.toString())
        if (selected == "Themes") {
            assertEquals(10, c.entries.first { it.id == "theme" }.options.size)
            assertEquals(7, c.entries.first { it.id == "uiScale" }.options.size)
        }
        println(c.name + ": " + c.entries.groupingBy { it.kind }.eachCount() + "; groups=" + c.groups.map { it.name })
    }

    @Test
    fun currentCategoryControlsRemainContainedAtNormalAndConstrainedSizes() {
        val c = category()
        for (theme in listOf("sbo-dark", "light", "high-contrast", "minecraft")) for (width in listOf(640f, 360f)) {
            val f = SettingsSweepData.fixture(c, width, 400f, theme)
            assertEquals(c.entries.size, f.all(".cfg-row").size)
            assertEquals(c.groups.size, f.all(".cfg-sub").size)
            for (row in f.all(".cfg-row")) {
                val cell = row.querySelector(".cfg-row-control")!!.getBoundingClientRect()
                assertEquals(if (width > 460f) 160f else 76f, cell.width, 0.1f)
                val box = row.getBoundingClientRect()
                assertTrue(cell.x >= box.x && cell.right <= box.right + 0.1f)
                for (selector in listOf(".cfg-select", ".cfg-number", ".guilib-slider-field", ".guilib-color-input", ".cfg-button")) {
                    row.querySelectorAll(selector).forEach { field ->
                        val b = field.getBoundingClientRect()
                        assertTrue(b.x >= cell.x - 0.1 && b.right <= cell.right + 0.1, "$selected/$theme/$width $selector $b in $cell")
                    }
                }
            }
        }
    }

    @Test
    fun categorySpecificStateAndKeyboardChecks() {
        val c = category()
        val f = SettingsSweepData.fixture(c)
        when (selected) {
            "Medal" -> {
                assertEquals(19, f.all(".cfg-row.locked").size)
                f.click(f.element("#row-medalEnabled .guilib-switch"))
                assertTrue(f.all(".cfg-row.locked").isEmpty())
                f.scrollTo("medalCaptureDelayMs")
                f.element("#row-medalCaptureDelayMs .cfg-slider").focus(); f.key("ArrowRight")
                assertEquals("10001", f.currentValues["medalCaptureDelayMs"])
                val off = SettingsSweepData.fixture(c, off = setOf("Medal Clips"))
                assertTrue(off.all(".cfg-row").isEmpty())
                off.click(off.element(".cfg-section-switch"))
                assertEquals(20, off.all(".cfg-row").size)
            }
            "PartyCommands", "QOL", "Debug" -> {
                assertTrue(c.groups.isEmpty())
                val first = c.entries.first()
                val target = f.element("#row-${first.id} .guilib-switch")
                val bounds = target.getBoundingClientRect()
                target.focus(); f.key(" ")
                assertEquals((first.initial != "true").toString(), f.currentValues[first.id])
                assertEquals(bounds, target.getBoundingClientRect())
                if (selected == "Debug") {
                    val light = SettingsSweepData.fixture(c, theme = "light")
                    assertFalse(light.all(".cfg-format-c").isEmpty())
                    for (width in listOf(640f, 360f)) {
                        val debug = SettingsSweepData.fixture(c, width = width)
                        val entry = c.entries.single { it.name == "Show Internal State Waypoints" }
                        debug.scrollTo(entry.id)
                        val row = debug.element("#row-${entry.id}").getBoundingClientRect()
                        val description = debug.element("#row-${entry.id} .cfg-row-description").getBoundingClientRect()
                        assertTrue(description.y >= row.y && description.bottom <= row.bottom,
                            "Debug description $description must fit its row $row")
                        println("Debug description at $width: $description inside $row")
                    }
                }
            }
            "Customization" -> {
                assertEquals(13, c.entries.count { it.kind in listOf("rgb", "rgba") })
                assertTrue("locked" in f.element("#row-waypointOpacity").styleClasses)
                f.scrollTo("dynamicWaypointOpacity")
                f.click(f.element("#row-dynamicWaypointOpacity .guilib-switch"))
                assertFalse("locked" in f.element("#row-waypointOpacity").styleClasses)
                f.scrollTo("waypointTextScale")
                f.element("#row-waypointTextScale .cfg-slider").focus(); f.key("ArrowRight")
                assertEquals(0.72f, f.currentValues.getValue("waypointTextScale").toFloat(), 0.001f)
            }
            "Themes" -> {
                f.open("#row-theme .cfg-select")
                assertEquals(10, f.all(".guilib-option").size)
                f.key("ArrowDown"); f.key("Enter")
                assertEquals("hypixel", f.currentValues["theme"])
                assertTrue(f.all(".guilib-select-menu").isEmpty())
                f.open("#row-uiScale .cfg-select")
                f.key("ArrowDown"); f.key("Enter")
                assertEquals("1", f.currentValues["uiScale"])
            }
        }
    }

    data class View(val name: String, val width: Float = 640f, val height: Float = 480f, val theme: String = "sbo-dark",
        val values: Map<String, String> = emptyMap(), val scroll: String? = null, val off: Set<String> = emptySet(),
        val collapsed: Set<String> = emptySet(), val open: String? = null)

    private fun views(): List<View> = when (selected) {
        "Medal" -> listOf(View("default-normal"), View("default-constrained", 360f, 400f),
            View("enabled-sliders-normal", values = mapOf("medalEnabled" to "true"), scroll = "medalClipDurationSeconds"),
            View("enabled-sliders-light", 360f, 400f, "light", mapOf("medalEnabled" to "true"), "medalClipDurationSeconds"),
            View("off-constrained", 360f, 400f, "high-contrast", off = setOf("Medal Clips")),
            View("collapsed-normal", collapsed = setOf("Medal Clips")))
        "PartyCommands" -> listOf(View("normal"), View("constrained", 360f, 400f),
            View("long-command-light", 360f, 400f, "light", scroll = "transferCommand"),
            View("bottom-minecraft", 360f, 400f, "minecraft", scroll = "dianaPartyCommands"))
        "Customization" -> listOf(View("normal"), View("constrained", 360f, 400f),
            View("glow-alpha", 360f, 400f, "high-contrast", scroll = "KingMinosGlowColor", open = "#row-KingMinosGlowColor .guilib-color-input"),
            View("waypoint-dependencies", 360f, 400f, "light", scroll = "dynamicWaypointOpacity"),
            View("numeric-minecraft", theme = "minecraft", scroll = "waypointTextScale"),
            View("sounds-bottom", 360f, 400f, scroll = "group-Sounds"))
        "Themes" -> listOf(View("normal"), View("constrained", 360f, 400f),
            View("theme-menu-light", 360f, 400f, "light", open = "#row-theme .cfg-select"),
            View("size-menu-minecraft", 360f, 400f, "minecraft", open = "#row-uiScale .cfg-select"))
        "QOL" -> listOf(View("normal"), View("constrained", 360f, 400f), View("light", 360f, 400f, "light"))
        "Debug" -> listOf(View("normal"), View("constrained", 360f, 400f),
            View("warning-light", 360f, 400f, "light"), View("bottom-high-contrast", 360f, 400f, "high-contrast", scroll = "separateAlphaData"))
        else -> error(selected)
    }

    @Test
    fun exportSelectedCategoryWhenRequested() {
        if (System.getenv("SBO_SETTINGS_SWEEP") != "1") return
        requireHeadlessReview()
        val c = category()
        val version = Regex("versions/([^/]+)/").find(javaClass.protectionDomain.codeSource.location.path)?.groupValues?.get(1) ?: "current"
        val out = ReviewResources.project.resolve("build/ui-review/settings-sweep/$version/${c.id}")
        val measurements = StringBuilder("Source-derived ${c.name}; isolated disposable values, inert actions. Synthetic GuiLib renders, not native screenshots.\n")
        for (view in views()) {
            val f = SettingsSweepData.fixture(c, view.width, view.height, view.theme, view.values, view.collapsed, view.off)
            view.scroll?.let { f.scrollTo(it) }
            view.open?.let { f.open(it) }
            HubReviewRaster.write(f, out.resolve(view.name + ".png"))
            measurements.append(view.name + " rows=" + f.all(".cfg-row").size + " body=" + f.element(".cfg-body").getBoundingClientRect() +
                " fields=" + f.all(".cfg-row-control").take(3).map { it.getBoundingClientRect() } + "\n")
        }
        Files.writeString(out.resolve("measurements.txt"), measurements)
        Files.writeString(out.resolve("inventory.txt"), c.name + "\n" + c.description + "\n" + c.entries.joinToString("\n") { it.toString() })
        if (System.getenv("SBO_SETTINGS_CHECKPOINT") == "1") {
            HubReviewRaster.settingsCheckpoint(ReviewResources.project.resolve("build/ui-review/settings-checkpoint.png"), version)
        }
    }
}
