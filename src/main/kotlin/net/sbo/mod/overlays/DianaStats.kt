package net.sbo.mod.overlays

import net.minecraft.ChatFormatting
import net.minecraft.ChatFormatting.*
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.DataManager.sboData
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiCloseEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.overlay.*

object DianaStats : DianaOverlay<DianaStats.View>("Diana Stats", Diana.TrackerOverlay.STATS, { Diana.statsTracker }) {
    fun init() {
        initOverlays()
        updateLines()
    }

    // The new look shows hidden lines and the hint only in the inventory
    @SboEvent
    fun onGuiClose(event: GuiCloseEvent) {
        if (CRAFTING_PLAYER_INVENTORY_FILTER(event.screen)) updateLines()
    }

    @SboEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (CRAFTING_PLAYER_INVENTORY_FILTER(event.screen)) updateLines()
    }

    private fun createStatLine(name: String, formattedText: String): OverlayTextLine {
        val line = OverlayTextLine(formattedText).onClick { toggleHidden(name) }.setCondition {
            val meetsManualHideCondition = !(!isCraftingScreenOpen() && name in DataManager.sboData.hideTrackerLines)
            meetsManualHideCondition
        }
        if (name in DataManager.sboData.hideTrackerLines) {
            line.text = "$GRAY$STRIKETHROUGH${formattedText.removeFormatting()}"
        }
        return line
    }

    // The stats of the new look: hide key, name, color, the count and the lootshare count (null for stats without lootshare)
    private class Stat(val key: String, val name: String, val color: ChatFormatting, val count: () -> Int, val lsCount: (() -> Int)? = null)

    private val STATS = listOf(
        Stat("STATS_MOBS_SINCE_KING", "Mobs since King", RED, { sboData.mobsSinceKing }),
        Stat("STATS_KINGS_SINCE_WOOL", "Kings since Wool", RED, { sboData.kingSinceWool }, { sboData.kingSinceLsWool }),
        Stat("STATS_MOBS_SINCE_MANTI", "Mobs since Manti", RED, { sboData.mobsSinceManti }),
        Stat("STATS_MANTIS_SINCE_CORE", "Mantis since Core", RED, { sboData.mantiSinceCore }, { sboData.mantiSinceLsCore }),
        Stat("STATS_MANTIS_SINCE_STINGER", "Mantis since Stinger", RED, { sboData.mantiSinceStinger }, { sboData.mantiSinceLsStinger }),
        Stat("STATS_MOBS_SINCE_INQ", "Mobs since Inq", LIGHT_PURPLE, { sboData.mobsSinceInq }),
        Stat("STATS_INQS_SINCE_CHIM", "Inqs since Chimera", LIGHT_PURPLE, { sboData.inqsSinceChim }, { sboData.inqsSinceLsChim }),
        Stat("STATS_MOBS_SINCE_SPHINX", "Mobs since Sphinx", LIGHT_PURPLE, { sboData.mobsSinceSphinx }),
        Stat("STATS_SPHINXES_SINCE_FOOD", "Sphinxes since Food", DARK_PURPLE, { sboData.sphinxSinceFood }, { sboData.sphinxSinceLsFood }),
        Stat("STATS_CHAMPS_SINCE_RELIC", "Champs since Relic", DARK_PURPLE, { sboData.champsSinceRelic }),
        Stat("STATS_MINOTAURS_SINCE_STICK", "Minotaurs since Stick", GOLD, { sboData.minotaursSinceStick })
    )

    data class View(val rows: List<Row>, val inventory: Boolean)

    override fun view(): View {
        val inventory = isCraftingScreenOpen()
        val hidden = DataManager.sboData.hideTrackerLines
        val rows = STATS.mapNotNull { stat ->
            // Hidden lines only show (crossed out) in the inventory, to unhide them
            val isHidden = stat.key in hidden
            if (isHidden && !inventory) return@mapNotNull null
            Row(
                id = stat.key,
                name = stat.name,
                color = stat.color,
                value = Helper.formatNumber(stat.count(), withCommas = true),
                extra = stat.lsCount?.takeIf { !Diana.hideLsStats }?.let { "LS ${Helper.formatNumber(it(), withCommas = true)}" },
                hidden = isHidden
            )
        }
        return View(rows, inventory)
    }

    override fun NodeBuilder.content(view: View) {
        tracker("stats") {
            head("Diana Stats")
            view.rows.forEach { row(it) }
            if (view.inventory) hint("Click a stat to hide it")
        }
    }

    override fun elementClicked(id: String) {
        rowId(id)?.let(::toggleHidden)
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
            createStatLine("STATS_CHAMPS_SINCE_RELIC", "$GRAY - ${DARK_PURPLE}Champs since Relic: $AQUA${Helper.formatNumber(sboData.champsSinceRelic,
                withCommas = true
            )}"),
            createStatLine("STATS_MINOTAURS_SINCE_STICK", "$GRAY - ${GOLD}Minotaurs since Stick: $AQUA${Helper.formatNumber(sboData.minotaursSinceStick,
                withCommas = true
            )}"),
        )
    }
}
