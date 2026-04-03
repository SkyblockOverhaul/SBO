package net.sbo.mod.utils.overlay

import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.data.OverlayValues
import net.sbo.mod.utils.data.SboDataObject.overlayData
import java.awt.Color

fun isCraftingScreenOpen(): Boolean {
    return CRAFTING_PLAYER_INVENTORY_FILTER(mc.currentScreen)
}

val CHAT_SCREEN_FILTER = fun(screen: Screen?): Boolean {
    return screen is ChatScreen
}

val CRAFTING_PLAYER_INVENTORY_FILTER = fun(screen: Screen?): Boolean {
    if (screen !is InventoryScreen) return false

    val handler = screen.screenHandler

    if (handler !is PlayerScreenHandler) return false // PlayerScreenHandler extends AbstractCraftingScreenHandler, but checking AbstractCraftingScreenHandler will also match the full 3x3 crafting inventory.

    return true // could also check for handler.slots.size == 46 but checking for PlayerScreenHandler seems to work already. 46 is because 27 inventory slots, 9 hotbar slots, 4 armor slots, 4 crafting input slots, 1 crafting output slot and 1 offhand/shield slot, totalling 46. when a chest/large chest (which hypixel uses for menus) is open, only the 27 inventory slots + the chest size is considered; so .size in these cases are 63 (regular chest) or 90 (double chest), because 27 inventory + 9 hotbar + 27 chest, and 27 inventory + 9 hotbar + 54 double chest (which is 27*2).
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
        if (!allowedScreens.any { it(mc.currentScreen) }) return
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

        drawContext.matrices.pushMatrix()
        drawContext.matrices.scale(scale, scale)

        var currentY = (y / scale)
        var currentX = (x / scale)

        // We don't need thread safety as we are in method-local context
        // Making these vars lazy ensures they are only computed when needed (e.g not computed if both of the if conditions below are false)
        // and only computed once when both of the if conditions are true.
        val totalWidth by lazy(LazyThreadSafetyMode.NONE) { getTotalWidth() }
        val totalHeight by lazy(LazyThreadSafetyMode.NONE) { getTotalHeight() }

        if (selected) {
            drawDebugBox(drawContext, currentX.toInt(), currentY.toInt(), totalWidth, totalHeight)
            drawContext.drawText(textRenderer, "X: ${x.toInt()} Y: ${y.toInt()} Scale: ${String.format("%.1f", scale)}", (currentX).toInt(), (currentY - textRenderer.fontHeight - 1).toInt(), Color(255, 255, 255, 200).rgb, true)
        }

        if (Helper.currentScreen is OverlayEditScreen && isOverOverlay(mouseX, mouseY, totalWidth, totalHeight)) {
            drawContext.fill(currentX.toInt(), currentY.toInt(), (currentX + totalWidth).toInt(), (currentY + totalHeight).toInt(), Color(0, 0, 0, 100).rgb)
        }

        val shouldRender by lazy(LazyThreadSafetyMode.NONE) { allowedScreens.any { it(mc.currentScreen) } }

        for (line in getLines()) {
            if (!line.checkCondition()) continue
            if (shouldRender) line.updateMouseInteraction(mouseX, mouseY, currentX * scale, currentY * scale, textRenderer, scale, drawContext)
            line.draw(drawContext, currentX.toInt(), currentY.toInt(), textRenderer)
            if (line.linebreak) {
                currentY += textRenderer.fontHeight + 1
                currentX = (x / scale)
            } else {
                currentX += line.width
            }
        }

        drawContext.matrices.popMatrix()
    }

    private fun drawDebugBox(drawContext: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        // Multiply everything by scale so that the box renders correctly when scale != 1.0F
        val scaledX = (x * scale).toInt()
        val scaledY = (y * scale).toInt()
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()

        //#if MC > 1.21.10
        //$$ drawContext.drawStrokedRectangle(x, y, width, height, Color(255, 0, 0, 170).rgb)
        //#else
        drawContext.drawStrokedRectangle(scaledX, scaledY, scaledWidth, scaledHeight, Color(255, 0, 0, 170).rgb)
        //#endif
    }
}
