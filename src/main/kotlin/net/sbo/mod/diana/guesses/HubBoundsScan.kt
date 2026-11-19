package net.sbo.mod.diana.guesses

import it.unimi.dsi.fastutil.longs.LongArrayList
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.levelgen.Heightmap
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.General
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.utils.events.impl.render.RenderEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.hubcache.ChunkSnapshot
import net.sbo.mod.utils.hubcache.HubChunkCache
import net.sbo.mod.utils.hubcache.HubIsland
import net.sbo.mod.utils.math.SboVec
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Dev only (registered in development environments): measures the Hub island to update [ArrowGuessBurrow.HUB_BOUNDS]
 * and [HubIsland], e.g. after Hypixel changes the Hub.
 *
 * - `/sbodebughubscan start` shows a HUD with the chunks next to the main island (everything connected to x 0 z 0)
 *   that were never loaded, and an arrow to the nearest one. Fly there until none are left, then every chunk of the
 *   island is cached.
 * - `/sbodebughubscan stop` (or no argument) scans all cached and loaded Hub chunks. The main island gets its grass
 *   extents in chat, a height histogram and the [HubIsland] chunk list in sbo_hubscan.txt, and sbo_hubscan.png in the
 *   game folder shows a top-down map of all grass.
 *
 * Needs the Chunk Cache with "Cache Hub Island Only" off, otherwise chunks outside [HubIsland] are never stored.
 * Grass counts like in [ArrowGuessBurrow]: a grass block with air directly above it.
 */
object HubBoundsScan {
    /** Loaded chunks are read within this many chunks around 0 0. */
    private const val CHUNK_RANGE = 50
    /** Lowest y checked for grass, below the coal mine. */
    private const val SCAN_MIN_Y = 30
    /** Map pixels per block. */
    private const val SCALE = 2
    private const val VOID_COLOR = 0x0A0F1E
    /** Ticks between HUD updates. */
    private const val UPDATE_TICKS = 20
    /** Cached chunks read per HUD update, so starting the HUD does not freeze the game. */
    private const val NEW_CHUNKS_PER_UPDATE = 150

    /** Clockwise from straight ahead, one per 45 degrees. */
    private val ARROWS = arrayOf("↑", "↗", "→", "↘", "↓", "↙", "←", "↖")

    private var active = false
    /**
     * Chunk -> its non-void columns, bit `lz * 16 + lx` set for a column with any block. Chunks never change, so each
     * is read once while the HUD runs.
     */
    private val solidByChunk = HashMap<Long, LongArray>()
    /** Number of chunks with a column of the main island, from the last HUD update. */
    private var islandChunkCount = 0
    /** Chunks next to the main island that were never loaded, the island may continue there. */
    private var gaps: List<Long> = emptyList()
    /** Cached chunks not read yet. */
    private var pending = 0

