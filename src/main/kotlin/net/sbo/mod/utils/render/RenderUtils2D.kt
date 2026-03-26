package net.sbo.mod.utils.render

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text

object RenderUtils2D {
    fun drawHoveringString(
        drawContext: DrawContext,
        text: String,
        x: Double,
        y: Double,
        textRenderer: TextRenderer,
        scale: Float = 1.0f,
        padding: Int = 2
    ) {
        if (text.isEmpty()) return

        val textLines = text.split("\n").map { Text.of(it) }

        drawContext.matrices.pushMatrix()

        drawContext.drawTooltip(textRenderer, textLines, x.toInt(), y.toInt())

        drawContext.matrices.popMatrix()
    }
}
