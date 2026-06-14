package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import com.teamresourceful.resourcefulconfigkt.api.ObservableEntry
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.overlays.DianaMobs
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.waypoint.AdditionalHubWarps
import java.awt.Color
import gg.essential.universal.UScreen
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.PastEventsGui

object Diana : CategoryKt("Diana") {
    enum class ShareList {
        INQ, MANTICORE, KING, SPHINX
    }

    enum class ReceiveList {
        INQ, MANTICORE, KING, SPHINX, OTHER
    }

    enum class NoShurikenList {
        INQ, MANTICORE, KING, SPHINX
    }

    enum class SettingDiana {
        INSTASELL, SELLOFFER;

        fun next(): SettingDiana {
            return if (this == INSTASELL) {
                SELLOFFER
            } else {
                INSTASELL
            }
        }
    }

    enum class Tracker {
        OFF, TOTAL, EVENT, SESSION;

        fun next(): Tracker {
            val values = entries
            val currentIndex = ordinal

            if (currentIndex == 0) return values[0 + 1]
            if (this == values.last()) return TOTAL
            return values[currentIndex + 1]
        }
    }

    init {
        separator {
            this.title = "Diana Burrows"
        }
    }

    var dianaBurrowGuess by boolean(true) {
        this.name = Literal("Spade Guess")
        this.description = Literal("Guess the burrow location when using spade ability. Needs Driping Lava Partciles and set /particlequality to Extreme for more accuracy")
    }

    var arrowGuess by boolean(true) {
        this.name = Literal("Arrow Guess")
        this.description = Literal("Guesses the burrow location from the arrow direction after digging a burrow \n" +
            "§bNOTE!: This replaces the old Multi guess system!\n" +
            "§cHave Dust Particles enabled!!!\n" +
            "§aDo every burrow you see for doing diana the fastest way!"
        )
    }

    var dianaBurrowDetect by boolean(true) {
        this.name = Literal("Close Burrow Detection")
        this.description = Literal("Detects Diana burrows when being close and holding shovel to show it as treasure or mob burrow. | to reset waypoints /sboclearburrows")
    }

    var showBeaconBeam by boolean(true) {
        this.name = Literal("Show Beacon Beam")
        this.description = Literal("Shows a beacon beam for waypoints going to the sky if enabled.")
    }

    var showTitleWhenInaccurate by boolean(false) {
        this.name = Literal("Show Title When Inaccurate")
        this.description = Literal("Shows a title to guess normally when the arrow guess is inaccurate")
    }

    var showTitleWhenChainEnd by boolean(false) {
        this.name = Literal("Show Title When Chain End")
        this.description = Literal("Shows a title to guess normally when the burrow chain is complete and there's no more guesses or burrows at least 90 blocks nearby")
    }

    var dontClearArrowGuess by boolean(true) {
        this.name = Literal("Don't Clear Arrow Guess on World Change")
        this.description = Literal("If enabled, the arrow guess waypoints will not be cleared when changing worlds/lobby")
    }

    init {
        separator {
            this.title = "Diana Warp"
            this.description = "You must configure the warp keys from vanilla Minecraft settings, under (ESC) -> Options -> Controls -> Key Binds... scroll till you find SBO - Keybinds and configure it from there."
        }
    }

    var allowedWarps by select(AdditionalHubWarps.WIZARD, AdditionalHubWarps.DA, AdditionalHubWarps.CASTLE) {
        this.name = Literal("Add Warps")
        this.description = Literal("Select the warps you want to be able to warp to with the guess and inquisitor warp keys.")
    }

    var showTitleWhenWarpAvailable by boolean(false) {
        this.name = Literal("Show Title When Warp Is Available")
        this.description = Literal("If enabled, will show a title when warp is available. Wait a few seconds to see if any close burrows appear, and if not, proceed to warp with the key, as the title updates live with new burrows discovered, it might sometimes only show for a split second when a new closer burrow is found making the warp unnecessary after title was shown.")
    }

    var dontWarpIfBurrowClose by boolean(true) {
        this.name = Literal("Don't Warp If a Burrow is nearby")
        this.description = Literal("If enabled, the warp key will not warp you if you are within 60 blocks of a burrow")
    }

    var warpDiff by int(10) {
        this.range = 0..60
        this.slider = true
        this.name = Literal("Warp Block Difference")
        this.description = Literal("The additional block difference to consider when warping to a waypoint. (0 to disable)")
    }

