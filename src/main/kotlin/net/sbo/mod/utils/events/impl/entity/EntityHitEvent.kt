package net.sbo.mod.utils.events.impl.entity

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

/**
 * Called when a player hits an entity.
 * @param player The player who hit the entity.
 * @param world The world where the entity is located.
 * @param entity The entity that was hit.
 */
class EntityHitEvent (
    val player: Player,
    val world: Level,
    val entity: Entity,
)
