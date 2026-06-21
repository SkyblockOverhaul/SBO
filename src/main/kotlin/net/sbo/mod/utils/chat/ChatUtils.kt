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

    /**
     * Taken from https://github.com/Synnerz/devonian/blob/127e982052f690353b435d01f567ce36f02facab/src/main/kotlin/com/github/synnerz/devonian/utils/StringUtils.kt#L119-L171 under GNU General Public License v3.0
     */
    fun fromLegacy(string: String): Component {
        val component = Component.literal("")
        var oldStr = ""
        var hadSS = false
        var style = Style.EMPTY

        for (char in string) {
            if (char == '§') {
                hadSS = true
                if (oldStr.isNotEmpty()) {
                    component.append(Component.literal(oldStr).withStyle(style))
                    oldStr = ""
                }
                continue
            }
            if (hadSS && (char.isLetter() || char.isDigit())) {
                hadSS = false

                style = when (char) {
                    'l' -> style.withBold(true)
                    'o' -> style.withItalic(true)
                    'n' -> style.withUnderlined(true)
                    'm' -> style.withStrikethrough(true)
                    'k' -> style.withObfuscated(true)
                    //#if MC >= 26.2
                    //$ '0' -> Style.EMPTY.withColor(TextColor.BLACK)
                    //$ '1' -> Style.EMPTY.withColor(TextColor.DARK_BLUE)
                    //$ '2' -> Style.EMPTY.withColor(TextColor.DARK_GREEN)
                    //$ '3' -> Style.EMPTY.withColor(TextColor.DARK_AQUA)
                    //$ '4' -> Style.EMPTY.withColor(TextColor.DARK_RED)
                    //$ '5' -> Style.EMPTY.withColor(TextColor.DARK_PURPLE)
                    //$ '6' -> Style.EMPTY.withColor(TextColor.GOLD)
                    //$ '7' -> Style.EMPTY.withColor(TextColor.GRAY)
                    //$ '8' -> Style.EMPTY.withColor(TextColor.DARK_GRAY)
                    //$ '9' -> Style.EMPTY.withColor(TextColor.BLUE)
                    //$ 'a' -> Style.EMPTY.withColor(TextColor.GREEN)
                    //$ 'b' -> Style.EMPTY.withColor(TextColor.AQUA)
                    //$ 'c' -> Style.EMPTY.withColor(TextColor.RED)
                    //$ 'd' -> Style.EMPTY.withColor(TextColor.LIGHT_PURPLE)
                    //$ 'e' -> Style.EMPTY.withColor(TextColor.YELLOW)
                    //$ 'f' -> Style.EMPTY.withColor(TextColor.WHITE)
                    //#endif
                    'r' -> Style.EMPTY
                    else -> Style.EMPTY
                }
                continue
            }

            oldStr += char
        }
        if (oldStr.isNotEmpty())
            component.append(Component.literal(oldStr).withStyle(style))

        return component
    }
}
