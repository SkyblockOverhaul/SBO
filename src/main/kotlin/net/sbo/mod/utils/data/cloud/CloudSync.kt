package net.sbo.mod.utils.data.cloud

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.sbo.mod.SBOKotlin
import net.sbo.mod.guis.CloudSyncGui
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SboKey
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.CloudEnvelope
import net.sbo.mod.utils.data.CloudSlotMeta
import net.sbo.mod.utils.data.CloudSlotResponse
import net.sbo.mod.utils.data.CloudStatusResponse
import net.sbo.mod.utils.data.CloudUploadRequest
import net.sbo.mod.utils.data.CloudUploadResponse
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.sbo.CloudSyncState
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.utils.events.impl.game.GameCloseEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.http.Http.getBoolean
import net.sbo.mod.utils.http.Http.getString
import net.sbo.mod.utils.http.SboApi
import net.sbo.mod.utils.overlay.OverlayManager
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object CloudSync {
    private const val SLOT = "sbo"
    const val COMMAND = "sbocloud"
    private const val TITLE = "Cloud Sync"
    private const val OPEN_HINT = "Type /$COMMAND to choose."

    private const val SBO_DATA_FILE = "SboData.json"

    private val LOCAL_ONLY = listOf("sboKey", "cloudSync")

    private const val PAST_EVENTS_FILE = "pastDianaEvents.json"
    private const val MAX_PAST_EVENTS = 100

    // Longest the game waits on close, for a running and the last upload together
    private const val CLOSE_WAIT_MS = 4_000L
    // The status endpoint allows few requests, known info is reused this long
    private const val STATUS_MAX_AGE_MS = 5 * 60 * 1000L
    // Refresh button: at most one request in this time
    private const val REFRESH_COOLDOWN_MS = 30 * 1000L
    private const val AUTO_UPLOAD_MINUTES = 30

    private val json = Json { ignoreUnknownKeys = true }
    private val gson = Gson()

    // Collecting and hashing all data is too slow for the client thread; one thread keeps the steps in order
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "SBO Cloud Sync").apply { isDaemon = true } }

    enum class SyncState { NO_SAVE, NOT_USED_HERE, BOTH_CHANGED, CLOUD_NEWER, PC_CHANGED, SAME, ERROR }

    data class Status(val state: SyncState, val updatedAt: Long = 0, val size: Int = 0, val error: String? = null, val notSupporter: Boolean = false)

    data class Action(val label: String, val hint: String, val danger: Boolean = false, val run: () -> Unit)

    // Something the player has to decide, answered in the window
    data class Question(val text: String, val actions: List<Action>)

    sealed interface CompareResult {
        data class Ready(
            val areas: List<CloudArea>,
            val pc: Map<String, String>,
            val cloud: Map<String, String>,
            val cloudVersion: Int,
            val cloudCounter: Long,
            val unsigned: Boolean,
        ) : CompareResult

        data class Failed(val message: String) : CompareResult
    }

    private sealed interface Check {
        data class Ok(val envelope: CloudEnvelope, val unsigned: Boolean) : Check
        data class Problem(val question: Question, val needsSignKey: Boolean = false) : Check
    }

    private var checkedThisSession = false

    // Old config value, read before the config drops it
    private var legacyAutoSync: Boolean? = null

    private fun state(): CloudSyncState = DataManager.sboData.cloudSync.getOrPut(Player.accountUuid()) { CloudSyncState() }

    @Volatile private var autoPaused = false
    @Volatile private var uploadInFlight: CountDownLatch? = null

    @Volatile private var downloadWaitingForSignKey = false

    // Last known cloud save info, kept up to date after our own uploads, downloads and deletes
    private class CloudInfo(val slot: CloudSlotMeta?, val at: Long)
    @Volatile private var cloudInfo: CloudInfo? = null

    private fun remember(slot: CloudSlotMeta?) {
        cloudInfo = CloudInfo(slot, System.currentTimeMillis())
    }

    @Volatile var question: Question? = null
        private set

    // Text of a running upload or download, null when idle
    @Volatile var busy: String? = null
        private set

    // Set by the open window, toasts go there instead of Minecraft's toasts
    @Volatile var toaster: ((kind: String, title: String, message: String) -> Unit)? = null

    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    // Called after anything that changes the status; returns the remover
    fun addListener(listener: () -> Unit): () -> Unit {
        listeners += listener
        return { listeners -= listener }
    }

    private fun changed() = listeners.forEach { it() }

    var autoSync: Boolean
        get() = state().autoSync ?: legacyAutoSync ?: false
        set(value) {
            state().autoSync = value
            DataManager.sboData.save()
            if (value) autoPaused = false
            changed()
        }

    fun readLegacyAutoSync() {
        val config = File(FabricLoader.getInstance().configDir.toFile(), "sbo/config.jsonc")
        legacyAutoSync = runCatching {
            JsonParser.parseString(config.readText()).asJsonObject
                .getAsJsonObject("Cloud Sync")?.get("autoSync")?.asBoolean
        }.getOrNull()
    }

    fun init() {
        Register.onTick(20 * 60 * AUTO_UPLOAD_MINUTES) { _ ->
            if (SBOKotlin.mc.level == null || !autoActive()) return@onTick
            if ((uploadInFlight?.count ?: 0L) > 0L) return@onTick
            SBOKotlin.logger.info("[CloudSync] periodic auto upload")
            upload(auto = true)
        }

        Register.command(COMMAND) {
            SBOKotlin.mc.schedule { CloudSyncGui.open() }
        }
    }

    // Older servers only send the text
    private fun notSupporter(code: String?, error: String?): Boolean =
        code == "NOT_SUPPORTER" || error?.contains("supporter feature", ignoreCase = true) == true

    // Server messages for players
    private fun friendly(error: String?): String = when {
        error == null -> "unknown error"
        "429" in error || "Rate limit" in error -> "too many requests, please wait a few minutes"
        else -> error
    }

    private fun notify(kind: String, message: String) {
        toaster?.let { return it(kind, TITLE, message) }
        val color = when (kind) {
            "error" -> ChatFormatting.RED
            "warning" -> ChatFormatting.YELLOW
            "success" -> ChatFormatting.GREEN
            else -> ChatFormatting.WHITE
        }
        SBOKotlin.mc.schedule {
            SBOKotlin.toast(
                Component.literal("SBO $TITLE").withStyle(ChatFormatting.GOLD),
                Component.literal(message).withStyle(color)
            )
        }
    }

    // Shows the question in the window, outside of it a toast points there
    private fun ask(text: String, vararg actions: Action) {
        question = Question(text, actions.toList())
        if (toaster == null) notify("warning", "$text $OPEN_HINT")
        changed()
    }

    fun dismissQuestion() {
        question = null
        changed()
    }

    private fun answer(action: () -> Unit): () -> Unit = {
        question = null
        changed()
        action()
    }

    private fun askWhichToKeep(text: String) {
        ask(
            text,
            Action("Load cloud save", "Use the settings and data from your cloud save on this PC. A backup is made first.", run = answer { download() }),
            Action("Keep this PC's data", "Replace your cloud save with the settings and data from this PC. The cloud save is lost.", danger = true, run = answer { upload(force = true) }),
        )
    }

    // Empty text removes the sign key
    fun saveSignKey(signKey: String) {
        val text = signKey.trim()
        if (text.isEmpty()) {
            CloudSyncKeys.removeKey()
            notify("info", "Sign key removed from this PC.")
            changed()
            return
        }
        if (text.length < CloudSyncKeys.MIN_SIGN_KEY_LENGTH) {
            notify("error", "The sign key needs at least ${CloudSyncKeys.MIN_SIGN_KEY_LENGTH} characters.")
            return
        }
        busy = "Saving sign key..."
        changed()
        thread(name = "SBO Cloud Key", isDaemon = true) {
            try {
                CloudSyncKeys.setSignKey(text)
                notify("success", "Sign key saved. Use the same key on all your PCs.")
                if (downloadWaitingForSignKey) {
                    downloadWaitingForSignKey = false
                    download()
                } else {
                    nextStepAfterSignKey()
                }
            } catch (e: Exception) {
                SBOKotlin.logger.error("Failed to save the sign key", e)
                notify("error", "Could not save the sign key: ${e.message}")
            } finally {
                busy = null
                changed()
            }
        }
    }

    // Suggests download or upload depending on what the cloud has
    private fun nextStepAfterSignKey() {
        withCloudInfo { slot ->
            if (slot == null) return@withCloudInfo
            if (slot.version != state().version) {
                ask("Your cloud save has data this PC does not have yet.",
                    Action("Load cloud save", "Use the settings and data from your cloud save on this PC. A backup is made first.", run = answer { download() }))
            } else {
                ask("Upload once so your cloud save is protected with the sign key too.",
                    Action("Upload", "Uploads this PC's settings and data again, now protected with your sign key", run = answer { upload() }))
            }
        }
    }

    // Known info if fresh, otherwise asks the server; skipped on errors
    private fun withCloudInfo(block: (CloudSlotMeta?) -> Unit) {
        cloudInfo?.takeIf { System.currentTimeMillis() - it.at < STATUS_MAX_AGE_MS }?.let { return block(it.slot) }
        SboApi.cloudStatus()
            .toJson<CloudStatusResponse>(ignoreUnknownKeys = true) { response ->
                if (!response.success) return@toJson SBOKotlin.logger.warn("[CloudSync] status failed: ${response.error}")
                val slot = response.slots.find { it.slot == SLOT }
                remember(slot)
                block(slot)
            }
            .error { SBOKotlin.logger.warn("[CloudSync] status failed: ${it.message}") }
    }

    // Fresh install: offers the cloud save right after /sbokey, if there is one this PC never used
    fun onSboKeySet() {
        cloudInfo = null
        if (state().version != 0) return
        SboApi.cloudStatus()
            .toJson<CloudStatusResponse>(ignoreUnknownKeys = true) { response ->
                // Not a supporter or no save: nothing to offer
                if (!response.success) return@toJson
                val slot = response.slots.find { it.slot == SLOT } ?: return@toJson
                remember(slot)
                SBOKotlin.mc.schedule {
                    notify("info", "You have a cloud save. Click Load in chat to use it on this PC.")
                    Chat.chat("§6[SBO] §eYou have a cloud save. Load your settings and data on this PC?")
                    Chat.clickableChat("§6[SBO] §b[Load cloud save]", "Loads your settings and data from the cloud. A backup is made first.") { download() }
                    Chat.clickableChat("§6[SBO] §7[Open Cloud Sync]", "Opens the Cloud Sync window") { SBOKotlin.mc.schedule { CloudSyncGui.open() } }
                }
            }
            .error { SBOKotlin.logger.warn("[CloudSync] status after /sbokey failed: ${it.message}") }
    }

    private fun autoActive(): Boolean = autoSync && !autoPaused && SboKey.get().isNotBlank()

    // Players without supporter status only hear about it in the window
    private fun pauseQuietly(reason: String) {
        SBOKotlin.logger.info("[CloudSync] auto sync paused quietly: $reason")
        autoPaused = true
        changed()
    }

    private fun pauseAuto(reason: String) {
        SBOKotlin.logger.warn("[CloudSync] auto sync paused: $reason")
        if (autoPaused) return
        autoPaused = true
        notify("error", "Auto Sync stopped for now: $reason. It starts again after an Upload or Download in /$COMMAND.")
        changed()
    }

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (checkedThisSession || !autoActive()) return
        checkedThisSession = true
        SboApi.cloudStatus()
            .toJson<CloudStatusResponse>(ignoreUnknownKeys = true) { response ->
                if (!response.success) {
                    if (notSupporter(response.code, response.error)) pauseQuietly("not a supporter")
                    else pauseAuto(friendly(response.error))
                    return@toJson
                }
                val slot = response.slots.find { it.slot == SLOT }
                remember(slot)
                worker.execute { autoDecide(slot) }
            }
            .error { pauseAuto(friendly(it.message)) }
    }

    // Worker thread
    private fun autoDecide(slot: CloudSlotMeta?) {
        val dirty = runCatching { hashOf(collectFiles()) != state().hash }.getOrDefault(true)
        SBOKotlin.logger.info("[CloudSync] join check: cloud=${slot?.version} local=${state().version} dirty=$dirty")
        when {
            slot == null -> upload(auto = true)
            slot.version == state().version -> if (dirty) upload(auto = true)
            !dirty && state().version != 0 -> download(auto = true)
            state().version == 0 -> {
                autoPaused = true
                askWhichToKeep("You already have a cloud save from another PC. Load it here? Your current data is backed up first.")
            }
            else -> {
                autoPaused = true
                askWhichToKeep("Your cloud save and this PC both have changes the other one does not have. Compare them or pick one.")
            }
        }
    }

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        SBOKotlin.logger.info("[CloudSync] disconnect, auto=${autoActive()}")
        if (autoActive()) SBOKotlin.mc.schedule { upload(auto = true) }
    }

    @SboEvent
    fun onGameClose(event: GameCloseEvent) {
        SBOKotlin.logger.info("[CloudSync] game close, auto=${autoActive()} paused=$autoPaused")
        if (!autoActive()) return
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CLOSE_WAIT_MS)
        fun left() = maxOf(0L, deadline - System.nanoTime())
        // Wait for a running upload
        uploadInFlight?.await(left(), TimeUnit.NANOSECONDS)
        val finished = upload(auto = true).await(left(), TimeUnit.NANOSECONDS)
        SBOKotlin.logger.info("[CloudSync] game close upload: ${if (finished) "done" else "timed out"}")
    }

    private fun canonical(files: Map<String, String>): String =
        files.toSortedMap().entries.joinToString("") { (name, content) -> "$name\n${content.length}\n$content\n" }

    private fun signedBytes(uuid: String, counter: Long, files: Map<String, String>): ByteArray =
        "sbo-cloud-v1\n$uuid\n$SLOT\n$counter\n${canonical(files)}".toByteArray(Charsets.UTF_8)

    private fun hashOf(files: Map<String, String>): String =
        Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(canonical(files).toByteArray(Charsets.UTF_8))
        )

    private fun configFile(): File {
        val base = FabricLoader.getInstance().configDir.resolve(SBOKotlin.settings.file)
        val json = File("$base.json")
        return if (json.exists()) json else File("$base.jsonc")
    }

    private fun backupFile(config: File): File =
        File(config.parentFile, "${config.nameWithoutExtension}.cloud-backup.${config.extension}")

    // Worker thread. Each data file is serialized once; the config text comes from memory, same as what save() writes
    private fun collectFiles(): Map<String, String> {
        val trees = DataManager.exportJson()
        (trees[SBO_DATA_FILE] as? JsonObject)?.let { sbo -> LOCAL_ONLY.forEach { sbo.remove(it) } }
        (trees[PAST_EVENTS_FILE] as? JsonObject)?.let { past ->
            val events = past.getAsJsonArray("events") ?: return@let
            if (events.size() <= MAX_PAST_EVENTS) return@let
            // Newest events are at the end
            val newest = JsonArray()
            events.toList().takeLast(MAX_PAST_EVENTS).forEach(newest::add)
            past.add("events", newest)
        }
        // Compact json, the data files are pretty printed on disk
        val files = trees.mapValues { (_, tree) -> tree.toString() }.toMutableMap()
        files[CloudDiff.CONFIG] = SBOKotlin.settings.toJsonc()
        return files
    }

    private fun withLocalFields(files: Map<String, String>): Map<String, String> {
        val raw = files[SBO_DATA_FILE] ?: return files
        val sbo = JsonParser.parseString(raw).asJsonObject
        sbo.add("cloudSync", gson.toJsonTree(DataManager.sboData.cloudSync))
        return files + (SBO_DATA_FILE to sbo.toString())
    }

    // Milliseconds until the refresh button may ask the server again
    fun refreshWaitMs(): Long = maxOf(0L, (lastStatusRequest + REFRESH_COOLDOWN_MS) - System.currentTimeMillis())

    @Volatile private var lastStatusRequest = 0L

    // Callback on the client thread; asks the server only if the known info is old or refresh is set
    fun fetchStatus(refresh: Boolean = false, callback: (Status) -> Unit) {
        val known = cloudInfo?.takeIf { System.currentTimeMillis() - it.at < STATUS_MAX_AGE_MS && !(refresh && refreshWaitMs() == 0L) }
        if (known != null) return deliverStatus(known.slot, callback)
        lastStatusRequest = System.currentTimeMillis()
        SboApi.cloudStatus()
            .toJson<CloudStatusResponse>(ignoreUnknownKeys = true) { response ->
                if (!response.success) return@toJson SBOKotlin.mc.schedule {
                    callback(Status(SyncState.ERROR, error = friendly(response.error), notSupporter = notSupporter(response.code, response.error)))
                }
                val slot = response.slots.find { it.slot == SLOT }
                remember(slot)
                deliverStatus(slot, callback)
            }
            .error { SBOKotlin.mc.schedule { callback(Status(SyncState.ERROR, error = friendly(it.message ?: "server not reachable"))) } }
    }

    // Compares on the worker, answers on the client thread
    private fun deliverStatus(slot: CloudSlotMeta?, callback: (Status) -> Unit) {
        worker.execute {
            val status = if (slot == null) Status(SyncState.NO_SAVE) else Status(syncState(slot), slot.updatedAt, slot.size)
            SBOKotlin.mc.schedule { callback(status) }
        }
    }

    // Worker thread
    private fun syncState(slot: CloudSlotMeta): SyncState {
        val dirty = runCatching { hashOf(collectFiles()) != state().hash }.getOrDefault(true)
        return when {
            state().version == 0 -> SyncState.NOT_USED_HERE
            slot.version != state().version && dirty -> SyncState.BOTH_CHANGED
            slot.version != state().version -> SyncState.CLOUD_NEWER
            dirty -> SyncState.PC_CHANGED
            else -> SyncState.SAME
        }
    }

    fun upload(force: Boolean = false) {
        upload(force, auto = false)
    }

    // Done when the upload finished, failed or had nothing to send
    private fun upload(force: Boolean = false, auto: Boolean): CountDownLatch {
        val done = CountDownLatch(1)
        uploadInFlight = done
        if (!auto) {
            busy = "Uploading..."
            changed()
        }
        val finish = {
            if (!auto) busy = null
            done.countDown()
            changed()
        }
        worker.execute {
            val files = runCatching { collectFiles() }.getOrElse {
                SBOKotlin.logger.error("Failed to collect data for the cloud upload", it)
                notify("error", "Could not read your SBO settings and data: ${it.message}")
                return@execute finish()
            }
            val hash = hashOf(files)
            val state = state()
            if (auto && hash == state.hash) {
                SBOKotlin.logger.info("[CloudSync] auto upload skipped, nothing changed")
                done.countDown()
                return@execute
            }

            val counter = maxOf(System.currentTimeMillis(), state.counter + 1)
            val uuid = Player.accountUuid()
            val signature = CloudSyncKeys.key(uuid)?.let { CloudSyncKeys.sign(it, signedBytes(uuid, counter, files)) }
            val envelope = json.encodeToString(CloudEnvelope(counter = counter, files = files, sig = signature))

            SboApi.cloudUpload(SLOT, CloudUploadRequest(envelope, state.version, force))
                .toJson<CloudUploadResponse>(ignoreUnknownKeys = true) { response ->
                    try {
                        when {
                            response.success -> {
                                state.version = response.version
                                state.counter = counter
                                state.hash = hash
                                DataManager.sboData.save()
                                remember(CloudSlotMeta(SLOT, response.version, envelope.toByteArray(Charsets.UTF_8).size, System.currentTimeMillis()))
                                question = null
                                SBOKotlin.logger.info("[CloudSync] uploaded version ${response.version} (auto=$auto)")
                                if (!auto) {
                                    autoPaused = false
                                    notify("success", "Uploaded. Your cloud save is up to date.")
                                }
                            }
                            response.conflict -> {
                                if (auto) autoPaused = true
                                cloudInfo = null
                                askWhichToKeep("Your cloud save was changed on another PC. Compare them or pick one.")
                            }
                            auto && notSupporter(response.code, response.error) -> pauseQuietly("not a supporter")
                            auto -> pauseAuto("upload failed: ${friendly(response.error)}")
                            else -> notify("error", "Upload failed: ${friendly(response.error)}")
                        }
                    } finally {
                        finish()
                    }
                }
                .error {
                    if (auto) pauseAuto("upload failed: ${friendly(it.message)}")
                    else notify("error", "Upload failed: ${friendly(it.message)}")
                    finish()
                }
        }
        return done
    }

    fun download(allowOlder: Boolean = false, allowUnsigned: Boolean = false) {
        download(allowOlder, allowUnsigned, auto = false)
    }

    private fun download(allowOlder: Boolean = false, allowUnsigned: Boolean = false, auto: Boolean) {
        if (!auto) {
            busy = "Loading your cloud save..."
            changed()
        }
        val finish = {
            if (!auto) busy = null
            changed()
        }
        SboApi.cloudDownload(SLOT)
            .toJson<CloudSlotResponse>(ignoreUnknownKeys = true) { response ->
                if (!response.success) {
                    if (auto) pauseAuto("download failed: ${friendly(response.error)}")
                    else notify("error", "Download failed: ${friendly(response.error)}")
                    return@toJson finish()
                }
                when (val check = verified(response.data, allowOlder, allowUnsigned)) {
                    is Check.Problem -> {
                        if (auto) autoPaused = true
                        if (check.needsSignKey) downloadWaitingForSignKey = true
                        ask(check.question.text, *check.question.actions.toTypedArray())
                        finish()
                    }
                    is Check.Ok -> SBOKotlin.mc.schedule {
                        remember(CloudSlotMeta(SLOT, response.version, response.data.toByteArray(Charsets.UTF_8).size, response.updatedAt))
                        apply(check.envelope, response.version, auto)
                        finish()
                    }
                }
            }
            .error {
                if (auto) pauseAuto("download failed: ${friendly(it.message)}")
                else notify("error", "Download failed: ${friendly(it.message)}")
                finish()
            }
    }

    private fun verified(data: String, allowOlder: Boolean, allowUnsigned: Boolean): Check {
        val envelope = runCatching { json.decodeFromString<CloudEnvelope>(data) }.getOrNull()
        if (envelope == null || CloudDiff.CONFIG !in envelope.files) {
            return Check.Problem(Question("Your cloud save could not be read. Update SBO and try again.", emptyList()))
        }

        val uuid = Player.accountUuid()
        val key = CloudSyncKeys.key(uuid)
        if (key == null) {
            if (envelope.sig != null) {
                return Check.Problem(Question("Your cloud save is protected with a sign key. Enter the sign key from your other PC below, loading then continues on its own.", emptyList()), needsSignKey = true)
            }
            return Check.Ok(envelope, unsigned = false)
        }

        if (envelope.sig == null) {
            if (allowUnsigned) return Check.Ok(envelope, unsigned = true)
            return Check.Problem(Question(
                "Your cloud save was uploaded without a sign key, so SBO cannot check that it really comes from you. It was not loaded.",
                listOf(Action("Load it anyway", "Only do this if you uploaded it yourself from a PC without a sign key", danger = true, run = answer { download(allowUnsigned = true) }))
            ))
        }
        if (!CloudSyncKeys.verify(key, signedBytes(uuid, envelope.counter, envelope.files), envelope.sig)) {
            return Check.Problem(Question("The sign key on this PC is not the one your cloud save was protected with. Enter the sign key from your other PC below and try again.", emptyList()))
        }
        if (envelope.counter < state().counter && !allowOlder) {
            return Check.Problem(Question(
                "Your cloud save is older than data this PC already had. It was not loaded.",
                listOf(Action("Load it anyway", "Replaces your newer settings and data on this PC with the older cloud save. A backup is made first.", danger = true, run = answer { download(allowOlder = true) }))
            ))
        }
        return Check.Ok(envelope, unsigned = false)
    }

    // false if nothing was changed
    private fun applyFiles(files: Map<String, String>): Boolean {
        val config = configFile()
        val backup = backupFile(config)
        try {
            DataManager.importAll(withLocalFields(files - CloudDiff.CONFIG))
        } catch (e: Exception) {
            SBOKotlin.logger.error("Failed to apply the cloud data", e)
            notify("error", "Your cloud save could not be loaded, nothing on this PC was changed.")
            return false
        }
        try {
            if (config.exists()) config.copyTo(backup, overwrite = true)
            config.writeText(files.getValue(CloudDiff.CONFIG))
            SBOKotlin.settings.reload()
        } catch (e: Exception) {
            SBOKotlin.logger.error("Failed to apply the cloud config", e)
            if (backup.exists()) {
                backup.copyTo(config, overwrite = true)
                SBOKotlin.settings.reload()
            }
            notify("error", "Your settings could not be loaded from the cloud, your old settings were kept. Everything else was loaded.")
        }
        OverlayManager.reloadPositions()
        return true
    }

    private fun apply(envelope: CloudEnvelope, version: Int, auto: Boolean) {
        if (!applyFiles(envelope.files)) {
            if (auto) autoPaused = true
            return
        }
        state().version = version
        state().counter = maxOf(state().counter, envelope.counter)
        question = null
        // Hash of the loaded data, read on the worker like every other hash
        worker.execute {
            state().hash = runCatching { hashOf(collectFiles()) }.getOrDefault("")
            DataManager.sboData.save()
        }
        if (auto) {
            notify("success", "Auto Sync loaded your newer settings and data from your other PC.")
        } else {
            autoPaused = false
            notify("success", "Cloud save loaded. Your old settings and data were backed up.")
        }
    }

    // Callback on the client thread
    fun compare(callback: (CompareResult) -> Unit) {
        SboApi.cloudDownload(SLOT)
            .toJson<CloudSlotResponse>(ignoreUnknownKeys = true) { response ->
                if (!response.success) {
                    return@toJson SBOKotlin.mc.schedule { callback(CompareResult.Failed(friendly(response.error))) }
                }
                val check = verified(response.data, allowOlder = true, allowUnsigned = true)
                if (check is Check.Problem) return@toJson SBOKotlin.mc.schedule { callback(CompareResult.Failed(check.question.text)) }
                check as Check.Ok
                worker.execute {
                    val cloud = check.envelope.files
                    val result = runCatching { collectFiles() }.fold(
                        onSuccess = { pc -> CompareResult.Ready(CloudDiff.compare(pc, cloud), pc, cloud, response.version, check.envelope.counter, check.unsigned) },
                        onFailure = { CompareResult.Failed("Could not read your SBO settings and data: ${it.message}") }
                    )
                    SBOKotlin.mc.schedule { callback(result) }
                }
            }
            .error { SBOKotlin.mc.schedule { callback(CompareResult.Failed(friendly(it.message ?: "server not reachable"))) } }
    }

    // Applies the picked values on this PC, then uploads the result. Reads on the worker, applies on the client thread.
    fun merge(compared: CompareResult.Ready, choices: Map<String, CloudSide>) {
        worker.execute {
            val pcNow = runCatching { collectFiles() }.getOrElse {
                notify("error", "Could not read your SBO settings and data: ${it.message}")
                return@execute
            }
            if (hashOf(pcNow) != hashOf(compared.pc)) {
                notify("warning", "Your data changed while comparing. Compare again.")
                return@execute
            }
            val merged = CloudDiff.merge(compared.pc, compared.cloud, compared.areas, choices)
            SBOKotlin.mc.schedule {
                if (merged != compared.pc && !applyFiles(merged)) return@schedule
                question = null
                // This PC now contains the cloud save, the upload builds on it
                state().version = compared.cloudVersion
                state().counter = maxOf(state().counter, compared.cloudCounter)
                DataManager.sboData.save()
                upload()
            }
        }
    }

    fun delete() {
        SboApi.cloudDelete(SLOT)
            .toJsonObject { response ->
                if (response.getBoolean("Success")) {
                    state().version = 0
                    state().hash = ""
                    DataManager.sboData.save()
                    remember(null)
                    question = null
                    if (autoSync) notify("success", "Cloud save deleted. Auto Sync is on and will upload this PC's data again soon.")
                    else notify("success", "Cloud save deleted.")
                } else {
                    notify("error", "Delete failed: ${friendly(response.getString("Error"))}")
                }
                changed()
            }
            .error { notify("error", "Delete failed: ${friendly(it.message)}") }
    }
}
