package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category
import net.sbo.mod.config.Choice
import net.sbo.mod.config.ObservableEntry
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.EventsGui
import net.sbo.mod.diana.LootshareParty
import net.sbo.mod.overlays.Chains
import net.sbo.mod.overlays.CrownOfAvarice
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.overlays.DianaMobs
import net.sbo.mod.overlays.DianaStats

object Trackers : Category("Trackers") {
    override val description = "The loot, mob, stats and magic find trackers. You can interact with the trackers in the inventory, /sboguis moves them."

    // Option ids are the old enum names in lower case, the migration converts saved values like that
    val TRACKER_MODES = listOf(
        Choice("off", "Off"),
        Choice("total", "Total", "Everything you tracked, over all events."),
        Choice("event", "Event", "The current Diana event."),
        Choice("session", "Session", "Since you started the game, or since the last reset."),
    )

    val LOOT_SORTS = listOf(
        Choice("default", "Default"),
        Choice("price_desc", "Price ↓", "Most coins first."),
        Choice("price_asc", "Price ↑", "Fewest coins first."),
        Choice("amount_desc", "Amount ↓", "Most dropped first."),
        Choice("amount_asc", "Amount ↑", "Fewest dropped first."),
        Choice("custom", "Custom", "Your own order, set with the arrows on the lines in the inventory."),
    )

    val LINE_SORTS = listOf(
        Choice("default", "Default"),
        Choice("highest", "Highest ↓", "Highest count first."),
        Choice("lowest", "Lowest ↑", "Lowest count first."),
        Choice("custom", "Custom", "Your own order, set with the arrows on the lines in the inventory."),
    )

    val BAZAAR_PRICES = listOf(
        Choice("instasell", "Instasell", "What you get selling to buy orders right away."),
        Choice("selloffer", "Sell Offer", "What you get with a sell offer."),
    )

    // The trackers with a GuiLib look, for the look settings that apply to several of them
    val LOOKS = listOf(
        Choice("loot", "Loot"),
        Choice("mobs", "Mobs"),
        Choice("stats", "Stats"),
        Choice("coa", "CoA"),
        Choice("chains", "Chains"),
        Choice("lootshare", "Lootshare"),
    )

    init {
        button {
            title = "Open Events"
            text = "Open Events"
            description = "Opens the Events menu: the running Diana event, your saved trackers of previous events and the total, with details and comparisons. You can also open it with /sboevents."
            onClick {
                mc.schedule { EventsGui.open() }
            }
        }

        sub {
            this.title = "Trackers"
        }
    }

    var lootTracker by ObservableEntry(
        choice("event") {
            this.name = Literal("Loot Tracker")
            options(TRACKER_MODES)
            this.description = Literal(
                "Shows your Diana loot, /sboguis to move the overlay.\n" +
                    "§bNOTE!: You can interact with the tracker in the inventory!!!§r\n" +
                    "By clicking on a loot line you can hide/unhide it.\n" +
                    "Hovering over some lines may display additional information."
            )
        }
    ) { old, new ->
        if (old != new) {
            if (new != "off") {
                DianaLoot.updateLines()
            }
        }
    }

    var mobTracker by ObservableEntry(
        choice("off") {
            this.name = Literal("Mob Tracker")
            options(TRACKER_MODES)
            this.description = Literal(
                "Shows your Diana mob kills, /sboguis to move the overlay.\n" +
                "§bNOTE!: You can interact with the tracker in the inventory!!!§r\n" +
                "By clicking on a mob line you can hide/unhide it.\n" +
                "Hovering over some lines may display additional information."
            )
        }
    ) { old, new ->
        if (old != new) {
            if (new != "off") {
                DianaMobs.updateLines()
            }
        }
    }

    var coaTracker by ObservableEntry(
        choice("event") {
            this.name = Literal("CoA Tracker")
            options(TRACKER_MODES)
            this.description = Literal(
                "Shows a tracker for the Crown of Avarice. \n" +
                    "/sboguis to move the overlay.\n"
            )
        }
    ) { old, new ->
        if (old != new) {
            if (new != "off") {
                CrownOfAvarice.updateLines()
            }
        }
    }

