package net.sbo.mod.utils.game

import gg.essential.universal.utils.toFormattedString
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.nbt.NbtCompound
import net.minecraft.item.ItemStack
import net.minecraft.text.Text

object ItemUtils {

    fun getTimestamp(customData: NbtComponent?, nbt: NbtCompound? = customData?.copyNbt()): Long {
        if (customData == null || nbt == null) return 0L
        if (!nbt.contains("timestamp")) return 0L
        return nbt.getLong("timestamp").orElse(0L)
    }

    fun getSBID(customData: NbtComponent?, nbt: NbtCompound? = customData?.copyNbt()): String {
        if (customData == null || nbt == null) return ""
        if (!nbt.contains("id")) return ""
        return nbt.getString("id").orElse("")
    }

    fun getUUID(customData: NbtComponent?, nbt: NbtCompound? = customData?.copyNbt()): String {
        if (customData == null || nbt == null) return ""
        if (!nbt.contains("uuid")) return ""
        return nbt.getString("uuid").orElse("")
    }

    fun getDisplayName(stack: ItemStack): String {
        return stack.name.toFormattedString()
    }

    fun getLoreList(stack: ItemStack): List<String> {
        val linesList: List<Text> = stack.get(DataComponentTypes.LORE)?.lines ?: listOf()
        return linesList.map { it.toFormattedString() }
    }
}