package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.data.OverlayValues
import net.sbo.mod.utils.data.SboDataObject.overlayData
import java.awt.Color

fun isCraftingScreenOpen(): Boolean {
    return CRAFTING_PLAYER_INVENTORY_FILTER(mc.screen)
}

val CHAT_SCREEN_FILTER = fun(screen: Screen?): Boolean {
    return screen is ChatScreen
}

val CRAFTING_PLAYER_INVENTORY_FILTER = fun(screen: Screen?): Boolean {
    return screen is InventoryScreen
}

/**
 * Represents an overlay that can display text lines on the screen.
 * Overlays can be customized with position, scale, and render conditions.
 * They can also be clicked to trigger actions on the text lines.
 * @property name The name of the overlay.
 * @property x The x-coordinate of the overlay.
 * @property y The y-coordinate of the overlay.
 * @property scale The scale of the overlay, default is 1.0f.
 * @property allowedScreens The list of screens where the overlay is allowed to render.
 */
class Overlay(
    var name: String,
    var x: Float,
    var y: Float,
    var scale: Float = 1.0f,
    val allowedScreens: List<(screen: Screen?) -> Boolean> = listOf(CHAT_SCREEN_FILTER),
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
        if (!allowedScreens.any { it(mc.screen) }) return
        val textRenderer = mc.font
        if (!isOverOverlay(mouseX, mouseY)) return

        var currentY = y / scale
        var currentX = x / scale

        for (line in getLines()) {
            if (!line.checkCondition()) continue
            line.lineClicked(mouseX, mouseY, currentX * scale, currentY * scale, textRenderer, scale)

            if (line.linebreak) {
                currentY += textRenderer.lineHeight + 1
                currentX = x / scale
            } else {
                currentX += textRenderer.width(line.text) / scale
            }
        }
    }

    fun getTotalHeight(): Int {
        val textRenderer = mc.font
        var totalHeight = 0
        for (line in getLines()) {
            if (line.linebreak && line.checkCondition()) {
                totalHeight += textRenderer.lineHeight + 1
            }
        }
        return totalHeight
    }

    fun getTotalWidth(): Int {
        val textRenderer = mc.font
        var maxWidth = 0
        var currentWidth = 0
        for (line in getLines()) {
            currentWidth += textRenderer.width(line.text)
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

    fun render(drawContext: GuiGraphics, mouseX: Double, mouseY: Double) {
        if (!condition()) return
        val textRenderer = mc.font

        drawContext.pose().pushMatrix()
        drawContext.pose().scale(scale, scale)

        var currentY = (y / scale)
        var currentX = (x / scale)

        // We don't need thread safety as we are in method-local context
        // Making these vars lazy ensures they are only computed when needed (e.g not computed if both of the if conditions below are false)
        // and only computed once when both of the if conditions are true.
        val totalWidth by lazy(LazyThreadSafetyMode.NONE) { getTotalWidth() }
        val totalHeight by lazy(LazyThreadSafetyMode.NONE) { getTotalHeight() }

        if (selected) {
            drawDebugBox(drawContext, currentX.toInt(), currentY.toInt(), totalWidth, totalHeight)
            drawContext.drawString(textRenderer, "X: ${x.toInt()} Y: ${y.toInt()} Scale: ${String.format("%.1f", scale)}", (currentX).toInt(), (currentY - textRenderer.lineHeight - 1).toInt(), Color(255, 255, 255, 200).rgb, true)
        }

        if (Helper.currentScreen is OverlayEditScreen && isOverOverlay(mouseX, mouseY, totalWidth, totalHeight)) {
            drawContext.fill(currentX.toInt(), currentY.toInt(), (currentX + totalWidth).toInt(), (currentY + totalHeight).toInt(), Color(0, 0, 0, 100).rgb)
        }

        val shouldRender by lazy(LazyThreadSafetyMode.NONE) { allowedScreens.any { it(mc.screen) } }

        for (line in getLines()) {
            if (!line.checkCondition()) continue
            if (shouldRender) line.updateMouseInteraction(mouseX, mouseY, currentX * scale, currentY * scale, textRenderer, scale, drawContext)
            line.draw(drawContext, currentX.toInt(), currentY.toInt(), textRenderer)
            if (line.linebreak) {
                currentY += textRenderer.lineHeight + 1
                currentX = (x / scale)
            } else {
                currentX += line.width
            }
        }

        drawContext.pose().popMatrix()
    }

    private fun drawDebugBox(drawContext: GuiGraphics, x: Int, y: Int, width: Int, height: Int) {
        // Multiply everything by scale so that the box renders correctly when scale != 1.0F
        val scaledX = (x * scale).toInt()
        val scaledY = (y * scale).toInt()
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()

        //#if MC > 1.21.10
        //$$ drawContext.renderOutline(x, y, width, height, Color(255, 0, 0, 170).rgb)
        //#else
        drawContext.submitOutline(scaledX, scaledY, scaledWidth, scaledHeight, Color(255, 0, 0, 170).rgb)
        //#endif
    }
}
