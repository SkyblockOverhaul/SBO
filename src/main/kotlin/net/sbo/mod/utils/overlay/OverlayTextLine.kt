package net.sbo.mod.utils.overlay

import net.sbo.mod.SBOKotlin.mc

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.Font

import net.minecraft.util.FormattedCharSequence
import net.minecraft.network.chat.Component

import java.awt.Color

class OverlayTextLine(
    text: String,
    var shadow: Boolean = true,
    var linebreak: Boolean = true
) {
    private var orderedText: FormattedCharSequence = Component.nullToEmpty(text).visualOrderText // to avoid nullable OrderedText?

    private var cachedWidth: Int = -1 // filler value used to differenate non-init vs actual width
        private get() {
            val isInit = field != -1 // check if actual width

            if (isInit) {
                return field
            }

            val width = mc.font!!.width(orderedText)
            field = width

            return field
        }

    var text: String = text
        set(value) {
            field = value

            // update cached values when text changes
            orderedText = Component.nullToEmpty(value).visualOrderText
            cachedWidth = mc.font?.width(orderedText) ?: -1 // textRenderer is null at init time
        }

    var mouseEnterAction: (() -> Unit)? = null
    var mouseLeaveAction: (() -> Unit)? = null
    var hoverAction: ((drawContext: GuiGraphics, textRenderer: Font) -> Unit)? = null
    var clickAction: (() -> Unit)? = null
    var isHovered: Boolean = false
    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    private var height: Int = 0
    var renderDebugBox: Boolean = false
    private var condition: () -> Boolean = { true }

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
        val textWidth = cachedWidth * scale
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
        this.width = cachedWidth
        this.height = textRenderer.lineHeight

        if (renderDebugBox) {
            drawContext.fill(x, y, x + width, y + height + 1, Color(128, 128, 128, 130).rgb)
        }

        drawContext.drawString(textRenderer, orderedText, x, y, -1, shadow)
    }
}
