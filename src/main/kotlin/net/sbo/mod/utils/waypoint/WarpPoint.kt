package net.sbo.mod.utils.waypoint

import net.sbo.mod.config.Option
import net.sbo.mod.utils.math.SboVec

/**
 * Represents a single warp point with coordinates and optional data.
 * @property pos The position of the warp point in the game world.
 * @property unlocked Indicates if the warp is unlocked.
 * @property setting An optional name for an associated setting.
 * @property warpType The [AdditionalHubWarps] enum type of this [WarpPoint], null for hub.
 * @property preferWarpAgainstCompetitive The competing warp for this warp, where this warp wins over it.
 * @property extraBlocks The extra blocks to compensate for any negative time loss from exiting through this warp.
 */
data class WarpPoint(
    val pos: SboVec,
    val unlocked: Boolean,
    private val setting: String? = null, // Nullable, since not all warps have a setting
    val warpType: AdditionalHubWarps? = null,
    val preferWarpAgainstCompetitive: AdditionalHubWarps? = null, // null for hub, set for all others
    val extraBlocks: Int = 0
)

val hubWarpPoint = WarpPoint(
    pos = SboVec(0.50, 77.00, -0.50),
    unlocked = true
)

val hubWarps: Map<String, WarpPoint> = mapOf(
    "hub" to hubWarpPoint
)

val additionalHubWarps: Map<String, WarpPoint> =
    AdditionalHubWarps.entries.associate { it.lowercaseName to it.warpPoint }

/**
 * Priority is only used in case of ties in distance, lower priority wins before higher in case of a tie in distance. The Hub has priority 0 implicitly, so it always wins in case of ties with any other warps.
 * extraBlocks determines extra blocks the player has to be far from the warp for it to be eligible for warping to it.
 * preferWarpAgainstCompetitive makes it so the warp is preferred against another, if the other warp was the closest, and this warp was the second closest, up to a certain user-configured distance limit.
 *
 * See the logic at WaypointManager.kt, in getClosestWarp function.
 */
enum class AdditionalHubWarps(override val label: String, val priority: Int, val pos: SboVec, val setting: String, val preferWarpAgainstCompetitiveName: String? = null, val extraBlocks: Int = 0) : Option {
    CASTLE("Castle", 1, SboVec(-250.00, 130.00, 45.00), "castleWarp", preferWarpAgainstCompetitiveName = "CRYPT"),
    WIZARD("Wizard", 2, SboVec(44.50, 119.00, 93.50), "wizardWarp"),
    DA("Dark Auction", 3, SboVec(91.50, 75.00, 173.50), "darkAuctionWarp"),
    STONKS("Stonks", 4, SboVec(-36.50, 70.00, -81.50), "stonksWarp"),
    TAYLOR("Taylor", 5, SboVec(29.50, 73.00, -41.50), "taylorWarp"),
    MUSEUM("Museum", 6, SboVec(29.50, 72.00, 1.50), "museumWarp"),
    CRYPT("Crypt", 7, SboVec(-160.50, 62.00, -106.50), "cryptWarp", extraBlocks = 10);

    val lowercaseName: String = name.lowercase()

    val preferWarpAgainstCompetitive: AdditionalHubWarps?
        get() = preferWarpAgainstCompetitiveName?.let(::valueOf)

    val warpPoint: WarpPoint by lazy {
        WarpPoint(
            pos = pos,
            unlocked = true,
            setting = setting,
            warpType = this,
            preferWarpAgainstCompetitive = preferWarpAgainstCompetitive,
            extraBlocks = extraBlocks
        )
    }
}
