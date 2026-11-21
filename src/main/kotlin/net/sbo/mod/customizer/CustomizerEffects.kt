package net.sbo.mod.customizer

import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import kotlin.math.PI
import kotlin.math.cos

/** Customizer colors and motion over time: effects, chains of parts and the chars moving through the shift fonts. */
object CustomizerEffects {
    private const val WAVE_MS = 3000L
    private const val PULSE_MS = 2000L
    private const val CHROMA_MS = 4000L
    /** Share of the rainbow one text spans, so short names still show several colors. */
    private const val CHROMA_SPREAD = 0.6f
    /** A pulse without a second color fades this far towards white. */
    private const val PULSE_WHITE = 0.6f
    /** Shine: a gleam [SHINE_WIDTH] wide (share of the text) crosses in the first 1 / [SHINE_REST] of the period. */
    private const val SHINE_MS = 3000L
    private const val SHINE_REST = 2.5f
    private const val SHINE_WIDTH = 0.25f
    private const val SHINE_STRENGTH = 0.75f
    private const val FLASH_MS = 1000L
    /** Sparkle: every slot about one char in [SPARKLE_ONE_IN] twinkles. */
    private const val SPARKLE_MS = 150L
    private const val SPARKLE_ONE_IN = 7

    private const val WAVY_MS = 1500L
    /** Phase between two chars of the wavy motion. */
    private const val WAVY_STEP = 0.12f
    private const val BOUNCE_MS = 2200L
    /** Chars of rest after the hop ran over the text. */
    private const val BOUNCE_REST = 4
    private const val SHAKE_MS = 90L
    private const val GLITCH_MS = 80L
    /** Glitch: about one char in [GLITCH_SPREAD] lines scrambles per slot. */
    private const val GLITCH_SPREAD = 1
    /** The default font 1 to 3 pixels lower (assets/sbo/font/shift*.json), fonts cannot rise above the line. */
    private val SHIFT_FONTS = (1..3).map { FontDescription.Resource(Identifier.fromNamespaceAndPath("sbo", "shift$it")) }
    /**
     * The customizer symbols (BADGE_SYMBOLS on the backend) the shift fonts also draw lower; the ones vanilla takes from
     * unifont come from sbo:font/symbols.png there, so they move with the chars around them.
     */
    private const val MOVING_SYMBOLS = "✿❀☁❤♥★☆✦✧⚡☀☾♪♫❄☘♛♚⚔☠✪✔✌☕⚓✈∞☯♣♠♦☂"

    /** Pixels a char with this style sits lower through the motion fonts, 0 for every other style. */
    fun dropOf(style: Style): Int = SHIFT_FONTS.indexOf(style.font) + 1

    /** A colored part of a line and its chars. */
    class Segment(val part: String, val color: PartColor, val range: IntRange)

    /** The colors and motion of a part, counted over the chars of its whole chain. */
    class Paint(val color: PartColor, val part: String, private val motion: String?, val range: IntRange, private val colorRange: IntRange) {
        private val length get() = range.last - range.first + 1
        fun at(i: Int, animate: Boolean, now: Long): Int =
            colorAt(color, part, i - colorRange.first, colorRange.last - colorRange.first + 1, animate, now)

        /**
         * A bracket or plus with its own color [hex] moves along with the part instead of standing still. A wave is
         * the gradient itself moving, so there it takes the wave's color at its place to stay in step with the chars
         * next to it; the other effects run from [hex].
         */
        fun at(i: Int, hex: String, animate: Boolean, now: Long): Int {
            if (!animate || !colorMoves(color, part)) return parseHex(hex)
            if (color.effectOf(part) == CustomizerEffect.WAVE) return at(i, animate, now)
            val own = color.copy(color = hex)
            return colorAt(own, part, i - colorRange.first, colorRange.last - colorRange.first + 1, animate, now)
        }
        fun moved(style: Style, i: Int, char: Char, animate: Boolean, now: Long): Style =
            moved(style, motion, i - range.first, length, char, animate, now)
        fun dropAt(i: Int, now: Long): Int = dropAt(motion, i - range.first, length, now)
        fun glitchAt(i: Int, now: Long): Boolean = glitchAt(motion, i - range.first, length, now)
    }

