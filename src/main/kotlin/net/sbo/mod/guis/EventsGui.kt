package net.sbo.mod.guis

import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.UiScale
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import net.sbo.guilib.core.controls.ToastAction
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.modal
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.useClipboard
import net.sbo.guilib.core.dsl.useEscapeBack
import net.sbo.guilib.core.dsl.useToast
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.diana.DianaTracker
import net.sbo.mod.utils.data.configs.diana.DianaTrackerMayorData
import net.sbo.mod.utils.events.Register
import java.util.Locale
import kotlin.math.abs

/** All Diana events: the running one, the past ones and the total, each with details. */
object EventsGui {
    private val STYLES = listOf("sbo:ui/events/events.css", SboLook.STYLE)
    private val SORTS = linkedMapOf("year" to "Year", "profit" to "Profit", "profitPerHour" to "Profit/h", "chimeras" to "Chimeras")
    private const val TOTAL = "total"
    private const val CURRENT = "current"

    /** Opens the window. Must run on the client thread. */
    fun open() {
        GuiLib.open(App, STYLES, title = "SBO Events")
    }

    fun register() {
        Register.command("sboevents", "sboapastdianaevents", "sbopevents", "sbopastevents", "sbopde") { mc.schedule { open() } }
    }

    /** An event in the list, with the numbers the list sorts and marks by. */
    private class Entry(val key: String, val year: Int, val data: DianaTracker, val current: Boolean) {
        /** Profit split like [PARTS]: chimeras, sticks & relics, other drops, coins (0 when coins don't count). */
        val parts: List<Long> = DianaLoot.profitByItem(data).let { byItem ->
            val chimeras = byItem["CHIMERA"] ?: 0L
            val sticksRelics = (byItem["DAEDALUS_STICK"] ?: 0L) + (byItem["MINOS_RELIC"] ?: 0L)
            listOf(chimeras, sticksRelics, byItem.values.sum() - chimeras - sticksRelics, if (Diana.excludeCoinsFromProfit) 0L else data.items.COINS)
        }
        val profit = parts.sum()
        val profitPerHour = perHour(profit.toDouble(), data.items.TIME)
        val chimeras = data.items.CHIMERA + data.items.CHIMERA_LS
        val chimeraRate = (data.mobs.MINOS_INQUISITOR + data.mobs.MINOS_INQUISITOR_LS).takeIf { it > 0 }?.let { chimeras * 100.0 / it }
    }

    /** One line of the details: label (with § color), shown value and rate, and the numbers to compare with. */
    private data class Stat(val label: String, val value: String, val number: Double, val rate: String? = null, val rateNumber: Double? = null) {
        val compared get() = rateNumber ?: number
    }

