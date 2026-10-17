package net.sbo.mod.utils.data.configs.achievements

data class AchievementsView(
    var status: String = "all",
    var sort: String = "id",
    var type: String = "all",
    var hiddenRarities: MutableList<String> = mutableListOf(),
    var uiScale: Float? = null
) {
    companion object {
        /** The old single filter (Default, Rarity, Locked or Unlocked) as the separate status and sort. */
        fun fromLegacyFilter(filter: String): AchievementsView = when (filter.uppercase()) {
            "LOCKED" -> AchievementsView(status = "locked")
            "UNLOCKED" -> AchievementsView(status = "unlocked")
            "RARITY" -> AchievementsView(sort = "rarity")
            else -> AchievementsView()
        }
    }
}
