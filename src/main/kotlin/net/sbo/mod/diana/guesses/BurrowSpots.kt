package net.sbo.mod.diana.guesses

import net.sbo.mod.utils.math.SboVec
import kotlin.math.floor

/**
 * The blocks burrows can spawn on, if known. With a list, the spade guess only picks blocks on it; without one, any
 * block [ArrowGuessBurrow.isBlockValid] accepts.
 *
 * The list is read from /assets/sbo/diana/burrow_spots.txt when present: one block per line as "x y z" (the grass
 * block itself), blank lines and lines starting with # ignored. It can also be set from code with [set].
 */
object BurrowSpots {
    private const val RESOURCE = "/assets/sbo/diana/burrow_spots.txt"

    private var spots: Set<Long>? = null
    private var loaded = false

    /** Replaces the list; null goes back to no list (any valid block). */
    fun set(blocks: Collection<SboVec>?) {
        spots = blocks?.mapTo(HashSet()) { key(it) }
        loaded = true
    }

    /** Whether a burrow can be on this block, as far as the list says (true when there's no list). */
    fun isPossible(block: SboVec): Boolean {
        if (!loaded) load()
        return spots?.contains(key(block)) ?: true
    }

    private fun load() {
        loaded = true
        val text = BurrowSpots::class.java.getResourceAsStream(RESOURCE)?.use { it.readBytes().decodeToString() } ?: return
        val list = HashSet<Long>()
        for (line in text.lineSequence()) {
            val l = line.trim()
            if (l.isEmpty() || l.startsWith("#")) continue
            val p = l.split(Regex("[\\s,]+")).mapNotNull { it.toIntOrNull() }
            if (p.size == 3) list.add(key(p[0], p[1], p[2]))
        }
        if (list.isNotEmpty()) spots = list
    }

    private fun key(v: SboVec) = key(floor(v.x).toInt(), floor(v.y).toInt(), floor(v.z).toInt())

    // Hub coordinates fit easily: x, z within +-2^20, y within 0..4095
    private fun key(x: Int, y: Int, z: Int): Long =
        ((x.toLong() and 0x1FFFFF) shl 33) or ((z.toLong() and 0x1FFFFF) shl 12) or (y.toLong() and 0xFFF)
}
