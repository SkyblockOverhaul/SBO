package net.sbo.mod.utils.events.impl.diana

import net.sbo.mod.utils.math.SboVec

/**
 * Event emitted when a mob or treasure burrow is fully dug.
 * To listen for the first dug on a treasure or mob burrow, see the regexes with Register.onChatMessage at BurrowDetector.kt.
 * <p>
 * This will also not fire for the Start burrows, or on a second dug of the burrow at the chain end. Again look at BurrowDetector.kt
 * example to catch these.
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
    private val lastBlock: SboVec?,
    val currentBurrow: Int,
    val maxBurrow: Int
)
