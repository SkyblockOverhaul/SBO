package net.sbo.mod.utils.chat

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor

private val LEGACY_CODES: Map<TextColor, ChatFormatting> = ChatFormatting.entries.mapNotNull { format ->
    TextColor.fromLegacyFormat(format)?.let { it to format }
}.toMap()

/**
 * The text with § codes, exactly like UniversalCraft's `toFormattedString` (the chat and item regexes rely on it): every
 * style change writes `§r`, the legacy color if there is one, then only the first of bold, italic, underline,
 * strikethrough and obfuscated.
 */
fun Component.toFormattedString(): String {
    val builder = StringBuilder()
    var lastStyle: Style? = null
    visualOrderText.accept { _, style, codePoint ->
        if (style != lastStyle) {
            lastStyle = style
            builder.append("§r")
            style.color?.let(LEGACY_CODES::get)?.let { builder.append(it) }
            when {
                style.isBold -> builder.append("§l")
                style.isItalic -> builder.append("§o")
                style.isUnderlined -> builder.append("§n")
                style.isStrikethrough -> builder.append("§m")
                style.isObfuscated -> builder.append("§k")
            }
        }
        builder.append(codePoint.toChar())
        true
    }
    return builder.toString()
}
