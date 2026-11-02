package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category
import net.sbo.mod.config.Option
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat

object Drops : Category("Drops") {
    override val description = "What happens when you drop something: chat and screen announcements, the drop messages and Medal clips."

    enum class MedalDrop(override val label: String) : Option {
        CHIMERA("Chimera"),
        SHIMMERING_WOOL("Shimmering Wool"),
        BRAIN_FOOD("Brain Food"),
        MINOS_RELIC("Minos Relic"),
        MANTI_CORE("Manti-core"),
        FATEFUL_STINGER("Fateful Stinger"),
        DAEDALUS_STICK("Daedalus Stick"),
        BRAIDED_GRIFFIN_FEATHER("Braided Griffin Feather"),
        MYTHOLOGICAL_DYE("Mythological Dye"),
        MYTH_THE_FISH("Myth the Fish"),
        CROWN_OF_GREED("Crown of Greed"),
        HILT_OF_REVELATIONS("Hilt of Revelations"),
        WASHED_UP_SOUVENIR("Washed-up Souvenir"),
        DWARF_TURTLE_SHELMET("Dwarf Turtle Shelmet"),
        CROCHET_TIGER_PLUSHIE("Crochet Tiger Plushie"),
        ANTIQUE_REMEDIES("Antique Remedies"),
        CRETAN_URN("Cretan Urn"),
    }

    init {
        sub {
            this.title = "Announcer"
        }
    }

    var lootAnnouncerChat by boolean(true) {
        this.name = literal("Rare Drop Announcer")
        this.description = literal("Announces relic/shelmet/plushie/remedies in chat.")
    }

    var hiltDropMessage by boolean(true) {
        this.name = literal("Hilt of Revelations Drop Message")
        this.description = literal("Sends rare drop message for Hilt of Revelations, since Hypixel does not send one.")
    }

    var lootAnnouncerScreen by boolean(true) {
        this.name = literal("Loot Screen Announcer")
        this.description = literal("Announces chimera/stick/relic on screen.")
    }

    var announceCrownOfGreed by boolean(true) {
        this.name = literal("Crown Of Greed Title")
        this.description = literal("Whether you want \"§6Crown Of Greed§r\" to appear on your screen when dropping one")
    }

    var lootAnnouncerParty by boolean(true) {
        this.name = literal("Loot Party Announcer")
        this.description = literal("Announces chimera/wool/stinger/food in party chat.")
    }

    var sendSinceMessage by boolean(true) {
        this.name = literal("Stats Message")
        this.description = literal("Sends the chat Message with stat: [SBO] Took 120 Mobs to get a Minos Inquisitor!")
    }

    init {
        sub {
            this.title = "Custom Drop Messages"
            this.description = "Leave empty to use the default. Use {mf} for Magic Find, {amount} for the drop amount this event, {since} for the mobs it took, {price} for the price, {percentage} for the drop/mob ratio and {lstext} for (LS) on lootshare drops. Colors can be used with & and a color code (stripped when announcing to party)."
        }
    }

    var customChimeraMessage by strings("") {
        this.name = literal("Chimera")
        this.description = literal("{since}: inquisitor it took to drop the chim.")
    }

    var customManticoreMessage by strings("") {
        this.name = literal("Manti-core")
        this.description = literal("{since}: manti it took to drop the core.")
    }

    var customFatefulStingerMessage by strings("") {
        this.name = literal("Fateful Stinger")
        this.description = literal("{since}: manti it took to drop the stinger.")
    }

    var customBrainFoodMessage by strings("") {
        this.name = literal("Brain Food")
        this.description = literal("{since}: sphinx it took to drop the food.")
    }

    var customShimmeringWoolMessage by strings("") {
        this.name = literal("Shimmering Wool")
        this.description = literal("{since}: kings it took to drop the wool.")
    }

    init {
        button {
            title = "Send All Test Messages"
            text = "Send Test"
            description = "Sends a test message for all rare drop messages."
            onClick {
                if (Helper.checkCustomDropMessage("Chimera", 400, false).first) {
                    val drops = mutableListOf<String>()
                    if (customChimeraMessage.isNotEmpty()) drops.add("Chimera")
                    if (customBrainFoodMessage.isNotEmpty()) drops.add("Brain Food")
                    if (customShimmeringWoolMessage.isNotEmpty()) drops.add("Wool")
                    if (customManticoreMessage.isNotEmpty()) drops.add("Core")
                    if (customFatefulStingerMessage.isNotEmpty()) drops.add("Stinger")

                    for (drop: String in drops) {
                        Chat.chat(
                            Helper.checkCustomDropMessage(drop, 400, false).second
                        )
                    }
                }
            }
        }
    }

    init {
        sub {
            this.title = "Medal Clips"
            this.description = "Saves Medal clips on Diana drops. Requires Medal to be running locally."
        }
    }

    var medalEnabled by boolean(false) {
        this.name = literal("Enable Medal Clips")
        this.description = literal("Saves Medal clips on selected Diana drops")
    }

    // Was one Clip ... switch per drop
    var medalClips by options(MedalDrop.entries) {
        this.name = literal("Clip Drops")
        this.since = "0.7.2-beta.2"
        depends(::medalEnabled)
        this.description = literal("The drops that save a Medal clip.")
    }

    var medalClipDurationSeconds by int(30) {
        this.range = 1..120
        this.slider = true
        this.name = literal("Clip Duration (seconds)")
        depends(::medalEnabled)
        this.description = literal("How long Medal clips should be")
    }

    var medalCaptureDelayMs by int(10000) {
        this.range = 0..60000
        this.slider = true
        this.name = literal("Capture Delay (ms)")
        depends(::medalEnabled)
        this.description = literal("Delay before Medal captures clips")
    }
}
