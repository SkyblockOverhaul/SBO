package net.sbo.mod.utils.events.impl.guis

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/**
 * Event fired when a GUI screen is closed.
 * @param client The Minecraft client instance.
 * @param screen The screen that is being closed.
 * @param scaledWidth The width of the screen in scaled pixels.
 * @param scaledHeight The height of the screen in scaled pixels.
 */
class GuiCloseEvent(
    val client: Minecraft,
    val screen: Screen,
    private val scaledWidth: Int,
    private val scaledHeight: Int
)