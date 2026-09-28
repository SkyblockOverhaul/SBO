package net.sbo.mod.utils.data

import net.sbo.mod.utils.data.configs.achievements.AchievementsData
import net.sbo.mod.utils.data.configs.diana.*
import net.sbo.mod.utils.data.configs.overlay.OverlayData
import net.sbo.mod.utils.data.configs.partyfinder.PartyFinderConfigState
import net.sbo.mod.utils.data.configs.partyfinder.PartyFinderData
import net.sbo.mod.utils.data.configs.sbo.SboData
import net.sbo.mod.utils.data.configs.sound.SoundSettingsData

/**
 * Central registry for all configs.
 * Adding a new config = one register() line in registerAll().
 */
object DataRegistry {
    private val _entries: LinkedHashMap<String, ConfigEntry<*>> = linkedMapOf()
    val entries: Collection<ConfigEntry<*>> get() = _entries.values

    fun register(entry: ConfigEntry<*>) { _entries[entry.name] = entry }
    fun get(name: String): ConfigEntry<*>? = _entries[name]
    fun contains(name: String): Boolean = _entries.containsKey(name)

    fun registerAll() {
        register(ConfigEntry("SboData", "SboData.json", SboData::class.java) { DataManager.sboData })
        register(ConfigEntry("AchievementsData", "sbo_achievements.json", AchievementsData::class.java) { DataManager.achievementsData })
        register(ConfigEntry("PastDianaEventsData", "pastDianaEvents.json", PastDianaEventsData::class.java) { DataManager.pastDianaEventsData })
        register(ConfigEntry("DianaTrackerTotalData", "dianaTrackerTotal.json", DianaTrackerTotalData::class.java) { DataManager.dianaTrackerTotal })
        register(ConfigEntry("DianaTrackerSessionData", "dianaTrackerSession.json", DianaTrackerSessionData::class.java) { DataManager.dianaTrackerSession })
        register(ConfigEntry("DianaTrackerMayorData", "dianaTrackerMayor.json", DianaTrackerMayorData::class.java) { DataManager.dianaTrackerMayor })
        register(ConfigEntry("PartyFinderConfigState", "partyFinderConfigState.json", PartyFinderConfigState::class.java) { DataManager.pfConfigState })
        register(ConfigEntry("PartyFinderData", "partyFinderData.json", PartyFinderData::class.java) { DataManager.partyFinderData })
        register(ConfigEntry("OverlayData", "overlayData.json", OverlayData::class.java) { DataManager.overlayData })
        register(ConfigEntry("SoundSettingsData", "soundSettingsData.json", SoundSettingsData::class.java) { DataManager.soundSettingsData })
    }
}
