package net.sbo.mod.diana.guide.attributes

import java.util.Locale
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.span
import net.sbo.mod.diana.guide.DianaGuide
import net.sbo.mod.diana.guide.GearScan
import net.sbo.mod.diana.guide.GuideAttribute
import net.sbo.mod.diana.guide.GuideGui
import net.sbo.mod.diana.guide.ago
import net.sbo.mod.diana.guide.menus.MenuGuide
import net.sbo.mod.utils.game.ItemPrices

/** The highest attribute level. */
internal const val ATTRIBUTE_MAX_LEVEL = 10

/**
 * Shards per attribute level by shard rarity. The entry at index i is what level i + 1 takes. From the wiki and the
 * NEU repo. A full attribute takes 96 common, 64 uncommon, 48 rare, 32 epic or 24 legendary shards.
 */
internal val ATTRIBUTE_SHARDS: Map<String, List<Int>> = mapOf(
    "common" to listOf(1, 3, 5, 6, 7, 8, 10, 14, 18, 24),
    "uncommon" to listOf(1, 2, 3, 4, 5, 6, 7, 8, 12, 16),
    "rare" to listOf(1, 2, 3, 3, 4, 4, 5, 6, 8, 12),
    "epic" to listOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 7),
    "legendary" to listOf(1, 1, 1, 2, 2, 2, 3, 3, 4, 5),
)

/**
 * One card of the Attributes tab.
 *
 * @property group the title of the guide group the attribute is in
 * @property level the level from 0 to 10. Null when neither the profile nor the Attribute Menu has it
 * @property from "Menu" when read live from the Attribute Menu and "Profile" when it comes from the SBO profile
 * @property next the shards still needed for the next level. 0 when maxed
 * @property shards the shards still needed for level 10. Exact after a menu read. From the profile it assumes no
 * shards were syphoned into the next level yet
 * @property price the bazaar price of one shard. Null without a price
 */
internal data class AttrRow(
    val group: String,
    val attr: GuideAttribute,
    val level: Int?,
    val from: String?,
    val next: Int?,
    val shards: Int?,
    val price: Long?
) {
    val maxed: Boolean get() = level == ATTRIBUTE_MAX_LEVEL
    val coins: Long? get() = if (shards != null && price != null) shards * price else null
}

private enum class AttrFilter(val label: String) { ALL("All"), MUST("Must"), SHOULD("Should"), CAN("Can"), NOT_MAXED("Not maxed") }

private data class AttrProps(val guide: DianaGuide, val profileLevels: Map<String, Int>?, val now: Long)

internal fun NodeBuilder.attributesTab(guide: DianaGuide, profileLevels: Map<String, Int>?, now: Long) {
    AttributesView(AttrProps(guide, profileLevels, now))
}

/**
 * The Attributes tab. One card per guide attribute with its level and the shards and coins still needed for level 10.
 * A click on a card buys its shard on the bazaar. The levels come from the SBO profile. A read of /attributemenu is newer and wins.
 */
private val AttributesView = component<AttrProps>("DianaAttributes") { (guide, profileLevels, now) ->
    var filter by useState(AttrFilter.ALL)
    val all = attrRows(guide, profileLevels)
    val rows = all.filter { row ->
        when (filter) {
            AttrFilter.ALL -> true
            AttrFilter.MUST -> row.attr.priority == "must"
            AttrFilter.SHOULD -> row.attr.priority == "should"
            AttrFilter.CAN -> row.attr.priority == "can"
            AttrFilter.NOT_MAXED -> !row.maxed
        }
    }
    val known = all.any { it.level != null }
    val readAt = AttributeMenuReader.readAt
    val mustCoins = all.filter { it.attr.priority == "must" }.mapNotNull { it.coins }.sum()

    if (!known) {
        div(className = "dg-banner") {
            span(className = "dg-banner-text") {
                +"Attribute levels are not loaded. They come from your SBO profile. For a live read go through every page of /attributemenu."
            }
        }
    }
    div(className = "dg-toolbar") {
        span(className = "dg-stat") {
            span(className = "dg-stat-name") { +"Maxed " }
            +"${all.count { it.maxed }} of ${all.size}"
        }
        if (mustCoins > 0) span(className = "dg-stat") {
            span(className = "dg-stat-name") { +"Must to max " }
            +shortCoins(mustCoins.toDouble())
        }
        span(className = "dg-hint dg-grow") {
            +if (readAt == null) "Levels from your SBO profile." else menuReadText(now - readAt)
        }
        button(
            disabled = !GearScan.onSkyblock(),
            title = if (GearScan.onSkyblock()) "Opens /attributemenu and outlines Advanced Mode and Next Page until every page was read" else "Join SkyBlock to open the Attribute Menu",
            onClick = { MenuGuide.start(MenuGuide.ATTRIBUTES) { GuideGui.open("attributes") } }
        ) { +"Show Attribute Menu" }
    }
    div(className = "dg-toolbar") {
        AttrFilter.entries.forEach { entry ->
            button(className = if (filter == entry) "dg-filter on" else "dg-filter", key = entry.name, onClick = { filter = entry }) { +entry.label }
        }
    }
    val notes = guide.attributes.associate { it.title to it.note }
    rows.groupBy { it.group }.forEach { (group, list) ->
        div(className = "dg-section", key = "group:$group") {
            +group
            notes[group]?.let { span(className = "dg-hint") { +it } }
        }
        div(className = "dg-attr-grid", key = "grid:$group") {
            list.forEach { attrCard(it) }
        }
    }
    if (rows.isEmpty()) div(className = "dg-hint") { +"Every attribute here is maxed." }
}

