package net.sbo.mod.profileviewer

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.aside
import net.sbo.guilib.core.dsl.b
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h2
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.input
import net.sbo.guilib.core.dsl.main
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.playerHead
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.tabs
import net.sbo.guilib.core.dsl.tooltip
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.UiScale
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import net.sbo.mod.partyfinder.PartyCategories
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.ReqMatcher
import net.sbo.mod.partyfinder.api.CategoriesData
import net.sbo.mod.partyfinder.api.CategoryDef
import net.sbo.mod.partyfinder.api.TrackingData
import net.sbo.mod.partyfinder.gui.PartyFinderGui
import net.sbo.mod.partyfinder.gui.Piece
import net.sbo.mod.partyfinder.gui.StatView
import net.sbo.mod.partyfinder.gui.ago
import net.sbo.mod.partyfinder.gui.pieces
import net.sbo.mod.partyfinder.gui.statLabel
import net.sbo.mod.partyfinder.gui.uuidOf
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import java.util.Locale

/** Stats of a player per category, for checking who wants to join your party. */
object ProfileViewerGui {
    private val STYLES = listOf("sbo:ui/partyfinder/partyfinder.css", "sbo:ui/profileviewer/profileviewer.css", SboLook.STYLE)
    private const val KOFI_URL = "https://ko-fi.com/skyblock_overhaul"
    private val GENERAL_STATS = setOf("sbLevel", "ironman")
    // kuudraCompletionsAll already shows the completions per tier
    private val SHOWN_BY = mapOf("kuudraCompletions" to "kuudraCompletionsAll")

    private var startName: String? = null
    private var startParty = false
    private var lastCategory: String? = null

    // From the last Hypixel party packet, leader first
    @Volatile
    private var partyUuids: List<String> = emptyList()
    @Volatile
    private var inParty = false

    fun open(name: String? = null, party: Boolean = false) {
        startName = name
        startParty = party
        GuiLib.open(App, STYLES, title = "SBO Profile Viewer")
    }

    fun register() {
        Register.command("sbopv", "spv", "sboprofileviewer", "sbocheck", "sboc") { args ->
            val party = args.firstOrNull()?.equals("party", ignoreCase = true) == true
            mc.schedule { open(if (party) null else args.firstOrNull(), party) }
        }
        Register.command("sbocheckparty", "sbocheckp", "sbocp") { mc.schedule { open(party = true) } }
        HypixelModApi.onPartyInfo { isInParty, _, members ->
            inParty = isInParty
            partyUuids = members.map { it.replace("-", "") }
        }
    }

    private fun ownName(): String = Player.getName() ?: Player.accountName()

    private fun ownUuid(): String = Player.getUUIDString().replace("-", "")

    private fun queuedParty(): CompareParty? =
        PartyFinderManager.queuedParty?.takeIf { PartyFinderManager.inQueue }?.let { CompareParty(it) }

    private fun startCategory(data: CategoriesData?): String =
        queuedParty()?.partyType ?: lastCategory ?: data?.categories?.firstOrNull()?.id ?: "diana"