    private val App = component("Events") {
        val data = DataManager.pastDianaEventsData
        var past by useState(data.events)
        var shown by useState<String?>(null)
        useEscapeBack(shown != null) { shown = null }
        var deleting by useState<DianaTrackerMayorData?>(null)
        var sort by useState(DataManager.sboData.eventsSort.takeIf { it in SORTS } ?: "year")
        var uiScale by useState(UiScale.own(DataManager.sboData.eventsUiScale))
        useSboScale(uiScale)
        useSboTheme()
        // The running event keeps counting while the window is open
        var tick by useState(0)
        useInterval(1000) { tick++ }
        val toast = useToast()
        val clipboard = useClipboard()

        val pastEntries = useMemo(past) { past.map { Entry(it.year.toString(), it.year, it, current = false) } }
        val mayor = DataManager.dianaTrackerMayorData
        val current = if (hasData(mayor) && past.none { it.year == mayor.year }) Entry(CURRENT, mayor.year, mayor, current = true) else null
        val sorted = when (sort) {
            "profit" -> pastEntries.sortedByDescending { it.profit }
            "profitPerHour" -> pastEntries.sortedByDescending { it.profitPerHour ?: -1.0 }
            "chimeras" -> pastEntries.sortedByDescending { it.chimeras }
            else -> pastEntries.sortedByDescending { it.year }
        }
        val order = listOfNotNull(current) + sorted
        val averages = useMemo(past) { averageStats(past) }

        // Best values among the past events, only worth a badge with more than one event
        fun best(of: (Entry) -> Double?): Entry? =
            pastEntries.takeIf { it.size > 1 }?.maxByOrNull { of(it) ?: -1.0 }?.takeIf { (of(it) ?: 0.0) > 0.0 }
        val bestProfit = best { it.profit.toDouble() }
        val bestPerHour = best { it.profitPerHour }
        val bestChimeras = best { it.chimeras.toDouble() }
        fun badges(e: Entry) = listOfNotNull(
            "Most profit".takeIf { e === bestProfit },
            "Best profit/h".takeIf { e === bestPerHour },
            "Most chimeras".takeIf { e === bestChimeras },
        )

        fun delete(event: DianaTrackerMayorData) {
            val before = data.events
            data.events = before.filterNot { it === event }
            DataManager.save(DataManager::pastDianaEventsData)
            past = data.events
            toast.info("The event of year ${event.year} was deleted.", title = "Events", durationMs = 8000, action = ToastAction("Undo") {
                if (data.events.none { it === event }) {
                    // Back at its old place, events after it may have been deleted meanwhile
                    data.events = before.filter { it === event || it in data.events }
                    DataManager.save(DataManager::pastDianaEventsData)
                    past = data.events
                }
            })
        }

        val detail: Entry? = when (shown) {
            null -> null
            TOTAL -> Entry(TOTAL, 0, DataManager.dianaTrackerTotalData, current = false)
            else -> order.firstOrNull { it.key == shown }
        }
        val index = detail?.let { d -> order.indexOfFirst { it.key == d.key } } ?: -1

        div(className = "ev-window") {
            header(className = "ev-header") {
                if (detail != null) button(className = "ev-back", title = "Back to the events", onClick = { shown = null }) { +"‹ Back" }
                span(className = "ev-title") {
                    +when {
                        detail == null -> "Events"
                        detail.key == TOTAL -> "Total Overview"
                        detail.current -> "Current Event · Year ${detail.year}"
                        else -> "Year ${detail.year}"
                    }
                }
                if (detail == null) span(className = "ev-count") { +"${past.size} past ${if (past.size == 1) "event" else "events"}" }
                if (index >= 0) {
                    button(className = "ev-nav", title = "Previous in the list", disabled = index == 0, onClick = { shown = order[index - 1].key }) { +"‹" }
                    button(className = "ev-nav", title = "Next in the list", disabled = index == order.lastIndex, onClick = { shown = order[index + 1].key }) { +"›" }
                }
                div(className = "ev-spacer")
                if (detail != null) button(className = "ev-copy", title = "Copies a short summary, e.g. to post it on Discord", onClick = {
                    clipboard.set(summaryText(detail))
                    toast.success("Summary copied.", title = "Events")
                }) { +"Copy" }
                select(value = UiScale.id(uiScale), onChange = { e ->
                    uiScale = UiScale.parse(e.value)
                    DataManager.sboData.eventsUiScale = uiScale
                    DataManager.save(DataManager::sboData)
                }, className = "ev-scale-select") {
                    UiScale.OWN_CHOICES.forEach { scale -> option(UiScale.id(scale), "Size: ${UiScale.label(scale)}") }
                }
                button(className = "ev-close", title = "Close", onClick = { GuiLib.close() }) { +"x" }
            }

            if (detail == null) {
                div(className = "ev-toolbar") {
                    span(className = "ev-toolbar-label") { +"Sort" }
                    segmented(value = sort, onChange = {
                        sort = it
                        DataManager.sboData.eventsSort = it
                        DataManager.save(DataManager::sboData)
                    }) {
                        SORTS.forEach { (id, label) -> option(id, label) }
                    }
                }
            }

            scroll(className = "ev-body guilib-autohide", key = "page:$shown") {
                if (detail != null) {
                    details(detail, averages.takeIf { detail.key != TOTAL && past.size > 1 })
                    return@scroll
                }
                EventsChart(ChartProps(
                    points = (pastEntries + listOfNotNull(current)).sortedBy { it.year }.map {
                        ChartPoint(it.year, it.profit, it.profitPerHour?.toLong(), it.chimeras, it.current, it.parts, it.chimeraRate)
                    },
                    metric = sort,
                    onSelect = { year -> shown = order.firstOrNull { it.year == year }?.key },
                ))
                div(className = "ev-card ev-total", key = TOTAL) {
                    div(className = "ev-card-head") {
                        span(className = "ev-year") { +"All events" }
                        div(className = "ev-spacer")
                        button(className = "primary", onClick = { shown = TOTAL }) { +"Total Overview" }
                    }
                    summary(Entry(TOTAL, 0, DataManager.dianaTrackerTotalData, current = false))
                }
                if (order.isEmpty()) div(className = "ev-empty") { +"No events recorded yet." }
                order.forEach { e ->
                    div(className = classNames("ev-card", "ev-current" to e.current), key = e.key) {
                        div(className = "ev-card-head") {
                            span(className = "ev-year") { +"Year ${e.year}" }
                            if (e.current) span(className = "ev-badge live", title = "Still running, the numbers update live") { +"Current" }
                            badges(e).forEach { span(className = "ev-badge") { +it } }
                            div(className = "ev-spacer")
                            div(className = "ev-buttons") {
                                button(onClick = { shown = e.key }) { +"Details" }
                                if (!e.current) button(className = "ev-danger", onClick = { deleting = e.data as DianaTrackerMayorData }) { +"Delete" }
                            }
                        }
                        summary(e)
                    }
                }
            }
        }

        modal(open = deleting != null, onClose = { deleting = null }, className = "ev-modal") {
            div(className = "ev-modal-title") { +"Delete the event of year ${deleting?.year}?" }
            div(className = "ev-modal-text") { +"You can undo it for a few seconds, the total stays." }
            div(className = "ev-buttons ev-modal-buttons") {
                button(onClick = { deleting = null }) { +"Cancel" }
                button(className = "ev-danger-filled", onClick = {
                    deleting?.let(::delete)
                    deleting = null
                }) { +"Delete" }
            }
        }
    }