/** How long ago the Attribute Menu was read and how many of its pages. */
private fun menuReadText(elapsed: Long): String {
    val count = AttributeMenuReader.pageCount ?: return "Read from the Attribute Menu ${ago(elapsed)}."
    val read = AttributeMenuReader.pagesRead.size
    val rest = if (read < count) " Turn the other pages for every attribute." else ""
    return "Read $read of $count pages of the Attribute Menu ${ago(elapsed)}.$rest"
}

/**
 * A card with the level and what is left to buy. A card that can still be bought on the bazaar is a button that
 * searches the bazaar for its shard, see [AttributeShopping].
 */
private fun NodeBuilder.attrCard(row: AttrRow) {
    val level = row.level
    val state = when {
        level == null -> "unknown"
        row.maxed -> "max"
        level == 0 -> "missing"
        else -> "part"
    }
    val buyable = row.attr.bazaar != null && !row.maxed && GearScan.onSkyblock()
    div(
        className = "dg-attr $state" + if (buyable) " buy" else "",
        key = row.attr.key,
        title = if (buyable) "Search the bazaar for ${row.attr.name} Shards" else row.attr.attribute,
        onClick = if (buyable) ({ AttributeShopping.start(row.attr) }) else null
    ) {
        div(className = "dg-attr-head") {
            span(className = "dg-attr-name dg-r-${row.attr.rarity}") { +row.attr.name }
            span(className = "dg-chip ${row.attr.priority}") {
                +when (row.attr.priority) {
                    "must" -> "Must"
                    "should" -> "Should"
                    else -> "Can"
                }
            }
        }
        div(className = "dg-attr-effect") { +row.attr.effect }
        div(className = "dg-attr-foot") {
            span(className = "dg-attr-lvl") {
                +when {
                    level == null -> "? Unknown"
                    row.maxed -> "Maxed"
                    level == 0 -> "Not unlocked"
                    else -> "Level $level/$ATTRIBUTE_MAX_LEVEL"
                }
            }
            span(className = if (row.maxed) "dg-attr-need done" else "dg-attr-need") {
                +when {
                    row.maxed -> "✓"
                    row.shards == null -> ""
                    row.attr.bazaar == null -> "${need(row)} · not on bazaar"
                    else -> need(row) + (row.coins?.let { " · ${shortCoins(it.toDouble())}" } ?: "")
                }
            }
            if (buyable) span(className = "dg-attr-buy") { +"Buy ›" }
        }
    }
}

/** Shards to the next level and to level 10 like "2 next · 16 max". Just the max when the next level is the last. */
private fun need(row: AttrRow): String =
    if (row.next == row.shards) "${row.shards} shards" else "${row.next} next · ${row.shards} max"

/** The guide attributes with the level from the Attribute Menu when it was read and the profile level otherwise. */
internal fun attrRows(guide: DianaGuide, profileLevels: Map<String, Int>?): List<AttrRow> {
    val menu = AttributeMenuReader.levels
    return guide.attributes.flatMap { group -> group.list.map { group.title to it } }.map { (group, attr) ->
        val read = menu?.get(attr.key)
        val profile = profileLevels?.get(attr.key)
        val (level, from) = when {
            read?.level != null -> read.level to "Menu"
            profile != null -> profile to "Profile"
            else -> null to null
        }
        val steps = ATTRIBUTE_SHARDS[attr.rarity].orEmpty()
        val next = level?.let { lvl ->
            if (lvl >= ATTRIBUTE_MAX_LEVEL || steps.size < ATTRIBUTE_MAX_LEVEL) 0
            else read?.toNext?.takeIf { from == "Menu" } ?: steps[lvl]
        }
        val shards = level?.let { lvl -> (next ?: 0) + steps.drop(lvl + 1).sum() }
        val price = attr.bazaar?.let { ItemPrices.getItemPrice(it) }?.takeIf { it > 0 }
        AttrRow(group, attr, level, from, next, shards, price)
    }
}

/** Coins like "1.3M" or "400.0k". */
internal fun shortCoins(value: Double): String = when {
    value >= 1e9 -> String.format(Locale.ROOT, "%.2fB", value / 1e9)
    value >= 1e6 -> String.format(Locale.ROOT, "%.1fM", value / 1e6)
    value >= 1e3 -> String.format(Locale.ROOT, "%.1fk", value / 1e3)
    else -> value.toLong().toString()
}