    private val App = component("ProfileViewer") {
        val config = DataManager.partyFinderConfigState
        var data by useState(PartyCategories.data)
        var mode by useState(if (startParty) "party" else "player")
        var name by useState(startName ?: ownName())
        var query by useState("")
        var category by useState(startCategory(data))
        var tab by useState("stats")
        var upgradesShown by useState("missing")
        var profile by useState(ProfileViewerApi.cached(name))
        var loading by useState(false)
        var error by useState<String?>(null)
        var reload by useState(0)
        var refresh by useState(false)
        var checking by useState(startParty && PartyFinderManager.inQueue)
        var members by useState(partyUuids)
        var hasParty by useState(inParty)
        var party by useState<List<ViewedProfile>?>(null)
        var partyError by useState<String?>(null)
        val now = useState(System.currentTimeMillis())
        PartyFinderGui.FONTS.keys.forEach { id -> useBodyClass("pf-font-$id", config.font == id) }
        useSboScale(UiScale.own(config.uiScale))
        val theme = useSboTheme(config.theme)
        useBodyClass("pf-hypixel", theme.hypixelColors)
        useBodyClass("pf-recomb", config.recombobulated)
        useBodyClass("pf-marks", theme.marks)

        useEffect {
            PartyCategories.get { loaded -> if (loaded != null) data = loaded else error = "Could not load the categories from the SBO server." }
        }
        useInterval(1000) {
            now.set(System.currentTimeMillis())
            members = partyUuids
            hasParty = inParty
        }
        useEffect(category) { lastCategory = category }

        useEffect(data != null, name, reload) {
            if (data == null) return@useEffect
            val wanted = name
            val fromHypixel = refresh
            refresh = false
            loading = true
            error = null
            if (!fromHypixel) ProfileViewerApi.cached(wanted)?.let { profile = it }
            ProfileViewerApi.player(wanted, fromHypixel, onError = { e ->
                if (name == wanted) {
                    error = ProblemText.error(e)
                    loading = false
                }
            }) { loaded ->
                if (name == wanted) {
                    profile = loaded
                    loading = false
                }
            }
        }

        // The party packet only comes on request, so the list is fresh when the tab opens
        useEffect(mode) { if (mode == "party" && HypixelModApi.isOnHypixel) HypixelModApi.sendPartyInfoPacket() }

        val partyCheck = mode == "party" && checking
        val compare = queuedParty()
        // Your own stats are loaded on their own, so this only asks for the others
        useEffect(mode, members, data != null) {
            if (mode != "party" || data == null || members.size < 2) return@useEffect
            party = null
            partyError = null
            val own = ownUuid()
            val others = members.filter { it != own }
            fun loadOthers(ownProfile: ViewedProfile?) {
                ProfileViewerApi.party(others, onError = { partyError = ProblemText.error(it) }) { loaded ->
                    party = members.mapNotNull { id -> if (id == own) ownProfile else loaded.firstOrNull { it.uuid == id } }
                }
            }
            ProfileViewerApi.player(ownName(), refresh = false, onError = { loadOthers(null) }) { loadOthers(it) }
        }

        val categoryDef = data?.categories?.firstOrNull { it.id == category }

        fun show(next: String) {
            val wanted = next.trim()
            if (wanted.isEmpty()) return
            if (!wanted.equals(name, ignoreCase = true)) profile = ProfileViewerApi.cached(wanted)
            name = wanted
            mode = "player"
            query = ""
            reload++
        }

        div(className = "pf-window pv-window") {
            header(className = "pf-header") {
                span(className = "pf-title") { +"Profile Viewer" }
                tabs(value = mode, onChange = { mode = it }, className = "pf-nav") {
                    tab("player", "Player")
                    tab("party", "Party")
                }
                div(className = "pf-spacer")
                div(className = "pv-searchbox") {
                    input(
                        type = "text",
                        value = query,
                        onInput = { query = it.value },
                        onKeyDown = { if (it.key == "Enter") show(query) },
                        placeholder = "Player",
                        title = "Type a name and press Enter to see their stats",
                        className = "pv-search",
                        maxLength = 16
                    )
                    button(className = "pv-search-button", onClick = { show(query) }) { +"Search" }
                }
                button(className = "pf-close", title = "Close", onClick = { GuiLib.close() }) { +"x" }
            }

            val shown = profile?.takeIf { it.name.equals(name, ignoreCase = true) }
            if (mode == "player") {
                profileBar(shown, name, loading, now.value) {
                    refresh = true
                    reload++
                }
            }

            div(className = "pf-body") {
                aside(className = "pf-sidebar") {
                    scroll(className = "pf-side-scroll guilib-autohide") {
                        if (mode == "party") {
                            div(
                                className = classNames("pf-side-item", "pv-check-item", "active" to partyCheck),
                                title = "Shows if your party members meet the requirements of your party",
                                onClick = { checking = true }
                            ) {
                                span(className = "pf-side-label") { +"Party Check" }
                            }
                        }
                        div(className = "pf-side-title") { +"Categories" }
                        val queuedType = compare?.partyType
                        data?.categories?.forEach { cat ->
                            div(
                                className = classNames("pf-side-item", "pf-type-${cat.id}", "active" to (cat.id == category && !partyCheck)),
                                key = cat.id,
                                onClick = {
                                    category = cat.id
                                    checking = false
                                    if (cat.id != "diana") tab = "stats"
                                }
                            ) {
                                span(className = "pf-side-label") { +cat.label }
                                if (cat.id == queuedType) {
                                    img(src = "${StatView.ICONS}/star-filled.svg", className = "pf-icon pf-star on", title = "Your party is listed here")
                                }
                            }
                        }
                    }
                    div(
                        className = "pf-side-item pf-support",
                        title = "Opens Ko-fi in your browser. Donations help keep SBO running.",
                        onClick = { SBOKotlin.openInBrowser(KOFI_URL) }
                    ) {
                        img(src = "${StatView.ICONS}/heart.svg", className = "pf-icon pf-heart")
                        span(className = "pf-side-label") { +"Support SBO" }
                    }
                }

                main(className = "pf-main") {
                    when {
                        categoryDef == null && error != null -> message(error!!)
                        categoryDef == null -> message("Loading categories...")
                        mode == "party" -> when {
                            !hasParty || members.size < 2 -> message("You are not in a party. Join one and the members show up here.")
                            partyError != null -> message(partyError!!)
                            partyCheck && compare == null -> message("List a party in the Party Finder first, then this shows if your members meet its requirements.")
                            party == null -> message("Loading party members...")
                            partyCheck -> partyCheckView(compare!!, party!!) { show(it) }
                            else -> partyView(categoryDef, party!!) { show(it) }
                        }
                        shown == null && error != null -> message(error!!)
                        shown == null -> message("Loading stats...")
                        else -> {
                            div(className = "pf-toolbar pv-toolbar") {
                                h2(className = "pf-heading pf-type-${categoryDef.id}") { +categoryDef.label }
                                if (categoryDef.id == "diana" && shown.tracking != null) {
                                    tabs(value = tab, onChange = { tab = it }, variant = "pills", className = "pv-tabs") {
                                        tab("stats", "Stats")
                                        tab("upgrades", "Upgrades")
                                    }
                                }
                            }
                            scroll(className = "pv-scroll guilib-autohide") {
                                val tracking = shown.tracking
                                if (tab == "upgrades" && categoryDef.id == "diana" && tracking != null) {
                                    upgrades(tracking, upgradesShown == "all") { upgradesShown = it }
                                } else {
                                    statsList(shown, categoryDef)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun NodeBuilder.profileBar(profile: ViewedProfile?, name: String, loading: Boolean, now: Long, onRefresh: () -> Unit) {
        div(className = "pv-profile") {
            if (profile == null) {
                b(className = "pv-name") { +name }
                return@div
            }
            playerHead(uuidOf(profile.uuid), className = "pv-head")
            div(className = "pv-who") {
                div(className = "pv-name-row") {
                    b(className = "pv-name") { +profile.name }
                    profile.profile?.let { span(className = "pf-tag", title = "SkyBlock profile the stats come from") { +it } }
                    if (profile.ironman) span(className = "pf-tag", title = "This profile is an Ironman profile") { +"Ironman" }
                }
                div(className = "pv-general") {
                    general("SkyBlock Level", StatView.valuePieces("sbLevel", profile.general("sbLevel")))
                    profile.networth?.let { general("Networth", listOf(networthPiece(it)), "Value of everything the player owns, calculated like other mods do.") }
                    profile.general("magicalPower")?.let { general("Magical Power", StatView.valuePieces("magicalPower", it)) }
                    profile.skillAverage?.let { general("Skill Average", listOf(Piece(String.format(Locale.US, "%.2f", it)))) }
                }
            }
            div(className = "pf-spacer")
            val wait = (profile.refreshAt - now).coerceAtLeast(0)
            div(className = "pv-age") {
                profile.dataAt?.let { span(title = "When SBO last loaded this player's data from Hypixel") { +"Updated ${ago(it, now)}" } }
                button(
                    className = classNames("pf-icon-button", "loading" to loading),
                    disabled = wait > 0 || loading,
                    title = if (wait > 0) "You can load this player again in ${minutesSeconds(wait)}" else "Load the newest data from Hypixel",
                    onClick = { onRefresh() }
                ) {
                    img(src = "${StatView.ICONS}/refresh.svg", className = "pf-icon")
                }
                if (wait > 0) span(className = "pv-wait") { +minutesSeconds(wait) }
            }
        }
    }

    private fun NodeBuilder.general(label: String, value: List<Piece>, info: String? = null) {
        div(className = "pv-chip", title = info) {
            span(className = "pv-chip-label") { +label }
            span(className = "pv-chip-value") { pieces(value) }
        }
    }

    private fun NodeBuilder.compareBanner(compare: CompareParty) {
        val target = PartyCategories.target(compare.partyType, compare.subType)
        div(className = "pv-compare") {
            img(src = "${StatView.ICONS}/check.svg", className = "pf-icon")
            span { +"Requirements of your party: ${target?.label ?: compare.partyType}" }
        }
    }

    private fun NodeBuilder.statsList(profile: ViewedProfile, category: CategoryDef) {
        val bySub = profile.stats[category.id].orEmpty()
        if (bySub.isEmpty()) return message("No stats for ${category.label}.")
        val ids = statIds(category).filter { it !in GENERAL_STATS }
        val varying = varyingStats(ids, bySub)

        div(className = "pv-list") {
            ids.filter { it !in varying }.forEach { id -> statRow(id, merged(id, bySub)) }
        }
        if (varying.isEmpty()) return
        div(className = "pv-section") { +perSubTitle(category.id) }
        val subs = category.subcategories.filter { bySub.containsKey(it.id) }
        if (varying.size == 1) {
            val id = varying.first()
            div(className = "pv-row") {
                statLabel(id)
                div(className = "pv-sub-chips") {
                    subs.forEach { sub ->
                        div(className = "pv-sub-chip", key = sub.id) {
                            span(className = "pf-muted") { +"${sub.label} " }
                            pieces(StatView.valuePieces(id, bySub[sub.id]?.get(id)))
                        }
                    }
                }
            }
        } else {
            div(className = "pv-sub-grid") {
                subs.forEach { sub ->
                    div(className = "pv-sub-card", key = sub.id) {
                        div(className = "pv-sub-title") { b { +sub.label } }
                        varying.forEach { id -> statRow(id, bySub[sub.id]?.get(id), compact = true) }
                    }
                }
            }
        }
    }

    private fun NodeBuilder.statRow(id: String, value: JsonElement?, compact: Boolean = false) {
        div(className = classNames("pv-row", "compact" to compact), key = id) {
            statLabel(id)
            span(className = classNames(
                "pf-panel-value",
                "estimated" to StatView.estimated(id),
                "missing" to (value == null || value is JsonNull)
            )) { pieces(StatView.valuePieces(id, value)) }
        }
    }

    // Item lists don't count, merged() joins them
    private fun varyingStats(ids: List<String>, bySub: Map<String, Map<String, JsonElement>>): List<String> =
        ids.filter { id ->
            val values = bySub.values.mapNotNull { it[id] }
            values.distinct().size > 1 && values.any { it !is JsonArray }
        }

    // Item lists of all subcategories become one list without duplicates
    private fun merged(id: String, bySub: Map<String, Map<String, JsonElement>>): JsonElement? {
        val values = bySub.values.mapNotNull { it[id] }
        if (values.isEmpty() || values.any { it !is JsonArray }) return values.firstOrNull()
        return JsonArray(values.flatMap { it as JsonArray }.distinct())
    }

    // Met or not with the requirement as text, null when the party doesn't ask for it
    private fun markFor(compare: CompareParty, statId: String, have: JsonElement?): Pair<Boolean, String>? {
        val need = compare.reqs[statId]?.takeIf { it !is JsonNull } ?: return null
        val def = PartyCategories.target(compare.partyType, compare.subType)?.req(statId) ?: return null
        return ReqMatcher.meets(def.type, have ?: JsonNull, need) to StatView.need(def, need)
    }

    private fun NodeBuilder.upgrades(tracking: TrackingData, showAll: Boolean, onShow: (String) -> Unit) {
        div(className = "pv-upgrade-head") {
            div(className = "pv-upgrade-title") {
                b { +"Tracking ${number(tracking.tracking)} of ${number(tracking.max)}" }
                segmented(value = if (showAll) "all" else "missing", onChange = onShow, className = "pv-upgrade-filter") {
                    option("missing", "Missing")
                    option("all", "All")
                }
            }
            bar(tracking.tracking, tracking.max, "pv-bar big")
        }
        p(className = "pf-hint") {
            +(if (showAll) "Everything that gives Diana Tracking, the biggest missing gains first." else "What is still missing for more Diana Tracking, the biggest gains first.")
        }
        val sources = tracking.sources.sortedByDescending { it.max - it.value }.filter { showAll || it.value < it.max }
        if (sources.isEmpty()) return div(className = "pf-message") { p { +"Nothing missing, every source of Tracking is maxed." } }
        sources.forEach { source ->
            val done = source.value >= source.max
            div(className = classNames("pv-upgrade", "done" to done), key = source.id) {
                div(className = "pv-upgrade-label") {
                    tooltip(text = source.info, className = "pf-tip") {
                        span(className = "pf-stat-label") {
                            +source.label
                            img(src = "${StatView.ICONS}/info.svg", className = "pf-icon pf-info")
                        }
                    }
                }
                span(className = "pv-upgrade-level") {
                    if (source.level != null && source.maxLevel != null) +"Lvl ${source.level}/${source.maxLevel}"
                }
                bar(source.value, source.max, "pv-bar")
                span(className = "pv-upgrade-gain") {
                    if (done) img(src = "${StatView.ICONS}/check.svg", className = "pf-icon pv-ok")
                    else +"+${number(source.max - source.value)} missing"
                }
            }
        }
    }

    private fun NodeBuilder.bar(value: Double, max: Double, className: String) {
        val percent = if (max <= 0) 0.0 else (value / max * 100).coerceIn(0.0, 100.0)
        div(className = className) { div(className = "pv-bar-fill", style = "width: ${String.format(Locale.ROOT, "%.2f", percent)}%") }
    }

    private fun NodeBuilder.partyView(category: CategoryDef, players: List<ViewedProfile>, onOpen: (String) -> Unit) {
        val ids = statIds(category).filter { it !in GENERAL_STATS }
        val varying = varyingStats(ids, players.firstOrNull()?.stats?.get(category.id).orEmpty())
        partyHead(category.label, "pf-type-${category.id}")
        scroll(className = "pv-scroll guilib-autohide") {
            div(className = "pv-table") {
                playerRow(players, onOpen)
                if (players.any { it.networth != null }) {
                    div(className = "pv-table-row") {
                        div(className = "pv-table-label") { +"Networth" }
                        players.forEach { player -> div(className = "pv-table-cell", key = player.name) { pieces(listOf(player.networth?.let(::networthPiece) ?: Piece("no data"))) } }
                    }
                }
                (listOf("sbLevel") + ids.filter { it !in varying }).forEach { id ->
                    tableRow(id, null, players) { merged(id, it.stats[category.id].orEmpty()) }
                }
                if (varying.size == 1) {
                    val id = varying.first()
                    category.subcategories.forEach { sub -> tableRow(id, sub.label, players) { it.stats[category.id]?.get(sub.id)?.get(id) } }
                } else if (varying.isNotEmpty()) {
                    category.subcategories.forEach { sub ->
                        div(className = "pv-table-group", key = "group:${sub.id}") { +sub.label }
                        varying.forEach { id -> tableRow(id, null, players, key = "${sub.id}:$id") { it.stats[category.id]?.get(sub.id)?.get(id) } }
                    }
                }
            }
        }
    }

    private fun NodeBuilder.partyCheckView(compare: CompareParty, players: List<ViewedProfile>, onOpen: (String) -> Unit) {
        val target = PartyCategories.target(compare.partyType, compare.subType)
        val ids = target?.reqs?.map { it.stat }.orEmpty().filter { id -> compare.reqs[id]?.let { it !is JsonNull } == true }
        fun valueOf(player: ViewedProfile, id: String): JsonElement? {
            val bySub = player.stats[compare.partyType].orEmpty()
            return (bySub[compare.subType] ?: bySub.values.firstOrNull())?.get(id)
        }
        partyHead("Party Check", "pv-check-heading")
        compareBanner(compare)
        if (ids.isEmpty()) return message("Your party has no requirements.")
        scroll(className = "pv-scroll guilib-autohide") {
            div(className = "pv-table") {
                playerRow(players, onOpen)
                div(className = "pv-table-row pv-table-summary") {
                    div(className = "pv-table-label") { +"Requirements met" }
                    players.forEach { player ->
                        val met = ids.count { id -> markFor(compare, id, valueOf(player, id))?.first == true }
                        div(className = classNames("pv-table-cell", "ok" to (met == ids.size), "bad" to (met < ids.size)), key = player.name) {
                            +"$met of ${ids.size}"
                        }
                    }
                }
                ids.forEach { id ->
                    div(className = "pv-table-row", key = id) {
                        div(className = "pv-table-label") {
                            statLabel(id)
                            markFor(compare, id, null)?.let { div(className = "pv-need") { +"needs ${it.second}" } }
                        }
                        players.forEach { player ->
                            val value = valueOf(player, id)
                            val mark = markFor(compare, id, value)
                            div(className = classNames("pv-table-cell", "ok" to (mark?.first == true), "bad" to (mark?.first == false)), key = player.name) {
                                span(className = classNames("estimated" to StatView.estimated(id), "pf-muted" to (value == null || value is JsonNull))) {
                                    compactPieces(id, value)
                                }
                                if (mark?.first == false) span(className = "pv-bad") { +" ✕" }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun NodeBuilder.partyHead(title: String, headingClass: String) {
        div(className = "pf-toolbar pv-toolbar") {
            h2(className = "pf-heading $headingClass") { +title }
            if (partyUuids.size > ProfileViewerApi.MAX_OTHERS + 1) span(className = "pf-muted pv-party-age") { +"Only the first ${ProfileViewerApi.MAX_OTHERS + 1} members" }
        }
    }

    private fun NodeBuilder.playerRow(players: List<ViewedProfile>, onOpen: (String) -> Unit) {
        div(className = "pv-table-row pv-table-head") {
            div(className = "pv-table-label")
            players.forEach { player ->
                div(className = "pv-table-cell pv-table-player", key = player.name, title = "Open ${player.name}", onClick = { onOpen(player.name) }) {
                    playerHead(uuidOf(player.uuid), className = "pf-head")
                    span(className = "pv-table-name") { +player.name }
                }
            }
        }
    }

    private fun NodeBuilder.tableRow(
        id: String,
        suffix: String?,
        players: List<ViewedProfile>,
        key: String = suffix?.let { "$id:$it" } ?: id,
        valueOf: (ViewedProfile) -> JsonElement?
    ) {
        div(className = "pv-table-row", key = key) {
            div(className = "pv-table-label") {
                statLabel(id)
                suffix?.let { div(className = "pv-need") { +it } }
            }
            players.forEach { player ->
                val value = valueOf(player)
                div(className = "pv-table-cell", key = player.name) {
                    span(className = classNames("estimated" to StatView.estimated(id), "pf-muted" to (value == null || value is JsonNull))) {
                        compactPieces(id, value)
                    }
                }
            }
        }
    }

    // Item lists are long, so table cells show the first item and the rest on hover
    private fun NodeBuilder.compactPieces(id: String, value: JsonElement?) {
        val all = StatView.valuePieces(id, value)
        val items = all.filter { it.text != ", " }
        if (value !is JsonArray || items.size <= 1) return pieces(all)
        tooltip(content = { div(className = "pf-tip-text") { pieces(all) } }, className = "pf-tip") {
            span {
                pieces(listOf(items.first()))
                span(className = "pf-muted") { +" +${items.size - 1}" }
            }
        }
    }

    private fun statIds(category: CategoryDef): List<String> {
        val all = (category.reqs.map { it.stat } + category.display +
            category.subcategories.flatMap { sub -> sub.reqs.map { it.stat } + sub.display }).distinct()
        return all.filter { SHOWN_BY[it] !in all }
    }

    private fun perSubTitle(categoryId: String): String = when (categoryId) {
        "kuudra" -> "Per tier"
        "slayer" -> "Per boss"
        "fishing" -> "Per fishing type"
        else -> "Per type"
    }

    private fun networthPiece(value: Double): Piece {
        val text = when {
            value >= 1e9 -> String.format(Locale.US, "%.2fB", value / 1e9)
            value >= 1e6 -> String.format(Locale.US, "%.1fM", value / 1e6)
            value >= 1e3 -> String.format(Locale.US, "%.0fk", value / 1e3)
            else -> String.format(Locale.US, "%.0f", value)
        }
        return Piece(text, "pf-c-gold")
    }

    private fun number(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.1f", value)

    private fun minutesSeconds(ms: Long): String {
        val seconds = ms / 1000
        return if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"
    }

    private fun NodeBuilder.message(text: String) {
        div(className = "pf-message") { p { +text } }
    }
}
