package net.sbo.mod.utils.events.impl.entity

import net.minecraft.world.entity.decoration.ArmorStand

/**
 * Event triggered when a Diana mob (represented by an ArmorStandEntity) dies.
 *
 * @param name The name of the mob.
 * @param entity The ArmorStandEntity representing the mob.
 */
class DianaMobDeathEvent(val name: String, val entity: ArmorStand)