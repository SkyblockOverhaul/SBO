package net.sbo.mod.utils.game

import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent

object TabList {
    /**
     * Holds cached tab lines, rebuilt only when the tab list changed.
     */
    @Volatile
    private var cachedTabLines = emptyList<String>()

    /**
     * Set when the tab list changed (player info packets, world change, disconnect).
     */
    private var dirty = true

    /**
     * Registers a task that rebuilds the cache on the next tick after the tab list changed.
     */
    fun init() {
        Register.onTick(1) {
            if (dirty) updateCache()
        }
    }

    /**
     * Marks the cache as outdated. Called from ClientPacketListenerMixin after
     * player info update/remove packets were applied.
     */
    fun markDirty() {
        dirty = true
    }

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) = markDirty()

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) = markDirty()

    /**
     * Updates tab list cache by fetching, filtering and mapping the tab list.
     */
    private fun updateCache() {
        dirty = false
        val tabEntries = getTabEntries()
        val tabLines = ArrayList<String>(tabEntries.size)

        for (entry in tabEntries) {
            if (entry == null) continue

            val displayName = entry.tabListDisplayName
            val profile = entry.profile

            val text = displayName ?: profile.name?.let { Component.literal(it) } ?: continue
            tabLines.add(text.string.trim())
        }

        cachedTabLines = tabLines
        World.updateLocation()
    }

    /**
     * Returns a list of all PlayerListEntry objects from the current tab list.
     * Each PlayerListEntry object contains detailed information about a player.
     */
    private fun getTabEntries(): Collection<PlayerInfo?> = mc.connection?.onlinePlayers ?: emptyList()

    /**
     * Finds the value associated with a specific key in the tab list entries.
     * The key should be a prefix that appears at the start of the line in the tab list.
     * @param key The key to search for in the tab list entries.
     * @return The value associated with the key, or null if not found.
     */
    fun findInfo(key: String): String? {
        for (line in cachedTabLines) {
            if (line.startsWith(key)) {
                return line.substring(key.length)
            }
        }

        return null
    }
}
