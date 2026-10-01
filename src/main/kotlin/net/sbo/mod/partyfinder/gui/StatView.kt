package net.sbo.mod.partyfinder.gui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.b
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.tooltip
import net.sbo.mod.partyfinder.PartyCategories
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.ReqMatcher
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.ReqDef
import net.sbo.mod.partyfinder.api.StatDef
import java.util.Locale

/** Stat values, requirement texts and info tooltips shared by the party finder pages. */
internal object StatView {
    const val ICONS = "sbo:ui/partyfinder"

    fun label(statId: String): String = PartyCategories.stat(statId)?.label ?: statId

    /** A value with its accuracy mark: "42+" for checked minimums, "~620" for estimates. */
    fun value(statId: String, value: JsonElement?): String {
        val text = ProblemText.value(statId, value)
        if (value == null || value is JsonNull) return text
        return when (PartyCategories.stat(statId)?.accuracy) {
            "minimum" -> "$text+"
            "reported", "calculated" -> "~$text"
            else -> text
        }
    }

    fun estimated(statId: String): Boolean = PartyCategories.stat(statId)?.accuracy.let { it != null && it != "exact" }

    /** What a requirement asks for, e.g. "50+", "Legendary or better", "Terror Armor (Fiery or better)". */
    fun need(def: ReqDef, need: JsonElement): String = when (def.type) {
        "flag" -> "needed"
        "rarity" -> "${ProblemText.title((need as? JsonPrimitive)?.contentOrNull ?: "")} or better"
        "anyOf" -> (need as? JsonArray).orEmpty().joinToString(" or ") { pick ->
            val id = (pick as? JsonObject)?.get("id")?.let { (it as? JsonPrimitive)?.contentOrNull }
                ?: (pick as? JsonPrimitive)?.contentOrNull ?: "?"
            val label = def.choices.firstOrNull { it.id == id }?.label ?: ProblemText.title(id)
            val minTier = ((pick as? JsonObject)?.get("minTier") as? JsonPrimitive)?.contentOrNull
            if (minTier != null) "$label (${ProblemText.title(minTier)} or better)" else label
        }
        else -> {
            val number = (need as? JsonPrimitive)?.doubleOrNull
            val labels = PartyCategories.stat(def.stat)?.valueLabels.orEmpty()
            when {
                number != null && labels.isNotEmpty() -> "${labels.getOrNull(number.toInt()) ?: ProblemText.number(number)} or better"
                number != null -> "${ProblemText.number(number)}+"
                else -> ProblemText.value(def.stat, need)
            }
        }
    }

    /** True or false when [own] stats are known, null otherwise. */
    fun meets(def: ReqDef, need: JsonElement, own: MemberView?): Boolean? {
        own ?: return null
        return ReqMatcher.meets(def.type, own.stats[def.stat] ?: JsonNull, need)
    }

    /** Where a value comes from and how exact it is, in plain words. */
    fun accuracyText(stat: StatDef): String = when (stat.accuracy) {
        "minimum" -> {
            val more = stat.unverifiedUpTo?.let { " The real value can be up to ${ProblemText.number(it)} higher." } ?: ""
            "Estimated: SBO only counts what it can check, so this is the lowest possible value.$more"
        }
        "reported" -> "Estimated: sent by the player's own SBO mod, Hypixel can not confirm it."
        "calculated" -> "Calculated by SBO, can differ a little from the game."
        else -> "Exact value from the Hypixel API."
    }
}

/** Label of a stat with an info icon; hovering shows what the stat means and where it comes from. */
internal fun NodeBuilder.statLabel(statId: String, own: MemberView? = null, className: String? = null) {
    val stat = PartyCategories.stat(statId)
    tooltip(content = { statInfo(statId, own) }, className = "pf-tip") {
        span(className = classNames("pf-stat-label", className)) {
            +(stat?.label ?: statId)
            img(src = "${StatView.ICONS}/info.svg", className = "pf-icon pf-info")
        }
    }
}

/** Tooltip body for one stat. */
internal fun NodeBuilder.statInfo(statId: String, own: MemberView? = null) {
    val stat = PartyCategories.stat(statId)
    div(className = "pf-tip-title") { b { +(stat?.label ?: statId) } }
    if (stat != null) {
        if (stat.info.isNotBlank()) div(className = "pf-tip-text") { +stat.info }
        div(className = classNames("pf-tip-accuracy", "estimated" to (stat.accuracy != "exact"))) { +StatView.accuracyText(stat) }
    }
    if (own != null) div(className = "pf-tip-own") { +"You: ${StatView.value(statId, own.stats[statId])}" }
}

internal fun sizeLabel(size: Int): String = when (size) {
    2 -> "Duo"
    3 -> "Trio"
    else -> "$size players"
}

/** "5m ago", "2h ago". */
internal fun ago(epochMs: Long, now: Long = System.currentTimeMillis()): String {
    if (epochMs <= 0) return ""
    val minutes = ((now - epochMs) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        else -> "${minutes / 60}h ago"
    }
}

/** "2d 4h", "35m". */
internal fun until(epochMs: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((epochMs - now) / 60_000).coerceAtLeast(0)
    val days = minutes / (60 * 24)
    val hours = minutes / 60 % 24
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        else -> "${minutes}m"
    }
}

/** Short label of a subcategory tab, with the opening time while its event is closed. */
internal fun subLabel(target: PartyTarget): String {
    val sub = target.sub ?: return target.category.label
    if (sub.open) return sub.label
    val opensAt = sub.opensAt ?: return "${sub.label} (event closed)"
    return "${sub.label} (opens in ${until(opensAt)})"
}

internal fun JsonElement?.isTrue(): Boolean = (this as? JsonPrimitive)?.booleanOrNull == true

internal fun title(text: String): String = text.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