    var statsTracker by boolean(false) {
        this.name = Literal("Diana Stats Tracker")
        this.description = Literal("Shows stats like Mobs since Inquisitor, Inquisitors since Chimera, /sboguis to move the overlay.")
    }

    var magicFindTracker by boolean(false) {
        this.name = Literal("Magic Find Tracker")
        this.description = Literal("Shows your highest magic find for sticks and chimeras (only after you dropped it once), /sboguis to move the overlay.")
    }

    init {
        sub {
            this.title = "Lines"
        }
    }

    var hideUnobtainedItems by ObservableEntry(
        boolean(true) {
            this.name = Literal("Hide Unobtained Items")
            dependsAny(state(::mobTracker, "on") { it != "off" }, state(::lootTracker, "on") { it != "off" })
            this.description = Literal("Hides any loot or mob lines that have not been tracked yet (value is 0) to reduce clutter in the overlays.")
        }
    ) { old, new ->
        if (old != new) {
            if (lootTracker != "off") {
                DianaLoot.updateLines()
            }
            if (mobTracker != "off") {
                DianaMobs.updateLines()
            }
        }
    }

    var combineLootLines by ObservableEntry(
        boolean(false) {
            this.name = Literal("Combine LS Loot")
            depends(state(::lootTracker, "on") { it != "off" })
            this.description = Literal("Combines the base item and the Loot Share (LS) variant into a single line. The individual LS count is shown on hover.")
        }
    ) { old, new ->
        if (old != new) {
            if (lootTracker != "off") {
                DianaLoot.updateLines()
            }
        }
    }

    var hideLsStats by ObservableEntry(boolean(false) {
            this.name = Literal("Hide LS Stats")
            dependsAny(state(::mobTracker, "on") { it != "off" }, on(::statsTracker))
            this.description = Literal("Hides the Loot Share (LS) portion of the Diana stats lines.")
        }
    ) { old, new ->
        if (old != new) {
            DianaStats.updateLines()
        }
    }

    var lootSort by ObservableEntry(
        choice("default") {
            this.name = Literal("Loot Sort")
            options(LOOT_SORTS)
            depends(state(::lootTracker, "on") { it != "off" })
            this.description = Literal(
                "Order of the loot tracker lines: the default order, by price or by amount (highest or lowest first), or your own order.\n" +
                    "It can also be changed by clicking the sort badge of the tracker in the inventory. With Custom, the arrows next to each line move it up or down there (new look only)."
            )
        }
    ) { old, new ->
        if (old != new) {
            if (lootTracker != "off") {
                DianaLoot.updateLines()
            }
        }
    }

    var mobSort by ObservableEntry(
        choice("default") {
            this.name = Literal("Mob Sort")
            options(LINE_SORTS)
            depends(state(::mobTracker, "on") { it != "off" })
            this.description = Literal(
                "Order of the mob tracker lines: the default order, by kills (most or fewest first), or your own order.\n" +
                    "It can also be changed by clicking the sort badge of the tracker in the inventory. With Custom, the arrows next to each line move it up or down there (new look only)."
            )
        }
    ) { old, new ->
        if (old != new) {
            if (mobTracker != "off") {
                DianaMobs.updateLines()
            }
        }
    }

    var statsSort by ObservableEntry(
        choice("default") {
            this.name = Literal("Stats Sort")
            options(LINE_SORTS)
            depends(on(::statsTracker))
            this.description = Literal(
                "Order of the Diana stats lines: the default order, by count (highest or lowest first), or your own order.\n" +
                    "It can also be changed by clicking the sort badge of the tracker in the inventory. With Custom, the arrows next to each line move it up or down there (new look only)."
            )
        }
    ) { old, new ->
        if (old != new) {
            DianaStats.updateLines()
        }
    }

    init {
        sub {
            this.title = "Look"
        }
    }

