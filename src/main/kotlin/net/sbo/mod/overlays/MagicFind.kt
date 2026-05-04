package net.sbo.mod.overlays

import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.minecraft.ChatFormatting.*
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.data.SboDataObject.sboData

object MagicFind {
    val overlay = Overlay("Diana MagicFind", 10f, 10f, 1f).setCondition { Diana.magicFindTracker && (Helper.checkDiana() || Helper.hasSpade) }

    fun init() {
        overlay.init()
        updateLines()
    }

    fun updateLines() {
        val lines = mutableListOf<OverlayTextLine>()
        lines.addAll(
            listOf(
                OverlayTextLine("$YELLOW${BOLD}Diana MagicFind"),
                OverlayTextLine("$GRAY - ${LIGHT_PURPLE}Chimera: $AQUA${sboData.highestChimMagicFind}%"),
                OverlayTextLine("$GRAY - ${GOLD}Sticks: $AQUA${sboData.highestStickMagicFind}%")
            )
        )
        overlay.setLines(lines)
    }
}