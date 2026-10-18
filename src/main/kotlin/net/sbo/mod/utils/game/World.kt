package net.sbo.mod.utils.game

import net.sbo.mod.utils.events.Register
import net.sbo.mod.settings.categories.Debug

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
    fun isInSkyblock(): Boolean {
        return isInSkyblock
    }
}
