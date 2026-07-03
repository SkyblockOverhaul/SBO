package net.sbo.mod.overlays

import net.minecraft.ChatFormatting.*
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.data.SboDataObject.sboData
import net.sbo.mod.utils.overlay.DirtyFlushableOverlay
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine

object MagicFind : DirtyFlushableOverlay() {
    override val overlay = Overlay("Diana MagicFind", 10f, 10f).setCondition { Diana.magicFindTracker && (Helper.checkDiana() || Helper.hasSpade) }

    fun init() {
        overlay.init()
        updateLines()
    }

    override fun generateLines(): List<OverlayTextLine> {
        return listOf(
            OverlayTextLine("$YELLOW${BOLD}Diana MagicFind"),
            OverlayTextLine("$GRAY - ${RED}Wool: $AQUA${sboData.highestWoolMagicFind}%"),
            OverlayTextLine("$GRAY - ${RED}Manticore: $AQUA${sboData.highestCoreMagicFind}%"),
            OverlayTextLine("$GRAY - ${RED}Stinger: $AQUA${sboData.highestStingerMagicFind}%"),
            OverlayTextLine("$GRAY - ${LIGHT_PURPLE}Chimera: $AQUA${sboData.highestChimMagicFind}%"),
            OverlayTextLine("$GRAY - ${DARK_PURPLE}Food: $AQUA${sboData.highestFoodMagicFind}%"),
            OverlayTextLine("$GRAY - ${DARK_PURPLE}Relic: $AQUA${sboData.highestRelicMagicFind}%"),
            OverlayTextLine("$GRAY - ${GOLD}Stick: $AQUA${sboData.highestStickMagicFind}%")
        )
    }
}
