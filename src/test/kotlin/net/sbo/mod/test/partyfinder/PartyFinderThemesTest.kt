package net.sbo.mod.test.partyfinder

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sbo.mod.partyfinder.PartyCategories
import net.sbo.mod.partyfinder.api.CategoriesData
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.gui.Piece
import net.sbo.mod.partyfinder.gui.PartyFinderThemes
import net.sbo.mod.partyfinder.gui.StatView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class PartyFinderThemesTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun loadCategories() {
            val body = PartyFinderThemesTest::class.java.getResource("/partyfinder/categories.json")!!.readText()
            PartyCategories.use(PartyFinderApi.parse<CategoriesData>(body, 200).getOrThrow())
        }

        private fun themeFile(name: String, text: String): File =
            Files.createTempDirectory("sbo-themes").resolve(name).toFile().apply { writeText(text) }
    }

    @Test
    fun valuesGetHypixelColors() {
        assertEquals(listOf(Piece("466", "pf-c-red")), StatView.valuePieces("sbLevel", JsonPrimitive(466)))
        assertEquals("pf-c-gold", StatView.numberColor("sbLevel", 400.0))
        assertEquals("pf-c-gray", StatView.numberColor("sbLevel", 39.0))
        assertEquals("pf-c-white", StatView.numberColor("sbLevel", 40.0))
        assertEquals("pf-c-dark-green", StatView.numberColor("sbLevel", 199.0))
        assertEquals(listOf(Piece("Mythic", "pf-c-light-purple")), StatView.valuePieces("griffin", JsonPrimitive("MYTHIC")))
        assertEquals(listOf(Piece("Adept", "pf-trophy-adept")), StatView.valuePieces("trophyFisher", JsonPrimitive(2)))
        assertEquals("pf-c-gold", StatView.numberColor("dianaKills", 250_000.0))
        // Stats without game colors stay plain
        assertNull(StatView.numberColor("magicalPower", 1855.0))

        val def = PartyCategories.target("kuudra", "infernal")!!.req("kuudraArmor")!!
        val need = JsonArray(listOf(buildJsonObject { put("id", "CRIMSON"); put("minTier", "FIERY") }, JsonPrimitive("TERROR")))
        assertEquals(
            listOf(Piece("Crimson Armor", "pf-r-legendary"), Piece(" (Fiery or better)"), Piece(" or "), Piece("Terror Armor", "pf-r-legendary")),
            StatView.needPieces(def, need)
        )
        // The text without colors stays the same
        assertEquals("Crimson Armor (Fiery or better) or Terror Armor", StatView.need(def, need))
    }

    @Test
    fun customThemesKeepOnlyKnownColors() {
        val theme = PartyFinderThemes.load(themeFile("mine.json", """
            {
              "name": "Mine",
              "base": "hypixel",
              "colors": {
                "accent": "#ff8800",
                "--bg": "#101014",
                "ok": "red; background: url(x)",
                "not-a-color": "#ffffff",
                "mc-gold": "#ffcc00aa",
              }
            }
        """.trimIndent()))!!
        assertEquals("custom:mine.json", theme.id)
        assertEquals("Mine", theme.label)
        assertEquals(PartyFinderThemes.DEFAULT, theme.base)
        assertTrue(theme.hypixelColors)
        assertEquals(mapOf("accent" to "#ff8800", "bg" to "#101014", "mc-gold" to "#ffcc00aa"), theme.colors)
        assertFalse(theme.example)
    }

    @Test
    fun brokenOrMinimalFiles() {
        assertNull(PartyFinderThemes.load(themeFile("broken.json", "{ name: ")))
        val minimal = PartyFinderThemes.load(themeFile("plain.json", "{}"))!!
        assertEquals("plain", minimal.label)
        assertEquals(PartyFinderThemes.DEFAULT, minimal.base)
        assertFalse(minimal.hypixelColors)
        val light = PartyFinderThemes.load(themeFile("example.json", """{ "base": "light", "marks": true }"""))!!
        assertTrue(light.base == "light" && light.marks && light.example)
    }

    @Test
    fun bundledExampleIsValid() {
        val text = PartyFinderThemesTest::class.java.getResource("/assets/sbo/ui/partyfinder/themes/example.json")!!.readText()
        val example = PartyFinderThemes.load(themeFile("example.json", text))!!
        assertTrue(example.example)
        // Every color of the example passes the check
        assertEquals(12, example.colors.size)
    }
}