    var ignoreYLevel by boolean(false) {
        this.name = Literal("Ignore Y Level for Castle & Wizard")
        this.description = Literal("If enabled, ignores Y-Level when comparing distance between warp and the burrow when determining the closest warp for the Wizard and Castle warps, which have high Y-Level.")
    }

    var badWarpDistance by int(0) {
        this.range = 0..150
        this.name = Literal("Bad Warp Distance")
        this.description = Literal("Blocks at which good warps are prioritized over bad ones (Currently only Castle and Crypt). For example, when the Crypt warp is closest warp but Castle is second closest and the distance is less than this value, Castle will be preferred over Crypt warp for accessibility. (0 to disable)")
    }

    var warpDelay by int(0) {
        this.range = 0..1000
        this.slider = true
        this.name = Literal("Warp Delay (<X>ms)")
        this.description = Literal("The delay bevor you can warp after guessing with spade. (0 to disable)")
    }

    init {
        separator {
            this.title = "Diana Tracker"
        }
    }

    init {
        button {
            title = "Open Past Events"
            text = "Open Past Events"
            description = "Opens the Past Events menu, allowing you to see your saved trackers for previous Diana events. You can also open it by typing the /sbopastevents command."
            onClick {
                mc.schedule {
                    UScreen.displayScreen(PastEventsGui())
                }
            }
        }
    }

    var mobTracker by ObservableEntry(
        enum(Tracker.OFF) {
            this.name = Literal("Mob Tracker")
            this.description = Literal(
                "Shows your Diana mob kills, /sboguis to move the overlay\n" +
                "§bNOTE!: You can interact with the tracker in the inventory!!!§r\n" +
                "By clicking on a mob line you can hide/unhide it\n" +
                "Hovering over some lines may display additional information"
            )
        }
    ) { old, new ->
        if (old != new) {
            if (new != Tracker.OFF) {
                DianaMobs.updateLines()
            }
        }
    }

    var assumeAllLS by boolean(false) {
        this.name = Literal("Assume All LS")
        this.description = Literal("Assumes you get loot share on all Kings, Inquisitors and Manticores. This works around the LOOT SHARE! message not being always sent by the server, but might inflate your numbers if you don't actually do enough damage to these mobs for lootshare. To reduce false positives a bit (not fully!), you need to be 30 blocks nearby the rare mob when it died.")
    }

    var lootTracker by ObservableEntry(
        enum(Tracker.EVENT) {
            this.name = Literal("Loot Tracker")
            this.description = Literal(
                "Shows your Diana loot, /sboguis to move the overlay\n" +
                    "§bNOTE!: You can interact with the tracker in the inventory!!!§r\n" +
                    "By clicking on a loot line you can hide/unhide it\n" +
                    "Hovering over some lines may display additional information"
            )
        }
    ) { old, new ->
        if (old != new) {
            if (new != Tracker.OFF) {
                DianaLoot.updateLines()
            }
        }
    }

    var npcPriceOverrides by boolean(false) {
        this.name = Literal("NPC Price Overrides")
        this.description = Literal("Always prefers NPC prices for select items; such as the Pet Item drops like Cretan Urn, Dwarf Turtle Shelmet, Antique Remedies, Washed Up Souvenir and Crochet Tiger Plushie, along with Hilt of Revelations, useful if NPC selling them.")
    }

    var excludeCoinsFromProfit by boolean(false) {
        this.name = Literal("Exclude Coins from Profit")
        this.description = Literal("Excludes coins from the loot tracker's profit calculation when enabled, useful if using a non-1B crown since coins are consumed.")
    }

    var hideUnobtainedItems by ObservableEntry(
        boolean(true) {
            this.name = Literal("Hide Unobtained Items")
            this.description = Literal("Hides any loot or mob lines that have not been tracked yet (value is 0) to reduce clutter in the overlays.")
        }
    ) { old, new ->
        if (old != new) {
            if (lootTracker != Tracker.OFF) {
                DianaLoot.updateLines()
            }
            if (mobTracker != Tracker.OFF) {
                DianaMobs.updateLines()
            }
        }
    }

    var combineLootLines by ObservableEntry(
        boolean(false) {
            this.name = Literal("Combine LS Loot")
            this.description = Literal("Combines the base item and the Loot Share (LS) variant into a single line. The individual LS count is shown on hover.")
        }
    ) { old, new ->
        if (old != new) {
            if (lootTracker != Tracker.OFF) {
                DianaLoot.updateLines()
            }
        }
    }