    private fun hasData(data: DianaTracker) = data.items.TIME > 0 || data.items.TOTAL_BURROWS > 0 || data.mobs.TOTAL_MOBS > 0

    private fun perHour(value: Double, timeMs: Long): Double? = if (timeMs < 60_000) null else value / (timeMs / 3_600_000.0)

    private fun formatPerHour(value: Double?) = value?.let { "${Helper.formatNumber(it)}/h" } ?: "–"

    private fun NodeBuilder.summary(e: Entry) {
        val i = e.data.items
        div(className = "ev-summary") {
            stat("§ePlaytime", Helper.formatTime(i.TIME))
            stat("§6Total Profit", Helper.formatNumber(e.profit))
            stat("§6Profit/h", formatPerHour(e.profitPerHour))
            stat("§dChimeras", Helper.formatNumber(e.chimeras, true))
            stat("§cWools", Helper.formatNumber(i.SHIMMERING_WOOL + i.SHIMMERING_WOOL_LS, true))
            stat("§7Burrows/h", formatPerHour(perHour(i.TOTAL_BURROWS.toDouble(), i.TIME)))
            stat("§7Mobs/h", formatPerHour(perHour(e.data.mobs.TOTAL_MOBS.toDouble(), i.TIME)))
        }
    }

    private fun NodeBuilder.stat(label: String, value: String) {
        div(className = "ev-stat") {
            div(className = "ev-stat-label") { +label }
            div(className = "ev-stat-value") { +value }
        }
    }

