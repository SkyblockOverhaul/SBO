package net.sbo.mod.test

import net.sbo.guilib.core.paint.PaintCommand
import net.sbo.guilib.core.dom.Rect
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.floor

/** Diagnostic rasterizer for GuiLib's real paint list, not a Minecraft screenshot.
 * Native items, Minecraft glyphs, shader shadows and theme textures are not reproduced.
 */
internal object HubReviewRaster {
    data class Tile(val label: String, val path: Path, val bounds: Rect)

    /** A small review sheet cropped to measured stage bounds, with readable image resampling. */
    fun partyFinderCheckpoint(out: Path, tiles: List<Tile>) {
        requireHeadlessReview()
        val image = BufferedImage(2000, ((tiles.size + 1) / 2) * 690 + 60, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color(0x151617); g.fillRect(0, 0, image.width, image.height)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        for ((i, tile) in tiles.withIndex()) {
            val source = ImageIO.read(tile.path.toFile())
            val b = tile.bounds
            val sx = (b.x * 3).toInt().coerceAtLeast(0)
            val sy = (b.y * 3).toInt().coerceAtLeast(0)
            val sw = ceil(b.width * 3).toInt().coerceAtMost(source.width - sx)
            val sh = ceil(b.height * 3).toInt().coerceAtMost(source.height - sy)
            val scale = minOf(970.0 / sw, 640.0 / sh)
            val w = (sw * scale).toInt(); val h = (sh * scale).toInt()
            val x = 15 + i % 2 * 1000; val y = 38 + i / 2 * 690
            g.color = Color.WHITE; g.font = Font(Font.SANS_SERIF, Font.BOLD, 22)
            g.drawString(tile.label, x, y - 10)
            g.drawImage(source, x + (970 - w) / 2, y, x + (970 - w) / 2 + w, y + h, sx, sy, sx + sw, sy + sh, null)
        }
        g.color = Color(0xB5BAC1); g.font = Font(Font.SANS_SERIF, Font.PLAIN, 20)
        g.drawString("Synthetic GuiLib fixtures · inert actions/data · native game rendering not verified", 15, image.height - 18)
        g.dispose(); ImageIO.write(image, "png", out.toFile())
    }

    fun settingsCheckpoint(out: Path, version: String) {
        requireHeadlessReview()
        val base = ReviewResources.project.resolve("build/ui-review")
        val sources = listOf("General" to base.resolve("general-template/general-template-revised.png"),
            "Customization" to base.resolve("settings-sweep/$version/Customization/normal.png"),
            "Themes" to base.resolve("settings-sweep/$version/Themes/normal.png"),
            "Debug" to base.resolve("settings-sweep/$version/Debug/normal.png"))
        val result = BufferedImage(1960, 1580, BufferedImage.TYPE_INT_RGB)
        val g = result.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.color = Color(0x151617); g.fillRect(0, 0, result.width, result.height)
        g.color = Color.WHITE; g.font = Font(Font.SANS_SERIF, Font.BOLD, 24)
        sources.forEachIndexed { i, (label, path) ->
            val x = 20 + (i % 2) * 980
            val y = 40 + (i / 2) * 770
            g.drawString(label, x, y - 10)
            g.drawImage(ImageIO.read(path.toFile()), x, y, 940, 705, null)
        }
        g.color = Color(0xB5BAC1); g.font = Font(Font.SANS_SERIF, Font.PLAIN, 20)
        g.drawString("Production Settings styles · synthetic GuiLib fixtures · native game QA pending", 20, 1565)
        g.dispose()
        Files.createDirectories(out.parent)
        ImageIO.write(result, "png", out.toFile())
    }

    fun comparison(out: Path, state: String) {
        requireHeadlessReview()
        val rows = listOf("normal", "constrained")
        val bitmaps = rows.map { size -> listOf("before", "after").map { label ->
            ImageIO.read(out.resolve("$label-$size-$state.png").toFile())
        } }
        val width = bitmaps.flatten().maxOf { it.width }
        val rowHeights = bitmaps.map { row -> row.maxOf { it.height } + 30 }
        val result = BufferedImage(width * 2, rowHeights.sum() + 130, BufferedImage.TYPE_INT_RGB)
        val g = result.createGraphics()
        g.color = Color(0x151617)
        g.fillRect(0, 0, result.width, result.height)
        g.color = Color.WHITE
        g.font = Font(Font.SANS_SERIF, Font.BOLD, 30)
        g.drawString("BEFORE", 24, 38)
        g.drawString("AFTER", width + 24, 38)
        var y = 70
        bitmaps.forEach { row ->
            row.forEachIndexed { column, bitmap ->
                g.drawImage(bitmap, column * width + (width - bitmap.width) / 2, y, null)
            }
            y += row.maxOf { it.height } + 30
        }
        g.color = Color(0xB5BAC1)
        g.font = Font(Font.SANS_SERIF, Font.PLAIN, 23)
        val dimensions = bitmaps.map { it.first().let { image -> (image.width / 3).toString() + "×" + image.height / 3 } }
        g.drawString("Synthetic GuiLib fixtures · top: " + dimensions[0] + " · bottom: " + dimensions[1] + " · not in-game screenshots", 24, result.height - 25)
        g.dispose()
        ImageIO.write(result, "png", out.resolve("comparison-$state.png").toFile())
    }

    fun write(f: ReviewFrame, path: Path) {
        requireHeadlessReview()
        val scale = 3
        val image = BufferedImage((f.width * scale).toInt(), (f.height * scale).toInt(), BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color(0x151617)
        g.fillRect(0, 0, image.width, image.height)
        g.scale(scale.toDouble(), scale.toDouble())
        val base = g.transform
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        val clips = mutableListOf<Shape?>()
        var deviceClip: Shape? = null
        var emblemPainted = false
        fun applyClip() {
            val transform = g.transform
            g.transform = AffineTransform()
            g.clip = deviceClip
            g.transform = transform
        }
        for (c in f.settle()) when (c) {
            is PaintCommand.SetTransform -> {
                g.transform = AffineTransform(base).apply {
                    c.transform?.let { t -> concatenate(AffineTransform(t.a.toDouble(), t.b.toDouble(), t.c.toDouble(), t.d.toDouble(), t.tx.toDouble(), t.ty.toDouble())) }
                }
                applyClip()
            }
            is PaintCommand.PushClip -> {
                clips.add(deviceClip)
                // Clip rectangles are already mapped to viewport coordinates by GuiLib's Painter.
                val area = Area(base.createTransformedShape(Rectangle2D.Float(c.rect.x, c.rect.y, c.rect.width, c.rect.height)))
                deviceClip?.let { area.intersect(Area(it)) }
                deviceClip = area
                applyClip()
            }
            is PaintCommand.PopClip -> { deviceClip = clips.removeAt(clips.lastIndex); applyClip() }
            is PaintCommand.Box -> {
                g.composite = AlphaComposite.SrcOver
                val radius = c.radii.maxOrNull() ?: 0f
                val shape = RoundRectangle2D.Float(c.x, c.y, c.width, c.height, radius * 2, radius * 2)
                g.color = Color(c.background, true)
                g.fill(shape)
                if (c.hasBorder && c.borders.distinct().size == 1 && c.borderColors.distinct().size == 1) {
                    val inset = c.borders[0]
                    val ring = Area(shape)
                    ring.subtract(Area(RoundRectangle2D.Float(c.x + inset, c.y + inset,
                        (c.width - inset * 2).coerceAtLeast(0f), (c.height - inset * 2).coerceAtLeast(0f),
                        (radius * 2 - inset * 2).coerceAtLeast(0f), (radius * 2 - inset * 2).coerceAtLeast(0f))))
                    g.color = Color(c.borderColors[0], true)
                    g.fill(ring)
                } else if (c.hasBorder) {
                    val original = g.clip
                    g.clip(shape)
                    val rects = listOf(
                        Rectangle2D.Float(c.x, c.y, c.width, c.borders[0]),
                        Rectangle2D.Float(c.x + c.width - c.borders[1], c.y, c.borders[1], c.height),
                        Rectangle2D.Float(c.x, c.y + c.height - c.borders[2], c.width, c.borders[2]),
                        Rectangle2D.Float(c.x, c.y, c.borders[3], c.height),
                    )
                    rects.forEachIndexed { i, rect -> g.color = Color(c.borderColors[i], true); g.fill(rect) }
                    g.clip = original
                }
            }
            is PaintCommand.Text -> {
                if (!emblemPainted) {
                    // Fabric normally initializes img rendering. Composite the bundled static emblem
                    // at its GuiLib-measured rectangle before text, so modal backdrops dim it too.
                    val savedTransform = g.transform
                    g.transform = base
                    f.emblem?.let { rect ->
                        ReviewResources.resource("/assets/sbo/ui/hub/emblem.png").use { stream ->
                            g.composite = AlphaComposite.SrcOver
                            g.drawImage(ImageIO.read(stream), rect.x.toInt(), rect.y.toInt(), rect.width.toInt(), rect.height.toInt(), null)
                        }
                    }
                    g.transform = savedTransform
                    emblemPainted = true
                }
                g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, c.alpha.coerceIn(0f, 1f))
                g.color = Color(c.color, true)
                g.font = f.fonts.font(c.style)
                var x = c.x
                val baseline = c.y + f.fonts.metrics(c.style).ascent
                // GuiLib has already wrapped/transformed the text; preserve its measured spacing.
                for (ch in c.text) {
                    g.font = f.fonts.font(c.style, ch.toString())
                    g.drawString(ch.toString(), x, baseline)
                    x += f.fonts.width(ch.toString(), c.style) + c.style.letterSpacing
                }
            }
            is PaintCommand.Gradient -> {
                // GuiLib supplies local triangle vertices and per-vertex ARGB colors. Interpolate
                // that mesh headlessly so picker hue/SV/alpha surfaces are visible in diagnostics.
                val bitmap = BufferedImage(ceil(c.width * scale).toInt().coerceAtLeast(1),
                    ceil(c.height * scale).toInt().coerceAtLeast(1), BufferedImage.TYPE_INT_ARGB)
                val mesh = c.mesh
                for (i in mesh.x.indices step 3) {
                    val x0 = mesh.x[i] * scale; val y0 = mesh.y[i] * scale
                    val x1 = mesh.x[i + 1] * scale; val y1 = mesh.y[i + 1] * scale
                    val x2 = mesh.x[i + 2] * scale; val y2 = mesh.y[i + 2] * scale
                    val determinant = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
                    if (kotlin.math.abs(determinant) < 0.0001f) continue
                    val left = floor(minOf(x0, x1, x2)).toInt().coerceAtLeast(0)
                    val right = ceil(maxOf(x0, x1, x2)).toInt().coerceAtMost(bitmap.width - 1)
                    val top = floor(minOf(y0, y1, y2)).toInt().coerceAtLeast(0)
                    val bottom = ceil(maxOf(y0, y1, y2)).toInt().coerceAtMost(bitmap.height - 1)
                    for (y in top..bottom) for (x in left..right) {
                        val a = ((y1 - y2) * (x + 0.5f - x2) + (x2 - x1) * (y + 0.5f - y2)) / determinant
                        val b = ((y2 - y0) * (x + 0.5f - x2) + (x0 - x2) * (y + 0.5f - y2)) / determinant
                        val d = 1 - a - b
                        if (minOf(a, b, d) < -0.0001f) continue
                        var color = 0
                        for (shift in listOf(0, 8, 16, 24)) {
                            val channel = (((mesh.color[i] ushr shift) and 255) * a +
                                ((mesh.color[i + 1] ushr shift) and 255) * b + ((mesh.color[i + 2] ushr shift) and 255) * d).toInt().coerceIn(0, 255)
                            color = color or (channel shl shift)
                        }
                        bitmap.setRGB(x, y, color)
                    }
                }
                val previousClip = g.clip
                val radius = c.radii.maxOrNull() ?: 0f
                g.clip(RoundRectangle2D.Float(c.x, c.y, c.width, c.height, radius * 2, radius * 2))
                g.composite = AlphaComposite.SrcOver
                g.drawImage(bitmap, AffineTransform().apply { translate(c.x.toDouble(), c.y.toDouble()); scale(1.0 / scale, 1.0 / scale) }, null)
                g.clip = previousClip
            }
            is PaintCommand.Image -> {
                val resource = "/assets/" + c.src.replace(':', '/')
                ReviewResources::class.java.getResourceAsStream(resource)?.use { stream ->
                    val bitmap = ImageIO.read(stream)
                    if (bitmap != null) {
                        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, c.alpha.coerceIn(0f, 1f))
                        g.drawImage(bitmap, c.x.toInt(), c.y.toInt(), c.width.toInt(), c.height.toInt(), null)
                    }
                }
            }
            else -> Unit
        }
        g.dispose()
        Files.createDirectories(path.parent)
        ImageIO.write(image, "png", path.toFile())
    }
}
