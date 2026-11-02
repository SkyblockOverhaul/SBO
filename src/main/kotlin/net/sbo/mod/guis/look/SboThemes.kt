package net.sbo.mod.guis.look

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Util
import org.slf4j.LoggerFactory
import java.io.File
import java.security.MessageDigest

/**
 * Looks of all SBO windows: built-in themes plus custom ones from `config/sbo/themes` (a .json with colors or a .css
 * file), and `user.css` there on top of every theme.
 */
object SboThemes {
    const val DEFAULT = "sbo-dark"
    private const val CUSTOM_PREFIX = "custom:"
    private const val EXAMPLE_FILE = "example.json"
    private const val EXAMPLE_CSS = "example.css"
    private const val USER_CSS = "user.css"
    // Bigger CSS files are not read
    private const val MAX_CSS_BYTES = 256 * 1024
    private const val RESOURCES = "/assets/sbo/ui/themes"
    // SHA-256 of the first example.json (Forest) without "\r"
    private const val OLD_EXAMPLE_SHA256 = "3bfceb75dd1d977ca3be889b135234a0f36903abc3d065e802b0a823135fd53b"

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
        // CSS file added after SBO's own CSS in every window
        val css: File? = null,
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

    // The comment at the top of a .css theme
    private val CSS_HEADER = Regex("^\\s*/\\*(.*?)\\*/", RegexOption.DOT_MATCHES_ALL)

