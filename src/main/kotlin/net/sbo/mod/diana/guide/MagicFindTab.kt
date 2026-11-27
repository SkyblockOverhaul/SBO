package net.sbo.mod.diana.guide

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.span
import net.sbo.mod.utils.game.ItemPrices
import java.util.Locale

/** Whether the player has a Magic Find source. Sometimes is for sources that only count some of the time. */
internal enum class MfState { HAVE, MISSING, UNKNOWN, SOMETIMES }

/**
 * One row of the Magic Find table.
 *
 * @property from where the state comes from: "/stats" or "Item" or "Guide"
 * @property value the read Magic Find when known and the guide value otherwise
 * @property price the price of the source when it is missing and can be bought
 */
internal data class MfRow(
    val name: String,
    val group: String,
    val from: String,
    val value: Double,
    val state: MfState,
    val price: Long?,
    val grind: Boolean
) {
    val perMf: Double? get() = price?.takeIf { value > 0 }?.let { it / value }
}

private enum class MfSort(val label: String, val firstDir: Int, val numeric: Boolean) {
    NAME("Source", 1, false), FROM("From", 1, false), MF("MF", -1, true),
    STATUS("Status", 1, false), PRICE("Price", 1, true), PER_MF("Per MF", 1, true)
}

private data class MfProps(val guide: DianaGuide, val scanned: List<ScannedSlot>, val now: Long)

internal fun NodeBuilder.magicFindTab(guide: DianaGuide, scanned: List<ScannedSlot>, now: Long) {
    MagicFindView(MfProps(guide, scanned, now))
}

/**
 * The Magic Find tab. Every source of the guide with whether the player has it, its price and coins per Magic Find.
 * Sources from the /stats pages that the guide does not list are added at the end. The header buttons sort the table.
 * The first click uses the useful direction, the second flips it and the third goes back to the groups.
 */
private val MagicFindView = component<MfProps>("DianaMagicFind") { (guide, scanned, now) ->
    var sort by useState<MfSort?>(null)
    var dir by useState(1)
    var missingOnly by useState(false)
    val rows = mfRows(guide, scanned).filter { !missingOnly || it.state == MfState.MISSING }
    val counted = rows.filter { it.state == MfState.HAVE }
    val total = StatsMenuReader.total ?: counted.sumOf { it.value }
    val readAt = StatsMenuReader.readAt

    div(className = "dg-toolbar") {
        span(className = "dg-stat") {
            span(className = "dg-stat-name") { +"Magic Find " }
            +"${number(total)} of ${number(guide.magicFind.max)} guide max"
        }
        span(className = "dg-hint dg-grow") {
            +when {
                readAt == null -> "Not read yet. Open Flat Bonuses in /stats once with Group By Category and Flatten Stats Menu off."
                !StatsMenuReader.complete -> "Read ${ago(now - readAt)}. Open Flat Bonuses too for every source."
                else -> "Read from /stats ${ago(now - readAt)}."
            }
        }
        button(
            disabled = !GearScan.onSkyblock(),
            title = if (GearScan.onSkyblock()) "Opens /stats and outlines where to click until Flat Bonuses" else "Join SkyBlock to open /stats",
            onClick = { MenuGuide.start(MenuGuide.MAGIC_FIND) }
        ) { +"Show Magic Find" }
    }
    div(className = "dg-toolbar") {
        button(className = if (!missingOnly) "dg-filter on" else "dg-filter", onClick = { missingOnly = false }) { +"All" }
        button(className = if (missingOnly) "dg-filter on" else "dg-filter", onClick = { missingOnly = true }) { +"Missing only" }
    }
    div(className = "dg-mf-row head") {
        MfSort.entries.forEach { key ->
            val active = sort == key
            button(className = "dg-sort" + (if (key.numeric) " num" else "") + (if (active) " on" else ""), key = key.name, onClick = {
                when {
                    sort != key -> {
                        sort = key
                        dir = key.firstDir
                    }
                    dir == key.firstDir -> dir = -dir
                    else -> sort = null
                }
            }) { +(key.label + if (active) (if (dir == 1) " ▲" else " ▼") else "") }
        }
    }
    val current = sort
    if (current == null) {
        rows.groupBy { it.group }.forEach { (group, list) ->
            div(className = "dg-section", key = "group:$group") {
                +group
                if (group == "Only sometimes") span(className = "dg-hint") { +"not in the total" }
            }
            list.forEach { mfRow(it) }
        }
    } else {
        sorted(rows, current, dir).forEach { mfRow(it) }
    }
    if (rows.isEmpty()) div(className = "dg-hint") { +"Nothing missing. Your Magic Find matches the guide." }
}

