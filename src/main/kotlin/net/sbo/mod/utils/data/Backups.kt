package net.sbo.mod.utils.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import net.sbo.mod.utils.overlay.OverlayManager
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipFile

// The zip backups DataManager makes on game close and before a cloud save is loaded
object Backups {
    private val NAME = Regex("""SBOBackup_(\d{8}_\d{6})\.zip""")
    private val STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private const val SBO_DATA_FILE = "SboData.json"

    // Belong to this PC, a backup never replaces them
    private val LOCAL_ONLY = listOf("sboKey", "cloudSync")

    private val gson = Gson()

    data class Backup(val file: File, val createdAt: Long, val size: Long)

    // Newest first
    fun list(): List<Backup> = DataManager.backupDir().listFiles().orEmpty().mapNotNull { file ->
        val stamp = NAME.matchEntire(file.name)?.groupValues?.get(1) ?: return@mapNotNull null
        val createdAt = runCatching {
            LocalDateTime.parse(stamp, STAMP).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrDefault(file.lastModified())
        Backup(file, createdAt, file.length())
    }.sortedByDescending { it.createdAt }

    // Client thread. The current data is backed up first (DataManager.importAll)
    fun load(backup: Backup) {
        val files = ZipFile(backup.file).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".json") }
                .associate { File(it.name).name to zip.getInputStream(it).readBytes().toString(Charsets.UTF_8) }
        }
        require(files.isNotEmpty()) { "the backup is empty" }
        DataManager.importAll(withLocalFields(files))
        OverlayManager.reloadPositions()
    }

    private fun withLocalFields(files: Map<String, String>): Map<String, String> {
        val raw = files[SBO_DATA_FILE] ?: return files
        val sbo = JsonParser.parseString(raw).asJsonObject
        val current = gson.toJsonTree(DataManager.sboData).asJsonObject
        LOCAL_ONLY.forEach { key -> current.get(key)?.let { sbo.add(key, it) } ?: sbo.remove(key) }
        return files + (SBO_DATA_FILE to sbo.toString())
    }
}
