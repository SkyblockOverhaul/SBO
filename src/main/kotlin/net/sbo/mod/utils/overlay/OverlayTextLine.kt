package net.sbo.mod.utils.overlay

import net.sbo.mod.SBOKotlin.mc

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.Font

import net.minecraft.util.FormattedCharSequence
import net.minecraft.network.chat.Component

import java.awt.Color

class OverlayTextLine(
    var text: String,
    var shadow: Boolean = true,
    var linebreak: Boolean = true
) {
    private var orderedText: FormattedCharSequence? = null
    private var orderedTextSource: String? = null

    var mouseEnterAction: (() -> Unit)? = null
    var mouseLeaveAction: (() -> Unit)? = null
    var hoverAction: ((drawContext: GuiGraphics, textRenderer: Font) -> Unit)? = null
    var clickAction: (() -> Unit)? = null
    var isHovered: Boolean = false
    var x: Int = 0
    var y: Int = 0

    var width: Int = -1
        get() {
            val currentText = text

            if (orderedTextSource == currentText && field != -1) {
                return field
            }

            val computedWidth = mc.font.width(getOrderedText())
            field = computedWidth

            return computedWidth
        }
        private set

    private var height: Int = 0
    var renderDebugBox: Boolean = false
    private var condition: () -> Boolean = { true }

    private fun getOrderedText(): FormattedCharSequence { // we return FormattedCharSequence instead of FormattedCharSequence? so this can't be a property getter
        val currentText = text
        val cachedText = orderedTextSource
        val cachedOrderedText = orderedText

        if (cachedOrderedText != null && currentText == cachedText) {
            return cachedOrderedText
        }

        val computed = Component.nullToEmpty(currentText).visualOrderText

        orderedText = computed
        orderedTextSource = currentText

        return computed
    }

    fun setCondition(condition: () -> Boolean): OverlayTextLine {
        this.condition = condition
        return this
    }

    fun checkCondition(): Boolean {
        return condition()
    }

    /**
     * Executes the mouse enter action when the mouse enters the text line.
     * @param action The action to execute when the mouse enters the text line.
     */
    fun onMouseEnter(action: () -> Unit): OverlayTextLine {
        mouseEnterAction = action
        return this
    }

    /**
     * Executes the mouse leave action when the mouse leaves the text line.
     * @param action The action to execute when the mouse leaves the text line.
     */
    fun onMouseLeave(action: () -> Unit): OverlayTextLine {
        mouseLeaveAction = action
        return this
    }

    /**
     * Executes the hover action when the mouse is over the text line.
     * warning: This action is executed in the render loop,
     * so it should be lightweight to avoid performance issues.
     * @param action The action to execute when the mouse hovers over the text line.
     */
    fun onHover(action: (drawContext: GuiGraphics, textRenderer: Font) -> Unit): OverlayTextLine {
        hoverAction = action
        return this
    }

    /**
     * Executes the click action when the text line is clicked.
     * @param action The action to execute when the text line is clicked.
     */
    fun onClick(action: () -> Unit): OverlayTextLine {
        clickAction = action
        return this
    }

    private fun mouseEnter() {
        mouseEnterAction?.invoke()
    }

    private fun mouseLeave() {
        mouseLeaveAction?.invoke()
    }

    private fun hover(drawContext: GuiGraphics, textRenderer: Font) {
        if (isHovered) hoverAction?.invoke(drawContext, textRenderer)
    }

    fun lineClicked(mouseX: Double, mouseY: Double, x: Float, y: Float, textRenderer: Font, scale: Float) {
        if (text.isEmpty() || clickAction == null) return
        if (isMouseOver(mouseX, mouseY, x, y, textRenderer, scale)) {
            clickAction?.invoke()
        }
    }

    private fun isMouseOver(mouseX: Double, mouseY: Double, x: Float, y: Float, textRenderer: Font, scale: Float): Boolean {
        val textWidth = width * scale
        val textHeight = (textRenderer.lineHeight + 1) * scale - 1

        return mouseX >= x && mouseX <= x + textWidth && mouseY >= y && mouseY <= y + textHeight
    }

    fun updateMouseInteraction(mouseX: Double, mouseY: Double, x: Float, y: Float, textRenderer: Font, scale: Float, drawContext: GuiGraphics) {
        if (text.isEmpty()) return
        if (mouseEnterAction == null && mouseLeaveAction == null && hoverAction == null) {
            return
        }
        val wasHovered = isHovered
        val isNowHovered = isMouseOver(mouseX, mouseY, x, y, textRenderer, scale)
        isHovered = isNowHovered

        if (isNowHovered && !wasHovered) {
            mouseEnter()
        } else if (!isNowHovered && wasHovered) {
            mouseLeave()
        }

        if (isNowHovered) {
            hover(drawContext, textRenderer)
        }
    }

    fun draw(drawContext: GuiGraphics, x: Int, y: Int, textRenderer: Font) {
        if (text.isEmpty()) return

        this.x = x
        this.y = y
        this.height = textRenderer.lineHeight

        if (renderDebugBox) {
            drawContext.fill(x, y, x + width, y + height + 1, Color(128, 128, 128, 130).rgb)
        }

        drawContext.drawString(textRenderer, getOrderedText(), x, y, -1, shadow)
    }
}
