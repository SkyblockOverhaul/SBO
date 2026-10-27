package net.sbo.mod.test

import com.google.gson.JsonParser
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.*
import java.nio.file.Files
import java.util.Locale

/** Source-only achievement definitions, synthetic unlock/progress state. Does not construct
 * Achievement objects (whose initialization reads live state). Native obfuscation/check SVG
 * rendering is not reproduced; locked secrets have a safe question-mark specimen instead.
 */
internal class AchievementsReviewFixture(css: String = StandaloneReviewFrame.css("achievements"), width: Float = 640f,
    height: Float = 480f, theme: String = "sbo-dark", grouped: Boolean = false, repeatableOnly: Boolean = false,
    initialQuery: String = "") {
    data class Entry(val id: Int, val name: String, val description: String, val rarity: String, val hidden: Boolean, val repeatable: Boolean)
    companion object {
        val out get() = ReviewResources.project.resolve("build/ui-review/achievements")
        val rarities = listOf("Common","Uncommon","Rare","Epic","Legendary","Mythic","Divine","Celestial","Impossible")
        private val source = Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/diana/achievements/AchievementManager.kt"))
        private val quoted="\"(?:\\\\.|[^\"\\\\])*\""
        val catalog = Regex("addAchievement\\((\\d+),\\s*($quoted),\\s*($quoted),\\s*($quoted)([^\\n]*)").findAll(source).map {
            val g=it.groupValues
            Entry(g[1].toInt(),JsonParser.parseString(g[2]).asString,JsonParser.parseString(g[3]).asString,
                JsonParser.parseString(g[4]).asString,"hidden = true" in g[5],"repeatable = false" !in g[5])
        }.toList()
        val samples = rarities.map { r -> catalog.first { it.rarity==r } }.let { picked ->
            picked + catalog.filter { it.hidden && it.id !in picked.map { e -> e.id } }.take(2)
        }
        fun unlocked(index:Int)=index==samples.lastIndex || (!samples[index].hidden && index%2==0)
    }
    var queryValue=initialQuery; private set
    var shownCount=0; private set
    private val app=component("Offline Achievements specimens") {
        var query by useState(initialQuery)
        var status by useState("all")
        var sort by useState(if(grouped)"grouped"else"id")
        var type by useState(if(repeatableOnly)"repeatable"else"all")
        var shownRarities by useState(rarities)
        var scale by useState("global")
        val ofType=samples.withIndex().filter { type!="repeatable" || it.value.repeatable }
        val unlockedCount=ofType.count { unlocked(it.index) }
        val percent=unlockedCount*100.0/ofType.size.coerceAtLeast(1)
        val shown=ofType.filter { (i,e) ->
            (status=="all" || (status=="unlocked")==unlocked(i)) && e.rarity in shownRarities &&
                (query.isBlank() || listOf(e.name,if(e.hidden&&!unlocked(i))""else e.description,e.rarity).any { it.contains(query.trim(),true) })
        }.sortedBy { if(sort=="rarity")rarities.indexOf(it.value.rarity)*1000+it.value.id else it.value.id }
        queryValue=query;shownCount=shown.size
        div(className="ach-window") {
            header(className="ach-header") {
                div(className="ach-heading") {
                    span(className="ach-title") { +"Achievements" }
                    div(className="ach-progress") { span(className="ach-progress-text") { +"$unlockedCount/${ofType.size} · ${String.format(Locale.ROOT,"%.1f",percent)}%" };div(className="ach-bar") { div(className="ach-bar-fill",style="width:${percent}%;") } }
                }
                select(value=scale,onChange={scale=it.value},className="ach-scale-select") { option("global","Size: Global");option("auto","Size: Auto");option("2","Size: 2") }
                button(className="ach-close sbo-close") { +"×" }
            }
            div(className="ach-toolbar") {
                input(value=query,onChange={query=it.value},placeholder="Search...",className="ach-search")
                select(value=status,onChange={status=it.value}) { option("all","All");option("locked","Locked");option("unlocked","Unlocked") }
                segmented(value=sort,onChange={sort=it}) { option("id","Default");option("rarity","Rarity");option("grouped","Grouped") }
                segmented(value=type,onChange={type=it}) { option("all","All");option("repeatable","Repeatable") }
            }
            chips(values=shownRarities,onChange={shownRarities=it},className="ach-rarities") { rarities.forEach { option(it,it,className="ach-r-${it.lowercase()}") } }
            scroll(className="ach-list guilib-autohide") {
                when {
                    shown.isEmpty() -> div(className="ach-empty") { +"No achievements match." }
                    sort=="grouped" -> rarities.forEach { rarity ->
                        val entries=shown.filter{it.value.rarity==rarity}
                        if(entries.isNotEmpty()) {
                            val all=ofType.filter{it.value.rarity==rarity}
                            div(className="ach-group-title ach-r-${rarity.lowercase()}") { span(className="ach-group-name") { +rarity };span(className="ach-group-count") { +"${all.count{unlocked(it.index)}}/${all.size}" } }
                            div(className="ach-grid") { entries.forEach { card(it.value,it.index) } }
                        }
                    }
                    else -> div(className="ach-grid") { shown.forEach { card(it.value,it.index) } }
                }
            }
        }
    }
    private fun NodeBuilder.card(e:Entry,index:Int) {
        val on=unlocked(index);val times=if(on&&e.repeatable)3 else if(on)1 else 0
        div(className=classNames("ach-card","ach-r-${e.rarity.lowercase()}","unlocked" to on),id="achievement-${e.id}") {
            div(className="ach-card-head") { span(className="ach-name") { +e.name };if(on)img("sbo:ui/achievements/check.svg",className="ach-check",title="Unlocked") }
            div(className="ach-desc") { +(if(e.hidden&&!on)"????????????????????"else e.description) }
            div(className="ach-card-foot") {
                span(className="ach-rarity") { +e.rarity }
                if(e.hidden)span(className="ach-tag") { +"Secret" }
                if(e.repeatable)span(className="ach-tag") { +"Repeatable" }
                if(times>1)span(className="ach-tag") { +"×$times" }
                if(on&&e.repeatable&&index%3==0)span(className="ach-tag ok") { +"This event" }
            }
        }
    }
    val frame=StandaloneReviewFrame("achievements",css,width,height,theme) { app(Unit) }
}
