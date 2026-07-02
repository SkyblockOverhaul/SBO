package net.sbo.mod.utils.events.impl.entity

import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.EntityHitResult

/**
 * Called when a player hits an entity.
 * @param player The player who hit the entity.
 * @param world The world where the entity is located.
 * @param hand The hand used to hit the entity.
 * @param entity The entity that was hit.
 * @param hitResult The result of the hit.
 */
class EntitiyHitEvent (
    val player: Player,
    val world: Level,
    private val hand: InteractionHand,
    val entity: Entity,
    private val hitResult: EntityHitResult?
)