package net.sbo.mod.utils.game

import net.sbo.mod.utils.events.Register
import net.sbo.mod.settings.categories.Debug
import net.minecraft.world.entity.LivingEntity
import net.sbo.mod.SBOKotlin.mc

object World {
    @Volatile
    private var location = "None"
    private var inHub = false
    private var none = true

    private var isInSkyblock = false

    init {
        Register.onTick(1) {
            isInSkyblock = Debug.alwaysInSkyblock || ScoreBoard.getTitle().contains("SKYBLOCK")
        }
    }

    /**
     * Re-reads the area from the TabList. Called by TabList whenever its cache was rebuilt.
     */
    internal fun updateLocation() {
        location = TabList.findInfo("Area: ") ?: "None"
        inHub = "Hub" == location
        none = "None" == location
    }

    /**
     * Retrieves the current world name from the TabList.
     * If the world name is not found, it returns "None".
     */
    fun getWorld(): String = location

    fun isInHub(): Boolean = inHub

    fun isNone(): Boolean = none

    /**
     * Checks if the player is currently in Skyblock.
     * This is determined by checking if the scoreboard title contains "SKYBLOCK".
     * @return true if the player is in Skyblock, false otherwise.
     */
    fun isInSkyblock(): Boolean = isInSkyblock

    /**
     * Checks whether a mob is a valid entity in the hub. Checks that player is not null, level is not null and equal to the entity's level (reference equality), the entity must not be dead, the entity must be alive (different check than dead), and belong exactly to the current level (where a getEntity query with its entity ID returns exactly the same Entity instance with reference equality). Additionally performs invisibility check based on the parameter, although you usually do not want that.
     *
     * @param checkInvisibility whether or not a valid entity must not be invisible. Usually pass false for this, as Hypixel might hide and unhide the entity, and this checks the entity itself being invisible, not it is nametag; so for example an ArmorStand used only for its nametag counts as invisible, which would return as an invalid entity if this parameter was passed as true. Even for player entities; for example the Sphinx Diana mob becomes invisible whilst the question is pending an answer, then becomes visible again; only pass true into checkInvisibility if absolutely sure about it.
     */
    fun LivingEntity.isValidEntityInHub(checkInvisibility: Boolean): Boolean {
        val level = mc.level

        // Intentional reference equality; Entity#level() should return exactly mc.level, and ClientLevel#getEntity(I) should return exactly this entity; otherwise the entity does not belong to the current level.
        return mc.player != null && level != null && this.level() === mc.level && !this.dead && World.isInHub() && this.isAlive && (!checkInvisibility || !this.isInvisible) && level.getEntity(this.id) === this
    }
}
