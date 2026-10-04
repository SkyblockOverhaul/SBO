package net.sbo.mod.partyfinder.gui

import kotlinx.serialization.json.JsonNull
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.b
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.checkbox
import net.sbo.guilib.core.dsl.chips
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.collapse
import net.sbo.guilib.core.dsl.contextMenu
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h3
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.input
import net.sbo.guilib.core.dsl.modal
import net.sbo.guilib.core.dsl.numberInput
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.playerHead
import net.sbo.guilib.core.dsl.radioGroup
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.multiSelect
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.textarea
import net.sbo.guilib.core.dsl.tooltip
import net.sbo.guilib.core.dsl.useClipboard
import net.sbo.guilib.core.dsl.useToast
import net.sbo.guilib.core.event.KeyboardEvent
import net.sbo.mod.partyfinder.OwnStats
import net.sbo.mod.partyfinder.PartyCategories
import net.sbo.mod.partyfinder.PartyCheck
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.partyfinder.PartyListFilters
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.ReqMatcher
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PartyReportBody
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.partyfinder.api.ReportReason
import net.sbo.mod.partyfinder.api.Problem
import net.sbo.mod.partyfinder.api.ReqDef
import net.sbo.mod.partyfinder.gui.PartyFinderGui.message
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.partyfinder.PartyListFilter
import java.util.UUID

internal data class PartiesProps(
    val target: PartyTarget,
    val own: MemberView?,
    // Own stats for the subcategory of a party, differs from own only in the list of all subcategories
    val ownFor: (PartyTarget) -> MemberView?,
    val ownError: String?,
    val reload: Int,
    val queuedAt: Long,
    val inQueue: Boolean,
    val onEdit: () -> Unit,
    // Uuid of the player shown in the side panel
    val inspected: String?,
    val onInspect: (InspectedPlayer) -> Unit,
    // Reloads the list after the player joined a party
    val joinedParties: Int,
    val onRules: () -> Unit = {}
)

