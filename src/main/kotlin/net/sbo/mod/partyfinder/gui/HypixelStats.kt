package net.sbo.mod.partyfinder.gui

import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.img

/** The icon and color of a SkyBlock stat like in Hypixel's stats menu. */
internal class HypixelStat(val icon: String, val color: String)

/**
 * Hypixel stats the party finder and the profile viewer can show with Hypixel's icon and color. Only stats with few
 * sources are here, so the backend can add them up from the profile. A party finder stat with the same id, like
 * tracking, gets its icon from here.
 *
 * Where the icons come from: the SVGs in assets/sbo/ui/stats are Hypixel's own stat icons from the Hypixel SkyBlock
 * Resource Pack (Hypixel Inc., "Hypixel SkyBlock Resource Pack License"). We took them from the font
 * fonts/hypixel-skyblock of the Hypixel SkyBlock Wiki (github.com/skyblock-wiki/wiki-assets), which has them from
 * that pack.
 */
internal object HypixelStats {
    const val ICONS = "sbo:ui/stats"

    private val STATS = mapOf(
        "tracking" to HypixelStat("tracking.svg", "light-purple"),
        "magicFind" to HypixelStat("magic-find.svg", "aqua"),
        "petLuck" to HypixelStat("pet-luck.svg", "light-purple"),
        "huntingFortune" to HypixelStat("hunting-fortune.svg", "light-purple")
    )

    fun ofStat(statId: String): HypixelStat? = STATS[statId]
}

/** The icon of a Hypixel stat, colored when the theme uses Hypixel colors. */
internal fun NodeBuilder.statIcon(stat: HypixelStat) {
    img(src = "${HypixelStats.ICONS}/${stat.icon}", className = "pf-stat-icon pf-c-${stat.color}")
}
