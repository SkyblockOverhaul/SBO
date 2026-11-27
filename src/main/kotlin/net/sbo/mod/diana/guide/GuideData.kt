package net.sbo.mod.diana.guide

import kotlinx.serialization.Serializable
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.profileviewer.BuiltItem
import net.sbo.mod.profileviewer.DisplayItemStacks

/** Who wrote the guide the window is built from. Shown under every tab. */
@Serializable
internal data class GuideCredit(
    val guide: String,
    val author: String,
    val support: String,
    val url: String,
    val discord: String,
    val questions: String
)

@Serializable
internal data class GuidePath(val steps: List<String>, val goal: String)

/**
 * An enchant the guide wants on the item of a slot.
 *
 * @property id the enchant as Hypixel stores it in the `enchantments` of the item
 * @property level the level the guide wants. Null when any level is fine
 */
@Serializable
internal data class GuideEnchant(val id: String, val name: String, val level: Int? = null, val ultimate: Boolean = false)

/**
 * The pet a pet slot wants.
 *
 * @property heldItem part of the id of the held item. "HEPHAESTUS" matches both the relic and the remedies
 */
@Serializable
internal data class GuidePet(val type: String, val tier: String, val heldItem: String)

/**
 * One slot of the Loadout tab.
 *
 * @property ids the Skyblock ids that count as having it. The first one is the one the guide means
 * @property icon the key in guide/items.json that is drawn when the player does not have it
 * @property badge a second small icon in the corner of the slot like the held item of a pet
 * @property price the Skyblock id that is priced when missing. The first id when null
 * @property loreMagicFind the lore of the owned item says how much Magic Find it gives. Shown instead of the first gain
 */
@Serializable
internal data class GuideSlot(
    val key: String,
    val group: String,
    val type: String,
    val name: String,
    val rarity: String,
    val ids: List<String> = emptyList(),
    val pet: GuidePet? = null,
    val icon: String,
    val badge: String? = null,
    val price: String? = null,
    val loreMagicFind: Boolean = false,
    val gains: List<String> = emptyList(),
    val why: String,
    val enchants: List<GuideEnchant> = emptyList()
) {
    val priceId: String? get() = price ?: ids.firstOrNull()
}

@Serializable
internal data class TrackingSource(val name: String, val value: String, val sometimes: Boolean = false)

@Serializable
internal data class TrackingScenario(val name: String, val value: String)

@Serializable
internal data class GuideTracking(val sources: List<TrackingSource>, val scenarios: List<TrackingScenario>)

/**
 * How a guide pet rule is found in the Autopet menu. Every text only has to be part of the line in the menu.
 *
 * @property condition part of the line under "When:"
 * @property except part of the line under "Except if:" or null when the rule needs none
 */
@Serializable
internal data class RuleMatch(val condition: String, val pet: String, val heldItem: String? = null, val except: String? = null)

/**
 * A pet rule of the guide.
 *
 * @property unless the exception of the rule shown in a small line under the trigger
 * @property icon the key in guide/items.json of the pet
 * @property badge the key in guide/items.json of the held item
 */
@Serializable
internal data class PetRule(
    val `when`: String,
    val unless: String? = null,
    val pet: String,
    val rarity: String,
    val why: String? = null,
    val icon: String? = null,
    val badge: String? = null,
    val match: RuleMatch? = null
)

@Serializable
internal data class PetRuleGroup(val title: String, val hint: String? = null, val rules: List<PetRule>)

@Serializable
internal data class GuideLink(val label: String, val url: String)

/** A paragraph of the Technique tab with an optional heading bullet list and video link. */
@Serializable
internal data class TechniquePart(
    val heading: String? = null,
    val text: String? = null,
    val bullets: List<String> = emptyList(),
    val link: GuideLink? = null
)

@Serializable
internal data class TechniqueSection(val title: String, val parts: List<TechniquePart>)

/**
 * A Magic Find source of the guide. It is checked in one of three ways: by its name in the /stats Magic Find pages
 * ([stats]) or by a Loadout slot ([slot]) or by an enchant on the item of a slot ([enchant]).
 *
 * @property mf the guide value as text like "9 to 10"
 * @property value the guide value as number for sorting and coins per Magic Find
 * @property stats part of the source name in the /stats pages like "Magic Find · Attribute". Several names are split
 * by "|". With [exact] the whole name has to match. Sources without it and without a slot can not be checked
 * @property price the Skyblock id that is priced when the source is missing
 * @property grind the source is earned by playing and not bought
 * @property sometimes the source only counts some of the time and is left out of the total
 */
@Serializable
internal data class MfSource(
    val name: String,
    val group: String,
    val mf: String,
    val value: Double,
    val stats: String? = null,
    val exact: Boolean = false,
    val slot: String? = null,
    val enchant: String? = null,
    val level: Int? = null,
    val price: String? = null,
    val grind: Boolean = false,
    val sometimes: Boolean = false
)

/** @property max the Magic Find the guide reaches in the menu outside Aatrox with a Looting VII weapon */
@Serializable
internal data class GuideMagicFind(val max: Double, val sources: List<MfSource>)

@Serializable
internal data class DianaGuide(
    val credit: GuideCredit,
    val path: GuidePath,
    val slots: List<GuideSlot>,
    val magicFind: GuideMagicFind,
    val tracking: GuideTracking,
    val petRules: List<PetRuleGroup>,
    val technique: List<TechniqueSection>
)

/** How an icon from guide/items.json looks. Same as assets/sbo/ui/stats/items.json plus the color of leather armor. */
@Serializable
internal data class GuideItemLook(
    val item: String,
    val model: String? = null,
    val texture: String? = null,
    val color: Int? = null,
    val glint: Boolean = false
)

/**
 * The guide content from assets/sbo/guide/diana.json and the item icons from assets/sbo/guide/items.json.
 *
 * The advice lives in the JSON and not in code. A guide update is a data change. The icons come from the Hypixel item
 * list (api.hypixel.net/v2/resources/skyblock/items). The `hypixel_skyblock:item/...` models only show with the Hypixel
 * resource pack. Without it the plain item is drawn.
 */
internal object GuideData {
    val guide: DianaGuide by lazy { read("/assets/sbo/guide/diana.json") }

    private val looks: Map<String, GuideItemLook> by lazy {
        runCatching { read<Map<String, GuideItemLook>>("/assets/sbo/guide/items.json") }
            .onFailure { SBOKotlin.logger.warn("[SBO] Could not read the Diana guide icons", it) }
            .getOrDefault(emptyMap())
    }

    private val built = HashMap<String, BuiltItem>()

    /** The icon of an item the player may not own. Null without a world or for an unknown key. */
    fun icon(key: String): ItemStack? {
        built[key]?.let { return it.stack() }
        val look = looks[key] ?: return null
        val item = DisplayItemStacks.icon(look.item, look.texture, look.model, look.glint, look.color) ?: return null
        built[key] = item
        return item.stack()
    }

    private inline fun <reified T> read(path: String): T {
        val text = GuideData::class.java.getResourceAsStream(path)!!.use { it.readBytes().decodeToString() }
        return PartyFinderApi.json.decodeFromString<T>(text)
    }
}
