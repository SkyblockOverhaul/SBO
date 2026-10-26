package net.sbo.mod.profileviewer

import com.mojang.serialization.Dynamic
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import net.minecraft.SharedConstants
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.Identifier
import net.minecraft.util.datafix.DataFixers
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.TooltipDisplay
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.partyfinder.api.PartyFinderApi
import java.util.Base64
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream

/** An item as Hypixel stores it: the old 1.8 item id with name, lore and the Skyblock data. */
@Serializable
class DisplayItem(
    val id: Int,
    val damage: Int = 0,
    val count: Int = 1,
    val name: String = "",
    val lore: List<String> = emptyList(),
    // skin of player heads
    val texture: String? = null,
    // leather armor
    val color: Int? = null,
    val glint: Boolean = false,
    // model of the Hypixel resource pack, from Hypixel's item list
    val itemModel: String? = null,
    val extraAttributes: JsonObject = JsonObject(emptyMap())
) {
    @Volatile
    @Transient
    var built: BuiltItem? = null
        internal set
}

/** An item with and without the model of the Hypixel resource pack, which is only loaded while you are on Hypixel. */
class BuiltItem(private val plain: ItemStack, private val model: Identifier?) {
    private val withModel = model?.let { id -> plain.copy().also { it.set(DataComponents.ITEM_MODEL, id) } }

    fun stack(): ItemStack = if (withModel != null && DisplayItemStacks.hasModel(model!!)) withModel else plain
}

@Serializable
data class LoadoutItems(
    val name: String = "",
    // wardrobe slot and equipment set, both from 1
    val armorSet: Int? = null,
    val equipmentSet: Int? = null
)

@Serializable
data class BackpackItems(
    // from 0 like Hypixel numbers them
    val slot: Int,
    // the backpack itself
    val icon: DisplayItem? = null,
    val items: List<DisplayItem?> = emptyList()
)

@Serializable
data class PlayerItems(
    // hotbar first, like Hypixel sends it
    val inventory: List<DisplayItem?> = emptyList(),
    // helmet to boots
    val armor: List<DisplayItem?> = emptyList(),
    // necklace, cloak, belt, gloves
    val equipment: List<DisplayItem?> = emptyList(),
    val pet: DisplayItem? = null,
    // per slot helmet to boots, null for empty slots
    val wardrobe: List<List<DisplayItem?>?> = emptyList(),
    val wardrobeSlot: Int? = null,
    val equipmentSets: List<List<DisplayItem?>?> = emptyList(),
    val equipmentSlot: Int? = null,
    val loadouts: List<LoadoutItems> = emptyList(),
    val backpacks: List<BackpackItems> = emptyList(),
    // every page after the other, 45 slots each
    val enderChest: List<DisplayItem?> = emptyList(),
    val accessories: List<DisplayItem?> = emptyList(),
    // the pet menu, the active pet first
    val pets: List<DisplayItem> = emptyList()
) {
    // Hypixel leaves the worn set out of the wardrobe and the equipment sets, so it comes from what the player has on
    fun wardrobeSet(slot: Int): List<DisplayItem?>? = wardrobe.getOrNull(slot - 1) ?: armor.takeIf { slot == wardrobeSlot }

    fun equipmentSet(set: Int): List<DisplayItem?>? = equipmentSets.getOrNull(set - 1) ?: equipment.takeIf { set == equipmentSlot }

    fun all(): List<DisplayItem> =
        (inventory + armor + equipment + pet + wardrobe.flatMap { it.orEmpty() } + equipmentSets.flatMap { it.orEmpty() }).filterNotNull()

    // Storage, accessories and pets hold most of the items, so they are built after the ones that show first
    fun stored(): List<DisplayItem> =
        (backpacks.flatMap { listOf(it.icon) + it.items } + enderChest + accessories + pets).filterNotNull()

    companion object {
        /** The backend packs them like Hypixel packs inventories: gzipped JSON in base64. */
        fun unpack(packed: String): PlayerItems? = runCatching {
            val text = GZIPInputStream(Base64.getDecoder().decode(packed).inputStream()).use { it.readBytes().decodeToString() }
            PartyFinderApi.json.decodeFromString(serializer(), text)
        }.onFailure { SBOKotlin.logger.warn("[SBO] Could not read the profile viewer items", it) }.getOrNull()
    }
}

/**
 * Turns Hypixel's 1.8 items into the items of this Minecraft version with Minecraft's own data fixer, so ids, names,
 * lore and heads come out like Minecraft would load an old world. The Skyblock data goes into custom_data like Hypixel
 * sends it, which is what texture packs look at.
 *
 * Where the looks come from: SBO ships no item textures. Minecraft draws every item with the resource packs the player
 * has loaded. Items, names, lore and head skins come from the Hypixel API, the item models (`hypixel_skyblock:item/...`)
 * from Hypixel's item list and they only show with Hypixel's own resource pack, which Hypixel loads when you join.
 * Pet heads come from the NotEnoughUpdates repo (github.com/NotEnoughUpdates/NotEnoughUpdates-REPO, MIT).
 */
