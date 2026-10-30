package net.sbo.mod.overlays
/* todo: Refactoring
* This Code definitely needs refactoring, but I don't have the time to do it right now.
* I will do it in the future, but for now it works and I don't want to break it.
* If you want to refactor it, feel free to do so, but please keep the functionality intact.
*/
import net.minecraft.ChatFormatting
import net.minecraft.ChatFormatting.*
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.mod.config.nextOption
import net.sbo.mod.settings.categories.Trackers
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.calcPercentOne
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.diana.DianaMobsData
import net.sbo.mod.utils.data.configs.diana.DianaTracker
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiCloseEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.overlay.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

object DianaMobs : DianaOverlay<DianaMobs.View>("Diana Mobs", Trackers.TrackerLook.MOBS, { Trackers.mobTracker != Trackers.TrackerMode.OFF }) {
    private val changeView: OverlayTextLine = OverlayTextLine("${YELLOW}Change View")
        .onClick {
            Trackers.mobTracker = Trackers.mobTracker.nextOption()
            updateLines()
        }
        .onMouseEnter {
            changeView.text = "$YELLOW${UNDERLINE}Change View"
        }
        .onMouseLeave {
            changeView.text = "${YELLOW}Change View"
        }

    private val changeSort: OverlayTextLine = OverlayTextLine("")
        .onClick {
            Trackers.mobSort = Trackers.mobSort.nextOption()
            updateLines()
        }
        .onMouseEnter {
            changeSort.text = "$YELLOW${UNDERLINE}Sort: ${Trackers.mobSort.label}"
        }
        .onMouseLeave {
            changeSort.text = "${YELLOW}Sort: ${Trackers.mobSort.label}"
        }

    fun init() {
        initOverlays()
        updateLines()

        Register.onTick(20) {
            updateLines()
        }
    }

    @SboEvent
    fun onGuiClose(event: GuiCloseEvent) {
        if (CRAFTING_PLAYER_INVENTORY_FILTER(event.screen)) {
            overlay.removeLines(listOf(changeView, changeSort))
            updateLines()
        }
    }

