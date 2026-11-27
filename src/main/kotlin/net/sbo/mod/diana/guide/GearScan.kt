package net.sbo.mod.diana.guide

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.profileviewer.DisplayItem
import net.sbo.mod.profileviewer.PlayerItems
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.TextUtil.removeFormatting
import net.sbo.mod.utils.game.ItemLookup
import net.sbo.mod.utils.game.World

/** Whether the player has the item of a guide slot. Unknown when the place it would be in could not be read. */
internal enum class SlotState { HAVE, MISSING, UNKNOWN }

/**
 * A guide slot compared with the items of the player.
 *
 * @property stack the own item of the player. Null when missing or when the profile item is not built yet
 * @property enchants the levels of the guide enchants on the item by enchant id. Null when the item is missing or unknown
 * @property heldItem the Skyblock id of the held item of the found pet
 * @property magicFind the Magic Find the item lore says it gives. Only for slots with [GuideSlot.loreMagicFind]
 */
internal class ScannedSlot(
    val slot: GuideSlot,
    val state: SlotState,
    val stack: ItemStack?,
    val enchants: Map<String, Int>?,
    val heldItem: String? = null,
    val magicFind: String? = null
)

/**
 * Finds the guide items on the player.
 *
 * Armor hotbar and inventory are read live from the player. Equipment and pets are not sent to the client.
 * They come from the profile items of the SBO backend ([PlayerItems]). Without them those slots are [SlotState.UNKNOWN].
 * Armor and weapons are also looked up in the wardrobe ender chest and backpacks of the profile items.
 * That way a stored item is not shown as missing. Island chests are not in the profile so an item in one counts as missing.
 */
internal object GearScan {
    private val PROFILE_GROUPS = setOf("Equipment", "Pets")

    /**
     * On SkyBlock armor and inventory are only read live. The profile copy of them can be minutes old and would still
     * have an item the player just put away. Off SkyBlock the lobby items mean nothing so the profile copy is used instead.
     */
    fun scan(slots: List<GuideSlot>, profile: PlayerItems?): List<ScannedSlot> {
        val onSkyblock = onSkyblock()
        val live = if (onSkyblock) liveItems() else emptyList()
        val stored = profile?.let { items ->
            val carried = if (onSkyblock) emptyList() else items.armor + items.inventory
            carried + items.equipment + items.enderChest + items.wardrobe.flatMap { it.orEmpty() } +
                items.backpacks.flatMap { it.items }
        }.orEmpty().filterNotNull()
        return slots.map { slot -> if (slot.pet != null) pet(slot, profile) else item(slot, live, stored, profile) }
    }

    private fun item(slot: GuideSlot, live: List<ItemStack>, stored: List<DisplayItem>, profile: PlayerItems?): ScannedSlot {
        live.firstOrNull { ItemLookup(it).sbId in slot.ids }?.let { stack ->
            val lookup = ItemLookup(stack)
            val lore = lookup.cleanLoreList
            return ScannedSlot(slot, SlotState.HAVE, stack, levels(slot, lore, lookup.enchantments), magicFind = magicFind(slot, lore))
        }
        stored.firstOrNull { sbId(it) in slot.ids }?.let { item ->
            val lore = item.lore.map { it.removeFormatting() }
            return ScannedSlot(slot, SlotState.HAVE, item.built?.stack(), levels(slot, lore, enchants(item)), magicFind = magicFind(slot, lore))
        }
        val unknown = profile == null && slot.group in PROFILE_GROUPS
        return ScannedSlot(slot, if (unknown) SlotState.UNKNOWN else SlotState.MISSING, null, null)
    }

