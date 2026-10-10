package net.sbo.mod.test

import net.sbo.mod.guis.look.SboThemes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SboThemesTest {
    @TempDir
    lateinit var dir: File

    private val oldExample = """
        {
          "name": "Example (Forest)",
          "description": "An example theme. Open the theme folder, copy this file and change the colors to make your own.",
          "base": "sbo-dark",
          "hypixelColors": true,
          "marks": false,
          "colors": {
            "bg": "#121a15",
            "panel": "#18231c",
            "panel-2": "#203026",
            "card": "#16201a",
            "card-hover": "#1c2a21",
            "border": "#2c4033",
            "border-hover": "#466350",
            "title": "#9be7b0",
            "accent": "#2f9e5b",
            "accent-hover": "#3cb86c",
            "accent-soft": "#2f9e5b4d",
            "accent-faint": "#2f9e5b2e",
            "banner-bg": "#2f9e5b24",
            "banner-border": "#2f9e5b80",
            "banner-text": "#c8f2d4",
            "input-bg": "#0f1612",
            "input-border": "#335040",
            "input-border-hover": "#4c7059",
            "control": "#223328",
            "control-border": "#3a5545",
            "control-hover": "#2b4033",
            "control-active": "#1a271f",
            "menu": "#18231c",
            "popup": "#0c120e",
            "highlight": "#2b4033",
            "type-diana": "#ffd166"
          }
        }
    """.trimIndent() + "\n"

    private fun exampleFile(): File {
        val text = SboThemes::class.java.getResourceAsStream("/assets/sbo/ui/themes/example.json")!!.use { it.readBytes() }
        return File(dir, "example.json").apply { writeBytes(text) }
    }

    @Test
    fun exampleSetsEverything() {
        val theme = SboThemes.load(exampleFile())
        assertNotNull(theme)
        theme!!
        assertEquals("Example (Purple Minecraft)", theme.label)
        assertEquals("minecraft", theme.base)
        assertTrue(theme.hypixelColors)
        assertTrue(theme.example)
        assertEquals(SboThemes.COLOR_NAMES, theme.colors.keys)
    }

    @Test
    fun onlyTheUntouchedOldExampleIsReplaced() {
        val file = File(dir, "example.json")
        file.writeText(oldExample.replace("\n", "\r\n"))
        assertTrue(SboThemes.isOldExample(file))
        file.writeText(oldExample.replace("#121a15", "#000000"))
        assertFalse(SboThemes.isOldExample(file))
        assertFalse(SboThemes.isOldExample(exampleFile()))
        assertFalse(SboThemes.isOldExample(File(dir, "missing.json")))
    }
}
