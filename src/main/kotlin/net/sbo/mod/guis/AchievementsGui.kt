package net.sbo.mod.guis

import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.UiScale
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.chips
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.input
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.achievements.Achievement
import net.sbo.mod.diana.achievements.AchievementManager
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.data.configs.achievements.AchievementsView
import java.util.Locale

object AchievementsGui {
    private val STYLES = listOf("sbo:ui/achievements/achievements.css", SboLook.STYLE)
    private val RARITIES = listOf("Common", "Uncommon", "Rare", "Epic", "Legendary", "Mythic", "Divine", "Celestial", "Impossible")

    /** Opens the window. Must run on the client thread. */
    fun open() {
        GuiLib.open(App, STYLES, title = "SBO Achievements")
    }

    fun register() {
        Register.command("sboachievements") { mc.schedule { open() } }
    }

    private data class Entry(val achievement: Achievement, val unlocked: Boolean, val times: Int, val thisEvent: Boolean)

    private fun loadView(): AchievementsView {
        val data = DataManager.sboData
        return data.achievementsView ?: AchievementsView.fromLegacyFilter(data.achievementFilter)
    }

    private fun entries(): List<Entry> {
        val totals = DataManager.achievementsData.totalAchievements
        return AchievementManager.achievements.values.map { a ->
            Entry(a, a.isUnlocked(total = true), totals[a.id] ?: 0, a.repeatable && Debug.repeatableAchie && a.isUnlocked())
        }
    }

    private val App = component("Achievements") {
        var view by useState(loadView())
        var query by useState("")
        val grouped = view.sort == "grouped"
        useSboScale(UiScale.own(view.uiScale))
        useSboTheme()

        fun update(next: AchievementsView) {
            view = next
            DataManager.sboData.achievementsView = next
            DataManager.save(DataManager::sboData)
        }

        val ofType = entries().filter { view.type != "repeatable" || it.achievement.repeatable }
        val unlocked = ofType.count { it.unlocked }
        val percent = if (ofType.isEmpty()) 0.0 else unlocked * 100.0 / ofType.size
        val shown = ofType
            .filter { e ->
                when (view.status) {
                    "locked" -> !e.unlocked
                    "unlocked" -> e.unlocked
                    else -> true
                }
            }
            .filter { it.achievement.rarity !in view.hiddenRarities }
            .filter { e ->
                val a = e.achievement
                // The description of a locked secret achievement stays hidden, also from the search
                val description = if (a.hidden && !e.unlocked) "" else a.description
                query.isBlank() || listOf(a.name, description, a.rarity).any { it.contains(query.trim(), ignoreCase = true) }
            }
            .sortedWith(
                if (view.sort == "rarity") compareBy({ RARITIES.indexOf(it.achievement.rarity) }, { it.achievement.id })
                else compareBy { it.achievement.id }
            )

        div(className = "ach-window") {
            header(className = "ach-header") {
                span(className = "ach-title") { +"Achievements" }
                div(className = "ach-progress", title = if (view.type == "repeatable") "Repeatable achievements you have unlocked" else "Achievements you have unlocked") {
                    span(className = "ach-progress-text") { +"$unlocked/${ofType.size} · ${String.format(Locale.ROOT, "%.1f", percent)}%" }
                    div(className = "ach-bar") { div(className = "ach-bar-fill", style = "width: ${String.format(Locale.ROOT, "%.2f", percent)}%") }
                }
                div(className = "ach-spacer")
                select(value = UiScale.id(UiScale.own(view.uiScale)), onChange = { e -> update(view.copy(uiScale = UiScale.parse(e.value))) }, className = "ach-scale-select") {
                    UiScale.OWN_CHOICES.forEach { scale -> option(UiScale.id(scale), "Size: ${UiScale.label(scale)}") }
                }
                button(className = "ach-close", title = "Close", onClick = { GuiLib.close() }) { +"x" }
            }

            div(className = "ach-toolbar") {
                input(type = "text", value = query, placeholder = "Search...", onChange = { query = it.value }, className = "ach-search")
                select(value = view.status, onChange = { e -> update(view.copy(status = e.value)) }) {
                    option("all", "All")
                    option("locked", "Locked")
                    option("unlocked", "Unlocked")
                }
                segmented(value = view.sort, onChange = { update(view.copy(sort = it)) }) {
                    option("id", "Default")
                    option("rarity", "Rarity")
                    option("grouped", "Grouped", title = "A section per rarity with its own progress")
                }
                segmented(value = view.type, onChange = { update(view.copy(type = it)) }) {
                    option("all", "All")
                    option("repeatable", "Repeatable", title = "Achievements you can unlock again every Diana event")
                }
            }
            chips(
                values = RARITIES.filter { it !in view.hiddenRarities },
                onChange = { shownRarities -> update(view.copy(hiddenRarities = RARITIES.filter { it !in shownRarities }.toMutableList())) },
                className = "ach-rarities"
            ) {
                RARITIES.forEach { option(it, it, className = rarityClass(it)) }
            }

            scroll(className = "ach-list guilib-autohide") {
                when {
                    shown.isEmpty() -> div(className = "ach-empty") { +"No achievements match." }
                    grouped -> RARITIES.forEach { rarity ->
                        val group = shown.filter { it.achievement.rarity == rarity }
                        if (group.isEmpty()) return@forEach
                        val all = ofType.filter { it.achievement.rarity == rarity }
                        div(className = classNames("ach-group-title", rarityClass(rarity)), key = "group:$rarity") {
                            span(className = "ach-group-name") { +rarity }
                            span(className = "ach-group-count") { +"${all.count { it.unlocked }}/${all.size}" }
                        }
                        div(className = "ach-grid", key = "grid:$rarity") { group.forEach { card(it) } }
                    }
                    else -> div(className = "ach-grid") { shown.forEach { card(it) } }
                }
            }
        }
    }

    private fun NodeBuilder.card(entry: Entry) {
        val a = entry.achievement
        div(className = classNames("ach-card", rarityClass(a.rarity), "unlocked" to entry.unlocked), key = a.id) {
            div(className = "ach-card-head") {
                span(className = "ach-name") { +a.name }
                if (entry.unlocked) img(src = "sbo:ui/achievements/check.svg", className = "ach-check", title = "Unlocked")
            }
            div(className = "ach-desc") { +a.description }
            div(className = "ach-card-foot") {
                span(className = "ach-rarity") { +a.rarity }
                if (a.hidden) span(className = "ach-tag", title = "Hidden until unlocked") { +"Secret" }
                if (a.repeatable) span(className = "ach-tag", title = "Can be unlocked again every Diana event") { +"Repeatable" }
                if (entry.times > 1) span(className = "ach-tag", title = "Unlocked ${entry.times} times") { +"×${entry.times}" }
                if (entry.thisEvent) span(className = "ach-tag ok", title = "Unlocked in the current Diana event") { +"This event" }
            }
        }
    }

    private fun rarityClass(rarity: String) = "ach-r-${rarity.lowercase()}"

}