/** The parties of one party type with filters, details, the right click menu and joining. */
internal val PartiesPage = component<PartiesProps>("PartiesPage") { props ->
    val target = props.target
    val config = DataManager.partyFinderConfigState
    var parties by useState<List<PartyView>?>(null)
    var error by useState<String?>(null)
    var loading by useState(false)
    var search by useState("")
    var filtersOpen by useState(false)
    var filterVersion by useState(0)
    var hidden by useState(setOf<String>())
    var expanded by useState<String?>(null)
    var joining by useState<PartyView?>(null)
    var role by useState<String?>(null)
    var reporting by useState<PartyView?>(null)
    val refresh = useState(0)
    val loadKey = useRef("")
    // Set by the refresh button and F5
    val manual = useRef(false)
    val toast = useToast()
    val clipboard = useClipboard()

    val autoRefresh = config.autoRefreshSeconds
    useEffect(target.key, props.reload, props.queuedAt, props.joinedParties, refresh.value, autoRefresh) {
        val key = target.key
        if (loadKey.current != key) {
            parties = null
            error = null
        }
        loadKey.current = key
        fun load() {
            loading = true
            PartyFinderManager.listParties(target.partyType, target.subType, onComplete = { list ->
                if (loadKey.current == key) {
                    parties = list
                    error = null
                    loading = false
                    if (manual.current) toast.success("Party list refreshed, ${list.size} ${if (list.size == 1) "party" else "parties"} found.")
                }
                manual.current = false
            }, onError = { e ->
                if (loadKey.current == key) {
                    error = ProblemText.error(e)
                    loading = false
                    if (manual.current) toast.error("Could not refresh the party list. ${ProblemText.error(e)}")
                }
                manual.current = false
            })
        }
        load()
        if (autoRefresh > 0) setInterval(autoRefresh * 1000L) { load() }
    }

    fun refreshNow() {
        if (loading) return
        manual.current = true
        refresh.update { it + 1 }
    }

    useDocumentEvent("keydown") { e ->
        e as KeyboardEvent
        if (e.key == "F5" && !e.repeat) {
            e.preventDefault()
            refreshNow()
        }
    }

    // filterVersion makes the page read the saved filter again after a change
    val filter = filterVersion.let { config.listFilters[target.key] ?: PartyListFilter() }
    fun setFilter(block: PartyListFilter.() -> Unit) {
        val next = filter.edited(block)
        if (next == PartyListFilter()) config.listFilters.remove(target.key) else config.listFilters[target.key] = next
        config.save()
        filterVersion++
    }

    val me = props.own
    val myId = OwnStats.uuid()

    // Parties with another slayer tier (or other fields stats depend on) need own stats for it
    val ownVersion = useRef(0)
    var ownLoaded by useState(0)
    val ownLoading = useRef(setOf<String>())
    fun meFor(party: PartyView, partyTarget: PartyTarget): MemberView? =
        if (OwnStats.statOptions(partyTarget, party.options) == OwnStats.statOptions(partyTarget)) props.ownFor(partyTarget)
        else OwnStats.cached(partyTarget, party.options)
    useEffect(parties) {
        parties.orEmpty().forEach { party ->
            val partyTarget = target.forParty(party)
            val options = OwnStats.statOptions(partyTarget, party.options)
            val key = "${partyTarget.key}?$options"
            if (options == OwnStats.statOptions(partyTarget) || OwnStats.cached(partyTarget, options) != null || key in ownLoading.current) return@forEach
            ownLoading.current = ownLoading.current + key
            OwnStats.get(partyTarget, options, onError = { ownLoading.current = ownLoading.current - key }) {
                ownLoading.current = ownLoading.current - key
                ownVersion.current++
                ownLoaded = ownVersion.current
            }
        }
    }
    val all = parties.orEmpty().filter { it.id !in hidden }
    val visible = PartyListFilters.apply(all, filter, target, me, myId, search, ::meFor)

    fun copy(text: String, what: String) {
        clipboard.set(text)
        toast.info("$what copied. Paste it with Ctrl+V.")
    }

    fun join(party: PartyView) {
        if (party.roles.wanted.isEmpty()) {
            PartyFinderManager.sendJoinRequest(party)
        } else {
            role = party.roles.wanted.firstOrNull()
            joining = party
        }
    }

    div(className = "pf-list-bar") {
        chips(
            values = listOfNotNull("canJoin".takeIf { filter.canJoin }, "notFull".takeIf { filter.notFull }),
            onChange = { values -> setFilter { canJoin = "canJoin" in values; notFull = "notFull" in values } },
            className = "pf-filters"
        ) {
            option("canJoin", "Can I join")
            option("notFull", "Not full")
        }
        val count = filter.dialogCount()
        // Looks like the quick filter chips next to it
        div(
            className = classNames("guilib-chip", "pf-filter-button", "selected" to (count > 0)),
            title = "More filters and sorting",
            onClick = { filtersOpen = true }
        ) {
            img(src = "${StatView.ICONS}/filter.svg", className = "pf-icon")
            +(if (count > 0) "Filters ($count)" else "Filters")
        }
        input(
            type = "text",
            value = search,
            onChange = { search = it.value },
            placeholder = "Search",
            title = "Search a player name or a note",
            className = "pf-search"
        )
        span(className = "pf-count") {
            +"${visible.size} ${if (visible.size == 1) "party" else "parties"}"
        }
        if (hidden.isNotEmpty()) {
            // Looks like the filter chips in the same bar
            div(className = "guilib-chip", title = "Show the parties you hid", onClick = { hidden = emptySet() }) {
                +"Show ${hidden.size} hidden"
            }
        }
        div(className = "pf-spacer")
        button(
            className = classNames("pf-icon-button", "loading" to loading),
            title = "Refresh the list (F5)",
            onClick = { refreshNow() }
        ) {
            img(src = "${StatView.ICONS}/refresh.svg", className = "pf-icon")
        }
    }
    if (me == null && props.ownError != null) {
        div(className = "pf-banner") {
            +"Your own stats could not be loaded, so SBO can not tell which parties you can join. ${props.ownError}"
        }
    }

    scroll(className = "pf-list guilib-autohide") {
        when {
            parties == null && error != null -> message("Could not load the parties. $error")
            parties == null -> message("Loading parties...")
            visible.isEmpty() && all.isEmpty() -> message("There are no ${target.label} parties right now. Create one under \"Create Party\".")
            visible.isEmpty() -> message("No party matches your filters.") {
                button(onClick = {
                    search = ""
                    setFilter { canJoin = false; notFull = false; sizes.clear(); minFreeSlots = 0; options.clear(); roles.clear(); reqs.clear() }
                }) { +"Clear filters" }
            }
            else -> visible.forEach { party ->
                val partyTarget = target.forParty(party)
                partyCard(
                    party, partyTarget, meFor(party, partyTarget),
                    showSub = target.all,
                    mine = party.id == myId,
                    expanded = party.id == expanded,
                    onToggle = { expanded = if (expanded == party.id) null else party.id },
                    onJoin = { join(party) },
                    onHide = { hidden = hidden + party.id },
                    onReport = { reporting = party },
                    onEdit = props.onEdit,
                    inspected = props.inspected,
                    onInspect = props.onInspect,
                    copy = ::copy,
                    checkStats = { name ->
                        PartyCheck.checkPlayer(name)
                        toast.info("The stats of $name are shown in chat.")
                    },
                    partyCommand = { command, text ->
                        Chat.command(command)
                        toast.info(text)
                    }
                )
            }
        }
        if (visible.isNotEmpty()) {
            div(className = "pf-legend") {
                +"Requirements: "
                span(className = "pf-legend-ok") { +"you meet it" }
                +", "
                span(className = "pf-legend-bad") { +"you don't" }
                +". A + means at least this much, a ~ means estimated. Hover a value for details, click a player for all stats. Right click a party or player for more."
            }
        }
    }

    filterDialog(filtersOpen, target, filter, ::setFilter, onClose = { filtersOpen = false })

    modal(open = joining != null, onClose = { joining = null }, className = "pf-dialog") {
        val party = joining ?: return@modal
        h3 { +"Join ${party.leader?.name ?: "party"}" }
        p(className = "pf-hint") { +"This party looks for these roles. Pick the one you want to play." }
        radioGroup(value = role, onChange = { role = it }, vertical = true) {
            party.roles.wanted.forEach { id -> option(id, target.roles.firstOrNull { it.id == id }?.label ?: id) }
        }
        div(className = "pf-dialog-buttons") {
            button(onClick = { joining = null }) { +"Cancel" }
            button(className = "primary", disabled = role == null, onClick = {
                PartyFinderManager.sendJoinRequest(party, role)
                joining = null
            }) { +"Send join request" }
        }
    }

    reporting?.let { party ->
        ReportDialog(
            ReportProps(party, onClose = { reporting = null }, onSent = { hidden = hidden + party.id; reporting = null }, onRules = {
                reporting = null
                props.onRules()
            }),
            key = "report:${party.id}"
        )
    }
}

