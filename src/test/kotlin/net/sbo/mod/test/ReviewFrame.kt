package net.sbo.mod.test

import net.sbo.guilib.core.dom.Rect
import net.sbo.guilib.core.layout.FontMetrics
import net.sbo.guilib.core.layout.TextMeasurer
import net.sbo.guilib.core.layout.TextStyle
import net.sbo.guilib.core.paint.PaintCommand
import java.awt.Font
import java.awt.font.FontRenderContext
import java.nio.file.Files
import java.nio.file.Path

/** Shared offline review primitives; no live screen or game initialization. */
internal interface ReviewFrame {
    val width: Float
    val height: Float
    val fonts: ReviewFonts
    val emblem: Rect? get() = null
    fun settle(): List<PaintCommand>
}

internal object ReviewResources {
    val project: Path = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
        .first { Files.exists(it.resolve("gradlew")) && Files.exists(it.resolve("src/main")) }
    fun resource(path: String) = requireNotNull(ReviewResources::class.java.getResourceAsStream(path)) { path }
}

internal class ReviewFonts : TextMeasurer {
    private val context = run { requireHeadlessReview(); FontRenderContext(null, true, true) }
    private val regular = ReviewResources.resource("/assets/guilib/fonts/inter-regular.ttf").use { Font.createFont(Font.TRUETYPE_FONT, it) }
    private val bold = ReviewResources.resource("/assets/guilib/fonts/inter-bold.ttf").use { Font.createFont(Font.TRUETYPE_FONT, it) }
    // AWT has no Minecraft renderer. Monospace is an explicit approximation for command strings.
    fun font(style: TextStyle, text: String = "Ag"): Font {
        val preferred = (if (style.fontFamily.firstOrNull() == "minecraft") Font(Font.MONOSPACED, if (style.bold) Font.BOLD else Font.PLAIN, 8)
            else if (style.bold) bold else regular).deriveFont(style.fontSize)
        return if (preferred.canDisplayUpTo(text) < 0) preferred
            else Font(Font.DIALOG, if (style.bold) Font.BOLD else Font.PLAIN, 8).deriveFont(style.fontSize)
    }
    override fun width(text: String, style: TextStyle) = font(style, text).getStringBounds(text, context).width.toFloat() +
        (text.length - 1).coerceAtLeast(0) * style.letterSpacing
    override fun metrics(style: TextStyle): FontMetrics = font(style).getLineMetrics("Ag", context).let { FontMetrics(it.ascent, it.descent, it.height) }
}
