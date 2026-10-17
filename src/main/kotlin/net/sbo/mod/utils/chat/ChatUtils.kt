package net.sbo.mod.utils.chat

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.*
import java.util.*

object ChatUtils {
    private val colorToFormatChar: Map<TextColor, ChatFormatting> = ChatFormatting.entries.mapNotNull { format ->
        TextColor.fromLegacyFormat(format)?.let { it to format }
    }.toMap()

    private fun getColorFormatChar(color: TextColor): Char? {
        val formatting = colorToFormatChar[color]
        return formatting?.char
    }

    private fun Style.getFormatCodes() = buildString {
        this@getFormatCodes.color?.let(ChatUtils::getColorFormatChar)?.run { append("§").append(this) }

        if (this@getFormatCodes.isBold) append("§l")
        if (this@getFormatCodes.isItalic) append("§o")
        if (this@getFormatCodes.isUnderlined) append("§n")
        if (this@getFormatCodes.isStrikethrough) append("§m")
        if (this@getFormatCodes.isObfuscated) append("§k")
    }

    fun Component.formattedString(): String {
        val builder = StringBuilder()

        this.visit(
            { style, content ->
                builder.append(style.getFormatCodes())
                builder.append(content)
                Optional.empty<Any>()
            },
            Style.EMPTY
        )
        return builder.toString()
    }

    internal fun Component.getShowTextHoverEvent(): HoverEvent? {
        val hover = this.style.hoverEvent ?: return null
        if (hover is HoverEvent.ShowText) {
            return HoverEvent.ShowText(hover.value())
        }
        return null
    }

    internal fun String.toStyledText(click: ClickEvent?, hover: HoverEvent?): Component {
        return Component.literal(this).setStyle(
            Style.EMPTY
                .withClickEvent(click)
                .withHoverEvent(hover)
        )
    }

    internal fun Component.toClickableText(command: String): Component {
        val content = this.formattedString()
        val hover = this.getShowTextHoverEvent()
        val click = ClickEvent.RunCommand(command)
        return content.toStyledText(click, hover)
    }
}
