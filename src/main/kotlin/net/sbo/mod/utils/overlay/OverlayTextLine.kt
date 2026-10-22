package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin.mc
import java.awt.Color

/**
 * A line of an [Overlay]. In front of the text it can draw an item [icon]: [ICON_SMALL] fits the text line, a bigger
 * one like [ICON_LARGE] makes the line taller.
 * Item icons cost more to draw than text, so only use them on a few lines.
 */
class OverlayTextLine(
    var text: String,
    private var shadow: Boolean = true,
    var linebreak: Boolean = true,
    var centered: Boolean = false,
    var icon: ItemStack? = null,
    var iconSize: Int = ICON_SMALL
) {
    private var orderedText: FormattedCharSequence? = null
    private var orderedTextSource: String? = null

    private var mouseEnterAction: (() -> Unit)? = null
    private var mouseLeaveAction: (() -> Unit)? = null
    private var hoverAction: ((drawContext: GuiGraphicsExtractor, textRenderer: Font) -> Unit)? = null
    private var clickAction: (() -> Unit)? = null
    private var isHovered: Boolean = false
    var x: Int = 0
    var y: Int = 0

    private var textWidth: Int = -1
        get() {
            val currentText = text

            if (orderedTextSource == currentText && field != -1) {
                return field
            }

            val computedWidth = mc.font.width(getOrderedText())
            field = computedWidth

            return computedWidth
        }

    private val prefixWidth: Int
        get() = if (icon != null) iconSize + GAP else 0

    /** The width of the icon and text together. */
    val width: Int
        get() = prefixWidth + textWidth

    /** The height of the line including the 1px gap below it. */
    val height: Int
        get() = maxOf(mc.font.lineHeight, if (icon != null) iconSize else 0) + 1

    private var renderDebugBox: Boolean = false

    private var condition: () -> Boolean = { true }
    private var cachedCondition = true

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

    fun checkCondition(): Boolean = cachedCondition

    fun updateCondition() {
        cachedCondition = condition()
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
    fun onHover(action: (drawContext: GuiGraphicsExtractor, textRenderer: Font) -> Unit): OverlayTextLine {
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

    private fun hover(drawContext: GuiGraphicsExtractor, textRenderer: Font) {
        if (isHovered) hoverAction?.invoke(drawContext, textRenderer)
    }

    fun lineClicked(mouseX: Double, mouseY: Double, x: Float, y: Float, textRenderer: Font, scale: Float) {
        if (text.isEmpty() || clickAction == null) return
        if (isMouseOver(mouseX, mouseY, x, y, textRenderer, scale)) {
            clickAction?.invoke()
        }
    }

    private fun isMouseOver(mouseX: Double, mouseY: Double, x: Float, y: Float, textRenderer: Font, scale: Float): Boolean {
        val lineWidth = width * scale
        val lineHeight = height * scale - 1

        return mouseX >= x && mouseX <= x + lineWidth && mouseY >= y && mouseY <= y + lineHeight
    }

    fun updateMouseInteraction(mouseX: Double, mouseY: Double, x: Float, y: Float, textRenderer: Font, scale: Float, drawContext: GuiGraphicsExtractor) {
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

    fun draw(drawContext: GuiGraphicsExtractor, x: Int, y: Int, textRenderer: Font) {
        if (text.isEmpty() && icon == null) return

        this.x = x
        this.y = y
        val lineHeight = height

        if (renderDebugBox) {
            drawContext.fill(x, y, x + width, y + lineHeight, Color(128, 128, 128, 130).rgb)
        }

        // Everything is centered vertically in the line, which is taller than the text with a large icon
        var currentX = x
        icon?.let { stack ->
            drawIcon(drawContext, stack, currentX, y + (lineHeight - 1 - iconSize) / 2)
            currentX += iconSize + GAP
        }
        if (text.isNotEmpty()) {
            drawContext.text(textRenderer, getOrderedText(), currentX, y + (lineHeight - 1 - textRenderer.lineHeight) / 2, -1, shadow)
        }
    }

    // Items are 16px, scaled down to the icon size
    private fun drawIcon(drawContext: GuiGraphicsExtractor, stack: ItemStack, x: Int, y: Int) {
        val scale = iconSize / 16f
        drawContext.pose().pushMatrix()
        drawContext.pose().translate(x.toFloat(), y.toFloat())
        drawContext.pose().scale(scale, scale)
        drawContext.item(stack, 0, 0)
        drawContext.pose().popMatrix()
    }

    companion object {
        const val ICON_SMALL = 8
        const val ICON_LARGE = 16
        private const val GAP = 2
    }
}
