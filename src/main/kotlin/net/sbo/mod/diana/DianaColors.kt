package net.sbo.mod.diana

import net.sbo.mod.utils.math.NumberUtil.formatNumber

/** § colors for Diana levels, kills, griffin items and rarities. */
object DianaColors {
    fun matchLvlToColor(lvl: Int): String {
        return when {
            lvl >= 480 -> "§4$lvl"
            lvl >= 440 -> "§c$lvl"
            lvl >= 400 -> "§6$lvl"
            lvl >= 360 -> "§5$lvl"
            lvl >= 320 -> "§d$lvl"
            lvl >= 280 -> "§9$lvl"
            lvl >= 240 -> "§3$lvl"
            lvl >= 200 -> "§b$lvl"
            else -> "§7$lvl"
        }
    }

    fun getNumberColor(number: Int, range: Int): String {
        return when (number) {
            range -> "§c$number"
            range - 1 -> "§6$number"
            else -> "§9$number"
        }
    }

    fun getGriffinItemColor(item: String?): String {
        if (item.isNullOrEmpty()) return ""
        val name = item.replace("PET_ITEM_", "").replace("_", " ").replaceFirstChar { it.uppercase() }
        return when (name) {
            "Four Eyed Fish" -> "§5$name"
            "Dwarf Turtle Shelmet" -> "§a$name"
            "Crochet Tiger Plushie" -> "§5$name"
            "Antique Remedies" -> "§5$name"
            "Lucky Clover" -> "§a$name"
            "Minos Relic" -> "§5$name"
            else -> "§7$name"
        }
    }

    fun getRarity(item: String): String {
        return when (item.lowercase().trim()) {
            "common" -> "§f$item"
            "uncommon" -> "§a$item"
            "rare" -> "§9$item"
            "epic" -> "§5$item"
            "legendary" -> "§6$item"
            "mythic" -> "§d$item"
            else -> "§7$item"
        }
    }

    fun matchDianaKillsToColor(kills: Int): String {
        return when {
            kills >= 200_000 -> "§6${formatNumber(kills, true)}"
            kills >= 150_000 -> "§e${formatNumber(kills, true)}"
            kills >= 100_000 -> "§c${formatNumber(kills, true)}"
            kills >= 75_000 -> "§d${formatNumber(kills, true)}"
            kills >= 50_000 -> "§9${formatNumber(kills, true)}"
            kills >= 25_000 -> "§a${formatNumber(kills, true)}"
            kills >= 10_000 -> "§2${formatNumber(kills, true)}"
            else -> "§7${formatNumber(kills, true)}"
        }
    }
}
