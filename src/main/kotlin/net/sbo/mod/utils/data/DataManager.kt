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
import net.sbo.mod.utils.data.configs.achievements.AchievementsData
import net.sbo.mod.utils.data.configs.diana.*
import net.sbo.mod.utils.data.configs.overlay.OverlayData
import net.sbo.mod.utils.data.configs.partyfinder.PartyFinderConfigState
import net.sbo.mod.utils.data.configs.partyfinder.PartyFinderData
import net.sbo.mod.utils.data.configs.sbo.SboData
import net.sbo.mod.utils.data.configs.sound.SoundSettingsData
import java.io.*
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties

/**
 * Central class for all data operations.
 * Manages atomic writes, backup, executor, and registry.
 */
object DataManager {
    @JvmField @DataField("SboData.json")
    var sboData: SboData = SboData()

    @JvmField @DataField("sbo_achievements.json")
    var achievementsData: AchievementsData = AchievementsData()

    @JvmField @DataField("pastDianaEvents.json")
    var pastDianaEventsData: PastDianaEventsData = PastDianaEventsData()

    @JvmField @DataField("dianaTrackerTotal.json")
    var dianaTrackerTotal: DianaTrackerTotalData = DianaTrackerTotalData()

    @JvmField @DataField("dianaTrackerSession.json")
    var dianaTrackerSession: DianaTrackerSessionData = DianaTrackerSessionData()

    @JvmField @DataField("dianaTrackerMayor.json")
    var dianaTrackerMayor: DianaTrackerMayorData = DianaTrackerMayorData()

    @JvmField @DataField("partyFinderConfigState.json")
    var pfConfigState: PartyFinderConfigState = PartyFinderConfigState()

    @JvmField @DataField("partyFinderData.json")
    var partyFinderData: PartyFinderData = PartyFinderData()

    @JvmField @DataField("overlayData.json")
    var overlayData: OverlayData = OverlayData()

    @JvmField @DataField("soundSettingsData.json")
    var soundSettingsData: SoundSettingsData = SoundSettingsData()

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private const val MAX_BACKUPS = 10

