package net.sbo.mod.test

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.*
import net.sbo.guilib.core.paint.PaintCommand
import net.sbo.mod.guis.niceTicks
import java.nio.file.Files
import java.util.Locale
import kotlin.math.*

/** Synthetic event history, source-derived table labels and source-matched chart DOM.
 * Tick generation is production's pure function. Prices/totals/chart math are specimens;
 * the live Events/Chart components and DataManager are never initialized/rendered.
 */
internal class EventsReviewFixture(css: String = StandaloneReviewFrame.css("events"), override val width: Float = 640f,
    override val height: Float = 480f, theme: String = "sbo-dark", page: String = "list", initialChart: String = "value", expanded: Boolean = false) : ReviewFrame {
    companion object {
        val out get() = ReviewResources.project.resolve("build/ui-review/events")
        private val source = Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/guis/EventsGui.kt"))
        val summaryLabels = Regex("stat\\(\"([^\"]+)\"").findAll(source.substringAfter("private fun NodeBuilder.summary").substringBefore("private fun NodeBuilder.stat")).map { it.groupValues[1] }.toList()
        val itemLabels = Regex("(?:count|drop|Stat)\\(\"([^\"]+)\"").findAll(source.substringAfter("val items = buildList").substringBefore("fun mob(")).map { it.groupValues[1] }.toList()
        val mobLabels = Regex("(?:mob|Stat)\\(\"([^\"]+)\"").findAll(source.substringAfter("val mobs = listOf").substringBefore("return items to mobs")).map { it.groupValues[1] }.toList()
        val rateLabels = Regex("drop\\(\"([^\"]+)\"").findAll(source.substringAfter("val items = buildList").substringBefore("fun mob(")).map { it.groupValues[1] }.toSet()
        val profits = listOf(150L, 550L, 0L, 950L, 630L).map { it * 1_000_000 }
        val years = listOf(386, 387, 388, 389, 390)
        private fun pct(v: Double) = String.format(Locale.ROOT, "%.2f%%", v)
        private fun n(v: Long) = if (v >= 1_000_000_000) "${v/1_000_000_000.0}B" else "${v/1_000_000}M"
    }
    var selections = 0; private set
    var deletes = 0; private set
    var chartMode = initialChart; private set
    private var resize: (Pair<Float,Float>) -> Unit = {}
    private val app = component("Offline Events specimens") {
        var shown by useState(page.takeIf { it in listOf("details", "total", "current") })
        var mode by useState(initialChart)
        var big by useState(expanded)
        var deleting by useState(page == "delete")
        var sort by useState("year")
        var size by useState("global")
        var zoom by useState(1f)
        var hidden by useState(emptySet<Int>())
        var plotSize by useState(0f to 0f)
        resize = { plotSize = it }; chartMode = mode
        fun selected() { selections++; shown = "details"; big = false }
        fun NodeBuilder.chart(large: Boolean) {
            div(className = classNames("ev-chart", "big" to large)) {
                div(className = "ev-chart-head") {
                    span(className = "ev-chart-title") { +when(mode) { "split" -> "Where the profit came from"; "lines" -> "Profit per event, total and by source"; "luck" -> "Chimera luck (chimeras per inquisitor vs. your average)"; else -> "Profit per event" } }
                    button(className = if (large) "sbo-close" else "sbo-button ev-chart-expand", onClick = { big = !large }) { +(if (large) "×" else "Expand") }
                }
                segmented(value = mode, onChange = { mode = it }, className = "ev-chart-views") { option("value", "Value"); option("split", "Profit split"); option("lines", "Lines"); option("luck", "Luck") }
                if (page == "empty") div(className = "ev-chart-empty") { +"No events yet." }
                else if (mode == "lines") div(className = classNames("ev-lines", "big" to large)) {
                    div(className = "ev-lines-bar") {
                        if (large) div(className = "ev-zoom") {
                            button(className = "sbo-button ev-zoom-button", disabled = zoom <= 1, onClick = { zoom = max(1f,zoom/1.25f) }) { +"−" }
                            span(className = "ev-zoom-value") { +"${(zoom*100).toInt()}%" }
                            button(className = "sbo-button ev-zoom-button", onClick = { zoom = min(10f,zoom*1.25f) }) { +"+" }
                            button(className = "sbo-button ev-zoom-button", disabled = zoom == 1f, onClick = { zoom = 1f }) { +"Reset" }
                        }
                        div(className = "ev-lines-legend") { listOf("Total", "Chimeras", "Sticks & Relics", "Other drops", "Coins").forEachIndexed { i, label ->
                            span(className = classNames("ev-lines-key", if(i==0) "ev-sc-total" else "ev-sc-${i-1}", "off" to (i in hidden)), onClick = { hidden = if(i in hidden) hidden-i else hidden+i }) { span(className = "ev-lines-dot-key"); +label }
                        } }
                    }
                    val ticks = niceTicks(profits.max()); val top = ticks.last().toDouble()
                    span(className = "ev-lines-axis-title") { +"Profit" }
                    div(className = "ev-lines-area") {
                        div(className = "ev-lines-yaxis") { ticks.forEach { t -> span(className = "ev-lines-tick", style = "top:${pct((1-t/top)*100)}") { +n(t) } } }
                        div(className = "ev-lines-scroll") { div(className = "ev-lines-inner", style = if(large) "width:${pct(zoom*100.0)}" else null) {
                            div(className = "ev-lines-plot") {
                                ticks.forEach { t -> div(className = "ev-lines-grid", style = "top:${pct((1-t/top)*100)}") }
                                if(plotSize.first>0) for (series in 0..4) if(series !in hidden) div(className = "ev-lines-series " + if(series==0) "ev-sc-total" else "ev-sc-${series-1}") {
                                    val factor = listOf(1.0,.55,.25,.15,.05)[series]
                                    val points = profits.mapIndexed { i,v -> (plotSize.first*(i+.5f)/profits.size) to (plotSize.second*(1-v*factor/top)).toFloat() }
                                    points.zipWithNext().forEachIndexed { i,(a,b) ->
                                        val dx=b.first-a.first; val dy=b.second-a.second
                                        div(className = classNames("ev-lines-segment", "current" to (i==3)), style = "left:${a.first}px;top:${a.second-1}px;width:${hypot(dx,dy)}px;transform:rotate(${Math.toDegrees(atan2(dy.toDouble(),dx.toDouble()))}deg)")
                                    }
                                    points.forEachIndexed { i,p -> div(className = classNames("ev-lines-dot", "current" to (i==4)), style = "left:${p.first-2.5}px;top:${p.second-2.5}px;", title = "Year ${years[i]}: ${n(profits[i])}", onClick = { selected() }) }
                                }
                            }
                            div(className = "ev-lines-years") { years.forEach { span(className = "ev-chart-label") { +it.toString() } } }
                        } }
                    }
                } else {
                    div(className = classNames("ev-chart-plot", "ev-chart-luck" to (mode=="luck"))) {
                        if(mode=="value") div(className = "ev-chart-avg", style = "bottom:43.42%;", title = "Average: 412.5M")
                        if(mode=="luck") div(className = "ev-chart-zero", title = "Your average: 1.88%")
                        profits.forEachIndexed { i,v -> div(className = "ev-chart-col", title = "Year ${years[i]}: ${n(v)}", onClick = { selected() }) {
                            when(mode) {
                                "split" -> div(className = classNames("ev-chart-stack", "current" to (i==4)), style = "height:${pct(v/950_000_000.0*100)}") { listOf(.55,.25,.15,.05).forEachIndexed { p,f -> div(className = "ev-chart-part ev-part-$p" + if(p==3) " top" else "", style = "flex-grow:$f;") } }
                                "luck" -> { val diff=listOf(-.4,1.6,-1.5,.3,.7)[i]; div(className = "ev-chart-luck-bar " + if(diff>0) "up" else "down", style = "${if(diff>0) "bottom" else "top"}:50%;height:${pct(abs(diff)/1.6*50)}") }
                                else -> div(className = classNames("ev-chart-bar", "best" to (i==3), "current" to (i==4)), style = "height:${pct(v/950_000_000.0*100)}")
                            }
                        } }
                    }
                    div(className = "ev-chart-labels") { years.forEachIndexed { i,y -> span(className = classNames("ev-chart-label", "current" to (i==4))) { +y.toString() } } }
                    if(mode=="split") div(className = "ev-chart-legend") { listOf("Chimeras", "Sticks & Relics", "Other drops", "Coins").forEachIndexed { i,l -> span(className = "ev-chart-key") { span(className = "ev-chart-swatch ev-part-$i"); +l } } }
                }
            }
        }
        div(className = "ev-window") {
            header(className = "ev-header") {
                div(className = "ev-heading") {
                    if(shown!=null) button(className = "sbo-button ev-back", onClick = { shown=null }) { +"‹ Back" }
                    span(className = "ev-title") { +when(shown) { "total"->"Total Overview"; "current"->"Current Event · Year 390"; "details"->"Year 389"; else->"Events" } }
                    if(shown==null) span(className = "ev-count") { +"4 past events" }
                    else if(shown!="total") { button(className = "sbo-button ev-nav", disabled = shown=="current") { +"‹" }; button(className = "sbo-button ev-nav") { +"›" } }
                }
                div(className = "ev-spacer")
                div(className = "ev-header-actions") { if(shown!=null) button(className = "sbo-button ev-copy", onClick = { selections++ }) { +"Copy" }
                    select(value=size,onChange={size=it.value},className="ev-scale-select") { option("global","Size: Global");option("auto","Size: Auto");option("2","Size: 2") }
                    button(className="sbo-close") { +"×" }
                }
            }
            if(shown==null) div(className="ev-toolbar") { span(className="ev-toolbar-label") { +"Sort" };segmented(value=sort,onChange={sort=it}) { option("year","Year");option("profit","Profit");option("profitPerHour","Profit/h");option("chimeras","Chimeras") } }
            scroll(className="ev-body guilib-autohide") {
                if(shown!=null) {
                    if(shown!="total") div(className="ev-hint") { +"▲ / ▼ compare with the average of your past events (hover for the average)." }
                    div(className="ev-details") { table("Items",itemLabels,shown=="total");table("Mobs",mobLabels,shown=="total") }
                } else {
                    chart(false)
                    div(className="ev-card ev-total") { div(className="ev-card-head") { span(className="ev-year") { +"All events" };div(className="ev-spacer");button(className="sbo-button primary",onClick={shown="total"}) { +"Total Overview" } };summary() }
                    if(page=="empty") div(className="ev-empty") { +"No events recorded yet." } else for(i in 4 downTo 0) div(className=classNames("ev-card","ev-current" to (i==4))) {
                        div(className="ev-card-head") { span(className="ev-year") { +"Year ${years[i]}" };if(i==4)span(className="ev-badge live") { +"Current" };if(i==3)span(className="ev-badge") { +"Most profit" };div(className="ev-spacer");div(className="ev-buttons") { button(className="sbo-button",onClick={shown=if(i==4)"current" else "details"}) { +"Details" };if(i!=4)button(className="sbo-button ev-danger",onClick={deleting=true}) { +"Delete" } } }
                        summary(profits[i])
                    }
                }
            }
        }
        modal(open=deleting,onClose={deleting=false},className="ev-modal") { div(className="ev-modal-title") { +"Delete the event of year 389?" };div(className="ev-modal-text") { +"You can undo it for a few seconds, the total stays." };div(className="ev-buttons ev-modal-buttons") { button(className="sbo-button",id="cancel-delete",onClick={deleting=false}) { +"Cancel" };button(className="sbo-button ev-danger-filled",id="confirm-delete",onClick={deletes++;deleting=false}) { +"Delete" } } }
        modal(open=big,onClose={big=false},className="ev-chart-modal") { chart(true) }
    }
    private fun NodeBuilder.summary(profit:Long=profits.sum()) {
        val total=profit==profits.sum()
        div(className="ev-summary") { summaryLabels.zip(listOf(if(total)"62h 50m"else"12h 34m",n(profit),String.format(Locale.ROOT,"%.1fM/h",profit/1_000_000.0/(if(total)62.83 else 12.56)),"1,234","82","543/h","317/h")).forEach { (l,v) ->
            div(className="ev-stat") { div(className="ev-stat-label") { +l };div(className="ev-stat-value") { +v } }
        } }
    }
    private fun NodeBuilder.table(title:String,labels:List<String>,total:Boolean) {
        div(className="ev-table") {
            div(className="ev-table-title") { +title }
            labels.filter{total||it!="§eEvents"}.forEachIndexed { i,l ->
                val v=when { l.contains("Playtime")->"123h 45m";l.contains("Profit/h")->"100.2M/h";l.contains("Profit")->"12.34B";else->listOf("0","1,234","57","123,456")[i%4] }
                val rated=l in rateLabels || (title=="Mobs" && !l.contains("Total Mobs"))
                div(className=classNames("ev-row","zero" to (v=="0"))) {
                    span(className="ev-row-label") { +l };span(className="ev-row-value") { +v }
                    span(className="ev-row-percent") { +(if(!rated)""else if(v=="0")"0.00%"else"12.34%") }
                    span(className="ev-trend "+if(i%2==0)"up"else"down",title="Average: 123.4") { +(if(i%2==0)"▲"else"▼") }
                }
            }
        }
    }
    val frame=StandaloneReviewFrame("events",css,width,height,theme) { app(Unit) }
    override val fonts get()=frame.fonts
    override fun settle():List<PaintCommand> { frame.settle();frame.all(".ev-lines-plot").lastOrNull()?.getBoundingClientRect()?.let { resize(it.width to it.height) };return frame.settle() }
}
