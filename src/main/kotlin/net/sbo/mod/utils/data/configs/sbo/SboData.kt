package net.sbo.mod.utils.data.configs.sbo

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

    var lastStatsProfile: String = "",

    var cloudSync: MutableMap<String, CloudSyncState> = mutableMapOf(), // account uuid -> state
    var cloudSyncUiScale: Float? = null, // null = the global size, see UiScale
    var eventsUiScale: Float? = null, // null = the global size, see UiScale
    var eventsSort: String = "year",
    var eventsChartView: String = "value",
    var eventsHiddenLines: MutableList<String> = mutableListOf(),
    var pfOnboardingSeen: Boolean = false,
    var trackerLookAnswered: Boolean = false, // the new tracker look notice was answered with /sbolegacylook
    var badgesInTab: Boolean = true,
    var badgesInChat: Boolean = true,
    var badgeOwnInF5: Boolean = true,
    var badgeNamesInChat: String = "off", // "own" or "all": names inside chat messages look like the sender's, see SboBadges.NAMES_*
    var badgeNamesTag: Boolean = true, // false: names inside chat messages only get their color, no team tag
    var badgeAnimationsOfOthers: Boolean = true, // false shows other players' moving badge colors standing still
    var badgeMotionOfOthers: Boolean = true, // false keeps the chars of other players' badges from moving above heads
    var badgeAnimationsInTab: Boolean = true, // false shows every badge in the tab list standing still, your own too
    var badgeAccess: MutableMap<String, Boolean> = mutableMapOf(), // account uuid -> may have a badge
    var collapsedSettings: MutableSet<String> = mutableSetOf(), // collapsed subs in the settings, "Diana/Waypoints"
    var currentCoaUuid: String = "",
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
