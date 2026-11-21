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
    // val success: Boolean = false,
    // val lastUpdated: Long = 0,
    val products: Map<String, Product> = emptyMap()
)

@Suppress("PropertyName")
@Serializable
data class Product(
    // val product_id: String,
    // val sell_summary: List<SummaryItem>,
    // val buy_summary: List<SummaryItem>,
    val quick_status: QuickStatus
)

/*
@Serializable
data class SummaryItem(
    val amount: Int,
    val pricePerUnit: Double,
    val orders: Int
)
*/

@Serializable
data class QuickStatus(
    // val productId: String,
    val sellPrice: Double,
    val buyPrice: Double
    // val sellVolume: Int,
    // val sellMovingWeek: Int,
    // val sellOrders: Int,
    // val buyVolume: Int,
    // val buyMovingWeek: Int,
    // val buyOrders: Int
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
