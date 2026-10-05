package net.sbo.mod.guis

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.modal
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.span
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.data.DataManager
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow

/** One event in the chart; [profitPerHour] and [chimeraRate] are null when there is nothing to divide by. */
internal data class ChartPoint(
    val year: Int,
    val profit: Long,
    val profitPerHour: Long?,
    val chimeras: Int,
    val current: Boolean,
    /** Profit split into [EventsChart.PARTS], in that order. */
    val parts: List<Long>,
    /** Chimeras per inquisitor in percent (lootshare included). */
    val chimeraRate: Double?,
)

/** [points] oldest first; [metric] is the sort of the events list; [onSelect] opens the details of a year. */
internal data class ChartProps(
    val points: List<ChartPoint>,
    val metric: String,
    val onSelect: (Int) -> Unit,
)

private val VIEWS = linkedMapOf("value" to "Value", "split" to "Profit split", "lines" to "Lines", "luck" to "Luck")

/** Labels of the profit parts; the colors are `.ev-part-0` … in events.css. */
internal val PARTS = listOf("Chimeras", "Sticks & Relics", "Other drops", "Coins")

/** Bar chart of the events above the list: the sorted value, where the profit came from, or chimera luck. */
internal val EventsChart = component<ChartProps>("EventsChart") { (points, metric, onSelect) ->
    var view by useState(DataManager.sboData.eventsChartView.takeIf { it in VIEWS } ?: "value")
    var expanded by useState(false)

    fun NodeBuilder.chart(big: Boolean) = div(className = classNames("ev-chart", "big" to big)) {
        div(className = "ev-chart-head") {
            span(className = "ev-chart-title") {
                +when (view) {
                    "split" -> "Where the profit came from"
                    "lines" -> "Profit per event, total and by source"
                    "luck" -> "Chimera luck (chimeras per inquisitor vs. your average)"
                    else -> "${metricLabel(metric)} per event"
                }
            }
            segmented(value = view, onChange = {
                view = it
                DataManager.sboData.eventsChartView = it
                DataManager.save(DataManager::sboData)
            }, className = "ev-chart-views") {
                VIEWS.forEach { (id, label) -> option(id, label) }
            }
            if (big) {
                button(className = "ev-chart-expand", title = "Close the big view", onClick = { expanded = false }) { +"x" }
            } else {
                button(className = "ev-chart-expand", title = "Show the chart big", onClick = { expanded = true }) { +"Expand" }
            }
        }
        if (points.isEmpty()) {
            div(className = "ev-chart-empty") { +"No events yet." }
            return@div
        }
        when (view) {
            "split" -> splitChart(points) { year -> expanded = false; onSelect(year) }
            "lines" -> ProfitLines(LinesProps(points, { year -> expanded = false; onSelect(year) }, big))
            "luck" -> luckChart(points) { year -> expanded = false; onSelect(year) }
            else -> valueChart(points, metric) { year -> expanded = false; onSelect(year) }
        }
    }

    chart(big = false)
    modal(open = expanded, onClose = { expanded = false }, className = "ev-chart-modal") {
        chart(big = true)
    }
}

private fun NodeBuilder.yearLabels(points: List<ChartPoint>) {
    div(className = "ev-chart-labels") {
        points.forEach { p -> span(className = classNames("ev-chart-label", "current" to p.current), key = p.year) { +"${p.year}" } }
    }
}

private fun metricLabel(metric: String) = when (metric) {
    "profitPerHour" -> "Profit/h"
    "chimeras" -> "Chimeras"
    else -> "Profit"
}

private fun metricValue(p: ChartPoint, metric: String): Double? = when (metric) {
    "profitPerHour" -> p.profitPerHour?.toDouble()
    "chimeras" -> p.chimeras.toDouble()
    else -> p.profit.toDouble()
}

private fun format(value: Double, metric: String) = when (metric) {
    "chimeras" -> Helper.formatNumber(value.toInt())
    "profitPerHour" -> "${Helper.formatNumber(value)}/h"
    else -> Helper.formatNumber(value)
}