    var afkTimeout by int(60) {
        this.name = Literal("AFK Timeout")
        this.description = Literal("Pause the trackers if it is not modified for this amount of seconds. Minimum 15, maximum 900 seconds is allowed.")
        this.range = 15..900
    }

    var statsTracker by boolean(false) {
        this.name = Literal("Diana Stats Tracker")
        this.description = Literal("Shows stats like Mobs since Inquisitor, Inquisitors since Chimera, /sboguis to move the overlay")
    }

    var resetSessionOnGameRestart by boolean(false) {
        this.name = Literal("Reset Session on Game Restart")
        this.description = Literal("Resets the Diana session tracker when you restart Minecraft")
    }

    var magicFindTracker by boolean(false) {
        this.name = Literal("Magic Find Tracker")
        this.description = Literal("Shows your highest magic find for sticks and chimeras (only after you dropped it once), /sboguis to move the overlay")
    }

    var fourEyedFish by boolean(false) {
        this.name = Literal("Four-Eyed Fish")
        this.description = Literal("Set if you have a Four-Eyed Fish on your griffin pet")
    }

    var sendSinceMessage by boolean(true) {
        this.name = Literal("Stats Message")
        this.description = Literal("Sends the chat Message with stat: [SBO] Took 120 Mobs to get a Inquis!")
    }

    var bazaarSettingDiana by enum(SettingDiana.SELLOFFER) {
        this.name = Literal("Bazaar Setting")
        this.description = Literal("Bazaar setting to set the price for loot")
    }

    init {
        separator {
            this.title = "Diana Announcer"
        }
    }

    var lootAnnouncerChat by boolean(true) {
        this.name = Literal("Rare Drop Announcer")
        this.description = Literal("Announces relic/shelmet/plushie/remedies in chat")
    }

    var lootAnnouncerScreen by boolean(true) {
        this.name = Literal("Loot Screen Announcer")
        this.description = Literal("Announces chimera/stick/relic on screen")
    }

    var lootAnnouncerPrice by ObservableEntry(boolean(true) {
            this.name = Literal("Show Price Title")
            this.description = Literal("Shows chimera/stick/relic price as a subtitle on screen")
        }
    ) { old, new ->
        if (old != new) {
            if (new) {
                lootAnnouncerScreen = true
            }
        }
    }

    init {
        separator {
            this.title = "Title Timings"
            this.description = "Customize fade-in, display and fade-out ticks for titles"
        }
    }

    var rareTitleFadeIn by int(0) {
        this.name = Literal("Rare Title Fade In")
        this.description = Literal("Fade-in (ticks) for rare mob titles")
        this.range = 0..100
        this.slider = true
    }

    var rareTitleTime by int(90) {
        this.name = Literal("Rare Title Time")
        this.description = Literal("Display time (ticks) for rare mob titles")
        this.range = 0..100
        this.slider = true
    }

    var rareTitleFadeOut by int(20) {
        this.name = Literal("Rare Title Fade Out")
        this.description = Literal("Fade-out (ticks) for rare mob titles")
        this.range = 0..100
        this.slider = true
    }

    var lootAnnouncerParty by boolean(true) {
        this.name = Literal("Loot Party Announcer")
        this.description = Literal("Announces chimera/wool/stinger/food in party chat")
    }

    var chimMessageBool by boolean(false) {
        this.name = Literal("Chim Message")
        this.description = Literal("Enables custom chim message")
    }

    var customChimMessage by strings("&6[SBO] &6&lRARE DROP! &dChimera! &b+{mf} ✯ Magic Find &b#{amount}") {
        this.name = Literal("Custom Chim Message Text")
        this.description = Literal("use: {mf} for MagicFind, {amount} for drop Amount this event and {percentage} for chimera/inquis ratio.")
    }

    var coreMessageBool by boolean(false) {
        this.name = Literal("Manti-Core Message")
        this.description = Literal("Enables custom Manti-Core message (core/manti)")
    }

    var customCoreMessage by strings("&6[SBO] &6&lRARE DROP! &6Manti-Core! &b+{mf} ✯ Magic Find &b#{amount}") {
        this.name = Literal("Custom Manti-Core Message Text")
        this.description = Literal("Use: {mf} for MagicFind, {amount} for drop amount this event and {percentage} for core/manti ratio.")
    }

    var stingerMessageBool by boolean(false) {
        this.name = Literal("Fateful Stinger Message")
        this.description = Literal("Enables custom Fateful Stinger message (stinger/manti)")
    }

