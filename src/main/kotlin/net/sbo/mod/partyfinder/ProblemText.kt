package net.sbo.mod.partyfinder

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import net.sbo.mod.partyfinder.api.ItemChoice
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.partyfinder.api.Problem
import java.util.Locale

/** Turns requirement problems and stat values into short English texts for chat and GUI. */
object ProblemText {
    private const val RELOAD = "/sboreloadstats"
    private val API_NAMES = mapOf("inventory" to "Inventory API", "vault" to "Vault API", "skills" to "Skills API")

    /** Player's own stats look wrong. */
    const val OWN_RELOAD_HINT = "Wrong or old stats? Turn on your Hypixel API settings and type $RELOAD."

    /** Party leader whose members miss requirements. */
    const val MEMBERS_RELOAD_HINT = "Players who don't meet them should turn on their Hypixel API settings and type $RELOAD."

    /** Tooltip line under every stat from the Hypixel API; [apis] are the API settings it needs. */
    fun apiHint(apis: List<String>): String {
        val names = apis.map { API_NAMES[it] ?: it }
        val where = if (names.isEmpty()) "their Hypixel API settings" else "the ${names.joinToString(" and ")} in their Hypixel settings"
        return "From the Hypixel API. Wrong or old? The player has to turn on $where and type $RELOAD."
    }

    /** E.g. "Tracking: needs at least 50, has 42". */
    fun describe(problem: Problem, target: PartyTarget?): String = when (problem.stat) {
        "ironman" -> if (problem.need.text() == "only") "this party is only for Ironman players" else "this party is not for Ironman players"
        "role" -> {
            val labels = (problem.need as? JsonArray).orEmpty().mapNotNull { it.text() }
                .map { id -> target?.roles?.firstOrNull { it.id == id }?.label ?: id }
            "pick one of these roles: ${labels.joinToString(", ")}"
        }
        else -> {
            val stat = PartyCategories.stat(problem.stat)
            val label = stat?.label ?: problem.stat
            val def = target?.req(problem.stat)
            when (def?.type) {
                "flag" -> "$label: needed, but missing"
                "rarity" -> "$label: needs ${rarityNeed(problem.need.text())}, has ${value(problem.stat, problem.have)}"
                "anyOf" -> if (ReqMatcher.matchesAll(problem.need)) {
                    "$label: needs all of ${picks(problem.need, def.choices)}, is missing some"
                } else {
                    "$label: needs one of ${picks(problem.need, def.choices)}, has none of them"
                }
                else -> "$label: needs at least ${value(problem.stat, problem.need)}, has ${value(problem.stat, problem.have)}"
            }
        }
    }

    /** What went wrong, in words a player understands. */
    fun error(error: PfError): String = when (error.code) {
        PfError.REQS_NOT_MET -> "Not everyone meets the requirements."
        PfError.PARTY_FULL -> "The party is already full."
        PfError.PARTY_NOT_FOUND -> "The party does not exist anymore."
        PfError.PARTY_TOO_OLD -> "The party was listed for over an hour and got removed."
        PfError.CATEGORY_CLOSED -> "This party type is only open while its event is running."
        PfError.INVALID_KEY -> "Your SBO key is not valid. Set a new one with /sbokey <key>, you get one in our Discord."
        PfError.NO_PROFILE -> "No SkyBlock stats found: ${error.message}. Make sure your API settings are on."
        PfError.HYPIXEL_UNAVAILABLE -> "Hypixel does not answer right now. Please try again in a few minutes."
        PfError.RATE_LIMITED -> "Too many requests. Please wait a moment."
        PfError.NETWORK -> "Could not reach the SBO server. Check your internet connection."
        else -> error.message.ifBlank { "Unknown error (${error.code})" }
    }

