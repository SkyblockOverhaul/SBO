package net.sbo.mod.utils.data.configs.partyfinder

data class PlayerStats(
    var name: String = "",
    var sbLvl: Int = 0,
    var eman9: Boolean = false,
    var looting5daxe: Boolean = false,
    var emanLvl: Int = 0,
    var warnings: List<String> = emptyList(),
    var uuid: String = "",
    var clover: Boolean = false,
    var daxeLootingLvl: Int = 0,
    var daxeChimLvl: Int = 0,
    var invApi: Boolean = false,
    var magicalPower: Int = 0,
    var enrichments: Int = 0,
    var missingEnrichments: Int = 0,
    var griffinRarity: String = "",
    var griffinItem: String? = null,
    var killLeaderboard: Int = 0,
    var mythosKills: Long = 0L
)
