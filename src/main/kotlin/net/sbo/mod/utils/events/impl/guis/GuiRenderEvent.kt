package net.sbo.mod.utils.events.impl.guis

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen

/**
 * Event fired when a GUI is rendered.
 * @param client The Minecraft client instance.
 * @param screen The screen that is being rendered.
 * @param context The drawing context used for rendering.
 * @param mouseX The current X position of the mouse.
 * @param mouseY The current Y position of the mouse.
 * @param delta The partial tick time.
 */
class GuiRenderEvent(
    val client: Minecraft,
    val screen: Screen,
    val context: GuiGraphics,
    val mouseX: Int,
    val mouseY: Int,
    val delta: Float
)
