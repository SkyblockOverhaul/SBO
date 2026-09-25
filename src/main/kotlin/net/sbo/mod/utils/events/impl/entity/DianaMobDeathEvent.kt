package net.sbo.mod.utils.events.impl.entity

import net.minecraft.world.entity.decoration.ArmorStand

/**
 * Event triggered when a Diana mob (represented by an ArmorStandEntity) dies.
 * <p>
 * The logic is handled inside DianaMobDetect.kt file. The event will not trigger
 * if the ArmorStand dies before showing a 0 HP label. Some mods, such as NoammAddons
 * below version 1.2.5 historically had a bug with the "Hide 0 Health" feature where it
 * removed the entity instead of just hiding it - which caused this event to not fire.
 * For that reason, it was marked with "breaks" in the fabric.mod.json.
 *
 * @param name The name of the mob.
 * @param entity The ArmorStandEntity representing the mob.
 */
class DianaMobDeathEvent(val name: String, val entity: ArmorStand)
