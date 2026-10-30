package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category
import net.sbo.mod.config.ObservableEntry
import net.sbo.mod.config.OffOption
import net.sbo.mod.config.Option
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

    // Saved as the lower case names, the migration turns the names saved before into those

    enum class TrackerMode(override val label: String, override val description: String = "") : Option {
        @OffOption OFF("Off"),
        TOTAL("Total", "Everything you tracked, over all events."),
        EVENT("Event", "The current Diana event."),
        SESSION("Session", "Since you started the game, or since the last reset."),
    }

    enum class LootSort(override val label: String, override val description: String = "") : Option {
        DEFAULT("Default"),
        PRICE_DESC("Price ↓", "Most coins first."),
        PRICE_ASC("Price ↑", "Fewest coins first."),
        AMOUNT_DESC("Amount ↓", "Most dropped first."),
        AMOUNT_ASC("Amount ↑", "Fewest dropped first."),
        CUSTOM("Custom", "Your own order, set with the arrows on the lines in the inventory."),
    }

    // Order of the mob and stats tracker lines
    enum class LineSort(override val label: String, override val description: String = "") : Option {
        DEFAULT("Default"),
        HIGHEST("Highest ↓", "Highest count first."),
        LOWEST("Lowest ↑", "Lowest count first."),
        CUSTOM("Custom", "Your own order, set with the arrows on the lines in the inventory."),
    }

    enum class BazaarPrice(override val label: String, override val description: String) : Option {
        INSTASELL("Instasell", "What you get selling to buy orders right away."),
        SELLOFFER("Sell Offer", "What you get with a sell offer."),
    }

    // Was the two switches NPC Price Overrides and Ironman Overrides
    enum class PriceSource(override val label: String, override val description: String) : Option {
        MARKET("Bazaar & AH", "The market price of every item."),
        NPC_ITEMS("NPC for some items", "NPC prices for the pet item drops (Cretan Urn, Dwarf Turtle Shelmet, Antique Remedies, Washed Up Souvenir, Crochet Tiger Plushie) and the Hilt of Revelations, useful if you NPC sell them."),
        NPC_ONLY("NPC only (Ironman)", "Only NPC prices, items without one have no value."),
    }

    enum class TrackerFont(override val label: String) : Option {
        MINECRAFT("Minecraft"), INTER("Inter"),
    }

    // The trackers with a GuiLib look, for the look settings that apply to several of them
    enum class TrackerLook(override val label: String) : Option {
        LOOT("Loot"), MOBS("Mobs"), STATS("Stats"), COA("CoA"), CHAINS("Chains"), LOOTSHARE("Lootshare"),
    }

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
        option(TrackerMode.EVENT) {
            this.name = Literal("Loot Tracker")
            this.description = Literal(
                "Shows your Diana loot, /sboguis to move the overlay.\n" +
                    "§bNOTE!: You can interact with the tracker in the inventory!!!§r\n" +
                    "By clicking on a loot line you can hide/unhide it.\n" +
                    "Hovering over some lines may display additional information."
            )
        }
    ) { old, new ->
        if (old != new) {
            if (new != TrackerMode.OFF) {
                DianaLoot.updateLines()
            }
        }
    }

    var mobTracker by ObservableEntry(
        option(TrackerMode.OFF) {
            this.name = Literal("Mob Tracker")
            this.description = Literal(
                "Shows your Diana mob kills, /sboguis to move the overlay.\n" +
                "§bNOTE!: You can interact with the tracker in the inventory!!!§r\n" +
                "By clicking on a mob line you can hide/unhide it.\n" +
                "Hovering over some lines may display additional information."
            )
        }
    ) { old, new ->
        if (old != new) {
            if (new != TrackerMode.OFF) {
                DianaMobs.updateLines()
            }
        }
    }

    var coaTracker by ObservableEntry(
        option(TrackerMode.EVENT) {
            this.name = Literal("CoA Tracker")
            this.description = Literal(
                "Shows a tracker for the Crown of Avarice. \n" +
                    "/sboguis to move the overlay.\n"
            )
        }
    ) { old, new ->
        if (old != new) {
            if (new != TrackerMode.OFF) {
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
            dependsAny(notOff(::mobTracker), notOff(::lootTracker))
            this.description = Literal("Hides any loot or mob lines that have not been tracked yet (value is 0) to reduce clutter in the overlays.")
        }
    ) { old, new ->
        if (old != new) {
            if (lootTracker != TrackerMode.OFF) {
                DianaLoot.updateLines()
            }
            if (mobTracker != TrackerMode.OFF) {
                DianaMobs.updateLines()
            }
        }
    }

    var combineLootLines by ObservableEntry(
        boolean(false) {
            this.name = Literal("Combine LS Loot")
            depends(notOff(::lootTracker))
            this.description = Literal("Combines the base item and the Loot Share (LS) variant into a single line. The individual LS count is shown on hover.")
        }
    ) { old, new ->
        if (old != new) {
            if (lootTracker != TrackerMode.OFF) {
                DianaLoot.updateLines()
            }
        }
    }

    var hideLsStats by ObservableEntry(boolean(false) {
            this.name = Literal("Hide LS Stats")
            dependsAny(notOff(::mobTracker), on(::statsTracker))
            this.description = Literal("Hides the Loot Share (LS) portion of the Diana stats lines.")
        }
    ) { old, new ->
        if (old != new) {
            DianaStats.updateLines()
        }
    }

    var lootSort by ObservableEntry(
        option(LootSort.DEFAULT) {
            this.name = Literal("Loot Sort")
            depends(notOff(::lootTracker))
            this.description = Literal(
                "Order of the loot tracker lines: the default order, by price or by amount (highest or lowest first), or your own order.\n" +
                    "It can also be changed by clicking the sort badge of the tracker in the inventory. With Custom, the arrows next to each line move it up or down there (new look only)."
            )
        }
    ) { old, new ->
        if (old != new) {
            if (lootTracker != TrackerMode.OFF) {
                DianaLoot.updateLines()
            }
        }
    }

    var mobSort by ObservableEntry(
        option(LineSort.DEFAULT) {
            this.name = Literal("Mob Sort")
            depends(notOff(::mobTracker))
            this.description = Literal(
                "Order of the mob tracker lines: the default order, by kills (most or fewest first), or your own order.\n" +
                    "It can also be changed by clicking the sort badge of the tracker in the inventory. With Custom, the arrows next to each line move it up or down there (new look only)."
            )
        }
    ) { old, new ->
        if (old != new) {
            if (mobTracker != TrackerMode.OFF) {
                DianaMobs.updateLines()
            }
        }
    }

    var statsSort by ObservableEntry(
        option(LineSort.DEFAULT) {
            this.name = Literal("Stats Sort")
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

    var legacyTrackers by ObservableEntry(options<TrackerLook> {
            this.name = Literal("Tracker Legacy Look")
            this.description = Literal("The trackers shown as Minecraft text lines like the other overlays instead of the new box. Slightly better performance: plain text is the cheapest overlay to draw every frame.")
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    var trackerFont by ObservableEntry(option(TrackerFont.MINECRAFT) {
            this.name = Literal("Tracker Font")
            depends(state(::legacyTrackers, "not all legacy") { it.size < TrackerLook.entries.size })
            this.description = Literal("Font of the trackers with the new look. Minecraft has better performance (about 30% less work per frame than Inter), Inter looks smoother.")
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    var trackerBackground by ObservableEntry(options(*TrackerLook.entries.toTypedArray()) {
            this.name = Literal("Tracker Background")
            depends(state(::legacyTrackers, "not all legacy") { it.size < TrackerLook.entries.size })
            this.description = Literal("The trackers with the new look that get a dark box behind them. The others show only the text, with a shadow so it stays readable, and slightly better performance since the box isn't drawn.")
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    fun isLegacy(look: TrackerLook): Boolean = look in legacyTrackers

    fun hasBackground(look: TrackerLook): Boolean = look in trackerBackground

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

    var priceSource by option(PriceSource.MARKET) {
        this.name = Literal("Prices")
        this.description = Literal("Where the loot prices come from, hover an option for what it does.")
    }

    var bazaarSettingDiana by option(BazaarPrice.SELLOFFER) {
        this.name = Literal("Bazaar Setting")
        this.description = Literal("Bazaar setting to set the price for loot.")
    }

    var excludeCoinsFromProfit by boolean(false) {
        this.name = Literal("Exclude Coins from Profit")
        this.description = Literal("Excludes coins from the loot tracker's profit calculation when enabled, useful if using a non-1B crown since coins are consumed.")
    }

    val npcOnlyPrices: Boolean get() = priceSource == PriceSource.NPC_ONLY

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
