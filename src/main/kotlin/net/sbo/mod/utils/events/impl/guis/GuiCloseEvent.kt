package net.sbo.mod.utils.events.impl.guis

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/**
 * Event fired when a GUI screen is closed.
 * @param client The Minecraft client instance.
 * @param screen The screen that is being closed.
 */
class GuiCloseEvent(
    val client: Minecraft,
    val screen: Screen,
)
