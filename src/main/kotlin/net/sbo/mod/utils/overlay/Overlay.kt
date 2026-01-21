package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.DrawContext
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.data.OverlayValues
import net.sbo.mod.utils.data.SboDataObject.overlayData
import java.awt.Color

/**
 * Represents an overlay that can display text lines on the screen.
 * Overlays can be customized with position, scale, and render conditions.
 * They can also be clicked to trigger actions on the text lines.
 * @property name The name of the overlay.
 * @property x The x-coordinate of the overlay.
 * @property y The y-coordinate of the overlay.
 * @property scale The scale of the overlay, default is 1.0f.
 * @property allowedGuis The list of GUI names where the overlay is allowed to render.
 */
class Overlay(
    var name: String,
    var x: Float,
    var y: Float,
    var scale: Float = 1.0f,
    var allowedGuis: List<String> = listOf("Chat screen"),
    var exampleView: List<OverlayTextLine> = listOf()
) {
    private var lines = mutableListOf<OverlayTextLine>()
    private var condition: () -> Boolean = { true }

    var selected: Boolean = false

    fun init() {
        if (overlayData.overlays.containsKey(name)) {
            val data = overlayData.overlays[name]!!
            x = data.x
            y = data.y
            scale = data.scale
        } else {
            overlayData.overlays[name] = OverlayValues(x, y, scale)
        }
        OverlayManager.overlays.add(this)
    }

    fun setCondition(condition: () -> Boolean): Overlay {
        this.condition = condition
        return this
    }

    fun checkCondition(): Boolean {
        return condition()
    }

    fun addLine(line: OverlayTextLine) {
        lines.add(line)
    }

    fun addLineAt(index: Int, line: OverlayTextLine) {
        lines.add(index, line)
    }

    fun addLines(newLines: List<OverlayTextLine>) {
        lines.addAll(newLines)
    }

    fun setLines(newLines: List<OverlayTextLine>) {
        lines = newLines.toMutableList()
    }

    fun removeLine(line: OverlayTextLine) {
        lines.remove(line)
    }

    fun removeLines(linesToRemove: List<OverlayTextLine>) {
        lines.removeAll(linesToRemove)
    }

    fun clearLines() {
        lines = mutableListOf()
    }

    fun getLines(): List<OverlayTextLine> {
        if (lines.isEmpty() && exampleView.isNotEmpty() && Helper.currentScreen is OverlayEditScreen) {
            return exampleView
        }
        return lines
    }

    fun overlayClicked(mouseX: Double, mouseY: Double) {
        if (!World.isInSkyblock()) return
        if (Helper.getGuiName() !in allowedGuis) return
        val textRenderer = mc.textRenderer ?: return
        if (!isOverOverlay(mouseX, mouseY)) return

        var currentY = y / scale
        var currentX = x / scale

        for (line in getLines()) {
            if (!line.checkCondition()) continue
            line.lineClicked(mouseX, mouseY, currentX * scale, currentY * scale, textRenderer, scale)

            if (line.linebreak) {
                currentY += textRenderer.fontHeight + 1
                currentX = x / scale
            } else {
                currentX += textRenderer.getWidth(line.text) / scale
            }
        }
    }

    fun getTotalHeight(): Int {
        val textRenderer = mc.textRenderer ?: return 0
        var totalHeight = 0
        for (line in getLines()) {
            if (line.linebreak && line.checkCondition()) {
                totalHeight += textRenderer.fontHeight + 1
            }
        }
        return totalHeight
    }

    fun getTotalWidth(): Int {
        val textRenderer = mc.textRenderer ?: return 0
        var maxWidth = 0
        var currentWidth = 0
        for (line in getLines()) {
            currentWidth += textRenderer.getWidth(line.text)
            if (line.linebreak) {
                if (currentWidth > maxWidth) {
                    maxWidth = currentWidth
                }
                currentWidth = 0
            }
        }

        if (currentWidth > maxWidth) {
            maxWidth = currentWidth
        }
        return maxWidth
    }

    fun isOverOverlay(mouseX: Double, mouseY: Double, width: Int = getTotalWidth(), height: Int = getTotalHeight()): Boolean {
        if (!condition()) return false
        val totalWidth = width * scale
        val totalHeight = height * scale

        return mouseX >= x && mouseX <= x + totalWidth && mouseY >= y && mouseY <= y + totalHeight
    }

    fun render(drawContext: DrawContext, mouseX: Double, mouseY: Double) {
        if (!condition()) return
        val textRenderer = mc.textRenderer ?: return

        //#if MC >= 1.21.7
        //$$ drawContext.matrices.pushMatrix()
        //$$ drawContext.matrices.scale(scale, scale)
        //#else
        drawContext.matrices.push()
        drawContext.matrices.scale(scale, scale, 1.0f)
        //#endif

        var currentY = (y / scale)
        var currentX = (x / scale)

        val totalWidth = getTotalWidth()
        val totalHeight = getTotalHeight()

        if (selected) {
            drawDebugBox(drawContext, currentX.toInt(), currentY.toInt(), totalWidth, totalHeight)
            drawContext.drawText(textRenderer, "X: ${x.toInt()} Y: ${y.toInt()} Scale: ${String.format("%.1f", scale)}", (currentX).toInt(), (currentY - textRenderer.fontHeight - 1).toInt(), Color(255, 255, 255, 200).rgb, true)
        }

        if (isOverOverlay(mouseX, mouseY, totalWidth, totalHeight) && Helper.currentScreen is OverlayEditScreen) {
            drawContext.fill(currentX.toInt(), currentY.toInt(), (currentX + totalWidth).toInt(), (currentY + totalHeight).toInt(), Color(0, 0, 0, 100).rgb)
        }

        for (line in getLines()) {
            if (!line.checkCondition()) continue
            if (Helper.getGuiName() in allowedGuis) line.updateMouseInteraction(mouseX, mouseY, currentX * scale, currentY * scale, textRenderer, scale, drawContext)
            line.draw(drawContext, currentX.toInt(), currentY.toInt(), textRenderer)
            if (line.linebreak) {
                currentY += textRenderer.fontHeight + 1
                currentX = (x / scale)
            } else {
                currentX += line.width
            }
        }

        //#if MC >= 1.21.7
        //$$ drawContext.matrices.popMatrix()
        //#else
        drawContext.matrices.pop()
        //#endif
    }

    private fun drawDebugBox(drawContext: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        //#if MC >= 1.21.9
        //$$ drawContext.drawStrokedRectangle(x, y, width, height, Color(255, 0, 0, 170).rgb)
        //#else
        drawContext.drawBorder(x, y, width, height, Color(255, 0, 0, 170).rgb)
        //#endif
    }
}
