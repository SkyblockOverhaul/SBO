package net.sbo.mod.diana.burrows

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Map file: gzip of magic, version, generation, entry count, then per block short x, unsigned byte y, short z, int count.
 * New digs are appended to a journal and folded in now and then, so a save doesn't grow with the map. The journal starts
 * with the generation it belongs to: one left behind by a crash mid-fold is ignored instead of counted twice.
 */
class BurrowMapFile(private val file: Path) {
    private val journal = file.resolveSibling("${file.fileName}.log")
    private val lockFile = file.resolveSibling("${file.fileName}.lock")

    fun add(digs: Map<Long, Int>) = locked {
        val generation = readGeneration()
        if (journalGeneration() != generation) Files.deleteIfExists(journal)
        val fresh = !Files.exists(journal)
        val frame = ByteBuffer.allocate((if (fresh) 4 else 0) + 8 + digs.size * ENTRY_SIZE)
        if (fresh) frame.putInt(generation)
        frame.putInt(digs.size)
        val start = frame.position()
        for ((block, n) in digs) {
            frame.putShort((block shr 24).toShort()).put(block.toByte()).putShort((block shr 8).toShort()).putInt(n)
        }
        frame.putInt(crc(frame.array(), start, digs.size * ENTRY_SIZE))
        Files.write(journal, frame.array(), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        if (Files.size(journal) > MAX_JOURNAL_BYTES) fold()
    }

    fun compact() = locked { if (Files.exists(journal)) fold() }

    /** The whole map in the file format. */
    fun export(): ByteArray = locked {
        if (Files.exists(journal)) fold()
        if (Files.exists(file)) Files.readAllBytes(file) else encode(emptyMap(), 0)
    }

    private fun fold() {
        val (generation, counts) = readMap()
        if (journalGeneration() == generation) readJournal(counts)
        val tmp = file.resolveSibling("${file.fileName}.tmp")
        Files.write(tmp, encode(counts, generation + 1))
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        Files.deleteIfExists(journal)
    }

    private fun readGeneration(): Int {
        if (!Files.exists(file)) return 0
        return try {
            DataInputStream(GZIPInputStream(Files.newInputStream(file).buffered())).use { input ->
                require(input.readInt() == MAGIC && input.readByte().toInt() == VERSION)
                input.readInt()
            }
        } catch (_: Exception) {
            readMap().first
        }
    }

    private fun readMap(): Pair<Int, HashMap<Long, Int>> {
        val counts = HashMap<Long, Int>()
        if (!Files.exists(file)) return 0 to counts
        try {
            DataInputStream(GZIPInputStream(Files.newInputStream(file).buffered())).use { input ->
                require(input.readInt() == MAGIC && input.readByte().toInt() == VERSION)
                val generation = input.readInt()
                repeat(input.readInt()) {
                    val x = input.readShort().toInt()
                    val y = input.readUnsignedByte()
                    val z = input.readShort().toInt()
                    val n = input.readInt()
                    require(n > 0)
                    counts[key(x, y, z)] = n
                }
                // Reading to the end makes gzip check its checksum
                require(input.read() == -1)
                return generation to counts
            }
        } catch (_: Exception) {
            // Keep the damaged file and whatever the journal still holds
            Files.move(file, file.resolveSibling("${file.fileName}.bad"), StandardCopyOption.REPLACE_EXISTING)
            return (journalGeneration() ?: 0) to HashMap()
        }
    }

    // A frame cut off or damaged by a crash is skipped, the frames after it still count
    private fun readJournal(counts: MutableMap<Long, Int>) {
        val bytes = Files.readAllBytes(journal)
        val buffer = ByteBuffer.wrap(bytes)
        var at = 4
        while (bytes.size - at >= 8) {
            val n = buffer.getInt(at)
            val size = n.toLong() * ENTRY_SIZE
            if (n <= 0 || size + 8 > bytes.size - at || buffer.getInt(at + 4 + size.toInt()) != crc(bytes, at + 4, size.toInt())) {
                at++
                continue
            }
            for (i in 0 until n) {
                val o = at + 4 + i * ENTRY_SIZE
                val count = buffer.getInt(o + 5)
                if (count > 0) counts.merge(key(buffer.getShort(o).toInt(), bytes[o + 2].toInt() and 0xFF, buffer.getShort(o + 3).toInt()), count, Int::plus)
            }
            at += 8 + size.toInt()
        }
    }

    private fun journalGeneration(): Int? {
        if (!Files.exists(journal) || Files.size(journal) < 4) return null
        return Files.newInputStream(journal).use { DataInputStream(it).readInt() }
    }

    private fun encode(counts: Map<Long, Int>, generation: Int): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(GZIPOutputStream(bytes)).use { out ->
            out.writeInt(MAGIC)
            out.writeByte(VERSION)
            out.writeInt(generation)
            out.writeInt(counts.size)
            for ((block, n) in counts.toSortedMap()) {
                out.writeShort((block shr 24).toInt())
                out.writeByte((block and 0xFF).toInt())
                out.writeShort((block shr 8).toInt())
                out.writeInt(n)
            }
        }
        return bytes.toByteArray()
    }

    private fun crc(bytes: ByteArray, offset: Int, length: Int) = CRC32().apply { update(bytes, offset, length) }.value.toInt()

    // Two open game instances share these files
    private inline fun <T> locked(block: () -> T): T {
        Files.createDirectories(file.parent)
        return FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            channel.lock()
            block()
        }
    }

    companion object {
        private const val MAGIC = 0x53424D50
        private const val VERSION = 1
        private const val ENTRY_SIZE = 9
        private const val MAX_JOURNAL_BYTES = 64 * 1024

        fun key(x: Int, y: Int, z: Int) = x.toLong() shl 24 or ((z and 0xFFFF).toLong() shl 8) or y.toLong()
    }
}
