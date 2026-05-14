package net.sbo.mod.utils.events.impl.guis

import net.minecraft.client.gui.screens.Screen

/**
 * Event fired when a GUI mouse click occurs.
 * @param screen The screen where the mouse click happened.
 * @param mouseX The X coordinate of the mouse click.
 * @param mouseY The Y coordinate of the mouse click.
 * @param button The mouse button that was clicked.
 */
class GuiMouseClickAfter (
    val screen: Screen,
    val mouseX: Double,
    val mouseY: Double,
    val button: Int
)