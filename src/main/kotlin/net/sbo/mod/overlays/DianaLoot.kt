package net.sbo.mod.overlays

import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.minecraft.util.Formatting.*
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.calcPercentOne
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.data.DianaTracker
import net.sbo.mod.utils.data.SboDataObject.SBOConfigBundle
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiCloseEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.render.RenderUtils2D
import net.sbo.mod.overlays.OverlayUtils.LootItemData
import java.util.concurrent.TimeUnit

object DianaLoot {
    private var isSellTypeHovered = false
    val timerLine: OverlayTextLine = OverlayTextLine("")
    val overlay = Overlay("Diana Loot", 10f, 10f, 1f, listOf("Chat screen", "Crafting"))
        .setCondition { Diana.lootTracker != Diana.Tracker.OFF && (Helper.checkDiana() || Helper.hasSpade) }

    val changeView: OverlayTextLine = OverlayUtils.createClickableTextLine(
        text = "${YELLOW}Change View",
        hoverText = "$YELLOW${UNDERLINE}Change View",
        defaultText = "${YELLOW}Change View",
        onClick = {
            Diana.lootTracker = Diana.lootTracker.next()
            updateLines()
        },
        lineBreak = false
    )

    val delimiter = OverlayTextLine(" | ", linebreak = false)

    val changeSellType: OverlayTextLine = OverlayUtils.createClickableTextLine(
        text = "",
        onClick = {
            Diana.bazaarSettingDiana = Diana.bazaarSettingDiana.next()
            updateLines()
        },
        onMouseEnter = {
            isSellTypeHovered = true
            updateLines()
        },
        onMouseLeave = {
            isSellTypeHovered = false
            updateLines()
        }
    )

    val resetSession = OverlayUtils.createClickableTextLine(
        text = "${RED}Reset Session",
        hoverText = "$RED${UNDERLINE}Reset Session",
        defaultText = "${RED}Reset Session",
        onClick = {
            SboTimerManager.timerSession.reset()
            SBOConfigBundle.dianaTrackerSessionData.reset().save()
            updateLines()
            DianaMobs.updateLines()
        }
    )

    private val LOOT_ITEMS = listOf<LootItemData>(
        LootItemData("MYTHOLOGICAL_DYE", "Mythological Dye", RED),
        LootItemData("SHIMMERING_WOOL", "Shimmering Wool", RED, combined = true, dropMobId = "KING_MINOS", dropMobLsId = "KING_MINOS_LS"),
        LootItemData("MANTI_CORE", "Manti-core", RED, combined = true, dropMobId = "MANTICORE", dropMobLsId = "MANTICORE_LS"),
        LootItemData("KING_MINOS_SHARD", "King Minos Shard", RED, isRarerDrop = true, dropMobId = "KING_MINOS"),
        LootItemData("FATEFUL_STINGER", "Fateful Stinger", LIGHT_PURPLE, combined = true, dropMobId = "MANTICORE", dropMobLsId = "MANTICORE_LS"),
        LootItemData("CHIMERA", "Chimera", LIGHT_PURPLE, combined = true, dropMobId = "MINOS_INQUISITOR", dropMobLsId = "MINOS_INQUISITOR_LS"),
        LootItemData("BRAIN_FOOD", "Brain Food", DARK_PURPLE, combined = true, dropMobId = "SPHINX", dropMobLsId = "SPHINX_LS"),
        LootItemData("MINOS_RELIC", "Minos Relic", DARK_PURPLE, isRarerDrop = true, dropMobId = "MINOS_CHAMPION"),
        LootItemData("SPHINX_SHARD", "Sphinx Shard", DARK_PURPLE, isRarerDrop = true, dropMobId = "SPHINX"),
        LootItemData("BRAIDED_GRIFFIN_FEATHER", "Braided Griffin Feather", DARK_PURPLE, isRarerDrop = true),
        LootItemData("DAEDALUS_STICK", "Daedalus Stick", GOLD, isRarerDrop = true, dropMobId = "MINOTAUR"),
        LootItemData("MINOTAUR_SHARD", "Minotaur Shard", GOLD, isRarerDrop = true),
        LootItemData("CROWN_OF_GREED", "Crown of Greed", GOLD, isRarerDrop = true),
        LootItemData("WASHED_UP_SOUVENIR", "Washed-up Souvenir", GOLD, isRarerDrop = true),
        LootItemData("GRIFFIN_FEATHER", "Griffin Feather", GOLD),
        LootItemData("MYTHOS_FRAGMENT", "Mytho Fragment", GOLD),
        LootItemData("CRETAN_URN", "Cretan Urn", DARK_GREEN),
        LootItemData("DWARF_TURTLE_SHELMET", "Dwarf Turtle Shelmet", DARK_GREEN),
        LootItemData("CROCHET_TIGER_PLUSHIE", "Crochet Tiger Plushie", DARK_GREEN),
        LootItemData("ANTIQUE_REMEDIES", "Antique Remedies", DARK_GREEN),
        LootItemData("CRETAN_BULL_SHARD", "Cretan Bull Shard", DARK_GREEN),
        LootItemData("HARPY_SHARD", "Harpy Shard", DARK_GREEN),
        LootItemData("HILT_OF_REVELATIONS", "Hilt of Revelations", BLUE),
        LootItemData("ANCIENT_CLAW", "Ancient Claw", BLUE),
        LootItemData("ENCHANTED_ANCIENT_CLAW", "Enchanted Ancient Claw", BLUE),
        LootItemData("ENCHANTED_GOLD", "Enchanted Gold", BLUE)
    )

