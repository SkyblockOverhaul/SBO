package net.sbo.mod.utils.data.configs.sbo

import com.google.gson.annotations.SerializedName
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.achievements.AchievementsView

data class SboData(
    var effects: List<Effect> = emptyList(),
    var resetVersion: String = "0.1.3",
    var changelogVersion: String = "1.0.0",
    // For What's New in the settings (SettingsChanges): the version run last, the one before it, and the version What's New was opened and announced for
    var settingsVersion: String = "",
    var settingsPreviousVersion: String = "",
    var whatsNewOpenedVersion: String = "",
    var whatsNewChatVersion: String = "",
    var downloadMsg: Boolean = false,
    /** Set once the Diana trackers were saved with a seal, from then on a missing seal means an edited file. */
    internal var trackersSealed: Boolean = false,
    var mobsSinceInq: Int = 0,
    var inqsSinceChim: Int = 0,
    var minotaursSinceStick: Int = 0,
    var champsSinceRelic: Int = 0,
    var inqsSinceLsChim: Int = 0,
    var highestChimMagicFind: Int = 0,
    var highestStickMagicFind: Int = 0,
    var highestFoodMagicFind: Int = 0,
    var highestLootingMagicFind: Int = 0,
    var highestWoolMagicFind: Int = 0,
    var highestCoreMagicFind: Int = 0,
    var highestStingerMagicFind: Int = 0,
    var highestRelicMagicFind: Int = 0,
    var hideTrackerLines: MutableList<String> = mutableListOf(),
    var collapsedOverlays: MutableSet<String> = mutableSetOf(), // overlay names collapsed with their eye, only in the inventory
    var trackerOrder: MutableMap<String, MutableList<String>> = mutableMapOf(), // "loot"/"mobs"/"stats" -> line keys for the "Custom" sort, missing lines go last
    var suppressedMessages: MutableSet<String> = mutableSetOf(),
    var partyBlacklist: List<String> = emptyList(),
    var achievementFilter: String = "Locked",
    // null until the achievements window saves it, then achievementFilter is no longer used
    var achievementsView: AchievementsView? = null,
    var lastKingDate: Long = 0,
    var lastMantiDate: Long = 0,
    var lastInqDate: Long = 0,
    var lastSphinxDate: Long = 0,
    var b2bStick: Boolean = false,
    var b2bChim: Boolean = false,
    var b2bChimLs: Boolean = false,
    var b2bInq: Boolean = false,
    var b2bChimLsInq: Boolean = false,
    var sboKey: String = "", // legacy, moved to ~/.sbo
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
    var sphinxSinceLooting: Int = 0,
    var b2bLooting: Boolean = false,
    var sphinxSinceLsLooting: Int = 0,
    var b2bLootingLs: Boolean = false,

    var lastStatsProfile: String = "",

    var cloudSync: MutableMap<String, CloudSyncState> = mutableMapOf(), // account uuid -> state
    var cloudSyncUiScale: Float? = null, // null = the global size, see UiScale
    var eventsUiScale: Float? = null, // null = the global size, see UiScale
    var eventsSort: String = "year",
    var eventsChartView: String = "value",
    var eventsHiddenLines: MutableList<String> = mutableListOf(),
    var pfOnboardingSeen: Boolean = false,
    var trackerLookAnswered: Boolean = false, // the new tracker look notice was answered with /sbolegacylook
    @SerializedName("customizerInTab", alternate = ["badgesInTab"]) var customizerInTab: Boolean = true,
    @SerializedName("customizerInChat", alternate = ["badgesInChat"]) var customizerInChat: Boolean = true,
    @SerializedName("customizerOwnInF5", alternate = ["badgeOwnInF5"]) var customizerOwnInF5: Boolean = true,
    /** Whose names inside chat messages look like the sender's: "off", "own" or "all", see [SboCustomizer.NAMES_OFF]. */
    @SerializedName("customizerNamesInChat", alternate = ["badgeNamesInChat"]) var customizerNamesInChat: String = "off",
    /** Names inside chat messages get the team tag too, not only their color. */
    @SerializedName("customizerNamesTag", alternate = ["badgeNamesTag"]) var customizerNamesTag: Boolean = true,
    /** The names [customizerNamesInChat] picks are colored in all drawn text: menus, titles, scoreboard, signs. */
    @SerializedName("customizerReplaceEverywhere", alternate = ["badgeReplaceEverywhere"]) var customizerReplaceEverywhere: Boolean = false,
    @SerializedName("customizerAnimationsOfOthers", alternate = ["badgeAnimationsOfOthers"]) var customizerAnimationsOfOthers: Boolean = true,
    /** Whether the letters of other players' customizers move above heads, apart from their colors. */
    @SerializedName("customizerMotionOfOthers", alternate = ["badgeMotionOfOthers"]) var customizerMotionOfOthers: Boolean = true,
    /** Off, every customizer in the tab list stands still, your own too. */
    @SerializedName("customizerAnimationsInTab", alternate = ["badgeAnimationsInTab"]) var customizerAnimationsInTab: Boolean = true,
    /** Whose customizer colors and motion move in chat: "off", "own" or "all", see [SboCustomizer.NAMES_OFF]. */
    @SerializedName("customizerAnimationsInChat", alternate = ["badgeAnimationsInChat"]) var customizerAnimationsInChat: String = "own",
    /** Symbols and text styles (bold, italic...) of other players' names. */
    @SerializedName("customizerLooksOfOthers", alternate = ["badgeLooksOfOthers"]) var customizerLooksOfOthers: Boolean = true,
    @SerializedName("customizerSizesOfOthers", alternate = ["badgeSizesOfOthers"]) var customizerSizesOfOthers: Boolean = true,
    /** Text size of the customizer line above heads, only on your screen. */
    @SerializedName("customizerLineSize", alternate = ["badgeLineSize"]) var customizerLineSize: Float = 1f,
    /** Text size of every player's name line above heads, only on your screen. */
    @SerializedName("customizerNameSize", alternate = ["badgeNameSize"]) var customizerNameSize: Float = 1f,
    /** Account uuid to whether it may use the customizer. */
    @SerializedName("customizerAccess", alternate = ["badgeAccess"]) var customizerAccess: MutableMap<String, Boolean> = mutableMapOf(),
    var collapsedSettings: MutableSet<String> = mutableSetOf(), // collapsed subs in the settings, "Diana/Waypoints"
    var currentCoaUuid: String = "",
    // Cloud Sync combines both sides on its own when they both changed. It is uploaded, so it is the same on every PC
    var cloudAutoMerge: Boolean = true,
) {
    fun save() = DataManager.save(DataManager::sboData)
}

data class CloudSyncState(
    var version: Int = 0, // 0 = never synced
    var counter: Long = 0,
    var hash: String = "", // last synced state
    var autoSync: Boolean? = null, // null = not set on this PC yet
    var premium: Boolean? = null, // null = never asked
)
