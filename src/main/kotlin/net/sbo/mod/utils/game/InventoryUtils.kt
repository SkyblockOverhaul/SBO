package net.sbo.mod.utils.game

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.events.Register
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.Item

object InventoryUtils {
    private var currentItemId: String = "AIR"
    private var currentItemStartTime: Long = System.nanoTime()
    private var currentItemIsDianaSpade = false
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
        val lookup = if (stack.isEmpty) null else ItemLookup(stack)
        val newItemId = lookup?.sbId?.takeIf { it.isNotEmpty() }
            ?: if (lookup == null) "AIR" else BuiltInRegistries.ITEM.getKey(stack.item).toString()
        val now = System.nanoTime()

        if (newItemId != currentItemId) {
            val durationHeld = now - currentItemStartTime
            if (durationHeld >= MIN_HOLD_NS) {
                heldHistory.add(Triple(currentItemId, durationHeld, now))
            }
            heldHistory.removeAll { (_, _, timestampStopped) -> now - timestampStopped > HISTORY_RETENTION_NS }
            currentItemId = newItemId
            currentItemStartTime = now
            currentItemIsDianaSpade = lookup?.isDianaSpade() == true
        }
    }

    /** The [System.nanoTime] timestamp when the current Diana spade was taken out, or null if none is held. */
    fun dianaSpadeHeldSince(): Long? = if (currentItemIsDianaSpade) currentItemStartTime else null

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

        if (itemId in currentItemId && now - currentItemStartTime >= requiredNs) {
            return true
        }

        return heldHistory.any { (histId, histDuration, stoppedAt) ->
            itemId in histId &&
            histDuration >= requiredNs &&
            now - stoppedAt <= RECENT_HOLD_NS
        }
    }

    private var prevInv = mutableMapOf<String, Item>()

    private fun getCursorItemStack(): ItemStack? {
        val handler = mc.player?.containerMenu ?: return null
        return handler.carried
    }

    fun readPlayerInv(): Map<String, Item> {
        val inventory = Player.getPlayerInventory()?.getNonEquipmentItems() ?: return emptyMap<String, Item>()
        val invItems = mutableMapOf<String, Item>()

        if (getCursorItemStack()?.count != 0) return prevInv

        for ((slot, stack) in inventory.withIndex()) {
            if (slot == 8) continue // Skip SB Star
            if (!stack.isEmpty) {
                val id: String
                val item: Item
                val lookup = ItemLookup(stack)
                val sbId = lookup.sbId
                var isChimera = false
                if (sbId == "ENCHANTED_BOOK") {
                    val lore = lookup.cleanLoreList
                    for (line in lore) {
                        if (line.contains("Chimera")) {
                            isChimera = true
                            break
                        }
                    }
                }

                if (!isChimera) {
                    item = Item(
                        sbId,
                        lookup.uuid,
                        lookup.displayName,
                        lookup.timestamp,
                        stack.count
                    )
                    id = if (item.itemUUID != "") item.itemUUID else item.itemId
                } else {
                    item = Item(
                        "CHIMERA",
                        lookup.uuid,
                        "§d§lChimera",
                        lookup.timestamp,
                        stack.count
                    )
                    id = "CHIMERA"
                }

                val existing = invItems.putIfAbsent(id, item)

                if (existing != null) {
                    existing.count += item.count
                }
            }
        }
        prevInv = invItems
        return invItems
    }

    fun playerHasItem(sbId: String): Boolean = playerHasItem { it.sbId == sbId }

    inline fun playerHasItem(predicate: (ItemLookup) -> Boolean): Boolean {
        val inventory = Player.getPlayerInventory() ?: return false
        return inventory.getNonEquipmentItems().any { stack -> !stack.isEmpty && predicate(ItemLookup(stack)) }
    }

    fun getKillsFromLore(stack: ItemStack?): Int {
        if (stack == null || stack.isEmpty) return 0

        val linesList: List<Component> = stack.get(DataComponents.LORE)?.lines ?: return 0

        if (linesList.isEmpty()) return 0

        for (lineText in linesList) {
            val line = lineText.string
            if (line.startsWith("Kills: ")) {
                val numberString = line.substringAfter("Kills: ")
                    .replace(",", "")
                    .trim()

                return numberString.toIntOrNull() ?: 0
            }
        }

        return 0
    }
}
