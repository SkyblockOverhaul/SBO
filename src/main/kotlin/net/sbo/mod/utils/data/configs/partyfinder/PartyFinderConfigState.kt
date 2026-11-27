package net.sbo.mod.utils.data.configs.partyfinder

import net.sbo.mod.utils.data.DataManager

data class PartyFinderConfigState(
    // Last used input per party key ("kuudra/infernal")
    var drafts: MutableMap<String, PartyDraft> = mutableMapOf(),
    // Favorite category or subcategory keys, in the order the player sorted them
    var favorites: MutableList<String> = mutableListOf(),
    var startWithFavorites: Boolean = false,
    // Favorite party types show only under Favorites in the sidebar
    var favoritesOnlyOnce: Boolean = false,
    // Seconds between automatic reloads of the party list, 0 = off
    var autoRefreshSeconds: Int = 30,
    // Font id of the party finder window, see PartyFinderGui.FONTS
    var font: String = "inter",
    // Own size of the party finder window, null = the global one, see UiScale
    var uiScale: Float? = null,
    // Own theme id of the party finder, null = the global one, see SboThemes. Custom themes are "custom:<file name>"
    var theme: String? = null,
    // Hypixel colors show items one rarity higher
    var recombobulated: Boolean = false,
    var listFilters: MutableMap<String, PartyListFilter> = mutableMapOf(),
    var autoInvite: Boolean = true,
    var autoRequeue: Boolean = true,
    // Kicks players who join a listed Kuudra party through Hypixel's party finder without meeting its requirements
    var autoKick: Boolean = false,
    // Offers to list a party on SBO too after it was listed in Hypixel's party finder
    var hypixelOffer: Boolean = true,
    // Removes the Kuudra party from SBO without asking when it leaves Hypixel's party finder
    var hypixelAutoDelist: Boolean = false,
    // Asks whether to remove the Kuudra party from SBO when it leaves Hypixel's party finder
    var hypixelDelistQuestion: Boolean = true,
    // Chat reminder when a party event starts, see PartyEventHints
    var eventHints: Boolean = true,
    // Chat reminder for Dragon parties after placing a Summoning Eye alone
    var dragonHints: Boolean = true,
    // Chat reminder for Safari parties when going on a run alone
    var safariHints: Boolean = true,
    // Chat reminder for Scatha parties when a Worm or Scatha spawns for a player alone
    var scathaHints: Boolean = true,
    // Day on which the offer after Hypixel's party finder pauses, because the player ignored it twice in a row
    var hypixelOfferPausedOn: String? = null,
    // SBO Party Finder buttons above Hypixel menus, see HypixelMenus
    var menuButtons: Boolean = true,
    // When each kind of reminder was shown last, in epoch milliseconds by PartyHintKind
    var hintsShownAt: MutableMap<String, Long> = mutableMapOf(),
    // Hypixel only prints the tier when it changes
    var lastKuudraTier: String? = null,
    // Players who can't request to join own parties and whose parties are hidden, see BlockedPlayers
    var blockedPlayers: MutableList<BlockedPlayer> = mutableListOf(),
    // Players pinned in the profile viewer, in the order the player sorted them
    var viewerFavorites: MutableList<FavoritePlayer> = mutableListOf()
) {
    fun save() = DataManager.save(DataManager::partyFinderConfigState)
}

/** The uuid without dashes and the name from when the player was blocked. */
data class BlockedPlayer(val uuid: String = "", val name: String = "")

/** The uuid without dashes and the name from when the player was last opened. */
data class FavoritePlayer(val uuid: String = "", val name: String = "")