internal data class ReportProps(val party: PartyView, val onClose: () -> Unit, val onSent: () -> Unit, val onRules: () -> Unit)

/** Asks for a reason and sends the report to the moderators on the SBO Discord. */
private val ReportDialog = component<ReportProps>("ReportDialog") { props ->
    var reason by useState<ReportReason?>(null)
    var details by useState("")
    var sending by useState(false)
    val toast = useToast()
    val tooShort = reason == ReportReason.OTHER && details.trim().length < ReportReason.MIN_OTHER_DETAILS

    fun send(picked: ReportReason) {
        sending = true
        val body = PartyReportBody(props.party.id, picked.id, details.trim().ifEmpty { null })
        PartyFinderApi.reportParty(body, onError = { e ->
            sending = false
            toast.error(reportError(e))
        }) {
            toast.success("Thanks, the moderators got your report. The party is now hidden for you.")
            props.onSent()
        }
    }

    modal(open = true, onClose = props.onClose, className = "pf-dialog pf-report-dialog") {
        h3 { +"Report ${props.party.leader?.name ?: "this"}'s party" }
        p(className = "pf-hint") {
            +"Moderators on the SBO Discord see your report together with your name and the party. Please only report parties that really break the "
            span(className = "pf-link", onClick = { props.onRules() }) { +"rules" }
            +"."
        }
        radioGroup(value = reason?.id, onChange = { reason = ReportReason.of(it) }, vertical = true) {
            ReportReason.entries.forEach { option(it.id, it.label) }
        }
        textarea(
            value = details,
            onChange = { e -> details = e.value },
            placeholder = if (reason == ReportReason.OTHER) "Describe the problem in a few words" else "More details for the moderators",
            rows = 3,
            maxLength = ReportReason.MAX_DETAILS,
            className = "pf-report-input"
        )
        if (tooShort) {
            p(className = "pf-hint") { +"Please write at least ${ReportReason.MIN_OTHER_DETAILS} characters, so the moderators know what happened." }
        }
        div(className = "pf-dialog-buttons") {
            button(onClick = { props.onClose() }) { +"Cancel" }
            val blocked = reason == null || tooShort || sending
            // New key per state: GuiLib keeps the disabled text color after the button turns on until the next full repaint
            button(className = "primary", disabled = blocked, onClick = { reason?.let(::send) }, key = "send:$blocked") {
                +"Send report"
            }
        }
    }
}

