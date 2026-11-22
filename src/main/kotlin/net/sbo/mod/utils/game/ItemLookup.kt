package net.sbo.mod.utils.game

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.sbo.mod.utils.chat.toFormattedString
import net.sbo.mod.utils.TextUtil.removeFormatting

private val SENTINEL = Any()
private const val LONG_SENTINEL = Long.MIN_VALUE + 31L
private const val T3_SPADE = "DEIFIC_SPADE"
private const val T2_SPADE = "ARCHAIC_SPADE"
private const val T1_SPADE = "ANCESTRAL_SPADE"

class ItemLookup(private val stack: ItemStack) {
    private var customDataCached: Any? = SENTINEL
    private val customData: CustomData?
        get() {
            if (customDataCached !== SENTINEL) return customDataCached as CustomData?
            return stack[DataComponents.CUSTOM_DATA].also { customDataCached = it }
        }

    private var nbtCached: Any? = SENTINEL
    private val nbt: CompoundTag?
        get() {
            if (nbtCached !== SENTINEL) return nbtCached as CompoundTag?
            return customData?.tag.also { nbtCached = it }
        }

    private var sbIdCached: String? = null
    val sbId: String
        get() = sbIdCached ?: nbt?.getString("id")?.orElse("").orEmpty().also { sbIdCached = it }

    private var uuidCached: String? = null
    val uuid: String
        get() = uuidCached ?: nbt?.getString("uuid")?.orElse("").orEmpty().also { uuidCached = it }

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
        get() = loreListCached ?: stack[DataComponents.LORE]?.lines?.map { it.toFormattedString() }
            .orEmpty().also { loreListCached = it }

    private var cleanLoreListCached: List<String>? = null
    val cleanLoreList: List<String>
        get() = cleanLoreListCached ?: stack[DataComponents.LORE]?.lines?.map { it.string.removeFormatting() }
            .orEmpty().also { cleanLoreListCached = it }
}

fun ItemLookup.isCrownOfAvarice(): Boolean {
    val sbId = this.sbId

    return sbId == "CROWN_OF_AVARICE" || sbId == "CROWN_OF_AVARICE_CELEBRATION"
}

fun ItemStack?.isCrownOfAvarice(): Boolean {
    if (this == null || this.isEmpty) return false

    return ItemLookup(this).isCrownOfAvarice()
}

fun ItemLookup.isDianaSpade(): Boolean {
    val sbId = this.sbId

    return sbId == T3_SPADE || sbId == T2_SPADE || sbId == T1_SPADE
}
