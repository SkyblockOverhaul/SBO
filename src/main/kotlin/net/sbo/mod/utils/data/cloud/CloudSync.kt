package net.sbo.mod.utils.data.cloud

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import net.sbo.mod.SBOKotlin
import net.sbo.mod.guis.Guis
import net.sbo.mod.settings.categories.CloudSync
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object CloudSync {
    private const val SLOT = "sbo"
    private const val PREFIX = "§6[SBO] "
    const val PASSWORD_COMMAND = "sbosyncpassword"

    private const val CONFIG_ENTRY = "config"
    private const val SBO_DATA_FILE = "SboData.json"

    private val LOCAL_ONLY = listOf("sboKey", "cloudSync")

    private const val PAST_EVENTS_FILE = "pastDianaEvents.json"
    private const val MAX_PAST_EVENTS = 100

    private const val CLOSE_UPLOAD_TIMEOUT_SECONDS = 5L
    private const val AUTO_UPLOAD_MINUTES = 5

    private val json = Json { ignoreUnknownKeys = true }
    private val gson = Gson()
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

    private var checkedThisSession = false

    private fun state(): CloudSyncState = DataManager.sboData.cloudSync.getOrPut(Player.accountUuid()) { CloudSyncState() }

    @Volatile private var autoPaused = false
    @Volatile private var uploadInFlight: CountDownLatch? = null

    @Volatile private var downloadWaitingForPassword = false

    fun init() {
        Register.onTick(20 * 60 * AUTO_UPLOAD_MINUTES) { _ ->
            if (SBOKotlin.mc.level == null || !autoActive()) return@onTick
            if ((uploadInFlight?.count ?: 0L) > 0L) return@onTick
            SBOKotlin.logger.info("[CloudSync] periodic auto upload")
            upload(auto = true)
        }

        Register.command(PASSWORD_COMMAND) { args ->
            val password = args.joinToString(" ")
            if (password.length < CloudSyncKeys.MIN_PASSWORD_LENGTH) {
                Chat.chat("$PREFIX§cType /$PASSWORD_COMMAND followed by a password with at least ${CloudSyncKeys.MIN_PASSWORD_LENGTH} characters.")
                return@command
            }
            setPassword(password)
        }

        Register.command("sboclearsyncpassword") {
            Chat.clickableChat("$PREFIX§c[Click to remove your sync password from this PC]", "You can set it again any time with /$PASSWORD_COMMAND") {
                CloudSyncKeys.removeKey()
                Chat.chat("$PREFIX§aSync password removed from this PC.")
            }
        }
    }

    // Asks whether the cloud save or this PC's data should win
    private fun askWhichToKeep() {
        Chat.clickableChat("$PREFIX§b[Load cloud save]", "Use the settings and data from your cloud save on this PC. A backup is made first.") { download() }
        Chat.clickableChat("$PREFIX§c[Keep this PC's data]", "Replace your cloud save with the settings and data from this PC. The cloud save is lost.") { upload(force = true) }
    }

    private fun setPassword(password: String) {
        Chat.chat("$PREFIX§eSaving password...")
        thread(name = "SBO Cloud Key", isDaemon = true) {
            try {
                CloudSyncKeys.setPassword(password.toCharArray())
                Chat.chat("$PREFIX§aPassword saved. Use the exact same password on all your PCs. Write it down, it cannot be reset.")
                if (downloadWaitingForPassword) {
                    downloadWaitingForPassword = false
                    download()
                } else {
                    nextStepAfterPassword()
                }
            } catch (e: Exception) {
                SBOKotlin.logger.error("Failed to save the sync password", e)
                Chat.chat("$PREFIX§4Could not save the password: ${e.message}")
            }
        }
    }

    // Suggests download or upload depending on what the cloud has
    private fun nextStepAfterPassword() {
        SboApi.cloudStatus()
            .toJson<CloudStatusResponse>(ignoreUnknownKeys = true) { response ->
                if (!response.success) return@toJson
                val slot = response.slots.find { it.slot == SLOT }
                when {
                    slot == null -> Chat.chat("$PREFIX§eFrom now on your cloud save is protected with this password.")
                    slot.version != state().version -> {
                        Chat.chat("$PREFIX§eYour cloud save has data this PC does not have yet.")
                        Chat.clickableChat("$PREFIX§b[Load cloud save]", "Use the settings and data from your cloud save on this PC. A backup is made first.") { download() }
                    }
                    else -> {
                        Chat.chat("$PREFIX§eClick below once so your cloud save is protected with the password too.")
                        Chat.clickableChat("$PREFIX§b[Protect cloud save]", "Uploads this PC's settings and data again, now protected with your password") { upload() }
                    }
                }
            }
            .error { SBOKotlin.logger.warn("[CloudSync] status after password failed: ${it.message}") }
    }

    private fun autoActive(): Boolean =
        CloudSync.autoSync && !autoPaused && SboKey.get().isNotBlank()

    private fun pauseAuto(reason: String) {
        SBOKotlin.logger.warn("[CloudSync] auto sync paused: $reason")
        if (autoPaused) return
        autoPaused = true
        Chat.chat("$PREFIX§cAuto sync stopped for now: $reason")
        Chat.chat("$PREFIX§eIt starts again after you click Upload or Download in /sbo under Cloud Sync.")
    }

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (checkedThisSession || !autoActive()) return
        checkedThisSession = true
        SboApi.cloudStatus()
            .toJson<CloudStatusResponse>(ignoreUnknownKeys = true) { response ->
                if (!response.success) {
                    pauseAuto(response.error ?: "unknown error")
                    return@toJson
                }
                val slot = response.slots.find { it.slot == SLOT }
                SBOKotlin.mc.schedule { autoDecide(slot) }
            }
            .error { pauseAuto(it.message ?: "unknown error") }
    }

    private fun autoDecide(slot: CloudSlotMeta?) {
        val dirty = runCatching { hashOf(collectFiles()) != state().hash }.getOrDefault(true)
        SBOKotlin.logger.info("[CloudSync] join check: cloud=${slot?.version} local=${state().version} dirty=$dirty")
        when {
            slot == null -> upload(auto = true)
            slot.version == state().version -> if (dirty) upload(auto = true)
            !dirty && state().version != 0 -> download(auto = true)
            state().version == 0 -> {
                autoPaused = true
                Chat.chat("$PREFIX§eYou already have a cloud save from another PC. Do you want to use it here?")
                askWhichToKeep()
                Chat.chat("$PREFIX§7Not sure? Load the cloud save, your current data is backed up first.")
            }
            else -> {
                autoPaused = true
                Chat.chat("$PREFIX§eYour cloud save and this PC both have changes the other one does not have. Which one do you want to keep?")
                askWhichToKeep()
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
        // Wait for a running upload
        uploadInFlight?.await(CLOSE_UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val done = upload(auto = true)
        val finished = done?.await(CLOSE_UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        SBOKotlin.logger.info("[CloudSync] game close upload: ${if (done == null) "nothing to upload" else if (finished == true) "done" else "timed out"}")
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
        val base = FabricLoader.getInstance().configDir.resolve(SBOKotlin.settings.id())
        val json = File("$base.json")
        return if (json.exists()) json else File("$base.jsonc")
    }

    private fun backupFile(config: File): File =
        File(config.parentFile, "${config.nameWithoutExtension}.cloud-backup.${config.extension}")

    private fun collectFiles(): Map<String, String> {
        SBOKotlin.settings.save()
        // Compact json, the data files are pretty printed on disk
        val files = DataManager.exportAll().mapValues { (_, raw) -> JsonParser.parseString(raw).toString() }.toMutableMap()
        files[CONFIG_ENTRY] = configFile().readText()
        files[SBO_DATA_FILE]?.let { raw ->
            val sbo = JsonParser.parseString(raw).asJsonObject
            LOCAL_ONLY.forEach { sbo.remove(it) }
            files[SBO_DATA_FILE] = sbo.toString()
        }
        files[PAST_EVENTS_FILE]?.let { raw ->
            val past = JsonParser.parseString(raw).asJsonObject
            val events = past.getAsJsonArray("events") ?: return@let
            if (events.size() <= MAX_PAST_EVENTS) return@let
            // Newest events are at the end
            val newest = JsonArray()
            events.toList().takeLast(MAX_PAST_EVENTS).forEach(newest::add)
            past.add("events", newest)
            files[PAST_EVENTS_FILE] = past.toString()
        }
        return files
    }

    private fun withLocalFields(files: Map<String, String>): Map<String, String> {
        val raw = files[SBO_DATA_FILE] ?: return files
        val sbo = JsonParser.parseString(raw).asJsonObject
        sbo.add("cloudSync", gson.toJsonTree(DataManager.sboData.cloudSync))
        return files + (SBO_DATA_FILE to sbo.toString())
    }

    fun upload(force: Boolean = false) {
        upload(force, auto = false)
    }

    private fun upload(force: Boolean = false, auto: Boolean): CountDownLatch? {
        val files = runCatching { collectFiles() }.getOrElse {
            SBOKotlin.logger.error("Failed to collect data for the cloud upload", it)
            Chat.chat("$PREFIX§4Could not read your SBO settings and data: ${it.message}")
            return null
        }
        val hash = hashOf(files)
        val state = state()
        if (auto && hash == state.hash) {
            SBOKotlin.logger.info("[CloudSync] auto upload skipped, nothing changed")
            return null
        }

        val counter = maxOf(System.currentTimeMillis(), state.counter + 1)
        val uuid = Player.accountUuid()
        val signature = CloudSyncKeys.key(uuid)?.let { CloudSyncKeys.sign(it, signedBytes(uuid, counter, files)) }
        val envelope = json.encodeToString(CloudEnvelope(counter = counter, files = files, sig = signature))

        val done = CountDownLatch(1)
        uploadInFlight = done
        if (!auto) Chat.chat("$PREFIX§eUploading your settings and data...")
        SboApi.cloudUpload(SLOT, CloudUploadRequest(envelope, state.version, force))
            .toJson<CloudUploadResponse>(ignoreUnknownKeys = true) { response ->
                try {
                    when {
                        response.success -> {
                            state.version = response.version
                            state.counter = counter
                            state.hash = hash
                            DataManager.sboData.save()
                            SBOKotlin.logger.info("[CloudSync] uploaded version ${response.version} (auto=$auto)")
                            if (!auto) {
                                autoPaused = false
                                Chat.chat("$PREFIX§aUploaded. Your cloud save is up to date.")
                                if (signature == null) Chat.chat("$PREFIX§7Tip: protect your cloud save with /$PASSWORD_COMMAND <password>")
                            }
                        }
                        response.conflict -> {
                            if (auto) autoPaused = true
                            conflict()
                        }
                        auto -> pauseAuto("upload failed: ${response.error}")
                        else -> Chat.chat("$PREFIX§4Upload failed: ${response.error}")
                    }
                } finally {
                    done.countDown()
                }
            }
            .error {
                if (auto) pauseAuto("upload failed: ${it.message}")
                else Chat.chat("$PREFIX§4Upload failed: ${it.message}")
                done.countDown()
            }
        return done
    }

    private fun conflict() {
        Chat.chat("$PREFIX§eYour cloud save was changed on another PC. Which one do you want to keep?")
        askWhichToKeep()
    }

    fun download(allowOlder: Boolean = false, allowUnsigned: Boolean = false) {
        download(allowOlder, allowUnsigned, auto = false)
    }

    private fun download(allowOlder: Boolean = false, allowUnsigned: Boolean = false, auto: Boolean) {
        if (!auto) Chat.chat("$PREFIX§eLoading your cloud save...")
        SboApi.cloudDownload(SLOT)
            .toJson<CloudSlotResponse>(ignoreUnknownKeys = true) { response ->
                if (!response.success) {
                    if (auto) pauseAuto("download failed: ${response.error}")
                    else Chat.chat("$PREFIX§4Download failed: ${response.error}")
                    return@toJson
                }
                val envelope = verified(response.data, allowOlder, allowUnsigned)
                if (envelope == null) {
                    if (auto) autoPaused = true
                    return@toJson
                }
                SBOKotlin.mc.schedule { apply(envelope, response.version, auto) }
            }
            .error {
                if (auto) pauseAuto("download failed: ${it.message}")
                else Chat.chat("$PREFIX§4Download failed: ${it.message}")
            }
    }

    // null if the save can't be trusted
    private fun verified(data: String, allowOlder: Boolean, allowUnsigned: Boolean): CloudEnvelope? {
        val envelope = runCatching { json.decodeFromString<CloudEnvelope>(data) }.getOrNull()
        if (envelope == null || CONFIG_ENTRY !in envelope.files) {
            Chat.chat("$PREFIX§4Your cloud save could not be read. Update SBO and try again.")
            return null
        }

        val uuid = Player.accountUuid()
        val key = CloudSyncKeys.key(uuid)
        if (key == null) {
            if (envelope.sig != null) {
                downloadWaitingForPassword = true
                Chat.chat("$PREFIX§eYour cloud save is protected with a password. Type /$PASSWORD_COMMAND <password> with the password from your other PC, loading then continues on its own.")
                return null
            }
            return envelope
        }

        if (envelope.sig == null) {
            if (allowUnsigned) return envelope
            Chat.chat("$PREFIX§eYour cloud save was uploaded without a password, so SBO cannot check that it really comes from you. It was not loaded.")
            Chat.clickableChat("$PREFIX§c[Load it anyway]", "Only do this if you uploaded it yourself from a PC without a password") { download(allowUnsigned = true) }
            return null
        }
        if (!CloudSyncKeys.verify(key, signedBytes(uuid, envelope.counter, envelope.files), envelope.sig)) {
            Chat.chat("$PREFIX§4The password on this PC is not the one your cloud save was protected with. It was not loaded.")
            Chat.chat("$PREFIX§eType /$PASSWORD_COMMAND <password> with the password from your other PC and try again.")
            return null
        }
        if (envelope.counter < state().counter && !allowOlder) {
            Chat.chat("$PREFIX§eYour cloud save is older than data this PC already had. It was not loaded.")
            Chat.clickableChat("$PREFIX§c[Load it anyway]", "Replaces your newer settings and data on this PC with the older cloud save. A backup is made first.") { download(allowOlder = true) }
            return null
        }
        return envelope
    }

    private fun apply(envelope: CloudEnvelope, version: Int, auto: Boolean) {
        val config = configFile()
        val backup = backupFile(config)
        try {
            DataManager.importAll(withLocalFields(envelope.files - CONFIG_ENTRY))
        } catch (e: Exception) {
            SBOKotlin.logger.error("Failed to apply the cloud data", e)
            Chat.chat("$PREFIX§4Your cloud save could not be loaded, nothing on this PC was changed.")
            if (auto) autoPaused = true
            return
        }
        try {
            if (config.exists()) config.copyTo(backup, overwrite = true)
            config.writeText(envelope.files.getValue(CONFIG_ENTRY))
            SBOKotlin.settings.load { }
            SBOKotlin.settings.save()
        } catch (e: Exception) {
            SBOKotlin.logger.error("Failed to apply the cloud config", e)
            if (backup.exists()) {
                backup.copyTo(config, overwrite = true)
                SBOKotlin.settings.load { }
            }
            Chat.chat("$PREFIX§4Your settings could not be loaded from the cloud, your old settings were kept. Everything else was loaded.")
        }
        Guis.resetCachedGuis()
        OverlayManager.reloadPositions()

        state().version = version
        state().counter = maxOf(state().counter, envelope.counter)
        state().hash = runCatching { hashOf(collectFiles()) }.getOrDefault("")
        DataManager.sboData.save()
        if (auto) {
            Chat.chat("$PREFIX§aAuto sync: loaded your newer settings and data from your other PC.")
        } else {
            autoPaused = false
            Chat.chat("$PREFIX§aCloud save loaded. Your old settings and data were backed up.")
        }
    }

    fun status() {
        val password = if (CloudSyncKeys.hasKey()) "§aset" else "§7not set (optional, /$PASSWORD_COMMAND)"
        Chat.chat("$PREFIX§ePassword on this PC: $password")
        SboApi.cloudStatus()
            .toJson<CloudStatusResponse>(ignoreUnknownKeys = true) { response ->
                if (!response.success) {
                    Chat.chat("$PREFIX§4${response.error}")
                    return@toJson
                }
                val slot = response.slots.find { it.slot == SLOT }
                if (slot == null) {
                    Chat.chat("$PREFIX§eYou have no cloud save yet. Click Upload to create one.")
                    return@toJson
                }
                val updated = DATE_FORMAT.format(Instant.ofEpochMilli(slot.updatedAt))
                Chat.chat("$PREFIX§eLast upload: §b$updated")
                SBOKotlin.mc.schedule { Chat.chat("$PREFIX${syncState(slot)}") }
            }
            .error { Chat.chat("$PREFIX§4Could not check the cloud: ${it.message}") }
    }

    // CollectFiles saves the config
    private fun syncState(slot: CloudSlotMeta): String {
        val dirty = runCatching { hashOf(collectFiles()) != state().hash }.getOrDefault(true)
        return when {
            state().version == 0 -> "§eThis PC has not used your cloud save yet. Click Download to load it here."
            slot.version != state().version && dirty -> "§eYour cloud save and this PC both have changes the other one does not have."
            slot.version != state().version -> "§eYour cloud save is newer than this PC. Click Download to get it."
            dirty -> "§eThis PC has changes that are not in your cloud save yet. Click Upload to save them."
            else -> "§aThis PC and your cloud save are the same."
        }
    }

    fun delete() {
        Chat.clickableChat("$PREFIX§c[Click to delete your cloud save]", "Deletes it for good. The settings and data on this PC stay.") {
            SboApi.cloudDelete(SLOT)
                .toJsonObject { response ->
                    if (response.getBoolean("Success")) {
                        state().version = 0
                        state().hash = ""
                        DataManager.sboData.save()
                        Chat.chat("$PREFIX§aCloud save deleted.")
                        if (CloudSync.autoSync) Chat.chat("$PREFIX§eAuto Sync is on and will upload this PC's data again soon. Turn off Auto Sync in /sbo if you want no cloud save.")
                    } else {
                        Chat.chat("$PREFIX§4Delete failed: ${response.getString("Error")}")
                    }
                }
                .error { Chat.chat("$PREFIX§4Delete failed: ${it.message}") }
        }
    }
}