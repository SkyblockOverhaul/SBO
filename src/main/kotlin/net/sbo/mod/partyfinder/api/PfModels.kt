package net.sbo.mod.partyfinder.api

import kotlinx.serialization.Serializable
import net.sbo.mod.utils.MojangAuth
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/** Error of a `/pf` call. Codes come from the backend, plus [PfError.NETWORK] and [PfError.BAD_RESPONSE] from the mod. */
@Serializable
data class PfError(
    val code: String = "UNKNOWN",
    val message: String = "",
    val problems: List<Problem> = emptyList()
) {
    companion object {
        const val NETWORK = "NETWORK"
        const val BAD_RESPONSE = "BAD_RESPONSE"
        const val REQS_NOT_MET = "REQS_NOT_MET"
        const val PARTY_FULL = "PARTY_FULL"
        const val PARTY_NOT_FOUND = "PARTY_NOT_FOUND"
        const val PARTY_TOO_OLD = "PARTY_TOO_OLD"
        const val CATEGORY_CLOSED = "CATEGORY_CLOSED"
        const val DEFINITIONS_OUTDATED = "DEFINITIONS_OUTDATED"
        const val INVALID_REQUEST = "INVALID_REQUEST"
        const val INVALID_KEY = "INVALID_KEY"
        const val NO_PROFILE = "NO_PROFILE"
        const val HYPIXEL_UNAVAILABLE = "HYPIXEL_UNAVAILABLE"
        const val RATE_LIMITED = "RATE_LIMITED"
        const val REPORT_NOT_ALLOWED = "REPORT_NOT_ALLOWED"
        const val INTERNAL_ERROR = "INTERNAL_ERROR"
        const val SESSION_REQUIRED = MojangAuth.SESSION_REQUIRED
        const val KEY_NOT_YOURS = MojangAuth.KEY_NOT_YOURS
        const val MOJANG_BUSY = MojangAuth.MOJANG_BUSY
    }
}

/** One failed requirement: [stat] is a stat id, `role` or `ironman`. */
@Serializable
data class Problem(
    val name: String = "",
    val stat: String = "",
    val have: JsonElement = JsonNull,
    val need: JsonElement = JsonNull
)

// GET /pf/categories

@Serializable
data class CategoriesData(
    val version: String,
    val stats: List<StatDef> = emptyList(),
    val categories: List<CategoryDef> = emptyList()
)

@Serializable
data class StatDef(
    val id: String,
    val label: String = id,
    val info: String = "",
    /** exact, calculated, minimum or reported */
    val accuracy: String = "exact",
    /** number, flag, rarity, items or breakdown */
    val kind: String = "number",
    val unverifiedUpTo: Double? = null,
    /** Highest value a party can ask for */
    val max: Int? = null,
    val valueLabels: List<String> = emptyList(),
    /** Hypixel API settings the value needs, e.g. inventory */
    val apis: List<String> = emptyList()
)

@Serializable
data class ReqDef(
    val stat: String,
    /** min, flag, rarity or anyOf */
    val type: String,
    val choices: List<ItemChoice> = emptyList()
)

@Serializable
data class ItemChoice(
    val id: String,
    val label: String = id,
    val tiers: Boolean = false,
    val minLevel: Int? = null,
    // Base rarity ("LEGENDARY"), pets have none
    val rarity: String? = null,
    // Rarities a pet exists in, lowest first; with more than one the creator may ask for a minimum
    val rarities: List<String> = emptyList()
) {
    /** Field of a pick that holds its lowest tier or rarity, null when the creator can't choose one. */
    val minimumField: String? get() = if (tiers) "minTier" else if (rarities.size > 1) "minRarity" else null
}

@Serializable
data class PartyOption(
    val id: String,
    val label: String = id,
    val values: List<OptionValue> = emptyList(),
    val default: String = "",
    // Several values at once, sent as a comma separated list of ids
    val multiple: Boolean = false,
    // Stats read this field (e.g. the slayer tier), own stats are loaded per value
    val affectsStats: Boolean = false
) {
    /** Picked value ids of a [multiple] field, in definition order. */
    fun picks(value: String?): List<String> {
        val picked = value.orEmpty().split(',')
        return values.map { it.id }.filter { it in picked }
    }
}

@Serializable
data class OptionValue(val id: String, val label: String = id)

@Serializable
data class RoleDef(val id: String, val label: String = id)

