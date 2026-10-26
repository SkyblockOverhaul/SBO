package net.sbo.mod.utils.events.impl.game

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult

class StartAttackingEvent (
    val player: Player,
    val target: HitResult,
    val world: Level
) {
    fun isAirOrEntity(): Boolean {
        return target.type == HitResult.Type.MISS || target.type == HitResult.Type.ENTITY
    }

    fun getMainHandItem(): ItemStack {
        return player.mainHandItem
    }
}