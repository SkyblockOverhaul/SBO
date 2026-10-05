package net.sbo.mod.partyfinder.gui

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.aside
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h2
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.main
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.sortableList
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.tabs
import net.sbo.guilib.core.dsl.useToast
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin
import net.sbo.mod.partyfinder.OwnStats
import net.sbo.mod.partyfinder.PartyCategories
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.utils.data.DataManager

object PartyFinderGui {
    private val STYLES = listOf("sbo:ui/partyfinder/partyfinder.css", "sbo:ui/partyfinder/themes.css")
    private const val KOFI_URL = "https://ko-fi.com/skyblock_overhaul"

    /** Selectable fonts, id to label. Inter and Minecraft come with GuiLib, the others are declared in the CSS. */
    internal val FONTS = linkedMapOf(
        "inter" to "Inter",
        "minecraft" to "Minecraft",
        "nunito" to "Nunito",
        "jetbrains-mono" to "JetBrains Mono"
    )

    /** Selectable window sizes; null follows the Minecraft GUI scale. */
    internal val SCALES: List<Float?> = listOf(null, 1f, 1.5f, 2f, 2.5f, 3f, 4f)

    /** Must run on the client thread. */
    fun open() {
        GuiLib.open(App, STYLES, title = "SBO Party Finder")
    }

    /** "kuudra/infernal" or "diana" to its target; a category with subcategories alone means all of them. */
    internal fun targetOf(key: String): PartyTarget? {
        val parts = key.split('/', limit = 2)
        return PartyCategories.target(parts[0], parts.getOrElse(1) { PartyTarget.ALL })
    }

    private fun startKey(): String {
        val config = DataManager.partyFinderConfigState
        return if (config.startWithFavorites) config.favorites.firstOrNull() ?: "" else ""
    }

