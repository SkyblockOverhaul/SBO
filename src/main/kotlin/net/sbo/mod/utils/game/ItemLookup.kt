package net.sbo.mod.utils.game

import gg.essential.universal.utils.toFormattedString
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

class ItemLookup(private val stack: ItemStack) {
    val customData: CustomData? by lazy(LazyThreadSafetyMode.NONE) { this.stack.get(DataComponents.CUSTOM_DATA) }
    val nbt: CompoundTag? by lazy(LazyThreadSafetyMode.NONE) { this.customData?.copyTag() }

    val sbId: String by lazy(LazyThreadSafetyMode.NONE) { this.nbt?.getString("id")?.orElse("") ?: "" }
    val uuid: String by lazy(LazyThreadSafetyMode.NONE) { this.nbt?.getString("uuid")?.orElse("") ?: "" }
    val timestamp: Long by lazy(LazyThreadSafetyMode.NONE) { this.nbt?.getLong("timestamp")?.orElse(0L) ?: 0L }

    val displayName: String by lazy(LazyThreadSafetyMode.NONE) { this.stack.hoverName.toFormattedString() }
    val loreList: List<String> by lazy (LazyThreadSafetyMode.NONE) {
        val lines = this.stack.get(DataComponents.LORE)?.lines ?: return@lazy emptyList()
        lines.map { it.toFormattedString() }
    }
}
