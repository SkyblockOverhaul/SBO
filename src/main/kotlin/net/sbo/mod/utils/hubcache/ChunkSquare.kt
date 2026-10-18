package net.sbo.mod.utils.hubcache

import java.lang.FunctionalInterface

@FunctionalInterface
fun interface ChunkConsumer {
    fun accept(chunkX: Int, chunkZ: Int)
}

/**
 * A square of chunks around a center. [update] reports the chunks that fell out of the old square.
 */
class ChunkSquare {
    var centerX = 0
        private set
    var centerZ = 0
        private set
    var radius = -1
        private set

    /** Returns true if the square changed. */
    fun update(newCenterX: Int, newCenterZ: Int, newRadius: Int, removed: ChunkConsumer?): Boolean {
        if (newCenterX == centerX && newCenterZ == centerZ && newRadius == radius) return false
        if (removed != null && radius >= 0) {
            for (x in centerX - radius..centerX + radius) {
                val xOutside = x < newCenterX - newRadius || x > newCenterX + newRadius
                for (z in centerZ - radius..centerZ + radius) {
                    if (xOutside || z < newCenterZ - newRadius || z > newCenterZ + newRadius) removed.accept(x, z)
                }
            }
        }
        centerX = newCenterX
        centerZ = newCenterZ
        radius = newRadius
        return true
    }

    fun updateCenter(newCenterX: Int, newCenterZ: Int, removed: ChunkConsumer?) = update(newCenterX, newCenterZ, radius, removed)

    fun updateRadius(newRadius: Int, removed: ChunkConsumer?) = update(centerX, centerZ, newRadius, removed)

    fun contains(chunkX: Int, chunkZ: Int): Boolean =
        chunkX in centerX - radius..centerX + radius && chunkZ in centerZ - radius..centerZ + radius
}
