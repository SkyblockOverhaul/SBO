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

    // The box of tracker.css and coa.css, rgba(14, 14, 16, 0.78)
    private const val DEFAULT_BACKGROUND_OPACITY = 78

    // The trackers with a compact look, in both looks
    enum class CompactTracker(override val label: String) : Option {
        LOOT("Loot"), MOBS("Mobs"), STATS("Stats"),
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
            this.name = literal("Loot Tracker")
            this.description = literal(
                """
                Shows your Diana loot, /sboguis to move the overlay.
                §bNOTE!: You can interact with the tracker in the inventory!!!§r
                By clicking on a loot line you can hide/unhide it.
                Hovering over some lines may display additional information.
                """.trimIndent()
            )
        }
    ) { old, new ->
        if (old != new && new != TrackerMode.OFF) {
            DianaLoot.updateLines()
        }
    }

    var mobTracker by ObservableEntry(
        option(TrackerMode.OFF) {
            this.name = literal("Mob Tracker")
            this.description = literal(
                """
                Shows your Diana mob kills, /sboguis to move the overlay.
                §bNOTE!: You can interact with the tracker in the inventory!!!§r
                By clicking on a mob line you can hide/unhide it.
                Hovering over some lines may display additional information.
                """.trimIndent()
            )
        }
    ) { old, new ->
        if (old != new && new != TrackerMode.OFF) {
            DianaMobs.updateLines()
        }
    }

    var coaTracker by ObservableEntry(
        option(TrackerMode.EVENT) {
            this.name = literal("CoA Tracker")
            this.description = literal(
                "Shows a tracker for the Crown of Avarice. \n" +
                    "/sboguis to move the overlay.\n"
            )
        }
    ) { old, new ->
        if (old != new && new != TrackerMode.OFF) {
            CrownOfAvarice.updateLines()
        }
    }

    var statsTracker by boolean(false) {
        this.name = literal("Diana Stats Tracker")
        this.description = literal("Shows stats like Mobs since Inquisitor, Inquisitors since Chimera, /sboguis to move the overlay.")
    }

    var magicFindTracker by boolean(false) {
        this.name = literal("Magic Find Tracker")
        this.description = literal("Shows your highest magic find for sticks and chimeras (only after you dropped it once), /sboguis to move the overlay.")
    }

    init {
        sub {
            this.title = "Lines"
        }
    }

    var hideUnobtainedItems by ObservableEntry(
        boolean(true) {
            this.name = literal("Hide Unobtained Items")
            dependsAny(notOff(::mobTracker), notOff(::lootTracker))
            this.description = literal("Hides any loot or mob lines that have not been tracked yet (value is 0) to reduce clutter in the overlays.")
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
            this.name = literal("Combine LS Loot")
            depends(notOff(::lootTracker))
            this.description = literal("Combines the base item and the Loot Share (LS) variant into a single line. The individual LS count is shown on hover.")
        }
    ) { old, new ->
        if (old != new && lootTracker != TrackerMode.OFF) {
            DianaLoot.updateLines()
        }
    }

    var hideLsStats by ObservableEntry(boolean(false) {
            this.name = literal("Hide LS Stats")
            dependsAny(notOff(::mobTracker), on(::statsTracker))
            this.description = literal("Hides the Loot Share (LS) portion of the Diana stats lines.")
        }
    ) { old, new ->
        if (old != new) {
            DianaStats.updateLines()
        }
    }

    var lootSort by ObservableEntry(
        option(LootSort.DEFAULT) {
            this.name = literal("Loot Sort")
            this.since = "0.7.2-beta.2"
            depends(notOff(::lootTracker))
            this.description = literal(
                "Order of the loot tracker lines: the default order, by price or by amount (highest or lowest first), or your own order.\n" +
                    "It can also be changed by clicking the sort badge of the tracker in the inventory. With Custom, the arrows next to each line move it up or down there (new look only)."
            )
        }
    ) { old, new ->
        if (old != new && lootTracker != TrackerMode.OFF) {
            DianaLoot.updateLines()
        }
    }

    var mobSort by ObservableEntry(
        option(LineSort.DEFAULT) {
            this.name = literal("Mob Sort")
            this.since = "0.7.2-beta.2"
            depends(notOff(::mobTracker))
            this.description = literal(
                "Order of the mob tracker lines: the default order, by kills (most or fewest first), or your own order.\n" +
                    "It can also be changed by clicking the sort badge of the tracker in the inventory. With Custom, the arrows next to each line move it up or down there (new look only)."
            )
        }
    ) { old, new ->
        if (old != new && mobTracker != TrackerMode.OFF) {
            DianaMobs.updateLines()
        }
    }

    var statsSort by ObservableEntry(
        option(LineSort.DEFAULT) {
            this.name = literal("Stats Sort")
            this.since = "0.7.2-beta.2"
            depends(on(::statsTracker))
            this.description = literal(
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
            this.name = literal("Tracker Legacy Look")
            this.since = "0.7.2-beta.2"
            this.description = literal("The trackers shown as Minecraft text lines like the other overlays instead of the new box. Slightly better performance: plain text is the cheapest overlay to draw every frame.")
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    var compactTrackers by ObservableEntry(options<CompactTracker> {
            this.name = Literal("Tracker Compact Look")
            this.since = "0.7.2-beta.3"
            this.description = Literal(
                "The trackers that take as little space as possible: a few lines with short names and several entries per line, only the rare mobs and the main loot, no prices or percentages. " +
                    "Works with the legacy and the new look. In the inventory the full tracker is shown, to hide lines and change the view or sort.\n" +
                    "Lootshare counts are added after a +, in the stats after a /."
            )
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    var trackerFont by ObservableEntry(option(TrackerFont.MINECRAFT) {
            this.name = literal("Tracker Font")
            this.since = "0.7.2-beta.2"
            depends(state(::legacyTrackers, "not all legacy") { it.size < TrackerLook.entries.size })
            this.description = literal("Font of the trackers with the new look. Minecraft has better performance (about 30% less work per frame than Inter), Inter looks smoother.")
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    private val trackerBackground by ObservableEntry(options(TrackerLook.entries) {
            this.name = literal("Tracker Background")
            this.since = "0.7.2-beta.2"
            depends(state(::legacyTrackers, "not all legacy") { it.size < TrackerLook.entries.size })
            this.description = literal("The trackers with the new look that get a dark box behind them. The others show only the text, with a shadow so it stays readable, and slightly better performance since the box isn't drawn.")
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    var trackerBackgroundOpacity by ObservableEntry(int(DEFAULT_BACKGROUND_OPACITY) {
            this.name = Literal("Tracker Background Opacity")
            this.since = "0.7.2-beta.3"
            this.range = 10..100
            depends(state(::trackerBackground, "a tracker with background") { it.isNotEmpty() })
            this.description = Literal("How dark the box behind the trackers is, in percent ($DEFAULT_BACKGROUND_OPACITY is the normal look). Lower lets more of the game through, about 30% still keeps the text readable.")
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    var trackerOutline by ObservableEntry(options<TrackerLook> {
            this.name = Literal("Tracker Text Outline")
            this.since = "0.7.2-beta.3"
            depends(state(::legacyTrackers, "not all legacy") { it.size < TrackerLook.entries.size })
            this.description = Literal("The trackers with the new look whose text gets a black outline instead of the shadow, readable on any ground even without a box. Slightly more work per frame: the text is drawn five times.")
        }
    ) { old, new ->
        if (old != new) onTrackerLookChanged()
    }

    fun isLegacy(look: TrackerLook): Boolean = look in legacyTrackers

    fun hasBackground(look: TrackerLook): Boolean = look in trackerBackground

    /** The classes of [look]'s box for the look settings: font, background and outline. */
    fun lookClasses(look: TrackerLook): String = buildString {
        if (trackerFont == TrackerFont.INTER) append(" inter")
        if (!hasBackground(look)) append(" no-bg")
        if (look in trackerOutline) append(" outline")
    }

    /** The inline style of [look]'s box for "Tracker Background Opacity", null for the stylesheet's own. */
    fun boxStyle(look: TrackerLook): String? =
        if (hasBackground(look) && trackerBackgroundOpacity != DEFAULT_BACKGROUND_OPACITY) "background-color: rgba(14, 14, 16, ${trackerBackgroundOpacity / 100f})"
        else null

    fun isCompact(tracker: CompactTracker): Boolean = tracker in compactTrackers

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
        this.name = literal("Prices")
        this.since = "0.7.2-beta.2"
        this.description = literal("Where the loot prices come from, hover an option for what it does.")
    }

    var bazaarSettingDiana by option(BazaarPrice.SELLOFFER) {
        this.name = literal("Bazaar Setting")
        this.description = literal("Bazaar setting to set the price for loot.")
    }

    var excludeCoinsFromProfit by boolean(false) {
        this.name = literal("Exclude Coins from Profit")
        this.description = literal("Excludes coins from the loot tracker's profit calculation when enabled, useful if using a non-1B crown since coins are consumed.")
    }

    val npcOnlyPrices: Boolean get() = priceSource == PriceSource.NPC_ONLY

    init {
        sub {
            this.title = "Tracking"
        }
    }

    var afkTimeout by int(60) {
        this.name = literal("AFK Timeout")
        this.description = literal("Pause the trackers if it is not modified for this amount of seconds. Minimum 15, maximum 900 seconds is allowed.")
        this.range = 15..900
    }

    var assumeAllLS by boolean(false) {
        this.name = literal("Assume All LS")
        this.description = literal("Assumes you get loot share on all Kings, Inquisitors and Manticores. This works around the LOOT SHARE! message not being always sent by the server, but might inflate your numbers if you don't actually do enough damage to these mobs for lootshare. To reduce false positives a bit (not fully!), you need to be 32 blocks nearby the rare mob when it died.")
    }

    var fourEyedFish by boolean(false) {
        this.name = literal("Four-Eyed Fish")
        this.description = literal("Set if you have a Four-Eyed Fish on your griffin pet.")
    }

    var resetSessionOnGameRestart by boolean(false) {
        this.name = literal("Reset Session on Game Restart")
        this.description = literal("Resets the Diana session tracker when you restart Minecraft.")
    }
}