/** Rows without a value for the sorted column always go last. */
private fun sorted(rows: List<MfRow>, sort: MfSort, dir: Int): List<MfRow> {
    val key: (MfRow) -> Comparable<*>? = when (sort) {
        MfSort.NAME -> { r -> r.name.lowercase() }
        MfSort.FROM -> { r -> r.from }
        MfSort.MF -> { r -> r.value }
        MfSort.STATUS -> { r -> r.state.ordinal }
        MfSort.PRICE -> { r -> r.price }
        MfSort.PER_MF -> { r -> r.perMf }
    }
    val (known, unknown) = rows.partition { key(it) != null }
    @Suppress("UNCHECKED_CAST")
    val ordered = known.sortedWith { a, b -> (key(a) as Comparable<Any>).compareTo(key(b) as Any) * dir }
    return ordered + unknown
}

private fun NodeBuilder.mfRow(row: MfRow) {
    div(className = "dg-mf-row", key = "${row.group}:${row.name}") {
        span(className = "dg-name") { +row.name }
        span(className = "dg-hint") { +row.from }
        span(className = "dg-value") { +"+${number(row.value)}" }
        span(className = "dg-chip ${row.state.name.lowercase()}") {
            +when (row.state) {
                MfState.HAVE -> "✓ Have"
                MfState.MISSING -> "Missing"
                MfState.UNKNOWN -> "? Unknown"
                MfState.SOMETIMES -> "Reminder"
            }
        }
        span(className = "dg-value") {
            +when {
                row.state != MfState.MISSING -> "-"
                row.price != null -> short(row.price.toDouble())
                row.grind -> "Grind"
                else -> "No price"
            }
        }
        span(className = "dg-value") { +(row.perMf?.let(::short) ?: "-") }
    }
}

/**
 * The guide sources compared with what was read. A /stats source counts as missing only after the full Flat Bonuses
 * page was read. Before that it is unknown.
 */
internal fun mfRows(guide: DianaGuide, scanned: List<ScannedSlot>): List<MfRow> {
    val read = StatsMenuReader.sources
    val used = HashSet<String>()
    val rows = guide.magicFind.sources.map { source ->
        var value = source.value
        val (state, from) = when {
            source.stats != null -> {
                val names = source.stats.split("|")
                val hit = read?.entries?.firstOrNull { (name, _) ->
                    names.any { if (source.exact) name.equals(it, ignoreCase = true) else name.contains(it, ignoreCase = true) }
                }
                hit?.let {
                    used += it.key
                    value = it.value
                }
                when {
                    hit != null -> MfState.HAVE
                    source.sometimes -> MfState.SOMETIMES
                    read == null -> MfState.UNKNOWN
                    StatsMenuReader.complete -> MfState.MISSING
                    else -> MfState.UNKNOWN
                } to "/stats"
            }
            source.sometimes -> MfState.SOMETIMES to "Guide"
            source.slot != null -> {
                val slot = scanned.firstOrNull { it.slot.key == source.slot }
                if (source.enchant == null) slot?.magicFind?.toDoubleOrNull()?.let { value = it }
                val slotState = when {
                    slot == null || slot.state == SlotState.UNKNOWN -> MfState.UNKNOWN
                    slot.state == SlotState.MISSING -> MfState.MISSING
                    source.enchant != null -> if ((slot.enchants?.get(source.enchant) ?: 0) >= (source.level ?: 1)) MfState.HAVE else MfState.MISSING
                    else -> MfState.HAVE
                }
                slotState to "Item"
            }
            else -> MfState.UNKNOWN to "Not in /stats"
        }
        val price = source.price?.takeIf { state == MfState.MISSING }?.let { ItemPrices.getItemPrice(it).takeIf { p -> p > 0 } }
        MfRow(source.name, source.group, from, value, state, price, source.grind)
    }
    val others = read.orEmpty().filterKeys { it !in used }.map { (name, value) ->
        MfRow(name, "Other sources in /stats", "/stats", value, MfState.HAVE, null, false)
    }
    return rows + others
}

private fun number(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.ROOT, "%.1f", value)

private fun short(value: Double): String = when {
    value >= 1e9 -> String.format(Locale.ROOT, "%.2fB", value / 1e9)
    value >= 1e6 -> String.format(Locale.ROOT, "%.1fM", value / 1e6)
    value >= 1e3 -> String.format(Locale.ROOT, "%.1fk", value / 1e3)
    else -> value.toLong().toString()
}
