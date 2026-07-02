package net.sbo.mod.overlays

import net.minecraft.ChatFormatting.*
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.data.SboDataObject.SBOConfigBundle
import net.sbo.mod.utils.data.SboDataObject.sboData
import net.sbo.mod.utils.overlay.CHAT_SCREEN_FILTER
import net.sbo.mod.utils.overlay.CRAFTING_PLAYER_INVENTORY_FILTER
import net.sbo.mod.utils.overlay.DirtyFlushableOverlay
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.sbo.mod.utils.overlay.isCraftingScreenOpen

object DianaStats : DirtyFlushableOverlay() {
    override val overlay = Overlay("Diana Stats", 10f, 10f,
        allowedScreens = listOf(CHAT_SCREEN_FILTER, CRAFTING_PLAYER_INVENTORY_FILTER)
    ).setCondition { Diana.statsTracker && (Helper.checkDiana() || Helper.hasSpade) }

    fun init() {
        overlay.init()
        updateLines()
    }

    private fun createStatLine(name: String, formattedText: String): OverlayTextLine {
        val line = OverlayTextLine(formattedText).onClick {
            if (!isCraftingScreenOpen()) return@onClick
            val hideList = SBOConfigBundle.sboData.hideTrackerLines
            if (hideList.contains(name)) {
                hideList.remove(name)
            } else {
                hideList.add(name)
            }
            updateLines()
        }.setCondition {
            val meetsManualHideCondition = !(!isCraftingScreenOpen() && SBOConfigBundle.sboData.hideTrackerLines.contains(name))
            meetsManualHideCondition
        }
        if (SBOConfigBundle.sboData.hideTrackerLines.contains(name)) {
            line.text = "$GRAY$STRIKETHROUGH${formattedText.removeFormatting()}"
        }
        return line
    }

    override fun generateLines(): List<OverlayTextLine> {
        val hideLs = Diana.hideLsStats
        return listOf(
            OverlayTextLine("$YELLOW${BOLD}Diana Stats"),
            createStatLine("STATS_MOBS_SINCE_KING", "$GRAY - ${RED}Mobs since King: $AQUA${sboData.mobsSinceKing}"),
            createStatLine("STATS_KINGS_SINCE_WOOL", if (hideLs) "$GRAY - ${RED}Kings since Wool: $AQUA${sboData.kingSinceWool}" else "$GRAY - ${RED}Kings since Wool: $AQUA${sboData.kingSinceWool}$GRAY, ${RED}since §7[§bLS§7]: $AQUA${sboData.kingSinceLsWool}"),
            createStatLine("STATS_MOBS_SINCE_MANTI", "$GRAY - ${RED}Mobs since Manti: $AQUA${sboData.mobsSinceManti}"),
            createStatLine("STATS_MANTIS_SINCE_CORE", if (hideLs) "$GRAY - ${RED}Mantis since Core: $AQUA${sboData.mantiSinceCore}" else "$GRAY - ${RED}Mantis since Core: $AQUA${sboData.mantiSinceCore}$GRAY, ${RED}since §7[§bLS§7]: $AQUA${sboData.mantiSinceLsCore}"),
            createStatLine("STATS_MANTIS_SINCE_STINGER", if (hideLs) "$GRAY - ${RED}Mantis since Stinger: $AQUA${sboData.mantiSinceStinger}" else "$GRAY - ${RED}Mantis since Stinger: $AQUA${sboData.mantiSinceStinger}$GRAY, ${RED}since §7[§bLS§7]: $AQUA${sboData.mantiSinceLsStinger}"),
            createStatLine("STATS_MOBS_SINCE_INQ", "$GRAY - ${LIGHT_PURPLE}Mobs since Inq: $AQUA${sboData.mobsSinceInq}"),
            createStatLine("STATS_INQS_SINCE_CHIM", if (hideLs) "$GRAY - ${LIGHT_PURPLE}Inqs since Chimera: $AQUA${sboData.inqsSinceChim}" else "$GRAY - ${LIGHT_PURPLE}Inqs since Chimera: $AQUA${sboData.inqsSinceChim}$GRAY, ${LIGHT_PURPLE}since §7[§bLS§7]: $AQUA${sboData.inqsSinceLsChim}"),
            createStatLine("STATS_MOBS_SINCE_SPHINX", "$GRAY - ${LIGHT_PURPLE}Mobs since Sphinx: $AQUA${sboData.mobsSinceSphinx}"),
            createStatLine("STATS_SPHINXES_SINCE_FOOD", if (hideLs) "$GRAY - ${DARK_PURPLE}Sphinxes since Food: $AQUA${sboData.sphinxSinceFood}" else "$GRAY - ${DARK_PURPLE}Sphinxes since Food: $AQUA${sboData.sphinxSinceFood}$GRAY, ${DARK_PURPLE}since §7[§bLS§7]: $AQUA${sboData.sphinxSinceLsFood}"),
            createStatLine("STATS_CHAMPS_SINCE_RELIC", "$GRAY - ${DARK_PURPLE}Champs since Relic: $AQUA${Helper.formatNumber(sboData.champsSinceRelic, true)}"),
            createStatLine("STATS_MINOTAURS_SINCE_STICK", "$GRAY - ${GOLD}Minotaurs since Stick: $AQUA${Helper.formatNumber(sboData.minotaursSinceStick, true)}"),
        )
    }
}