    private fun pet(slot: GuideSlot, profile: PlayerItems?): ScannedSlot {
        val want = slot.pet ?: return ScannedSlot(slot, SlotState.UNKNOWN, null, null)
        profile ?: return ScannedSlot(slot, SlotState.UNKNOWN, null, null)
        val found = (listOfNotNull(profile.pet) + profile.pets).firstNotNullOfOrNull { item ->
            val info = petInfo(item) ?: return@firstNotNullOfOrNull null
            val held = info.text("heldItem")
            if (info.text("type") == want.type && info.text("tier") == want.tier && held?.contains(want.heldItem) == true) item to held else null
        }
        return if (found != null) ScannedSlot(slot, SlotState.HAVE, found.first.built?.stack(), null, found.second)
        else ScannedSlot(slot, SlotState.MISSING, null, null)
    }

    /**
     * The level of each guide enchant of the slot. The lore is read first because it is what the player sees.
     * The enchant ids Hypixel stores in the item data do not always match the name. They are only used when the lore has
     * no such line. Lore lines list enchants like "Growth VI" or "Looting VII, Prosecute VI, Scavenger VI".
     */
    private fun levels(slot: GuideSlot, lore: List<String>, byId: Map<String, Int>): Map<String, Int> {
        val text = lore.joinToString("\n")
        return slot.enchants.mapNotNull { enchant ->
            val pattern = Regex("(?:^|, )${Regex.escape(enchant.name)} ([IVXL]+)(?=\\s*(?:,|$))", RegexOption.MULTILINE)
            val level = pattern.find(text)?.groupValues?.get(1)?.let(::fromRoman) ?: byId[enchant.id]
            level?.let { enchant.id to it }
        }.toMap()
    }

    /**
     * The own Magic Find line of the item. That is "+9✯ Magic Find" under "Coins Consumed" on the Crown of Avarice and
     * "+45.6✯ Magic Find" under the Mythological bestiary tiers on the Daedalus Blade. Only lines that start with the
     * number count so "Grants +1✯ Magic Find against ..." is skipped. The symbol after the number changes with the
     * resource pack so any characters are allowed there.
     */
    private fun magicFind(slot: GuideSlot, lore: List<String>): String? {
        if (!slot.loreMagicFind) return null
        return lore.firstNotNullOfOrNull { LORE_MAGIC_FIND.find(it.trim())?.groupValues?.get(1) }
    }

    private val LORE_MAGIC_FIND = Regex("^\\+([0-9]*\\.?[0-9]+)\\S* Magic Find")

    private fun fromRoman(text: String): Int? {
        val values = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50)
        var total = 0
        text.forEachIndexed { i, c ->
            val value = values[c] ?: return null
            val after = text.getOrNull(i + 1)?.let { values[it] } ?: 0
            total += if (value < after) -value else value
        }
        return total.takeIf { it > 0 }
    }

    /**
     * On Hypixel the server type from the Hypixel location packet decides. [World.isInSkyblock] reads the scoreboard title
     * and can still say SkyBlock in a lobby. It is only used off Hypixel.
     */
    fun onSkyblock(): Boolean = if (HypixelModApi.isOnHypixel) HypixelModApi.isOnSkyblock else World.isInSkyblock()

    /** Worn armor first and then hotbar and inventory. That way the enchants of the worn piece are shown. */
    private fun liveItems(): List<ItemStack> {
        val player = mc.player ?: return emptyList()
        val armor = listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET).map { player.getItemBySlot(it) }
        val inventory = (0 until 36).map { player.inventory.getItem(it) }
        return (armor + inventory).filter { !it.isEmpty }
    }

    private fun sbId(item: DisplayItem): String? = (item.extraAttributes["id"] as? JsonPrimitive)?.content

    private fun enchants(item: DisplayItem): Map<String, Int> {
        val tag = item.extraAttributes["enchantments"] as? JsonObject ?: return emptyMap()
        return tag.mapNotNull { (id, level) -> (level as? JsonPrimitive)?.intOrNull?.let { id to it } }.toMap()
    }

    private fun petInfo(item: DisplayItem): JsonObject? {
        val text = (item.extraAttributes["petInfo"] as? JsonPrimitive)?.content ?: return null
        return runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()
    }

    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.content
}
