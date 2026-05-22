package net.sbo.mod.utils.chat

import net.sbo.mod.SBOKotlin.mc
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.ChatFormatting
import net.sbo.mod.utils.events.ClickActionManager
import net.sbo.mod.settings.categories.General

object Chat {

    /**
     * Sends a text to party chat via the /pc command, or
     * sends it locally if the user opted-in to the "Assume Muted"
     * option for accessibility, so that they can still see the message
     * and/or possibly copy and share the message in another platform such as Discord.
     *
     * @param text The text to send to party chat if possible or otherwise locally.
     */
    fun pc(text: String) {
        if (General.assumeMuted) {
            // User is muted (or we assume it bc of the opt-in setting thats false by default)
            // trying send message will show up a huge output about the remaining mute time,
            // server rules and all that yapping with no other output about the text we want
            // to send, so send locally instead via the chat method. We can have color since it's regular chat.
            Chat.chat("§6[SBO] §e${text}")
        } else {
            Chat.command("pc ${text}")
        }
    }

    /**
     * Sends a command to the server.
     * This correctly simulates a player typing a command.
     * @param command The command to send, without the leading slash.
     */
    fun command(command: String) {
        if (!command.startsWith("/")) {
            mc.player?.connection?.sendChat("/$command")
        } else {
            mc.player?.connection?.sendChat(command)
        }
    }

    /**
     * Shows a local chat message only visible to the player.
     * @param string The message to display in the chat.
     */
    fun chat(string: String) {
        mc.execute {
            mc.gui.chat.addMessage(Component.nullToEmpty(string))
        }
    }

    /**
     * Shows a local chat message only visible to the player.
     * @param text The message to display in the chat.
     */
    fun chat(text: Component) {
        mc.execute {
            mc.gui.chat.addMessage(text)
        }
    }

    /**
     * Sends a message to the server chat.
     * @param message The message to send.
     */
    fun say(message: String) {
        mc.connection?.sendChat(message)
    }

    /**
     * Sends a clickable message to the player.
     *
     * @param message The text to display in the message.
     * @param hover The text to show when the player hovers over the message.
     * @param onClick The code to execute when the player clicks the message.
     */
    fun clickableChat(
        message: String,
        hover: String,
        onClick: () -> Unit,
    ) {
        val actionId = ClickActionManager.registerAction(onClick)
        val hoverText = Component.literal(hover).withStyle(ChatFormatting.YELLOW)

        val clickEvent = ClickEvent.RunCommand("/__sbo_run_clickable_action $actionId")
        val hoverEvent = HoverEvent.ShowText(hoverText)

        val styledText = Component.literal(message).setStyle(
            Style.EMPTY
                .withClickEvent(clickEvent)
                .withHoverEvent(hoverEvent)
        )

        mc.execute {
            mc.gui.chat.addMessage(styledText)
        }
    }

    /**
     * Sends a simple clickable chat message with a command action.
     * This version doesn't require a callback.
     * @param message The text to display in the message.
     * @param hover The text to show when the player hovers over the message.
     * @param command The command to execute when the player clicks the message.
     */
    fun clickableChat(
        message: String,
        hover: String,
        command: String
    ) {
        val hoverText = Component.literal(hover).withStyle(ChatFormatting.YELLOW)

        val clickEvent = ClickEvent.RunCommand(command)
        val hoverEvent = HoverEvent.ShowText(hoverText)

        val styledText = Component.literal(message).setStyle(
            Style.EMPTY
                .withClickEvent(clickEvent)
                .withHoverEvent(hoverEvent)
        )

        mc.execute {
            mc.gui.chat.addMessage(styledText)
        }
    }

    /**
     * Sends a chat message with multiple text components.
     * This is useful for combining multiple Text objects into one message.
     * @param textComponents The list of Text components to combine and send.
     */
    fun chat(vararg textComponents: Component) {
        if (textComponents.isEmpty()) return

        val combinedText = Component.literal("")

        textComponents.forEach { component ->
            combinedText.append(component)
        }

        mc.execute {
            mc.gui.chat.addMessage(combinedText)
        }
    }

    /**
     * Creates a styled text component with optional hover and click events.
     * @param message The main text of the component.
     * @param hover Optional hover text to show when the player hovers over the component.
     * @param command Optional command to run when the player clicks the component.
     * @return A Text object with the specified style and events.
     */
    fun textComponent(
        message: String,
        hover: String? = null,
        command: String? = null
    ): Component {
        val styledText = Component.literal(message).setStyle(
            Style.EMPTY
        )

        if (hover != null) {
            val hoverText = Component.literal(hover).withStyle(ChatFormatting.YELLOW)
            styledText.style = styledText.style.withHoverEvent(HoverEvent.ShowText(hoverText))
        }

        if (command != null) {
            val clickEvent = ClickEvent.RunCommand(command)
            styledText.style = styledText.style.withClickEvent(clickEvent)
        }

        return styledText
    }

    /**
     * Gets a message that fills one line of chat by repeating the separator.
     * @param separator The string to repeat. Defaults to "-".
     * @return The message string that fills the chat line.
     */
    fun getChatBreak(separator: String = "-", colorcodes: String = "§b"): String {
        if (separator.isEmpty()) {
            return ""
        }
        val textRenderer = mc.font
        val chatWidth = mc.gui.chat.width
        val separatorWidth = textRenderer.width(separator)

        if (separatorWidth <= 0) {
            return ""
        }

        val repeatCount = chatWidth / separatorWidth
        return colorcodes + separator.repeat(repeatCount)
    }
}