/** Height in percent of the plot, at least a sliver so a small value still shows. */
private fun height(value: Double, max: Double) = if (max <= 0.0 || value <= 0.0) 0.0 else maxOf(value / max * 100.0, 2.0)

private fun pct(value: Double) = String.format(Locale.ROOT, "%.2f%%", value)

private fun NodeBuilder.valueChart(points: List<ChartPoint>, metric: String, onSelect: (Int) -> Unit) {
    val values = points.map { metricValue(it, metric) }
    val max = values.maxOf { it ?: 0.0 }
    // The average and the best of the finished events, like the badges in the list
    val finished = points.indices.filter { !points[it].current && values[it] != null }
    val avg = if (finished.isEmpty()) null else finished.sumOf { values[it]!! } / finished.size
    val best = finished.maxByOrNull { values[it]!! }?.takeIf { finished.size > 1 }
    div(className = "ev-chart-plot") {
        if (avg != null && max > 0.0) {
            div(className = "ev-chart-avg", style = "bottom: ${pct(avg / max * 100.0)}", title = "Average: ${format(avg, metric)}")
        }
        points.forEachIndexed { i, p ->
            val v = values[i]
            val tip = "Year ${p.year}${if (p.current) " (running)" else ""}: ${v?.let { format(it, metric) } ?: "no playtime"}"
            div(className = "ev-chart-col", key = p.year, title = tip, onClick = { onSelect(p.year) }) {
                div(
                    className = classNames("ev-chart-bar", "current" to p.current, "best" to (i == best)),
                    style = "height: ${pct(height(v ?: 0.0, max))}",
                )
            }
        }
    }
    yearLabels(points)
}

private fun NodeBuilder.splitChart(points: List<ChartPoint>, onSelect: (Int) -> Unit) {
    val max = points.maxOf { p -> p.parts.sum().toDouble() }
    div(className = "ev-chart-plot") {
        points.forEach { p ->
            val total = p.parts.sum()
            val tip = buildString {
                append("Year ${p.year}: ${Helper.formatNumber(total)}")
                PARTS.forEachIndexed { n, label -> if (p.parts[n] > 0) append("\n$label: ${Helper.formatNumber(p.parts[n])}") }
            }
            div(className = "ev-chart-col", key = p.year, title = tip, onClick = { onSelect(p.year) }) {
                div(className = classNames("ev-chart-stack", "current" to p.current), style = "height: ${pct(height(total.toDouble(), max))}") {
                    // Bottom part first: column-reverse in the CSS; only the top part gets the round corners
                    val top = p.parts.indexOfLast { it > 0 }
                    p.parts.forEachIndexed { n, part ->
                        if (part > 0) div(className = classNames("ev-chart-part", "ev-part-$n", "top" to (n == top)), style = "flex-grow: ${part.toDouble() / total}")
                    }
                }
            }
        }
    }
    yearLabels(points)
    div(className = "ev-chart-legend") {
        PARTS.forEachIndexed { n, label ->
            span(className = "ev-chart-key") {
                span(className = "ev-chart-swatch ev-part-$n")
                +label
            }
        }
    }
}

private fun NodeBuilder.luckChart(points: List<ChartPoint>, onSelect: (Int) -> Unit) {
    val rated = points.filter { !it.current && it.chimeraRate != null }
    val avg = if (rated.isEmpty()) null else rated.sumOf { it.chimeraRate!! } / rated.size
    val maxDiff = points.maxOf { p -> if (avg == null || p.chimeraRate == null) 0.0 else abs(p.chimeraRate - avg) }
    div(className = "ev-chart-plot ev-chart-luck") {
        div(className = "ev-chart-zero", title = avg?.let { "Your average: ${pct(it)}" })
        points.forEach { p ->
            val rate = p.chimeraRate
            val diff = if (avg == null || rate == null) 0.0 else rate - avg
            val tip = "Year ${p.year}: " + if (rate == null) "no inquisitors" else "${pct(rate)} (average ${avg?.let(::pct) ?: "–"})"
            div(className = "ev-chart-col", key = p.year, title = tip, onClick = { onSelect(p.year) }) {
                if (rate != null && maxDiff > 0.0 && diff != 0.0) {
                    // Up from the middle line for luck, down for bad luck; the largest difference fills half the plot
                    val size = pct(maxOf(abs(diff) / maxDiff * 50.0, 1.0))
                    div(
                        className = classNames("ev-chart-luck-bar", if (diff > 0) "up" else "down", "current" to p.current),
                        style = if (diff > 0) "bottom: 50%; height: $size" else "top: 50%; height: $size",
                    )
                }
            }
        }
    }
    yearLabels(points)
}

