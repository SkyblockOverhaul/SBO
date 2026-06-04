package net.sbo.mod.utils.render

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.network.chat.Component

object RenderUtils2D {
    fun drawHoveringString(
        drawContext: GuiGraphics,
        text: String,
        x: Double,
        y: Double,
        textRenderer: Font,
    ) {
        if (text.isEmpty()) return

        val textLines = text.split("\n").map { Component.nullToEmpty(it) }
        drawContext.renderTooltip(textRenderer, textLines.map { it.visualOrderText }.map { ClientTooltipComponent.create(it) }, x.toInt(), y.toInt(), DefaultTooltipPositioner.INSTANCE, null)
    }
}
