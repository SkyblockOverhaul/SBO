package net.sbo.mod.utils.events.impl.entity

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity

/**
 * Called when an entity is loaded into the world.
 * @param entity The entity that was loaded.
 * @param world The world the entity was loaded into.
 */
class EntityLoadEvent(val entity: Entity, val world: ClientLevel)