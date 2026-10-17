package net.sbo.mod.utils.hubcache

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.SectionPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.chunk.DataLayer
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.levelgen.Heightmap

/**
 * In-memory copy of a real chunk: blocks, light, heightmaps and block entities. Not bound to a level, so a
 * chunk seen on one Hub server can be shown again on any other Hub server of the same connection.
 * Never changed after [capture].
 */
class ChunkSnapshot private constructor(
    private val minSectionY: Int,
    /** Null for sections that are only air. */
    private val sections: Array<LevelChunkSection?>,
    val minLightSection: Int,
    /** One entry per light section, starting at [minLightSection]. Never null. */
    val blockLight: Array<DataLayer>,
    val skyLight: Array<DataLayer>,
    private val heightmaps: Map<Heightmap.Types, LongArray>,
    private val blockEntities: List<CompoundTag>,
) {
    fun createChunk(level: ClientLevel, chunkX: Int, chunkZ: Int): FakeChunk? {
        if (level.sectionsCount != sections.size || level.minSectionY != minSectionY) return null

        // LevelChunk fills the null (air) sections in place, so it gets its own array.
        val chunk = FakeChunk(level, ChunkPos(chunkX, chunkZ), sections.copyOf())
        for ((type, data) in heightmaps) chunk.setHeightmap(type, data)
        for (tag in blockEntities) chunk.setBlockEntityNbt(tag)
        // Block entities are created lazily and not thread safe, so create them now before a render
        // thread can touch them.
        for (pos in chunk.blockEntitiesPos) chunk.getBlockEntity(pos)
        return chunk
    }

    companion object {
        private val DARK = DataLayer(0)
        private val LIT = DataLayer(15)

        /**
         * Copies [chunk] (client thread only). Returns null when the server did not send its light yet,
         * a copy without light would render black.
         */
        fun capture(chunk: LevelChunk): ChunkSnapshot? {
            val level = chunk.level
            val pos = chunk.pos
            val lightEngine = level.lightEngine
            if (!lightEngine.lightOnInColumn(SectionPos.getZeroNode(pos.x(), pos.z()))) return null

            val chunkSections = chunk.sections
            val sections = arrayOfNulls<LevelChunkSection>(chunkSections.size)
            for (i in chunkSections.indices) {
                val section = chunkSections[i]
                if (!section.hasOnlyAir()) sections[i] = section.copy()
            }

            val minLightSection = lightEngine.minLightSection
            val lightCount = lightEngine.maxLightSection - minLightSection
            val blockListener = lightEngine.getLayerListener(LightLayer.BLOCK)
            val skyListener = lightEngine.getLayerListener(LightLayer.SKY)
            val blockLight = Array(lightCount) { i ->
                compact(blockListener.getDataLayerData(SectionPos.of(pos, minLightSection + i))) ?: DARK
            }
            val skyLight = arrayOfNulls<DataLayer>(lightCount)
            for (i in 0 until lightCount) {
                skyLight[i] = compact(skyListener.getDataLayerData(SectionPos.of(pos, minLightSection + i)))
            }

            val heightmaps = HashMap<Heightmap.Types, LongArray>()
            for ((type, heightmap) in chunk.heightmaps) heightmaps[type] = heightmap.rawData.copyOf()

            val registries = level.registryAccess()
            val blockEntities = chunk.blockEntitiesPos.mapNotNull { chunk.getBlockEntityNbtForSaving(it, registries) }

            return ChunkSnapshot(
                chunk.minSectionY, sections, minLightSection, blockLight, fillSkyLight(skyLight), heightmaps, blockEntities
            )
        }

        /** Copies a light layer, or shares one instance when the whole section has the same level. */
        private fun compact(layer: DataLayer?): DataLayer? {
            if (layer == null) return null
            if (layer.isDefinitelyFilledWith(0)) return DARK
            if (layer.isDefinitelyFilledWith(15)) return LIT
            if (layer.isDefinitelyHomogenous()) return layer.copy()

            val data = layer.data
            val first = data[0]
            if ((first.toInt() and 15) == (first.toInt() shr 4 and 15) && data.all { it == first }) {
                return when (val value = first.toInt() and 15) {
                    0 -> DARK
                    15 -> LIT
                    else -> DataLayer(value)
                }
            }
            return layer.copy()
        }

        /**
         * The server leaves out sky light sections that equal the one above. Fill them in from the top:
         * fully lit above the highest section, otherwise the bottom layer of the section above.
         */
        private fun fillSkyLight(skyLight: Array<DataLayer?>): Array<DataLayer> {
            var fill: DataLayer = LIT
            for (i in skyLight.indices.reversed()) {
                val layer = skyLight[i]
                if (layer != null) {
                    fill = floodDown(layer)
                } else {
                    skyLight[i] = fill
                }
            }
            @Suppress("UNCHECKED_CAST")
            return skyLight as Array<DataLayer>
        }

        private fun floodDown(above: DataLayer): DataLayer {
            if (above === DARK || above === LIT || above.isDefinitelyHomogenous()) return above
            val aboveData = above.data
            val below = ByteArray(DataLayer.SIZE)
            // The bottom y layer of a section is its first 128 bytes, repeat it for all 16 layers.
            for (y in 0 until 16) System.arraycopy(aboveData, 0, below, y * 128, 128)
            return compact(DataLayer(below)) ?: DARK
        }
    }
}
