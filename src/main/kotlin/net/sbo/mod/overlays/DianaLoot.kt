package net.sbo.mod.overlays

import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.ChatFormatting.*
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.overlays.OverlayUtils.LootItemData
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.calcPercentOneReflectively
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.data.configs.diana.DianaTracker
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiCloseEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.overlay.*
import net.sbo.mod.utils.render.RenderUtils2D
import net.sbo.mod.utils.data.configs.diana.DianaItemsData
import net.sbo.mod.utils.game.ItemLookup
import net.sbo.mod.utils.game.World
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object DianaLoot : DianaOverlay<DianaLoot.View>("Diana Loot", Diana.TrackerOverlay.LOOT, { Diana.lootTracker != Diana.Tracker.OFF }) {
    private var isSellTypeHovered = false
    private val timerLine: OverlayTextLine = OverlayTextLine("")

    private val changeView: OverlayTextLine = OverlayUtils.createClickableTextLine(
        text = "${YELLOW}Change View",
        hoverText = "$YELLOW${UNDERLINE}Change View",
        defaultText = "${YELLOW}Change View",
        onClick = {
            Diana.lootTracker = Diana.lootTracker.next()
            updateLines()
        },
        lineBreak = false
    )

    private val delimiter = OverlayTextLine(" | ", linebreak = false)

    private val changeSellType: OverlayTextLine = OverlayUtils.createClickableTextLine(
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

    private val resetSession = OverlayUtils.createClickableTextLine(
        text = "${RED}Reset Session",
        hoverText = "$RED${UNDERLINE}Reset Session",
        defaultText = "${RED}Reset Session",
        onClick = {
            resetSession()
        }
    )

    fun resetSession() {
        SboTimerManager.timerSession.reset()
        SboTimerManager.timerCoaSession.reset()
        DataManager.dianaTrackerSessionData.reset()
        DataManager.save(DataManager::dianaTrackerSessionData)
        updateLines()
        DianaMobs.updateLines()
    }

    private val LOOT_ITEMS = listOf(
        LootItemData("MYTHOLOGICAL_DYE", "Mythological Dye", RED),
        LootItemData("MYTH_THE_FISH", "Myth the Fish", RED),
        LootItemData("SHIMMERING_WOOL", "Shimmering Wool", RED, combined = true, dropMobId = "KING_MINOS", dropMobLsId = "KING_MINOS_LS"),
        LootItemData("MANTI_CORE", "Manti-core", RED, combined = true, dropMobId = "MANTICORE", dropMobLsId = "MANTICORE_LS"),
        LootItemData("KING_MINOS_SHARD", "King Minos Shard", RED, isRarerDrop = true),
        LootItemData("FATEFUL_STINGER", "Fateful Stinger", LIGHT_PURPLE, combined = true, dropMobId = "MANTICORE", dropMobLsId = "MANTICORE_LS"),
        LootItemData("CHIMERA", "Chimera", LIGHT_PURPLE, combined = true, dropMobId = "MINOS_INQUISITOR", dropMobLsId = "MINOS_INQUISITOR_LS"),
        LootItemData("BRAIN_FOOD", "Brain Food", DARK_PURPLE, combined = true, dropMobId = "SPHINX", dropMobLsId = "SPHINX_LS"),
        LootItemData("MINOS_RELIC", "Minos Relic", DARK_PURPLE, isRarerDrop = true, dropMobId = "MINOS_CHAMPION"),
        LootItemData("SPHINX_SHARD", "Sphinx Shard", DARK_PURPLE, isRarerDrop = true),
        LootItemData("BRAIDED_GRIFFIN_FEATHER", "Braided Griffin Feather", DARK_PURPLE, isRarerDrop = true),
        LootItemData("DAEDALUS_STICK", "Daedalus Stick", GOLD, isRarerDrop = true, dropMobId = "MINOTAUR"),
        LootItemData("MINOTAUR_SHARD", "Minotaur Shard", GOLD, isRarerDrop = true),
        LootItemData("CROWN_OF_GREED", "Crown of Greed", GOLD, isRarerDrop = true),
        LootItemData("GRIFFIN_FEATHER", "Griffin Feather", GOLD),
        LootItemData("MYTHOS_FRAGMENT", "Mytho Fragment", GOLD),
        LootItemData("WASHED_UP_SOUVENIR", "Washed-up Souvenir", GOLD, isRarerDrop = true),
        LootItemData("CRETAN_URN", "Cretan Urn", DARK_GREEN),
        LootItemData("DWARF_TURTLE_SHELMET", "Dwarf Turtle Shelmet", DARK_GREEN),
        LootItemData("CROCHET_TIGER_PLUSHIE", "Crochet Tiger Plushie", DARK_GREEN),
        LootItemData("ANTIQUE_REMEDIES", "Antique Remedies", DARK_GREEN),
        LootItemData("CRETAN_BULL_SHARD", "Cretan Bull Shard", DARK_GREEN),
        LootItemData("HARPY_SHARD", "Harpy Shard", DARK_GREEN),
        LootItemData("HILT_OF_REVELATIONS", "Hilt of Revelations", BLUE),
        LootItemData("ANCIENT_CLAW", "Ancient Claw", BLUE),
        LootItemData("ENCHANTED_ANCIENT_CLAW", "Enchanted Ancient Claw", BLUE),
        LootItemData("ENCHANTED_GOLD", "Enchanted Gold", BLUE),
        LootItemData("FAKE_SHURIKEN", "Shuriken", RED, negativeValue = true),
    )

    fun init() {
        initOverlays()
        updateTimerText()
        updateLines()
        shurikenTracker()

        Register.onTick(1) { updateTimerText() }
        Register.onTick(20) {
            updateLines()
        }
    }

    @SboEvent
    fun onGuiClose(event: GuiCloseEvent) {
        if (CRAFTING_PLAYER_INVENTORY_FILTER(event.screen)) {
            overlay.removeLines(listOf(changeView, delimiter, changeSellType, resetSession))
            updateLines()
        }
    }

    @SboEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (CRAFTING_PLAYER_INVENTORY_FILTER(event.screen)) {
            updateLines()
        }
    }

    private fun shurikenTracker() {
        // On item use
        UseItemCallback.EVENT.register { player, _, hand ->
            val isDiana = World.isInHub() && Helper.hasSpade
            if (player.getItemInHand(hand).isShuriken() && isDiana) {
                listOf(
                    DataManager.dianaTrackerTotalData,
                    DataManager.dianaTrackerMayorData,
                    DataManager.dianaTrackerSessionData
                ).forEach { it.items.FAKE_SHURIKEN += 1 }
                updateLines()
            }

            return@register InteractionResult.PASS
        }

        // Cancel the shuriken addition if on cooldown
        Register.onChatMessage(Regex("§cYou need to wait before you can use this again.")) { _, _ ->
            val isDiana = World.isInHub() && Helper.hasSpade
            if (mc.player?.mainHandItem?.isShuriken() ?: false && isDiana) {
                listOf(
                    DataManager.dianaTrackerTotalData,
                    DataManager.dianaTrackerMayorData,
                    DataManager.dianaTrackerSessionData
                ).forEach { it.items.FAKE_SHURIKEN -= 1 }
                updateLines()
            }
        }

        // No need to use a UseBlockCallback bc you can't use the shuriken while looking at a block
    }

    private fun ItemStack.isShuriken(): Boolean {
        val id = ItemLookup(this).sbId
        return id == "FAKE_SHURIKEN"
    }

    private fun createLootLine(data: LootItemData, tracker: DianaTracker): OverlayTextLine {
        val itemName = data.id
        val amount = tracker.getAmountOf(itemName)
        val color = if (Diana.ironmanOverrides && data.id == "WASHED_UP_SOUVENIR") DARK_GREEN else data.color
        val formattedName = "$color${data.name}: $AQUA${Helper.formatNumber(amount, withCommas = true)}"
        val price = (if (data.negativeValue) "$RED-" else "") + Helper.getItemPriceFormatted(itemName.replace("_LS", ""), amount)
        val percent = data.dropMobId?.let { dropId ->
            calcPercentOneReflectively(tracker.items, tracker.mobs, itemName, dropId)
        }
        val percentText = percent?.let { " $GRAY($AQUA$it%$GRAY)" } ?: ""
        val hidePrice = Diana.ironmanOverrides && Helper.getItemPrice(itemName.replace("_LS", ""), amount) == 0L
        val formattedText = "${if (hidePrice) "$DARK_GRAY  -  " else "$GOLD$price"} $GRAY| $formattedName$percentText"
        val line = OverlayTextLine(formattedText).onClick { toggleHidden(itemName) }
            .setCondition {
                val meetsZeroValueCondition = amount > 0 || !Diana.hideUnobtainedItems
                val meetsManualHideCondition = !(!isCraftingScreenOpen() && itemName in DataManager.sboData.hideTrackerLines)
                meetsZeroValueCondition && meetsManualHideCondition
            }
        if (itemName in DataManager.sboData.hideTrackerLines) {
            line.text = "$GRAY$STRIKETHROUGH${formattedText.removeFormatting()}"
        }
        return line
    }

    private fun createCombinedLootLine(data: LootItemData, tracker: DianaTracker): OverlayTextLine {
        val itemNameBase = data.id
        val itemNameLs = "${data.id}_LS"
        val amountBase = tracker.getAmountOf(itemNameBase)
        val amountLs = tracker.getAmountOf(itemNameLs)
        val totalAmount = amountBase + amountLs
        val hidePrices = Diana.ironmanOverrides && Helper.getItemPrice(itemNameBase, totalAmount) == 0L
        val priceLs = if (hidePrices) "$DARK_GRAY  -  " else "$GOLD${Helper.getItemPriceFormatted(itemNameLs.replace("_LS", ""), amountLs)}"
        val priceCombined = if (hidePrices) "$DARK_GRAY  -  " else "$GOLD${Helper.getItemPriceFormatted(itemNameBase, totalAmount)}"
        val percent = data.dropMobId?.let { dropId ->
            calcPercentOneReflectively(tracker.items, tracker.mobs, itemNameBase, dropId)
        }
        val percentText = percent?.let { " $GRAY($AQUA$it%$GRAY)" } ?: ""
        val percentLs = data.dropMobLsId?.let { dropLsId ->
            calcPercentOneReflectively(tracker.items, tracker.mobs, itemNameLs, dropLsId)
        }
        val percentLsText = percentLs?.let { " $GRAY($AQUA$it%$GRAY)" } ?: ""
        val color = if (Diana.ironmanOverrides && data.id == "WASHED_UP_SOUVENIR") DARK_GREEN else data.color
        val baseText = "$priceCombined $GRAY| $color${data.name}: $AQUA${Helper.formatNumber(amountBase,
            withCommas = true
        )}$percentText"
        val lsText = "$priceLs $GRAY| $color${data.name} $GRAY[${AQUA}LS$GRAY]: $AQUA${Helper.formatNumber(amountLs,
            withCommas = true
        )}$percentLsText"
        val combinedText = "$baseText $GRAY[${AQUA}LS$GRAY:$AQUA${Helper.formatNumber(amountLs, withCommas = true)}$GRAY]"

        val line = OverlayTextLine(combinedText).onClick { toggleHidden(itemNameBase) }
            .setCondition {
                val meetsZeroValueCondition = totalAmount > 0 || !Diana.hideUnobtainedItems
                val meetsManualHideCondition = !(!isCraftingScreenOpen() && itemNameBase in DataManager.sboData.hideTrackerLines)
                meetsZeroValueCondition && meetsManualHideCondition
            }
            .onHover { drawContext, textRenderer ->
                val scaleFactor = mc.window.guiScale
                val mouseX = mc.mouseHandler.xpos() / scaleFactor
                val mouseY = mc.mouseHandler.ypos() / scaleFactor

                RenderUtils2D.drawHoveringString(
                    drawContext,
                    "$YELLOW${Helper.toTitleCase(itemNameBase.replace("_", " ").lowercase())} LS Details:\n" +
                            lsText,
                    mouseX, mouseY, textRenderer
                )
            }
        if (itemNameBase in DataManager.sboData.hideTrackerLines) {
            line.text = "$GRAY$STRIKETHROUGH${combinedText.removeFormatting()}"
        }
        return line
    }

    data class View(
        val mode: String,
        val sellType: String,
        val session: Boolean,
        val rows: List<Row>,
        val stats: List<Row>,
        val inventory: Boolean
    )

    override fun view(): View {
        val type = Diana.lootTracker
        val inventory = isCraftingScreenOpen()
        val sellType = Helper.toTitleCase(Diana.bazaarSettingDiana.toString())
        val tracker = getDianaTracker(type) ?: return View("", sellType, false, emptyList(), emptyList(), inventory)
        val rows = mutableListOf<Row>()
        for (data in LOOT_ITEMS) {
            if (data.combined && Diana.combineLootLines) {
                lootRow(data, tracker, inventory, combined = true)?.let(rows::add)
            } else {
                lootRow(data, tracker, inventory)?.let(rows::add)
                if (data.combined) lootRow(data, tracker, inventory, ls = true)?.let(rows::add)
            }
        }
        return View(Helper.toTitleCase("$type"), sellType, type == Diana.Tracker.SESSION, rows, statRows(tracker, type), inventory)
    }

    // One loot line of the new look, null while it is hidden; [ls] for the lootshare line, [combined] for both in one
    private fun lootRow(data: LootItemData, tracker: DianaTracker, inventory: Boolean, ls: Boolean = false, combined: Boolean = false): Row? {
        val id = if (ls) "${data.id}_LS" else data.id
        val amount = tracker.getAmountOf(id)
        val lsAmount = if (combined) tracker.getAmountOf("${data.id}_LS") else 0
        if (amount + lsAmount <= 0 && Diana.hideUnobtainedItems) return null
        // Hidden lines only show (crossed out) in the inventory, to unhide them
        val hidden = id in DataManager.sboData.hideTrackerLines
        if (hidden && !inventory) return null
        val dropMob = if (ls) data.dropMobLsId else data.dropMobId
        val percent = dropMob?.let { "${calcPercentOneReflectively(tracker.items, tracker.mobs, id, it)}%" }
        val lsText = if (combined) "LS ${Helper.formatNumber(lsAmount, withCommas = true)}" else null
        val hidePrice = Diana.ironmanOverrides && Helper.getItemPrice(data.id, amount + lsAmount) == 0L
        return Row(
            id = id,
            name = if (ls) "${data.name} LS" else data.name,
            color = if (Diana.ironmanOverrides && data.id == "WASHED_UP_SOUVENIR") DARK_GREEN else data.color,
            value = Helper.formatNumber(amount, withCommas = true),
            extra = listOfNotNull(percent, lsText).joinToString("  ").ifEmpty { null },
            price = if (hidePrice) "-" else (if (data.negativeValue) "$RED-" else "") + (Helper.getItemPriceFormatted(data.id, amount + lsAmount)),
            hidden = hidden
        )
    }

    private fun statRows(tracker: DianaTracker, type: Diana.Tracker): List<Row> {
        val timer = timer(type)
        val totalProfit = totalProfit(tracker)
        val burrowsPerHr = Helper.getBurrowsPerHr(tracker, timer)
        val profitPerHr = profitPerHour(tracker, totalProfit)
        val rows = mutableListOf(
            Row(null, "Burrows", null, Helper.formatNumber(tracker.items.TOTAL_BURROWS, withCommas = true), if (burrowsPerHr.isNaN()) null else "$burrowsPerHr/h"),
            Row("coins", "Coins", null, Helper.formatNumber(tracker.items.COINS), if (Diana.excludeCoinsFromProfit) "Excluded" else null, valueClass = "coins"),
            Row("profit", "Profit", null, Helper.formatNumber(totalProfit), if (profitPerHr > 0.0) "${Helper.formatNumber(profitPerHr)}/h" else null, valueClass = "profit"),
            Row(null, "Playtime", null, Helper.formatTime(tracker.items.TIME), if (timer.isRunning()) null else "Paused")
        )
        if (type == Diana.Tracker.TOTAL) rows += Row(null, "Events", null, DataManager.pastDianaEventsData.events.size.toString())
        return rows
    }

    override fun NodeBuilder.content(view: View) {
        tracker("loot") {
            head("Diana Loot") {
                if (view.inventory) {
                    badge(view.sellType, "sell", clickable = true)
                    if (view.session) badge("Reset", "reset", clickable = true, extraClass = "danger")
                }
                badge(view.mode, "mode", view.inventory)
            }
            view.rows.forEach { row(it) }
            separator()
            view.stats.forEach { row(it) }
            if (view.inventory) hint("Click an item to hide it, the badges to change them")
        }
    }

    override fun elementClicked(id: String) {
        when (id) {
            "mode" -> {
                Diana.lootTracker = Diana.lootTracker.next()
                updateLines()
            }
            "sell" -> {
                Diana.bazaarSettingDiana = Diana.bazaarSettingDiana.next()
                updateLines()
            }
            "reset" -> resetSession()
            // Coins and profit only have a tooltip
            else -> rowId(id)?.takeIf { it != "coins" && it != "profit" }?.let(::toggleHidden)
        }
    }

    override fun tooltip(id: String): String? {
        val tracker = getDianaTracker(Diana.lootTracker) ?: return null
        return when (val row = rowId(id)) {
            "coins" -> coinBreakdown(tracker)
            "profit" -> {
                val totalProfit = totalProfit(tracker)
                profitBreakdown(Helper.formatNumber(profitPerHour(tracker, totalProfit)), profitPerBurrow(tracker, totalProfit), CrownOfAvarice.getProfitPerHour())
            }
            null -> null
            // The lootshare count of a combined line
            else -> LOOT_ITEMS.find { it.id == row && it.combined && Diana.combineLootLines }?.let { lsDetails(it, tracker) }
        }
    }

    private fun timer(type: Diana.Tracker) = when (type) {
        Diana.Tracker.TOTAL -> SboTimerManager.timerTotal
        Diana.Tracker.SESSION -> SboTimerManager.timerSession
        else -> SboTimerManager.timerMayor
    }

    // Item profit per hour played plus the CoA profit per hour
    private fun profitPerHour(tracker: DianaTracker, totalProfit: Long): Double {
        val playTimeHrs = tracker.items.TIME.toDouble() / TimeUnit.HOURS.toMillis(1)
        return (if (playTimeHrs > 0) totalProfit / playTimeHrs else 0.0) + CrownOfAvarice.getProfitPerHour()
    }

    private fun profitPerBurrow(tracker: DianaTracker, totalProfit: Long): Any {
        val totalBurrows = tracker.items.TOTAL_BURROWS
        return if (totalBurrows > 0) Helper.formatNumber(totalProfit / totalBurrows) else 0.0
    }

    private fun coinBreakdown(tracker: DianaTracker): String =
        "$YELLOW${BOLD}Coin Break Down:\n" +
            "${GOLD}Treasure: $AQUA${Helper.formatNumber(tracker.items.COINS - tracker.items.FISH_COINS - tracker.items.SCAVENGER_COINS)}\n" +
            "${GOLD}Four-Eyed Fish: $AQUA${Helper.formatNumber(tracker.items.FISH_COINS)}\n" +
            "${GOLD}Scavenger: $AQUA${Helper.formatNumber(tracker.items.SCAVENGER_COINS)}"

    private fun profitBreakdown(profitPerHr: Any, profitPerBurrow: Any, coaProfitPerHr: Number): String =
        "$GOLD$profitPerHr coins/hr\n" +
            "$GOLD$profitPerBurrow coins/burrow\n" +
            "$GOLD${Helper.formatNumber(coaProfitPerHr)} coa coins/hr"

    private fun lsDetails(data: LootItemData, tracker: DianaTracker): String {
        val itemNameLs = "${data.id}_LS"
        val amountLs = tracker.getAmountOf(itemNameLs)
        val hidePrices = Diana.ironmanOverrides && Helper.getItemPrice(data.id, tracker.getAmountOf(data.id) + amountLs) == 0L
        val priceLs = if (hidePrices) "$DARK_GRAY  -  " else "$GOLD${Helper.getItemPriceFormatted(data.id, amountLs)}"
        val percentLs = data.dropMobLsId?.let { " $GRAY($AQUA${calcPercentOneReflectively(tracker.items, tracker.mobs, itemNameLs, it)}%$GRAY)" } ?: ""
        val color = if (Diana.ironmanOverrides && data.id == "WASHED_UP_SOUVENIR") DARK_GREEN else data.color
        return "$YELLOW${Helper.toTitleCase(data.id.replace("_", " ").lowercase())} LS Details:\n" +
            "$priceLs $GRAY| $color${data.name} $GRAY[${AQUA}LS$GRAY]: $AQUA${Helper.formatNumber(amountLs, withCommas = true)}$percentLs"
    }

    override fun generateLines(): List<OverlayTextLine> {
        val type = Diana.lootTracker
        val tracker = getDianaTracker(type) ?: run {
            return@generateLines emptyList()
        }

        val isCraftingOpen = CRAFTING_PLAYER_INVENTORY_FILTER(mc.screen)
        val lines = mutableListOf<OverlayTextLine>()

        updateControlLines(lines, isCraftingOpen)
        lines.add(OverlayTextLine("$YELLOW${BOLD}Diana Loot $GRAY($YELLOW${Helper.toTitleCase("$type")}$GRAY)"))
        lines.addAll(generateLootLines(tracker))
        lines.addAll(generateStatisticsLines(tracker, type))

        return lines
    }

    private fun getDianaTracker(type: Diana.Tracker): DianaTracker? {
        return when (type) {
            Diana.Tracker.TOTAL -> DataManager.dianaTrackerTotalData
            Diana.Tracker.EVENT -> DataManager.dianaTrackerMayorData
            Diana.Tracker.SESSION -> DataManager.dianaTrackerSessionData
            Diana.Tracker.OFF -> null
        }
    }

    private fun updateControlLines(lines: MutableList<OverlayTextLine>, isCraftingOpen: Boolean) {
        val screenOpen = isCraftingOpen || isCraftingScreenOpen()
        val sellTypeText = if (isSellTypeHovered) {
            "$YELLOW$UNDERLINE${Helper.toTitleCase(Diana.bazaarSettingDiana.toString())}"
        } else {
            "$YELLOW${Helper.toTitleCase(Diana.bazaarSettingDiana.toString())}"
        }
        changeSellType.text = sellTypeText

        if (screenOpen) {
            lines.addAll(listOf(changeView, delimiter, changeSellType))
            if (Diana.lootTracker == Diana.Tracker.SESSION) lines.add(resetSession)
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

    private fun generateStatisticsLines(tracker: DianaTracker, type: Diana.Tracker): List<OverlayTextLine> {
        val timer = when (type) {
            Diana.Tracker.TOTAL -> SboTimerManager.timerTotal
            Diana.Tracker.EVENT -> SboTimerManager.timerMayor
            Diana.Tracker.SESSION -> SboTimerManager.timerSession
            else -> SboTimerManager.timerMayor
        }
        val totalBurrows = tracker.items.TOTAL_BURROWS
        val totalProfitValue = totalProfit(tracker)
        val playTimeHrs = tracker.items.TIME.toDouble() / TimeUnit.HOURS.toMillis(1)
        val totalEvents = DataManager.pastDianaEventsData.events.size

        val burrowsPerHr = Helper.getBurrowsPerHr(tracker, timer)
        val bphText = if (burrowsPerHr.isNaN()) "" else " $GRAY[$AQUA$burrowsPerHr$GRAY/${AQUA}hr$GRAY]"

        val coaProfitPerHr = CrownOfAvarice.getProfitPerHour()
        val profitPerHr = Helper.formatNumber((if (playTimeHrs > 0) totalProfitValue / playTimeHrs else 0.0) + coaProfitPerHr)
        val profitPerBurrow = if (totalBurrows > 0) Helper.formatNumber(totalProfitValue / totalBurrows) else 0.0

        val stats = mutableListOf(
            OverlayTextLine("${GRAY}Total Burrows: $AQUA${Helper.formatNumber(totalBurrows, withCommas = true)}$bphText"),
            createCoinLine(tracker),
            createProfitLine(totalProfitValue, profitPerHr, profitPerBurrow, coaProfitPerHr)
        )

        stats.add(timerLine)
        if (type == Diana.Tracker.TOTAL) stats.add(OverlayTextLine("${YELLOW}Total Events: $AQUA$totalEvents"))

        return stats
    }

    private fun createCoinLine(tracker: DianaTracker): OverlayTextLine {
        return OverlayTextLine("${GOLD}Total Coins: $AQUA${Helper.formatNumber(tracker.items.COINS)}" + if (Diana.excludeCoinsFromProfit) " $GRAY[${RED}EXCLUDED$GRAY]" else "")
            .onHover { drawContext, textRenderer ->
                val scaleFactor = mc.window.guiScale
                val mouseX = mc.mouseHandler.xpos() / scaleFactor
                val mouseY = mc.mouseHandler.ypos() / scaleFactor
                RenderUtils2D.drawHoveringString(drawContext,
                    "$YELLOW${BOLD}Coin Break Down:\n" +
                            "${GOLD}Treasure: $AQUA${Helper.formatNumber(tracker.items.COINS - tracker.items.FISH_COINS - tracker.items.SCAVENGER_COINS)}\n" +
                            "${GOLD}Four-Eyed Fish: $AQUA${Helper.formatNumber(tracker.items.FISH_COINS)}\n" +
                            "${GOLD}Scavenger: $AQUA${Helper.formatNumber(tracker.items.SCAVENGER_COINS)}",
                    mouseX, mouseY, textRenderer)
            }
    }

    private fun createProfitLine(totalProfitValue: Long, profitPerHr: Any, profitPerBurrow: Any, coaProfitPerHr: Number): OverlayTextLine {
        val pphText = if (profitPerHr == "NaN" || profitPerHr == "0.0") "" else "$GRAY[$AQUA$profitPerHr$GRAY/${AQUA}hr$GRAY]"
        return OverlayTextLine("${YELLOW}Total Profit: $AQUA${Helper.formatNumber(totalProfitValue)} $pphText")
            .onHover { drawContext, textRenderer ->
                val scaleFactor = mc.window.guiScale
                val mouseX = mc.mouseHandler.xpos() / scaleFactor
                val mouseY = mc.mouseHandler.ypos() / scaleFactor
                RenderUtils2D.drawHoveringString(drawContext,
                    "$GOLD$profitPerHr coins/hr\n" +
                            "$GOLD$profitPerBurrow coins/burrow\n" +
                            "$GOLD${Helper.formatNumber(coaProfitPerHr)} coa coins/hr",
                    mouseX, mouseY, textRenderer)
            }
    }

    fun totalProfit(tracker: DianaTracker): Long {
        val totalProfit = profitByItem(tracker).values.sum()
        return if (Diana.excludeCoinsFromProfit) totalProfit else totalProfit + tracker.items.COINS
    }

    private val excludedProfitFields = setOf(
        "TIME",
        "TOTAL_BURROWS",
        "COINS",
        "SCAVENGER_COINS",
        "FISH_COINS",
        "COA_COINS",
        "CURRENT_COA_START_COINS",
        "COA_TIME"
    )

    private val profitFields = DianaItemsData::class.java.declaredFields
        .filterNot { it.name in excludedProfitFields }
        .map { field ->
            field.isAccessible = true
            field to field.name.replace("_LS", "")
        }

    /** Coins worth of every dropped item with a price (field name without "_LS", lootshare drops added in), coins not included. */
    fun profitByItem(tracker: DianaTracker): Map<String, Long> {
        val profit = HashMap<String, Long>()

        for ((field, itemName) in profitFields) {
            val itemValue = field.getInt(tracker.items)
            if (itemValue <= 0) continue

            val itemPrice = Helper.getItemPrice(itemName) * (if (field.name == "FAKE_SHURIKEN") -1 else 1)
            if (itemPrice <= 0 && field.name != "FAKE_SHURIKEN") continue

            profit[itemName] = (profit[itemName] ?: 0L) + itemPrice * itemValue
        }

        return profit
    }

    private fun updateTimerText() {
        val type = Diana.lootTracker
        val tracker = when (type) {
            Diana.Tracker.TOTAL -> DataManager.dianaTrackerTotalData
            Diana.Tracker.EVENT -> DataManager.dianaTrackerMayorData
            Diana.Tracker.SESSION -> DataManager.dianaTrackerSessionData
            Diana.Tracker.OFF -> {
                timerLine.text = ""
                return
            }
        }

        val timer = when (type) {
            Diana.Tracker.TOTAL -> SboTimerManager.timerTotal
            Diana.Tracker.EVENT -> SboTimerManager.timerMayor
            Diana.Tracker.SESSION -> SboTimerManager.timerSession
            Diana.Tracker.OFF -> {
                timerLine.text = ""
                return
            }
        }

        val formattedTime = Helper.formatTime(tracker.items.TIME)
        val text = if (timer.isRunning()) {
            "${YELLOW}Playtime: $AQUA$formattedTime"
        } else {
            "${YELLOW}Playtime: $AQUA$formattedTime $GRAY[${RED}PAUSED$GRAY]"
        }
        timerLine.text = text
    }

}
