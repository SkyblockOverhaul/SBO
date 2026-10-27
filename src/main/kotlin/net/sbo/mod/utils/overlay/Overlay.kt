package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.game.World

fun isCraftingScreenOpen(): Boolean = CRAFTING_PLAYER_INVENTORY_FILTER(mc.screen)

val CHAT_SCREEN_FILTER = fun(screen: Screen?): Boolean = screen is ChatScreen

val CRAFTING_PLAYER_INVENTORY_FILTER = fun(screen: Screen?): Boolean = screen is InventoryScreen

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
    name: String,
    x: Float,
    y: Float,
    scale: Float = 1.0f,
    allowedScreens: List<(screen: Screen?) -> Boolean> = listOf(CHAT_SCREEN_FILTER),
    val exampleView: List<OverlayTextLine> = emptyList()
) : HudElement(name, x, y, scale, allowedScreens) {
    private var lines = mutableListOf<OverlayTextLine>()

    fun setCondition(condition: () -> Boolean): Overlay {
        this.condition = condition
        return this
    }

    fun setExtraCondition(condition: () -> Boolean): Overlay {
        this.extraCondition = condition
        return this
    }


    override fun updateConditions() {
        super.updateConditions()
        lines.forEach { it.updateCondition() }
    }

    fun addLine(line: OverlayTextLine) {
        line.updateCondition()
        lines.add(line)
    }

    fun setLines(newLines: List<OverlayTextLine>) {
        lines = newLines.toMutableList()
        lines.forEach { it.updateCondition() }
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

    private fun getLines(): List<OverlayTextLine> {
        if (inEditingScreen() && lines.isEmpty() && exampleView.isNotEmpty()) {
            return exampleView
        }
        return lines
    }

    override fun overlayClicked(mouseX: Double, mouseY: Double) {
        if (!World.isInSkyblock()) return
        if (!allowedScreens.any { it(mc.screen) }) return
        val textRenderer = mc.font
        if (!isOverOverlay(mouseX, mouseY)) return

        var currentY = renderY / scale
        var currentX = renderX / scale

        for (line in getLines()) {
            if (!line.checkCondition()) continue
            line.lineClicked(mouseX, mouseY, currentX * scale, currentY * scale, textRenderer, scale)

            if (line.linebreak) {
                currentY += line.height
                currentX = renderX / scale
            } else {
                currentX += line.width
            }
        }
    }

    override fun contentHeight(): Int {
        var totalHeight = 0
        for (line in getLines()) {
            if (line.linebreak && line.checkCondition()) {
                totalHeight += line.height
            }
        }
        return totalHeight
    }

    override fun contentWidth(): Int {
        var maxWidth = 0
        var currentWidth = 0
        for (line in getLines()) {
            currentWidth += line.width
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

    private class LineRenderState(
        val line: OverlayTextLine,
        val x: Int,
        val y: Int
    )

    override fun render(drawContext: GuiGraphicsExtractor, mouseX: Double, mouseY: Double) {
        if (!checkCondition()) {
            return
        }

        val textRenderer = mc.font

        val totalWidth = contentWidth()
        val totalHeight = contentHeight()
        updateRenderPosition(totalWidth, totalHeight)

        if (inEditingScreen()) {
            OverlayEditorStyle.draw(
                drawContext, renderX.toInt(), renderY.toInt(), (totalWidth * scale).toInt(),
                (totalHeight * scale).toInt(), scale, isOverOverlay(mouseX, mouseY), selected,
            )
        }

        drawContext.pose().pushMatrix()
        drawContext.pose().scale(scale, scale)
        var currentY = renderY / scale
        var currentX = renderX / scale

        val lines = getLines()
        val lineStates = mutableListOf<LineRenderState>()

        for (line in lines) {
            if (!line.checkCondition()) continue

            val lineX = if (line.centered) {
                currentX.toInt() + (totalWidth - line.width) / 2
            } else {
                currentX.toInt()
            }
            val lineY = currentY.toInt()

            lineStates += LineRenderState(line, lineX, lineY)
            line.draw(drawContext, lineX, lineY, textRenderer)

            if (line.linebreak) {
                currentY += line.height
                currentX = renderX / scale
            } else {
                currentX += line.width
            }
        }

        if (lineStates.isNotEmpty()) {
            val shouldRender = allowedScreens.any { it(mc.screen) }

            if (shouldRender) {
                for (state in lineStates) {
                    // Renders hover text after all main text, so it appears on top.
                    state.line.updateMouseInteraction(mouseX, mouseY, state.x * scale, state.y * scale, textRenderer, scale, drawContext)
                }
            }
        }

        drawContext.pose().popMatrix()
    }
}
