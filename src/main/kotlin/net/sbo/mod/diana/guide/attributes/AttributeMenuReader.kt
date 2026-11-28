package net.sbo.mod.diana.guide.attributes

import kotlinx.serialization.Serializable
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.guide.GuideData
import net.sbo.mod.partyfinder.OwnStats
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.utils.TextUtil.removeFormatting
import net.sbo.mod.utils.data.LocalStore
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.TickScheduler.sleep
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.game.ItemLookup

/**
 * An attribute as the Attribute Menu shows it.
 *
 * @property level the level from 0 to 10. Null when the item did not say it
 * @property toNext the shards the lore asks for until the next level or the unlock. Null when maxed or not said
 */
@Serializable
internal data class AttributeRead(val level: Int? = null, val toNext: Int? = null)

/**
 * What the Attribute Menu showed on one profile.
 *
 * @property levels guide attribute key to what the menu showed
 * @property pagesRead the page numbers read so far out of [pageCount]. They start over when the page count changes
 * because Advanced Mode shows more pages
 * @property pageCount the number of pages from the menu title like "(1/12) Attribute Menu"
 */
@Serializable
internal data class AttributeMenuState(
    val levels: Map<String, AttributeRead> = emptyMap(),
    val readAt: Long = 0,
    val pagesRead: Set<Int> = emptySet(),
    val pageCount: Int? = null
)

/**
 * Reads the attribute levels of the player from the Attribute Menu (/attributemenu). It is the live
 * fallback for the levels from the SBO profile. Every page that opens is read and added to the levels before.
 * An item belongs to a guide attribute when its name starts with the attribute name or its lore names the shard.
 * The longest matching name wins so "Attack Speed" is not read as "Speed".
 * The level is the number at the end of the name or a "Level" line in the lore.
 *
 * The read is saved per account and SkyBlock profile in .sbo/attribute-menu.json so it is still there after a restart.
 * The profile is the one Hypixel names in chat on every join. Before that the last loaded read is kept.
 */
internal object AttributeMenuReader {
    private const val MENU_ITEMS_DELAY_MS = 500L
    private const val MENU_TITLE = "Attribute Menu"
    private val PAGE = Regex("\\((\\d+)/(\\d+)\\)")
    private const val MAX_LEVEL = ATTRIBUTE_MAX_LEVEL
    private val SYPHONED = Regex("^\\+\\d+ (.+?) Attribute \\(Level (\\d+)\\)(?: - (\\d+) more to upgrade!)?")
    private val SYPHON = Regex("Syphon (\\d+) shards? to (level up|unlock)!")
    private val LEVEL_LINE = Regex("Level:? (\\d+)")
    private val NAME_LEVEL = Regex("\\s([IVX]+|\\d+)$")
    private val ROMAN = mapOf('I' to 1, 'V' to 5, 'X' to 10)

    private val store = LocalStore("attribute-menu.json")
    private var loadedKey: String? = null

    @Volatile
    private var state: AttributeMenuState? = null

    /** Guide attribute key to what the menu showed. Null until the menu was read on this profile. */
    val levels: Map<String, AttributeRead>? get() = current()?.levels

    val readAt: Long? get() = current()?.readAt

    val pagesRead: Set<Int> get() = current()?.pagesRead.orEmpty()

    /** Null until a page with a page number was read. */
    val pageCount: Int? get() = current()?.pageCount

    /** The account and profile the read belongs to. Null before Hypixel named the profile. */
    private fun key(): String? = OwnStats.profileId()?.let { "${OwnStats.uuid()}:${it.replace("-", "").lowercase()}" }

    /** Loads the saved read when the profile changed since the last load. */
    @Synchronized
    private fun current(): AttributeMenuState? {
        val key = key() ?: return state
        if (key == loadedKey) return state
        loadedKey = key
        state = store[key]?.let { text ->
            runCatching { PartyFinderApi.json.decodeFromString<AttributeMenuState>(text) }
                .onFailure { SBOKotlin.logger.warn("[SBO] Could not read the saved Attribute Menu", it) }
                .getOrNull()
        }
        return state
    }

    @Synchronized
    private fun save(next: AttributeMenuState) {
        state = next
        val key = key() ?: return
        runCatching { store[key] = PartyFinderApi.json.encodeToString(next) }
            .onFailure { SBOKotlin.logger.warn("[SBO] Could not save the Attribute Menu", it) }
    }