    @SboEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (CRAFTING_PLAYER_INVENTORY_FILTER(event.screen)) {
            updateLines()
        }
    }

    private fun createLine(name: String, formattedText: String, amount: Int) : OverlayTextLine {
        val line = OverlayTextLine(formattedText).onClick { toggleHidden(name) }
            .setCondition {
                val meetsZeroValueCondition = amount > 0 || !Trackers.hideUnobtainedItems
                val meetsManualHideCondition = !(!isCraftingScreenOpen() && name in DataManager.sboData.hideTrackerLines)
                meetsZeroValueCondition && meetsManualHideCondition
            }
        if (name in DataManager.sboData.hideTrackerLines) {
            line.text = "$GRAY$STRIKETHROUGH${formattedText.removeFormatting()}"
        }
        return line
    }

    private fun tracker(type: Trackers.TrackerMode): DianaTracker? = when (type) {
        Trackers.TrackerMode.TOTAL -> DataManager.dianaTrackerTotalData
        Trackers.TrackerMode.EVENT -> DataManager.dianaTrackerMayorData
        Trackers.TrackerMode.SESSION -> DataManager.dianaTrackerSessionData
        Trackers.TrackerMode.OFF -> null
    }

    private fun mobsPerHour(tracker: DianaTracker): Double {
        val playTimeHrs = tracker.items.TIME.toDouble() / TimeUnit.HOURS.toMillis(1)
        return if (playTimeHrs > 0) {
            BigDecimal(tracker.mobs.TOTAL_MOBS.toDouble() / playTimeHrs).setScale(2, RoundingMode.HALF_UP).toDouble()
        } else 0.0
    }

    // The mobs of the new look: hide key, name, color, kills and lootshare kills (null for mobs without lootshare)
    private class Mob(
        val key: String,
        val name: String,
        val color: ChatFormatting,
        val kills: (DianaMobsData) -> Int,
        val lsKills: ((DianaMobsData) -> Int)? = null
    )

    private val MOBS = listOf(
        Mob("KING_MINOS", "King Minos", RED, { it.KING_MINOS }, { it.KING_MINOS_LS }),
        Mob("MANTICORE", "Manticore", RED, { it.MANTICORE }, { it.MANTICORE_LS }),
        Mob("INQUISITOR", "Inquisitor", LIGHT_PURPLE, { it.MINOS_INQUISITOR }, { it.MINOS_INQUISITOR_LS }),
        Mob("SPHINX", "Sphinx", LIGHT_PURPLE, { it.SPHINX }, { it.SPHINX_LS }),
        Mob("CHAMPION", "Champion", DARK_PURPLE, { it.MINOS_CHAMPION }),
        Mob("MINOTAUR", "Minotaur", GOLD, { it.MINOTAUR }),
        Mob("GAIA_CONSTRUCT", "Gaia Construct", GREEN, { it.GAIA_CONSTRUCT }),
        Mob("HARPY", "Harpy", GREEN, { it.HARPY }),
        Mob("CRETAN_BULL", "Cretan Bull", GREEN, { it.CRETAN_BULL }),
        Mob("STRANDED_NYMPH", "Stranded Nymph", GREEN, { it.STRANDED_NYMPH }),
        Mob("SIAMESE_LYNXES", "Siamese Lynxes", GREEN, { it.SIAMESE_LYNXES }),
        Mob("MINOS_HUNTER", "Minos Hunter", GREEN, { it.MINOS_HUNTER })
    )

    private const val ORDER = "mobs"
    private val MOB_KEYS = MOBS.map { it.key }

    /** The mobs in the order picked in "Mob Sort". */
    private fun sortedMobs(mobs: DianaMobsData): List<Mob> =
        sortLines(MOBS, Trackers.mobSort, ORDER, { it.key }) { it.kills(mobs) + (it.lsKills?.invoke(mobs) ?: 0) }

    data class View(
        val mode: String,
        val sort: String,
        val rows: List<Row>,
        val total: String,
        val perHour: String,
        val inventory: Boolean,
        // Arrows on the lines, with the Custom sort in the inventory
        val reorder: Boolean
    )

    override fun view(): View {
        val type = Trackers.mobTracker
        val inventory = isCraftingScreenOpen()
        val sort = Trackers.mobSort.label
        val reorder = inventory && Trackers.mobSort == Trackers.LineSort.CUSTOM
        val tracker = tracker(type) ?: return View("", sort, emptyList(), "", "", inventory, reorder)
        val mobs = tracker.mobs
        val hidden = DataManager.sboData.hideTrackerLines
        val rows = sortedMobs(mobs).mapNotNull { mob ->
            val kills = mob.kills(mobs)
            val lsKills = mob.lsKills?.invoke(mobs)
            if (kills + (lsKills ?: 0) <= 0 && Trackers.hideUnobtainedItems) return@mapNotNull null
            // Hidden lines only show (crossed out) in the inventory, to unhide them
            val isHidden = mob.key in hidden
            if (isHidden && !inventory) return@mapNotNull null
            val ls = if (lsKills != null && !Trackers.hideLsStats) "  LS ${Helper.formatNumber(lsKills, withCommas = true)}" else ""
            Row(
                id = mob.key,
                name = mob.name,
                color = mob.color,
                value = Helper.formatNumber(kills, withCommas = true),
                extra = "${calcPercentOne(kills, mobs.TOTAL_MOBS)}%$ls",
                hidden = isHidden
            )
        }
        return View(
            mode = Helper.toTitleCase("$type"),
            sort = sort,
            rows = rows,
            total = Helper.formatNumber(mobs.TOTAL_MOBS, withCommas = true),
            perHour = "${mobsPerHour(tracker)}/h",
            inventory = inventory,
            reorder = reorder
        )
    }

    override fun NodeBuilder.content(view: View) {
        tracker("mobs") {
            head("Diana Mobs") {
                if (view.inventory) badge(view.sort, "sort", clickable = true)
                badge(view.mode, "mode", view.inventory)
            }
            rows(view.rows, view.reorder)
            separator()
            row(Row(null, "Total Mobs", null, view.total, view.perHour))
            if (view.reorder) hint("Click a mob to hide it, the arrows to move it")
            else if (view.inventory) hint("Click a mob to hide it, the badges to change them")
        }
    }

    override fun elementClicked(id: String) {
        if (moveClicked(id, ORDER, MOB_KEYS) { view().rows.mapNotNull { it.id } }) return
        when (id) {
            "mode" -> {
                Trackers.mobTracker = Trackers.mobTracker.nextOption()
                updateLines()
            }
            "sort" -> {
                Trackers.mobSort = Trackers.mobSort.nextOption()
                updateLines()
            }
            else -> rowId(id)?.let(::toggleHidden)
        }
    }

    override fun generateLines(): List<OverlayTextLine> {
        val type = Trackers.mobTracker
        val tracker = tracker(type) ?: return emptyList()
        val kingPercent = calcPercentOne(tracker.mobs.KING_MINOS, tracker.mobs.TOTAL_MOBS)
        val manticorePercent = calcPercentOne(tracker.mobs.MANTICORE, tracker.mobs.TOTAL_MOBS)
        val inqPercent = calcPercentOne(tracker.mobs.MINOS_INQUISITOR, tracker.mobs.TOTAL_MOBS)
        val sphinxPercent = calcPercentOne(tracker.mobs.SPHINX, tracker.mobs.TOTAL_MOBS)
        val champPercent = calcPercentOne(tracker.mobs.MINOS_CHAMPION, tracker.mobs.TOTAL_MOBS)
        val minotaurPercent = calcPercentOne(tracker.mobs.MINOTAUR, tracker.mobs.TOTAL_MOBS)
        val gaiaPercent = calcPercentOne(tracker.mobs.GAIA_CONSTRUCT, tracker.mobs.TOTAL_MOBS)
        val harpyPercent = calcPercentOne(tracker.mobs.HARPY, tracker.mobs.TOTAL_MOBS)
        val cretanPercent = calcPercentOne(tracker.mobs.CRETAN_BULL, tracker.mobs.TOTAL_MOBS)
        val nymphPercent = calcPercentOne(tracker.mobs.STRANDED_NYMPH, tracker.mobs.TOTAL_MOBS)
        val lynxPercent = calcPercentOne(tracker.mobs.SIAMESE_LYNXES, tracker.mobs.TOTAL_MOBS)
        val hunterPercent = calcPercentOne(tracker.mobs.MINOS_HUNTER, tracker.mobs.TOTAL_MOBS)
        val mobsPerHr = mobsPerHour(tracker)

        val lines = mutableListOf<OverlayTextLine>()

        if (isCraftingScreenOpen()) {
            changeSort.text = "${YELLOW}Sort: ${Trackers.mobSort.label}"
            lines.add(changeView)
            lines.add(changeSort)
        }

        lines.add(OverlayTextLine("$YELLOW${BOLD}Diana Mobs $GRAY($YELLOW${Helper.toTitleCase("$type")}$GRAY)"))

        val hideLs = Trackers.hideLsStats

        // In the order of MOBS
        val mobLines = listOf(
            createLine(
                "KING_MINOS",
                "$GRAY - ${RED}King Minos: $AQUA${Helper.formatNumber(tracker.mobs.KING_MINOS, withCommas = true)} $GRAY($AQUA$kingPercent%$GRAY)" +
                    if (hideLs) "" else " [${AQUA}LS$GRAY:$AQUA${Helper.formatNumber(tracker.mobs.KING_MINOS_LS,
                        withCommas = true
                    )}$GRAY]",
                tracker.mobs.KING_MINOS + tracker.mobs.KING_MINOS_LS
            ),
            createLine(
                "MANTICORE",
                "$GRAY - ${RED}Manticore: $AQUA${Helper.formatNumber(tracker.mobs.MANTICORE, withCommas = true)} $GRAY($AQUA$manticorePercent%$GRAY)" +
                    if (hideLs) "" else " [${AQUA}LS$GRAY:$AQUA${Helper.formatNumber(tracker.mobs.MANTICORE_LS,
                        withCommas = true
                    )}$GRAY]",
                tracker.mobs.MANTICORE + tracker.mobs.MANTICORE_LS
            ),
            createLine(
                "INQUISITOR",
                "$GRAY - ${LIGHT_PURPLE}Inquisitor: $AQUA${Helper.formatNumber(tracker.mobs.MINOS_INQUISITOR,
                    withCommas = true
                )} $GRAY($AQUA$inqPercent%$GRAY)" +
                    if (hideLs) "" else " [${AQUA}LS$GRAY:$AQUA${Helper.formatNumber(tracker.mobs.MINOS_INQUISITOR_LS,
                        withCommas = true
                    )}$GRAY]",
                tracker.mobs.MINOS_INQUISITOR + tracker.mobs.MINOS_INQUISITOR_LS
            ),
            createLine(
                "SPHINX",
                "$GRAY - ${LIGHT_PURPLE}Sphinx: $AQUA${Helper.formatNumber(tracker.mobs.SPHINX, withCommas = true)} $GRAY($AQUA$sphinxPercent%$GRAY)" +
                    if (hideLs) "" else " [${AQUA}LS$GRAY:$AQUA${Helper.formatNumber(tracker.mobs.SPHINX_LS,
                        withCommas = true
                    )}$GRAY]",
                tracker.mobs.SPHINX + tracker.mobs.SPHINX_LS
            ),
            createLine("CHAMPION", "$GRAY - ${DARK_PURPLE}Champion: $AQUA${Helper.formatNumber(tracker.mobs.MINOS_CHAMPION,
                withCommas = true
            )} $GRAY($AQUA$champPercent%$GRAY)", tracker.mobs.MINOS_CHAMPION),
            createLine("MINOTAUR", "$GRAY - ${GOLD}Minotaur: $AQUA${Helper.formatNumber(tracker.mobs.MINOTAUR,
                withCommas = true
            )} $GRAY($AQUA$minotaurPercent%$GRAY)", tracker.mobs.MINOTAUR),
            createLine("GAIA_CONSTRUCT", "$GRAY - ${GREEN}Gaia Construct: $AQUA${Helper.formatNumber(tracker.mobs.GAIA_CONSTRUCT,
                withCommas = true
            )} $GRAY($AQUA$gaiaPercent%$GRAY)", tracker.mobs.GAIA_CONSTRUCT),
            createLine("HARPY", "$GRAY - ${GREEN}Harpy: $AQUA${Helper.formatNumber(tracker.mobs.HARPY,
                withCommas = true
            )} $GRAY($AQUA$harpyPercent%$GRAY)", tracker.mobs.HARPY),
            createLine("CRETAN_BULL", "$GRAY - ${GREEN}Cretan Bull: $AQUA${Helper.formatNumber(tracker.mobs.CRETAN_BULL,
                withCommas = true
            )} $GRAY($AQUA$cretanPercent%$GRAY)", tracker.mobs.CRETAN_BULL),
            createLine("STRANDED_NYMPH", "$GRAY - ${GREEN}Stranded Nymph: $AQUA${Helper.formatNumber(tracker.mobs.STRANDED_NYMPH,
                withCommas = true
            )} $GRAY($AQUA$nymphPercent%$GRAY)", tracker.mobs.STRANDED_NYMPH),
            createLine("SIAMESE_LYNXES", "$GRAY - ${GREEN}Siamese Lynxes: $AQUA${Helper.formatNumber(tracker.mobs.SIAMESE_LYNXES,
                withCommas = true
            )} $GRAY($AQUA$lynxPercent%$GRAY)", tracker.mobs.SIAMESE_LYNXES),
            createLine("MINOS_HUNTER", "$GRAY - ${GREEN}Minos Hunter: $AQUA${Helper.formatNumber(tracker.mobs.MINOS_HUNTER,
                withCommas = true
            )} $GRAY($AQUA$hunterPercent%$GRAY)", tracker.mobs.MINOS_HUNTER)
        )
        lines.addAll(sortedMobs(tracker.mobs).map { mobLines[MOBS.indexOf(it)] })
        lines.add(OverlayTextLine("$GRAY - ${GRAY}Total Mobs: $AQUA${Helper.formatNumber(tracker.mobs.TOTAL_MOBS,
            withCommas = true
        )} $GRAY[$AQUA$mobsPerHr$GRAY/${AQUA}hr$GRAY]"))

        return lines
    }
}
