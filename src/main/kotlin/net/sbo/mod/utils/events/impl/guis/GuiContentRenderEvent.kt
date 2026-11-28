package net.sbo.mod.utils.events.impl.guis

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen

/**
 * Fired when a menu or a sign screen has drawn its content and before the deferred item tooltip is drawn.
 * Things drawn here stay under the tooltip, unlike [GuiPostRenderEvent].
 *
 * @param screen the menu or sign screen
 * @param context the drawing context
 * @param mouseX the X position of the mouse
 * @param mouseY the Y position of the mouse
 */
class GuiContentRenderEvent(
    val screen: Screen,
    val context: GuiGraphicsExtractor,
    val mouseX: Int,
    val mouseY: Int,
)
