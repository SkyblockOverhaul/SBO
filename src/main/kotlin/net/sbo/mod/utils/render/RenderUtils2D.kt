package net.sbo.mod.utils.render

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

object RenderUtils2D {
    fun drawHoveringString(
        drawContext: GuiGraphics,
        text: String,
        x: Double,
        y: Double,
        textRenderer: Font,
        scale: Float = 1.0f,
        padding: Int = 2
    ) {
        if (text.isEmpty()) return

        val textLines = text.split("\n").map { Component.nullToEmpty(it) }

        drawContext.pose().pushMatrix()

        drawContext.setComponentTooltipForNextFrame(textRenderer, textLines, x.toInt(), y.toInt())

        drawContext.pose().popMatrix()
    }
}