    /**
     * Paints of [segments] in text order: a part with `chain` flows on from the one before it while [touching] says
     * the two stand right next to each other. Every part keeps its own colors, effect and motion; a chain only counts
     * their chars over all of its parts, so a wave on level and name runs on as one wave instead of starting again.
     * Colors that stand still keep their gradient over the part itself.
     */
    fun paints(segments: List<Segment>, touching: (Segment, Segment) -> Boolean): List<Paint> {
        val starts = IntArray(segments.size)
        for (k in segments.indices) {
            val joins = k > 0 && segments[k].color.chain == true && touching(segments[k - 1], segments[k])
            starts[k] = if (joins) starts[k - 1] else k
        }
        return segments.indices.map { k ->
            val chain = segments.indices.filter { starts[it] == starts[k] }
            val own = segments[k]
            val range = segments[chain.first()].range.first..segments[chain.last()].range.last
            Paint(own.color, own.part, own.color.motion, range, if (colorMoves(own.color, own.part)) range else own.range)
        }
    }

    /** Whether the part's colors or chars change over time. */
    fun moves(color: PartColor, part: String): Boolean = colorMoves(color, part) || !color.motion.isStill()

    /** Whether the part changes over time at [place]; chars only move above heads, the tab list and chat only change colors. */
    internal fun movesAt(color: PartColor, part: String, place: String): Boolean =
        if (place == TeamTagPlace.HEAD) moves(color, part) else colorMoves(color, part)

    /** Whether the part changes over time at [place] for the viewer: its colors only while [colors] allows them, its chars always. */
    internal fun changesAt(color: PartColor, part: String, place: String, colors: Boolean): Boolean =
        colors && colorMoves(color, part) || place == TeamTagPlace.HEAD && !color.motion.isStill()

    private fun colorMoves(color: PartColor, part: String): Boolean = when (color.effectOf(part)) {
        CustomizerEffect.WAVE -> color.to != null
        CustomizerEffect.STATIC -> false
        else -> true
    }

    internal fun String?.isStill(): Boolean = this == null || this == CustomizerMotion.NONE

    /**
     * Color of char [i] of a text [length] chars long; with [animate] false moving effects stand still:
     * they show the plain gradient, chroma a fixed rainbow. The wave runs there and back again, so the moving gradient has
     * no seam; chroma runs from the end of the text to its start like the wave; shine's gleam crosses in the first part of
     * the period, then the text rests.
     */
    fun colorAt(color: PartColor, part: String, i: Int, length: Int, animate: Boolean, now: Long = System.currentTimeMillis()): Int {
        val from = parseHex(color.color)
        val to = color.to?.let(::parseHex)
        val t = i / (length - 1).coerceAtLeast(1).toFloat()
        val still = to?.let { mix(from, it, t) } ?: from
        if (!animate) {
            return if (color.effectOf(part) == CustomizerEffect.CHROMA) hue(i / length.toFloat() * CHROMA_SPREAD) else still
        }
        return when (color.effectOf(part)) {
            CustomizerEffect.WAVE -> {
                if (to == null) return from
                val pos = (i / length.toFloat() + phase(now, WAVE_MS)) % 1f
                mix(from, to, if (pos < 0.5f) pos * 2f else (1f - pos) * 2f)
            }
            CustomizerEffect.PULSE -> {
                val fade = (1f - cos(phase(now, PULSE_MS) * 2f * PI.toFloat())) / 2f
                mix(from, to ?: mix(from, 0xFFFFFF, PULSE_WHITE), fade)
            }
            CustomizerEffect.CHROMA -> hue((i / length.toFloat() * CHROMA_SPREAD + 1f - phase(now, CHROMA_MS)) % 1f)
            CustomizerEffect.SHINE -> {
                val center = phase(now, SHINE_MS) * SHINE_REST - SHINE_WIDTH
                val distance = kotlin.math.abs((i + 0.5f) / length - center)
                if (distance >= SHINE_WIDTH) still else mix(still, 0xFFFFFF, (1f - distance / SHINE_WIDTH) * SHINE_STRENGTH)
            }
            CustomizerEffect.FLASH -> if (phase(now, FLASH_MS) < 0.5f) still else to?.let { mix(it, from, t) } ?: mix(from, 0xFFFFFF, PULSE_WHITE)
            CustomizerEffect.SPARKLE -> if (noise(i, now / SPARKLE_MS) % SPARKLE_ONE_IN == 0) mix(still, 0xFFFFFF, SHINE_STRENGTH) else still
            else -> still
        }
    }

