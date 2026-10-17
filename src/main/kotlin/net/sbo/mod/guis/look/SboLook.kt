package net.sbo.mod.guis.look

import net.sbo.guilib.core.dsl.ComponentScope
import net.sbo.mod.settings.Settings
import net.sbo.mod.settings.categories.Themes
import net.sbo.mod.utils.data.DataManager

/** Window sizes. A window's own size is null for the global one from the settings, [AUTO] for Minecraft's GUI scale. */
object UiScale {
    const val AUTO = 0f

    val GLOBAL_CHOICES: List<Float> = listOf(AUTO, 1f, 1.5f, 2f, 2.5f, 3f, 4f)

    val OWN_CHOICES: List<Float?> = listOf(null) + GLOBAL_CHOICES

    val global: Float get() = parse(Themes.uiScale)?.takeIf { it in GLOBAL_CHOICES } ?: AUTO

    fun id(scale: Float?): String = when {
        scale == null -> "global"
        scale == AUTO -> "auto"
        scale % 1f == 0f -> scale.toInt().toString()
        else -> scale.toString()
    }

    fun parse(id: String): Float? = when (id) {
        "global" -> null
        "auto" -> AUTO
        else -> id.toFloatOrNull()?.takeIf { it in GLOBAL_CHOICES }
    }

    fun label(scale: Float?): String = when (scale) {
        null -> "Global (${label(global)})"
        AUTO -> "Auto"
        else -> id(scale)
    }

    fun own(stored: Float?): Float? = stored?.takeIf { it in GLOBAL_CHOICES }

    /** The scale GuiLib gets, null = Minecraft's GUI scale. */
    fun resolve(own: Float?): Float? = (own(own) ?: global).takeIf { it != AUTO }
}

fun ComponentScope.useSboScale(own: Float? = null) = useScreenScale(UiScale.resolve(own))

/**
 * Puts the theme on the body, so modals, tooltips and toasts follow it too, and applies the background blur setting.
 * [ownId] is a theme picked only for this window, null uses the global one. Needs `sbo:ui/themes/themes.css` in the
 * window's styles.
 */
fun ComponentScope.useSboTheme(ownId: String? = null): SboThemes.Theme {
    val id = ownId ?: Themes.theme
    val theme = useMemo(id) { SboThemes.find(id) }
    useBackgroundBlur(Themes.backgroundBlur)
    SboThemes.BASES.forEach { base -> useBodyClass("sbo-theme-$base", theme.base == base) }
    val document = useDocument()
    useEffect(theme) {
        val body = document.body
        // Kept here, the cleanup would read the next theme from the state
        val colors = theme.colors
        colors.forEach { (name, value) -> body.setStyleProperty("--$name", value) }
        onCleanup { colors.keys.forEach { body.removeStyleProperty("--$it") } }
    }
    return theme
}

object SboLook {
    const val STYLE = "sbo:ui/themes/themes.css"

    /** Before the global theme existed only the party finder had one; it becomes the global theme once. */
    fun init() {
        if (Themes.inFile) return
        val config = DataManager.partyFinderConfigState
        val old = config.theme ?: return
        if (old != SboThemes.DEFAULT && SboThemes.find(old).id == old) {
            Themes.theme = old
            Settings.save()
        }
        config.theme = null
        config.save()
    }
}
