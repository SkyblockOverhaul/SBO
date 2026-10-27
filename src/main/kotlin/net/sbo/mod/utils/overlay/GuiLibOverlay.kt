package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.sbo.guilib.core.dom.Element
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.fabric.GuiLibHud
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.render.RenderUtils2D
import kotlin.math.ceil

/**
 * An overlay built with GuiLib (tags + CSS) instead of text lines, moved and scaled in /sboguis like the others.
 * Call [setContent] whenever the shown data changes; drawing reuses the last content every frame, so it stays cheap.
 * It gets no mouse input of its own. In the [allowedScreens], [onClick] runs for a click on it (or on one element of it),
 * [onElementClick] and [tooltip] for the element with an id under the mouse.
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
    private var elementClick: ((id: String) -> Unit)? = null
    private var tooltip: ((id: String) -> String?)? = null

    /** Runs [action] for a click on the element matching [selector] (CSS), or anywhere on the overlay without one. */
    fun onClick(selector: String? = null, action: () -> Unit) {
        clickTarget = selector
        clickAction = action
    }

    /** Runs [action] with the id of the clicked element (the innermost one with an id), not for clicks outside one. */
    fun onElementClick(action: (id: String) -> Unit) {
        elementClick = action
    }

    /** Shows the text [text] gives for the hovered element (the innermost one with an id) as a tooltip, none for null. */
    fun tooltip(text: (id: String) -> String?) {
        tooltip = text
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
        if (!World.isInSkyblock()) return
        if (!allowedScreens.any { it(mc.screen) }) return
        if (!isOverOverlay(mouseX, mouseY)) return
        elementClick?.let { click -> idAt(mouseX, mouseY)?.let(click) }
        val action = clickAction ?: return
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

        if (!inEditingScreen()) {
            drawTooltip(drawContext, mouseX, mouseY)
            return
        }
        val width = (contentWidth() * scale).toInt()
        val height = (contentHeight() * scale).toInt()
        val left = renderX.toInt()
        val top = renderY.toInt()
        OverlayEditorStyle.draw(drawContext, left, top, width, height, scale, isOverOverlay(mouseX, mouseY), selected)
    }

    private fun drawTooltip(drawContext: GuiGraphicsExtractor, mouseX: Double, mouseY: Double) {
        val tooltip = tooltip ?: return
        if (!allowedScreens.any { it(mc.screen) } || !isOverOverlay(mouseX, mouseY)) return
        val text = idAt(mouseX, mouseY)?.let(tooltip) ?: return
        RenderUtils2D.drawHoveringString(drawContext, text, mouseX, mouseY, mc.font)
    }

    // The id of the innermost element with one at that GUI position, from the last drawn frame
    private fun idAt(mouseX: Double, mouseY: Double): String? {
        val hud = hud ?: return null
        // The content is drawn from the top left of the document, at the overlay's scale
        var element: Element? = hud.root.painter.hitTest(((mouseX - renderX) / scale).toFloat(), ((mouseY - renderY) / scale).toFloat())
        while (element != null) {
            element.id?.let { return it }
            element = element.parent
        }
        return null
    }
}