    // Own logger, SBOKotlin.logger would start mod code in tests
    private val logger = LoggerFactory.getLogger("SBO")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; allowTrailingComma = true; allowComments = true }

    val folder: File get() = FabricLoader.getInstance().configDir.resolve("sbo/themes").toFile()

    @Serializable
    private data class ThemeFile(
        val name: String? = null,
        val description: String? = null,
        val base: String = DEFAULT,
        val hypixelColors: Boolean? = null,
        val marks: Boolean? = null,
        val colors: Map<String, String> = emptyMap(),
        val css: String? = null
    )

    /** CSS on top of every theme in all SBO windows. */
    val userCss: File get() = File(folder, USER_CSS)

    /** Built-in themes first, then the files in the theme folder by name, the examples last. */
    fun all(): List<Theme> {
        prepareFolder()
        val custom = folder.listFiles { file -> isThemeFile(file) }
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
            if (isThemeFile(file)) load(file)?.let { return it }
        }
        return BUILT_IN.first()
    }

    fun openFolder() {
        prepareFolder()
        Util.getPlatform().openFile(folder)
    }

    private val File.isCss get() = extension.equals("css", ignoreCase = true)

    // A .json or .css file directly in the folder; user.css is not a theme of its own
    private fun isThemeFile(file: File): Boolean =
        file.isFile && file.parentFile?.canonicalFile == folder.canonicalFile && !file.name.equals(USER_CSS, ignoreCase = true) &&
            (file.isCss || file.extension.equals("json", ignoreCase = true))

    internal fun load(file: File): Theme? = try {
        val text = file.readText()
        val data = if (file.isCss) cssHeader(text) else json.decodeFromString<ThemeFile>(text)
        val css = if (file.isCss) file else data.css?.let { cssFile(file, it.trim()) }
        val example = file.name.equals(EXAMPLE_FILE, ignoreCase = true) || file.name.equals(EXAMPLE_CSS, ignoreCase = true)
        // A custom theme may start from any built-in one, also from Hypixel Colors
        val base = BUILT_IN.firstOrNull { it.id == data.base } ?: BUILT_IN.first()
        val colors = data.colors
            .mapKeys { it.key.trim().removePrefix("--") }
            .filter { (name, value) -> name in COLOR_NAMES && COLOR_VALUE.matches(value.trim()) }
            .mapValues { it.value.trim() }
        Theme(
            id = CUSTOM_PREFIX + file.name,
            label = data.name?.takeIf { it.isNotBlank() }?.take(32) ?: file.nameWithoutExtension,
            description = data.description?.takeIf { it.isNotBlank() }?.take(200)
                ?: if (example) "An example theme. Open the theme folder, copy this file and change it to make your own."
                else "Your own theme from the theme folder (${file.name}).",
            base = base.base,
            hypixelColors = data.hypixelColors ?: base.hypixelColors,
            marks = data.marks ?: base.marks,
            colors = colors,
            css = css,
            example = example
        )
    } catch (e: Exception) {
        logger.warn("[SBO] Theme ${file.name} not loaded: ${e.message}")
        null
    }

    // "css" of a .json theme: a .css file next to it, nothing outside the theme folder
    private fun cssFile(theme: File, name: String): File? {
        val dir = theme.parentFile
        val file = File(dir, name)
        if (file.parentFile?.canonicalFile != dir?.canonicalFile || !file.isCss) {
            logger.warn("[SBO] Theme ${theme.name}: \"css\" must be the name of a .css file in the theme folder")
            return null
        }
        return file
    }

    /**
     * Settings of a .css theme from the comment at the top of the file, one `key: value` per line:
     * name, description, base, hypixelColors, marks.
     */
    private fun cssHeader(text: String): ThemeFile {
        val comment = CSS_HEADER.find(text)?.groupValues?.get(1).orEmpty()
        val fields = comment.lines()
            .map { it.trim().removePrefix("*").trim() }
            .mapNotNull { line ->
                val colon = line.indexOf(':').takeIf { it > 0 } ?: return@mapNotNull null
                line.substring(0, colon).trim().lowercase() to line.substring(colon + 1).trim()
            }
            .toMap()
        return ThemeFile(
            name = fields["name"],
            description = fields["description"],
            base = fields["base"] ?: DEFAULT,
            hypixelColors = fields["hypixelcolors"]?.toBooleanStrictOrNull(),
            marks = fields["marks"]?.toBooleanStrictOrNull()
        )
    }

    /** Text of a CSS file of the theme folder, null when it is missing, too big or can't be read. */
    fun readCss(file: File?): String? {
        if (file == null || !file.isFile) return null
        if (file.length() > MAX_CSS_BYTES) {
            logger.warn("[SBO] Theme CSS ${file.name} is bigger than ${MAX_CSS_BYTES / 1024} KB and not loaded")
            return null
        }
        return try {
            file.readText()
        } catch (e: Exception) {
            logger.warn("[SBO] Theme CSS ${file.name} not read: ${e.message}")
            null
        }
    }

    // The README is always rewritten so it lists the current colors, the example only with the folder or over the untouched old one,
    // example.css once: with the folder or when the README first talks about CSS
    private fun prepareFolder() {
        try {
            val created = !folder.exists()
            folder.mkdirs()
            val readme = File(folder, "README.txt")
            val cssIsNew = !readme.isFile || !readme.readText().contains(USER_CSS)
            copyResource("README.txt", readme)
            val example = File(folder, EXAMPLE_FILE)
            if (created || isOldExample(example)) copyResource(EXAMPLE_FILE, example)
            val exampleCss = File(folder, EXAMPLE_CSS)
            if (cssIsNew && !exampleCss.exists()) copyResource(EXAMPLE_CSS, exampleCss)
            writeReference()
        } catch (e: Exception) {
            logger.warn("[SBO] Theme folder not prepared: ${e.message}")
        }
    }

    // SBO's own CSS to look up classes, by file name in the reference folder; GuiLib's ua.css styles the plain elements
    private val REFERENCE = mapOf(
        "themes.css" to "/assets/sbo/ui/themes/themes.css",
        "hub.css" to "/assets/sbo/ui/hub/hub.css",
        "settings.css" to "/assets/sbo/ui/config/config.css",
        "partyfinder.css" to "/assets/sbo/ui/partyfinder/partyfinder.css",
        "profileviewer.css" to "/assets/sbo/ui/profileviewer/profileviewer.css",
        "events.css" to "/assets/sbo/ui/events/events.css",
        "achievements.css" to "/assets/sbo/ui/achievements/achievements.css",
        "sounds.css" to "/assets/sbo/ui/sounds/sounds.css",
        "cloud.css" to "/assets/sbo/ui/cloud/cloud.css",
        "badge.css" to "/assets/sbo/ui/badge/badge.css",
        "elements.css" to "/assets/guilib/css/ua.css"
    )

    /**
     * Copies of SBO's CSS in `reference/`, rewritten so they match the installed version. The folder is never loaded:
     * only files directly in the theme folder are themes.
     */
    private fun writeReference() {
        val dir = File(folder, "reference")
        dir.mkdirs()
        copyResource("reference-README.txt", File(dir, "README.txt"))
        for ((name, resource) in REFERENCE) {
            val css = SboThemes::class.java.getResourceAsStream(resource)?.use { it.readBytes() } ?: continue
            val header = "/* REFERENCE ONLY, NOT LOADED. A copy of SBO's own CSS ($resource), rewritten by SBO so changes here are lost.\n" +
                "   Copy the rules you want to change into user.css or your own .css theme one folder up. See README.txt here. */\n\n"
            val text = header.toByteArray() + css
            val target = File(dir, name)
            if (!target.exists() || !target.readBytes().contentEquals(text)) target.writeBytes(text)
        }
    }

    internal fun isOldExample(file: File): Boolean {
        if (!file.isFile) return false
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readText().replace("\r", "").toByteArray())
        return digest.joinToString("") { "%02x".format(it) } == OLD_EXAMPLE_SHA256
    }

    private fun copyResource(name: String, target: File) {
        val text = SboThemes::class.java.getResourceAsStream("$RESOURCES/$name")?.use { it.readBytes() } ?: return
        if (!target.exists() || !target.readBytes().contentEquals(text)) target.writeBytes(text)
    }
}
