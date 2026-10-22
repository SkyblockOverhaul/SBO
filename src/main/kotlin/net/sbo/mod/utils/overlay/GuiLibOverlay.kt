package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.fabric.GuiLibHud
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.game.World
import java.awt.Color
import kotlin.math.ceil

/**
 * An overlay built with GuiLib (tags + CSS) instead of text lines, moved and scaled in /sboguis like the others.
 * Call [setContent] whenever the shown data changes; drawing reuses the last content every frame, so it stays cheap.
 * It gets no mouse input of its own, [onClick] runs for a click on it (or on one element of it) in one of the
 * [allowedScreens].
 */
class GuiLibOverlay(
    name: String,
    x: Float,
    y: Float,
    scale: Float = 1.0f,
    allowedScreens: List<(screen: Screen?) -> Boolean> = listOf(CHAT_SCREEN_FILTER),
    private val stylesheets: List<String> = emptyList()
) : HudElement(name, x, y, scale, allowedScreens) {
    // Made on first draw, GuiLib documents can't be made while the game is still starting
    private var hud: GuiLibHud? = null
    private var content: (NodeBuilder.() -> Unit)? = null
    private var contentChanged = false

    private var clickAction: (() -> Unit)? = null
    private var clickTarget: String? = null

    /** Runs [action] for a click on the element matching [selector] (CSS), or anywhere on the overlay without one. */
    fun onClick(selector: String? = null, action: () -> Unit) {
        clickTarget = selector
        clickAction = action
    }

    fun setCondition(condition: () -> Boolean): GuiLibOverlay {
        this.condition = condition
        return this
    }

    fun setExtraCondition(condition: () -> Boolean): GuiLibOverlay {
        this.extraCondition = condition
        return this
    }

    fun setContent(content: NodeBuilder.() -> Unit) {
        this.content = content
        contentChanged = true
    }

    // The size of the last drawn frame, GuiLib lays it out while drawing
    override fun contentWidth(): Int = ceil(hud?.width ?: 0f).toInt()

    override fun contentHeight(): Int = ceil(hud?.height ?: 0f).toInt()

    override fun overlayClicked(mouseX: Double, mouseY: Double) {
        val action = clickAction ?: return
        if (!World.isInSkyblock()) return
        if (!allowedScreens.any { it(mc.screen) }) return
        if (!isOverOverlay(mouseX, mouseY)) return
        val target = clickTarget ?: return action()
        val rect = hud?.bounds(target) ?: return
        val x = renderX + rect.x * scale
        val y = renderY + rect.y * scale
        if (mouseX >= x && mouseX <= x + rect.width * scale && mouseY >= y && mouseY <= y + rect.height * scale) action()
    }

    override fun render(drawContext: GuiGraphicsExtractor, mouseX: Double, mouseY: Double) {
        if (!checkCondition()) return
        val content = content ?: return
        val hud = hud ?: GuiLibHud(stylesheets).also { hud = it }
        if (contentChanged) {
            hud.render(content)
            contentChanged = false
        }

        updateRenderPosition(contentWidth(), contentHeight())
        hud.draw(drawContext, renderX, renderY, scale)

        if (!inEditingScreen()) return
        val width = (contentWidth() * scale).toInt()
        val height = (contentHeight() * scale).toInt()
        val left = renderX.toInt()
        val top = renderY.toInt()
        if (isOverOverlay(mouseX, mouseY)) drawContext.fill(left, top, left + width, top + height, Color(0, 0, 0, 100).rgb)
        if (selected) {
            drawContext.outline(left, top, width, height, Color(255, 0, 0, 170).rgb)
            drawContext.text(mc.font, "X: $left Y: $top Scale: ${String.format("%.1f", scale)}", left, top - mc.font.lineHeight - 1, Color(255, 255, 255, 200).rgb, true)
        }
    }
}
