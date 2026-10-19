package net.sbo.mod.utils.game

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.sbo.mod.utils.chat.toFormattedString
import net.sbo.mod.utils.Helper.removeFormatting

private val SENTINEL = Any()
private val LONG_SENTINEL = Long.MIN_VALUE + 31L

class ItemLookup(private val stack: ItemStack) {
    private var customDataCached: Any? = SENTINEL
    private val customData: CustomData?
        get() {
            if (customDataCached !== SENTINEL) return customDataCached as CustomData?
            return (stack[DataComponents.CUSTOM_DATA]).also { customDataCached = it }
        }

    private var nbtCached: Any? = SENTINEL
    private val nbt: CompoundTag?
        get() {
            if (nbtCached !== SENTINEL) return nbtCached as CompoundTag?
            return customData?.tag.also { nbtCached = it }
        }

    private var sbIdCached: String? = null
    val sbId: String
        get() = sbIdCached ?: (nbt?.getString("id")?.orElse("") ?: "").also { sbIdCached = it }

    private var uuidCached: String? = null
    val uuid: String
        get() = uuidCached ?: (nbt?.getString("uuid")?.orElse("") ?: "").also { uuidCached = it }

    private var timestampCached = LONG_SENTINEL
    val timestamp: Long
        get() {
            if (timestampCached != LONG_SENTINEL) return timestampCached
            return (nbt?.getLong("timestamp")?.orElse(0L) ?: 0L).also { timestampCached = it }
        }

    private var displayNameCached: String? = null
    val displayName: String
        get() = displayNameCached ?: stack.hoverName.toFormattedString().also { displayNameCached = it }

    private var loreListCached: List<String>? = null
    val loreList: List<String>
        get() = loreListCached ?: (stack[DataComponents.LORE]?.lines?.map { it.toFormattedString() }
            ?: emptyList()).also { loreListCached = it }

    private var cleanLoreListCached: List<String>? = null
    val cleanLoreList: List<String>
        get() = cleanLoreListCached ?: (stack[DataComponents.LORE]?.lines?.map { it.string.removeFormatting() }
            ?: emptyList()).also { cleanLoreListCached = it }
}
