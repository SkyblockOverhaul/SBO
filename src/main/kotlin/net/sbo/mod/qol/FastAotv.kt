package net.sbo.mod.qol

import net.minecraft.client.player.LocalPlayer
import net.sbo.mod.settings.categories.General
import net.sbo.mod.utils.game.ItemLookup

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
        return General.fastAotvDelay.coerceAtLeast(2)
    }
}
