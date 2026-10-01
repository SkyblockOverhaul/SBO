package net.sbo.mod.partyfinder.gui

import kotlinx.serialization.json.JsonNull
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.b
import net.sbo.guilib.core.dsl.button
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
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.tooltip
import net.sbo.guilib.core.dsl.useClipboard
import net.sbo.guilib.core.dsl.useToast
import net.sbo.guilib.core.event.KeyboardEvent
import net.sbo.mod.partyfinder.OwnStats
import net.sbo.mod.partyfinder.PartyCheck
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.partyfinder.PartyListFilters
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.ReqMatcher
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.partyfinder.api.Problem
import net.sbo.mod.partyfinder.gui.PartyFinderGui.message
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.partyfinder.PartyListFilter
import java.util.UUID

internal data class PartiesProps(
    val target: PartyTarget,
    val own: MemberView?,
    val ownError: String?,
    val reload: Int,
    // Reloads the list when the own party changes
    val queuedAt: Long,
    val inQueue: Boolean,
    val onEdit: () -> Unit
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
    val refresh = useState(0)
    val loadKey = useRef("")
    // Set by the refresh button and F5, so only those show a toast
    val manual = useRef(false)
    val toast = useToast()
    val clipboard = useClipboard()

    useEffect(target.key, props.reload, props.queuedAt, refresh.value) {
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
        setInterval(30_000) { load() }
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
    val all = parties.orEmpty().filter { it.id !in hidden }
    val visible = PartyListFilters.apply(all, filter, target, me, myId, search)

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
        button(
            className = classNames("pf-small", "pf-filter-button", "active" to (count > 0)),
            title = "More filters and sorting",
            onClick = { filtersOpen = true }
        ) { +(if (count > 0) "Filters ($count)" else "Filters") }
        input(
            type = "text",
            value = search,
            onChange = { search = it.value },
            placeholder = "Search player or note",
            className = "pf-search"
        )
        span(className = "pf-count") {
            +"${visible.size} ${if (visible.size == 1) "party" else "parties"}"
        }
        if (hidden.isNotEmpty()) {
            button(className = "pf-small", title = "Show the parties you hid", onClick = { hidden = emptySet() }) {
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

    scroll(className = "pf-list") {
        when {
            parties == null && error != null -> message("Could not load the parties. $error")
            parties == null -> message("Loading parties...")
            visible.isEmpty() && all.isEmpty() -> message("There are no ${target.label} parties right now. Create one under \"Create Party\".")
            visible.isEmpty() -> message("No party matches your filters.") {
                button(onClick = {
                    search = ""
                    setFilter { canJoin = false; notFull = false; sizes.clear(); minFreeSlots = 0; options.clear(); roles.clear() }
                }) { +"Clear filters" }
            }
            else -> visible.forEach { party ->
                partyCard(
                    party, target, me,
                    mine = party.id == myId,
                    expanded = party.id == expanded,
                    onToggle = { expanded = if (expanded == party.id) null else party.id },
                    onJoin = { join(party) },
                    onHide = { hidden = hidden + party.id },
                    onEdit = props.onEdit,
                    copy = ::copy,
                    checkStats = { name ->
                        PartyCheck.checkPlayer(name)
                        toast.info("The stats of $name are shown in chat.")
                    }
                )
            }
        }
        if (visible.isNotEmpty()) {
            div(className = "pf-legend") {
                +"Green: you meet it, red: you don't. A + means at least this much, a ~ means estimated. Hover a value for details. Right click a party or player for more."
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
                filterRow(partyOption.label, null, key = partyOption.id) {
                    select(value = filter.options[partyOption.id] ?: "", onChange = { e ->
                        setFilter { if (e.value.isEmpty()) options.remove(partyOption.id) else options[partyOption.id] = e.value }
                    }) {
                        option("", "Show all")
                        partyOption.values.forEach { option(it.id, it.label) }
                    }
                }
            }
            if (target.roles.isNotEmpty()) {
                filterRow("My roles", "Only parties that look for one of these roles. Parties that don't ask for roles take everyone.") {
                    chips(values = filter.roles, onChange = { values -> setFilter { roles = values.toMutableList() } }) {
                        target.roles.forEach { option(it.id, it.label) }
                    }
                }
            }
            filterRow("Sort by", "Parties that wait longest come first, also when two parties are equal.") {
                val sorts = listOf(PartyListFilter.SORT_MOST_FREE, PartyListFilter.SORT_ALMOST_FULL, PartyListFilter.SORT_FEWEST_REQS)
                select(value = filter.sort.takeIf { it in sorts } ?: PartyListFilter.SORT_DEFAULT, onChange = { e -> setFilter { sort = e.value } }) {
                    option(PartyListFilter.SORT_DEFAULT, "Default")
                    option(PartyListFilter.SORT_MOST_FREE, "Most free spots")
                    option(PartyListFilter.SORT_ALMOST_FULL, "Almost full first")
                    option(PartyListFilter.SORT_FEWEST_REQS, "Fewest requirements")
                }
            }
        }
        div(className = "pf-dialog-buttons") {
            button(onClick = {
                setFilter { sizes.clear(); minFreeSlots = 0; options.clear(); roles.clear(); sort = PartyListFilter.SORT_DEFAULT }
            }) { +"Reset filters" }
            button(className = "primary", onClick = { onClose() }) { +"Done" }
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
    mine: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onJoin: () -> Unit,
    onHide: () -> Unit,
    onEdit: () -> Unit,
    copy: (String, String) -> Unit,
    checkStats: (String) -> Unit
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
        }
    }, className = "pf-card-anchor", key = party.id) {
        div(className = classNames("pf-card", "mine" to mine, "full" to full, "expanded" to expanded), onClick = { onToggle() }) {
            div(className = "pf-card-head") {
                party.leader?.let { playerHead(uuidOf(it.uuid), className = "pf-head") }
                b(className = "pf-leader") { +leaderName }
                if (mine) span(className = "pf-tag mine") { +"Your party" }
                span(className = "pf-tag", title = "Players in the party / party size") { +"${party.memberCount}/${party.partySize}" }
                if (party.partySize < target.maxSize) span(className = "pf-tag size") { +sizeLabel(party.partySize) }
                target.options.forEach { option ->
                    val value = party.options[option.id] ?: return@forEach
                    if (value == "any") return@forEach
                    val label = option.values.firstOrNull { it.id == value }?.label ?: value
                    span(className = "pf-tag option") { +"${option.label}: $label" }
                }
                div(className = "pf-spacer")
                span(className = "pf-age") { +ago(party.createdAt) }
                if (!mine) joinButton(full, problems, target, onJoin)
            }
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
                            +StatView.need(def, need)
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
                    val statIds = (target.reqs.map { it.stat }.filter { party.reqs[it] != null } + target.display).distinct()
                    party.members.forEach { member -> memberRow(member, party, target, statIds, copy, checkStats) }
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
    copy: (String, String) -> Unit,
    checkStats: (String) -> Unit
) {
    val name = member.name.ifBlank { "Unknown" }
    contextMenu(menu = {
        header(name)
        item("Copy name") { copy(name, "Name") }
        item("Check stats") { checkStats(name) }
    }, className = "pf-member-anchor", key = member.uuid) {
        div(className = "pf-member") {
            span(className = "pf-member-name") {
                playerHead(uuidOf(member.uuid), className = "pf-head")
                +name
                if (member.uuid == party.id) span(className = "pf-member-role") { +" leader" }
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
                            +StatView.value(id, value)
                            if (lastEvent) span(className = "pf-muted") { +" (last event)" }
                        }
                    }
                }
            }
        }
    }
}

// Backend uuids have no dashes
private fun uuidOf(id: String): Any =
    runCatching { UUID.fromString(id.replace(Regex("(.{8})(.{4})(.{4})(.{4})(.{12})"), "$1-$2-$3-$4-$5")) }.getOrDefault(id)
