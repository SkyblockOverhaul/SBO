package net.sbo.mod.utils.game

import gg.essential.universal.utils.toFormattedString
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

object ItemUtils {
    private fun CustomData?.orEmpty(): CompoundTag? = this?.copyTag()

    @Suppress("UNCHECKED_CAST")
    private fun <T> getNbtValue(customData: CustomData?, key: String, default: T): T {
        val nbt = customData.orEmpty() ?: return default
        return when {
            nbt.contains(key) && key == "timestamp" -> nbt.getLong(key).orElse(0L) as T
            nbt.contains(key) && (key == "uuid" || key == "id") -> nbt.getString(key).orElse("") as T
            else -> default
        }
    }

    fun getTimestamp(customData: CustomData?): Long = getNbtValue(customData, "timestamp", 0L)

    fun getSBID(customData: CustomData?): String = getNbtValue(customData, "id", "")

    fun getUUID(customData: CustomData?): String = getNbtValue(customData, "uuid", "")

    fun getDisplayName(stack: ItemStack): String = stack.hoverName.toFormattedString()

    fun getLoreList(stack: ItemStack): List<String> =
        (stack.get(DataComponents.LORE)?.lines ?: emptyList()).map { it.toFormattedString() }
}