    fun init() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment) return
        Register.command("sbodebughubscan", argName = "start|stop", completions = { listOf("start", "stop") }) { args ->
            when (args.getOrNull(0)?.lowercase()) {
                "start" -> {
                    active = true
                    solidByChunk.clear()
                    Chat.chat("§6[SBO] §eHub scan started, fly to the arrow until nothing is missed, then /sbodebughubscan stop")
                }
                "stop" -> {
                    active = false
                    solidByChunk.clear()
                    gaps = emptyList()
                    islandChunkCount = 0
                    pending = 0
                    scan()
                }
                else -> scan()
            }
        }
        Register.onTick(UPDATE_TICKS) { if (active) updateCoverage() }
    }

    /** The Chunk Cache empties on disconnect, so the HUD starts over too. */
    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        // Fired on the network thread, the HUD state belongs to the client thread.
        mc.execute {
            solidByChunk.clear()
            gaps = emptyList()
            islandChunkCount = 0
            pending = 0
        }
    }

    /** Reads new loaded and cached Hub chunks, then finds the main island and its never loaded neighbors ([gaps]). */
    private fun updateCoverage() {
        val level = mc.level ?: return
        val player = mc.player ?: return
        // Loaded chunks of other areas would count as Hub chunks.
        if (!World.isInHub()) return

        // Loaded chunks first, they need no copy.
        val pcx = player.blockX shr 4
        val pcz = player.blockZ shr 4
        val radius = mc.options.renderDistance().get() + 1
        for (cx in pcx - radius..pcx + radius) for (cz in pcz - radius..pcz + radius) {
            val key = ChunkPos.pack(cx, cz)
            if (key in solidByChunk) continue
            val chunk = level.chunkSource.getChunk(cx, cz, ChunkStatus.FULL, false) as? LevelChunk ?: continue
            solidByChunk[key] = solidColumns(chunk, level.minY)
        }

        var budget = NEW_CHUNKS_PER_UPDATE
        pending = 0
        for ((key, snapshot) in HubChunkCache.cachedChunks("hub")) {
            if (key in solidByChunk) continue
            if (budget-- <= 0) {
                pending++
                continue
            }
            val chunk = snapshot.createChunk(level, ChunkPos.getX(key), ChunkPos.getZ(key)) ?: continue
            try {
                solidByChunk[key] = solidColumns(chunk, level.minY)
            } finally {
                discardCopy(chunk)
            }
        }

        val island = islandChunks()
        islandChunkCount = island.size
        gaps = island.flatMapTo(HashSet()) { key ->
            val cx = ChunkPos.getX(key)
            val cz = ChunkPos.getZ(key)
            (-1..1).flatMap { dx -> (-1..1).map { dz -> ChunkPos.pack(cx + dx, cz + dz) } }
        }.filter { it !in solidByChunk }
    }

    /**
     * Drops a chunk made by [ChunkSnapshot.createChunk]. Creating it registers its ticking block entities in the level,
     * which keeps the copy alive and ticking until the level is left. Removing the block entities unregisters them.
     */
    private fun discardCopy(chunk: LevelChunk) {
        chunk.clearAllBlockEntities()
    }

    /** The non-void columns of [chunk] as 4 longs. A column is void like in [scan]. */
    private fun solidColumns(chunk: LevelChunk, minY: Int): LongArray {
        val bits = LongArray(4)
        for (lx in 0..15) for (lz in 0..15) {
            if (chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz) - 1 < minY + 1) continue
            val i = lz * 16 + lx
            bits[i shr 6] = bits[i shr 6] or (1L shl (i and 63))
        }
        return bits
    }

    /**
     * Chunks with a column of the main island: a flood fill over the non-void columns in [solidByChunk] from x 0 z 0.
     * Same as [mainIsland], but on the stored bits, so it is cheap enough to run every update.
     */
    private fun islandChunks(): Set<Long> {
        val visited = HashMap<Long, LongArray>()
        val queue = LongArrayList()
        fun visit(x: Int, z: Int) {
            val chunkKey = ChunkPos.pack(x shr 4, z shr 4)
            val solid = solidByChunk[chunkKey] ?: return
            val i = (z and 15) * 16 + (x and 15)
            val bit = 1L shl (i and 63)
            if ((solid[i shr 6] and bit) == 0L) return
            val seen = visited.getOrPut(chunkKey) { LongArray(4) }
            if ((seen[i shr 6] and bit) != 0L) return
            seen[i shr 6] = seen[i shr 6] or bit
            queue.add(columnKey(x, z))
        }
        visit(0, 0)
        var next = 0
        while (next < queue.size) {
            val key = queue.getLong(next++)
            val x = columnX(key)
            val z = columnZ(key)
            visit(x + 1, z)
            visit(x - 1, z)
            visit(x, z + 1)
            visit(x, z - 1)
        }
        return visited.keys
    }

    /** The scan HUD: island chunks, missed chunks, an arrow to the nearest one and missed chunks per side. */
    @SboEvent
    fun onRender(event: RenderEvent) {
        if (!active) return
        val player = mc.player ?: return
        val lines = mutableListOf<String>()
        lines += "§6Hub scan §7$islandChunkCount island chunks, ${if (gaps.isEmpty()) "§a" else "§c"}${gaps.size} missed" +
            if (pending > 0) " §7(reading $pending cached)" else ""
        if (!World.isInHub()) lines += "§cNot in the Hub, paused"
        if (!General.hubChunkCache || HubChunkCache.cachedChunks("hub").isEmpty()) lines += "§cChunk Cache is not storing chunks!"
        if (General.chunkCacheHubIslandOnly) lines += "§cTurn off \"Cache Hub Island Only\", it clips the cache"

        val px = player.x
        val pz = player.z
        fun centerX(key: Long) = ChunkPos.getX(key) * 16 + 8.0
        fun centerZ(key: Long) = ChunkPos.getZ(key) * 16 + 8.0
        val nearest = gaps.minByOrNull { (centerX(it) - px).let { dx -> dx * dx } + (centerZ(it) - pz).let { dz -> dz * dz } }
        if (nearest != null) {
            val dx = centerX(nearest) - px
            val dz = centerZ(nearest) - pz
            val dist = sqrt(dx * dx + dz * dz).toInt()
            // Yaw 0 looks at +z and grows clockwise, so this is the target's angle from straight ahead, clockwise.
            val targetYaw = Math.toDegrees(atan2(-dx, dz))
            val relative = Math.floorMod((targetYaw - player.yRot).roundToInt(), 360)
            val arrow = ARROWS[((relative + 22) / 45) % 8]
            lines += "§eNearest missed: §f§l$arrow §e${dist}m §7(x ${centerX(nearest).toInt()}, z ${centerZ(nearest).toInt()})"

            // Missed chunks per compass side of the player, north = -z.
            var n = 0
            var e = 0
            var s = 0
            var w = 0
            for (key in gaps) {
                val gx = centerX(key) - px
                val gz = centerZ(key) - pz
                if (abs(gx) > abs(gz)) {
                    if (gx > 0) e++ else w++
                } else {
                    if (gz > 0) s++ else n++
                }
            }
            lines += "§7N §f$n  §7E §f$e  §7S §f$s  §7W §f$w"
        } else if (islandChunkCount == 0) {
            lines += "§eGo to the Hub spawn, x 0 z 0 is not loaded yet"
        } else if (pending == 0) {
            lines += "§aWhole island loaded, /sbodebughubscan stop"
        }

        val x = 6
        var y = mc.window.guiScaledHeight / 3
        val width = lines.maxOf { mc.font.width(it) } + 6
        event.context.fill(x - 3, y - 3, x + width - 3, y + lines.size * (mc.font.lineHeight + 1) + 1, 0x90000000.toInt())
        for (line in lines) {
            event.context.text(mc.font, line, x, y, -1, true)
            y += mc.font.lineHeight + 1
        }
    }

    /** A block column packed into a long, x in the high and z in the low 32 bits. */
    private fun columnKey(x: Int, z: Int): Long = (x.toLong() shl 32) or (z.toLong() and 0xFFFFFFFFL)
    private fun columnX(key: Long): Int = (key shr 32).toInt()
    private fun columnZ(key: Long): Int = key.toInt()

    /**
     * One grass block with air above.
     *
     * @property covered Something other than air and leaves is above it (cave, roof, overhang).
     */
    private class Grass(val x: Int, val y: Int, val z: Int, val covered: Boolean)

    /** Scans all cached and loaded Hub chunks and writes the map and the report, see [HubBoundsScan]. Takes a few seconds. */
    private fun scan() {
        val level = mc.level ?: return
        val chunks = HashMap<Long, LevelChunk>()
        val store = HubChunkCache.cachedChunks("hub")
        val copies = ArrayList<LevelChunk>(store.size)
        try {
            for ((key, snapshot) in store) {
                val copy = snapshot.createChunk(level, ChunkPos.getX(key), ChunkPos.getZ(key)) ?: continue
                copies += copy
                chunks[key] = copy
            }
            var live = 0
            // Loaded chunks of other areas would count as Hub chunks.
            if (World.isInHub()) {
                for (cx in -CHUNK_RANGE..CHUNK_RANGE) for (cz in -CHUNK_RANGE..CHUNK_RANGE) {
                    val chunk = level.chunkSource.getChunk(cx, cz, ChunkStatus.FULL, false) as? LevelChunk ?: continue
                    chunks[ChunkPos.pack(cx, cz)] = chunk
                    live++
                }
            }
            if (chunks.isEmpty()) {
                Chat.chat("§6[SBO] §cNo Hub chunks loaded or cached")
                return
            }
            writeReport(level.minY, chunks, store.size, live)
        } finally {
            copies.forEach(::discardCopy)
        }
    }

    /**
     * Writes the map and the report for [chunks].
     *
     * @param minY The level's lowest y, columns without a block above it are void.
     * @param cached Number of chunks from the Chunk Cache, [live] the number of loaded ones.
     */
    private fun writeReport(minY: Int, chunks: Map<Long, LevelChunk>, cached: Int, live: Int) {

        val minCX = chunks.keys.minOf { ChunkPos.getX(it) }
        val maxCX = chunks.keys.maxOf { ChunkPos.getX(it) }
        val minCZ = chunks.keys.minOf { ChunkPos.getZ(it) }
        val maxCZ = chunks.keys.maxOf { ChunkPos.getZ(it) }
        val originX = minCX * 16
        val originZ = minCZ * 16
        val image = BufferedImage((maxCX - minCX + 1) * 16 * SCALE, (maxCZ - minCZ + 1) * 16 * SCALE, BufferedImage.TYPE_INT_RGB)

        val solidColumns = HashSet<Long>()
        val grassByColumn = HashMap<Long, List<Grass>>()

        for ((key, chunk) in chunks) {
            val cx = ChunkPos.getX(key)
            val cz = ChunkPos.getZ(key)
            for (lx in 0..15) for (lz in 0..15) {
                val x = cx * 16 + lx
                val z = cz * 16 + lz
                // The heightmap holds the first free y above the top block.
                val topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz) - 1
                if (topY < minY + 1) {
                    paint(image, x - originX, z - originZ, VOID_COLOR)
                    continue
                }
                solidColumns.add(columnKey(x, z))

                // All grass in the column, not only the top block, so grass under trees and in caves counts too.
                val grass = mutableListOf<Grass>()
                var aboveIsAir = true
                var covered = false
                for (y in topY downTo SCAN_MIN_Y) {
                    val state = chunk.getBlockState(BlockPos(x, y, z))
                    val block = state.block
                    if (block == Blocks.GRASS_BLOCK && aboveIsAir) grass += Grass(x, y, z, covered)
                    aboveIsAir = block == Blocks.AIR
                    if (!aboveIsAir && !state.`is`(BlockTags.LEAVES)) covered = true
                }
                if (grass.isNotEmpty()) grassByColumn[columnKey(x, z)] = grass

                // Grass is bright (green inside the current bounds, magenta outside), everything else dark grey.
                val top = grass.firstOrNull()
                val color = if (top != null) {
                    val shade = ((top.y - 60).coerceIn(0, 60) * 135 / 60) + 120
                    if (inBounds(top)) (shade / 3 shl 16) or (shade shl 8) or (shade / 4) else (shade shl 16) or (shade / 5 shl 8) or shade
                } else {
                    ((topY - 50).coerceIn(0, 100) * 50 / 100 + 35) * 0x010101
                }
                paint(image, x - originX, z - originZ, color)
            }
        }

        val main = mainIsland(solidColumns)
        val mainGrass = main.mapNotNull { grassByColumn[it] }.flatten()

        drawOverlay(image, originX, originZ)
        val png = File(mc.gameDirectory, "sbo_hubscan.png")
        ImageIO.write(image, "png", png)

        val islandChunks = main.mapTo(HashSet()) { ChunkPos.pack(Math.floorDiv(columnX(it), 16), Math.floorDiv(columnZ(it), 16)) }
        // The flood fill stops at chunks that were never loaded, so the island may continue in these.
        val gaps = islandChunks.flatMapTo(HashSet()) { key ->
            val cx = ChunkPos.getX(key)
            val cz = ChunkPos.getZ(key)
            (-1..1).flatMap { dx -> (-1..1).map { dz -> ChunkPos.pack(cx + dx, cz + dz) } }
        }.filter { it !in chunks }

        val report = mutableListOf<String>()
        report += "Chunks: ${chunks.size} ($cached cached, $live live), never loaded chunks next to the main island: ${gaps.size}"
        report += "Map: ${png.absolutePath}, top left = x $originX z $originZ, $SCALE pixels = 1 block, grid every 50 blocks"
        if (mainGrass.isEmpty()) {
            report += "No grass on the main island (the island at x 0 z 0)"
        } else {
            val open = mainGrass.filter { !it.covered }
            fun pos(g: Grass) = "${g.x} ${g.y} ${g.z}"
            report += "Main island: ${main.size} columns, ${mainGrass.size} grass blocks (${mainGrass.size - open.size} covered)," +
                " ${mainGrass.count { !inBounds(it) }} outside current bounds"
            report += "  min X ${mainGrass.minBy { it.x }.let(::pos)}, max X ${mainGrass.maxBy { it.x }.let(::pos)}"
            report += "  min Z ${mainGrass.minBy { it.z }.let(::pos)}, max Z ${mainGrass.maxBy { it.z }.let(::pos)}"
            report += "  min Y ${mainGrass.minBy { it.y }.let(::pos)}, max Y ${mainGrass.maxBy { it.y }.let(::pos)}"
            if (open.isNotEmpty()) {
                report += "  open sky: min Y ${open.minBy { it.y }.let(::pos)}, max Y ${open.maxBy { it.y }.let(::pos)}"
            }
        }
        report.forEach { Chat.chat("§6[SBO] §e${it.take(300)}") }

        val details = mutableListOf<String>()
        details += report
        details += ""
        details += "Main island grass per y: total, open sky, covered, examples (x y z, * = covered)"
        for ((y, list) in mainGrass.groupBy { it.y }.toSortedMap()) {
            val covered = list.count { it.covered }
            val examples = list.sortedBy { it.covered }.take(6).joinToString(", ") { "${it.x} ${it.y} ${it.z}" + if (it.covered) "*" else "" }
            details += "y $y: ${list.size}, open ${list.size - covered}, covered $covered: $examples"
        }
        details += ""
        details += "Main island chunks for HubIsland.ISLAND_CHUNKS (chunk z to chunk x ranges):"
        for ((cz, row) in islandChunks.groupBy({ ChunkPos.getZ(it) }, { ChunkPos.getX(it) }).toSortedMap()) {
            val runs = mutableListOf<IntRange>()
            for (cx in row.sorted()) {
                val last = runs.lastOrNull()
                if (last != null && last.last + 1 == cx) runs[runs.lastIndex] = last.first..cx else runs += cx..cx
            }
            details += "        $cz to listOf(${runs.joinToString(", ") { "${it.first}..${it.last}" }}),"
        }
        details += ""
        details += "Never loaded chunks next to the main island, fly there and scan again (chunk center x z): " +
            gaps.joinToString(", ") { "${ChunkPos.getX(it) * 16 + 8} ${ChunkPos.getZ(it) * 16 + 8}" }
        val txt = File(mc.gameDirectory, "sbo_hubscan.txt")
        txt.writeText(details.joinToString("\n"))
        Chat.chat("§6[SBO] §eHeight histogram: ${txt.absolutePath}")
        SBOKotlin.logger.info("[HubBoundsScan]\n" + report.joinToString("\n"))
    }

    /** All non-void columns connected to x 0 z 0. The islands around the Hub are separated from it by void. */
    private fun mainIsland(solidColumns: Set<Long>): Set<Long> {
        val start = columnKey(0, 0)
        if (start !in solidColumns) return emptySet()
        val island = HashSet<Long>()
        val queue = ArrayDeque<Long>()
        island.add(start)
        queue.add(start)
        while (queue.isNotEmpty()) {
            val key = queue.removeFirst()
            val x = columnX(key)
            val z = columnZ(key)
            for (next in longArrayOf(columnKey(x + 1, z), columnKey(x - 1, z), columnKey(x, z + 1), columnKey(x, z - 1))) {
                if (next in solidColumns && island.add(next)) queue.add(next)
            }
        }
        return island
    }

    private fun inBounds(grass: Grass): Boolean =
        ArrowGuessBurrow.isInHubBounds(SboVec(grass.x.toDouble(), grass.y.toDouble(), grass.z.toDouble()))

    /** Fills the [SCALE] x [SCALE] pixels of the block at [bx] [bz], counted from the image's top left block. */
    private fun paint(image: BufferedImage, bx: Int, bz: Int, color: Int) {
        for (sx in 0 until SCALE) for (sz in 0 until SCALE) image.setRGB(bx * SCALE + sx, bz * SCALE + sz, color)
    }

    /**
     * Current bounds in red, x 0 / z 0 in blue, a grid every 50 blocks. The edge of the chunks "Hub Island Only" keeps
     * ([HubIsland]) in yellow. Never seen chunks stay black.
     */
    private fun drawOverlay(image: BufferedImage, originX: Int, originZ: Int) {
        fun px(x: Int, z: Int, rgb: Int) {
            val ix = (x - originX) * SCALE
            val iz = (z - originZ) * SCALE
            if (ix in 0 until image.width && iz in 0 until image.height) image.setRGB(ix, iz, rgb)
        }
        for (x in originX until originX + image.width / SCALE) for (z in originZ until originZ + image.height / SCALE) {
            val cx = Math.floorDiv(x, 16)
            val cz = Math.floorDiv(z, 16)
            val inside = HubIsland.contains(cx, cz)
            val edge = inside != HubIsland.contains(Math.floorDiv(x + 1, 16), cz) ||
                inside != HubIsland.contains(Math.floorDiv(x - 1, 16), cz) ||
                inside != HubIsland.contains(cx, Math.floorDiv(z + 1, 16)) ||
                inside != HubIsland.contains(cx, Math.floorDiv(z - 1, 16))
            if (inside && edge) px(x, z, 0xFFD020)
        }
        val b = ArrowGuessBurrow.HUB_BOUNDS
        for (x in b.minX.toInt()..b.maxX.toInt()) { px(x, b.minZ.toInt(), 0xFF2020); px(x, b.maxZ.toInt(), 0xFF2020) }
        for (z in b.minZ.toInt()..b.maxZ.toInt()) { px(b.minX.toInt(), z, 0xFF2020); px(b.maxX.toInt(), z, 0xFF2020) }
        for (x in originX until originX + image.width / SCALE) for (z in originZ until originZ + image.height / SCALE) {
            val onAxis = (x == 0 || z == 0) && (x + z) % 3 == 0
            val onGrid = (x % 50 == 0 || z % 50 == 0) && (x + z) % 6 == 0
            if (onAxis) px(x, z, 0x6080FF) else if (onGrid) px(x, z, 0x304070)
        }
    }
}