    /** Items and mobs of an event; computed the same way for every event so the average can be taken per line. */
    private fun stats(data: DianaTracker, total: Boolean): Pair<List<Stat>, List<Stat>> {
        val i = data.items
        val m = data.mobs
        fun pct(count: Int, of: Int) = if (of <= 0) 0.0 else count * 100.0 / of
        fun count(label: String, value: Number, withCommas: Boolean = true) =
            Stat(label, Helper.formatNumber(value, withCommas), value.toDouble())
        fun drop(label: String, value: Int, of: Int) =
            Stat(label, Helper.formatNumber(value, true), value.toDouble(), "${Helper.calcPercentOne(value, of)}%", pct(value, of))
        val profit = DianaLoot.totalProfit(data)
        val items = buildList {
            add(count("§cDyes", i.MYTHOLOGICAL_DYE))
            add(count("§cMyth the Fishes", i.MYTH_THE_FISH))
            add(drop("§cWools", i.SHIMMERING_WOOL, m.KING_MINOS))
            add(drop("§cWools (LS)", i.SHIMMERING_WOOL_LS, m.KING_MINOS_LS))
            add(drop("§cManti-cores", i.MANTI_CORE, m.MANTICORE))
            add(drop("§cManti-cores (LS)", i.MANTI_CORE_LS, m.MANTICORE_LS))
            add(drop("§dChimeras", i.CHIMERA, m.MINOS_INQUISITOR))
            add(drop("§dChimeras (LS)", i.CHIMERA_LS, m.MINOS_INQUISITOR_LS))
            add(drop("§5Relics", i.MINOS_RELIC, m.MINOS_CHAMPION))
            add(drop("§6Sticks", i.DAEDALUS_STICK, m.MINOTAUR))
            add(count("§6Treasure", i.COINS - (i.FISH_COINS + i.SCAVENGER_COINS), false))
            add(count("§6Fish Coins", i.FISH_COINS, false))
            add(count("§6Scavenger", i.SCAVENGER_COINS, false))
            add(count("§6Feathers", i.GRIFFIN_FEATHER, false))
            add(count("§6Crowns", i.CROWN_OF_GREED))
            add(count("§6Souvenirs", i.WASHED_UP_SOUVENIR))
            add(count("§2Shelmets", i.DWARF_TURTLE_SHELMET))
            add(count("§2Remedies", i.ANTIQUE_REMEDIES))
            add(count("§2Plushies", i.CROCHET_TIGER_PLUSHIE))
            add(count("§7Claws", i.ANCIENT_CLAW, false))
            add(count("§7Ench. Claws", i.ENCHANTED_ANCIENT_CLAW))
            add(count("§7Ench. Gold", i.ENCHANTED_GOLD, false))
            add(count("§eBurrows", i.TOTAL_BURROWS, false))
            add(Stat("§ePlaytime", Helper.formatTime(i.TIME), i.TIME.toDouble()))
            if (total) add(Stat("§eEvents", DataManager.pastDianaEventsData.events.size.toString(), 0.0))
            add(Stat("§6Total Profit", Helper.formatNumber(profit), profit.toDouble()))
            val perHour = perHour(profit.toDouble(), i.TIME)
            add(Stat("§6Profit/h", formatPerHour(perHour), perHour ?: 0.0))
        }
        fun mob(label: String, value: Int) =
            Stat(label, Helper.formatNumber(value, true), value.toDouble(), "${Helper.calcPercentOne(value, m.TOTAL_MOBS)}%", pct(value, m.TOTAL_MOBS))
        val mobs = listOf(
            mob("§cKings", m.KING_MINOS),
            mob("§cKings (LS)", m.KING_MINOS_LS),
            mob("§cMantis", m.MANTICORE),
            mob("§cMantis (LS)", m.MANTICORE_LS),
            mob("§dInquisitors", m.MINOS_INQUISITOR),
            mob("§dInquisitors (LS)", m.MINOS_INQUISITOR_LS),
            mob("§dSphinxes", m.SPHINX),
            mob("§dSphinxes (LS)", m.SPHINX_LS),
            mob("§5Champions", m.MINOS_CHAMPION),
            mob("§6Minotaurs", m.MINOTAUR),
            mob("§2Gaias", m.GAIA_CONSTRUCT),
            mob("§2Harpy", m.HARPY),
            mob("§2Cretan Bulls", m.CRETAN_BULL),
            mob("§2Stranded Nymph", m.STRANDED_NYMPH),
            mob("§2Siamese", m.SIAMESE_LYNXES),
            mob("§2Hunters", m.MINOS_HUNTER),
            Stat("§eTotal Mobs", Helper.formatNumber(m.TOTAL_MOBS), m.TOTAL_MOBS.toDouble()),
        )
        return items to mobs
    }

