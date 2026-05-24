package net.sbo.mod.utils.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.GameCloseEvent
import java.io.File
import java.io.FileReader
import java.io.Writer
import java.io.FileWriter
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.FileVisitResult
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.AtomicMoveNotSupportedException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import kotlin.collections.iterator
import kotlin.reflect.KMutableProperty1
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SboDataObject {
    @JvmField
    var sboData: SboData = SboData()

    @JvmField
    var achievementsData: AchievementsData = AchievementsData()

    @JvmField
    var pastDianaEventsData: PastDianaEventsData = PastDianaEventsData()

    @JvmField
    var dianaTrackerTotal: DianaTrackerTotalData = DianaTrackerTotalData()

    @JvmField
    var dianaTrackerSession: DianaTrackerSessionData = DianaTrackerSessionData()

    @JvmField
    var dianaTrackerMayor: DianaTrackerMayorData = DianaTrackerMayorData()

    @JvmField
    var pfConfigState: PartyFinderConfigState = PartyFinderConfigState()

    @JvmField
    var partyFinderData: PartyFinderData = PartyFinderData()

    @JvmField
    var overlayData: OverlayData = OverlayData()

    lateinit var SBOConfigBundle: SboConfigBundle
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private const val MAX_BACKUPS = 10

    private val DATA_SAVER_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "sbo-data-saver-thread").apply { isDaemon = true }
    }

