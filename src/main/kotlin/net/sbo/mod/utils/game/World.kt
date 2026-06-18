package net.sbo.mod.utils.game

import net.sbo.mod.settings.categories.Debug

object World {
    /**
     * Retrieves the current world name from the TabList.
     * If the world name is not found, it returns "None".
     */
    fun getWorld(): String {
        val worldName = TabList.findInfo("Area: ") ?: "None"
        return worldName
    }

    /**
     * Checks if the player is currently in Skyblock.
     * This is determined by checking if the scoreboard title contains "skyblock".
     * @return true if the player is in Skyblock, false otherwise.
     */
    fun isInSkyblock(): Boolean {
        val title = ScoreBoard.getTitle().lowercase()
        return title.contains("skyblock") || Debug.alwaysInSkyblock
    }
}