object DisplayItemStacks {
    private const val LEGACY_VERSION = 99
    private val MISSING_MODEL = Identifier.fromNamespaceAndPath("sbo", "no_such_model")
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "SBO item builder").apply { isDaemon = true } }

    // Vanilla lines Hypixel hides on its items
    private val HIDDEN: TooltipDisplay by lazy {
        val hidden = linkedSetOf<DataComponentType<*>>(
            DataComponents.ATTRIBUTE_MODIFIERS, DataComponents.ENCHANTMENTS, DataComponents.UNBREAKABLE,
            DataComponents.DYED_COLOR, DataComponents.STORED_ENCHANTMENTS, DataComponents.TRIM
        )
        TooltipDisplay(false, hidden)
    }

    /** Builds the stacks off the render thread, the first data fixer run takes a moment. Needs a world. */
    fun prepare(items: List<DisplayItem>, onDone: () -> Unit) {
        val missing = items.filter { it.built == null }
        if (missing.isEmpty()) return onDone()
        worker.execute {
            missing.forEach { item ->
                item.built = runCatching { build(item) }
                    .onFailure { SBOKotlin.logger.warn("[SBO] Could not build item ${item.name}: ${it.message}") }
                    .getOrNull()
            }
            mc.execute(onDone)
        }
    }

    private fun build(item: DisplayItem): BuiltItem? {
        val registries = mc.level?.registryAccess() ?: return null
        val current = SharedConstants.getCurrentVersion().dataVersion().version()
        val fixed = DataFixers.getDataFixer().update(References.ITEM_STACK, Dynamic<Tag>(NbtOps.INSTANCE, legacyTag(item)), LEGACY_VERSION, current)
        val stack = ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), fixed.value).result().orElse(null) ?: return null
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(compound(item.extraAttributes)))
        stack.set(DataComponents.TOOLTIP_DISPLAY, HIDDEN)
        if (item.glint) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
        return BuiltItem(stack, item.itemModel?.let(Identifier::tryParse))
    }

    /** A plain item like "minecraft:iron_sword", for icons. Needs a world. */
    fun icon(item: String, texture: String? = null, model: String? = null, glint: Boolean = false): BuiltItem? {
        val registries = mc.level?.registryAccess() ?: return null
        val components = CompoundTag()
        texture?.let {
            val textures = CompoundTag().apply {
                putString("name", "textures")
                putString("value", it)
            }
            components.put("minecraft:profile", CompoundTag().apply { put("properties", ListTag().apply { add(textures) }) })
        }
        if (glint) components.putBoolean("minecraft:enchantment_glint_override", true)
        val tag = CompoundTag().apply {
            putString("id", item)
            putInt("count", 1)
            put("components", components)
        }
        val stack = ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(null) ?: return null
        return BuiltItem(stack, model?.let(Identifier::tryParse))
    }

    // Without the Hypixel resource pack the model has no texture, so the item keeps its own look
    fun hasModel(id: Identifier): Boolean {
        val models = mc.modelManager
        return models.getItemModel(id) !== models.getItemModel(MISSING_MODEL)
    }

    private fun legacyTag(item: DisplayItem): CompoundTag {
        val display = CompoundTag()
        display.putString("Name", item.name)
        display.put("Lore", ListTag().apply { item.lore.forEach { add(StringTag.valueOf(it)) } })
        item.color?.let { display.putInt("color", it) }
        val tag = CompoundTag()
        tag.put("display", display)
        item.texture?.let { texture ->
            val textures = ListTag().apply { add(CompoundTag().apply { putString("Value", texture) }) }
            tag.put("SkullOwner", CompoundTag().apply {
                putString("Id", "00000000-0000-0000-0000-000000000000")
                put("Properties", CompoundTag().apply { put("textures", textures) })
            })
        }
        return CompoundTag().apply {
            putShort("id", item.id.toShort())
            putShort("Damage", item.damage.toShort())
            putByte("Count", item.count.coerceIn(1, 127).toByte())
            put("tag", tag)
        }
    }

    private fun compound(json: JsonObject): CompoundTag =
        CompoundTag().apply { json.forEach { (key, value) -> toTag(value)?.let { put(key, it) } } }

    private fun toTag(json: JsonElement): Tag? = when (json) {
        is JsonNull -> null
        is JsonObject -> compound(json)
        is JsonArray -> ListTag().apply { json.mapNotNull(::toTag).forEach { add(it) } }
        is JsonPrimitive -> when {
            json.isString -> StringTag.valueOf(json.content)
            json.booleanOrNull != null -> ByteTag.valueOf(json.booleanOrNull!!)
            json.longOrNull != null -> json.longOrNull!!.let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) IntTag.valueOf(it.toInt()) else LongTag.valueOf(it) }
            else -> json.doubleOrNull?.let { DoubleTag.valueOf(it) }
        }
    }
}
