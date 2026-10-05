package net.sbo.mod.utils.game

import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.utils.events.Register

object World {
    private var location = "None"

    fun init() {
        Register.onTick(1) {
            location = TabList.findInfo("Area: ") ?: "None"
        }
    }

    /**
     * Retrieves the current world name from the TabList.
     * If the world name is not found, it returns "None".
     */
    fun getWorld(): String = location

    /**
     * Checks if the player is currently in Skyblock.
     * This is determined by checking if the scoreboard title contains "SKYBLOCK".
     * @return true if the player is in Skyblock, false otherwise.
     */
    fun isInSkyblock(): Boolean {
        val title = ScoreBoard.getTitle()
        return title.contains("SKYBLOCK") || Debug.alwaysInSkyblock
    }
}