/** The backend explains report problems itself, e.g. "You already reported this party". */
private fun reportError(e: PfError): String = when (e.code) {
    PfError.INVALID_REQUEST, PfError.RATE_LIMITED, PfError.REPORT_NOT_ALLOWED, PfError.PARTY_NOT_FOUND ->
        "${e.message.trimEnd('.')}."
    else -> ProblemText.error(e)
}

/** More filters and the sorting. Changes apply right away. */
private fun NodeBuilder.filterDialog(
    open: Boolean,
    target: PartyTarget,
    filter: PartyListFilter,
    setFilter: (PartyListFilter.() -> Unit) -> Unit,
    onClose: () -> Unit
) {
    modal(open = open, onClose = onClose, className = "pf-dialog pf-filter-dialog") {
        h3 { +"Filters" }
        p(className = "pf-hint") { +"Only parties that match everything you set here are shown. Your own party is always shown." }
        div(className = "pf-filter-list") {
            filterRow("Party size", "Only parties of these sizes. Pick none to see all sizes.") {
                chips(values = filter.sizes.map { it.toString() }, onChange = { values -> setFilter { sizes = values.map { it.toInt() }.toMutableList() } }) {
                    for (size in target.minSize..target.maxSize) option(size.toString(), sizeLabel(size))
                }
            }
            filterRow("Free spots", "Only parties with at least this many free spots.") {
                numberInput(
                    value = filter.minFreeSlots.takeIf { it > 0 },
                    onChange = { v -> setFilter { minFreeSlots = v ?: 0 } },
                    allowEmpty = true,
                    min = 1,
                    max = target.maxSize - 1
                )
            }
            PartyListFilters.filterableOptions(target).forEach { partyOption ->
                if (PartyListFilters.filtersSeveral(partyOption)) {
                    // Old saved filters hold one value, a comma list of one
                    filterRow(partyOption.label, "Only parties with one of these. Parties set to \"Any\" are always shown.", key = partyOption.id) {
                        multiSelect(
                            values = partyOption.picks(filter.options[partyOption.id]),
                            onChange = { ids -> setFilter { if (ids.isEmpty()) options.remove(partyOption.id) else options[partyOption.id] = partyOption.picks(ids.joinToString(",")).joinToString(",") } },
                            placeholder = "Show all"
                        ) {
                            PartyListFilters.filterValues(partyOption).forEach { option(it.id, it.label) }
                        }
                    }
                    return@forEach
                }
                filterRow(partyOption.label, null, key = partyOption.id) {
                    val shown = filter.options[partyOption.id]?.takeIf { value -> PartyListFilters.filterValues(partyOption).any { it.id == value } }
                    select(value = shown ?: "", onChange = { e ->
                        setFilter { if (e.value.isEmpty()) options.remove(partyOption.id) else options[partyOption.id] = e.value }
                    }) {
                        option("", "Show all")
                        PartyListFilters.filterValues(partyOption).forEach { option(it.id, it.label) }
                    }
                }
            }
            val reqs = PartyListFilters.filterableReqs(target)
            if (reqs.isNotEmpty()) {
                div(className = "pf-filter-section") {
                    div { +"Requirements" }
                    div(className = "pf-hint") { +"Only parties that ask for at least this much. Parties that don't ask for it are hidden." }
                }
                reqs.forEach { def -> reqFilterRow(def, filter.reqs[def.stat], setFilter) }
            }
            if (target.roles.isNotEmpty()) {
                filterRow("My roles", "Only parties that look for one of these roles. Parties that don't ask for roles take everyone.") {
                    chips(values = filter.roles, onChange = { values -> setFilter { roles = values.toMutableList() } }) {
                        target.roles.forEach { option(it.id, it.label) }
                    }
                }
            }
            filterRow("Sort by", "Parties that wait longest come first, also when two parties are equal.") {
                val sorts = listOf(PartyListFilter.SORT_MOST_FREE, PartyListFilter.SORT_ALMOST_FULL)
                select(value = filter.sort.takeIf { it in sorts } ?: PartyListFilter.SORT_DEFAULT, onChange = { e -> setFilter { sort = e.value } }) {
                    option(PartyListFilter.SORT_DEFAULT, "Default")
                    option(PartyListFilter.SORT_MOST_FREE, "Most free spots")
                    option(PartyListFilter.SORT_ALMOST_FULL, "Almost full first")
                }
            }
        }
        div(className = "pf-dialog-buttons") {
            button(onClick = {
                setFilter { sizes.clear(); minFreeSlots = 0; options.clear(); roles.clear(); reqs.clear(); sort = PartyListFilter.SORT_DEFAULT }
            }) { +"Reset filters" }
            button(className = "primary", onClick = { onClose() }) { +"Done" }
        }
    }
}

