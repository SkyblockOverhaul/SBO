package net.sbo.mod.general

import com.google.gson.JsonParser
import com.google.gson.Gson
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import net.sbo.mod.SBOKotlin
import net.sbo.mod.guis.Guis
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.CloudEnvelope
import net.sbo.mod.utils.data.CloudSlotMeta
import net.sbo.mod.utils.data.CloudSlotResponse
import net.sbo.mod.utils.data.CloudStatusResponse
import net.sbo.mod.utils.data.CloudUploadRequest
import net.sbo.mod.utils.data.CloudUploadResponse
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.DataManager.sboData
import net.sbo.mod.utils.data.configs.sbo.CloudSyncState
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.utils.events.impl.game.GameCloseEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.http.Http.getBoolean
import net.sbo.mod.utils.http.Http.getString
import net.sbo.mod.utils.http.SboApi
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SboKey
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import net.sbo.mod.settings.categories.CloudSync as CloudSyncSettings

object CloudSync {
    private const val SLOT = "sbo"
    private const val PREFIX = "§6[SBO] "
    const val PASSWORD_COMMAND = "sbosyncpassword"

    private const val CONFIG_ENTRY = "config"
    private const val SBO_DATA_FILE = "SboData.json"

    private val LOCAL_ONLY = listOf("sboKey", "cloudSync")
    private val NOT_SYNCED = setOf("pastDianaEvents.json")

    private const val CLOSE_UPLOAD_TIMEOUT_SECONDS = 5L
    private const val AUTO_UPLOAD_MINUTES = 5

    private val json = Json { ignoreUnknownKeys = true }
    private val gson = Gson()
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

    private var checkedThisSession = false

    private fun state(): CloudSyncState = sboData.cloudSync.getOrPut(Player.accountUuid()) { CloudSyncState() }

