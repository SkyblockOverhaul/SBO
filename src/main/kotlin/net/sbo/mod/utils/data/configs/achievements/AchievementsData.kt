package net.sbo.mod.utils.data.configs.achievements

import net.sbo.mod.utils.data.DataManager

data class AchievementsData(
    var achievements: MutableMap<Int, Boolean>? = mutableMapOf(),
    var totalAchievements: MutableMap<Int, Int> = mutableMapOf(),
    var currentEventAchievements: MutableMap<Int, Boolean> = mutableMapOf(),
    var lastEventYear: Int = -1
) {
    fun save() = DataManager.save("AchievementsData")
}
