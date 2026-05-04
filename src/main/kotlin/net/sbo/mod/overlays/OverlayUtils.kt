package net.sbo.mod.overlays

import net.minecraft.ChatFormatting
import net.minecraft.ChatFormatting.UNDERLINE
import net.sbo.mod.utils.overlay.OverlayTextLine

object OverlayUtils {
    data class LootItemData(
        val id: String,
        val name: String,
        val color: ChatFormatting,
        val combined: Boolean = false,
        val dropMobId: String? = null,
        val dropMobLsId: String? = null,
        val isRarerDrop: Boolean = false
    )

    internal fun createClickableTextLine(
        text: String,
        hoverText: String? = null,
        defaultText: String? = null,
        onClick: () -> Unit = {},
        onMouseEnter: (() -> Unit)? = null,
        onMouseLeave: (() -> Unit)? = null,
        lineBreak: Boolean = true
    ): OverlayTextLine {
        val line = OverlayTextLine(text, linebreak = lineBreak).onClick(onClick)
        val enterAction = onMouseEnter ?: { line.text = hoverText ?: "$text$UNDERLINE" }
        val leaveAction = onMouseLeave ?: { line.text = defaultText ?: text }
        line.onMouseEnter(enterAction)
        line.onMouseLeave(leaveAction)
        return line
    }
}