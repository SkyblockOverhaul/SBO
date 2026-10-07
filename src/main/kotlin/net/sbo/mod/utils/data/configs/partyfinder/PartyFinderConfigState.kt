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
    // Own theme id of the party finder, null = the global one; see SboThemes, custom themes are "custom:<file name>"
    var theme: String? = null,
    // Hypixel colors show items one rarity higher
    var recombobulated: Boolean = false,
    var listFilters: MutableMap<String, PartyListFilter> = mutableMapOf(),
    var autoInvite: Boolean = true,
    var autoRequeue: Boolean = true
) {
    fun save() = DataManager.save(DataManager::partyFinderConfigState)
}