@Serializable
data class CategoryDef(
    val id: String,
    val label: String = id,
    val minSize: Int = 2,
    val maxSize: Int = 6,
    val reqs: List<ReqDef> = emptyList(),
    val display: List<String> = emptyList(),
    val options: List<PartyOption> = emptyList(),
    val roles: List<RoleDef> = emptyList(),
    val subcategories: List<SubcategoryDef> = emptyList()
)

@Serializable
data class SubcategoryDef(
    val id: String,
    val label: String = id,
    val reqs: List<ReqDef> = emptyList(),
    val display: List<String> = emptyList(),
    val options: List<PartyOption> = emptyList(),
    /** Replaces the roles of the category when set. */
    val roles: List<RoleDef>? = null,
    val maxSize: Int? = null,
    val event: String? = null,
    val open: Boolean = true,
    val opensAt: Long? = null,
    /** Parties can be created, also shortly before the event starts; null from older backends */
    val createOpen: Boolean? = null
)

// Party list and member stats

@Serializable
data class PartiesData(val parties: List<PartyView> = emptyList())

// Listed parties per party key ("fishing", "fishing/lava"), unfiltered
@Serializable
data class PartyCounts(val counts: Map<String, Int> = emptyMap())

@Serializable
data class PartyView(
    /** Leader uuid without dashes. */
    val id: String,
    val partyType: String,
    val subType: String = "",
    val partySize: Int = 6,
    val memberCount: Int = 1,
    val note: String = "",
    val createdAt: Long = 0,
    val reqs: Map<String, JsonElement> = emptyMap(),
    val options: Map<String, String> = emptyMap(),
    val roles: WantedRoles = WantedRoles(),
    val members: List<MemberView> = emptyList()
) {
    val leader: MemberView? get() = members.firstOrNull { it.uuid == id } ?: members.firstOrNull()
}

@Serializable
data class WantedRoles(val wanted: List<String> = emptyList())

@Serializable
data class MemberView(
    val uuid: String,
    val name: String = "",
    val role: String? = null,
    val updatedAt: Long = 0,
    /** By stat id, `null` means no data. */
    val stats: Map<String, JsonElement> = emptyMap(),
    /** Stats the mod reported itself, e.g. `bph` with its scope. */
    val reported: Map<String, ReportedStat> = emptyMap()
)

@Serializable
data class ReportedStat(val scope: String = "")

@Serializable
data class CheckData(
    val members: List<MemberView> = emptyList(),
    val problems: List<Problem> = emptyList()
)

// Request bodies

@Serializable
data class PartyBody(
    val partyType: String,
    val subType: String = "",
    val version: String = "",
    val uuids: List<String>,
    val partySize: Int,
    val note: String = "",
    val reqs: Map<String, JsonElement> = emptyMap(),
    val options: Map<String, String> = emptyMap(),
    val roles: RolesBody = RolesBody()
)

@Serializable
data class RolesBody(
    val wanted: List<String> = emptyList(),
    val members: Map<String, String> = emptyMap()
)

@Serializable
data class CheckBody(
    val partyType: String,
    val subType: String = "",
    /** Exactly one of [uuids] and [names]; the backend only takes uuids with a key. */
    val uuids: List<String>? = null,
    val partyId: String? = null,
    val role: String? = null,
    val options: Map<String, String>? = null,
    val readcache: Boolean? = null,
    val names: List<String>? = null
)

@Serializable
data class StatsReportBody(val bph: BphReport)

@Serializable
data class RulesData(val intro: String = "", val rules: List<Rule> = emptyList(), val outro: String = "")

@Serializable
data class Rule(val title: String = "", val text: String = "")

/** [partyId] is the leader uuid, [reason] one of [ReportReason]. */
@Serializable
data class PartyReportBody(val partyId: String, val reason: String, val details: String? = null)

enum class ReportReason(val id: String, val label: String) {
    OFFENSIVE_NOTE("offensive_note", "Offensive note"),
    SELLING("selling", "Selling carries or services"),
    ADVERTISING("advertising", "Advertising"),
    FAKE_PARTY("fake_party", "Fake or misleading party"),
    OTHER("other", "Other");

    companion object {
        const val MIN_OTHER_DETAILS = 10
        const val MAX_DETAILS = 200
        fun of(id: String?) = entries.firstOrNull { it.id == id }
    }
}

@Serializable
data class BphReport(
    val value: Double,
    /** current or lastEvent */
    val scope: String,
    val hours: Double,
    val burrows: Long
)
