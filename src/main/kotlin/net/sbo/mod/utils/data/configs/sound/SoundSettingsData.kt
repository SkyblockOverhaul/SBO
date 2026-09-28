package net.sbo.mod.utils.data.configs.sound

import net.sbo.mod.utils.data.DataManager

data class SoundSettingsData(
    var rareMobSound: String = "",
    var rareMobVolume: Float = 1.0f,
    var lowInqHpSound: String = "",
    var lowInqHpVoume: Float = 1.0f,
    var lowSphinxHpSound: String = "",
    var lowSphinxHpVoume: Float = 1.0f,
    var lowKingHpSound: String = "",
    var lowKingHpVoume: Float = 1.0f,
    var lowMantiHpSound: String = "",
    var lowMantiHpVoume: Float = 1.0f,
    var inqSound: String = "",
    var inqVolume: Float = 1.0f,
    var sphinxSound: String = "",
    var sphinxVolume: Float = 1.0f,
    var kingSound: String = "",
    var kingVolume: Float = 1.0f,
    var mantiSound: String = "",
    var mantiVolume: Float = 1.0f,
    var cocoonSound: String = "",
    var cocoonVolume: Float = 1.0f,
    var burrowFoundSound: String = "",
    var burrowVolume: Float = 1.0f,
    var chimSound: String = "",
    var chimVolume: Float = 1.0f,
    var bfSound: String = "",
    var bfVolume: Float = 1.0f,
    var coreSound: String = "",
    var coreVolume: Float = 1.0f,
    var stingerSound: String = "",
    var stingerVolume: Float = 1.0f,
    var woolSound: String = "",
    var woolVolume: Float = 1.0f,
    var relicSound: String = "",
    var relicVolume: Float = 1.0f,
    var stickSound: String = "",
    var stickVolume: Float = 1.0f,
    var miscDropSound: String = "",
    var miscDropVolume: Float = 1.0f
) {
    fun save() = DataManager.save(DataManager::soundSettingsData)
}