    fun init() {
        overlay.init()
        updateLines()
        updateTimerText()
        Register.onTick(1) { updateTimerText() }
    }

    private const val CRAFTING_GUI_TITLE = "Crafting"

    @SboEvent
    fun onGuiClose(event: GuiCloseEvent) {
        if (event.screen.title.string == CRAFTING_GUI_TITLE) {
            overlay.removeLines(listOf(changeView, delimiter, changeSellType, resetSession))
        }
    }

    @SboEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (event.screen.title.string == CRAFTING_GUI_TITLE) {
            updateLines(isCraftingOpen = true)
        }
    }

    private fun isCraftingScreenOpen(): Boolean = mc.currentScreen?.title?.string == CRAFTING_GUI_TITLE

    fun hideLine(name: String) {
        if (!isCraftingScreenOpen()) return
        val hideList = SBOConfigBundle.sboData.hideTrackerLines
        if (hideList.contains(name)) hideList.remove(name) else hideList.add(name)
        updateLines()
    }

    fun createLootLine(data: LootItemData, tracker: DianaTracker): OverlayTextLine {
        val itemName = data.id
        val amount = tracker.getAmountOf(itemName)
        val formattedName = "${data.color}${data.name}: ${AQUA}${Helper.formatNumber(amount, true)}"
        val price = Helper.getItemPriceFormatted(itemName.replace("_LS", ""), amount)
        val percent = data.dropMobId?.let { dropId ->
            calcPercentOne(tracker.items, tracker.mobs, itemName, dropId)
        }
        val percentText = percent?.let { " $GRAY($AQUA${it}%$GRAY)" } ?: ""
        val formattedText = "$GOLD$price $GRAY| $formattedName$percentText"
        val line = OverlayTextLine(formattedText).onClick { hideLine(itemName) }
            .setCondition {
                val meetsZeroValueCondition = amount > 0 || !Diana.hideUnobtainedItems
                val meetsManualHideCondition = !(mc.currentScreen?.title?.string != CRAFTING_GUI_TITLE && SBOConfigBundle.sboData.hideTrackerLines.contains(itemName))
                meetsZeroValueCondition && meetsManualHideCondition
            }
        if (SBOConfigBundle.sboData.hideTrackerLines.contains(itemName)) {
            line.text = "$GRAY$STRIKETHROUGH${formattedText.removeFormatting()}"
        }
        return line
    }

    fun createCombinedLootLine(data: LootItemData, tracker: DianaTracker): OverlayTextLine {
        val itemNameBase = data.id
        val itemNameLs = "${data.id}_LS"
        val amountBase = tracker.getAmountOf(itemNameBase)
        val amountLs = tracker.getAmountOf(itemNameLs)
        val totalAmount = amountBase + amountLs
        val priceLs = Helper.getItemPriceFormatted(itemNameLs.replace("_LS", ""), amountLs)
        val priceCombined = Helper.getItemPriceFormatted(itemNameBase, totalAmount)
        val percent = data.dropMobId?.let { dropId ->
            calcPercentOne(tracker.items, tracker.mobs, itemNameBase, dropId)
        }
        val percentText = percent?.let { " $GRAY($AQUA${it}%$GRAY)" } ?: ""
        val percentLs = data.dropMobLsId?.let { dropLsId ->
            calcPercentOne(tracker.items, tracker.mobs, itemNameLs, dropLsId)
        }
        val percentLsText = percentLs?.let { " $GRAY($AQUA${it}%$GRAY)" } ?: ""
        val baseText = "$GOLD$priceCombined $GRAY| ${data.color}${data.name}: $AQUA${Helper.formatNumber(amountBase, true)}$percentText"
        val lsText = "$GOLD$priceLs $GRAY| ${data.color}${data.name} $GRAY[${AQUA}LS$GRAY]: $AQUA${Helper.formatNumber(amountLs, true)}$percentLsText"
        val combinedText = "$baseText $GRAY[${AQUA}LS$GRAY:$AQUA${Helper.formatNumber(amountLs, true)}$GRAY]"

        val line = OverlayTextLine(combinedText).onClick { hideLine(itemNameBase) }
            .setCondition {
                val meetsZeroValueCondition = totalAmount > 0 || !Diana.hideUnobtainedItems
                val meetsManualHideCondition = !(mc.currentScreen?.title?.string != CRAFTING_GUI_TITLE && SBOConfigBundle.sboData.hideTrackerLines.contains(itemNameBase))
                meetsZeroValueCondition && meetsManualHideCondition
            }
            .onHover { drawContext, textRenderer ->
                val scaleFactor = mc.window.scaleFactor
                val mouseX = mc.mouse.x / scaleFactor
                val mouseY = mc.mouse.y / scaleFactor

                RenderUtils2D.drawHoveringString(
                    drawContext,
                    "$YELLOW${Helper.toTitleCase(itemNameBase.replace("_", " ").lowercase())} LS Details:\n" +
                            lsText,
                    mouseX, mouseY, textRenderer, overlay.scale
                )
            }
        if (SBOConfigBundle.sboData.hideTrackerLines.contains(itemNameBase)) {
            line.text = "$GRAY$STRIKETHROUGH${combinedText.removeFormatting()}"
        }
        return line
    }

    fun updateLines(isCraftingOpen: Boolean = false) {
        val lines = mutableListOf<OverlayTextLine>()
        val type = Diana.lootTracker
        val tracker = getDianaTracker(type) ?: run {
            overlay.setLines(emptyList())
            return
        }

        updateControlLines(lines, isCraftingOpen)
        lines.add(OverlayTextLine("$YELLOW${BOLD}Diana Loot $GRAY($YELLOW${Helper.toTitleCase(type.toString())}$GRAY)"))
        lines.addAll(generateLootLines(tracker))
        lines.addAll(generateStatisticsLines(tracker, type, isCraftingOpen))
        overlay.setLines(lines)
    }

    private fun getDianaTracker(type: Diana.Tracker): DianaTracker? {
        return when (type) {
            Diana.Tracker.TOTAL -> SBOConfigBundle.dianaTrackerTotalData
            Diana.Tracker.EVENT -> SBOConfigBundle.dianaTrackerMayorData
            Diana.Tracker.SESSION -> SBOConfigBundle.dianaTrackerSessionData
            Diana.Tracker.OFF -> null
        }
    }

    private fun updateControlLines(lines: MutableList<OverlayTextLine>, isCraftingOpen: Boolean) {
        val screenOpen = isCraftingOpen || isCraftingScreenOpen()
        val sellTypeText = if (isSellTypeHovered) {
            "$YELLOW${UNDERLINE}${Helper.toTitleCase(Diana.bazaarSettingDiana.toString())}"
        } else {
            "${YELLOW}${Helper.toTitleCase(Diana.bazaarSettingDiana.toString())}"
        }
        changeSellType.text = sellTypeText

        if (screenOpen) {
            lines.addAll(listOf(changeView, delimiter, changeSellType))
        }
    }

    private fun generateLootLines(tracker: DianaTracker): List<OverlayTextLine> {
        val lines = mutableListOf<OverlayTextLine>()
        for (data in LOOT_ITEMS) {
            if (Diana.combineLootLines && data.combined) {
                lines.add(createCombinedLootLine(data, tracker))
            } else if (data.combined) {
                lines.add(createLootLine(data, tracker))
                val lsData = data.copy(
                    id = "${data.id}_LS",
                    name = "${data.name} $GRAY[${AQUA}LS$GRAY]",
                    combined = false,
                    isRarerDrop = true
                )
                lines.add(createLootLine(lsData, tracker))
            } else {
                lines.add(createLootLine(data, tracker))
            }
        }
        return lines
    }

    private fun generateStatisticsLines(tracker: DianaTracker, type: Diana.Tracker, isCraftingOpen: Boolean): List<OverlayTextLine> {
        val timer = when (type) {
            Diana.Tracker.TOTAL -> SboTimerManager.timerTotal
            Diana.Tracker.EVENT -> SboTimerManager.timerMayor
            Diana.Tracker.SESSION -> SboTimerManager.timerSession
            else -> SboTimerManager.timerMayor
        }
        val totalBurrows = tracker.items.TOTAL_BURROWS
        val totalProfitValue = totalProfit(tracker)
        val playTimeHrs = tracker.items.TIME.toDouble() / TimeUnit.HOURS.toMillis(1)
        val totalEvents = SBOConfigBundle.pastDianaEventsData.events.size

        val burrowsPerHr = Helper.getBurrowsPerHr(tracker, timer)
        val bphText = if (burrowsPerHr.isNaN() || burrowsPerHr == 0.0) "" else " $GRAY[$AQUA$burrowsPerHr$GRAY/${AQUA}hr$GRAY]"

        val profitPerHr = if (playTimeHrs > 0) Helper.formatNumber(totalProfitValue / playTimeHrs) else 0.0
        val profitPerBurrow = if (totalBurrows > 0) Helper.formatNumber(totalProfitValue / totalBurrows) else 0.0

        val screenOpen = isCraftingOpen || isCraftingScreenOpen()

        val stats = mutableListOf(
            OverlayTextLine("${GRAY}Total Burrows: $AQUA${Helper.formatNumber(totalBurrows, true)}$bphText"),
            createCoinLine(tracker),
            createProfitLine(totalProfitValue, profitPerHr, profitPerBurrow)
        )

        stats.add(timerLine)
        if (type == Diana.Tracker.TOTAL) stats.add(OverlayTextLine("${YELLOW}Total Events: $AQUA$totalEvents"))
        if (screenOpen && type == Diana.Tracker.SESSION) stats.add(resetSession)

        return stats
    }

    private fun createCoinLine(tracker: DianaTracker): OverlayTextLine {
        return OverlayTextLine("${GOLD}Total Coins: $AQUA${Helper.formatNumber(tracker.items.COINS)}")
            .onHover { drawContext, textRenderer ->
                val scaleFactor = mc.window.scaleFactor
                val mouseX = mc.mouse.x / scaleFactor
                val mouseY = mc.mouse.y / scaleFactor
                RenderUtils2D.drawHoveringString(drawContext,
                    "$YELLOW${BOLD}Coin Break Down:\n" +
                            "${GOLD}Treasure: $AQUA${Helper.formatNumber(tracker.items.COINS - tracker.items.FISH_COINS - tracker.items.SCAVENGER_COINS)}\n" +
                            "${GOLD}Four-Eyed Fish: $AQUA${Helper.formatNumber(tracker.items.FISH_COINS)}\n" +
                            "${GOLD}Scavenger: $AQUA${Helper.formatNumber(tracker.items.SCAVENGER_COINS)}",
                    mouseX, mouseY, textRenderer, overlay.scale)
            }
    }

    private fun createProfitLine(totalProfitValue: Long, profitPerHr: Any, profitPerBurrow: Any): OverlayTextLine {
        val pphText = if (profitPerHr == "NaN" || profitPerHr == "0.0") "" else "$GRAY[$AQUA$profitPerHr$GRAY/${AQUA}hr$GRAY]"
        return OverlayTextLine("${YELLOW}Total Profit: $AQUA${Helper.formatNumber(totalProfitValue)} $pphText")
            .onHover { drawContext, textRenderer ->
                val scaleFactor = mc.window.scaleFactor
                val mouseX = mc.mouse.x / scaleFactor
                val mouseY = mc.mouse.y / scaleFactor
                RenderUtils2D.drawHoveringString(drawContext,
                    "$GOLD$profitPerHr coins/hr\n" +
                            "$GOLD$profitPerBurrow coins/burrow",
                    mouseX, mouseY, textRenderer, overlay.scale)
            }
    }

    fun totalProfit(tracker: DianaTracker): Long {
        var totalProfit = 0L
        for (item in tracker.items::class.java.declaredFields) {
            item.isAccessible = true
            var itemName = item.name
            if (itemName == "TIME" || itemName == "TOTAL_BURROWS" || itemName == "COINS" || itemName == "SCAVENGER_COINS" || itemName == "FISH_COINS") continue
            itemName = itemName.replace("_LS", "")
            val itemValue = item.get(tracker.items) as Int
            if (itemValue <= 0) continue
            val itemPrice = Helper.getItemPrice(itemName)
            if (itemPrice > 0) {
                totalProfit += itemPrice * itemValue
            }
        }
        return totalProfit + tracker.items.COINS
    }

    fun updateTimerText() {
        val type = Diana.lootTracker
        val tracker = when (type) {
            Diana.Tracker.TOTAL -> SBOConfigBundle.dianaTrackerTotalData
            Diana.Tracker.EVENT -> SBOConfigBundle.dianaTrackerMayorData
            Diana.Tracker.SESSION -> SBOConfigBundle.dianaTrackerSessionData
            Diana.Tracker.OFF -> {
                timerLine.text = ""
                return
            }
        }

        val timer = when (type) {
            Diana.Tracker.TOTAL -> SboTimerManager.timerTotal
            Diana.Tracker.EVENT -> SboTimerManager.timerMayor
            Diana.Tracker.SESSION -> SboTimerManager.timerSession
            else -> return
        }

        val formattedTime = Helper.formatTime(tracker.items.TIME)
        val text = if (timer.running) {
            "${YELLOW}Playtime: $AQUA$formattedTime"
        } else {
            "${YELLOW}Playtime: $AQUA$formattedTime ${GRAY}[${RED}PAUSED${GRAY}]"
        }
        timerLine.text = text
    }

}
