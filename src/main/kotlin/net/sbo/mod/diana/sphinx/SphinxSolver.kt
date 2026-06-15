package net.sbo.mod.diana.sphinx

import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.Component
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.chat.ChatUtils.formattedString
import net.sbo.mod.utils.chat.ChatUtils.getShowTextHoverEvent
import net.sbo.mod.utils.chat.ChatUtils.toClickableText
import net.sbo.mod.utils.chat.ChatUtils.toStyledText
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiMouseClickBefore
import java.util.regex.Pattern

object SphinxSolver {
    private var currentSession: SphinxSession? = null

    fun init() {
        detectQuestion()
    }

    @SboEvent
    fun onGuiMouseClick(event: GuiMouseClickBefore) {
        if (!Diana.sphinxSolver) return

        val index = currentSession?.correctAnswersIndex ?: return
        if (event.button != 0) return
        if (index == -1) return
        if (event.screen !is ChatScreen) return
        Chat.command("/sphinxanswer $index")
        currentSession = null
    }


    fun detectQuestion() {
        Register.onChatMessageCancelable(
            Pattern.compile("^(.*?)$", Pattern.DOTALL)
        ) { message, matchResult ->
            if (!Diana.sphinxSolver) return@onChatMessageCancelable true
            val questionText = matchResult.group(1).trim()
            for (sphinxQuestion in SphinxQuestions.QUESTIONS) {
                if (sphinxQuestion.question.equals(questionText.removeFormatting(), ignoreCase = true)) {
                    Helper.sleep(100) {
                        Chat.chat("§6[SBO] §bClick anywhere on the screen to answer while the chat is open.")
                    }
                }
            }
            true
        }

        Register.onChatMessageCancelable(
            Pattern.compile("^§7 {3}([ABC])\\) §f(.*?)$")
        ) { msg, matcher ->
            if (!Diana.sphinxSolver) return@onChatMessageCancelable true

            val letter = matcher.group(1)
            val possibleAnswer = matcher.group(2).trim()
            val index = letterToIndex(letter)

            val isCorrect = possibleAnswer in SphinxQuestions.CORRECT_ANSWERS
            val session = currentSession ?: SphinxSession().also { currentSession = it }

            if (isCorrect) {
                session.correctAnswersIndex = index
            }

            val formattedMsg = msg.createStyledAnswerText(isCorrect)
            session.answerTexts[letter] = formattedMsg

            if (session.isComplete()) {
                handleSessionComplete(session)
            }
            false
        }
    }

    private fun letterToIndex(letter: String): Int = when (letter) {
        "A" -> 0
        "B" -> 1
        "C" -> 2
        else -> -1
    }

    private fun handleSessionComplete(session: SphinxSession) {
        if (session.correctAnswersIndex == -1) return

        for (msg in session.answerTexts.values) {
            val clickableText = msg.toClickableText("/sphinxanswer ${session.correctAnswersIndex}")
            Chat.chat(clickableText)
        }
    }

    private fun Component.createStyledAnswerText(isCorrect: Boolean): Component {
        val newColorCode = if (isCorrect) "§a§n" else "§c"
        val newString = this.formattedString().replace("§f", newColorCode)
        val originalHoverEvent = this.getShowTextHoverEvent()
        return newString.toStyledText(click = null, hover = originalHoverEvent)
    }
}
