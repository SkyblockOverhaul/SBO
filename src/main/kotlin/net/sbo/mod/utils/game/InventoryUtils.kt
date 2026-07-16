package net.sbo.mod.utils.game

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.events.Register
import kotlin.time.Duration
import java.util.concurrent.TimeUnit

object InventoryUtils {
    private var currentItemId: String = "AIR"
    private var currentItemStartTime: Long = System.nanoTime()
    private val heldHistory = mutableListOf<Triple<String, Long, Long>>() // durationNs, stoppedAtNs

    private val MIN_HOLD_NS = TimeUnit.MILLISECONDS.toNanos(50L)
    private val HISTORY_RETENTION_NS = TimeUnit.SECONDS.toNanos(5L)
    private val RECENT_HOLD_NS = TimeUnit.MILLISECONDS.toNanos(200L)

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
        val now = System.nanoTime()

        if (newItemId != currentItemId) {
            val durationHeld = now - currentItemStartTime
            if (durationHeld >= MIN_HOLD_NS) {
                heldHistory.add(Triple(currentItemId, durationHeld, now))
            }
            heldHistory.removeAll { (_, _, timestampStopped) -> now - timestampStopped > HISTORY_RETENTION_NS }
            currentItemId = newItemId
            currentItemStartTime = now
        }
    }

    /**
     * returns the SkyBlock ID (e.g. "HYPERION") if present,
     * otherwise returns vanilla ID (e.g. "minecraft:iron_sword")
     */
    private fun getInternalName(stack: ItemStack): String {
        if (stack.isEmpty) return "AIR"

        val lookup = ItemLookup(stack)
        val sbId = lookup.sbId

        if (sbId.isNotEmpty()) return sbId

        return BuiltInRegistries.ITEM.getKey(stack.item).toString()
    }

    /**
     * Checks if an item is currently held OR was recently held.
     * @param itemId The Item ID (e.g. "HYPERION" or "minecraft:stick")
     * @param duration The minimum time it must have been held (e.g. 500.milliseconds)
     */
    fun isItemHeld(
        itemId: String,
        duration: Duration
    ): Boolean {
        val now = System.nanoTime()
        val requiredNs = duration.inWholeNanoseconds

        if (currentItemId.contains(itemId)) {
            if (now - currentItemStartTime >= requiredNs) return true
        }

        return heldHistory.any { (histId, histDuration, stoppedAt) ->
            histId.contains(itemId) &&
            histDuration >= requiredNs &&
            now - stoppedAt <= RECENT_HOLD_NS
        }
    }
}