    var legacyTrackers by ObservableEntry(choices {
            this.name = Literal("Tracker Legacy Look")
            options(LOOKS)
            this.description = Literal("The trackers shown as Minecraft text lines like the other overlays instead of the new box. Slightly better performance: plain text is the cheapest overlay to draw every frame.")
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    var trackerFont by ObservableEntry(choice("minecraft") {
            this.name = Literal("Tracker Font")
            options(listOf(Choice("minecraft", "Minecraft"), Choice("inter", "Inter")))
            depends(state(::legacyTrackers, "not all legacy") { it.size < LOOKS.size })
            this.description = Literal("Font of the trackers with the new look. Minecraft has better performance (about 30% less work per frame than Inter), Inter looks smoother.")
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    var trackerBackground by ObservableEntry(choices(*LOOKS.map { it.id }.toTypedArray()) {
            this.name = Literal("Tracker Background")
            options(LOOKS)
            depends(state(::legacyTrackers, "not all legacy") { it.size < LOOKS.size })
            this.description = Literal("The trackers with the new look that get a dark box behind them. The others show only the text, with a shadow so it stays readable, and slightly better performance since the box isn't drawn.")
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    /** [look] is an id of [LOOKS]. */
    fun isLegacy(look: String): Boolean = look in legacyTrackers

    fun hasBackground(look: String): Boolean = look in trackerBackground

    // The trackers pick the changed look up on their next tick, the settings can also change while the config loads
    fun onTrackerLookChanged() {
        DianaLoot.onLookChanged()
        DianaMobs.onLookChanged()
        DianaStats.onLookChanged()
        CrownOfAvarice.onLookChanged()
        Chains.onLookChanged()
        LootshareParty.onLookChanged()
    }

    init {
        sub {
            this.title = "Prices"
        }
    }

    // Was the two switches NPC Price Overrides and Ironman Overrides
    var priceSource by choice("market") {
        this.name = Literal("Prices")
        this.description = Literal("Where the loot prices come from, hover an option for what it does.")
        options(listOf(
            Choice("market", "Bazaar & AH", "The market price of every item."),
            Choice("npc_items", "NPC for some items", "NPC prices for the pet item drops (Cretan Urn, Dwarf Turtle Shelmet, Antique Remedies, Washed Up Souvenir, Crochet Tiger Plushie) and the Hilt of Revelations, useful if you NPC sell them."),
            Choice("npc_only", "NPC only (Ironman)", "Only NPC prices, items without one have no value."),
        ))
    }

    var bazaarSettingDiana by choice("selloffer") {
        options(BAZAAR_PRICES)
        this.name = Literal("Bazaar Setting")
        this.description = Literal("Bazaar setting to set the price for loot.")
    }

    var excludeCoinsFromProfit by boolean(false) {
        this.name = Literal("Exclude Coins from Profit")
        this.description = Literal("Excludes coins from the loot tracker's profit calculation when enabled, useful if using a non-1B crown since coins are consumed.")
    }

    val npcOnlyPrices: Boolean get() = priceSource == "npc_only"

    init {
        sub {
            this.title = "Tracking"
        }
    }

    var afkTimeout by int(60) {
        this.name = Literal("AFK Timeout")
        this.description = Literal("Pause the trackers if it is not modified for this amount of seconds. Minimum 15, maximum 900 seconds is allowed.")
        this.range = 15..900
    }

    var assumeAllLS by boolean(false) {
        this.name = Literal("Assume All LS")
        this.description = Literal("Assumes you get loot share on all Kings, Inquisitors and Manticores. This works around the LOOT SHARE! message not being always sent by the server, but might inflate your numbers if you don't actually do enough damage to these mobs for lootshare. To reduce false positives a bit (not fully!), you need to be 32 blocks nearby the rare mob when it died.")
    }

    var fourEyedFish by boolean(false) {
        this.name = Literal("Four-Eyed Fish")
        this.description = Literal("Set if you have a Four-Eyed Fish on your griffin pet.")
    }

    var resetSessionOnGameRestart by boolean(false) {
        this.name = Literal("Reset Session on Game Restart")
        this.description = Literal("Resets the Diana session tracker when you restart Minecraft.")
    }
}
