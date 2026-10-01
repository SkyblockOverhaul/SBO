package net.sbo.mod.partyfinder.gui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.checkbox
import net.sbo.guilib.core.dsl.chips
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h3
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.multiSelect
import net.sbo.guilib.core.dsl.numberInput
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.textarea
import net.sbo.guilib.core.dsl.useToast
import net.sbo.mod.partyfinder.PartyCategories
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.ReqMatcher
import net.sbo.mod.partyfinder.api.ItemChoice
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.ReqDef
import net.sbo.mod.utils.SboKey
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.partyfinder.PartyDraft

internal data class CreateProps(val target: PartyTarget, val own: MemberView?, val inQueue: Boolean)

private const val NOTE_MAX_LENGTH = 100
private const val NOTE_MAX_LINES = 3
private val json = Json { ignoreUnknownKeys = true }

/** Form for a new party (or a changed one while the own party is listed). Starts with the last input for this party type. */
internal val CreatePage = component<CreateProps>("CreatePage") { props ->
    val target = props.target
    val config = DataManager.partyFinderConfigState
    var draft by useStateLazy { startDraft(target) }
    val latest = useRef(draft)
    latest.current = draft
    val toast = useToast()
    val hasKey = SboKey.get().startsWith("sbo")

    // Keeps the input when switching pages or closing the window
    useEffect {
        onCleanup {
            config.drafts[target.key] = latest.current
            config.save()
        }
    }

    fun change(block: PartyDraft.() -> Unit) {
        draft = draft.edited(block)
    }

    fun submit() {
        val final = draft.edited {}
        config.drafts[target.key] = final
        config.save()
        if (PartyFinderManager.inQueue) {
            PartyFinderManager.removePartyFromQueue { removed -> if (removed) PartyFinderManager.createParty(final) }
        } else {
            PartyFinderManager.createParty(final)
        }
        toast.info("Creating your party, this takes a few seconds...")
    }

    scroll(className = "pf-form") {
        if (!target.open) {
            div(className = "pf-banner") {
                img(src = "${StatView.ICONS}/lock.svg", className = "pf-icon")
                +" ${target.label} parties can only be created while the event is running."
                target.opensAt?.let { +" It starts in ${until(it)}." }
            }
        }
        if (!hasKey) {
            div(className = "pf-banner") {
                +"You need an SBO key to create a party. Get one in the SBO Discord and set it with /sbokey <key>."
            }
        }

        h3(className = "pf-section") { +"Party size" }
        div(className = "pf-size-row") {
            numberInput(value = draft.partySize, onChange = { size -> change { partySize = size } }, min = target.minSize, max = target.maxSize)
            span(className = "pf-muted") { +(if (draft.partySize == target.maxSize) "full party" else sizeLabel(draft.partySize)) }
        }
        p(className = "pf-hint") { +"How many players your party should have, you included. ${target.label} parties can have up to ${target.maxSize}." }

        h3(className = "pf-section") { +"Requirements" }
        p(className = "pf-hint") { +"Players who don't meet these can't join. Leave a field empty or at \"Any\" for no requirement. Your own value is shown on the right." }
        div(className = "pf-fields") {
            target.reqs.forEach { def -> reqField(def, draft, props.own, ::change) }
        }

        val shownOptions = target.options
        if (shownOptions.isNotEmpty()) {
            h3(className = "pf-section") { +"Party settings" }
            div(className = "pf-fields") {
                shownOptions.forEach { partyOption ->
                    div(className = "pf-field", key = partyOption.id) {
                        span(className = "pf-field-label") { +partyOption.label }
                        div(className = "pf-field-input") {
                            segmented(value = draft.options[partyOption.id] ?: partyOption.default, onChange = { value -> change { options[partyOption.id] = value } }) {
                                partyOption.values.forEach { option(it.id, it.label) }
                            }
                        }
                    }
                }
            }
        }

        if (target.roles.isNotEmpty()) {
            h3(className = "pf-section") { +"Wanted roles" }
            p(className = "pf-hint") { +"Optional. If you pick roles, everyone who wants to join has to choose one of them." }
            chips(values = draft.wantedRoles, onChange = { roles -> change { wantedRoles = roles.toMutableList() } }) {
                target.roles.forEach { option(it.id, it.label) }
            }
        }

        h3(className = "pf-section") { +"Note" }
        textarea(
            value = draft.note,
            onChange = { e -> change { note = e.value } },
            placeholder = "What are you planning? e.g. \"chill party, need one more\"",
            rows = NOTE_MAX_LINES,
            maxLength = NOTE_MAX_LENGTH,
            maxLines = NOTE_MAX_LINES,
            className = "pf-note-input"
        )
        p(className = "pf-hint") {
            +"${draft.note.length}/$NOTE_MAX_LENGTH. Letters, numbers, spaces and , . ! ? - _ only, other characters are removed."
        }

        div(className = "pf-form-buttons") {
            button(onClick = { draft = PartyDraft(target.partyType, target.subType, target.maxSize) }) { +"Reset" }
            button(className = "primary", disabled = !target.open || !hasKey, onClick = { submit() }) {
                +(if (props.inQueue) "Update party" else "Create party")
            }
        }
        p(className = "pf-hint") {
            +"You must be alone or the party leader. Everyone already in your party has to meet the requirements too."
            if (props.inQueue) +" Updating replaces the party you have listed right now."
        }
    }
}

