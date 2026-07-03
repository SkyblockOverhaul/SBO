package net.sbo.mod.utils.data

import com.google.gson.annotations.SerializedName
import net.sbo.mod.utils.game.Mayor

interface DianaTracker {
    var items: DianaItemsData
    var mobs: DianaMobsData

    fun reset(): DianaTracker {
        items = DianaItemsData()
        mobs = DianaMobsData()
        if (this is DianaTrackerMayorData) year = 0
        return this
    }

    fun save(): DianaTracker {
        when(this) {
            is DianaTrackerTotalData -> SboDataObject.save("DianaTrackerTotalData")
            is DianaTrackerSessionData -> SboDataObject.save("DianaTrackerSessionData")
            is DianaTrackerMayorData -> SboDataObject.save("DianaTrackerMayorData")
            else -> {}
        }
        return this
    }

    fun getAmountOf(itemId: String): Int {
        return when {
            items.COINS.toString() == itemId -> items.COINS.toInt()
            else -> {
                val itemField = DianaItemsData::class.members.find { it.name == itemId }
                itemField?.call(items) as? Int ?: 0
            }
        }
    }
}

data class SboConfigBundle(
    var sboData: SboData,
    var achievementsData: AchievementsData,
    var pastDianaEventsData: PastDianaEventsData,
    var dianaTrackerTotalData: DianaTrackerTotalData,
    var dianaTrackerSessionData: DianaTrackerSessionData,
    var dianaTrackerMayorData: DianaTrackerMayorData,
    var partyFinderConfigState: PartyFinderConfigState,
    var partyFinderData: PartyFinderData,
    var overlayData: OverlayData
)

// ------ Data Classes ------

data class Effect(
    var name: String,
    var duration: Double,
    var timeStamp: Long,
    var loggedOff: Boolean
)

// ------ Main Data Class ------

data class SboData(
    var effects: List<Effect> = emptyList(),
    var resetVersion: String = "0.1.3",
    var changelogVersion: String = "1.0.0",
    var downloadMsg: Boolean = false,
    var mobsSinceInq: Int = 0,
    var inqsSinceChim: Int = 0,
    var minotaursSinceStick: Int = 0,
    var champsSinceRelic: Int = 0,
    var inqsSinceLsChim: Int = 0,
    var highestChimMagicFind: Int = 0,
    var highestStickMagicFind: Int = 0,
    var highestFoodMagicFind: Int = 0,
    var highestWoolMagicFind: Int = 0,
    var highestCoreMagicFind: Int = 0,
    var highestStingerMagicFind: Int = 0,
    var highestRelicMagicFind: Int = 0,
    var hideTrackerLines: MutableList<String> = mutableListOf(),
    var partyBlacklist: List<String> = emptyList(),
    var achievementFilter: String = "Locked",
    var lastKingDate: Long = 0,
    var lastMantiDate: Long = 0,
    var lastInqDate: Long = 0,
    var lastSphinxDate: Long = 0,
    var b2bStick: Boolean = false,
    var b2bChim: Boolean = false,
    var b2bChimLs: Boolean = false,
    var b2bInq: Boolean = false,
    var b2bChimLsInq: Boolean = false,
    var sboKey: String = "",
    var b2bStreakCounter: MutableMap<String, Int> = mutableMapOf(),

    var mobsSinceKing: Int = 0,
    var b2bKing: Boolean = false,
    var kingSinceWool: Int = 0,
    var b2bWool: Boolean = false,
    var kingSinceLsWool: Int = 0,
    var b2bWoolLs: Boolean = false,

    var mobsSinceManti: Int = 0,
    var b2bManti: Boolean = false,
    var mantiSinceCore: Int = 0,
    var b2bCore: Boolean = false,
    var mantiSinceLsCore: Int = 0,
    var b2bCoreLs: Boolean = false,
    var mantiSinceStinger: Int = 0,
    var b2bStinger: Boolean = false,
    var mantiSinceLsStinger: Int = 0,
    var b2bStingerLs: Boolean = false,

    var mobsSinceSphinx: Int = 0,
    var b2bSphinx: Boolean = false,
    var sphinxSinceFood: Int = 0,
    var b2bFood: Boolean = false,
    var sphinxSinceLsFood: Int = 0,
    var b2bFoodLs: Boolean = false,
)