    private val DATA_SAVER_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "sbo-data-saver-thread").apply { isDaemon = true }
    }

    private val caseSensitive by lazy { isCaseSensitive(FabricLoader.getInstance().configDir.toFile().toPath()) }
    val dataDir by lazy { normalizeConfigDir("sbo", FabricLoader.getInstance().configDir.toFile().toPath(), "SBO", "sbo", caseSensitive).fileName.toString() }

    private val dirtyConfigs = ConcurrentHashMap.newKeySet<String>()
    private val dirtySaveQueued = AtomicBoolean(false)

    init {
        registerDataFields()
    }

    /**
     * Registers all @DataField annotated fields with DataRegistry using reflection.
     * Called once at init time.
     */
    @Suppress("UNCHECKED_CAST")
    private fun registerDataFields() {
        val clazz = this::class.java
        clazz.declaredFields
            .filter { it.isAnnotationPresent(DataField::class.java) }
            .forEach { field ->
                val annotation = field.getAnnotation(DataField::class.java)
                val fieldName = field.name
                val fileName = annotation.fileName
                val entryName = fieldName.replaceFirstChar { it.uppercase() }
                val fieldType = field.type as Class<Any>

                // Create a getter that reads the field value via reflection
                val getter = {
                    field.isAccessible = true
                    field.get(this) as Any
                }

                // Register with DataRegistry
                val entry = ConfigEntry(entryName, fileName, fieldType, getter)
                DataRegistry.register(entry)
            }
    }

    fun init() {
        loadAllData(dataDir)
        saveAllDataThreaded(dataDir)
        savePeriodically(5)
        Register.onTick(20) { saveDirtyData() }
    }

    @SboEvent
    fun onGameClose(event: GameCloseEvent) {
        saveAndBackupAllDataThreadedBlocking(dataDir)
    }

    // === GENERIC OPERATIONS ===

    fun <T : Any> load(modName: String, fileName: String, clazz: Class<T>, defaultSupplier: () -> T): T {
        val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), modName)
        modConfigDir.mkdirs()
        val dataFile = File(modConfigDir, fileName)

        if (!dataFile.exists()) {
            SBOKotlin.logger.info("[$modName] $fileName not found. Creating with default data.")
            val data = defaultSupplier()
            writeJsonAtomically(dataFile, data)
            return data
        }

        return try {
            FileReader(dataFile).use { reader ->
                gson.fromJson(reader, clazz)
            }
        } catch (_: JsonSyntaxException) {
            SBOKotlin.logger.error("[$modName] Error parsing JSON in $fileName, resetting to default data.")
            writeJsonAtomically(dataFile, defaultSupplier())
            defaultSupplier()
        } catch (e: Exception) {
            SBOKotlin.logger.error("[$modName] Error reading $fileName, resetting to default data.", e)
            writeJsonAtomically(dataFile, defaultSupplier())
            defaultSupplier()
        }
    }

    fun <T> write(modName: String, data: T, fileName: String) {
        val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), modName)
        modConfigDir.mkdirs()
        val dataFile = File(modConfigDir, fileName)
        writeJsonAtomically(dataFile, data as Any)
    }

    fun save(configName: String) {
        if (DataRegistry.contains(configName)) {
            dirtyConfigs.add(configName)
        } else {
            SBOKotlin.logger.warn("[$configName] is not a valid config name.")
        }
    }

    private fun loadAllData(modName: String) {
        this::class.java.declaredFields
            .filter { it.isAnnotationPresent(DataField::class.java) }
            .forEach { field ->
                field.isAccessible = true
                val annotation = field.getAnnotation(DataField::class.java)
                val fileName = annotation.fileName
                @Suppress("UNCHECKED_CAST")
                val data = loadDataForFile(modName, fileName, field.type as Class<Any>) { field.type.getDeclaredConstructor().newInstance() }
                field.set(this, data)
            }
    }

    /**
     * Loads data for a specific file, using special loaders for certain files.
     */
    @Suppress("UNCHECKED_CAST", "USELESS_CAST")
    private fun loadDataForFile(modName: String, fileName: String, fieldType: Class<*>, defaultSupplier: () -> Any): Any {
        return when (fileName) {
            "sbo_achievements.json" -> loadAchievementsData(modName)
            "dianaTrackerTotal.json", "dianaTrackerSession.json", "dianaTrackerMayor.json" -> {
                loadDianaTrackerData(modName, fileName, fieldType as Class<DianaTracker>, defaultSupplier as () -> DianaTracker)
            }
            else -> load(modName, fileName, fieldType as Class<Any>, defaultSupplier)
        }
    }

    private fun <T : DianaTracker> loadDianaTrackerData(modName: String, fileName: String, clazz: Class<T>, defaultSupplier: () -> T): T {
        val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), modName)
        modConfigDir.mkdirs()
        val dataFile = File(modConfigDir, fileName)

        if (!dataFile.exists()) {
            val data = defaultSupplier()
            writeJsonAtomically(dataFile, data)
            return data
        }

        return try {
            val loadedData = FileReader(dataFile).use { reader ->
                gson.fromJson(reader, clazz)
            }

            // Legacy migration: convert old time format (items.totalTime) to new (items.TIME)
            val jsonObject = FileReader(dataFile).use { reader ->
                JsonParser.parseReader(reader).asJsonObject
            }

            if (jsonObject.has("items")) {
                val itemsObject = jsonObject.getAsJsonObject("items")
                val totalTime = itemsObject.get("totalTime")?.asLong ?: 0
                val sessionTime = itemsObject.get("sessionTime")?.asLong ?: 0
                val mayorTime = itemsObject.get("mayorTime")?.asLong ?: 0
                val oldTime = maxOf(totalTime, sessionTime, mayorTime)

                if (oldTime > 0) {
                    loadedData.items.TIME = oldTime
                    SBOKotlin.logger.info("[$modName] Old DianaTracker time format detected and migrated.")
                    writeJsonAtomically(dataFile, loadedData)
                }
            }

            loadedData
        } catch (_: JsonSyntaxException) {
            SBOKotlin.logger.error("[$modName] Error parsing JSON in $fileName, resetting to default data.")
            val data = defaultSupplier()
            writeJsonAtomically(dataFile, data)
            data
        } catch (e: Exception) {
            SBOKotlin.logger.error("[$modName] Error reading $fileName, resetting to default data.", e)
            val data = defaultSupplier()
            writeJsonAtomically(dataFile, data)
            data
        }
    }

    private fun saveAllData() {
        DataRegistry.entries.forEach { entry ->
            @Suppress("UNCHECKED_CAST")
            val typedEntry = entry as ConfigEntry<Any>
            write(dataDir, typedEntry.getter(), typedEntry.fileName)
        }
    }

    private fun saveAllDataThreaded(modName: String) {
        DATA_SAVER_EXECUTOR.execute {
            SBOKotlin.logger.info("[$modName] Saving all data to disk...")
            saveAllData()
            SBOKotlin.logger.info("[$modName] All data saved successfully.")
        }
    }

    private fun saveAndBackupAllDataThreadedBlocking(modName: String) {
        DATA_SAVER_EXECUTOR.submit {
            SBOKotlin.logger.info("Saving all data to disk and creating backup...")
            saveAllData()
            SBOKotlin.logger.info("All data saved successfully.")
            createBackup(modName)
        }.get()
    }

    private fun saveDirtyData() {
        if (dirtyConfigs.isEmpty() || !dirtySaveQueued.compareAndSet(false, true)) return

        try {
            DATA_SAVER_EXECUTOR.execute {
                try {
                    while (true) {
                        val configName = dirtyConfigs.firstOrNull() ?: break
                        if (!dirtyConfigs.remove(configName)) continue
                        val entry = DataRegistry.get(configName) ?: continue
                        @Suppress("UNCHECKED_CAST")
                        write(dataDir, (entry as ConfigEntry<Any>).getter(), entry.fileName)
                    }
                } finally {
                    dirtySaveQueued.set(false)
                }
            }
        } catch (e: RuntimeException) {
            dirtySaveQueued.set(false)
            throw e
        }
    }

    /**
     * Saves all data periodically based on the specified interval in minutes.
     * @param interval The interval in minutes at which to save the data.
     */
    private fun savePeriodically(interval: Int) {
        Register.onTick(20 * 60 * interval) { _ ->
            saveAllDataThreaded(dataDir)
        }
    }

    // === BACKUP ===

    private fun createBackup(modName: String) {
        try {
            val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), modName)
            val backupDir = File(modConfigDir, "backup")

            backupDir.mkdirs()
            cleanupBrokenBackups(modName, backupDir)

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val tempBackupDir = File(backupDir, "SBOBackup_$timestamp")
            tempBackupDir.mkdirs()

            DataRegistry.entries.forEach { entry ->
                @Suppress("UNCHECKED_CAST")
                val typedEntry = entry as ConfigEntry<Any>
                saveToFolder(tempBackupDir, typedEntry.getter(), typedEntry.fileName)
            }

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
                    } catch (_: AtomicMoveNotSupportedException) {
                        val success = tempZipFile.renameTo(zipFile)
                        if (!success) {
                            throw IOException("Failed to rename temp zip")
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

    private fun <T> saveToFolder(folder: File, data: T, fileName: String) {
        writeJsonAtomically(File(folder, fileName), data as Any)
    }

    // === ATOMIC WRITE ===


    /**
     * Writes JSON data to a temporary file, then replaces the original file with it
     * only after the temp file has finished fully writing. Performs atomic move to avoid
     * data loss if crash or power loss during the move.
     * Includes retry logic to handle transient Windows lock errors (AV, indexing, etc.)
     */
    private fun writeJsonAtomically(file: File, data: Any) {
        val parentDirectory = file.parentFile

        parentDirectory.listFiles { candidate ->
            candidate.name.startsWith("${file.name}.") && candidate.name.endsWith(".tmp")
        }?.forEach(File::delete)

        val tempFile = Files.createTempFile(parentDirectory.toPath(), "${file.name}.", ".tmp")

        BufferedWriter(FileWriter(tempFile.toFile())).use { writer ->
            gson.toJson(data, writer)
        }

        val maxAttempts = 5

        for (attempt in 1..maxAttempts) {
            try {
                try {
                    Files.move(
                        tempFile,
                        file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    // Hopefully no power loss or crash because non-atomic move here
                    // This code path won't be taken unless outdated OS, weird partition setup or OneDrive (lame)
                    Files.move(
                        tempFile,
                        file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
                return
            } catch (_: IOException) {
                // AccessDeniedException, FileSystemException etc land here (Windows AV/indexing lock)
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(50L * attempt)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
        }

        // Give up after retries: try direct write as last resort (not atomic, but better than nothing)
        SBOKotlin.logger.warn("[sbo] Atomic move failed, attempting direct write for ${file.name}")
        try {
            BufferedWriter(FileWriter(file)).use { writer ->
                gson.toJson(data, writer)
            }
            SBOKotlin.logger.info("[sbo] Direct write succeeded for ${file.name}")
        } catch (e: IOException) {
            SBOKotlin.logger.error("[sbo] Direct write also failed for ${file.name}", e)
        } finally {
            // Clean up temp file regardless of direct write outcome
            try {
                Files.deleteIfExists(tempFile)
            } catch (_: IOException) {
            }
        }
    }

    // === ACHIEVEMENTS SPECIAL ===

    private fun loadAchievementsData(modName: String): AchievementsData {
        val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), modName)
        val dataFile = File(modConfigDir, "sbo_achievements.json")
        val defaultData = AchievementsData()

        if (!dataFile.exists()) {
            SBOKotlin.logger.info("[$modName] sbo_achievements.json not found. Creating with default data.")
            writeJsonAtomically(dataFile, defaultData)
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
                load(modName, "sbo_achievements.json", AchievementsData::class.java) { AchievementsData() }
            } else if (needsMigration) {
                SBOKotlin.logger.info("[$modName] Legacy achievement format detected. Migrating...")

                val newAchievementsData = AchievementsData()
                foundUnlockedIds.forEach { id ->
                    newAchievementsData.totalAchievements[id] = 1
                }

                writeJsonAtomically(dataFile, newAchievementsData)
                SBOKotlin.logger.info("[$modName] Successfully migrated ${foundUnlockedIds.size} achievements.")
                newAchievementsData
            } else {
                load(modName, "sbo_achievements.json", AchievementsData::class.java) { AchievementsData() }
            }
        } catch (e: Exception) {
            SBOKotlin.logger.error("[$modName] Error reading sbo_achievements.json, resetting to default data.", e)
            try {
                writeJsonAtomically(dataFile, defaultData)
            } catch (saveError: Exception) {
                SBOKotlin.logger.error("[$modName] Also failed to save default achievements data", saveError)
            }
            defaultData
        }
    }

    // === HELPER FUNCTIONS ===

    private fun isCaseSensitive(baseDir: Path): Boolean {
        val tempDir = try {
            Files.createTempDirectory(baseDir, "case_test_")
        } catch (_: IOException) {
            return true
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

    private fun normalizeConfigDir(modName: String, baseDir: Path, oldName: String, newName: String, caseSensitive: Boolean): Path {
        val oldDir = baseDir.resolve(oldName)
        val newDir = baseDir.resolve(newName)

        try {
            if (caseSensitive) {
                if (Files.isDirectory(oldDir)) {
                    Files.createDirectories(newDir)
                    val success = mergeDirectories(modName, oldDir, newDir)
                    cleanupIfSafe(modName, oldDir, success)
                    SBOKotlin.logger.info("[$modName] Merged config folder from $oldDir to $newDir (case-sensitive FS)")
                }
            } else {
                if (Files.isDirectory(oldDir)) {
                    try {
                        if (Files.exists(newDir) && !Files.isSameFile(oldDir, newDir)) {
                            if (!Files.isDirectory(newDir)) {
                                SBOKotlin.logger.error("[$modName] Expected directory but found file: $newDir")
                                throw RuntimeException("$newDir is a file but supposed to be a directory")
                            }
                            val success = mergeDirectories(modName, oldDir, newDir)
                            cleanupIfSafe(modName, oldDir, success)
                            SBOKotlin.logger.info("[$modName] Merged config folder from $oldDir to $newDir (case-insensitive FS)")
                        } else {
                            val tempDir = Files.createTempDirectory(baseDir, "${newName}_tmp_")
                            Files.delete(tempDir)
                            try {
                                Files.move(oldDir, tempDir)
                                Files.move(tempDir, newDir)
                                SBOKotlin.logger.info("[$modName] Renamed config folder from $oldDir to $newDir")
                            } catch (e: IOException) {
                                SBOKotlin.logger.error("[$modName] Failed during rename via temp dir", e)
                                if (Files.exists(tempDir)) {
                                    try {
                                        if (!Files.exists(oldDir)) {
                                            Files.move(tempDir, oldDir)
                                        } else {
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
                return@isEffectivelyEmpty Files.isDirectory(path) && isEffectivelyEmpty(path)
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

    fun saveTrackerData() {
        save("DianaTrackerTotalData")
        save("DianaTrackerSessionData")
        save("DianaTrackerMayorData")
    }

    fun updatePfConfigState(category: String, list: String, key: String, value: Boolean) {
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

    fun updatePfConfigState(category: String, list: String, key: String, value: String) {
        if (category != "inputs" && category != "textInputTexts") return

        val listInstance: Any? = when (list) {
            "diana" -> pfConfigState.inputs.diana
            "custom" -> pfConfigState.inputs.custom
            else -> null
        }

        if (listInstance != null) {
            val property = listInstance::class.members.find { it.name == key }
            if (property is KMutableProperty1<*, *>) {
                val currentValue = property.getter.call(listInstance)
                val convertedValue = when (property.returnType.classifier) {
                    Int::class -> value.toIntOrNull()
                    String::class -> value
                    else -> {
                        return
                    }
                }
                if (currentValue != convertedValue) {
                    if (convertedValue != null) {
                        property.setter.call(listInstance, convertedValue)
                        save("PartyFinderConfigState")
                    }
                }
            }
        }
    }
}