/** One requirement in the filter dialog; an empty field shows all parties. */
private fun NodeBuilder.reqFilterRow(def: ReqDef, wanted: String?, setFilter: (PartyListFilter.() -> Unit) -> Unit) {
    val stat = PartyCategories.stat(def.stat)
    fun save(value: String?) = setFilter { if (value == null) reqs.remove(def.stat) else reqs[def.stat] = value }
    filterRow(stat?.label ?: def.stat, null, key = "req:${def.stat}") {
        val labels = stat?.valueLabels.orEmpty()
        when {
            def.type == "flag" -> checkbox(checked = wanted == "true", onChange = { e -> save(if (e.checked) "true" else null) }, label = "Party requires it")
            def.type == "rarity" -> select(value = wanted ?: "", onChange = { e -> save(e.value.ifEmpty { null }) }) {
                option("", "Show all")
                // Common asks for nothing, like in create
                ReqMatcher.RARITIES.drop(1).forEach { option(it, ProblemText.rarityNeed(it), className = StatView.rarityColor(it)) }
            }
            labels.isNotEmpty() -> select(
                value = wanted ?: "",
                onChange = { e -> save(e.value.ifEmpty { null }) }
            ) {
                option("", "Show all")
                labels.forEachIndexed { i, label ->
                    if (i > 0) option(i.toString(), ProblemText.orBetter(label, i == labels.lastIndex), className = StatView.numberColor(def.stat, i.toDouble()))
                }
            }
            else -> numberInput(
                value = wanted?.toDoubleOrNull()?.toInt(),
                onChange = { v -> save(v?.takeIf { it > 0 }?.toString()) },
                allowEmpty = true,
                min = 1,
                max = stat?.max ?: Int.MAX_VALUE,
                className = wanted?.toDoubleOrNull()?.let { StatView.numberColor(def.stat, it) }
            )
        }
    }
}

private fun NodeBuilder.filterRow(label: String, hint: String?, key: Any? = null, control: NodeBuilder.() -> Unit) {
    div(className = "pf-filter-row", key = key ?: label) {
        div(className = "pf-filter-label") {
            div { +label }
            if (hint != null) div(className = "pf-hint") { +hint }
        }
        div(className = "pf-filter-input") { control() }
    }
}

