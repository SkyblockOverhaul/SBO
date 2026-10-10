package net.sbo.mod.utils.chat

import net.minecraft.network.chat.Component
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.chat.ChatUtils.formattedString
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.ChatMessageAllowEvent
import java.util.regex.Matcher
import java.util.regex.Pattern

object ChatHandler {

    private val messageHandlers = mutableListOf<ChatRule>()
    // Can't cancel; they only see messages no handler canceled, like before when each had its own callback
    private val listeners = mutableListOf<ChatListener>()

    @SboEvent
    fun onAllowMessage(event: ChatMessageAllowEvent) {
        event.isAllowed = processMessage(event.message)
    }

    fun registerListener(regex: Regex, noFormatting: Boolean, action: (Component, MatchResult) -> Unit) {
        listeners.add(ChatListener(regex, noFormatting, action))
    }

    fun registerHandler(
        pattern: Pattern,
        noFormatting: Boolean = false,
        action: (Component, Matcher) -> Boolean
    ) {
        messageHandlers.add(
            ChatRule(
                pattern = pattern,
                noFormatting = noFormatting,
                action = { message, matcher, _ ->
                    action(message, matcher)
                }
            )
        )
    }

    fun registerHandler(
        pattern: Pattern,
        noFormatting: Boolean = false,
        action: (
            Component,
            Matcher,
            () -> Unit
        ) -> Boolean
    ) {
        messageHandlers.add(
            ChatRule(
                pattern = pattern,
                noFormatting = noFormatting,
                action = action
            )
        )
    }

    // The component tree is walked once per message, every handler and listener matches on these strings
    private fun processMessage(message: Component): Boolean {
        val formatted = message.formattedString()
        val messageString = formatted.replace("§r", "")
        val plain by lazy { messageString.removeFormatting() }

        if (Debug.debugOnlyMessages && "❈ Defense" !in messageString) {
            println("Processing chat message: $messageString")
        }

        var allowMessage = true

        val iterator = messageHandlers.iterator()

        while (iterator.hasNext()) {
            val rule = iterator.next()
            val matcher = rule.pattern.matcher(if (rule.noFormatting) plain else messageString)

            if (!matcher.find()) {
                continue
            }

            var unregister = false

            val result = rule.action(message, matcher) {
                unregister = true
            }

            if (!result) {
                allowMessage = false

                if (unregister) {
                    iterator.remove()
                }
            }
        }

        if (allowMessage) {
            for (listener in listeners) {
                // Listeners match the text with §r, as they always did
                listener.regex.find(if (listener.noFormatting) plain else formatted)?.let { listener.action(message, it) }
            }
        }

        return allowMessage
    }

    private class ChatListener(val regex: Regex, val noFormatting: Boolean, val action: (Component, MatchResult) -> Unit)

    private data class ChatRule(
        val pattern: Pattern,
        val noFormatting: Boolean,
        val action: (
            message: Component,
            matcher: Matcher,
            unregister: () -> Unit
        ) -> Boolean
    )
}