    /** A stat value as text: numbers, labels like "Adept", rarities, item lists and per tier numbers. */
    fun value(statId: String, value: JsonElement?): String {
        if (value == null || value is JsonNull) return "no data"
        val stat = PartyCategories.stat(statId)
        return when (value) {
            is JsonPrimitive -> {
                value.booleanOrNull?.let { return if (it) "yes" else "no" }
                val number = value.doubleOrNull
                when {
                    number != null && stat?.valueLabels?.isNotEmpty() == true ->
                        stat.valueLabels.getOrNull(number.toInt()) ?: number(number)
                    number != null -> number(number)
                    stat?.kind == "rarity" -> rarity(value)
                    else -> value.contentOrNull ?: "no data"
                }
            }
            is JsonArray -> if (value.isEmpty()) "none" else value.joinToString(", ") { item ->
                val id = (item as? JsonObject)?.get("id")?.text() ?: item.text() ?: "?"
                val tier = (item as? JsonObject)?.get("tier")?.text()
                val rarity = (item as? JsonObject)?.get("rarity")?.text()
                val label = choiceLabel(statId, id)
                when {
                    rarity != null -> "${title(rarity)} $label"
                    tier != null && tier != "BASIC" -> "${title(tier)} $label"
                    else -> label
                }
            }
            is JsonObject -> value.entries.joinToString(", ") { (key, v) -> "${title(key)} ${value(statId, v)}" }
        }
    }

    fun number(value: Double): String =
        if (value == Math.floor(value) && !value.isInfinite()) String.format(Locale.US, "%,d", value.toLong())
        else String.format(Locale.US, "%,.1f", value)

    fun title(text: String): String = text.lowercase().split('_', ' ').joinToString(" ") { part ->
        part.replaceFirstChar { it.titlecase(Locale.US) }
    }

    private fun rarity(value: JsonElement): String = value.text()?.let { title(it) } ?: "none"

    /** "Legendary or better", just "Mythic" when nothing is better. */
    fun orBetter(label: String, top: Boolean): String = if (top) label else "$label or better"

    fun rarityNeed(rarity: String?): String =
        orBetter(title(rarity ?: ""), ReqMatcher.rarityRank(rarity) == ReqMatcher.RARITIES.lastIndex)

    /** "Terror Armor (Fiery or better)", "Infernal Terror Armor" at the top tier, any tier for Basic. */
    fun tierPick(label: String, minTier: String?): String {
        val rank = minTier?.let { ReqMatcher.tierRank(it) } ?: return label
        return when (rank) {
            0 -> label
            ReqMatcher.KUUDRA_TIERS.lastIndex -> "${title(minTier)} $label"
            else -> "$label (${title(minTier)} or better)"
        }
    }

    /** "Ender Dragon (Legendary or better)", "Legendary Ender Dragon" at the pet's top rarity, any rarity for its lowest. */
    fun rarityPick(label: String, minRarity: String, rarities: List<String>): String {
        val rank = rarities.indexOf(minRarity.uppercase(Locale.US))
        return when {
            rank <= 0 -> label
            rank == rarities.lastIndex -> "${title(minRarity)} $label"
            else -> "$label (${title(minRarity)} or better)"
        }
    }

    /** One anyOf pick with its lowest tier or rarity. */
    fun choicePick(label: String, pick: JsonElement, choice: ItemChoice?): String {
        val minRarity = (pick as? JsonObject)?.get("minRarity")?.text()
        if (minRarity != null) return rarityPick(label, minRarity, choice?.rarities.orEmpty())
        return tierPick(label, (pick as? JsonObject)?.get("minTier")?.text())
    }

    private fun picks(need: JsonElement, choices: List<ItemChoice>): String =
        ReqMatcher.picks(need).joinToString(", ") { pick ->
            val id = (pick as? JsonObject)?.get("id")?.text() ?: pick.text() ?: "?"
            val choice = choices.firstOrNull { it.id == id }
            choicePick(choice?.label ?: id, pick, choice)
        }

    fun choiceLabel(statId: String, id: String): String = PartyCategories.choice(statId, id)?.label ?: title(id)

    private fun JsonElement.text(): String? = (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
}
