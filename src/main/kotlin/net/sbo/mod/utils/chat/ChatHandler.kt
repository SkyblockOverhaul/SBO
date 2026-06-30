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

    fun registerHandler(pattern: Pattern, action: (Component, Matcher) -> Boolean) {
        messageHandlers.add(ChatRule(pattern, action))
    }

    private fun processMessage(message: Component): Boolean {
        val messageString = message.formattedString().replace("§r", "")
        if (Debug.debugMessages && !messageString.contains("❈ Defense")) {
            println("Processing chat message: $messageString")
        }

        var allowMessage = true

        messageHandlers.forEach { rule ->
            val matcher = rule.pattern.matcher(messageString)
            if (matcher.find()) {
                val result = rule.action(message, matcher)
                if (!result) allowMessage = false
            }
        }

        return allowMessage
    }

    private data class ChatRule(
        val pattern: Pattern,
        val action: (Component, Matcher) -> Boolean
    )
}