//    private val DATA_SAVER_EXECUTOR: ExecutorService = Executors.newThreadPerTaskExecutor(Thread
//            .ofVirtual()
//            .name("sbo-data-saver-thread-", 1) // sbo-data-saver-thread-1, sbo-data-saver-thread-2 etc. starting from 1 (second parameter)
//            .factory() // virtual threads are daemon by default
//    )

    private val caseSensitive by lazy { isCaseSensitive(FabricLoader.getInstance().configDir.toFile().toPath()) }
    val dataDir by lazy { normalizeConfigDir("sbo", FabricLoader.getInstance().configDir.toFile().toPath(), "SBO", "sbo", caseSensitive).fileName.toString() }

    private fun isCaseSensitive(baseDir: Path): Boolean {
        val tempDir = try {
            Files.createTempDirectory(baseDir, "case_test_")
        } catch (_: IOException) {
            return true // safest fallback if unsure
        }

        val fileName = "test_${System.nanoTime()}"
        val upper = tempDir.resolve(fileName.uppercase())
        val lower = tempDir.resolve(fileName.lowercase())

        return try {
            Files.createFile(upper)
            !Files.exists(lower)
        } catch (_: IOException) {
            true
        } finally {
            try { Files.deleteIfExists(upper) } catch (_: IOException) {}
            try { Files.deleteIfExists(lower) } catch (_: IOException) {}
            try { Files.deleteIfExists(tempDir) } catch (_: IOException) {}
        }
    }

    private fun mergeDirectories(modName: String, from: Path, to: Path): Boolean {
        var success = true

        Files.walkFileTree(from, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val targetDir = to.resolve(from.relativize(dir))
                try {
                    Files.createDirectories(targetDir)
                } catch (e: IOException) {
                    SBOKotlin.logger.error("[$modName] Failed to create directory: $targetDir", e)
                    success = false
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val targetFile = to.resolve(from.relativize(file))
                try {
                    if (!Files.exists(targetFile)) {
                        Files.move(file, targetFile)
                    } else {
                        SBOKotlin.logger.warn("[$modName] Skipping existing file: $targetFile")
                    }
                } catch (e: IOException) {
                    SBOKotlin.logger.error("[$modName] Failed to move file: $file", e)
                    success = false
                }
                return FileVisitResult.CONTINUE
            }
        })

        return success
    }

    private fun isEffectivelyEmpty(dir: Path): Boolean {
        if (!Files.isDirectory(dir)) return true

        Files.newDirectoryStream(dir).use { stream ->
            for (path in stream) {
                return if (Files.isDirectory(path)) {
                    isEffectivelyEmpty(path)
                } else {
                    false // file exists - not empty
                }
            }
        }

        return true
    }

    private fun deleteRecursively(modName: String, dir: Path) {
        Files.walkFileTree(dir, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                try {
                    Files.deleteIfExists(file)
                } catch (e: IOException) {
                    SBOKotlin.logger.error("[$modName] Failed to delete file: $file", e)
                }
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                try {
                    Files.deleteIfExists(dir)
                } catch (e: IOException) {
                    SBOKotlin.logger.error("[$modName] Failed to delete directory: $dir", e)
                }
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun cleanupIfSafe(modName: String, dir: Path, success: Boolean) {
        if (success) {
            if (isEffectivelyEmpty(dir)) {
                deleteRecursively(modName, dir)
                SBOKotlin.logger.info("[$modName] Cleaned up leftover $dir safely")
            } else {
                SBOKotlin.logger.info("[$modName] Migration succeeded but not cleaning up $dir due to leftover files.")
            }
        } else {
            SBOKotlin.logger.warn("[$modName] Migration failed, not deleting $dir")
        }
    }

    private fun normalizeConfigDir(modName: String, baseDir: Path, oldName: String, newName: String, caseSensitive: Boolean): Path {
        val oldDir = baseDir.resolve(oldName)
        val newDir = baseDir.resolve(newName)

        try {
            if (caseSensitive) {
                // Linux/macOS style: both directories can exist separately
                if (Files.isDirectory(oldDir)) {
                    Files.createDirectories(newDir)

                    val success = mergeDirectories(modName, oldDir, newDir)
                    cleanupIfSafe(modName, oldDir, success)
                    SBOKotlin.logger.info("[$modName] Merged config folder from $oldDir to $newDir (case-sensitive FS)")
                }
            } else {
                // Windows: case-insensitive, normalize folder name if needed
                if (Files.isDirectory(oldDir)) {
                    try {
                        if (Files.exists(newDir)) {
                            if (!Files.isDirectory(newDir)) {
                                SBOKotlin.logger.error("[$modName] Expected directory but found file: $newDir")
                                throw RuntimeException("$newDir is a file but supposed to be a directory")
                            } else {
                                val success = mergeDirectories(modName, oldDir, newDir)
                                cleanupIfSafe(modName, oldDir, success)
                                SBOKotlin.logger.info("[$modName] Merged config folder from $oldDir to $newDir (case-insensitive FS)")
                            }
                        } else {
                            val tempDir = Files.createTempDirectory(baseDir, "${newName}_tmp_")
                            try {
                                Files.move(oldDir, tempDir)
                                Files.move(tempDir, newDir)
                                SBOKotlin.logger.info("[$modName] Renamed config folder from $oldDir to $newDir")
                            } catch (e: IOException) {
                                SBOKotlin.logger.error("[$modName] Failed during rename via temp dir", e)
                                // Attempt rollback (best effort)
                                if (Files.exists(tempDir)) {
                                    try {
                                        if (!Files.exists(oldDir)) {
                                            Files.move(tempDir, oldDir)
                                        } else {
                                            // fallback: merge back if needed
                                            val rollbackSuccess = mergeDirectories(modName, tempDir, oldDir)
                                            if (rollbackSuccess && isEffectivelyEmpty(tempDir)) {
                                                deleteRecursively(modName, tempDir)
                                            } else {
                                                SBOKotlin.logger.warn("[$modName] Rollback incomplete, not deleting $tempDir")
                                            }
                                        }
                                    } catch (rollbackError: IOException) {
                                        SBOKotlin.logger.warn("[$modName] Rollback failed for $tempDir", rollbackError)
                                    }
                                }
                                throw e
                            }
                        }
                    } catch (e: IOException) {
                        SBOKotlin.logger.warn("[$modName] Failed to normalize case for $oldDir", e)
                    }
                }
            }
        } catch (e: IOException) {
            SBOKotlin.logger.error("[$modName] Failed to normalize config directory: ${e.message}", e)
        }

        return newDir
    }

    fun init() {
        SBOConfigBundle = loadAllData(dataDir)
        sboData = SBOConfigBundle.sboData
        achievementsData = SBOConfigBundle.achievementsData
        pastDianaEventsData = SBOConfigBundle.pastDianaEventsData
        dianaTrackerTotal = SBOConfigBundle.dianaTrackerTotalData
        dianaTrackerSession = SBOConfigBundle.dianaTrackerSessionData
        dianaTrackerMayor = SBOConfigBundle.dianaTrackerMayorData
        pfConfigState = SBOConfigBundle.partyFinderConfigState
        partyFinderData = SBOConfigBundle.partyFinderData
        overlayData = SBOConfigBundle.overlayData
        saveAllDataThreaded(dataDir)
        savePeriodically(5)
    }

    @SboEvent
    fun onGameClose(event: GameCloseEvent) {
        // Game is closing, if we do not block till save is complete, the game might close before save is complete, and so we might do a partial save which corrupts and resets stuff on the next launch.
        saveAndBackupAllDataThreaded(dataDir, true)
    }

    fun <T> load(modName: String, fileName: String, defaultData: T, type: Class<T>): T {
        val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), modName)
        modConfigDir.mkdirs()
        val dataFile = File(modConfigDir, fileName)

        if (!dataFile.exists()) {
            SBOKotlin.logger.info("[$modName] $fileName not found. Creating with default data.")
            save(modName, defaultData, fileName)
            return defaultData
        }

        return try {
            val loadedData = FileReader(dataFile).use { reader ->
                gson.fromJson(reader, type)
            }

            val jsonObject = FileReader(dataFile).use { reader ->
                JsonParser.parseReader(reader).asJsonObject
            }

            var dataModified = false

            if (loadedData is DianaTracker) {
                if (jsonObject.has("items")) {
                    val itemsObject = jsonObject.getAsJsonObject("items")

                    val totalTime = itemsObject.get("totalTime")?.asLong ?: 0
                    val sessionTime = itemsObject.get("sessionTime")?.asLong ?: 0
                    val mayorTime = itemsObject.get("mayorTime")?.asLong ?: 0
                    val oldTime = maxOf(totalTime, sessionTime, mayorTime)

                    if (oldTime > 0) {
                        loadedData.items.TIME = oldTime
                        dataModified = true
                    }
                }
            }

            if (loadedData is PastDianaEventsData) {
                val eventsArray = jsonObject.getAsJsonArray("events")
                if (eventsArray != null) {
                    for ((index, element) in eventsArray.withIndex()) {
                        val eventObject = element.asJsonObject
                        if (eventObject.has("items")) {
                            val itemsObject = eventObject.getAsJsonObject("items")
                            val oldTime = itemsObject.get("mayorTime")?.asLong ?: 0

                            if (oldTime > 0) {
                                loadedData.events[index].items.TIME = oldTime
                                dataModified = true
                            }
                        }
                    }
                }
            }

            if (dataModified) {
                SBOKotlin.logger.info("[$modName] Old data format detected and migrated. Saving updated file.")
                save(modName, loadedData, fileName)
            }
            loadedData
        } catch (_: JsonSyntaxException) {
            SBOKotlin.logger.error("[$modName] Error parsing JSON in $fileName, resetting to default data.")
            save(modName, defaultData, fileName)
            defaultData
        } catch (e: Exception) {
            SBOKotlin.logger.error("[$modName] Error reading data file $fileName, resetting to default data.", e)
            e.printStackTrace()
            save(modName, defaultData, fileName)
            defaultData
        }
    }

    internal fun updatePfConfigState(category: String, list: String, key: String, value: Boolean) {
        val categoryInstance: Any? = when (category) {
            "filters" -> pfConfigState.filters
            "checkboxes" -> pfConfigState.checkboxes
            else -> null
        }

        if (categoryInstance != null) {
            val listInstance: Any? = when (list) {
                "diana" -> if (category == "filters") pfConfigState.filters.diana else pfConfigState.checkboxes.diana
                "custom" -> if (category == "filters") pfConfigState.filters.custom else pfConfigState.checkboxes.custom
                else -> null
            }

            if (listInstance != null) {
                val property = listInstance::class.members.find { it.name == key }
                if (property is KMutableProperty1<*, *> && property.getter.call(listInstance) != value) {
                    property.setter.call(listInstance, value)
                    save("PartyFinderConfigState")
                }
            }
        }
    }

    internal fun updatePfConfigState(category: String, list: String, key: String, value: String) {
        if (category != "inputs") return
        val listInstance: Any? = when (list) {
            "diana" -> pfConfigState.inputs.diana
            "custom" -> pfConfigState.inputs.custom
            else -> null
        }

        if (listInstance != null) {
            val property = listInstance::class.members.find { it.name == key }
            if (property is KMutableProperty1<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val prop = property as KMutableProperty1<Any, Any?>

                val convertedValue = when (prop.returnType.classifier) {
                    Int::class -> value.toIntOrNull()
                    String::class -> value
                    else -> {
                        SBOKotlin.logger.warn("[$key] hasn an Unsupported type: ${prop.returnType.classifier}")
                        null
                    }
                }

                if (convertedValue != null) {
                    val currentValue = prop.get(listInstance)
                    if (currentValue != convertedValue) {
                        prop.set(listInstance, convertedValue)
                        save("PartyFinderConfigState")
                    }

                } else {
                    SBOKotlin.logger.warn("Failed to convert '$value' to type ${prop.returnType.classifier} for key '$key'")
                }
            }
        }
    }

    private fun loadAchievementsData(modName: String): AchievementsData {
        val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), modName)
        val dataFile = File(modConfigDir, "sbo_achievements.json")
        val defaultData = AchievementsData()

        if (!dataFile.exists()) {
            SBOKotlin.logger.info("[$modName] sbo_achievements.json not found. Creating with default data.")
            save(modName, defaultData, "sbo_achievements.json")
            return defaultData
        }

        return try {
            val typeToken = object : TypeToken<Map<String, Any>>() {}.type
            val rawData: Map<String, Any> = gson.fromJson(FileReader(dataFile), typeToken)

            val foundUnlockedIds = mutableSetOf<Int>()
            var needsMigration = false

            // migration 1: id keys ("1": true)
            for ((key, value) in rawData) {
                val id = key.toIntOrNull()
                if (id != null && value is Boolean && value) {
                    foundUnlockedIds.add(id)
                    needsMigration = true
                }
            }

            // migration 2: "unlocked" list ("unlocked": [1, 2, 3])
            val existingUnlockedList = (rawData["unlocked"] as? List<*>)?.mapNotNull {
                (it as? Number)?.toInt() ?: it.toString().toIntOrNull()
            } ?: emptyList()

            if (existingUnlockedList.isNotEmpty()) {
                foundUnlockedIds.addAll(existingUnlockedList)
                needsMigration = true
            }

            // migration 3: "achievements" map ("achievements": { "10": true })
            val oldAchievementsMap = rawData["achievements"] as? Map<*, *>
            oldAchievementsMap?.forEach { (key, value) ->
                val id = key.toString().toIntOrNull()
                if (id != null && value == true) {
                    foundUnlockedIds.add(id)
                    needsMigration = true
                }
            }

            if (rawData.containsKey("totalAchievements")) {
                // already in the new format
                load(modName, "sbo_achievements.json", defaultData, AchievementsData::class.java)
            } else if (needsMigration) {
                SBOKotlin.logger.info("[$modName] Legacy achievement format detected. Migrating...")

                val newAchievementsData = AchievementsData()
                foundUnlockedIds.forEach { id ->
                    newAchievementsData.totalAchievements[id] = 1
                }

                // save new format
                save(modName, newAchievementsData, "sbo_achievements.json")
                SBOKotlin.logger.info("[$modName] Successfully migrated ${foundUnlockedIds.size} achievements.")
                newAchievementsData
            } else {
                // default load empty files
                load(modName, "sbo_achievements.json", defaultData, AchievementsData::class.java)
            }
        } catch (e: Exception) {
            SBOKotlin.logger.error("[$modName] Error reading sbo_achievements.json, resetting to default data.", e)
            save(modName, defaultData, "sbo_achievements.json")
            defaultData
        }
    }

    fun loadAllData(modName: String): SboConfigBundle {
        val sboData = load(modName, "SboData.json", SboData(), SboData::class.java)
        val achievementsData = loadAchievementsData(modName)
        val pastDianaEventsData = load(modName, "pastDianaEvents.json", PastDianaEventsData(), PastDianaEventsData::class.java)
        val dianaTrackerTotalData = load(modName, "dianaTrackerTotal.json", DianaTrackerTotalData(), DianaTrackerTotalData::class.java)
        val dianaTrackerSessionData = load(modName, "dianaTrackerSession.json", DianaTrackerSessionData(), DianaTrackerSessionData::class.java)
        val dianaTrackerMayorData = load(modName, "dianaTrackerMayor.json", DianaTrackerMayorData(), DianaTrackerMayorData::class.java)
        val partyFinderConfigState = load(modName, "partyFinderConfigState.json", PartyFinderConfigState(), PartyFinderConfigState::class.java)
        val partyFinderData = load(modName, "partyFinderData.json", PartyFinderData(), PartyFinderData::class.java)
        val overlayData = load(modName, "overlayData.json", OverlayData(), OverlayData::class.java)
        return SboConfigBundle(sboData, achievementsData, pastDianaEventsData, dianaTrackerTotalData, dianaTrackerSessionData, dianaTrackerMayorData, partyFinderConfigState, partyFinderData, overlayData)
    }

    private fun zipFolder(folderToZip: File, zipFilePath: File) {
        ZipOutputStream(FileOutputStream(zipFilePath)).use { zos ->
            val basePath = folderToZip.toPath()

            folderToZip.walk().filter { it.isFile }.forEach { file ->
                val entryName = basePath.relativize(file.toPath()).toString()

                zos.putNextEntry(ZipEntry(entryName))

                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }

                zos.closeEntry()
            }
        }
    }

    private val backupRegex = Regex("SBOBackup_\\d{8}_\\d{6}")

    private fun cleanupBrokenBackups(modName: String, backupDir: File) {
        val files = backupDir.listFiles() ?: return

        val grouped = files.groupBy { file ->
            when {
                file.name.endsWith(".zip") -> file.name.removeSuffix(".zip")
                file.name.endsWith(".zip.part") -> file.name.removeSuffix(".zip.part")
                file.isDirectory -> file.name
                else -> file.name
            }
        }

        grouped.forEach { (base, group) ->
            val zip = group.find { it.isFile && it.name == "$base.zip" }
            val part = group.find { it.isFile && it.name == "$base.zip.part" }
            val folder = group.find { it.isDirectory && it.name == base }

            val zipInvalid = zip != null && zip.length() == 0L
            val zipMissing = zip == null
            val folderExists = folder != null

            try {
                when {
                    part != null -> {
                        SBOKotlin.logger.warn("[$modName] Removing leftover .part for $base")
                        part.delete()
                    }

                    zipInvalid -> {
                        SBOKotlin.logger.warn("[$modName] Removing empty/corrupt zip for $base")
                        zip.delete()
                        if (folderExists) folder.deleteRecursively()
                    }

                    zipMissing && folderExists -> {
                        SBOKotlin.logger.warn("[$modName] Removing orphan folder for $base (no zip)")
                        folder.deleteRecursively()
                    }
                }
            } catch (e: Exception) {
                SBOKotlin.logger.error("[$modName] Failed cleanup for backup $base", e)
            }
        }
    }

    private fun createBackup(modName: String) {
        try {
            val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), modName)
            val backupDir = File(modConfigDir, "backup")

            backupDir.mkdirs()
            cleanupBrokenBackups(modName, backupDir)

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val tempBackupDir = File(backupDir, "SBOBackup_$timestamp")
            tempBackupDir.mkdirs()

            val bundle = SBOConfigBundle
            saveToFolder(tempBackupDir, bundle.sboData, "SboData.json")
            saveToFolder(tempBackupDir, bundle.achievementsData, "sbo_achievements.json")
            saveToFolder(tempBackupDir, bundle.pastDianaEventsData, "pastDianaEvents.json")
            saveToFolder(tempBackupDir, bundle.dianaTrackerTotalData, "dianaTrackerTotal.json")
            saveToFolder(tempBackupDir, bundle.dianaTrackerSessionData, "dianaTrackerSession.json")
            saveToFolder(tempBackupDir, bundle.dianaTrackerMayorData, "dianaTrackerMayor.json")
            saveToFolder(tempBackupDir, bundle.partyFinderConfigState, "partyFinderConfigState.json")
            saveToFolder(tempBackupDir, bundle.partyFinderData, "partyFinderData.json")
            saveToFolder(tempBackupDir, bundle.overlayData, "overlayData.json")

            if (!tempBackupDir.exists() || tempBackupDir.listFiles()?.isEmpty() == true) {
                throw IOException("Backup temp directory not properly created")
            }

            val zipFile = File(backupDir, "SBOBackup_$timestamp.zip")
            val tempZipFile = File(backupDir, "SBOBackup_$timestamp.zip.part")

            try {
                zipFolder(tempBackupDir, tempZipFile)

                val isValid = tempZipFile.exists() && tempZipFile.length() > 0L

                if (isValid) {
                    if (zipFile.exists()) {
                        zipFile.delete()
                    }
                    try {
                        Files.move(
                            tempZipFile.toPath(),
                            zipFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                        )
                    } catch (e: AtomicMoveNotSupportedException) {
                        // Supported on all major OS unless trying to move to a different partition or drive
                        // The most likely culprit would be that MC was installed to a OneDrive sync folder
                        // Fallbacking to regular move should be OK here at this point.
                        val success = tempZipFile.renameTo(zipFile)
                        if (!success) {
                            throw IOException("Failed to rename temp zip to final zip")
                        }
                    }
                } else {
                    SBOKotlin.logger.error("[$modName] Backup zip was invalid, deleting temp file.")
                    tempZipFile.delete()
                    return
                }
            } catch (e: Exception) {
                SBOKotlin.logger.error("[$modName] Zip creation failed, cleaning up temp zip.", e)
                tempZipFile.delete()
                return
            }

            tempBackupDir.deleteRecursively()
            SBOKotlin.logger.info("[$modName] Created new backup: ${zipFile.name}")

            val existingBackups = backupDir.listFiles { file ->
                file.isFile &&
                file.extension == "zip" &&
                file.length() > 0L
            }?.toList() ?: emptyList()
            if (existingBackups.size > MAX_BACKUPS) {
                val oldestBackup = existingBackups.minByOrNull { it.lastModified() }
                oldestBackup?.let {
                    it.delete()
                    SBOKotlin.logger.info("[$modName] Deleted old backup: ${it.name}")
                }
            }
        } catch (e: Exception) {
            SBOKotlin.logger.error("[$modName] Error creating backup:", e)
        }
    }

    val configMapforSave = mapOf(
        "SboData" to Pair({ save(dataDir, sboData, "SboData.json") }, sboData),
        "AchievementsData" to Pair({ save(dataDir, achievementsData, "sbo_achievements.json") }, achievementsData),
        "PastDianaEventsData" to Pair({ save(dataDir, pastDianaEventsData, "pastDianaEvents.json") }, pastDianaEventsData),
        "DianaTrackerTotalData" to Pair({ save(dataDir, dianaTrackerTotal, "dianaTrackerTotal.json") }, dianaTrackerTotal),
        "DianaTrackerSessionData" to Pair({ save(dataDir, dianaTrackerSession, "dianaTrackerSession.json") }, dianaTrackerSession),
        "DianaTrackerMayorData" to Pair({ save(dataDir, dianaTrackerMayor, "dianaTrackerMayor.json") }, dianaTrackerMayor),
        "PartyFinderConfigState" to Pair({ save(dataDir, pfConfigState, "partyFinderConfigState.json") }, pfConfigState),
        "PartyFinderData" to Pair({ save(dataDir, partyFinderData, "partyFinderData.json") }, partyFinderData),
        "OverlayData" to Pair({ save(dataDir, overlayData, "overlayData.json") }, overlayData)
    )

    private fun <T> saveToFolder(folder: File, data: T, fileName: String) {
        writerForFile(File(folder, fileName)).use { writer ->
            gson.toJson(data, writer)
        }
    }

    private fun saveAllData() {
        configMapforSave.forEach { (_, configData) ->
            configData.first.invoke()
        }
    }

    fun saveAllDataThreaded(modName: String) {
        DATA_SAVER_EXECUTOR.execute {
            SBOKotlin.logger.info("[$modName] Saving all data to disk...")
            saveAllData()
            SBOKotlin.logger.info("[$modName] All data saved successfully.")
        }
    }

    fun saveAndBackupAllDataThreaded(modName: String, block: Boolean = false) {
        val future = DATA_SAVER_EXECUTOR.submit {
            SBOKotlin.logger.info("Saving all data to disk and creating backup...")
            saveAllData()
            SBOKotlin.logger.info("All data saved successfully.")
            createBackup(modName)
        }

        if (block) {
            future.get()
        }
    }

    /**
     * Saves all data periodically based on the specified interval in minutes.
     * @param interval The interval in minutes at which to save the data.
     */
    fun savePeriodically(interval: Int) {
        Register.onTick(20 * 60 * interval) { client ->
            saveAllDataThreaded(dataDir)
        }
    }

    fun <T> save(modName: String, data: T, fileName: String) {
        val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), modName)
        modConfigDir.mkdirs()
        val dataFile = File(modConfigDir, fileName)
        writerForFile(dataFile).use { writer ->
            gson.toJson(data, writer)
        }
    }

    /**
     * Obtains a writer suitable for writing to the given file.
     * The returned writer is buffered by default, which avoids repeated write syscalls.
     *
     * @return A writer suitable for writing to the given file.
     */
    fun writerForFile(file: File): Writer {
        return BufferedWriter(FileWriter(file))
    }

    /**
     * Saves the specified config by its name.
     * If the config name is not valid, it will log a warning.
     * @param configName The name of the config to save.
     */
    fun save(configName: String) {
        DATA_SAVER_EXECUTOR.execute {
            configMapforSave[configName]?.first?.invoke()
                ?: SBOKotlin.logger.warn("[$configName] is not a valid config name. Please use a valid config name")
        }
    }

    fun saveTrackerData() {
        DATA_SAVER_EXECUTOR.execute {
            save(dataDir, dianaTrackerTotal, "dianaTrackerTotal.json")
            save(dataDir, dianaTrackerSession, "dianaTrackerSession.json")
            save(dataDir, dianaTrackerMayor, "dianaTrackerMayor.json")
            SBOKotlin.logger.debug("[SBO] Diana Tracker data saved successfully.")
        }
    }
}
