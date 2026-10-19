package net.sbo.mod.utils.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AchievementStatsResponse(
    @SerialName("Success")
    val success: Boolean = false,

    @SerialName("Stats")
    val stats: AchievementStats? = null,

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

/**
 * Parts of a badge: logo (with the stat above the head), name, level, Hypixel's rank and team tag show per place;
 * stat, name, level, tag and rank have a color.
 */
object BadgePart {
    const val LOGO = "logo"
    const val STAT = "stat"
    const val NAME = "name"
    const val LEVEL = "level"
    const val TAG = "tag"
    /** Hypixel's rank in front of the name, e.g. [MVP+]. */
    const val RANK = "rank"
    /** Parts whose brackets can take their own color. */
    val BRACKETS = setOf(LEVEL, TAG, RANK)
    /** Parts of the name line that can flow on from the part in front of them; the stat is a line of its own. */
    val CHAINS = setOf(LEVEL, RANK, TAG, NAME)
}

@Serializable
data class PartColor(
    val color: String,
    val to: String? = null,
    /** Colors of the opening [ and the closing ]; null keeps the old look: the part's color, gray for the team tag. */
    val open: String? = null,
    val close: String? = null,
    /** Color of the rank's plus, e.g. the + of [MVP+]; null gives it the part's color. */
    val plus: String? = null,
    /** A BadgeEffect; null keeps the old look, see [effectOf]. */
    val effect: String? = null,
    /** Level, rank, tag and name: flows on from the part right in front, so their effects run on as one (Name Line). */
    val chain: Boolean? = null,
    /** A BadgeMotion of the chars; null stands still. */
    val motion: String? = null,
) {
    /** Badges from before effects: a gradient stat waves, everything else stands still. */
    fun effectOf(part: String): String = effect
        ?: if (part == BadgePart.STAT && to != null) BadgeEffect.WAVE else BadgeEffect.STATIC
}

/** How a part's colors move. */
object BadgeEffect {
    const val STATIC = "static"
    /** The gradient flows along the text, needs a second color. */
    const val WAVE = "wave"
    /** The whole text fades between both colors, or towards white with one color. */
    const val PULSE = "pulse"
    /** A rainbow running along the text, the colors are not used. */
    const val CHROMA = "chroma"
    /** A white gleam sweeps over the text now and then. */
    const val SHINE = "shine"
    /** Jumps between the two colors, or to a light version of one color. */
    const val FLASH = "flash"
    /** Single chars twinkle white. */
    const val SPARKLE = "sparkle"
    val ALL = listOf(STATIC, WAVE, PULSE, CHROMA, SHINE, FLASH, SPARKLE)
}

/** How a part's chars move, on top of its colors. Chars only move down, Minecraft fonts cannot rise above the line. */
object BadgeMotion {
    const val NONE = "none"
    /** The chars ride a wave along the text. */
    const val WAVY = "wavy"
    /** One char after the other hops. */
    const val BOUNCE = "bounce"
    /** The chars jitter. */
    const val SHAKE = "shake"
    /** Single chars flicker as scrambled ones, like §k. */
    const val GLITCH = "glitch"
    val ALL = listOf(NONE, WAVY, BOUNCE, SHAKE, GLITCH)
}

@Serializable
data class BadgePlaces(
    val logo: List<String> = emptyList(),
    val name: List<String> = emptyList(),
    val level: List<String> = emptyList(),
    val rank: List<String> = emptyList(),
) {
    operator fun get(part: String): List<String> = when (part) {
        BadgePart.LOGO -> logo
        BadgePart.NAME -> name
        BadgePart.LEVEL -> level
        BadgePart.RANK -> rank
        else -> emptyList()
    }

    fun with(part: String, places: List<String>): BadgePlaces = when (part) {
        BadgePart.LOGO -> copy(logo = places)
        BadgePart.NAME -> copy(name = places)
        BadgePart.LEVEL -> copy(level = places)
        BadgePart.RANK -> copy(rank = places)
        else -> this
    }
}

@Serializable
data class BadgeColors(
    val stat: PartColor? = null,
    val name: PartColor? = null,
    val level: PartColor? = null,
    val tag: PartColor? = null,
    val rank: PartColor? = null,
) {
    operator fun get(part: String): PartColor? = when (part) {
        BadgePart.STAT -> stat
        BadgePart.NAME -> name
        BadgePart.LEVEL -> level
        BadgePart.TAG -> tag
        BadgePart.RANK -> rank
        else -> null
    }

    fun with(part: String, color: PartColor): BadgeColors = when (part) {
        BadgePart.STAT -> copy(stat = color)
        BadgePart.NAME -> copy(name = color)
        BadgePart.LEVEL -> copy(level = color)
        BadgePart.TAG -> copy(tag = color)
        BadgePart.RANK -> copy(rank = color)
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
    /** Where the level is colored on badges saved before the places per part: not in chat. */
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
        TeamTagPlace.ALL.filter { it in placesOf(BadgePart.RANK) },
    )

    private fun styledColors() = BadgeColors(
        colorOf(BadgePart.STAT), colorOf(BadgePart.NAME), colorOf(BadgePart.LEVEL), colorOf(BadgePart.TAG), colorOf(BadgePart.RANK),
    )

    // The single fields follow the parts, for mods that only read those
    private fun withStyle(places: BadgePlaces, colors: BadgeColors): BadgeSettings {
        val name = colors.name ?: PartColor(color, colorTo)
        return copy(
            places = places,
            colors = colors,
            enabled = places.logo.isNotEmpty() || places.name.isNotEmpty() || places.level.isNotEmpty() || places.rank.isNotEmpty(),
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
data class AchievementStats(
    val name: String = "",
    val sbLvl: Int = -1,
    val eman9: Boolean = false,
    val mythosKills: Int = 0,
    val killLeaderboard: Int = 999999,
    // Bestiary kills per Diana mob, keyed by Hypixel's mob id
    val mythosBestiary: Map<String, Int> = emptyMap(),
    val dianaCarnivalMaxed: Boolean = false
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