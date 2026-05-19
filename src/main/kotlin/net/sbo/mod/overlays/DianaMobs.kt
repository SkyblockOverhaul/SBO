package net.sbo.mod.overlays
/* todo: Refactoring
* This Code definetly needs refactoring, but I don't have the time to do it right now.
* I will do it in the future, but for now it works and I don't want to break it.
* If you want to refactor it, feel free to do so, but please keep the functionality intact.
*/
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.isCraftingScreenOpen
import net.sbo.mod.utils.overlay.CHAT_SCREEN_FILTER
import net.sbo.mod.utils.overlay.CRAFTING_PLAYER_INVENTORY_FILTER
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.minecraft.ChatFormatting.*
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.calcPercentOne
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.data.SboDataObject.SBOConfigBundle
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiCloseEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import net.sbo.mod.utils.events.Register

object DianaMobs {
    private var dirty = false
    val overlay = Overlay("Diana Mobs", 10f, 10f, 1f, listOf(CHAT_SCREEN_FILTER, CRAFTING_PLAYER_INVENTORY_FILTER)).setCondition { Diana.mobTracker != Diana.Tracker.OFF && (Helper.checkDiana() || Helper.hasSpade) }
    val changeView: OverlayTextLine = OverlayTextLine("${YELLOW}Change View")
        .onClick {
            Diana.mobTracker = Diana.mobTracker.next()
            updateLines()
        }
        .onMouseEnter {
            changeView.text = "$YELLOW${UNDERLINE}Change View"
        }
        .onMouseLeave {
            changeView.text = "${YELLOW}Change View"
        }

    fun init() {
        overlay.init()
        updateLines()
        Register.onTick(1) { flushUpdateLines() }
    }

    @SboEvent
    fun onGuiClose(event: GuiCloseEvent) {
        if (CRAFTING_PLAYER_INVENTORY_FILTER(event.screen)) {
            overlay.removeLine(changeView)
        }
    }