    @Volatile private var autoPaused = false
    @Volatile private var uploadInFlight: CountDownLatch? = null

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
                Chat.chat("$PREFIX§cUsage: /$PASSWORD_COMMAND <password>, at least ${CloudSyncKeys.MIN_PASSWORD_LENGTH} characters.")
                return@command
            }
            setPassword(password)
        }

        Register.command("sboclearsyncpassword") {
            Chat.clickableChat("$PREFIX§c[Click to remove the sync password from this PC]", "Downloads are no longer verified afterwards") {
                CloudSyncKeys.removeKey()
                Chat.chat("$PREFIX§aSync password removed from this PC.")
            }
        }
    }

    private fun setPassword(password: String) {
        Chat.chat("$PREFIX§eSaving sync password...")
        thread(name = "SBO Cloud Key", isDaemon = true) {
            try {
                CloudSyncKeys.setPassword(password.toCharArray())
                Chat.chat("$PREFIX§aSync password set. Use the same password on every PC, SBO cannot recover it.")
                Chat.chat("$PREFIX§eUpload once so the cloud copy is protected.")
            } catch (e: Exception) {
                SBOKotlin.logger.error("Failed to save the sync password", e)
                Chat.chat("$PREFIX§4Could not save the sync password: ${e.message}")
            }
        }
    }

    private fun autoActive(): Boolean =
        CloudSyncSettings.autoSync && !autoPaused && SboKey.get().isNotBlank()

    private fun pauseAuto(reason: String) {
        SBOKotlin.logger.warn("[CloudSync] auto sync paused: $reason")
        if (autoPaused) return
        autoPaused = true
        Chat.chat("$PREFIX§cAuto sync paused for this session: $reason")
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
            else -> {
                autoPaused = true
                Chat.chat("$PREFIX§eAuto sync: the cloud has a save from another PC, but this PC has changes that were never uploaded.")
                Chat.clickableChat("$PREFIX§b[Load cloud save]", "Replace this PC's config and data with the cloud save") { download() }
                Chat.clickableChat("$PREFIX§c[Keep this PC's data]", "Overwrite the cloud save with this PC's config and data") { upload(force = true) }
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
        val files = (DataManager.exportAll() - NOT_SYNCED).toMutableMap()
        files[CONFIG_ENTRY] = configFile().readText()
        files[SBO_DATA_FILE]?.let { raw ->
            val sbo = JsonParser.parseString(raw).asJsonObject
            LOCAL_ONLY.forEach { sbo.remove(it) }
            files[SBO_DATA_FILE] = sbo.toString()
        }
        return files
    }

    private fun withLocalFields(files: Map<String, String>): Map<String, String> {
        val raw = files[SBO_DATA_FILE] ?: return files
        val sbo = JsonParser.parseString(raw).asJsonObject
        sbo.add("cloudSync", gson.toJsonTree(sboData.cloudSync))
        return files + (SBO_DATA_FILE to sbo.toString())
    }

    fun upload(force: Boolean = false) {
        upload(force, auto = false)
    }

    private fun upload(force: Boolean = false, auto: Boolean): CountDownLatch? {
        val files = runCatching { collectFiles() }.getOrElse {
            SBOKotlin.logger.error("Failed to collect data for the cloud upload", it)
            Chat.chat("$PREFIX§4Could not read your SBO data: ${it.message}")
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
        if (!auto) Chat.chat("$PREFIX§eUploading config and data...")
        SboApi.cloudUpload(SLOT, CloudUploadRequest(envelope, state.version, force))
            .toJson<CloudUploadResponse>(ignoreUnknownKeys = true) { response ->
                try {
                    when {
                        response.success -> {
                            state.version = response.version
                            state.counter = counter
                            state.hash = hash
                            sboData.save()
                            SBOKotlin.logger.info("[CloudSync] uploaded version ${response.version} (auto=$auto)")
                            if (!auto) {
                                autoPaused = false
                                val protection = if (signature != null) "signed" else "not signed, no sync password set"
                                Chat.chat("$PREFIX§aConfig and data uploaded to the cloud ($protection).")
                            }
                        }
                        response.conflict -> {
                            if (auto) autoPaused = true
                            conflict(response.version)
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

    private fun conflict(cloudVersion: Int) {
        Chat.chat("$PREFIX§eThe cloud has a newer save (version $cloudVersion) than this PC (version ${state().version}).")
        Chat.clickableChat("$PREFIX§b[Download cloud save]", "Replace your local config and data with the cloud ones") { download() }
        Chat.clickableChat("$PREFIX§c[Overwrite cloud save]", "Replace the cloud save with your local config and data") { upload(force = true) }
    }

    fun download(allowOlder: Boolean = false) {
        download(allowOlder, auto = false)
    }

    private fun download(allowOlder: Boolean = false, auto: Boolean) {
        if (!auto) Chat.chat("$PREFIX§eDownloading config and data...")
        SboApi.cloudDownload(SLOT)
            .toJson<CloudSlotResponse>(ignoreUnknownKeys = true) { response ->
                if (!response.success) {
                    if (auto) pauseAuto("download failed: ${response.error}")
                    else Chat.chat("$PREFIX§4Download failed: ${response.error}")
                    return@toJson
                }
                val envelope = verified(response.data, allowOlder)
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
    private fun verified(data: String, allowOlder: Boolean): CloudEnvelope? {
        val envelope = runCatching { json.decodeFromString<CloudEnvelope>(data) }.getOrNull()
        if (envelope == null || CONFIG_ENTRY !in envelope.files) {
            Chat.chat("$PREFIX§4The cloud save has an unknown format. Update SBO or upload again.")
            return null
        }

        val uuid = Player.accountUuid()
        val key = CloudSyncKeys.key(uuid)
        if (key == null) {
            if (envelope.sig != null) {
                Chat.chat("$PREFIX§eThis cloud save is protected by a sync password. Set it first: /$PASSWORD_COMMAND <password>")
                return null
            }
            return envelope
        }

        if (envelope.sig == null) {
            Chat.chat("$PREFIX§4The cloud save is not signed, so it could have been changed on the server. Not loaded.")
            Chat.chat("$PREFIX§eUpload from a PC with your sync password to protect it again.")
            return null
        }
        if (!CloudSyncKeys.verify(key, signedBytes(uuid, envelope.counter, envelope.files), envelope.sig)) {
            Chat.chat("$PREFIX§4The signature does not match. Either this PC has a different sync password, or the save was changed on the server. Not loaded.")
            return null
        }
        if (envelope.counter < state().counter && !allowOlder) {
            Chat.chat("$PREFIX§eThe cloud save is older than the newest one this PC has seen. Someone may have restored an old copy.")
            Chat.clickableChat("$PREFIX§c[Load it anyway]", "It is signed by you, just older") { download(allowOlder = true) }
            return null
        }
        return envelope
    }

    private fun apply(envelope: CloudEnvelope, version: Int, auto: Boolean) {
        val config = configFile()
        val backup = backupFile(config)
        try {
            DataManager.importAll(withLocalFields(envelope.files - CONFIG_ENTRY - NOT_SYNCED))
        } catch (e: Exception) {
            SBOKotlin.logger.error("Failed to apply the cloud data", e)
            Chat.chat("$PREFIX§4Could not apply the cloud save, nothing was changed.")
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
            Chat.chat("$PREFIX§4Could not apply the cloud config, your old one was restored. Your data was loaded.")
        }
        Guis.resetCachedGuis()

        state().version = version
        state().counter = maxOf(state().counter, envelope.counter)
        state().hash = runCatching { hashOf(collectFiles()) }.getOrDefault("")
        sboData.save()
        if (auto) {
            Chat.chat("$PREFIX§aAuto sync: loaded the newer cloud save from your other PC.")
        } else {
            autoPaused = false
            Chat.chat("$PREFIX§aCloud save loaded. Backups: ${backup.name} and config/sbo/backup. Reopen /sbo to see the changes.")
        }
    }

    fun status() {
        val password = if (CloudSyncKeys.hasKey()) "§aset" else "§cnot set §7(/$PASSWORD_COMMAND)"
        Chat.chat("$PREFIX§eSync password on this PC: $password")
        SboApi.cloudStatus()
            .toJson<CloudStatusResponse>(ignoreUnknownKeys = true) { response ->
                if (!response.success) {
                    Chat.chat("$PREFIX§4${response.error}")
                    return@toJson
                }
                val slot = response.slots.find { it.slot == SLOT }
                if (slot == null) {
                    Chat.chat("$PREFIX§eNothing stored in the cloud yet.")
                    return@toJson
                }
                val updated = DATE_FORMAT.format(Instant.ofEpochMilli(slot.updatedAt))
                val size = "%.1f KB".format(slot.size / 1024.0)
                Chat.chat("$PREFIX§eCloud save: version §b${slot.version}§e, $size, updated §b$updated")
                Chat.chat("$PREFIX§eThis PC is on version §b${state().version}")
            }
            .error { Chat.chat("$PREFIX§4Could not check the cloud: ${it.message}") }
    }

    fun delete() {
        Chat.clickableChat("$PREFIX§c[Click to delete your cloud save]", "This cannot be undone") {
            SboApi.cloudDelete(SLOT)
                .toJsonObject { response ->
                    if (response.getBoolean("Success")) {
                        state().version = 0
                        state().hash = ""
                        sboData.save()
                        Chat.chat("$PREFIX§aCloud save deleted.")
                        if (CloudSyncSettings.autoSync) Chat.chat("$PREFIX§eAuto sync is on and will upload again. Turn it off to keep the cloud empty.")
                    } else {
                        Chat.chat("$PREFIX§4Delete failed: ${response.getString("Error")}")
                    }
                }
                .error { Chat.chat("$PREFIX§4Delete failed: ${it.message}") }
        }
    }
}