private fun NodeBuilder.partyCard(
    party: PartyView,
    target: PartyTarget,
    me: MemberView?,
    showSub: Boolean,
    mine: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onJoin: () -> Unit,
    onHide: () -> Unit,
    onReport: () -> Unit,
    onEdit: () -> Unit,
    inspected: String?,
    onInspect: (InspectedPlayer) -> Unit,
    copy: (String, String) -> Unit,
    checkStats: (String) -> Unit,
    partyCommand: (command: String, text: String) -> Unit
) {
    val leaderName = party.leader?.name?.takeIf { it.isNotBlank() } ?: "Unknown"
    val full = ReqMatcher.isFull(party)
    val problems = if (me != null && !mine) ReqMatcher.checkJoin(party, target, me) else emptyList()

    contextMenu(menu = {
        header(leaderName)
        item("Copy note", disabled = party.note.isBlank()) { copy(party.note, "Note") }
        item("Copy leader name") { copy(leaderName, "Name") }
        if (mine) {
            item("Edit party") { onEdit() }
            separator()
            item("Remove from queue", danger = true) { PartyFinderManager.removePartyFromQueue() }
        } else {
            item("Join party", disabled = full) { onJoin() }
            separator()
            item("Hide this party") { onHide() }
            item("Report party…", danger = true) { onReport() }
        }
    }, className = "pf-card-anchor", key = party.id) {
        div(className = classNames("pf-card", "mine" to mine, "full" to full, "expanded" to expanded), onClick = { onToggle() }) {
            div(className = "pf-card-head") {
                party.leader?.let { playerHead(uuidOf(it.uuid), className = "pf-head") }
                b(className = "pf-leader") { +leaderName }
                // In the list of all subcategories every party says which one it was created in
                if (showSub) target.sub?.let { span(className = "pf-tag sub", title = "Created as a ${target.label} party") { +it.label } }
                if (mine) span(className = "pf-tag mine") { +"Your party" }
                span(className = "pf-tag", title = "Players in the party / party size") { +"${party.memberCount}/${party.partySize}" }
                if (party.partySize < target.maxSize) span(className = "pf-tag size") { +sizeLabel(party.partySize) }
                div(className = "pf-spacer")
                span(className = "pf-age") { +ago(party.createdAt) }
                if (!mine) joinButton(full, problems, target, onJoin)
            }
            // Party fields get their own row that wraps, long ones would push the join button out of the window
            val tags = buildList {
                target.opensAt?.takeIf { !target.open }?.let { add("Event starts in ${until(it)}") }
                target.options.forEach { option ->
                    val value = party.options[option.id] ?: return@forEach
                    if (value == "any" || value.isEmpty()) return@forEach
                    val label = if (option.multiple) {
                        val picks = option.picks(value)
                        if (picks.size == option.values.size) "All"
                        else picks.mapNotNull { id -> option.values.firstOrNull { it.id == id }?.label }.joinToString(", ")
                    } else {
                        option.values.firstOrNull { it.id == value }?.label ?: value
                    }
                    add("${option.label}: $label")
                }
            }
            if (tags.isNotEmpty()) div(className = "pf-card-tags") { tags.forEach { span(className = "pf-tag option") { +it } } }
            if (party.note.isNotBlank()) div(className = "pf-note") { +party.note }
            div(className = "pf-reqs") {
                var any = false
                target.reqs.forEach { def ->
                    val need = party.reqs[def.stat] ?: return@forEach
                    if (need is JsonNull) return@forEach
                    any = true
                    val meets = if (mine) null else StatView.meets(def, need, me)
                    tooltip(content = { statInfo(def.stat, me) }, className = "pf-tip") {
                        span(className = classNames("pf-req", "ok" to (meets == true), "bad" to (meets == false))) {
                            span(className = "pf-req-label") { +"${StatView.label(def.stat)}: " }
                            pieces(StatView.needPieces(def, need))
                        }
                    }
                }
                if (!any) span(className = "pf-muted") { +"No requirements" }
            }
            if (party.roles.wanted.isNotEmpty()) {
                div(className = "pf-roles") {
                    +"Looking for: "
                    +party.roles.wanted.joinToString(", ") { id -> target.roles.firstOrNull { it.id == id }?.label ?: id }
                }
            }
            collapse(open = expanded) {
                // Clicks on players keep the card open
                div(className = "pf-members", onClick = { e -> e.stopPropagation() }) {
                    // Only plain values here, item lists and the all tiers breakdown stay in the player panel
                    val statIds = (target.reqs.map { it.stat }.filter { party.reqs[it] != null } + target.display).distinct()
                        .filter { PartyCategories.stat(it)?.kind !in setOf("items", "breakdown") }
                    party.members.forEach { member ->
                        memberRow(
                            member, party, target, statIds, member.uuid == inspected, { onInspect(InspectedPlayer(member, party)) },
                            manage = mine && member.uuid != party.id, copy, checkStats, partyCommand
                        )
                    }
                }
            }
        }
    }
}

