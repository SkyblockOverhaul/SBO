package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category
import net.sbo.mod.diana.LootshareDisplay
import net.sbo.mod.diana.RareMob
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import java.awt.Color

object RareMobs : Category("Rare Mobs") {
    override val description = "Inquisitor, Manticore, King Minos and Sphinx: sharing, highlights, lootshare, alerts and spawn messages."


    init {
        sub {
            this.title = "Sharing"
        }
    }

    var shareRareMob by boolean(true) {
        this.name = literal("Share Rare-Mob")
        this.description = literal("Sends the coordinates of rare mobs to your party or public chat. (King, Manti, Sphinx, Inq)")
    }

    var shareRareMobInPublicChat by boolean(false) {
        this.name = literal("Share Rare-Mob in Public Chat")
        depends(::shareRareMob)
        this.description = literal("Send rare mob coordinates to public chat instead of party chat.")
    }

    var ShareMobs by options(RareMob.INQ, RareMob.MANTICORE, RareMob.KING, RareMob.SPHINX) {
        this.name = literal("Mobs to Share")
        depends(::shareRareMob)
        this.description = literal("Select which mobs to share.")
    }

    var receiveRareMob by boolean(true) {
        this.name = literal("Receive Rare-Mob")
        this.description = literal("Create a waypoint when someone in your party shares a rare mob. (King, Manti, Sphinx, Inq)")
    }

    var ReceiveMobs by options(RareMob.INQ, RareMob.MANTICORE, RareMob.KING, RareMob.SPHINX) {
        this.name = literal("Mobs to Receive")
        depends(::receiveRareMob)
        this.description = literal("Select which mobs to receive.")
    }

    var scanWorldForRareMob by boolean(true) {
        this.name = literal("Scan World for Rare-Mob")
        this.description = literal("Create a waypoint when finding a rare mob entity in a loaded chunk automatically. (King, Manti, Sphinx, Inq)")
    }

    init {
        sub {
            this.title = "Highlight"
        }
    }

    var HighlightRareMobs by boolean(true) {
        this.name = literal("Highlight Rare Mobs")
        this.description = literal("Highlights rare mobs (King, Manti, Sphinx, Inq) with a glowing effect.")
    }

    var MinosInquisitorGlowColor by color(
        Color(1.0f, 0.35f, 0.75f).rgb
    ) {
        this.name = literal("Minos Inquisitor Glow Color")
        depends(::HighlightRareMobs)
        this.description = literal("Glow color for Minos Inquisitor.")
        this.allowAlpha = true
    }

    var ManticoreGlowColor by color(
        Color(0.0f, 0.45f, 0.0f).rgb
    ) {
        this.name = literal("Manticore Glow Color")
        depends(::HighlightRareMobs)
        this.description = literal("Glow color for Manticore.")
        this.allowAlpha = true
    }

    var KingMinosGlowColor by color(
        Color(1.0f, 0.55f, 0.0f).rgb
    ) {
        this.name = literal("King Minos Glow Color")
        depends(::HighlightRareMobs)
        this.description = literal("Glow color for King Minos.")
        this.allowAlpha = true
    }

    var SphinxGlowColor by color(
        Color(0.35f, 0.8f, 1.0f).rgb
    ) {
        this.name = literal("Sphinx Glow Color")
        depends(::HighlightRareMobs)
        this.description = literal("Glow color for Sphinx.")
        this.allowAlpha = true
    }

    init {
        sub {
            this.title = "Lootshare"
        }
    }

    var lootshareDisplay by boolean(true) {
        this.name = literal("Lootshare Display")
        this.description = literal("In a party, shows Lootshared above rare mobs you did not spawn. Grey and struck through until you hit 1% of its max HP, then aqua. Mobs you don't share or receive are skipped.")
    }

    var lootshareMessage by boolean(true) {
        this.name = literal("Lootshare Message")
        this.description = literal("Sends a party chat message once you dealt enough damage to lootshare a rare mob.")
    }

    var lootshareText by strings(LootshareDisplay.DEFAULT_TEXT) {
        this.name = literal("Lootshare Message Text")
        this.since = "0.7.2-beta.2"
        this.description = literal("The lootshare party message, \"${LootshareDisplay.DEFAULT_TEXT}\" when empty. Use {mob} for the mob's name. Colors can be used with & and a color code (stripped when sending to party). ${LootshareDisplay.MARKER} is added at the end, so SBO users in your party see you lootshared.")
        depends(::lootshareMessage)
    }

