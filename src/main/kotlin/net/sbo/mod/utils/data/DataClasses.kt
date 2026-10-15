package net.sbo.mod.utils.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class PlayerInfoResponse(
    @SerialName("Success")
    val success: Boolean = false,

    @SerialName("PlayerInfo")
    val playerInfo: PartyPlayerStats? = null,

    @SerialName("Error")
    val error: String? = null
)

@Serializable
data class CloudUploadRequest(
    val data: String,
    val baseVersion: Int,
    val force: Boolean = false
)

@Serializable
data class SboBadge(
    val uuid: String,
    val name: String,
    val rank: String,
    val color: String,
    val to: String? = null,
    val level: Boolean? = null,
    val plainName: Boolean? = null,
    val label: String? = null,
    val value: String? = null,
    /** Places and colors per part; backends before it only send the fields above. */
    @SerialName("in")
    val places: BadgePlaces? = null,
    val colors: BadgeColors? = null,
) {
    fun shows(part: String, place: String): Boolean = places?.let { place in it[part] } ?: when (part) {
        BadgePart.LOGO -> true
        BadgePart.NAME -> plainName != true
        BadgePart.LEVEL -> level == true && place in TeamTagPlace.LEVEL
        else -> false
    }

    fun colorOf(part: String): PartColor = colors?.get(part) ?: PartColor(color, to)
}

/** Parts of a badge: logo (with the stat above the head), name, level and team tag show per place; stat, name, level and tag have a color. */
object BadgePart {
    const val LOGO = "logo"
    const val STAT = "stat"
    const val NAME = "name"
    const val LEVEL = "level"
    const val TAG = "tag"
}

@Serializable
data class PartColor(val color: String, val to: String? = null)

@Serializable
data class BadgePlaces(
    val logo: List<String> = emptyList(),
    val name: List<String> = emptyList(),
    val level: List<String> = emptyList(),
) {
    operator fun get(part: String): List<String> = when (part) {
        BadgePart.LOGO -> logo
        BadgePart.NAME -> name
        BadgePart.LEVEL -> level
        else -> emptyList()
    }

    fun with(part: String, places: List<String>): BadgePlaces = when (part) {
        BadgePart.LOGO -> copy(logo = places)
        BadgePart.NAME -> copy(name = places)
        BadgePart.LEVEL -> copy(level = places)
        else -> this
    }
}

@Serializable
data class BadgeColors(
    val stat: PartColor? = null,
    val name: PartColor? = null,
    val level: PartColor? = null,
    val tag: PartColor? = null,
) {
    operator fun get(part: String): PartColor? = when (part) {
        BadgePart.STAT -> stat
        BadgePart.NAME -> name
        BadgePart.LEVEL -> level
        BadgePart.TAG -> tag
        else -> null
    }

    fun with(part: String, color: PartColor): BadgeColors = when (part) {
        BadgePart.STAT -> copy(stat = color)
        BadgePart.NAME -> copy(name = color)
        BadgePart.LEVEL -> copy(level = color)
        BadgePart.TAG -> copy(tag = color)
        else -> this
    }
}

@Serializable
data class BadgeListResponse(
    @SerialName("Success")
    val success: Boolean = false,
    val version: String? = null,
    val unchanged: Boolean = false,
    val badges: List<SboBadge> = emptyList(),
    /** Moderators and above, also with their badge turned off; for the party finder and the name tag. */
    val team: List<TeamMember> = emptyList(),
    val ranks: Map<String, BadgeRank> = emptyMap(),
)

/** How a rank looks, sent by the backend so it can change without a mod update. */
@Serializable
data class BadgeRank(
    val label: String,
    /** Text of the team tag, null for ranks without one. */
    val tag: String? = null,
    val color: String = "#FFFFFF",
    val to: String? = null,
)

@Serializable
data class TeamMember(
    val uuid: String,
    val name: String,
    val rank: String,
    /** Shows the team tag somewhere; `tagIn` says where, backends before it only send this. */
    val nameTag: Boolean = false,
    val tagIn: List<String>? = null,
) {
    val tagPlaces: Set<String> get() = tagIn?.toSet() ?: if (nameTag) TeamTagPlace.ALL else emptySet()
}

