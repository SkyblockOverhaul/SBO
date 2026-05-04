package net.sbo.mod.overlays

import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.minecraft.ChatFormatting.*
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.data.SboDataObject.sboData

object DianaStats {
    val overlay = Overlay("Diana Stats", 10f, 10f, 1f).setCondition { Diana.statsTracker && (Helper.checkDiana() || Helper.hasSpade)}

    fun init() {
        overlay.init()
        updateLines()
    }

    fun updateLines() {
        val lines = mutableListOf<OverlayTextLine>()
        lines.addAll(
            listOf(
                OverlayTextLine("$YELLOW${BOLD}Diana Stats"),
                OverlayTextLine("$GRAY - ${RED}Mobs since King: $AQUA${sboData.mobsSinceKing}"),
                OverlayTextLine("$GRAY - ${RED}Kings since Wool: $AQUA${sboData.kingSinceWool}$GRAY, ${RED}since §7[§bLS§7]: $AQUA${sboData.kingSinceLsWool}"),
                OverlayTextLine("$GRAY - ${RED}Mobs since Manti: $AQUA${sboData.mobsSinceManti}"),
                OverlayTextLine("$GRAY - ${RED}Mantis since Core: $AQUA${sboData.mantiSinceCore}$GRAY, ${RED}since §7[§bLS§7]: $AQUA${sboData.mantiSinceLsCore}"),
                OverlayTextLine("$GRAY - ${RED}Mantis since Stinger: $AQUA${sboData.mantiSinceStinger}$GRAY, ${RED}since §7[§bLS§7]: $AQUA${sboData.mantiSinceLsStinger}"),
                OverlayTextLine("$GRAY - ${LIGHT_PURPLE}Mobs since Inq: $AQUA${sboData.mobsSinceInq}"),
                OverlayTextLine("$GRAY - ${LIGHT_PURPLE}Inqs since Chimera: $AQUA${sboData.inqsSinceChim}$GRAY, ${LIGHT_PURPLE}since §7[§bLS§7]: $AQUA${sboData.inqsSinceLsChim}"),
                OverlayTextLine("$GRAY - ${LIGHT_PURPLE}Mobs since Sphinx: $AQUA${sboData.mobsSinceSphinx}"),
                OverlayTextLine("$GRAY - ${DARK_PURPLE}Sphinxes since Food: $AQUA${sboData.sphinxSinceFood}$GRAY, ${DARK_PURPLE}since §7[§bLS§7]: $AQUA${sboData.sphinxSinceLsFood}"),
                OverlayTextLine("$GRAY - ${DARK_PURPLE}Champs since Relic: $AQUA${Helper.formatNumber(sboData.champsSinceRelic, true)}"),
                OverlayTextLine("$GRAY - ${GOLD}Minos since Stick: $AQUA${Helper.formatNumber(sboData.minotaursSinceStick, true)}"),
            )
        )
        overlay.setLines(lines)
    }
}