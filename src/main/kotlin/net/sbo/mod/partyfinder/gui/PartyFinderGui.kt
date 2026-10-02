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
import net.sbo.mod.partyfinder.OwnStats
import net.sbo.mod.partyfinder.PartyCategories
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.utils.data.DataManager

/** The party finder window: party types on the left, parties, the create form and settings on the right. */
object PartyFinderGui {
    private val STYLES = listOf("sbo:ui/partyfinder/partyfinder.css")

    /** Selectable fonts, id to label. Inter and Minecraft come with GuiLib, the others are declared in the CSS. */
    internal val FONTS = linkedMapOf(
        "inter" to "Inter",
        "minecraft" to "Minecraft",
        "nunito" to "Nunito",
        "jetbrains-mono" to "JetBrains Mono"
    )

    /** Selectable window sizes; null follows the Minecraft GUI scale. */
    internal val SCALES: List<Float?> = listOf(null, 1f, 1.5f, 2f, 2.5f, 3f, 4f)

    /** Opens the window. Must run on the client thread. */
    fun open() {
        GuiLib.open(App, STYLES, title = "SBO Party Finder")
    }

    /** "kuudra/infernal" or "diana" to its target; a category alone means its first subcategory. */
    internal fun targetOf(key: String): PartyTarget? {
        val parts = key.split('/', limit = 2)
        return PartyCategories.target(parts[0], parts.getOrElse(1) { "" })
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
        var ownError by useState<String?>(null)
        var reload by useState(0)
        var inspected by useState<InspectedPlayer?>(null)
        var font by useState(config.font.takeIf { it in FONTS } ?: "inter")
        // On the body, so modals, tooltips and toasts use the font too
        FONTS.keys.forEach { id -> useBodyClass("pf-font-$id", font == id) }
        var uiScale by useState(config.uiScale?.takeIf { it in SCALES })
        useScreenScale(uiScale)
        // Ticks so countdowns of closed events stay current
        val clock = useState(System.currentTimeMillis())
        var queued by useState(PartyFinderManager.queuedParty)
        var inQueue by useState(PartyFinderManager.inQueue)
        var joinedParties by useState(PartyFinderManager.joinedParties)
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

        val target = data?.let { loaded ->
            targetOf(selected) ?: loaded.categories.firstOrNull()?.let { PartyCategories.target(it.id) }
        }

        useEffect(target?.key, reload) {
            val t = target ?: return@useEffect
            currentKey.current = t.key
            own = OwnStats.cached(t)
            ownError = null
            OwnStats.get(t, onError = { if (currentKey.current == t.key) ownError = ProblemText.error(it) }) { me ->
                if (currentKey.current == t.key) own = me
            }
        }

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
            if (page == "settings") page = "parties"
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
                        tab("settings", "Settings")
                    }
                    div(className = "pf-spacer")
                    val mine = queued
                    if (inQueue && mine != null) {
                        val label = PartyCategories.target(mine.partyType, mine.subType)?.label ?: mine.partyType
                        div(className = "pf-queue", title = "Your party is listed, players can ask to join") {
                            span(className = "pf-queue-text", onClick = { select("${mine.partyType}/${mine.subType}") }) {
                                +"Your party: $label ${mine.memberCount}/${mine.partySize}"
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
                            data?.categories?.forEach { category ->
                                sideItem(
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
                    }

                    main(className = "pf-main") {
                        when {
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
                                toolbar(target, favorites, onSub = { select(it) }, onStar = { toggleFavorite(it) })
                                if (page == "create") {
                                    CreatePage(CreateProps(target, own, inQueue), key = "create:${target.key}")
                                } else {
                                    PartiesPage(
                                        PartiesProps(
                                            target, own, ownError, reload, queued?.createdAt ?: 0L, inQueue, onEdit = { page = "create" },
                                            inspected = inspected?.member?.uuid, onInspect = { inspected = it },
                                        joinedParties = joinedParties
                                        ),
                                        key = "list"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (panel != null && target != null) playerPanel(panel, target, own) { inspected = null }
        }
    }

    private fun NodeBuilder.sideItem(
        label: String,
        active: Boolean,
        favorite: Boolean,
        onSelect: () -> Unit,
        onStar: () -> Unit,
        key: Any? = null,
        id: String? = null
    ) {
        div(className = classNames("pf-side-item", "active" to active), id = id, key = key, onClick = { onSelect() }) {
            span(className = "pf-side-label") { +label }
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

    private fun NodeBuilder.toolbar(target: PartyTarget, favorites: List<String>, onSub: (String) -> Unit, onStar: (String) -> Unit) {
        val category = target.category
        div(className = "pf-toolbar") {
            h2(className = "pf-heading") { +category.label }
            // Pills wrap onto more lines instead of scrolling sideways
            if (category.subcategories.size > 1) {
                div(className = "pf-subs-row") {
                    tabs(value = target.subType, onChange = { onSub("${category.id}/$it") }, variant = "pills", className = "pf-subs", key = "subs:${category.id}") {
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