/** Where a team tag shows in front of the name. */
object TeamTagPlace {
    const val HEAD = "head"
    const val TAB = "tab"
    const val CHAT = "chat"
    val ALL = setOf(HEAD, TAB, CHAT)
    /** The level only takes the color right before the name; in chat Hypixel's rank stands between. */
    val LEVEL = setOf(HEAD, TAB)
}

/** Body of `POST /badge` */
@Serializable
data class BadgeSettings(
    val enabled: Boolean,
    val stat: String,
    val color: String,
    /** Second gradient color, only for ranks that may pick a gradient. */
    val colorTo: String? = null,
    val colorName: Boolean = true,
    val colorLevel: Boolean = false,
    val teamTag: Boolean = true,
    /** Places of the team tag (TeamTagPlace); null on backends without it, then `teamTag` means everywhere. */
    val teamTagIn: List<String>? = null,
    /** Places and colors per part; null on backends without them, then the single fields above apply everywhere. */
    @SerialName("in")
    val places: BadgePlaces? = null,
    val colors: BadgeColors? = null,
) {
    val teamTagPlaces: Set<String> get() = teamTagIn?.toSet() ?: if (teamTag) TeamTagPlace.ALL else emptySet()

    fun withTeamTagPlaces(places: Set<String>): BadgeSettings =
        copy(teamTag = places.isNotEmpty(), teamTagIn = TeamTagPlace.ALL.filter { it in places })

    fun placesOf(part: String): Set<String> = places?.get(part)?.toSet() ?: when (part) {
        BadgePart.LOGO -> if (enabled) TeamTagPlace.ALL else emptySet()
        BadgePart.NAME -> if (enabled && colorName) TeamTagPlace.ALL else emptySet()
        BadgePart.LEVEL -> if (enabled && colorLevel) TeamTagPlace.LEVEL else emptySet()
        else -> emptySet()
    }

    fun colorOf(part: String): PartColor = colors?.get(part) ?: PartColor(color, colorTo)

    fun withPlaces(part: String, places: Set<String>): BadgeSettings =
        withStyle(styledPlaces().with(part, TeamTagPlace.ALL.filter { it in places }), styledColors())

    fun withColor(part: String, color: PartColor): BadgeSettings = withStyle(styledPlaces(), styledColors().with(part, color))

    private fun styledPlaces() = BadgePlaces(
        TeamTagPlace.ALL.filter { it in placesOf(BadgePart.LOGO) },
        TeamTagPlace.ALL.filter { it in placesOf(BadgePart.NAME) },
        TeamTagPlace.ALL.filter { it in placesOf(BadgePart.LEVEL) },
    )

    private fun styledColors() = BadgeColors(
        colorOf(BadgePart.STAT), colorOf(BadgePart.NAME), colorOf(BadgePart.LEVEL), colorOf(BadgePart.TAG),
    )

    // The single fields follow the parts, for mods that only read those
    private fun withStyle(places: BadgePlaces, colors: BadgeColors): BadgeSettings {
        val name = colors.name ?: PartColor(color, colorTo)
        return copy(
            places = places,
            colors = colors,
            enabled = places.logo.isNotEmpty() || places.name.isNotEmpty() || places.level.isNotEmpty(),
            colorName = places.name.isNotEmpty(),
            colorLevel = places.level.isNotEmpty(),
            color = name.color,
            colorTo = name.to,
        )
    }
}

@Serializable
data class BadgeChoice(
    val id: String,
    val label: String,
    val hex: String? = null,
    val to: String? = null,
)

@Serializable
data class OwnBadgeResponse(
    @SerialName("Success")
    val success: Boolean = false,
    @SerialName("Error")
    val error: String? = null,
    @SerialName("Code")
    val code: String? = null,
    val rank: String? = null,
    val rankInfo: BadgeRank? = null,
    val settings: BadgeSettings? = null,
    val preview: SboBadge? = null,
    val hiddenByServer: Boolean = false,
    val values: Map<String, String> = emptyMap(),
    val level: Int? = null,
    val stats: List<BadgeChoice> = emptyList(),
    /** The rank may pick a gradient instead of one color. */
    val gradient: Boolean = false,
    /** The rank has a team tag it can show above the head. */
    val team: Boolean = false,
)

