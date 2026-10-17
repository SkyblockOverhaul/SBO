package net.sbo.mod.utils

import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.math.SboVec
import java.util.*

val ZERO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

object Player {
    fun getLastPosition(): SboVec {
        val player = mc.player ?: return SboVec.ZERO
        return SboVec(player.x, player.y, player.z)
    }

    fun getUUIDString(): String = mc.player?.stringUUID ?: ""

    // Works in the main menu
    fun accountUuid(): String = mc.user.profileId.toString().replace("-", "").lowercase()

    fun getUUID(): UUID = mc.player?.uuid ?: ZERO_UUID

    fun getPlayerInventory(): List<ItemStack> {
        val inventory = mc.player?.inventory?.toList()
        return inventory ?: emptyList()
    }

    fun getName(): String? = mc.player?.name?.string

    /** Name of the logged in account, also in the main menu. */
    fun accountName(): String = mc.user.name
}
