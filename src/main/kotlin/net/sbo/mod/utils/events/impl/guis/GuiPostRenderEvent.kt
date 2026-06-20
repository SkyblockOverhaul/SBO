package net.sbo.mod.utils.events.impl.guis

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen

/**
 * Event fired after a GUI screen has been rendered.
 * @param screen The screen that has been rendered.
 * @param context The drawing context used for rendering.
 * @param mouseX The current X position of the mouse.
 * @param mouseY The current Y position of the mouse.
 * @param delta The partial tick time.
 */
class GuiPostRenderEvent(
    val screen: Screen,
    val context: GuiGraphicsExtractor,
    val mouseX: Int,
    val mouseY: Int,
    val delta: Float
)