    var customStingerMessage by strings("&6[SBO] &6&lRARE DROP! &dFateful Stinger! &b+{mf} ✯ Magic Find &b#{amount}") {
        this.name = Literal("Custom Fateful Stinger Message Text")
        this.description = Literal("Use: {mf} for MagicFind, {amount} for drop amount this event and {percentage} for stinger/manti ratio.")
    }

    var bfMessageBool by boolean(false) {
        this.name = Literal("Brain Food Message")
        this.description = Literal("Enables custom Brain Food message (food/sphinx)")
    }

    var customBfMessage by strings("&6[SBO] &6&lRARE DROP! &5Brain Food! &b+{mf} ✯ Magic Find &b#{amount}") {
        this.name = Literal("Custom Brain Food Message Text")
        this.description = Literal("Use: {mf} for MagicFind, {amount} for drop amount this event and {percentage} for food/sphinx ratio.")
    }

    var woolMessageBool by boolean(false) {
        this.name = Literal("Shimmering Wool Message")
        this.description = Literal("Enables custom Shimmering Wool message (wool/king)")
    }

    var customWoolMessage by strings("&6[SBO] &6&lRARE DROP! &6Shimmering Wool! &b+{mf} ✯ Magic Find &b#{amount}") {
        this.name = Literal("Custom Shimmering Wool Message Text")
        this.description = Literal("Use: {mf} for MagicFind, {amount} for drop amount this event and {percentage} for wool/king ratio.")
    }

    init {
        button {
            title = "Send All Test Messages"
            text = "Send Test"
            description = "Sends a test message for all rare drop messages"
            onClick {
                if (Helper.checkCustomDropMessage("Chimera", 400).first) {
                    val drops = mutableListOf<String>()
                    if (chimMessageBool) drops.add("Chimera")
                    if (bfMessageBool) drops.add("Brain Food")
                    if (woolMessageBool) drops.add("Wool")
                    if (coreMessageBool) drops.add("Core")
                    if (stingerMessageBool) drops.add("Stinger")

                    for (drop: String in drops) {
                        Chat.chat(
                            Helper.checkCustomDropMessage(drop, 400).second
                        )
                    }
                }
            }
        }
    }


    init {
        separator {
            this.title = "Diana Waypoints"
            this.description =  "§bDisable View Bobbing in controls if lines are buggy"
        }
    }

    var guessLine by boolean(true) {
        this.name = Literal("Guess Line")
        this.description = Literal("Draws line for guess, Disable View Bobbing in controls if its buggy")
    }

    var inqLine by boolean(true) {
        this.name = Literal("Rare Mob Line")
        this.description = Literal("Draws line to rare mob waypoints, Disable View Bobbing in controls if its buggy")
    }

    var burrowLine by boolean(true) {
        this.name = Literal("Burrow Line")
        this.description = Literal("Draws line for burrow, Disable View Bobbing in controls if its buggy")
    }

    var dianaLineWidth by int(5) {
        this.range = 1..20
        this.slider = true
        this.name = Literal("Diana Line Width")
        this.description = Literal("The width of the lines drawn for Diana waypoints")
    }

    var removeGuessDistance by int(3) {
        this.range = 0..20
        this.slider = true
        this.name = Literal("Remove Guess When Close")
        this.description = Literal("Removes the guess waypoint when you are within this distance of it (0 to disable)")
    }

    var removeRareMobwaypoint by boolean(true) {
        this.name = Literal("Remove Rare Mob Waypoint when near")
        this.description = Literal("Removes the rare mob waypoint when you are within 3 blocks of it")
    }

    var removeBeam by int(8) {
        this.range = 0..20
        this.slider = true
        this.name = Literal("Remove Rare Mob Beam Distance")
        this.description = Literal("Removes the rare mob waypoint beam when you are within this distance of it (0 to disable)")
    }

    init {
        separator {
            this.title = "Rare Mobs"
        }
    }

    var shareRareMob by boolean(true) {
        this.name = Literal("Share Rare-Mob")
        this.description = Literal("Sends the coordinates of rare mobs(King, Manti, Sphinx, Inq)to your party")
    }

    var ShareMobs by select(ShareList.INQ, ShareList.MANTICORE, ShareList.KING, ShareList.SPHINX) {
        this.name = Literal("Select which Mobs to Share")
        this.description = Literal("Select which mobs to share")
    }

    var receiveRareMob by boolean(true) {
        this.name = Literal("Receive Rare-Mob")
        this.description = Literal("Create a waypoint when someone in your party shares a rare mob(King, Manti, Sphinx, Inq)")
    }