@Serializable
data class CloudEnvelope(
    val v: Int = 1,
    val counter: Long,
    val files: Map<String, String>,
    val sig: String? = null
)

@Serializable
data class CloudUploadResponse(
    @SerialName("Success")
    val success: Boolean = false,
    @SerialName("Error")
    val error: String? = null,
    @SerialName("Code")
    val code: String? = null,
    @SerialName("Conflict")
    val conflict: Boolean = false,
    val version: Int = 0
)

@Serializable
data class CloudSlotResponse(
    @SerialName("Success")
    val success: Boolean = false,
    @SerialName("Error")
    val error: String? = null,
    val data: String = "",
    val version: Int = 0,
    val updatedAt: Long = 0
)

@Serializable
data class CloudSlotMeta(
    val slot: String,
    val version: Int,
    val size: Int,
    val updatedAt: Long
)

@Serializable
data class CloudStatusResponse(
    @SerialName("Success")
    val success: Boolean = false,
    @SerialName("Error")
    val error: String? = null,
    @SerialName("Code")
    val code: String? = null,
    val slots: List<CloudSlotMeta> = emptyList()
)

@Serializable
data class HypixelBazaarResponse(
    val success: Boolean = false,
    val lastUpdated: Long = 0,
    val products: Map<String, Product> = emptyMap()
)

@Suppress("PropertyName")
@Serializable
data class Product(
    val product_id: String,
    val sell_summary: List<SummaryItem>,
    val buy_summary: List<SummaryItem>,
    val quick_status: QuickStatus
)

@Serializable
data class SummaryItem(
    val amount: Int,
    val pricePerUnit: Double,
    val orders: Int
)

@Serializable
data class QuickStatus(
    val productId: String,
    val sellPrice: Double,
    val sellVolume: Int,
    val sellMovingWeek: Int,
    val sellOrders: Int,
    val buyPrice: Double,
    val buyVolume: Int,
    val buyMovingWeek: Int,
    val buyOrders: Int
)

@Serializable
data class PartyPlayerStats(
    val name: String = "",
    val sbLvl: Int = -1,
    val eman9: Boolean = false,
    val looting5daxe: Boolean = false,
    val emanLvl: Int = 0,
    val warnings: List<String> = emptyList(),
    val uuid: String = "",
    val clover: Boolean = false,
    val daxeLootingLvl: Int = 0,
    val daxeChimLvl: Int = 0,
    val invApi: Boolean = false,
    val magicalPower: Int = 0,
    val enrichments: Int = 0,
    val missingEnrichments: Int = 0,
    val griffinRarity: String = "",
    val griffinItem: JsonPrimitive? = null,
    val killLeaderboard: Int = 999999,
    val mythosKills: Int = 0
)

@Serializable
data class MayorResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("lastUpdated") val lastUpdated: Long,
    @SerialName("mayor") val mayor: MayorData,
    @SerialName("current") val current: ElectionData? = null,
    @SerialName("error") val error: String? = null
)

@Serializable
data class MayorData(
    @SerialName("key") val key: String,
    @SerialName("name") val name: String,
    @SerialName("perks") val perks: List<PerkData>,
    @SerialName("minister") val minister: MinisterData? = null,
    @SerialName("election") val election: ElectionData
)

@Serializable
data class PerkData(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("minister") val minister: Boolean? = null
)

@Serializable
data class MinisterData(
    @SerialName("key") val key: String,
    @SerialName("name") val name: String,
    @SerialName("perk") val perk: PerkData
)

@Serializable
data class ElectionData(
    @SerialName("year") val year: Int,
    @SerialName("candidates") val candidates: List<CandidateData>
)

@Serializable
data class CandidateData(
    @SerialName("key") val key: String,
    @SerialName("name") val name: String,
    @SerialName("perks") val perks: List<PerkData>,
    @SerialName("votes") val votes: Int? = null
)

data class Item(
    val itemId: String,
    val itemUUID: String,
    val name: String,
    val creation: Long,
    var count: Int
)