package net.sbo.mod.diana

import net.sbo.mod.config.Option
import net.sbo.mod.settings.categories.RareMobs

// The rare Diana mobs, for detection and for the settings that pick mobs (share, receive, shuriken check, hide own waypoints)
enum class RareMob(val display: String, override val label: String) : Option {
    INQ("Minos Inquisitor", "Inquisitor"),
    MANTICORE("Manticore", "Manticore"),
    KING("King Minos", "King Minos"),
    SPHINX("Sphinx", "Sphinx");

    // Read when needed: RareMobs uses this enum for its defaults, reading its colors while this class loads would loop
    val glowColor: Int
        get() = when (this) {
            INQ -> RareMobs.MinosInquisitorGlowColor
            MANTICORE -> RareMobs.ManticoreGlowColor
            KING -> RareMobs.KingMinosGlowColor
            SPHINX -> RareMobs.SphinxGlowColor
        }

    companion object {
        fun fromName(name: String): RareMob? {
            // Avoids Iterator object allocation, small optimization.
            for (i in 0 until entries.size) {
                val mob = entries[i]
                if (name.contains(mob.display)) return mob
            }
            return null
        }
    }
}