    var ReceiveMobs by select(ReceiveList.INQ, ReceiveList.MANTICORE, ReceiveList.KING, ReceiveList.SPHINX, ReceiveList.OTHER) {
        this.name = Literal("Which Mobs to Receive")
        this.description = Literal(
        "Select which mobs to receive\n" +
            "§bOTHER = Rare mobs from players that dont ping with sbo (mainly skyhanni)"
        )
    }

    var HighlightRareMobs by boolean(true) {
        this.name = Literal("Highlight Rare Mobs")
        this.description = Literal("Highlights rare mobs(King, Manti, Sphinx, Inq) with a glowing effect")
    }

    var HighlightColor by color(
        Color(0.0f, 0.964f, 1.0f).rgb) {
        this.name = Literal("Highlight Color")
        this.description = Literal("Color for the rare mob highlight effect")
        this.allowAlpha = true
    }

    var allWaypointsAreInqs by boolean(false) {
        this.name = Literal("All Waypoints are Rare Mobs")
        this.description = Literal("All coordinates from chat are considered rare mobs(King, Manti, Sphinx, Inq) only works in hub during diana")
    }

    var announceInqText by strings("") {
        this.name = Literal("Send Text On Inq Spawn")
        this.description = Literal("Sends a text on Inq spawn 5 seconds after spawn, use {since} for mobs since mob, {chance} for mob chance")
    }

    var announceMantiText by strings("") {
        this.name = Literal("Send Text On Manti Spawn")
        this.description = Literal("Sends a text on Manti spawn 5 seconds after spawn, use {since} for mobs since mob, {chance} for mob chance")
    }

    var announceSphinxText by strings("") {
        this.name = Literal("Send Text On Sphinx Spawn")
        this.description = Literal("Sends a text on Sphinx spawn 5 seconds after spawn, use {since} for mobs since mob, {chance} for mob chance")
    }

    var announceKingText by strings("") {
        this.name = Literal("Send Text On King Spawn")
        this.description = Literal("Sends a text on King spawn 5 seconds after spawn, use {since} for mobs since mob, {chance} for mob chance")
    }

    var announceCocoon by boolean(true) {
        this.name = Literal("Send Text On Cocoon")
        this.description = Literal("Sends a text on cocoon")
    }

    var cocoonTitle by boolean(true) {
        this.name = Literal("Show Title On Cocoon")
        this.description = Literal("Shows a title on cocoon")
    }

    var legacyCocoonDetection by boolean(false) {
        this.name = Literal("Legacy Cocoon Detection")
        this.description = Literal("Uses egg sac player head texture to detect cocoon instead of the new cocoon chat message when enabled. Only enable if chat detection does not work.")
    }

    var hpAlert by double(0.0) {
        this.name = Literal("HP Alert")
        this.description = Literal("Sends a title alert when a Rare Mob is below the set HP value in Million (0 to disable)")
    }

    var noShurikenOverlay by boolean(true) {
        this.name = Literal("No Shuriken Overlay")
        this.description = Literal("Shows an overlay when the RareMob has no shuriken applied /sboguis to move it")
    }

    var NoShurikenMobs by select(NoShurikenList.INQ, NoShurikenList.MANTICORE, NoShurikenList.KING, NoShurikenList.SPHINX) {
        this.name = Literal("Select which Mobs to Check")
        this.description = Literal("Select which mobs to check for shuriken")
    }

    init {
        button {
            title = "Send All Test Messages"
            text = "Send Test"
            description = "Sends all test messages for the Rare Mob spawn message"
            onClick {
                Chat.chat("Inq Message: " + Helper.getSpawnMessage(announceInqText[0], "inq"))
                Chat.chat("Sphinx Message: " + Helper.getSpawnMessage(announceSphinxText[0], "sphinx"))
                Chat.chat("Manti Message: " + Helper.getSpawnMessage(announceMantiText[0], "manti"))
                Chat.chat("King Message: " + Helper.getSpawnMessage(announceKingText[0], "king"))
            }
        }
    }

    init {
        separator {
            this.title = "Other"
        }
    }

    var mythosMobHp by boolean(true) {
        this.name = Literal("Mythos Mob HP")
        this.description = Literal("Displays HP of mythological mobs near you. /sboguis to move it")
    }

    var sphinxSolver by boolean(true) {
        this.name = Literal("Sphinx Solver")
        this.description = Literal("Helps you solve the sphinx riddle by showing you the answer choices in chat and it automatically clicks the correct one for you when you click anywehre while the chat is open.")
    }
}
