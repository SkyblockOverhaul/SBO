package net.sbo.mod.diana.guesses

import net.sbo.mod.utils.math.SboVec
import kotlin.math.floor

/**
 * The blocks burrows can spawn on, if known. With a list the spade guess only uses blocks on it, without one any block
 * [ArrowGuessBurrow.isBlockValid] accepts.
 *
 * The list is read from /assets/sbo/diana/burrow_spots.txt when it exists, with one grass block per line as "x y z".
 * Empty lines and lines starting with # are skipped. It can also be set with [set].
 */
object BurrowSpots {
    private const val RESOURCE = "/assets/sbo/diana/burrow_spots.txt"

    private var spots: Set<Long>? = null
    private var loaded = false

    /**
     * Replaces the list.
     * @param blocks The blocks burrows can spawn on, or null to allow any valid block.
     */
    fun set(blocks: Collection<SboVec>?) {
        spots = blocks?.mapTo(HashSet()) { key(it) }
        loaded = true
    }

    /** Whether a burrow can be on the block according to the list, always true without a list. */
    fun isPossible(block: SboVec): Boolean {
        if (!loaded) load()
        return spots?.contains(key(block)) ?: true
    }

    private fun load() {
        loaded = true
        val text = BurrowSpots::class.java.getResourceAsStream(RESOURCE)?.use { it.readBytes().decodeToString() } ?: return
        val blocks = HashSet<Long>()
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val coords = trimmed.split(Regex("[\\s,]+")).mapNotNull { it.toIntOrNull() }
            if (coords.size == 3) blocks.add(key(coords[0], coords[1], coords[2]))
        }
        if (blocks.isNotEmpty()) spots = blocks
    }

    private fun key(block: SboVec) = key(floor(block.x).toInt(), floor(block.y).toInt(), floor(block.z).toInt())

    /** Packs a block into a Long, x and z within +-2^20 and y within 0..4095 (Hub coordinates fit easily). */
    private fun key(x: Int, y: Int, z: Int): Long =
        ((x.toLong() and 0x1FFFFF) shl 33) or ((z.toLong() and 0x1FFFFF) shl 12) or (y.toLong() and 0xFFF)
}
