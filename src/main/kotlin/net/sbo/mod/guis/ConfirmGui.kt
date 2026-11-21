package net.sbo.mod.guis

import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.useEscapeBack
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme

/** A small window that asks before doing something, e.g. resetting data, with a confirm and a reject button. */
object ConfirmGui {
    private val STYLES = listOf("sbo:ui/confirm/confirm.css", SboLook.STYLE)

    /**
     * Opens the window, must run on the client thread. Confirm or reject goes back to the screen that was open before
     * (see [screenAfter]) and then runs [onConfirm] or [onReject], Escape counts as reject.
     *
     * @param title the question, e.g. "Reset all achievements?"
     * @param text what happens on confirm, shown under the title
     * @param confirmText the confirm button
     * @param rejectText the reject button
     * @param danger true for something that can't be undone, the confirm button is red then
     * @param onReject runs after reject or Escape
     * @param onConfirm runs after confirm
     */
    fun open(
        title: String,
        text: String = "",
        confirmText: String = "Confirm",
        rejectText: String = "Cancel",
        danger: Boolean = false,
        onReject: () -> Unit = {},
        onConfirm: () -> Unit,
    ) {
        val props = Props(title, text, confirmText, rejectText, danger, onConfirm, onReject, mc.screen)
        // Only the window, the game stays visible behind it
        GuiLib.open(component("Confirm") { Window(props) }, STYLES, title, vanillaBackground = false)
    }

    /**
     * Asks before a reset: opens the window once the current screen (e.g. chat after a command) is closed, with a
     * red Reset button that runs [onReset].
     *
     * @param title the question, e.g. "Reset the session tracker?"
     * @param text what gets reset
     */
    fun confirmReset(title: String, text: String, onReset: () -> Unit) {
        mc.schedule { open(title, text, confirmText = "Reset", danger = true, onConfirm = onReset) }
    }

    /**
     * The screen to open when the window closes, [previous] was open before it: the same screen again, a new one for
     * the player's inventory, and none for a server container (a chest, a SkyBlock menu) since the server closed that
     * when the window opened.
     */
    private fun screenAfter(previous: Screen?): Screen? = when (previous) {
        is InventoryScreen -> mc.player?.let(::InventoryScreen)
        is AbstractContainerScreen<*> -> null
        else -> previous
    }

    /**
     * The arguments of [open].
     *
     * @property previous the screen that was open before the window, see [screenAfter]
     */
    private data class Props(
        val title: String,
        val text: String,
        val confirmText: String,
        val rejectText: String,
        val danger: Boolean,
        val onConfirm: () -> Unit,
        val onReject: () -> Unit,
        val previous: Screen?,
    )

    /** The window itself: the question, what happens and the two buttons. */
    private val Window = component<Props>("ConfirmWindow") { props ->
        useSboScale()
        useSboTheme("confirm")
        // After the theme, which turns the blur on with the Background Blur setting
        useBackgroundBlur(false)

        fun close(action: () -> Unit) {
            mc.setScreen(screenAfter(props.previous))
            action()
        }
        useEscapeBack { close(props.onReject) }

        div(className = "cf-window") {
            div(className = "cf-title") { +props.title }
            if (props.text.isNotEmpty()) div(className = "cf-text") { +props.text }
            div(className = "cf-buttons") {
                button(onClick = { close(props.onReject) }) { +props.rejectText }
                button(className = if (props.danger) "cf-confirm danger" else "cf-confirm", onClick = { close(props.onConfirm) }) {
                    +props.confirmText
                }
            }
        }
    }
}
