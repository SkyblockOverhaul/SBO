package net.sbo.mod.diana.achievements

import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.chat.Chat.textComponent
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.data.SboDataObject.achievementsData

class Achievement(
    val id: Int,
    val name: String,
    var description: String,
    val rarity: String,
    val previousId: Int? = null,
    val hidden: Boolean = false,
    val repeatable: Boolean = false,
) {
    val color = AchievementManager.rarityColorDict[rarity] ?: "§f"

    private fun checkYearReset() {
        val currentYear = SboDataObject.dianaTrackerMayor.year
        if (achievementsData.lastEventYear != currentYear) {
            achievementsData.currentEventAchievements.clear()
            achievementsData.lastEventYear = currentYear
            SboDataObject.save("AchievementsData")
        }
    }

    private fun showUnlockEffects() {
        var hiddenExtra = ""
        if (this.hidden) {
            this.description = this.description.substring(2)
            hiddenExtra = "§7[Secret Achievement] "
        }
        val player = mc.player
        if (this.rarity == "Divine" || this.rarity == "Impossible" || this.rarity == "Celestial") {
            Helper.showTitle("§kd§r $color$name §kd§r", "§aAchievement Unlocked!", 0, 50, 20)
            Chat.chat(textComponent("§6[SBO] §aAchievement Unlocked §7>> $color§kd§r $color$name §kd§r", "$hiddenExtra§a$description"))
            mc.level?.playSound(player, player?.blockPosition()!!, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f)

        } else {
            Helper.showTitle("$color$name", "§aAchievement Unlocked!", 0, 50, 20)
            Chat.chat(textComponent("§6[SBO] §aAchievement Unlocked §7>> $color$name", "$hiddenExtra§a$description"))
            mc.level?.playSound(player, player?.blockPosition()!!, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f)
        }
    }

    fun unlock() {
        checkYearReset()

        val currentTotal = achievementsData.totalAchievements.getOrDefault(id, 0)
        achievementsData.totalAchievements[id] = currentTotal + 1


        if ((this.repeatable && Debug.repeatableAchie)) {
            achievementsData.currentEventAchievements[id] = true
            AchievementManager.achievementsUnlockedEvent += 1
        }
        AchievementManager.achievementsUnlockedTotal += 1

        SboDataObject.save("AchievementsData")

        showUnlockEffects()
    }

    fun lock() {
        achievementsData.totalAchievements.remove(id)
        achievementsData.currentEventAchievements.remove(id)
        AchievementManager.achievementsUnlockedTotal -= 1
        if ((this.repeatable && Debug.repeatableAchie)) AchievementManager.achievementsUnlockedEvent -= 1
        if (this.hidden) {
            this.description = "§k" + this.description
        }
    }

    fun loadState() {
        if (isUnlocked(true)) {
            AchievementManager.achievementsUnlockedTotal += 1
        } else {
            if (this.hidden) this.description = "§k" + this.description
        }
        if ((this.repeatable && Debug.repeatableAchie) && isUnlocked()) {
            AchievementManager.achievementsUnlockedEvent += 1
        }
    }

    fun canBeUnlocked(): Boolean {
        checkYearReset()

        if (!(this.repeatable && Debug.repeatableAchie)) {
            return (achievementsData.totalAchievements[id] ?: 0) == 0
        }
        return achievementsData.currentEventAchievements[id] != true
    }

    fun isUnlocked(total: Boolean = false): Boolean {
        checkYearReset()

        if ((this.repeatable && Debug.repeatableAchie) && !total) {
            return achievementsData.currentEventAchievements[id] ?: false
        }
        return (achievementsData.totalAchievements[id] ?: 0) > 0
    }
}