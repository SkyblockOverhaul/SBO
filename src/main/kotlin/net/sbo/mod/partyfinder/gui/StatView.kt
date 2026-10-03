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
    fun value(statId: String, value: JsonElement?): String = valuePieces(statId, value).joinToString("") { it.text }

    /** [value] in pieces, colored by themes with Hypixel colors. */
    fun valuePieces(statId: String, value: JsonElement?): List<Piece> {
        if (value == null || value is JsonNull) return listOf(Piece(ProblemText.value(statId, value)))
        val pieces = when {
            value is JsonArray && value.isNotEmpty() -> value.flatMapIndexed { i, item ->
                val id = (item as? JsonObject)?.get("id")?.text() ?: item.text() ?: "?"
                val tier = (item as? JsonObject)?.get("tier")?.text()
                val label = ProblemText.choiceLabel(statId, id)
                val piece = Piece(if (tier != null && tier != "BASIC") "${ProblemText.title(tier)} $label" else label, itemColor(statId, id))
                if (i == 0) listOf(piece) else listOf(Piece(", "), piece)
            }
            else -> listOf(Piece(ProblemText.value(statId, value), valueColor(statId, value)))
        }
        return when (PartyCategories.stat(statId)?.accuracy) {
            "minimum" -> pieces + Piece("+")
            "reported", "calculated" -> listOf(Piece("~")) + pieces
            else -> pieces
        }
    }

    fun estimated(statId: String): Boolean = PartyCategories.stat(statId)?.accuracy.let { it != null && it != "exact" }

    /** What a requirement asks for, e.g. "50+", "Legendary or better", "Terror Armor (Fiery or better)". */
    fun need(def: ReqDef, need: JsonElement): String = needPieces(def, need).joinToString("") { it.text }

    /** [need] in pieces, colored by themes with Hypixel colors. */
    fun needPieces(def: ReqDef, need: JsonElement): List<Piece> = when (def.type) {
        "flag" -> listOf(Piece("needed"))
        "rarity" -> {
            val rarity = (need as? JsonPrimitive)?.contentOrNull
            listOf(Piece(ProblemText.rarityNeed(rarity), rarityColor(rarity)))
        }
        "anyOf" -> ReqMatcher.picks(need).flatMapIndexed { i, pick ->
            val id = (pick as? JsonObject)?.get("id")?.text() ?: pick.text() ?: "?"
            val choice = def.choices.firstOrNull { it.id == id }
            val text = ProblemText.tierPick(choice?.label ?: ProblemText.title(id), (pick as? JsonObject)?.get("minTier")?.text())
            val color = choice?.rarity?.let(::itemRarityClass)
            // The tier note in brackets stays uncolored
            val bracket = text.indexOf(" (")
            val pieces = if (bracket > 0) listOf(Piece(text.substring(0, bracket), color), Piece(text.substring(bracket))) else listOf(Piece(text, color))
            if (i == 0) pieces else listOf(Piece(if (ReqMatcher.matchesAll(need)) " and " else " or ")) + pieces
        }
        else -> {
            val number = (need as? JsonPrimitive)?.doubleOrNull
            val labels = PartyCategories.stat(def.stat)?.valueLabels.orEmpty()
            when {
                number != null && labels.isNotEmpty() -> listOf(Piece(
                    ProblemText.orBetter(labels.getOrNull(number.toInt()) ?: ProblemText.number(number), number.toInt() >= labels.lastIndex),
                    numberColor(def.stat, number)
                ))
                number != null -> listOf(Piece(ProblemText.number(number), numberColor(def.stat, number)), Piece("+"))
                else -> listOf(Piece(ProblemText.value(def.stat, need)))
            }
        }
    }

    private fun valueColor(statId: String, value: JsonElement): String? {
        val primitive = value as? JsonPrimitive ?: return null
        if (PartyCategories.stat(statId)?.kind == "rarity") return rarityColor(primitive.contentOrNull)
        return primitive.doubleOrNull?.let { numberColor(statId, it) }
    }

    private fun itemColor(statId: String, id: String): String? = PartyCategories.choice(statId, id)?.rarity?.let(::itemRarityClass)

    // Items can be recombobulated, so they get rarity classes the theme can shift one up
    fun itemRarityClass(rarity: String): String = "pf-r-" + rarity.lowercase(Locale.US).replace('_', '-')

    /** Minecraft color of a rarity, like item names in game. */
    fun rarityColor(rarity: String?): String? = when (rarity?.uppercase(Locale.US)) {
        "COMMON" -> "pf-c-white"
        "UNCOMMON" -> "pf-c-green"
        "RARE" -> "pf-c-blue"
        "EPIC" -> "pf-c-dark-purple"
        "LEGENDARY" -> "pf-c-gold"
        "MYTHIC" -> "pf-c-light-purple"
        "DIVINE" -> "pf-c-aqua"
        "SPECIAL", "VERY_SPECIAL" -> "pf-c-red"
        "ULTIMATE" -> "pf-c-dark-red"
        else -> null
    }

    /** Colors for numbers the game colors too: SkyBlock level prefix, Trophy Fisher title, Diana kills like /sboc. */
    fun numberColor(statId: String, number: Double): String? {
        val n = number.toInt()
        return when (statId) {
            "sbLevel" -> "pf-c-" + when {
                n >= 480 -> "dark-red"
                n >= 440 -> "red"
                n >= 400 -> "gold"
                n >= 360 -> "dark-purple"
                n >= 320 -> "light-purple"
                n >= 280 -> "blue"
                n >= 240 -> "dark-aqua"
                n >= 200 -> "aqua"
                n >= 160 -> "dark-green"
                n >= 120 -> "green"
                n >= 80 -> "yellow"
                n >= 40 -> "white"
                else -> "gray"
            }
            // Novice, Adept, Expert, Master in bronze, silver, gold and diamond
            "trophyFisher" -> when (n) {
                1 -> "pf-trophy-novice"
                2 -> "pf-trophy-adept"
                3 -> "pf-trophy-expert"
                4 -> "pf-trophy-master"
                else -> null
            }
            "dianaKills" -> "pf-c-" + when {
                n >= 200_000 -> "gold"
                n >= 150_000 -> "yellow"
                n >= 100_000 -> "red"
                n >= 75_000 -> "light-purple"
                n >= 50_000 -> "blue"
                n >= 25_000 -> "green"
                n >= 10_000 -> "dark-green"
                else -> "gray"
            }
            else -> null
        }
    }

    private fun JsonElement.text(): String? = (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull

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
        "reported" -> "Estimated: sent by the player's own SBO mod, cannot be verified by Hypixel API."
        "calculated" -> "Calculated by SBO, can differ a little from the game."
        else -> "Exact value."
    }
}

/** A piece of a value text; [color] is a CSS class ("pf-c-gold", "pf-r-epic") that only themes with Hypixel colors color. */
internal data class Piece(val text: String, val color: String? = null)

internal fun NodeBuilder.pieces(pieces: List<Piece>) {
    pieces.forEach { piece -> if (piece.color == null) +piece.text else span(className = piece.color) { +piece.text } }
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
        if (stat.accuracy != "reported") div(className = "pf-tip-api") { +ProblemText.apiHint(stat.apis) }
    }
    if (own != null) div(className = "pf-tip-own") {
        +"You: "
        pieces(StatView.valuePieces(statId, own.stats[statId]))
    }
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
