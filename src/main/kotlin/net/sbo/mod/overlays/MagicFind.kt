package net.sbo.mod.overlays

import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.minecraft.ChatFormatting.*
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.data.SboDataObject.sboData
import net.sbo.mod.utils.events.Register
import kotlin.Pair

object MagicFind {
    val overlay = Overlay("Diana MagicFind", 10f, 10f, 1f).setCondition { Diana.magicFindTracker && (Helper.checkDiana() || Helper.hasSpade) }
    private var dirty = false

    fun init() {
        overlay.init()
        updateLines()
        Register.onTick(1) { flushUpdateLines() }
    }

    fun updateLines() {
        dirty = true
    }

    fun flushUpdateLines() {
        if (!dirty) {
            return
        }

        val lines = mutableListOf<OverlayTextLine>()
        lines.addAll(
            listOf(
                OverlayTextLine("$YELLOW${BOLD}Diana MagicFind"),
                OverlayTextLine("$GRAY - ${LIGHT_PURPLE}Chimera: $AQUA${sboData.highestChimMagicFind}%"),
                OverlayTextLine("$GRAY - ${GOLD}Sticks: $AQUA${sboData.highestStickMagicFind}%")
            )
        )
        overlay.setLines(lines)

        dirty = false
    }
}
