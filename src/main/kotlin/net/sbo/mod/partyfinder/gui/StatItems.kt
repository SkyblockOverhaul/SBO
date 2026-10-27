package net.sbo.mod.partyfinder.gui

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import net.minecraft.world.item.ItemStack
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.item
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.profileviewer.BuiltItem
import net.sbo.mod.profileviewer.DisplayItemStacks

@Serializable
internal data class StatItem(val item: String, val model: String? = null, val texture: String? = null, val glint: Boolean = false)

/**
 * An item as icon for each stat, skill, slayer boss and general value, from assets/sbo/ui/stats/items.json.
 *
 * Where the looks come from: SBO ships no textures for these. Minecraft draws the items itself with the player's
 * resource packs. Head skins come from Hypixel's item list (api.hypixel.net/v2/resources/skyblock/items) and from the
 * NotEnoughUpdates repo for pets (MIT). The `hypixel_skyblock:item/...` models are from Hypixel's item list too and
 * only show with Hypixel's own resource pack.
 */
internal object StatItems {
    private val defs: Map<String, StatItem> by lazy {
        runCatching {
            val text = StatItems::class.java.getResourceAsStream("/assets/sbo/ui/stats/items.json")!!.use { it.readBytes().decodeToString() }
            PartyFinderApi.json.decodeFromString(MapSerializer(String.serializer(), StatItem.serializer()), text)
        }.getOrDefault(emptyMap())
    }

    private val built = HashMap<String, BuiltItem>()

    fun stack(key: String): ItemStack? {
        built[key]?.let { return it.stack() }
        val def = defs[key] ?: return null
        val item = DisplayItemStacks.icon(def.item, def.texture, def.model, def.glint) ?: return null
        built[key] = item
        return item.stack()
    }
}

internal fun NodeBuilder.statItem(key: String) {
    StatItems.stack(key)?.let { item(it, className = "pf-stat-item", decorations = false) }
}
