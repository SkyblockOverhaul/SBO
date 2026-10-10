package net.sbo.mod.guis.look

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Util
import org.slf4j.LoggerFactory
import java.io.File

/** Looks of all SBO windows: built-in themes plus custom ones from `config/sbo/themes`. */
object SboThemes {
    const val DEFAULT = "sbo-dark"
    private const val CUSTOM_PREFIX = "custom:"
    private const val EXAMPLE_FILE = "example.json"
    private const val RESOURCES = "/assets/sbo/ui/themes"

    data class Theme(
        val id: String,
        val label: String,
        val description: String,
        // Built-in color set the theme starts from, see themes.css
        val base: String,
        val hypixelColors: Boolean = false,
        // Requirements a player doesn't meet get a dashed border, or a "×" where there is no border
        val marks: Boolean = false,
        // Own colors of a custom theme, variable name without "--" to value
        val colors: Map<String, String> = emptyMap(),
        val example: Boolean = false
    )

    val BUILT_IN = listOf(
        Theme(DEFAULT, "SBO Dark", "The standard SBO look: dark gray with SBO blue.", DEFAULT),
        Theme(
            "hypixel", "Hypixel Colors",
            "Like SBO Dark, but values use the colors you know from Hypixel: item rarities, SkyBlock level colors, Trophy Fisher titles and one color per party type.",
            DEFAULT, hypixelColors = true
        ),
        Theme(
            "colorblind", "Colorblind Friendly",
            "Blue and orange instead of green and red, made for red-green color blindness. Requirements you don't meet also get a dashed border.",
            "colorblind", marks = true
        ),
        Theme("high-contrast", "High Contrast", "Black background, white text and strong borders. Easier to read.", "high-contrast"),
        Theme("light", "Light", "Bright background with dark text. (pain)", "light"),
        Theme("midnight", "Midnight", "Almost black with softer colors, nice in a dark room.", "midnight"),
        Theme("purple", "Purple", "Dark purple everywhere, also in dialogs, menus and fields, with a violet accent.", "purple"),
        Theme(
            "mythological", "Mythological",
            "Warm dark brown with Griffin gold, made for Diana. Comes with Hypixel colors.",
            "mythological", hypixelColors = true
        ),
        Theme("ocean", "Deep Ocean", "Deep navy blue with a calm teal accent.", "ocean"),
        Theme(
            "minecraft", "Minecraft",
            "Looks like the game's own menus: dirt background, stone buttons, black text fields, purple tooltips, square corners and always the Minecraft font with its shadow. Comes with Hypixel colors.",
            "minecraft", hypixelColors = true
        )
    )

    val BASES: List<String> = BUILT_IN.map { it.base }.distinct()

    /** Variables a custom theme may set; the README lists them with their meaning. */
    val COLOR_NAMES: Set<String> = setOf(
        "bg", "panel", "panel-2", "card", "card-hover", "border", "border-hover",
        "text", "text-strong", "text-soft", "subtle", "muted", "dim", "title",
        "accent", "accent-hover", "accent-text", "accent-soft", "accent-faint",
        "banner-bg", "banner-border", "banner-text",
        "ok", "ok-soft", "ok-border", "bad", "bad-border", "danger", "estimated",
        "input-bg", "input-border", "input-border-hover", "control", "control-border", "control-hover", "control-active",
        "menu", "popup", "highlight", "disabled", "disabled-text", "backdrop", "shadow", "scrollbar-thumb", "scrollbar-track",
        "toast-success", "toast-warning", "toast-error", "menu-danger"
    ) + listOf(
        "black", "dark-blue", "dark-green", "dark-aqua", "dark-red", "dark-purple", "gold", "gray",
        "dark-gray", "blue", "green", "aqua", "red", "light-purple", "yellow", "white"
    ).map { "mc-$it" } + listOf(
        "diana", "fishing", "mining", "kuudra", "bestiary", "rift", "safari", "slayer", "custom"
    ).map { "type-$it" } + listOf("novice", "adept", "expert", "master").map { "trophy-$it" } + listOf("tint", "tint-strong", "star", "glow", "glow-soft")