    /**
     * Syphoning a shard prints one line per attribute like "+1 Nature Elemental Attribute (Level 2) - 5 more to upgrade!".
     * The level and the shards to the next level from it are saved right away, so the guide is right without opening the
     * Attribute Menu again. A line without "more to upgrade" is a maxed attribute.
     */
    fun register() {
        Register.onChatMessage(SYPHONED, noFormatting = true) { _, match ->
            val name = match.groupValues[1].trim()
            val attr = GuideData.guide.attributes.flatMap { it.list }.firstOrNull { it.attribute.equals(name, ignoreCase = true) }
                ?: return@onChatMessage
            val level = match.groupValues[2].toIntOrNull() ?: return@onChatMessage
            val toNext = match.groupValues[3].toIntOrNull()?.takeIf { level < MAX_LEVEL }
            val before = current() ?: AttributeMenuState()
            save(
                before.copy(
                    levels = before.levels + (attr.key to AttributeRead(level.coerceIn(0, MAX_LEVEL), toNext)),
                    readAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** The items arrive a moment after the menu opens. */
    @SboEvent
    fun onMenuOpen(event: GuiOpenEvent) {
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        if (!screen.title.string.removeFormatting().contains(MENU_TITLE)) return
        sleep(MENU_ITEMS_DELAY_MS) { read(screen) }
    }

    private fun read(screen: AbstractContainerScreen<*>) {
        val own = mc.player?.inventory
        val items = screen.menu.slots.filter { it.container !== own && !it.item.isEmpty }.map { it.item }
        if (items.isEmpty()) return
        val attributes = GuideData.guide.attributes.flatMap { it.list }
        val found = HashMap<String, AttributeRead>()
        items.forEach { stack ->
            val name = stack.hoverName.string.removeFormatting().trim()
            val lore = ItemLookup(stack).cleanLoreList.map { it.trim() }
            val attr = attributes.filter { name.startsWith(it.attribute, ignoreCase = true) }.maxByOrNull { it.attribute.length }
                ?: attributes.firstOrNull { a -> lore.any { it.contains("${a.name} Shard", ignoreCase = true) } }
                ?: return@forEach
            parse(name.removePrefix(attr.attribute).trim(), lore)?.let { found[attr.key] = it }
        }
        if (found.isEmpty()) return
        val before = current() ?: AttributeMenuState()
        val page = PAGE.find(screen.title.string.removeFormatting())
        val number = page?.groupValues?.get(1)?.toInt()
        val count = page?.groupValues?.get(2)?.toInt() ?: before.pageCount
        val pages = if (count != before.pageCount) emptySet() else before.pagesRead
        save(
            before.copy(
                levels = before.levels + found,
                readAt = System.currentTimeMillis(),
                pagesRead = number?.let { pages + it } ?: pages,
                pageCount = count
            )
        )
    }

    /**
     * "to unlock" means level 0. A lore line with "MAX" and no shards to syphon means level 10.
     * Null when the item shows neither a level nor shards to syphon.
     */
    private fun parse(suffix: String, lore: List<String>): AttributeRead? {
        val syphon = lore.firstNotNullOfOrNull { SYPHON.find(it) }
        val toNext = syphon?.groupValues?.get(1)?.toIntOrNull()
        val level = when {
            syphon?.groupValues?.get(2) == "unlock" -> 0
            else -> NAME_LEVEL.find(" $suffix")?.groupValues?.get(1)?.let(::number)
                ?: lore.firstNotNullOfOrNull { LEVEL_LINE.find(it)?.groupValues?.get(1)?.toIntOrNull() }
                ?: if (syphon == null && lore.any { it.contains("MAX", ignoreCase = false) }) MAX_LEVEL else null
        }
        if (level == null && toNext == null) return null
        return AttributeRead(level?.coerceIn(0, MAX_LEVEL), toNext)
    }

    private fun number(text: String): Int? = text.toIntOrNull() ?: roman(text)

    private fun roman(text: String): Int? {
        if (text.any { it !in ROMAN }) return null
        var total = 0
        text.forEachIndexed { i, c ->
            val value = ROMAN.getValue(c)
            val next = text.getOrNull(i + 1)?.let { ROMAN.getValue(it) } ?: 0
            total += if (value < next) -value else value
        }
        return total
    }
}
