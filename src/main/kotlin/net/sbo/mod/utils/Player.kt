package net.sbo.mod.utils

import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.math.SboVec
import java.util.UUID

object Player {
    fun getLastPosition(): SboVec {
        val player = mc.player ?: return SboVec.ZERO
        return SboVec(player.x, player.y, player.z)
    }

    fun getUUIDString(): String {
        return mc.player?.stringUUID ?: ""
    }

    fun getUUID(): UUID {
        return mc.player?.uuid ?: UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    fun getPlayerInventory(): List<ItemStack> {
        val inventory = mc.player?.inventory?.toList()
        return inventory ?: emptyList()
    }

    fun getName(): String? {
        return mc.player?.name?.string
    }
}