    // #rgb, #rgba, #rrggbb or #rrggbbaa, nothing else reaches the style
    private val COLOR_VALUE = Regex("^#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

    // Own logger, SBOKotlin.logger would start mod code in tests
    private val logger = LoggerFactory.getLogger("SBO")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; allowTrailingComma = true }

    val folder: File get() = FabricLoader.getInstance().configDir.resolve("sbo/themes").toFile()

    @Serializable
    private data class ThemeFile(
        val name: String? = null,
        val description: String? = null,
        val base: String = DEFAULT,
        val hypixelColors: Boolean? = null,
        val marks: Boolean? = null,
        val colors: Map<String, String> = emptyMap()
    )

    /** Built-in themes first, then the files in the theme folder by name, the example last. */
    fun all(): List<Theme> {
        prepareFolder()
        val custom = folder.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }
            .orEmpty()
            .mapNotNull(::load)
            .sortedWith(compareBy<Theme> { it.example }.thenBy { it.label.lowercase() })
        return BUILT_IN + custom
    }

    /** The theme with this id; an unknown or deleted one falls back to SBO Dark. */
    fun find(id: String?): Theme {
        BUILT_IN.firstOrNull { it.id == id }?.let { return it }
        if (id != null && id.startsWith(CUSTOM_PREFIX)) {
            val file = File(folder, id.removePrefix(CUSTOM_PREFIX))
            if (file.isFile) load(file)?.let { return it }
        }
        return BUILT_IN.first()
    }

    fun openFolder() {
        prepareFolder()
        Util.getPlatform().openFile(folder)
    }

    internal fun load(file: File): Theme? = try {
        val data = json.decodeFromString<ThemeFile>(file.readText())
        // A custom theme may start from any built-in one, also from Hypixel Colors
        val base = BUILT_IN.firstOrNull { it.id == data.base } ?: BUILT_IN.first()
        val colors = data.colors
            .mapKeys { it.key.trim().removePrefix("--") }
            .filter { (name, value) -> name in COLOR_NAMES && COLOR_VALUE.matches(value.trim()) }
            .mapValues { it.value.trim() }
        val example = file.name.equals(EXAMPLE_FILE, ignoreCase = true)
        Theme(
            id = CUSTOM_PREFIX + file.name,
            label = data.name?.takeIf { it.isNotBlank() }?.take(32) ?: file.nameWithoutExtension,
            description = data.description?.takeIf { it.isNotBlank() }?.take(200)
                ?: if (example) "An example theme. Open the theme folder, copy this file and change the colors to make your own."
                else "Your own theme from the theme folder (${file.name}).",
            base = base.base,
            hypixelColors = data.hypixelColors ?: base.hypixelColors,
            marks = data.marks ?: base.marks,
            colors = colors,
            example = example
        )
    } catch (e: Exception) {
        logger.warn("[SBO] Theme ${file.name} not loaded: ${e.message}")
        null
    }

    // The README is always rewritten so it lists the current colors, the example only once with the folder
    private fun prepareFolder() {
        try {
            val created = !folder.exists()
            folder.mkdirs()
            copyResource("README.txt", File(folder, "README.txt"))
            if (created) copyResource(EXAMPLE_FILE, File(folder, EXAMPLE_FILE))
        } catch (e: Exception) {
            logger.warn("[SBO] Theme folder not prepared: ${e.message}")
        }
    }

    private fun copyResource(name: String, target: File) {
        val text = SboThemes::class.java.getResourceAsStream("$RESOURCES/$name")?.use { it.readBytes() } ?: return
        if (!target.exists() || !target.readBytes().contentEquals(text)) target.writeBytes(text)
    }
}