    /** The style with the char moved by [motion]; only plain chars move, the shifted fonts only have those. */
    internal fun moved(style: Style, motion: String?, i: Int, length: Int, char: Char, animate: Boolean, now: Long): Style {
        if (!animate || motion.isStill() || char !in ' '..'~' && char !in MOVING_SYMBOLS) return style
        if (glitchAt(motion, i, length, now)) return style.withObfuscated(true)
        val drop = dropAt(motion, i, length, now)
        return if (drop > 0) style.withFont(SHIFT_FONTS[drop - 1]) else style
    }

    /**
     * How many pixels char [i] sits lower, 0 to [SHIFT_FONTS] size. Fonts only move whole pixels and only down,
     * so every motion uses all steps and smooth curves to look as even as it can. A bounce is one hop over the text, then a short rest.
     */
    fun dropAt(motion: String?, i: Int, length: Int, now: Long): Int = when (motion) {
        CustomizerMotion.WAVY -> depth(swing(phase(now, WAVY_MS) - i * WAVY_STEP))
        CustomizerMotion.BOUNCE -> {
            val hop = phase(now, BOUNCE_MS) * (length + BOUNCE_REST) - i
            if (hop in 0f..1f) depth(kotlin.math.sin(hop * PI.toFloat())) else 0
        }
        CustomizerMotion.SHAKE -> if (noise(i, now / SHAKE_MS) % 3 == 0) 1 else 0
        else -> 0
    }

    /** 0 to 1 and back over one turn of [turns], eased at both ends. */
    private fun swing(turns: Float): Float = (1f - cos(turns * 2f * PI.toFloat())) / 2f

    /** Pixels for a depth from 0 to 1. */
    private fun depth(t: Float): Int = kotlin.math.round(t.coerceIn(0f, 1f) * SHIFT_FONTS.size).toInt()

    /** Whether char [i] shows scrambled right now. */
    fun glitchAt(motion: String?, i: Int, length: Int, now: Long): Boolean =
        motion == CustomizerMotion.GLITCH && noise(i, now / GLITCH_MS) % (length * GLITCH_SPREAD).coerceAtLeast(GLITCH_SPREAD) == 0

    /** Same number for the same char and time slot, so a frame drawn twice looks the same. */
    private fun noise(i: Int, slot: Long): Int {
        var h = slot * 0x9E3779B97F4A7C15uL.toLong() + i * 0xBF58476D1CE4E5B9uL.toLong()
        h = (h xor (h ushr 31)) * 0x94D049BB133111EBuL.toLong()
        return (h xor (h ushr 29) and 0x7FFFFFFF).toInt()
    }
    private fun phase(now: Long, periodMs: Long): Float = now % periodMs / periodMs.toFloat()

    /** Full, bright color of a hue from 0 to 1. */
    private fun hue(h: Float): Int {
        val x = h * 6f
        fun channel(offset: Float): Int {
            val k = (offset + x) % 6f
            return ((1f - maxOf(0f, minOf(k, 4f - k, 1f))) * 255).toInt()
        }
        return channel(5f) shl 16 or (channel(3f) shl 8) or channel(1f)
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun channel(shift: Int) = ((a shr shift and 0xFF) * (1 - t) + (b shr shift and 0xFF) * t).toInt()
        return channel(16) shl 16 or (channel(8) shl 8) or channel(0)
    }

    fun parseHex(hex: String): Int = hex.removePrefix("#").toIntOrNull(16) ?: 0xFFFFFF
}
