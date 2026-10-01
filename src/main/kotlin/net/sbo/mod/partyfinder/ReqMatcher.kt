package net.sbo.mod.partyfinder

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.partyfinder.api.Problem

/** "Does this player meet the requirements?", with the same rules as `checkMember` in the backend. */
object ReqMatcher {
    val RARITIES = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC")
    val KUUDRA_TIERS = listOf("BASIC", "HOT", "BURNING", "FIERY", "INFERNAL")

    fun meets(type: String, have: JsonElement?, need: JsonElement): Boolean = when (type) {
        "min" -> {
            val value = have.number()
            val wanted = need.number()
            value != null && wanted != null && value >= wanted
        }
        "flag" -> have.primitive()?.booleanOrNull == true
        "rarity" -> rarityRank(have.text()) >= rarityRank(need.text()).coerceAtLeast(0)
        "anyOf" -> {
            val owned = (have as? JsonArray).orEmpty().mapNotNull { it.pickId()?.let { id -> id to it.field("tier") } }
            (need as? JsonArray).orEmpty().any { pick ->
                val id = pick.pickId() ?: return@any false
                val minTier = pick.field("minTier")
                owned.any { (ownedId, tier) ->
                    ownedId == id && (minTier == null || tierRank(tier ?: "BASIC") >= tierRank(minTier))
                }
            }
        }
        else -> false
    }

    /** Every requirement [stats] fails, plus the Ironman party setting. */
    fun check(
        name: String,
        stats: Map<String, JsonElement>,
        reqs: Map<String, JsonElement>,
        target: PartyTarget,
        options: Map<String, String>
    ): List<Problem> {
        val problems = mutableListOf<Problem>()
        for (def in target.reqs) {
            val need = reqs[def.stat] ?: continue
            if (need is JsonNull) continue
            val have = stats[def.stat] ?: JsonNull
            if (!meets(def.type, have, need)) problems += Problem(name, def.stat, have, need)
        }
        val ironman = options["ironman"]
        if (ironman == "only" || ironman == "none") {
            val have = stats["ironman"] ?: JsonNull
            if (have.primitive()?.booleanOrNull != (ironman == "only")) {
                problems += Problem(name, "ironman", have, JsonPrimitive(ironman))
            }
        }
        return problems
    }

    /**
     * "Can I join?" for [me] on [party]. Slayer carry parties where the leader carries check nobody.
     * A [role] is only checked when given, the joiner picks it later.
     */
    fun checkJoin(party: PartyView, target: PartyTarget, me: MemberView, role: String? = null): List<Problem> {
        val leaderCarries = party.partyType == "slayer" &&
            party.options["purpose"] == "carry" && party.options["carrySide"] == "i_carry"
        val problems = if (leaderCarries) {
            check(me.name, me.stats, emptyMap(), target, party.options)
        } else {
            check(me.name, me.stats, party.reqs, target, party.options)
        }.toMutableList()
        val wanted = party.roles.wanted
        if (role != null && wanted.isNotEmpty() && role !in wanted) {
            problems += Problem(me.name, "role", JsonPrimitive(role), JsonArray(wanted.map { JsonPrimitive(it) }))
        }
        return problems
    }

    fun isFull(party: PartyView): Boolean = party.memberCount >= party.partySize

    fun rarityRank(rarity: String?): Int = RARITIES.indexOf(rarity?.uppercase())

    fun tierRank(tier: String): Int = KUUDRA_TIERS.indexOf(tier.uppercase())

    private fun JsonElement?.primitive(): JsonPrimitive? = this as? JsonPrimitive

    private fun JsonElement?.number(): Double? = primitive()?.takeUnless { it is JsonNull }?.doubleOrNull

    private fun JsonElement?.text(): String? = primitive()?.takeUnless { it is JsonNull }?.contentOrNull

    private fun JsonElement.pickId(): String? = when (this) {
        is JsonObject -> field("id")
        is JsonPrimitive -> contentOrNull
        else -> null
    }

    private fun JsonElement.field(key: String): String? =
        ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
}
