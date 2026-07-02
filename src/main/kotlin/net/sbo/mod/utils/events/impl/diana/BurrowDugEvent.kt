package net.sbo.mod.utils.events.impl.diana

import net.sbo.mod.utils.math.SboVec

/**
 * Event emitted when a burrow is dug.
 *
 * @param burrowPos   The position of the burrow that was dug.
 * @param waypointPos The position of the waypoint that was dug.
 * @param lastBlock   The position of the last block interacted with. (this may be off by 1-2 blocks from the actual burrow position) useful for the case that the burrow was not found.
 * @param currentBurrow The current number of burrows dug.
 * @param maxBurrow     The maximum number of burrows that can be dug.
 */
class BurrowDugEvent (
    private val burrowPos: SboVec?,
    private val waypointPos: SboVec?,
    val lastBlock: SboVec?,
    val currentBurrow: Int,
    val maxBurrow: Int
)
