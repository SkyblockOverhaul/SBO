package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.sbo.mod.SBOKotlin.mc

/** Pure positioning shared with offline presentation checks; no overlay or saved state access. */
internal fun overlayReadoutPosition(left: Int, top: Int, height: Int, labelWidth: Int, labelHeight: Int, viewportWidth: Int, viewportHeight: Int): Pair<Int, Int> {
    val x = left.coerceIn(0, (viewportWidth - labelWidth).coerceAtLeast(0))
    val preferredY = if (top >= labelHeight + 2) top - labelHeight - 2 else top + height + 2
    val y = preferredY.coerceIn(0, (viewportHeight - labelHeight).coerceAtLeast(0))
    return x to y
}

/** The native overlay canvas uses the launcher's neutral surfaces and blue selection accent. */
internal object OverlayEditorStyle {
    const val BACKGROUND = 0xB31E1F22.toInt()
    private const val HOVER = 0x603C3F45
    private const val SELECTED = 0xFF006EFA.toInt()
    private const val PANEL = 0xFF313338.toInt()
    private const val TEXT = 0xFFDBDEE1.toInt()

    fun draw(
        context: GuiGraphicsExtractor,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        scale: Float,
        hovered: Boolean,
        selected: Boolean,
    ) {
        if (hovered) context.fill(left, top, left + width, top + height, HOVER)
        if (!selected) return
        context.outline(left, top, width, height, SELECTED)

        val font = mc.font
        val label = "X: $left Y: $top Scale: ${String.format("%.1f", scale)}"
        val labelWidth = font.width(label) + 8
        val labelHeight = font.lineHeight + 6
        // Keep the existing coordinate readout visible when an overlay touches the top or right edge.
        val (x, y) = overlayReadoutPosition(left, top, height, labelWidth, labelHeight, mc.window.guiScaledWidth, mc.window.guiScaledHeight)
        context.fill(x, y, x + labelWidth, y + labelHeight, PANEL)
        context.text(font, label, x + 4, y + 3, TEXT, false)
    }
}
