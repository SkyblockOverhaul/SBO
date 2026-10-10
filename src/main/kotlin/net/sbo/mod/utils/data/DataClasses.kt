package net.sbo.mod.utils.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class PartyInfo(
    @SerialName("Success")
    val success: Boolean = false,

    @SerialName("PartyInfo")
    val partyInfo: List<PartyPlayerStats> = emptyList(),
)

@Serializable
data class PlayerInfoResponse(
    @SerialName("Success")
    val success: Boolean = false,

    @SerialName("PlayerInfo")
    val playerInfo: PartyPlayerStats? = null,

    @SerialName("Error")
    val error: String? = null
)

/** Body of `POST /v2/partyInfo` and `POST /v2/partyInfoByUuids` */
@Serializable
data class MembersRequest(
    val members: List<String>,
    val readcache: Boolean = true
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
    val label: String? = null,
    val value: String? = null,
)

@Serializable
data class BadgeListResponse(
    @SerialName("Success")
    val success: Boolean = false,
    val version: String? = null,
    val unchanged: Boolean = false,
    val badges: List<SboBadge> = emptyList(),
)

/** Body of `POST /badge` */
@Serializable
data class BadgeSettings(
    val enabled: Boolean,
    val stat: String,
    val color: String,
    val colorLevel: Boolean = false,
)

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
    val settings: BadgeSettings? = null,
    val preview: SboBadge? = null,
    val hiddenByServer: Boolean = false,
    val values: Map<String, String> = emptyMap(),
    val level: Int? = null,
    val stats: List<BadgeChoice> = emptyList(),
    val colors: List<BadgeChoice> = emptyList(),
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