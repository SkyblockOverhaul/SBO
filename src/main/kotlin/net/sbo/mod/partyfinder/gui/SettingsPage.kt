package net.sbo.mod.partyfinder.gui

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h3
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.playerHead
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.sortableList
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch
import net.sbo.guilib.core.dsl.tabs
import net.sbo.guilib.core.dsl.useToast
import net.sbo.mod.partyfinder.BlockedPlayers
import net.sbo.mod.partyfinder.OwnStats
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.guis.look.SboThemes
import net.sbo.mod.guis.look.UiScale
import net.sbo.mod.settings.categories.Themes
import net.sbo.mod.utils.data.DataManager

internal data class SettingsProps(
    val target: PartyTarget?,
    val favorites: List<String>,
    val onFavorites: (List<String>) -> Unit,
    val onStatsReloaded: () -> Unit,
    val font: String,
    val onFont: (String) -> Unit,
    val uiScale: Float?,
    val onScale: (Float?) -> Unit,
    val themeId: String?,
    val theme: SboThemes.Theme,
    val onTheme: (String?) -> Unit,
    val recombobulated: Boolean,
    val onRecombobulated: (Boolean) -> Unit,
    val favoritesOnlyOnce: Boolean,
    val onFavoritesOnlyOnce: (Boolean) -> Unit,
    // In the window's state, so the tour can open a section
    val section: String,
    val onSection: (String) -> Unit,
    val onTour: () -> Unit
)

/** Party finder settings inside the window, independent of the config menu. */
internal val SettingsPage = component<SettingsProps>("SettingsPage") { props ->
    val config = DataManager.partyFinderConfigState
    val section = props.section
    var autoInvite by useState(config.autoInvite)
    var autoRequeue by useState(config.autoRequeue)
    var startWithFavorites by useState(config.startWithFavorites)
    var autoRefresh by useState(config.autoRefreshSeconds)
    var blocked by useState(BlockedPlayers.list())
    var reloading by useState(false)
    // Read again every time the settings open, so new theme files show up without a restart
    val themes = useStateLazy { SboThemes.all() }
    val toast = useToast()

    div(className = "pf-toolbar") {
        tabs(value = section, onChange = { props.onSection(it) }, variant = "pills", className = "pf-subs pf-settings-tabs") {
            tab("general", "General")
            tab("favorites", "Favorites")
            tab("look", "Look")
        }
    }

    // Own key per section, so every section starts at the top
    scroll(className = "pf-form guilib-autohide", key = "settings:$section") {
        when (section) {
            "favorites" -> {
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
                settingRow(
                    "Hide favorites in Party Types",
                    "Favorite party types only show under Favorites, not again under Party Types."
                ) {
                    switch(checked = props.favoritesOnlyOnce, onChange = { e -> props.onFavoritesOnlyOnce(e.checked) })
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
            }

            "look" -> {
                settingRow(
                    "Theme",
                    "The colors of the party finder. Global uses the theme from the SBO settings (Themes), which all SBO windows share. Hover a theme in the list to see what it does."
                ) {
                    select(value = props.themeId ?: GLOBAL_THEME, onChange = { e ->
                        if (e.value == GLOBAL_THEME) props.onTheme(null)
                        else themes.value.firstOrNull { it.id == e.value }?.let { props.onTheme(it.id) }
                    }, className = "pf-theme-select") {
                        option(GLOBAL_THEME, "Global (${SboThemes.find(Themes.theme).label})", title = "The theme from the SBO settings, the same in every SBO window.")
                        themes.value.forEach { theme -> option(theme.id, theme.label, title = theme.description) }
                    }
                }
                p(className = "pf-hint pf-theme-info") { +props.theme.description }
                settingRow(
                    "Your own themes",
                    "Put theme files into this folder. The README in it explains every color, example.json is a theme to copy."
                ) {
                    button(onClick = {
                        SboThemes.openFolder()
                        themes.set(SboThemes.all())
                    }) { +"Open theme folder" }
                }
                settingRow(
                    "Recombobulated items",
                    "Shows item names one rarity higher, like after a Recombobulator 3000. Only for themes with Hypixel colors."
                ) {
                    switch(checked = props.recombobulated, onChange = { e -> props.onRecombobulated(e.checked) }, id = "pf-recomb-switch")
                }
                settingRow(
                    "Font",
                    "The font of the party finder window. Minecraft looks like the game, the others are easier to read."
                ) {
                    select(value = props.font, onChange = { e -> props.onFont(e.value) }) {
                        PartyFinderGui.FONTS.forEach { (id, label) -> option(id, label) }
                    }
                }
                settingRow(
                    "Size",
                    "How big the party finder window is. Global uses the size from the SBO settings, Auto your Minecraft GUI scale."
                ) {
                    select(value = UiScale.id(props.uiScale), onChange = { e -> props.onScale(UiScale.parse(e.value)) }, className = "pf-scale-select") {
                        UiScale.OWN_CHOICES.forEach { scale -> option(UiScale.id(scale), UiScale.label(scale)) }
                    }
                }
            }

            else -> {
                h3(className = "pf-section") { +"Your party" }
                settingRow(
                    "Auto invite",
                    "When someone asks to join your party and meets the requirements, SBO invites them for you."
                ) {
                    switch(checked = autoInvite, onChange = { e ->
                        autoInvite = e.checked
                        config.autoInvite = e.checked
                        config.save()
                    })
                }
                settingRow(
                    "Auto requeue",
                    "When a player leaves your party, SBO lists the party again by itself."
                ) {
                    switch(checked = autoRequeue, onChange = { e ->
                        autoRequeue = e.checked
                        config.autoRequeue = e.checked
                        config.save()
                    })
                }

                h3(className = "pf-section") { +"Blocked players" }
                p(className = "pf-hint") { +"Players on this list can't join your parties. Their join requests are declined automatically." }
                if (blocked.isEmpty()) {
                    p(className = "pf-hint") { +"No blocked players yet. Right-click a player in the party list to block them, or use /block add <name>." }
                }
                blocked.forEach { player ->
                    div(className = "pf-fav-row", key = player.uuid) {
                        playerHead(uuidOf(player.uuid), className = "pf-head")
                        span(className = "pf-fav-name") { +player.name }
                        button(className = "pf-small", onClick = {
                            BlockedPlayers.unblock(player.uuid)
                            blocked = BlockedPlayers.list()
                        }) { +"Unblock" }
                    }
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

                h3(className = "pf-section") { +"Help" }
                settingRow("Tour", "Shows the party finder tour from the first time again.") {
                    button(onClick = { props.onTour() }) { +"Start tour" }
                }
            }
        }
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

private const val GLOBAL_THEME = "global"