    /** Per line the average over the past events (of the rate where there is one), or null without events. */
    private fun averageStats(events: List<DianaTracker>): Pair<List<Double>, List<Double>>? {
        if (events.isEmpty()) return null
        val all = events.map { stats(it, total = false) }
        fun avg(pick: (Pair<List<Stat>, List<Stat>>) -> List<Stat>): List<Double> =
            pick(all.first()).indices.map { n -> all.sumOf { pick(it)[n].compared } / all.size }
        return avg { it.first } to avg { it.second }
    }

    private fun NodeBuilder.details(e: Entry, averages: Pair<List<Double>, List<Double>>?) {
        val (items, mobs) = stats(e.data, total = e.key == TOTAL)
        if (averages != null) div(className = "ev-hint") { +"▲ / ▼ compare with the average of your past events (hover for the average)." }
        div(className = "ev-details") {
            statTable("Items", items, averages?.first)
            statTable("Mobs", mobs, averages?.second)
        }
    }

    private fun NodeBuilder.statTable(title: String, stats: List<Stat>, averages: List<Double>?) {
        div(className = "ev-table") {
            div(className = "ev-table-title") { +title }
            stats.forEachIndexed { n, s ->
                div(className = classNames("ev-row", "zero" to (s.number == 0.0))) {
                    span(className = "ev-row-label") { +s.label }
                    span(className = "ev-row-value") { +s.value }
                    span(className = "ev-row-percent") { +(s.rate ?: "") }
                    val avg = averages?.getOrNull(n)
                    val diff = if (avg == null) 0.0 else s.compared - avg
                    when {
                        avg == null || avg == 0.0 && s.compared == 0.0 || abs(diff) <= abs(avg) * 0.01 -> span(className = "ev-trend")
                        else -> span(
                            className = classNames("ev-trend", if (diff > 0) "up" else "down"),
                            title = "Average: " + if (s.rateNumber != null) String.format(Locale.ROOT, "%.2f%%", avg) else Helper.formatNumber(avg),
                        ) { +(if (diff > 0) "▲" else "▼") }
                    }
                }
            }
        }
    }

    /** A few lines to paste somewhere, without § codes. */
    private fun summaryText(e: Entry): String {
        val i = e.data.items
        val m = e.data.mobs
        val title = when {
            e.key == TOTAL -> "All Diana events (${DataManager.pastDianaEventsData.events.size})"
            e.current -> "Diana event, year ${e.year} (running)"
            else -> "Diana event, year ${e.year}"
        }
        return listOf(
            title,
            "Playtime: ${Helper.formatTime(i.TIME)} | Profit: ${Helper.formatNumber(e.profit)} (${formatPerHour(e.profitPerHour)})",
            "Chimeras: ${i.CHIMERA} + ${i.CHIMERA_LS} LS | Inquisitors: ${m.MINOS_INQUISITOR} + ${m.MINOS_INQUISITOR_LS} LS",
            "Sticks: ${i.DAEDALUS_STICK} | Relics: ${i.MINOS_RELIC} | Wools: ${i.SHIMMERING_WOOL + i.SHIMMERING_WOOL_LS} | Cores: ${i.MANTI_CORE + i.MANTI_CORE_LS}",
            "Burrows: ${Helper.formatNumber(i.TOTAL_BURROWS)} | Mobs: ${Helper.formatNumber(m.TOTAL_MOBS)}",
        ).joinToString("\n")
    }

}
