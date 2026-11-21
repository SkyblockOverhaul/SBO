package net.sbo.mod.utils

import net.minecraft.network.chat.Component

/** Text helpers: § formatting, player names, casing and joining lines. */
object TextUtil {
    private val PLAYER_NAME = Regex("^[A-Za-z0-9_]{1,16}$")
    private val NON_ALPHANUMERIC = Regex("[^a-zA-Z0-9_]")

    /** A valid Minecraft name, so putting it into a command can't add anything else. */
    fun isPlayerName(name: String): Boolean = PLAYER_NAME.matches(name)

    fun getPlayerName(player: String): String {
        var name = player
        val num = name.indexOf(']')
        if (num != -1) {
            name = name.substring(num + 2)
        }
        name = name.removeFormatting()
        name = name.replace(NON_ALPHANUMERIC, "")
        return name.trim()
    }

    /**
     * Adapted from SkyHanni code, uses performant StringBuilder and native String operations rather than using a Regex to remove all formatting characters and the characters coming directly after formatting characters.
     */
    fun String.removeFormatting(): String {
        val length = length
        if (length == 0) return ""

        var nextFormattingSequence = indexOf('§')
        if (nextFormattingSequence == -1) return this

        val cleanedString = StringBuilder(length - 1)
        var readIndex = 0

        while (nextFormattingSequence != -1) {
            cleanedString.append(this, readIndex, nextFormattingSequence)

            readIndex = minOf(nextFormattingSequence + 2, length)
            nextFormattingSequence = indexOf('§', readIndex)
        }

        cleanedString.append(this, readIndex, length)
        return cleanedString.toString()
    }

    fun Component.removeFormatting(): String = this.string.removeFormatting()

    fun toUpperSnakeCase(input: String): String = input.replace("-", " ").split(" ").joinToString("_") { it.uppercase() }

    /** A message setting's lines as one chat message: the lines joined with spaces, blank lines skipped. */
    fun joinLines(lines: Array<out String>): String =
        lines.asSequence().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

    fun toTitleCase(input: String): String = input.lowercase().replaceFirstChar { char -> char.uppercase() }
}
