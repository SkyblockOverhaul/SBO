package net.sbo.mod.utils.data.configs.partyfinder

import net.sbo.mod.utils.data.DataManager

data class PartyFinderData(
    var playerStatsUpdated: Long = 0,
    var playerStats: Map<String, PlayerStats> = emptyMap(),
) {
    fun save() = DataManager.save("PartyFinderData")
}