    private val App = component("PartyFinder") {
        val config = DataManager.partyFinderConfigState
        var data by useState(PartyCategories.data)
        var failed by useState(false)
        var selected by useState(startKey())
        var page by useState("parties")
        var favorites by useState(config.favorites.toList())
        var own by useState<MemberView?>(null)
        var ownBySub by useState(mapOf<String, MemberView>())
        // Answers arrive one by one, the ref always holds all of them so far
        val ownCollected = useRef(mapOf<String, MemberView>())
        var ownError by useState<String?>(null)
        var reload by useState(0)
        var inspected by useState<InspectedPlayer?>(null)
        var font by useState(config.font.takeIf { it in FONTS } ?: "inter")
        // On the body, so modals, tooltips and toasts use the font too
        FONTS.keys.forEach { id -> useBodyClass("pf-font-$id", font == id) }
        var uiScale by useState(config.uiScale?.takeIf { it in SCALES })
        useScreenScale(uiScale)
        var theme by useState(PartyFinderThemes.find(config.theme))
        var recombobulated by useState(config.recombobulated)
        var favoritesOnlyOnce by useState(config.favoritesOnlyOnce)
        // On the body like the font, so modals, tooltips and toasts follow the theme
        PartyFinderThemes.BASES.forEach { base -> useBodyClass("pf-theme-$base", theme.base == base) }
        useBodyClass("pf-hypixel", theme.hypixelColors)
        useBodyClass("pf-recomb", recombobulated)
        useBodyClass("pf-marks", theme.marks)
        val document = useDocument()
        useEffect(theme) {
            val body = document.body
            // Kept here, the cleanup would read the next theme from the state
            val colors = theme.colors
            colors.forEach { (name, value) -> body.setStyleProperty("--$name", value) }
            onCleanup { colors.keys.forEach { body.removeStyleProperty("--$it") } }
        }
        // Ticks so countdowns of closed events stay current
        val clock = useState(System.currentTimeMillis())
        var queued by useState(PartyFinderManager.queuedParty)
        var inQueue by useState(PartyFinderManager.inQueue)
        var joinedParties by useState(PartyFinderManager.joinedParties)
        var onlineUsers by useState<Int?>(null)
        val currentKey = useRef("")
        val toast = useToast()

        useEffect {
            PartyCategories.get { loaded ->
                if (loaded == null) failed = true else data = loaded
            }
            PartyFinderManager.listener = { success, text ->
                if (success) toast.success(text) else toast.error(text, durationMs = 8000)
            }
            onCleanup { PartyFinderManager.listener = null }
        }
        useInterval(1000) {
            queued = PartyFinderManager.queuedParty
            inQueue = PartyFinderManager.inQueue
            joinedParties = PartyFinderManager.joinedParties
        }
        useInterval(30_000) { clock.set(System.currentTimeMillis()) }
        // The server counts users of the last 5 minutes, so once a minute is enough
        fun loadOnlineUsers() = PartyFinderManager.getActiveUsers(onError = { onlineUsers = null }) { onlineUsers = it }
        useEffect { loadOnlineUsers() }
        useInterval(60_000) { loadOnlineUsers() }

        // Parties per party type for the sidebar, hidden when the server can't tell
        var partyCounts by useState<Map<String, Int>?>(null)
        fun loadCounts() = PartyFinderApi.counts(onError = { partyCounts = null }) { partyCounts = it }
        useEffect(queued?.createdAt, inQueue) { loadCounts() }
        useInterval(60_000) { loadCounts() }

        val target = data?.let { loaded ->
            targetOf(selected) ?: loaded.categories.firstOrNull()?.let { PartyCategories.target(it.id) }
        }

        useEffect(target?.key, reload) {
            val t = target ?: return@useEffect
            currentKey.current = t.key
            // All loads the stats of every subcategory, tier stats like Kuudra completions differ between them
            val targets = t.subTargets()
            own = OwnStats.cached(targets.first())
            ownCollected.current = targets.mapNotNull { x -> OwnStats.cached(x)?.let { x.key to it } }.toMap()
            ownBySub = ownCollected.current
            ownError = null
            targets.forEach { x ->
                OwnStats.get(x, onError = { if (currentKey.current == t.key) ownError = ProblemText.error(it) }) { me ->
                    if (currentKey.current == t.key) {
                        ownCollected.current = ownCollected.current + (x.key to me)
                        ownBySub = ownCollected.current
                        if (x == targets.first()) own = me
                    }
                }
            }
        }

        fun ownFor(t: PartyTarget): MemberView? = if (target?.all == true) ownBySub[t.key] else own

        // The panel belongs to one party type
        useEffect(target?.key) { inspected = null }

        fun saveFavorites(next: List<String>) {
            favorites = next
            config.favorites = next.toMutableList()
            config.save()
        }

        fun toggleFavorite(key: String) = saveFavorites(if (key in favorites) favorites - key else favorites + key)

        fun select(key: String) {
            selected = key
            if (page == "settings" || page == "rules") page = "parties"
        }

        val panel = inspected?.takeIf { page == "parties" && target != null }
        // Window and panel are centered together, so the window moves left when the panel opens
        div(className = classNames("pf-stage", "with-panel" to (panel != null))) {
            div(className = "pf-window") {
                header(className = "pf-header") {
                    span(className = "pf-title") { +"Party Finder" }
                    tabs(value = page, onChange = { page = it }, className = "pf-nav") {
                        tab("parties", "Parties")
                        tab("create", if (inQueue) "Edit Party" else "Create Party")
                        tab("rules", "Rules")
                        tab("settings", "Settings")
                    }
                    div(className = "pf-spacer")
                    onlineUsers?.let { count ->
                        div(className = "pf-online", title = "Players using SBO right now.") {
                            span(className = "pf-online-dot")
                            span { +"$count" }
                        }
                    }
                    val mine = queued
                    if (inQueue && mine != null) {
                        val label = PartyCategories.target(mine.partyType, mine.subType)?.label ?: mine.partyType
                        div(className = "pf-queue", title = "Your party is listed, players can ask to join") {
                            span(className = "pf-queue-text", onClick = { select("${mine.partyType}/${mine.subType}") }) {
                                +"$label ${mine.memberCount}/${mine.partySize}"
                            }
                            button(className = "pf-small danger", onClick = { PartyFinderManager.removePartyFromQueue() }) { +"Remove" }
                        }
                    }
                    button(className = "pf-close", title = "Close", onClick = { GuiLib.close() }) { +"✕" }
                }

                div(className = "pf-body") {
                    aside(className = "pf-sidebar") {
                        scroll(className = "pf-side-scroll guilib-autohide") {
                            div(className = "pf-side-title") { +"Favorites" }
                            if (favorites.isEmpty()) {
                                p(className = "pf-hint") { +"Click a star to pin a party type here." }
                            } else {
                                sortableList(favorites, key = { it }, onReorder = { saveFavorites(it) }, className = "pf-fav-list") { key, _ ->
                                    val fav = if (data != null) targetOf(key) else null
                                    sideItem(
                                        type = key.substringBefore('/'),
                                        count = partyCounts?.let { it[key] ?: 0 },
                                        label = when {
                                            fav == null -> key
                                            '/' in key -> fav.label
                                            else -> fav.category.label
                                        },
                                        active = fav != null && fav.key == target?.key,
                                        favorite = true,
                                        onSelect = { select(key) },
                                        onStar = { toggleFavorite(key) }
                                    )
                                }
                            }
                            div(className = "pf-side-title") { +"Party Types" }
                            data?.categories?.filterNot { favoritesOnlyOnce && it.id in favorites }?.forEach { category ->
                                sideItem(
                                    type = category.id,
                                    count = partyCounts?.let { it[category.id] ?: 0 },
                                    label = category.label,
                                    active = target?.partyType == category.id,
                                    favorite = category.id in favorites,
                                    onSelect = { select(category.id) },
                                    onStar = { toggleFavorite(category.id) },
                                    key = category.id,
                                    id = "pf-cat-${category.id}"
                                )
                            }
                        }
                        // Stays below the scrolling list, so it shows on every page
                        div(
                            className = "pf-side-item pf-support",
                            title = "Opens Ko-fi in your browser. Donations help keep SBO running.",
                            onClick = { SBOKotlin.openInBrowser(KOFI_URL) }
                        ) {
                            img(src = "${StatView.ICONS}/heart.svg", className = "pf-icon pf-heart")
                            span(className = "pf-side-label") { +"Support SBO" }
                        }
                    }

                    // pf-main-list lets low windows scroll the toolbar away with the parties
                    main(className = classNames("pf-main", "pf-main-list guilib-autohide" to (page == "parties"))) {
                        when {
                            page == "rules" -> RulesPage(Unit, key = "rules")
                            page == "settings" -> SettingsPage(
                                SettingsProps(
                                    target, favorites, ::saveFavorites, { reload++ },
                                    font = font,
                                    onFont = { id ->
                                        font = id
                                        config.font = id
                                        config.save()
                                    },
                                    uiScale = uiScale,
                                    onScale = { scale ->
                                        uiScale = scale
                                        config.uiScale = scale
                                        config.save()
                                    },
                                    theme = theme,
                                    onTheme = { picked ->
                                        theme = picked
                                        config.theme = picked.id
                                        config.save()
                                    },
                                    recombobulated = recombobulated,
                                    onRecombobulated = { on ->
                                        recombobulated = on
                                        config.recombobulated = on
                                        config.save()
                                    },
                                    favoritesOnlyOnce = favoritesOnlyOnce,
                                    onFavoritesOnlyOnce = { on ->
                                        favoritesOnlyOnce = on
                                        config.favoritesOnlyOnce = on
                                        config.save()
                                    }
                                ),
                                key = "settings"
                            )
                            target == null && failed -> message("Could not load the party types from the SBO server.") {
                                button(onClick = {
                                    failed = false
                                    PartyCategories.get(force = true) { loaded -> if (loaded == null) failed = true else data = loaded }
                                }) { +"Try again" }
                            }
                            target == null -> message("Loading party types...")
                            else -> {
                                // A party belongs to one subcategory, so creating starts on the first one
                                val shown = if (page == "create" && target.all) target.subTargets().first() else target
                                toolbar(shown, favorites, showAll = page != "create", onSub = { select(it) }, onStar = { toggleFavorite(it) })
                                if (page == "create") {
                                    CreatePage(CreateProps(shown, ownFor(shown) ?: own, inQueue, onRules = { page = "rules" }), key = "create:${shown.key}")
                                } else {
                                    PartiesPage(
                                        PartiesProps(
                                            target, own, ::ownFor, ownError, reload, queued?.createdAt ?: 0L, inQueue, onEdit = { page = "create" },
                                            inspected = inspected?.member?.uuid, onInspect = { inspected = it },
                                        joinedParties = joinedParties,
                                            onRules = { page = "rules" }
                                        ),
                                        key = "list"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (panel != null && target != null) {
                val panelTarget = target.forParty(panel.party)
                val panelOwn = OwnStats.cached(panelTarget, panel.party.options) ?: ownFor(panelTarget)
                playerPanel(panel, panelTarget, panelOwn) { inspected = null }
            }
        }
    }

    private fun NodeBuilder.sideItem(
        type: String,
        count: Int?,
        label: String,
        active: Boolean,
        favorite: Boolean,
        onSelect: () -> Unit,
        onStar: () -> Unit,
        key: Any? = null,
        id: String? = null
    ) {
        // pf-type-* colors the name with Hypixel colors
        div(className = classNames("pf-side-item", "pf-type-$type", "active" to active), id = id, key = key, onClick = { onSelect() }) {
            span(className = "pf-side-label") { +label }
            count?.let { span(className = classNames("pf-side-count", "zero" to (it == 0)), title = "Parties listed right now") { +"$it" } }
            starIcon(favorite, onStar)
        }
    }

    internal fun NodeBuilder.starIcon(favorite: Boolean, onToggle: () -> Unit) {
        img(
            src = "${StatView.ICONS}/${if (favorite) "star-filled" else "star"}.svg",
            className = classNames("pf-icon", "pf-star", "on" to favorite),
            title = if (favorite) "Remove from favorites" else "Add to favorites",
            onClick = { e ->
                e.stopPropagation()
                onToggle()
            }
        )
    }

    private fun NodeBuilder.toolbar(target: PartyTarget, favorites: List<String>, showAll: Boolean, onSub: (String) -> Unit, onStar: (String) -> Unit) {
        val category = target.category
        div(className = "pf-toolbar") {
            h2(className = "pf-heading pf-type-${category.id}") { +category.label }
            // Pills wrap onto more lines instead of scrolling sideways
            if (category.subcategories.size > 1) {
                div(className = "pf-subs-row") {
                    tabs(value = target.subType, onChange = { onSub("${category.id}/$it") }, variant = "pills", className = "pf-subs", key = "subs:${category.id}") {
                        if (showAll) tab(PartyTarget.ALL, "All")
                        category.subcategories.forEach { sub ->
                            val subTarget = PartyCategories.target(category.id, sub.id) ?: return@forEach
                            tab(sub.id, subLabel(subTarget))
                        }
                    }
                    span(className = "pf-sub-star", title = "Pin ${target.label} to favorites") {
                        starIcon(target.key in favorites) { onStar(target.key) }
                    }
                }
            }
        }
    }

    internal fun NodeBuilder.message(text: String, actions: (NodeBuilder.() -> Unit)? = null) {
        div(className = "pf-message") {
            p { +text }
            actions?.invoke(this)
        }
    }
}
