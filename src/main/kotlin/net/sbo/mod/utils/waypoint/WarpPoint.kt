package net.sbo.mod.utils.waypoint

import net.sbo.mod.utils.math.SboVec

/**
 * Represents a single warp point with coordinates and optional data.
 * @property pos The position of the warp point in the game world.
 * @property unlocked Indicates if the warp is unlocked.
 * @property setting An optional name for an associated setting.
 */
data class WarpPoint(
    val pos: SboVec = SboVec(0.0, 0.0, 0.0),
    val unlocked: Boolean,
    val setting: String? = null // Nullable, since not all warps have a setting
)

val hubWarps: Map<String, WarpPoint> = mapOf(
    "hub" to WarpPoint(SboVec(0.50, 77.00, -0.50), true)
)

val additionalHubWarps: Map<String, WarpPoint> = mapOf(
    "castle" to WarpPoint(SboVec(-250.00, 130.00, 45.00), true, "castleWarp"),
    "wizard" to WarpPoint(SboVec(44.50, 119.00, 93.50), true, "wizardWarp"),
    "crypt" to WarpPoint(SboVec(-160.50, 62.00, -106.50), true, "cryptWarp"),
    "stonks" to WarpPoint(SboVec(-36.50, 70.00, -81.50), true, "stonksWarp"),
    "da" to WarpPoint(SboVec(91.50, 75.00, 173.50), true, "darkAuctionWarp"),
    "taylor" to WarpPoint(SboVec(29.50, 73.00, -41.50), true, "taylorWarp"),
    "museum" to WarpPoint(SboVec(29.50, 72.00, 1.50), true, "museumWarp")
)

enum class AdditionalHubWarps {
    CASTLE,
    WIZARD,
    CRYPT,
    STONKS,
    DA,
    TAYLOR,
    Museum
}
