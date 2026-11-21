package net.sbo.mod.diana.burrows

import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.waypoint.Waypoint

internal class Burrow(
    val pos: SboVec,
    var hasFootstep: Boolean = false,
    var hasEnchant: Boolean = false,
    var type: String? = null,
    var waypoint: Waypoint? = null
)

