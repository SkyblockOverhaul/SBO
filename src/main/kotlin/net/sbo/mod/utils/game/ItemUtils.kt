package net.sbo.mod.utils.game

import gg.essential.universal.utils.toFormattedString
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component

object ItemUtils {

    fun getTimestamp(customData: CustomData?, nbt: CompoundTag? = customData?.copyTag()): Long {
        if (customData == null || nbt == null) return 0L
        if (!nbt.contains("timestamp")) return 0L
        return nbt.getLong("timestamp").orElse(0L)
    }

    fun getSBID(customData: CustomData?, nbt: CompoundTag? = customData?.copyTag()): String {
        if (customData == null || nbt == null) return ""
        if (!nbt.contains("id")) return ""
        return nbt.getString("id").orElse("")
    }

    fun getUUID(customData: CustomData?, nbt: CompoundTag? = customData?.copyTag()): String {
        if (customData == null || nbt == null) return ""
        if (!nbt.contains("uuid")) return ""
        return nbt.getString("uuid").orElse("")
    }

    fun getDisplayName(stack: ItemStack): String {
        return stack.hoverName.toFormattedString()
    }

    fun getLoreList(stack: ItemStack): List<String> {
        val linesList: List<Component> = stack.get(DataComponents.LORE)?.lines ?: listOf()
        return linesList.map { it.toFormattedString() }
    }
}