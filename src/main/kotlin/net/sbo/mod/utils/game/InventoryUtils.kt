package net.sbo.mod.utils.game

import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.events.Register
import kotlin.time.Duration

object InventoryUtils {
    private var currentItemId: String = "AIR"
    private var currentItemStartTime: Long = System.currentTimeMillis()
    private val heldHistory = mutableListOf<Triple<String, Long, Long>>()

    fun init() {
        Register.onTick(1) {
            val client = SBOKotlin.mc
            if (client.player == null) return@onTick
            if (!World.isInSkyblock()) return@onTick
            trackHeldItemDuration(client)
        }
    }

    private fun trackHeldItemDuration(client: Minecraft) {
        val stack = client.player?.mainHandItem ?: ItemStack.EMPTY

        val newItemId = getInternalName(stack)
        val now = System.currentTimeMillis()

        if (newItemId != currentItemId) {
            val durationHeld = now - currentItemStartTime
            if (durationHeld > 50) {
                heldHistory.add(Triple(currentItemId, durationHeld, now))
            }
            heldHistory.removeAll { (_, _, timestampStopped) -> (now - timestampStopped) > 5000 }
            currentItemId = newItemId
            currentItemStartTime = now
        }
    }

    /**
     * returns the SkyBlock ID (e.g. "HYPERION") if present,
     * otherwise returns vanilla ID (e.g. "minecraft:iron_sword")
     */
    fun getInternalName(stack: ItemStack): String {
        if (stack.isEmpty) return "AIR"

        val lookup = ItemLookup(stack)
        val sbId = lookup.sbId

        if (sbId.isNotEmpty()) return sbId

        return BuiltInRegistries.ITEM.getKey(stack.item).toString()
    }

    /**
     * Checks if an item is currently held OR was recently held.
     * * @param itemId The Item ID (e.g. "HYPERION" or "minecraft:stick")
     * @param duration The minimum time it must have been held (e.g. 500.milliseconds)
     */
    fun isItemHeld(
        itemId: String,
        duration: Duration
    ): Boolean {
        val now = System.currentTimeMillis()
        val requiredMs = duration.inWholeMilliseconds

        if (currentItemId.contains(itemId)) {
            if ((now - currentItemStartTime) >= requiredMs) return true
        }

        return heldHistory.any { (histId, histDuration, stoppedAt) ->
            histId.contains(itemId) &&
            histDuration >= requiredMs &&
            (now - stoppedAt) <= 200
        }
    }
}
