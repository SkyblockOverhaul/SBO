package net.sbo.mod.utils.events.impl.diana

/**
 * Event emitted when a mob or treasure burrow is fully dug.
 * To listen for the first dug on a treasure or mob burrow, see the regexes with Register.onChatMessage at BurrowDetector.kt.
 * <p>
 * This will also not fire for the Start burrows, or on a second dug of the burrow at the chain end. Again look at BurrowDetector.kt
 * example to catch these.
 *
 * @param currentBurrow The current number of burrows dug.
 * @param maxBurrow     The maximum number of burrows that can be dug.
 */
class BurrowDugEvent (
    val currentBurrow: Int,
    val maxBurrow: Int
)
