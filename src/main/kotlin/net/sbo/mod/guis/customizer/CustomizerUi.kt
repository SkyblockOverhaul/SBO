package net.sbo.mod.guis.customizer

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.fabric.text
import net.sbo.mod.general.SboBadges
import net.sbo.mod.utils.data.BadgeEffect
import net.sbo.mod.utils.data.BadgeMotion
import net.sbo.mod.utils.data.BadgePart
import net.sbo.mod.utils.data.PartColor
import java.util.Locale
import java.util.Optional

// ---- Text helpers ----------------------------------------------------------------------------------------

/** § codes as real styles, the gui draws literal text as it is. */
internal fun legacyText(text: String): Component {
    val out = Component.empty()
    var style = Style.EMPTY
    val run = StringBuilder()
    fun flush() {
        if (run.isNotEmpty()) out.append(Component.literal(run.toString()).withStyle(style))
        run.setLength(0)
    }
    var i = 0
    while (i < text.length) {
        val format = if (text[i] == '§' && i + 1 < text.length) ChatFormatting.getByCode(text[i + 1]) else null
        if (format != null) {
            flush()
            style = if (format == ChatFormatting.RESET) Style.EMPTY else style.applyLegacyFormat(format)
            i += 2
            continue
        }
        run.append(text[i])
        i++
    }
    flush()
    return out
}

// ---- Labels and small helpers --------------------------------------------------------------------------

internal val ALL_PARTS = listOf(BadgePart.STAT, BadgePart.TAG, BadgePart.LEVEL, BadgePart.RANK, BadgePart.NAME)

internal val MOTION_LABELS = mapOf(
    BadgeMotion.NONE to "None",
    BadgeMotion.WAVY to "Wavy",
    BadgeMotion.BOUNCE to "Bounce",
    BadgeMotion.SHAKE to "Shake",
    BadgeMotion.GLITCH to "Glitch",
)

internal val MOTION_HINTS = mapOf(
    BadgeMotion.NONE to "The letters stay in place.",
    BadgeMotion.WAVY to "The letters ride a wave. Above heads, and in chat for those who turned chat animations on.",
    BadgeMotion.BOUNCE to "One letter after the other hops.",
    BadgeMotion.SHAKE to "The letters jitter.",
    BadgeMotion.GLITCH to "Single letters flicker scrambled, like §k.",
)

internal val EFFECT_LABELS = mapOf(
    BadgeEffect.STATIC to "None",
    BadgeEffect.WAVE to "Wave",
    BadgeEffect.PULSE to "Pulse",
    BadgeEffect.CHROMA to "Chroma",
    BadgeEffect.SHINE to "Shine",
    BadgeEffect.FLASH to "Flash",
    BadgeEffect.SPARKLE to "Sparkle",
)

internal val EFFECT_HINTS = mapOf(
    BadgeEffect.STATIC to "The colors stand still.",
    BadgeEffect.WAVE to "The gradient flows along the text.",
    BadgeEffect.PULSE to "Fades between both colors, or towards white with one color.",
    BadgeEffect.CHROMA to "A rainbow runs along the text, your colors are not used.",
    BadgeEffect.SHINE to "A white gleam sweeps over the text now and then.",
    BadgeEffect.FLASH to "Jumps between both colors, or to a light version of one color.",
    BadgeEffect.SPARKLE to "Single letters twinkle white.",
)

private const val TAG_BRACKETS = "#AAAAAA"
private const val RAINBOW = "linear-gradient(90deg, #FF5555, #FFFF55, #55FF55, #55FFFF, #5555FF, #FF55FF)"

/** A bracket as it looks in game: its own color, else gray for the team tag and the part's color otherwise. */
internal fun bracketOf(part: String, color: PartColor, close: Boolean): String = (if (close) color.close else color.open)
    ?: if (part == BadgePart.TAG && color.chain != true) TAG_BRACKETS else if (close) color.to ?: color.color else color.color

internal fun hex(color: Int): String = "#%06X".format(Locale.ROOT, color and 0xFFFFFF)

// The picker works with ARGB, badge colors have no alpha
internal fun argb(hex: String): Int = SboBadges.parseHex(hex) or 0xFF000000.toInt()

internal fun background(color: PartColor, part: String): String = when {
    color.effectOf(part) == BadgeEffect.CHROMA -> RAINBOW
    color.to != null -> "linear-gradient(90deg, ${color.color}, ${color.to})"
    else -> color.color
}

internal fun textSize(size: Float): String? = if (size == 1f) null else "font-size: ${"%.2f".format(Locale.ROOT, size)}em"

/**
 * A line from the game; the SBO logo is a char of its own font, which the window's text can't draw, so it shows as the image.
 * Every char is a piece of its own with a fixed key: moving colors would cut the line into other pieces every frame,
 * and the window's font places pieces a little differently, so chars would twitch. The motion fonts that set chars
 * lower in game are left out by the window's text too, so those chars are drawn the same pixels lower here.
 */
internal fun NodeBuilder.game(line: Component) {
    div(className = "cz-game") {
        var index = 0
        line.visit(FormattedText.StyledContentConsumer<Unit> { style, s ->
            for (char in s) {
                if (char.toString() == SboBadges.LOGO) img(LOGO, className = "cz-inline-logo", key = "c${index++}")
                // Spaces never move, and an inline block holding only a space would collapse
                else if (char == ' ') text(Component.literal(" ").withStyle(style), key = "c${index++}")
                else {
                    val drop = SboBadges.dropOf(style)
                    // A transform only moves where it is drawn, the line and its box stay. It needs a box of its own,
                    // so every char is an inline block, also while it doesn't move, and the layout is the same in every frame
                    span(className = "cz-ch", style = if (drop > 0) "transform: translateY(${drop}px)" else null, key = "c${index++}") {
                        text(Component.literal(char.toString()).withStyle(style))
                    }
                }
            }
            Optional.empty()
        }, Style.EMPTY)
    }
}

internal fun NodeBuilder.heading(text: String) {
    div(className = "cz-heading") { +text }
}

/** A heading with one short line under it that says what the settings below change. */
internal fun NodeBuilder.section(title: String, hint: String) {
    heading(title)
    div(className = "cz-hint cz-section-hint") { +hint }
}

/** One setting: its name left (the hint shows on hover), the control right. */
internal fun NodeBuilder.row(label: String, hint: String, control: NodeBuilder.() -> Unit) {
    div(className = "cz-row") {
        span(className = "cz-label", title = hint) { +label }
        div(className = "cz-control") { control() }
    }
}