data class AchievementsData(
    var achievements: MutableMap<Int, Boolean>? = mutableMapOf(),
    var totalAchievements: MutableMap<Int, Int> = mutableMapOf(),
    var currentEventAchievements: MutableMap<Int, Boolean> = mutableMapOf(),
    var lastEventYear: Int = -1
)

data class PastDianaEventsData(
    var events: List<DianaTrackerMayorData> = emptyList()
)

data class DianaTrackerTotalData(
    override var items: DianaItemsData = DianaItemsData(),
    override var mobs: DianaMobsData = DianaMobsData(),
) : DianaTracker

data class DianaTrackerSessionData(
    override var items: DianaItemsData = DianaItemsData(),
    override var mobs: DianaMobsData = DianaMobsData(),
) : DianaTracker

data class DianaTrackerMayorData(
    var year: Int = Mayor.mayorElectedYear,
    override var items: DianaItemsData = DianaItemsData(),
    override var mobs: DianaMobsData = DianaMobsData(),
) : DianaTracker {
    fun snapshot(): DianaTrackerMayorData {
        return DianaTrackerMayorData(
            year = this.year,
            items = this.items.copy(),
            mobs = this.mobs.copy(),
        )
    }
}

data class PartyFinderConfigState(
    var checkboxes: Checkboxes = Checkboxes(),
    var inputs: Inputs = Inputs(),
    var filters: Filters = Filters()
)

data class PartyFinderData(
    var playerStatsUpdated: Long = 0,
    var playerStats: Map<String, PlayerStats> = emptyMap(),
)

// ------ Diana Data ------
@Suppress("PropertyName")
data class DianaItemsData(
    @SerializedName("coins") var COINS: Long = 0,
    @SerializedName("Griffin Feather") var GRIFFIN_FEATHER: Int = 0,
    @SerializedName("Mythos Fragment") var MYTHOS_FRAGMENT: Int = 0,
    @SerializedName("Crown of Greed") var CROWN_OF_GREED: Int = 0,
    @SerializedName("Washed-up Souvenir") var WASHED_UP_SOUVENIR: Int = 0,
    @SerializedName("Shimmering Wool") var SHIMMERING_WOOL: Int = 0,
    @SerializedName("Shimmering Wool Ls") var SHIMMERING_WOOL_LS: Int = 0,
    @SerializedName("Manti-core") var MANTI_CORE: Int = 0,
    @SerializedName("Manti-core Ls") var MANTI_CORE_LS: Int = 0,
    @SerializedName("Chimera") var CHIMERA: Int = 0,
    @SerializedName("ChimeraLs") var CHIMERA_LS: Int = 0,
    @SerializedName("Brain Food") var BRAIN_FOOD: Int = 0,
    @SerializedName("Brain Food LS") var BRAIN_FOOD_LS: Int = 0,
    @SerializedName("Fateful Stinger") var FATEFUL_STINGER: Int = 0,
    @SerializedName("Fateful Stinger Ls") var FATEFUL_STINGER_LS: Int = 0,
    @SerializedName("Braided Griffin Feather") var BRAIDED_GRIFFIN_FEATHER: Int = 0,
    @SerializedName("Daedalus Stick") var DAEDALUS_STICK: Int = 0,
    @SerializedName("CRETAN_URN") var CRETAN_URN: Int = 0,
    @SerializedName("DWARF_TURTLE_SHELMET") var DWARF_TURTLE_SHELMET: Int = 0,
    @SerializedName("ANTIQUE_REMEDIES") var ANTIQUE_REMEDIES: Int = 0,
    @SerializedName("CROCHET_TIGER_PLUSHIE") var CROCHET_TIGER_PLUSHIE: Int = 0,
    @SerializedName("ENCHANTED_ANCIENT_CLAW") var ENCHANTED_ANCIENT_CLAW: Int = 0,
    @SerializedName("ANCIENT_CLAW") var ANCIENT_CLAW: Int = 0,
    @SerializedName("MINOS_RELIC") var MINOS_RELIC: Int = 0,
    @SerializedName("ENCHANTED_GOLD") var ENCHANTED_GOLD: Int = 0,
    @SerializedName("Hilt of Revelations") var HILT_OF_REVELATIONS: Int = 0,
    @SerializedName("Total Burrows") var TOTAL_BURROWS: Int = 0,
    @SerializedName("scavengerCoins") var SCAVENGER_COINS: Long  = 0,
    @SerializedName("fishCoins") var FISH_COINS: Long  = 0,
    @SerializedName("time") var TIME: Long = 0,
    @SerializedName("KING_MINOS_SHARD") var KING_MINOS_SHARD: Int = 0,
    @SerializedName("SPHINX_SHARD") var SPHINX_SHARD: Int = 0,
    @SerializedName("MINOTAUR_SHARD") var MINOTAUR_SHARD: Int = 0,
    @SerializedName("CRETAN_BULL_SHARD") var CRETAN_BULL_SHARD: Int = 0,
    @SerializedName("HARPY_SHARD") var HARPY_SHARD: Int = 0,
    @SerializedName("MYTHOLOGICAL_DYE") var MYTHOLOGICAL_DYE: Int = 0,
    @SerializedName("MYTH_THE_FISH") var MYTH_THE_FISH: Int = 0,
)

