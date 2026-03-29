package net.sbo.mod.general

import net.minecraft.text.HoverEvent
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.DianaTracker
import net.sbo.mod.settings.categories.QOL
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.data.Item
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.InventorySlotUpdateEvent
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.CHAT_SCREEN_FILTER
import net.sbo.mod.utils.overlay.CRAFTING_PLAYER_INVENTORY_FILTER
import net.sbo.mod.utils.overlay.OverlayExamples
import net.sbo.mod.utils.overlay.OverlayTextLine
import java.util.regex.Pattern

object Pickuplog {
    data class OverlayLineData(var amount: Int, val name: String, var modified: Long)

    private var oldPurse: Long = 0L
    private var newPurse: Long = 0L

    private var oldInventory = mutableMapOf<String, Item>()
    private var newInventory = mutableMapOf<String, Item>()

    private val regex = Regex("""\+([\d,]+) ([^(]+)""")

    private val overlay: Overlay = Overlay("pickuplog", 5f, 5f, 1f, listOf(CHAT_SCREEN_FILTER, CRAFTING_PLAYER_INVENTORY_FILTER), OverlayExamples.pickupLogExample)

    private val itemsShowAdded: MutableList<MutableMap<String, OverlayLineData>> = mutableListOf()
    private val itemsShowRemoved: MutableList<MutableMap<String, OverlayLineData>> = mutableListOf()

    fun init() {
        overlay.init()
        overlay.setCondition { QOL.pickuplogOverlay }

        Register.onChatMessageCancable(Pattern.compile("(.*?) item(.*?) (.*?)", Pattern.DOTALL)) { message, matchResult ->
            var cancel = true
            if (World.isInSkyblock() && matchResult.group(1).contains("Sacks")) {
                message.siblings.forEach { part ->
                    if (part.string.contains(" item")) {
                        val hover = part.style.hoverEvent
                        if (hover is HoverEvent.ShowText) {
                            val plain = hover.value().string
                            regex.findAll(plain).forEach { match ->
                                val amount = match.groupValues[1].replace(",", "")
                                val item = match.groupValues[2].trim()
                                DianaTracker.trackWithSacksMessage(item, amount.toInt())
                            }
                        }
                        cancel = !QOL.hideSacksMSG
                    }
                }
            }
            cancel
        }
    }

    @SboEvent
    fun onInventorySlotUpdate(event: InventorySlotUpdateEvent) {
        if (mc.player == null || !World.isInSkyblock() || World.getWorld() == "None") return
        newInventory = Helper.readPlayerInv()
        newPurse = Helper.getPurse()
        if (oldInventory.isEmpty()) {
            oldInventory = newInventory
            oldPurse = newPurse
            return
        }
        compareInventory()
        oldInventory = newInventory
        oldPurse = newPurse
        updateOverlay()
    }

    fun compareInventory() {
        val purseChange = newPurse - oldPurse
        if (purseChange != 0L) {
            DianaTracker.trackScavengerCoins(purseChange)
        }

        val newItems = mutableListOf<Item>()
        val changedItemCounts = mutableListOf<Pair<Item, Int>>()

        for ((key, newItem) in newInventory) {
            val oldItem = oldInventory[key]
            if (oldItem == null) {
                newItems.add(newItem)
            } else if (newItem.count != oldItem.count) {
                val countChange = newItem.count - oldItem.count
                changedItemCounts.add(Pair(newItem, countChange))
            }
        }

        val removedItems = mutableMapOf<String, Item>()
        for ((key, oldItem) in oldInventory) {
            if (!newInventory.containsKey(key)) {
                removedItems[oldItem.itemId] = oldItem
            }
        }

        for (item in newItems) {
            refreshOverlay(item.itemId, item.name, item.count)
            if (item.itemUUID != "") {
                DianaTracker.trackWithPickuplog(item)
            } else {
                DianaTracker.trackWithPickuplogStackable(item, item.count)
            }
        }

        for ((item, countChange) in changedItemCounts) {
            refreshOverlay(item.itemId, item.name, countChange)
            if (countChange > 0) {
                DianaTracker.trackWithPickuplogStackable(item, countChange)
            }
        }

        for ((itemId, item) in removedItems) {
            val itemName = item.name
            val itemCount = -item.count
            refreshOverlay(itemId, itemName, itemCount)
        }
    }

    fun refreshOverlay(itemId: String, name: String, amount: Int) {
        val currentTime = System.currentTimeMillis()
        if (amount > 0) {
            val existingItem = itemsShowAdded.find { it.containsKey(itemId) }?.get(itemId)
            if (existingItem != null) {
                existingItem.amount += amount
                existingItem.modified = currentTime
            } else {
                itemsShowAdded.add(mutableMapOf(itemId to OverlayLineData(amount, name, currentTime)))
            }
        } else {
            val existingItem = itemsShowRemoved.find { it.containsKey(itemId) }?.get(itemId)
            if (existingItem != null) {
                existingItem.amount += amount
                existingItem.modified = currentTime
            } else {
                itemsShowRemoved.add(mutableMapOf(itemId to OverlayLineData(amount, name, currentTime)))
            }
        }
        updateOverlay()
    }

    fun updateOverlay() {
        val currentTime = System.currentTimeMillis()
        val lines = mutableListOf<OverlayTextLine>()

        val newAddedList = mutableListOf<MutableMap<String, OverlayLineData>>()
        itemsShowAdded.forEach { map ->
            val (_, data) = map.entries.first()
            if (currentTime - data.modified <= 6000) {
                newAddedList.add(map)
                lines.add(OverlayTextLine("§a+ ${data.amount}x §r${data.name}"))
            }
        }
        itemsShowAdded.clear()
        itemsShowAdded.addAll(newAddedList)

        val newRemovedList = mutableListOf<MutableMap<String, OverlayLineData>>()
        itemsShowRemoved.forEach { map ->
            val (_, data) = map.entries.first()
            if (currentTime - data.modified <= 6000) {
                newRemovedList.add(map)
                lines.add(OverlayTextLine("§c- ${-data.amount}x §r${data.name}"))
            }
        }
        itemsShowRemoved.clear()
        itemsShowRemoved.addAll(newRemovedList)

        overlay.setLines(lines)
    }
}