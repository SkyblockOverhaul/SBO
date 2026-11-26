package net.sbo.mod.qol

import net.minecraft.client.player.LocalPlayer
import net.sbo.mod.settings.categories.General
import net.sbo.mod.utils.game.ItemLookup
import kotlin.random.Random

/**
 * Holding right click with an Aspect of the Void uses it every [General.fastAotvDelay] ticks instead of every 4.
 * Same idea as Nopo Mod's Fast AOTV, set from FastAotvMixin.
 */
object FastAotv {
    private const val AOTV = "ASPECT_OF_THE_VOID"

    /** The right click delay to use for [player] instead of vanilla's 4 ticks, -1 to keep vanilla's. */
    @JvmStatic
    fun delay(player: LocalPlayer?): Int {
        if (!General.fastAotv || player == null) return -1
        if (ItemLookup(player.mainHandItem).sbId != AOTV) return -1
        val base = General.fastAotvDelay.coerceIn(2, 4)
        if (!General.fastAotvJitter) return base
        // Whole ticks only, so the pace varies by now and then using the next tick instead of a steady rhythm
        val roll = Random.nextFloat()
        return when (base) {
            // Never 1 tick, that would be 20 uses a second
            2 -> if (roll < 0.2f) 3 else 2
            3 -> if (roll < 0.15f) 2 else if (roll < 0.3f) 4 else 3
            // Vanilla's delay, left as it is
            else -> base
        }
    }
}