@Suppress("PropertyName")
data class DianaMobsData(
    @SerializedName("King Minos") var KING_MINOS: Int = 0,
    @SerializedName("Manticore") var MANTICORE: Int = 0,
    @SerializedName("Minos Inquisitor") var MINOS_INQUISITOR: Int = 0,
    @SerializedName("Sphinx") var SPHINX: Int = 0,
    @SerializedName("Minos Champion") var MINOS_CHAMPION: Int = 0,
    @SerializedName("Minotaur") var MINOTAUR: Int = 0,
    @SerializedName("Gaia Construct") var GAIA_CONSTRUCT: Int = 0,
    @SerializedName("Harpy") var HARPY: Int = 0,
    @SerializedName("Cretan Bull") var CRETAN_BULL: Int = 0,
    @SerializedName("Stranded Nymph") var STRANDED_NYMPH: Int = 0,
    @SerializedName("Siamese Lynxes") var SIAMESE_LYNXES: Int = 0,
    @SerializedName("Minos Hunter") var MINOS_HUNTER: Int = 0,
    @SerializedName("TotalMobs") var TOTAL_MOBS: Int = 0,
    @SerializedName("Minos Inquisitor Ls") var MINOS_INQUISITOR_LS: Int = 0,
    @SerializedName("King Minos Ls") var KING_MINOS_LS: Int = 0,
    @SerializedName("Manticore Ls") var MANTICORE_LS: Int = 0,
    @SerializedName("Sphinx Ls") var SPHINX_LS: Int = 0,
)

// ------ Party Finder ------

data class Checkboxes(
    var custom: CustomCheckboxes = CustomCheckboxes(),
    var diana: DianaCheckboxes = DianaCheckboxes()
)

data class CustomCheckboxes(
    var eman9: Boolean = false
)

data class DianaCheckboxes(
    var eman9: Boolean = false,
    var looting5: Boolean = false
)

data class Inputs(
    var custom: CustomInputs = CustomInputs(),
    var diana: DianaInputs = DianaInputs()
)

data class CustomInputs(
    var lvl: Int = 0,
    var mp: Int = 0,
    var partySize: Int = 0,
    var note: String = "..."
)

data class DianaInputs(
    var kills: Int = 0,
    var lvl: Int = 0,
    var note: String = "..."
)

data class Filters(
    var custom: CustomFilters = CustomFilters(),
    var diana: DianaFilters = DianaFilters()
)

data class CustomFilters(
    var eman9Filter: Boolean = false,
    var noteFilter: String = ".",
    var canIjoinFilter: Boolean = false
)

data class DianaFilters(
    var eman9Filter: Boolean = false,
    var looting5Filter: Boolean = false,
    var canIjoinFilter: Boolean = false
)

data class PlayerStats(
    var name: String = "",
    var sbLvl: Int = 0,
    var eman9: Boolean = false,
    var looting5daxe: Boolean = false,
    var emanLvl: Int = 0,
    var warnings: List<String> = emptyList(),
    var uuid: String = "",
    var clover: Boolean = false,
    var daxeLootingLvl: Int = 0,
    var daxeChimLvl: Int = 0,
    var invApi: Boolean = false,
    var magicalPower: Int = 0,
    var enrichments: Int = 0,
    var missingEnrichments: Int = 0,
    var griffinRarity: String = "",
    var griffinItem: String? = null,
    var killLeaderboard: Int = 0,
    var mythosKills: Long = 0L
)

data class OverlayData(
    var overlays: MutableMap<String, OverlayValues> = mutableMapOf()
)

data class OverlayValues(
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 1.0f,
)