package net.sbo.mod.utils.data.configs.partyfinder

import net.sbo.mod.utils.data.DataManager

data class PartyFinderConfigState(
    // Last used input per party key ("kuudra/infernal")
    var drafts: MutableMap<String, PartyDraft> = mutableMapOf(),
    // Favorite category or subcategory keys, in the order the player sorted them
    var favorites: MutableList<String> = mutableListOf(),
    var startWithFavorites: Boolean = false,
    // Seconds between automatic reloads of the party list, 0 = off
    var autoRefreshSeconds: Int = 30,
    // Font id of the party finder window, see PartyFinderGui.FONTS
    var font: String = "inter",
    // Own GUI scale of the party finder window, null = Minecraft's GUI scale
    var uiScale: Float? = null,
    // Theme id, see PartyFinderThemes; custom themes are "custom:<file name>"
    var theme: String = "sbo-dark",
    // Hypixel colors show items one rarity higher
    var recombobulated: Boolean = false,
    // Party list filters per party key
    var listFilters: MutableMap<String, PartyListFilter> = mutableMapOf()
) {
    fun save() = DataManager.save(DataManager::partyFinderConfigState)
}
