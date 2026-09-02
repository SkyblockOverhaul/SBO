package net.sbo.mod.utils.chat

import net.minecraft.network.chat.Component
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.utils.chat.ChatUtils.formattedString
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.ChatMessageAllowEvent
import java.util.regex.Matcher
import java.util.regex.Pattern

object ChatHandler {

    private val messageHandlers = mutableListOf<ChatRule>()
    private val spammyPattern = Regex("§[0-9a-fk-or].+[0-9,]+/[0-9,]+❤.*")

    @SboEvent
    fun onAllowMessage(event: ChatMessageAllowEvent) {
        if (spammyPattern.matches(event.message.string)) {
            event.isAllowed = true
            return
        }

        event.isAllowed = processMessage(event.message)
    }

    fun registerHandler(
        pattern: Pattern,
        action: (Component, Matcher) -> Boolean
    ) {
        messageHandlers.add(
            ChatRule(
                pattern = pattern,
                action = { message, matcher, _ ->
                    action(message, matcher)
                }
            )
        )
    }

    fun registerHandler(
        pattern: Pattern,
        action: (
            Component,
            Matcher,
            () -> Unit
        ) -> Boolean
    ) {
        messageHandlers.add(
            ChatRule(
                pattern = pattern,
                action = action
            )
        )
    }

    private fun processMessage(message: Component): Boolean {
        val messageString = message.formattedString().replace("§r", "")

        if (Debug.debugOnlyMessages && "❈ Defense" !in messageString) {
            println("Processing chat message: $messageString")
        }

        var allowMessage = true

        val iterator = messageHandlers.iterator()

        while (iterator.hasNext()) {
            val rule = iterator.next()
            val matcher = rule.pattern.matcher(messageString)

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

        return allowMessage
    }

    private data class ChatRule(
        val pattern: Pattern,
        val action: (
            message: Component,
            matcher: Matcher,
            unregister: () -> Unit
        ) -> Boolean
    )
}