private fun NodeBuilder.joinButton(full: Boolean, problems: List<Problem>, target: PartyTarget, onJoin: () -> Unit) {
    val reason = when {
        full -> "This party is full."
        problems.isNotEmpty() -> "You don't meet: " + problems.joinToString("; ") { ProblemText.describe(it, target) }
        else -> null
    }
    val button: NodeBuilder.() -> Unit = {
        button(className = "pf-join primary", disabled = reason != null, onClick = { e ->
            e.stopPropagation()
            onJoin()
        }) { +"Join" }
    }
    if (reason == null) button() else tooltip(reason, placement = "left", className = "pf-tip") { button() }
}

private fun NodeBuilder.memberRow(
    member: MemberView,
    party: PartyView,
    target: PartyTarget,
    statIds: List<String>,
    inspected: Boolean,
    onInspect: () -> Unit,
    // Other members of the own party: transfer and kick
    manage: Boolean,
    copy: (String, String) -> Unit,
    checkStats: (String) -> Unit,
    partyCommand: (command: String, text: String) -> Unit
) {
    val name = member.name.ifBlank { "Unknown" }
    contextMenu(menu = {
        header(name)
        item("Show all stats") { onInspect() }
        item("Copy name") { copy(name, "Name") }
        item("Check stats") { checkStats(name) }
        if (manage) {
            separator()
            item("Make party leader") { partyCommand("p transfer $name", "Making $name the party leader...") }
            // Promoting a moderator would make them the leader, so promote only known plain members
            when (HypixelModApi.partyRole(member.uuid)) {
                "MEMBER" -> item("Make moderator") {
                    HypixelModApi.markRole(member.uuid, "MODERATOR")
                    partyCommand("p promote $name", "Making $name a party moderator...")
                }
                "MODERATOR" -> item("Remove moderator") {
                    HypixelModApi.markRole(member.uuid, "MEMBER")
                    partyCommand("p demote $name", "Making $name a normal member again...")
                }
            }
            item("Kick from party", danger = true) { partyCommand("p kick $name", "Kicking $name from the party...") }
        }
    }, className = "pf-member-anchor", key = member.uuid) {
        div(className = classNames("pf-member", "inspected" to inspected), onClick = { onInspect() }) {
            span(className = "pf-member-name") {
                playerHead(uuidOf(member.uuid), className = "pf-head")
                +name
                if (member.uuid == party.id) span(className = "pf-member-role") { +" leader" }
                else if (manage && HypixelModApi.partyRole(member.uuid) == "MODERATOR") span(className = "pf-member-role") { +" moderator" }
                member.role?.let { id ->
                    span(className = "pf-member-role") { +" as ${target.roles.firstOrNull { it.id == id }?.label ?: id}" }
                }
            }
            div(className = "pf-member-stats") {
                statIds.forEach { id ->
                    val value = member.stats[id]
                    val lastEvent = member.reported[id]?.scope == "lastEvent"
                    tooltip(content = { statInfo(id) }, className = "pf-tip", key = id) {
                        span(className = classNames("pf-stat", "estimated" to StatView.estimated(id), "missing" to (value == null || value is JsonNull))) {
                            span(className = "pf-stat-name") { +"${StatView.label(id)} " }
                            pieces(StatView.valuePieces(id, value))
                            if (lastEvent) span(className = "pf-muted") { +" (last event)" }
                        }
                    }
                }
            }
        }
    }
}

// Backend uuids have no dashes
internal fun uuidOf(id: String): Any =
    runCatching { UUID.fromString(id.replace(Regex("(.{8})(.{4})(.{4})(.{4})(.{12})"), "$1-$2-$3-$4-$5")) }.getOrDefault(id)
