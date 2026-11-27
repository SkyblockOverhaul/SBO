package net.sbo.mod.diana.guide

import net.minecraft.world.item.ItemStack
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.b
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.item
import net.sbo.guilib.core.dsl.span
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.game.ItemPrices

private val ROMAN = listOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

private fun roman(level: Int): String = ROMAN.getOrNull(level) ?: level.toString()

/** The picked slot is kept between openings of the window. The player comes back to the item they looked at. */
private var picked: String? = null

/**
 * The Loadout tab. The guide items are shown as inventory slots and the picked one is explained on the right.
 * The guide path is under it. Owned items show the own stack of the player with its tooltip. Missing ones show a faded
 * icon in a red slot.
 */
internal fun NodeBuilder.loadoutTab(guide: DianaGuide, scanned: List<ScannedSlot>) {
    LoadoutView(LoadoutProps(guide, scanned))
}

internal data class LoadoutProps(val guide: DianaGuide, val scanned: List<ScannedSlot>)

private val LoadoutView = component<LoadoutProps>("DianaLoadout") { (guide, scanned) ->
    var selected by useState(picked ?: (scanned.firstOrNull { it.state == SlotState.MISSING } ?: scanned.first()).slot.key)
    val shown = scanned.firstOrNull { it.slot.key == selected } ?: scanned.first()
    val next = scanned.firstOrNull { it.state == SlotState.MISSING }

    div(className = "dg-loadout") {
        div(className = "dg-inv") {
            listOf("Armor" to true, "Equipment" to true, "Pets" to false, "Hotbar" to false).forEach { (group, column) ->
                div(className = if (column) "dg-inv-group" else "dg-inv-group wide", key = group) {
                    div(className = "dg-slots-label") { +group.uppercase() }
                    div(className = if (column) "dg-slots col" else "dg-slots") {
                        scanned.filter { it.slot.group == group }.forEach { s ->
                            slotButton(s, s.slot.key == selected) {
                                selected = s.slot.key
                                picked = s.slot.key
                            }
                        }
                    }
                }
            }
            div(className = "dg-inv-legend") { +"Faded = missing. Click a slot for details." }
        }

        div(className = "dg-side") {
            detail(shown, shown === next)
            div(className = "dg-section") {
                +"Guide path"
                span(className = "dg-hint") { +"step ${pathStep(scanned) + 1} of ${guide.path.steps.size}" }
            }
            div(className = "dg-track") {
                val step = pathStep(scanned)
                guide.path.steps.forEachIndexed { i, text ->
                    val state = when {
                        i < step -> " done"
                        i == step -> " now"
                        else -> ""
                    }
                    div(className = "dg-track-step$state", key = "step$i") {
                        b { +"Step ${i + 1}" }
                        div { +text }
                    }
                }
            }
            div(className = "dg-hint") { +guide.path.goal }
        }
    }
}

private fun NodeBuilder.slotButton(s: ScannedSlot, active: Boolean, onClick: () -> Unit) {
    val state = s.state.name.lowercase()
    button(
        className = "dg-slot $state" + if (active) " active" else "",
        key = s.slot.key,
        title = "${s.slot.name}, ${stateWord(s.state)}",
        onClick = { onClick() }
    ) {
        icon(s)?.let { item(it, className = "dg-slot-icon", decorations = false, tooltip = s.stack != null) }
        (s.heldItem ?: s.slot.badge)?.let { key -> GuideData.icon(key)?.let { item(it, className = "dg-slot-badge", decorations = false) } }
        if (s.state != SlotState.HAVE) div(className = "dg-dim")
        span(className = "dg-slot-mark $state") {
            +when (s.state) {
                SlotState.HAVE -> "✓"
                SlotState.MISSING -> "+"
                SlotState.UNKNOWN -> "?"
            }
        }
    }
}

private fun icon(s: ScannedSlot): ItemStack? {
    if (mc.level == null) return null
    return s.stack ?: GuideData.icon(s.slot.icon)
}