    var lootsharePartyOverlay by boolean(false) {
        this.name = literal("Lootshare Party Overlay")
        this.description = literal("While a rare mob is around, lists your party in red until they sent the SBO lootshare message, then aqua. /sboguis to move it.")
        depends(::lootshareDisplay, ::lootshareMessage)
    }

    init {
        sub {
            this.title = "Alerts"
        }
    }

    var hpAlert by double(0.0) {
        this.name = literal("HP Alert")
        this.description = literal("Sends a title alert when a Rare Mob is below the set HP value in Million. (0 to disable)")
    }

    var soundHpAlert by double(0.0) {
        this.name = literal("Sound HP Alert")
        this.description = literal("Sends the Low HP sound when the Rare Mob is below the set HP value in Million. (0 to disable)")
    }

    var noShurikenOverlay by boolean(true) {
        this.name = literal("No Shuriken Overlay")
        this.description = literal("Shows an overlay when the RareMob has no shuriken applied, /sboguis to move it.")
    }

    var NoShurikenMobs by options(RareMob.INQ, RareMob.MANTICORE, RareMob.KING, RareMob.SPHINX) {
        this.name = literal("Mobs to Check")
        depends(::noShurikenOverlay)
        this.description = literal("Select which mobs to check for shuriken.")
    }

    var mythosMobHp by boolean(true) {
        this.name = literal("Mythos Mob Infos")
        this.description = literal("Displays HP and your lootshare status of mythological mobs near you, /sboguis to move it.")
    }

    init {
        sub {
            this.title = "Spawn Title"
            this.description = "Fade-in, display and fade-out ticks of the title shown when a rare mob spawns. Enter 0 in all three to disable the titles."
        }
    }

    var rareMobTitleFadeInTime by int(0) {
        this.name = literal("Fade In Time")
        this.description = literal("Fade-in (ticks) for rare mob titles.")
        this.range = 0..100
        this.slider = true
    }

    var rareMobTitleStayTime by int(60) {
        this.name = literal("Stay Time")
        this.description = literal("Display time (ticks) for rare mob titles.")
        this.range = 0..100
        this.slider = true
    }

    var rareMobTitleFadeOutTime by int(0) {
        this.name = literal("Fade Out Time")
        this.description = literal("Fade-out (ticks) for rare mob titles.")
        this.range = 0..100
        this.slider = true
    }

    init {
        sub {
            this.title = "Spawn Messages"
            this.description = "Sent 5 seconds after the mob spawned. Use {since} for mobs since the mob and {chance} for the mob chance. Leave empty to disable."
        }
    }

    var announceInqText by strings("") {
        this.name = literal("Inquisitor")
        this.description = literal("Sends a text on Inq spawn.")
    }

    var announceMantiText by strings("") {
        this.name = literal("Manticore")
        this.description = literal("Sends a text on Manti spawn.")
    }

    var announceKingText by strings("") {
        this.name = literal("King Minos")
        this.description = literal("Sends a text on King spawn.")
    }

    var announceSphinxText by strings("") {
        this.name = literal("Sphinx")
        this.description = literal("Sends a text on Sphinx spawn.")
    }

    init {
        button {
            title = "Send All Test Messages"
            text = "Send Test"
            description = "Sends all test messages for the Rare Mob spawn message."
            onClick {
                Chat.chat("Inq Message: " + Helper.getSpawnMessage(announceInqText[0], "inq"))
                Chat.chat("Sphinx Message: " + Helper.getSpawnMessage(announceSphinxText[0], "sphinx"))
                Chat.chat("Manti Message: " + Helper.getSpawnMessage(announceMantiText[0], "manti"))
                Chat.chat("King Message: " + Helper.getSpawnMessage(announceKingText[0], "king"))
            }
        }
    }

    var sendKillTimeMessage by boolean(true) {
        this.name = literal("Mob Kill Time Message")
        this.description = literal("Sends a chat message with how long it took to kill a rare Diana mob you spawned.")
    }

    var announceCocoon by boolean(true) {
        this.name = literal("Send Message On Cocoon")
        this.description = literal("Sends a message to party chat on cocoon.")
    }

    var cocoonTitle by boolean(true) {
        this.name = literal("Show Title On Cocoon")
        this.description = literal("Shows a title on cocoon.")
    }

    init {
        sub {
            this.title = "Sphinx"
        }
    }

    var sphinxSolver by boolean(true) {
        this.name = literal("Sphinx Solver")
        this.description = literal(
            "Helps you solve the sphinx riddle by showing you the answer choices in chat and it automatically clicks the correct one for you when you click anywhere while the chat is open.\n" +
                "Theres also the option to us a keybind in the mc keybinds menu but §c⚠ USE AT YOUR OWN RISK ⚠"
        )
    }
}