private fun startDraft(target: PartyTarget): PartyDraft {
    val queued = PartyFinderManager.draft?.takeIf { PartyFinderManager.inQueue && it.key == target.key }
    val saved = queued ?: DataManager.partyFinderConfigState.drafts[target.key]
    val draft = saved?.edited {} ?: PartyDraft(target.partyType, target.subType, target.maxSize)
    return draft.edited {
        partyType = target.partyType
        subType = target.subType
        partySize = target.clampSize(partySize)
    }
}

/** A copy with its own maps, so state changes never touch the saved config. */
private fun PartyDraft.edited(block: PartyDraft.() -> Unit): PartyDraft =
    copy(reqs = reqs.toMutableMap(), options = options.toMutableMap(), wantedRoles = wantedRoles.toMutableList()).apply(block)

private fun NodeBuilder.reqField(def: ReqDef, draft: PartyDraft, own: MemberView?, change: (PartyDraft.() -> Unit) -> Unit) {
    val stat = def.stat
    val saved = draft.reqs[stat]?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
    fun save(value: String?) = change { if (value == null) { reqs.remove(stat) } else { reqs[stat] = value } }

    div(className = classNames("pf-field", "wide" to (def.type == "anyOf")), key = stat) {
        statLabel(stat, own, className = "pf-field-label")
        div(className = "pf-field-input") {
            when (def.type) {
                "min" -> {
                    val number = (saved as? JsonPrimitive)?.doubleOrNull?.toInt()
                    val labels = PartyCategories.stat(stat)?.valueLabels.orEmpty()
                    if (labels.isNotEmpty()) {
                        select(value = (number ?: 0).toString(), onChange = { e -> save(e.value.takeIf { it != "0" }) }) {
                            option("0", "Any")
                            labels.forEachIndexed { i, label -> if (i > 0) option(i.toString(), "$label or better") }
                        }
                    } else {
                        numberInput(value = number, onChange = { v -> save(v?.takeIf { it > 0 }?.toString()) }, allowEmpty = true, min = 0, placeholder = "any")
                    }
                }
                "flag" -> checkbox(
                    checked = saved?.isTrue() == true,
                    onChange = { e -> save(if (e.checked) "true" else null) },
                    label = "Required"
                )
                "rarity" -> {
                    val rarity = (saved as? JsonPrimitive)?.contentOrNull ?: ""
                    select(value = rarity, onChange = { e -> save(e.value.takeIf { it.isNotEmpty() }?.let { "\"$it\"" }) }) {
                        option("", "Any")
                        ReqMatcher.RARITIES.forEach { option(it, "${title(it)} or better") }
                    }
                }
                "anyOf" -> anyOfInput(def, saved as? JsonArray, ::save)
            }
        }
        if (own != null) {
            val need = saved
            val meets = need?.let { StatView.meets(def, it, own) }
            val text = "You: ${StatView.value(stat, own.stats[stat])}"
            span(className = classNames("pf-own", "ok" to (meets == true), "bad" to (meets == false)), title = text) { +text }
        }
    }
}

/** Pick any of several items; sets with tiers (Kuudra armor) also get a lowest tier. */
private fun NodeBuilder.anyOfInput(def: ReqDef, saved: JsonArray?, save: (String?) -> Unit) {
    val picks = saved.orEmpty().mapNotNull { pick ->
        when (pick) {
            is JsonObject -> (pick["id"] as? JsonPrimitive)?.contentOrNull?.let { it to (pick["minTier"] as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> pick.contentOrNull?.let { it to null }
            else -> null
        }
    }
    fun store(next: List<Pair<String, String?>>) {
        if (next.isEmpty()) return save(null)
        save(buildJsonArray {
            next.forEach { (id, minTier) ->
                if (minTier == null) add(JsonPrimitive(id)) else add(buildJsonObject { put("id", id); put("minTier", minTier) })
            }
        }.toString())
    }

    div(className = "pf-any-of") {
        multiSelect(
            values = picks.map { it.first },
            onChange = { ids -> store(ids.map { id -> id to picks.firstOrNull { it.first == id }?.second }) },
            placeholder = "Any",
            searchable = def.choices.size > 8,
            searchPlaceholder = "Search..."
        ) {
            def.choices.forEach { option(it.id, it.label) }
        }
        picks.forEach { (id, minTier) ->
            val choice: ItemChoice = def.choices.firstOrNull { it.id == id } ?: return@forEach
            if (!choice.tiers) return@forEach
            div(className = "pf-tier-row", key = id) {
                span { +"${choice.label}, at least " }
                select(value = minTier ?: "", onChange = { e ->
                    store(picks.map { if (it.first == id) id to e.value.takeIf { v -> v.isNotEmpty() } else it })
                }) {
                    option("", "any tier")
                    ReqMatcher.KUUDRA_TIERS.forEach { option(it, ProblemText.title(it)) }
                }
            }
        }
    }
}
