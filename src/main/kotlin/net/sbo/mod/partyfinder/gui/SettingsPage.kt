package net.sbo.mod.partyfinder.gui

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h3
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.sortableList
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch
import net.sbo.guilib.core.dsl.useToast
import net.sbo.mod.partyfinder.OwnStats
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.settings.Settings
import net.sbo.mod.settings.categories.PartyFinder
import net.sbo.mod.utils.data.DataManager

internal data class SettingsProps(
    val target: PartyTarget?,
    val favorites: List<String>,
    val onFavorites: (List<String>) -> Unit,
    val onStatsReloaded: () -> Unit,
    val font: String,
    val onFont: (String) -> Unit
)

/** Party finder settings inside the window, independent of the config menu. */
internal val SettingsPage = component<SettingsProps>("SettingsPage") { props ->
    val config = DataManager.partyFinderConfigState
    var autoInvite by useState(PartyFinder.autoInvite)
    var autoRequeue by useState(PartyFinder.autoRequeue)
    var startWithFavorites by useState(config.startWithFavorites)
    var autoRefresh by useState(config.autoRefreshSeconds)
    var reloading by useState(false)
    val toast = useToast()

    scroll(className = "pf-form") {
        h3(className = "pf-section") { +"Your party" }
        settingRow(
            "Auto invite",
            "When someone asks to join your party and meets the requirements, SBO invites them for you."
        ) {
            switch(checked = autoInvite, onChange = { e ->
                autoInvite = e.checked
                PartyFinder.autoInvite = e.checked
                Settings.save()
            })
        }
        settingRow(
            "Auto requeue",
            "When a player leaves your party, SBO lists the party again by itself."
        ) {
            switch(checked = autoRequeue, onChange = { e ->
                autoRequeue = e.checked
                PartyFinder.autoRequeue = e.checked
                Settings.save()
            })
        }

        h3(className = "pf-section") { +"Party list" }
        settingRow(
            "Refresh automatically",
            "How often the party list loads new parties by itself. You can always refresh with the button or F5."
        ) {
            segmented(value = autoRefresh.toString(), onChange = { value ->
                autoRefresh = value.toInt()
                config.autoRefreshSeconds = autoRefresh
                config.save()
            }) {
                option("0", "Off")
                option("30", "30 s")
                option("60", "60 s")
            }
        }

        h3(className = "pf-section") { +"Look" }
        settingRow(
            "Font",
            "The font of the party finder window. Minecraft looks like the game, the others are easier to read."
        ) {
            select(value = props.font, onChange = { e -> props.onFont(e.value) }) {
                PartyFinderGui.FONTS.forEach { (id, label) -> option(id, label) }
            }
        }

        h3(className = "pf-section") { +"Favorites" }
        settingRow(
            "Start with favorites",
            "Opens the party finder on your first favorite instead of the first party type."
        ) {
            switch(checked = startWithFavorites, onChange = { e ->
                startWithFavorites = e.checked
                config.startWithFavorites = e.checked
                config.save()
            })
        }
        if (props.favorites.isEmpty()) {
            p(className = "pf-hint") { +"No favorites yet. Click the star next to a party type to add one." }
        } else {
            p(className = "pf-hint") { +"Drag to change the order. The first one opens when \"Start with favorites\" is on." }
            sortableList(props.favorites, key = { it }, onReorder = { props.onFavorites(it) }, className = "pf-fav-settings") { key, _ ->
                div(className = "pf-fav-row") {
                    img(src = "${StatView.ICONS}/grip.svg", className = "pf-icon pf-grip")
                    span(className = "pf-fav-name") {
                        +(PartyFinderGui.targetOf(key)?.let { if ('/' in key) it.label else it.category.label } ?: key)
                    }
                    button(className = "pf-small", onClick = { props.onFavorites(props.favorites - key) }) { +"Remove" }
                }
            }
        }

        h3(className = "pf-section") { +"Your stats" }
        p(className = "pf-hint") {
            +"Reload your stats after you got new gear or leveled up. ${ProblemText.OWN_RELOAD_HINT}"
        }
        val target = props.target
        button(disabled = target == null || reloading, onClick = {
            if (target != null) {
                reloading = true
                OwnStats.get(target, force = true, onError = { error ->
                    reloading = false
                    toast.error("Could not reload your stats: ${ProblemText.error(error)}")
                }) {
                    reloading = false
                    toast.success("Your stats are up to date.")
                    props.onStatsReloaded()
                }
            }
        }) { +(if (reloading) "Reloading..." else "Reload my stats") }
    }
}

private fun NodeBuilder.settingRow(title: String, text: String, control: NodeBuilder.() -> Unit) {
    div(className = "pf-setting") {
        div(className = "pf-setting-text") {
            div(className = "pf-setting-title") { +title }
            div(className = "pf-hint") { +text }
        }
        control()
    }
}