private fun NodeBuilder.detail(s: ScannedSlot, isNext: Boolean) {
    val slot = s.slot
    div(className = "dg-detail") {
        if (isNext) div(className = "dg-next-label") { +"NEXT UPGRADE" }
        div(className = "dg-detail-top") {
            icon(s)?.let { stack ->
                div(className = "dg-detail-icon") {
                    item(stack, className = "dg-detail-item", decorations = false, tooltip = s.stack != null)
                    if (s.state != SlotState.HAVE) div(className = "dg-dim")
                }
            }
            div {
                div(className = "dg-detail-name dg-r-${slot.rarity}") { +slot.name }
                div(className = "dg-detail-type") {
                    +"${slot.type} · "
                    chip(s.state)
                }
            }
        }
        val price = slot.priceId?.takeIf { s.state == SlotState.MISSING }?.let { id ->
            ItemPrices.getItemPrice(id).takeIf { it > 0 }?.let { ItemPrices.getItemPriceFormatted(id) }
        }
        val gains = slot.gains.mapIndexed { i, gain -> if (i == 0 && s.magicFind != null) "+${s.magicFind} MF" else gain }
        if (gains.isNotEmpty() || price != null) div(className = "dg-gains") {
            gains.forEach { span(className = "dg-gain", key = it) { +it } }
            price?.let { span(className = "dg-gain cost") { +"$it coins" } }
        }
        div(className = "dg-detail-why") { +slot.why }
        enchants(s)
    }
}

private fun NodeBuilder.chip(state: SlotState) {
    span(className = "dg-chip ${state.name.lowercase()}") {
        +when (state) {
            SlotState.HAVE -> "✓ Have"
            SlotState.MISSING -> "Missing"
            SlotState.UNKNOWN -> "? Unknown"
        }
    }
}

private fun stateWord(state: SlotState): String = when (state) {
    SlotState.HAVE -> "you have it"
    SlotState.MISSING -> "missing"
    SlotState.UNKNOWN -> "unknown"
}

/**
 * The guide enchants of the slot. On an owned item each one is had or too low or missing. On a missing item they are
 * the ones to apply. An enchant without a guide level counts at any level.
 */
private fun NodeBuilder.enchants(s: ScannedSlot) {
    val list = s.slot.enchants
    if (list.isEmpty()) return
    val owned = s.state == SlotState.HAVE && s.enchants != null
    var done = 0
    val states = list.map { e ->
        val has = s.enchants?.get(e.id)
        val state = when {
            !owned -> ""
            has == null -> "missing"
            e.level != null && has < e.level -> "low"
            else -> "have"
        }
        if (state == "have") done++
        Triple(e, has, state)
    }
    div(className = "dg-ench-head") {
        when {
            owned -> {
                b { +"Enchants" }
                span { +"$done of ${list.size} as in the guide" }
            }
            s.state == SlotState.UNKNOWN -> {
                b { +"Enchants" }
                span { +"unknown until the item is read" }
            }
            else -> b { +"Enchants to apply" }
        }
    }
    div(className = "dg-ench") {
        states.forEach { (e, has, state) ->
            val className = "dg-ench-chip" + (if (e.ultimate) " ult" else "") + (if (state.isNotEmpty()) " $state" else "")
            val level = when {
                state == "low" && has != null && e.level != null -> "${roman(has)} → ${roman(e.level)}"
                e.level != null -> roman(e.level)
                else -> ""
            }
            span(className = className, key = e.id) {
                when (state) {
                    "have" -> span(className = "dg-ench-mark have") { +"✓" }
                    "missing" -> span(className = "dg-ench-mark missing") { +"+" }
                    "low" -> span(className = "dg-ench-mark low") { +"↑" }
                }
                +e.name
                if (level.isNotEmpty()) span(className = "dg-ench-level") { +" $level" }
            }
        }
    }
}

/**
 * Where the player is on the guide path. Only the items are checked and the magical power of the steps is not read.
 * Step 1 lasts until the Mythos equipment and a crown are there. Step 2 lasts until the Infernal Crimson pieces are
 * there. After that it is minmax.
 */
private fun pathStep(scanned: List<ScannedSlot>): Int {
    fun has(vararg keys: String) = keys.all { key -> scanned.firstOrNull { it.slot.key == key }?.state != SlotState.MISSING }
    return when {
        !has("helmet", "necklace", "belt", "bracelet") -> 0
        !has("chestplate", "leggings", "boots") -> 1
        else -> 3
    }
}