    @SboEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (CRAFTING_PLAYER_INVENTORY_FILTER(event.screen)) {
            updateLines()
        }
    }

    fun createLine(name: String, formattedText: String, amount: Int) : OverlayTextLine {
        val line = OverlayTextLine(formattedText).onClick {
            if (!isCraftingScreenOpen()) return@onClick
            if (SBOConfigBundle.sboData.hideTrackerLines.contains(name)) {
                SBOConfigBundle.sboData.hideTrackerLines.remove(name)
            } else {
                SBOConfigBundle.sboData.hideTrackerLines.add(name)
            }
            updateLines()
        }
            .setCondition {
                val meetsZeroValueCondition = amount > 0 || !Diana.hideUnobtainedItems
                val meetsManualHideCondition = !(!isCraftingScreenOpen() && SBOConfigBundle.sboData.hideTrackerLines.contains(name))
                meetsZeroValueCondition && meetsManualHideCondition
            }
        if (SBOConfigBundle.sboData.hideTrackerLines.contains(name)) {
            line.text = "$GRAY$STRIKETHROUGH${formattedText.removeFormatting()}"
        }
        return line
    }

    fun updateLines() {
        dirty = true
    }

    fun flushUpdateLines() {
        if (!dirty) {
            return
        }

        val lines = mutableListOf<OverlayTextLine>()
        val type = Diana.mobTracker
        val tracker = when (type) {
            Diana.Tracker.TOTAL -> SBOConfigBundle.dianaTrackerTotalData
            Diana.Tracker.EVENT -> SBOConfigBundle.dianaTrackerMayorData
            Diana.Tracker.SESSION -> SBOConfigBundle.dianaTrackerSessionData
            Diana.Tracker.OFF -> {
                overlay.setLines(emptyList())
                return
            }
        }
        val kingPercent = calcPercentOne(tracker.items, tracker.mobs, "KING_MINOS")
        val manticorePercent = calcPercentOne(tracker.items, tracker.mobs, "MANTICORE")
        val inqPercent = calcPercentOne(tracker.items, tracker.mobs, "MINOS_INQUISITOR")
        val sphinxPercent = calcPercentOne(tracker.items, tracker.mobs, "SPHINX")
        val champPercent = calcPercentOne(tracker.items, tracker.mobs, "MINOS_CHAMPION")
        val minotaurPercent = calcPercentOne(tracker.items, tracker.mobs, "MINOTAUR")
        val gaiaPercent = calcPercentOne(tracker.items, tracker.mobs, "GAIA_CONSTRUCT")
        val harpyPercent = calcPercentOne(tracker.items, tracker.mobs, "HARPY")
        val cretanPercent = calcPercentOne(tracker.items, tracker.mobs, "CRETAN_BULL")
        val nymphPercent = calcPercentOne(tracker.items, tracker.mobs, "STRANDED_NYMPH")
        val lynxPercent = calcPercentOne(tracker.items, tracker.mobs, "SIAMESE_LYNXES")
        val hunterPercent = calcPercentOne(tracker.items, tracker.mobs, "MINOS_HUNTER")
        val playTimeHrs = tracker.items.TIME.toDouble() / TimeUnit.HOURS.toMillis(1)
        val mobsPerHr = if (playTimeHrs > 0) {
            BigDecimal(tracker.mobs.TOTAL_MOBS.toDouble() / playTimeHrs).setScale(2, RoundingMode.HALF_UP).toDouble()
        } else 0.0

        if (isCraftingScreenOpen()) {
            lines.add(changeView)
        }

        lines.add(OverlayTextLine("$YELLOW${BOLD}Diana Mobs $GRAY($YELLOW${Helper.toTitleCase(type.toString())}$GRAY)"))

        lines.addAll(
            listOf(
                createLine("KING_MINOS","$GRAY - ${RED}King Minos: $AQUA${Helper.formatNumber(tracker.mobs.KING_MINOS, true)} $GRAY($AQUA${kingPercent}%$GRAY) [${AQUA}LS$GRAY:$AQUA${Helper.formatNumber(tracker.mobs.KING_MINOS_LS, true)}$GRAY]", tracker.mobs.KING_MINOS + tracker.mobs.KING_MINOS_LS),
                createLine("MANTICORE","$GRAY - ${RED}Manticore: $AQUA${Helper.formatNumber(tracker.mobs.MANTICORE, true)} $GRAY($AQUA${manticorePercent}%$GRAY) [${AQUA}LS$GRAY:$AQUA${Helper.formatNumber(tracker.mobs.MANTICORE_LS, true)}$GRAY]", tracker.mobs.MANTICORE+ tracker.mobs.MANTICORE_LS),
                createLine("INQUISITOR","$GRAY - ${LIGHT_PURPLE}Inquisitor: $AQUA${Helper.formatNumber(tracker.mobs.MINOS_INQUISITOR, true)} $GRAY($AQUA${inqPercent}%$GRAY) [${AQUA}LS$GRAY:$AQUA${Helper.formatNumber(tracker.mobs.MINOS_INQUISITOR_LS, true)}$GRAY]", tracker.mobs.MINOS_INQUISITOR + tracker.mobs.MINOS_INQUISITOR_LS),
                createLine("SPHINX","$GRAY - ${LIGHT_PURPLE}Sphinx: $AQUA${Helper.formatNumber(tracker.mobs.SPHINX, true)} $GRAY($AQUA${sphinxPercent}%$GRAY)" + " [${AQUA}LS$GRAY:$AQUA${Helper.formatNumber(tracker.mobs.SPHINX_LS, true)}$GRAY]", tracker.mobs.SPHINX + tracker.mobs.SPHINX_LS),
                createLine("CHAMPION","$GRAY - ${DARK_PURPLE}Champion: $AQUA${Helper.formatNumber(tracker.mobs.MINOS_CHAMPION, true)} $GRAY($AQUA${champPercent}%$GRAY)", tracker.mobs.MINOS_CHAMPION),
                createLine("MINOTAUR","$GRAY - ${GOLD}Minotaur: $AQUA${Helper.formatNumber(tracker.mobs.MINOTAUR, true)} $GRAY($AQUA${minotaurPercent}%$GRAY)", tracker.mobs.MINOTAUR),
                createLine("GAIA_CONSTRUCT","$GRAY - ${GREEN}Gaia Construct: $AQUA${Helper.formatNumber(tracker.mobs.GAIA_CONSTRUCT, true)} $GRAY($AQUA${gaiaPercent}%$GRAY)", tracker.mobs.GAIA_CONSTRUCT),
                createLine("HARPY","$GRAY - ${GREEN}Harpy: $AQUA${Helper.formatNumber(tracker.mobs.HARPY, true)} $GRAY($AQUA${harpyPercent}%$GRAY)", tracker.mobs.HARPY),
                createLine("CRETAN_BULL","$GRAY - ${GREEN}Cretan Bull: $AQUA${Helper.formatNumber(tracker.mobs.CRETAN_BULL, true)} $GRAY($AQUA${cretanPercent}%$GRAY)", tracker.mobs.CRETAN_BULL),
                createLine("STRANDED_NYMPH","$GRAY - ${GREEN}Stranded Nymph: $AQUA${Helper.formatNumber(tracker.mobs.STRANDED_NYMPH, true)} $GRAY($AQUA${nymphPercent}%$GRAY)", tracker.mobs.STRANDED_NYMPH),
                createLine("SIAMESE_LYNXES","$GRAY - ${GREEN}Siamese Lynxes: $AQUA${Helper.formatNumber(tracker.mobs.SIAMESE_LYNXES, true)} $GRAY($AQUA${lynxPercent}%$GRAY)", tracker.mobs.SIAMESE_LYNXES),
                createLine("MINOS_HUNTER","$GRAY - ${GREEN}Minos Hunter: $AQUA${Helper.formatNumber(tracker.mobs.MINOS_HUNTER, true)} $GRAY($AQUA${hunterPercent}%$GRAY)", tracker.mobs.MINOS_HUNTER),
                OverlayTextLine("$GRAY - ${GRAY}Total Mobs: $AQUA${Helper.formatNumber(tracker.mobs.TOTAL_MOBS, true)} $GRAY[$AQUA$mobsPerHr$GRAY/${AQUA}hr$GRAY]")
            )
        )
        overlay.setLines(lines)

        dirty = false
    }
}