private data class LinesProps(val points: List<ChartPoint>, val onSelect: (Int) -> Unit, val big: Boolean)

/** A line of the line chart: label, CSS class for its color and the value per point. */
private class Series(val id: String, val label: String, val colorClass: String, val values: List<Long>)

private const val MAX_ZOOM = 10f

/**
 * Line chart of the profit per event: the total and every part of [PARTS], each a line through one dot per event.
 * The lines are rotated divs, so the plot's size in px is measured after layout (and again when the window resizes).
 * In the big view the wheel zooms: the plot gets wider than its box (more space between the events) and scrolls
 * sideways (Shift + wheel or the scrollbar).
 */
private val ProfitLines = component<LinesProps>("EventsProfitLines") { (points, onSelect, big) ->
    val plot = useElementRef()
    val scroller = useElementRef()
    var size by useState(0f to 0f)
    var zoom by useState(1f)
    // Where the scroll should go once the plot has its new width: the plot fraction at the mouse and the mouse offset
    val anchor = useRef<Pair<Float, Float>?>(null)
    fun measure() {
        val box = plot.current?.box ?: return
        val now = box.width to box.height
        if (now != size) size = now
        val a = anchor.current
        val s = scroller.current
        if (a != null && s != null && box.width > 0f) {
            s.scrollLeft = a.first * box.width - a.second
            anchor.current = null
        }
    }
    useEffect { measure() }
    useInterval(if (big) 50 else 250) { measure() }
    var hidden by useState(DataManager.sboData.eventsHiddenLines.toSet())

    fun zoomTo(next: Float, mouseX: Float?) {
        val z = next.coerceIn(1f, MAX_ZOOM)
        if (z == zoom) return
        val s = scroller.current
        val w = plot.current?.box?.width ?: 0f
        if (s != null && w > 0f) {
            // Keep the point under the mouse (or the middle) where it is
            val offset = mouseX ?: (s.box.width / 2f)
            anchor.current = ((s.scrollLeft + offset) / w) to offset
        }
        zoom = z
    }

    val series = buildList {
        add(Series("total", "Total", "ev-sc-total", points.map { it.parts.sum() }))
        PARTS.forEachIndexed { n, label ->
            val values = points.map { it.parts[n] }
            if (values.any { it > 0 }) add(Series("part$n", label, "ev-sc-$n", values))
        }
    }
    val shown = series.filter { it.id !in hidden }
    val ticks = niceTicks(shown.maxOfOrNull { s -> s.values.max() } ?: 0L)
    val top = ticks.last().toDouble()
    val (w, h) = size
    fun tickY(t: Long) = if (top <= 0.0) 100.0 else (1.0 - t / top) * 100.0

    div(className = classNames("ev-lines", "big" to big)) {
        div(className = "ev-lines-bar") {
            if (big) {
                div(className = "ev-zoom") {
                    button(className = "ev-zoom-button", title = "Zoom out", disabled = zoom <= 1f, onClick = { zoomTo(zoom / 1.25f, null) }) { +"−" }
                    span(className = "ev-zoom-value", title = "Mouse wheel zooms, Shift + wheel scrolls") { +"${(zoom * 100).toInt()}%" }
                    button(className = "ev-zoom-button", title = "Zoom in", disabled = zoom >= MAX_ZOOM, onClick = { zoomTo(zoom * 1.25f, null) }) { +"+" }
                    button(className = "ev-zoom-button", title = "Reset the zoom", disabled = zoom == 1f, onClick = { zoomTo(1f, null) }) { +"Reset" }
                }
            }
            div(className = "ev-lines-legend") {
                series.forEach { s ->
                    span(
                        className = classNames("ev-lines-key", s.colorClass, "off" to (s.id in hidden)),
                        title = if (s.id in hidden) "Show ${s.label}" else "Hide ${s.label}",
                        onClick = {
                            hidden = if (s.id in hidden) hidden - s.id else hidden + s.id
                            DataManager.sboData.eventsHiddenLines = hidden.toMutableList()
                            DataManager.save(DataManager::sboData)
                        },
                    ) {
                        span(className = "ev-lines-dot-key")
                        +s.label
                    }
                }
            }
        }
        span(className = "ev-lines-axis-title") { +"Profit" }
        div(className = "ev-lines-area") {
            // The profit axis stays put while the plot scrolls
            div(className = "ev-lines-yaxis") {
                ticks.forEach { t -> span(className = "ev-lines-tick", style = "top: ${pct(tickY(t))}", key = "tick$t") { +Helper.formatNumber(t) } }
            }
            div(
                className = "ev-lines-scroll",
                ref = scroller,
                onWheel = { e ->
                    if (big && !e.shiftKey) {
                        e.preventDefault()
                        val rect = scroller.current?.getBoundingClientRect()
                        zoomTo(if (e.deltaY < 0) zoom * 1.15f else zoom / 1.15f, rect?.let { e.clientX - it.x })
                    }
                },
            ) {
                div(className = "ev-lines-inner", style = if (big) "width: ${pct(zoom * 100.0)}" else null) {
                    div(className = "ev-lines-plot", ref = plot) {
                        ticks.forEach { t -> div(className = "ev-lines-grid", style = "top: ${pct(tickY(t))}", key = "grid$t") }
                        if (w > 0f && h > 0f) {
                            fun x(i: Int) = w * (i + 0.5f) / points.size
                            fun y(v: Long) = if (top <= 0.0) h else (h * (1.0 - v / top)).toFloat()
                            shown.forEach { s ->
                                div(className = classNames("ev-lines-series", s.colorClass), key = s.id) {
                                    for (i in 0 until points.size - 1) {
                                        val x1 = x(i)
                                        val y1 = y(s.values[i])
                                        val dx = x(i + 1) - x1
                                        val dy = y(s.values[i + 1]) - y1
                                        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                                        div(
                                            className = classNames("ev-lines-segment", "current" to points[i + 1].current),
                                            style = "left: ${px(x1)}; top: ${px(y1 - 1f)}; width: ${px(hypot(dx, dy))}; " +
                                                "transform: rotate(${String.format(Locale.ROOT, "%.2f", angle)}deg)",
                                        )
                                    }
                                    points.forEachIndexed { i, p ->
                                        val v = s.values[i]
                                        div(
                                            className = classNames("ev-lines-dot", "current" to p.current),
                                            style = "left: ${px(x(i) - 2.5f)}; top: ${px(y(v) - 2.5f)}",
                                            title = "Year ${p.year}${if (p.current) " (running)" else ""} · ${s.label}: ${Helper.formatNumber(v)}",
                                            onClick = { onSelect(p.year) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    div(className = "ev-lines-years") {
                        points.forEach { p -> span(className = classNames("ev-chart-label", "current" to p.current), key = p.year) { +"${p.year}" } }
                    }
                }
            }
        }
    }
}

private fun px(v: Float) = String.format(Locale.ROOT, "%.2fpx", v)

/** 0 and about 4 round steps up to at least [max] (1, 2, 2.5 or 5 times a power of ten). */
private fun niceTicks(max: Long): List<Long> {
    if (max <= 0L) return listOf(0L, 1L)
    val raw = max / 4.0
    val power = 10.0.pow(floor(log10(raw)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * power >= raw } * power
    val steps = ceil(max / step).toInt()
    return (0..steps).map { (it * step).toLong() }
}
