package net.sbo.mod.utils.chat

import net.sbo.mod.SBOKotlin.mc
import net.minecraft.text.Text
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.util.Formatting
import net.sbo.mod.utils.events.ClickActionManager

object Chat {

    /**
     * Sends a command to the server.
     * This correctly simulates a player typing a command.
     * @param command The command to send, without the leading slash.
     */
    fun command(command: String) {
        if (!command.startsWith("/")) {
            mc.player?.networkHandler?.sendChatMessage("/$command")
        } else {
            mc.player?.networkHandler?.sendChatMessage(command)
        }
    }

    /**
     * Shows a local chat message only visible to the player.
     * @param string The message to display in the chat.
     */
    fun chat(string: String) {
        mc.execute {
            mc.inGameHud.chatHud.addMessage(Text.of(string))
        }
    }

    /**
     * Shows a local chat message only visible to the player.
     * @param text The message to display in the chat.
     */
    fun chat(text: Text) {
        mc.execute {
            mc.inGameHud.chatHud.addMessage(text)
        }
    }

    /**
     * Sends a message to the server chat.
     * @param message The message to send.
     */
    fun say(message: String) {
        mc.networkHandler?.sendChatMessage(message)
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
        val hoverText = Text.literal(hover).formatted(Formatting.YELLOW)

        val clickEvent = ClickEvent.RunCommand("/__sbo_run_clickable_action $actionId")
        val hoverEvent = HoverEvent.ShowText(hoverText)

        val styledText = Text.literal(message).setStyle(
            Style.EMPTY
                .withClickEvent(clickEvent)
                .withHoverEvent(hoverEvent)
        )

        mc.execute {
            mc.inGameHud.chatHud.addMessage(styledText)
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
        val hoverText = Text.literal(hover).formatted(Formatting.YELLOW)

        val clickEvent = ClickEvent.RunCommand(command)
        val hoverEvent = HoverEvent.ShowText(hoverText)

        val styledText = Text.literal(message).setStyle(
            Style.EMPTY
                .withClickEvent(clickEvent)
                .withHoverEvent(hoverEvent)
        )

        mc.execute {
            mc.inGameHud.chatHud.addMessage(styledText)
        }
    }

    /**
     * Sends a chat message with multiple text components.
     * This is useful for combining multiple Text objects into one message.
     * @param textComponents The list of Text components to combine and send.
     */
    fun chat(vararg textComponents: Text) {
        if (textComponents.isEmpty()) return

        val combinedText = Text.literal("")

        textComponents.forEach { component ->
            combinedText.append(component)
        }

        mc.execute {
            mc.inGameHud.chatHud.addMessage(combinedText)
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
    ): Text {
        val styledText = Text.literal(message).setStyle(
            Style.EMPTY
        )

        if (hover != null) {
            val hoverText = Text.literal(hover).formatted(Formatting.YELLOW)
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
        val textRenderer = mc.textRenderer
        val chatWidth = mc.inGameHud.chatHud.width
        val separatorWidth = textRenderer.getWidth(separator)

        if (separatorWidth <= 0) {
            return ""
        }

        val repeatCount = chatWidth / separatorWidth
        return colorcodes + separator.repeat(repeatCount)
    }
}