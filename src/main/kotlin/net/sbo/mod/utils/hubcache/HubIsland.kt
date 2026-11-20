package net.sbo.mod.utils.hubcache

/**
 * The chunks of the Hub island itself, without the islands around it (The Park, Spider's Den, The Barn...).
 * Used by "Cache Hub Island Only" and "Render Hub Island Only".
 */
object HubIsland {
    /**
     * Chunk x ranges per chunk z of every chunk with a block of the main Hub island (everything connected to x 0 z 0).
     * Measured with /sbodebughubscan (dev client), which writes this list to sbo_hubscan.txt.
     *
     * TODO: merged from the live Hub and the Alpha Network (Nov 2026). Alpha moved the colosseum away from the east
     *  side (x 112..172, z -14..56 on live) and the fairy forest goes further east (up to x 203). Re-scan once the
     *  alpha changes are live and drop the chunks that are gone.
     */
    private val ISLAND_CHUNKS: Map<Int, List<IntRange>> = mapOf(
        -13 to listOf(-10..0),
        -12 to listOf(-10..2),
        -11 to listOf(-11..4),
        -10 to listOf(-12..6),
        -9 to listOf(-13..8),
        -8 to listOf(-13..9),
        -7 to listOf(-13..9),
        -6 to listOf(-13..8),
        -5 to listOf(-14..10),
        -4 to listOf(-14..10),
        -3 to listOf(-15..9),
        -2 to listOf(-15..9),
        -1 to listOf(-15..10),
        0 to listOf(-15..11),
        1 to listOf(-16..11),
        2 to listOf(-17..10),
        3 to listOf(-18..10),
        4 to listOf(-18..10),
        5 to listOf(-18..12),
        6 to listOf(-18..13),
        7 to listOf(-17..13),
        8 to listOf(-17..12),
        9 to listOf(-17..-11, -5..12),
        10 to listOf(-3..11),
        11 to listOf(2..9),
        12 to listOf(3..8),
    )

    // Bounds of [ISLAND_CHUNKS] plus the ring around it
    private val minX = ISLAND_CHUNKS.values.flatten().minOf { it.first } - 1
    private val minZ = ISLAND_CHUNKS.keys.min() - 1
    private val width = ISLAND_CHUNKS.values.flatten().maxOf { it.last } + 1 - minX + 1
    private val depth = ISLAND_CHUNKS.keys.max() + 1 - minZ + 1

    /**
     * [ISLAND_CHUNKS] plus one chunk around them (vanilla only renders a chunk once its neighbors are there), as a grid
     * over their bounds: [contains] runs for every chunk lookup, an array index is cheaper than a hash set.
     */
    private val chunks = BooleanArray(width * depth).apply {
        for ((z, ranges) in ISLAND_CHUNKS) for (range in ranges) for (x in range) {
            for (dx in -1..1) for (dz in -1..1) this[(z + dz - minZ) * width + (x + dx - minX)] = true
        }
    }

    /** True for chunks of the Hub island and the ring around it. Runs for every chunk lookup. */
    @JvmStatic
    fun contains(chunkX: Int, chunkZ: Int): Boolean {
        val x = chunkX - minX
        val z = chunkZ - minZ
        return x in 0 until width && z in 0 until depth && chunks[z * width + x]
    }
}
