package net.sbo.mod.utils

import javazoom.jl.player.JavaSoundAudioDevice
import com.google.gson.JsonParser
import javazoom.jl.player.Player
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Util
import net.sbo.mod.SBOKotlin.MOD_ID
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.game.World
import java.io.File
import java.io.FileInputStream
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.Semaphore
import javax.sound.sampled.*
import kotlin.math.log10

object SoundHandler {
    val SUPPORTED_EXTENSIONS = setOf(".ogg", ".mp3", ".wav", ".au", ".aif", ".aiff")
    private const val SOUND_DIR_PATH = "config/sbo/sounds"
    @Volatile
    private var availableSoundsWithExt: List<String> = emptyList()

    // Up to 3 running and 17 queued tasks; for a total of 20.
    private const val AUDIO_THREAD_COUNT = 3
    private const val MAX_QUEUED_SOUNDS = 17

    // Up to 9 sounds actively playing at the same time. Above this, the rest block until one of current ones finish.
    private const val MAX_ACTIVE_PLAYBACKS = 9
    private val ACTIVE_PLAYBACKS = Semaphore(MAX_ACTIVE_PLAYBACKS)

    // Thread pool for audio processing - bounded and daemon threads
    private val AUDIO_EXECUTOR = ThreadPoolExecutor(
        AUDIO_THREAD_COUNT,
        AUDIO_THREAD_COUNT,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_QUEUED_SOUNDS),
        { r ->
            Thread(r, "sbo-audio-thread").apply {
                isDaemon = true
                priority = Thread.MAX_PRIORITY // audio processing needs significiantly more priority than other tasks for less latency
            }
        },
        ThreadPoolExecutor.AbortPolicy()
    )

    /**
     * Initializes sound system: extracts built-in sounds and scans for available sounds.
     */
    fun init() {
        File(SOUND_DIR_PATH).mkdirs()
        migrateMasterVolume()
        // Defer the expensive operations to avoid blocking game startup
        AUDIO_EXECUTOR.execute {
            runCatching { extractBuiltInSounds() }
                .onFailure { logger.error("[$MOD_ID] Failed to extract built-in sounds", it) }

            scanUserSounds()
        }
    }

    private var legacyMasterVolume: Float? = null

    // Must run before the config registers, ResourcefulConfig drops the old option from config.jsonc
    fun readLegacyMasterVolume() {
        val config = File(FabricLoader.getInstance().configDir.toFile(), "sbo/config.jsonc")
        legacyMasterVolume = runCatching {
            JsonParser.parseString(config.readText()).asJsonObject
                .getAsJsonObject("Customization")?.get("masterVolume")?.asFloat
        }.getOrNull()
    }

    private fun migrateMasterVolume() {
        val settings = DataManager.soundSettingsData
        if (settings.masterVolumeMigrated) return
        legacyMasterVolume?.let { settings.masterVolume = it.coerceIn(0f, 1f) }
        settings.masterVolumeMigrated = true
        settings.save()
    }

    private fun acquirePlaybackPermit(): Boolean {
        return try {
            ACTIVE_PLAYBACKS.acquire()
            true
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    private fun releasePlaybackPermit(released: AtomicBoolean) {
        if (released.compareAndSet(false, true)) {
            ACTIVE_PLAYBACKS.release()
        }
    }

    fun openSoundFolder() {
        val directory = File(SOUND_DIR_PATH).apply { mkdirs() }
        runCatching { Util.getPlatform().openPath(directory.toPath()) }.onFailure { logger.error("[$MOD_ID] Failed to open the sound folder", it) }
    }

    /**
     * Returns available sounds with their file extensions (e.g., "sound.mp3", "music.ogg")
     */
    fun getAvailableSoundsWithExt(): List<String> {
        scanUserSounds() // Update to avoid the user having to restart minecraft to see his added sound
        return availableSoundsWithExt
    }

    /**
     * Plays a custom sound.
     * @param sound Sound name (extension optional, .ogg assumed if missing)
     * @param volume Volume level (0-1), combined with master volume
     */
    fun playCustomSound(sound: String, volume: Float) {
        if (sound.isEmpty() || !World.isInSkyblock()) return

        // Combine per-sound volume (0-1) with global master volume
        val volumePercent = (volume * DataManager.soundSettingsData.masterVolume).coerceIn(0f, 1f)

        if (volumePercent == 0f) return

        // Assume .ogg if no extension provided
        val soundFile = if (SUPPORTED_EXTENSIONS.none { sound.endsWith(it, ignoreCase = true) }) "$sound.ogg" else sound

        val file = File(SOUND_DIR_PATH, soundFile)
        if (!file.exists()) {
            logger.warn("[$MOD_ID] Sound file not found: ${file.absolutePath}")
            return
        }

        submitAudioTask(file, volumePercent, soundFile.endsWith(".mp3", ignoreCase = true))
    }

    private fun submitAudioTask(file: File, volumePercent: Float, isMp3: Boolean) {
        try {
            // Dispatch to appropriate player using thread pool
            AUDIO_EXECUTOR.execute {
                if (isMp3) {
                    playMp3WithVolume(file, volumePercent)
                } else {
                    playStandardAudio(file, volumePercent)
                }
            }
        } catch (e: RejectedExecutionException) {
            logger.error("[$MOD_ID] Audio queue is full; dropping sound: ${file.name}", e)
        }
    }

    /** Extracts built-in sounds from the mod JAR to the config directory */
    private fun extractBuiltInSounds() {
        val modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).orElse(null) ?: run {
            logger.warn("[$MOD_ID] Could not locate mod container; built-in sounds will not be extracted.")
            return
        }

        for (root in modContainer.rootPaths) {
            val sounds = root.resolve("assets/$MOD_ID/sounds")
            if (!Files.exists(sounds)) continue

            Files.walk(sounds).use { stream ->
                stream.filter(Files::isRegularFile)
                    .filter { it.fileName.toString().endsWith(".ogg") }
                    .forEach { source ->
                        val sound = source.fileName.toString()
                        val target = File(SOUND_DIR_PATH, sound).toPath()
                        if (!Files.exists(target)) {
                            runCatching {
                                Files.copy(source, target)
                            }.onFailure {
                                logger.error("[$MOD_ID] Failed to extract built-in sound: $sound", it)
                            }
                        }
                    }
            }
        }
    }

    private fun scanUserSounds() {
        availableSoundsWithExt = File(SOUND_DIR_PATH).listFiles()
            ?.asSequence()
            ?.filter { it.isFile && SUPPORTED_EXTENSIONS.any { ext -> it.name.endsWith(ext, ignoreCase = true) } }
            ?.map { it.name }
            ?.sorted()
            ?.toList()
            .orEmpty()
    }

    /** Converts linear volume (0-1) to decibels for audio control */
    private fun volumeToDecibels(volume: Float): Float {
        if (volume <= 0.0001f) return -96f // Avoid log10(0)
        return (20 * log10(volume.toDouble())).toFloat()
    }

    /** Plays standard audio formats (.ogg, .wav, .au, .aif, .aiff) */
    private fun playStandardAudio(file: File, volumePercent: Float) {
        if (!acquirePlaybackPermit()) return

        val permitReleased = AtomicBoolean()

        val clip = try {
            AudioSystem.getClip()
        } catch (e: Exception) {
            releasePlaybackPermit(permitReleased)
            logger.error("[$MOD_ID] Failed to initialize audio playback: ${file.name}", e)
            return
        }

        val listener = object : LineListener {
            override fun update(event: LineEvent) {
                if (event.type == LineEvent.Type.STOP) {
                    clip.removeLineListener(this)

                    try {
                        clip.close()
                    } catch (e: Exception) {
                        logger.error("[$MOD_ID] Failed to close audio clip: ${file.name}", e)
                    } finally {
                        releasePlaybackPermit(permitReleased)
                    }
                }
            }
        }

        try {
            AudioSystem.getAudioInputStream(file).use { inputStream ->
                val stream = if (inputStream.format.encoding == AudioFormat.Encoding.PCM_SIGNED) {
                    inputStream
                } else {
                    val decodedFormat = AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        inputStream.format.sampleRate,
                        16,
                        inputStream.format.channels,
                        inputStream.format.channels * 2,
                        inputStream.format.sampleRate,
                        false
                    )
                    AudioSystem.getAudioInputStream(decodedFormat, inputStream)
                }

                // Clip.open() preloads the audio data, so the stream can be closed immediately
                if (stream === inputStream) {
                    clip.open(stream)
                } else {
                    stream.use { clip.open(it) }
                }
            }

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                gain.value = volumeToDecibels(volumePercent)
            }

            // Close the clip and release its permit once playback stops
            clip.addLineListener(listener)
            clip.start()
        } catch (e: Exception) {
            clip.removeLineListener(listener)
            runCatching { clip.close() }.onFailure(e::addSuppressed)
            releasePlaybackPermit(permitReleased)
            logger.error("[$MOD_ID] Failed to play audio file: ${file.name}", e)
        }
    }

    /** Plays MP3 with volume control via custom audio device */
    private fun playMp3WithVolume(file: File, volumePercent: Float) {
        if (!acquirePlaybackPermit()) return

        try {
            FileInputStream(file).use { fileStream ->
                val player = Player(fileStream, createVolumeAdjustedAudioDevice(volumePercent))

                AutoCloseable { player.close() }.use {
                    player.play()
                }
            }
        } catch (e: Exception) {
            logger.error("[$MOD_ID] Failed to play MP3 sound: ${file.name}", e)
        } finally {
            ACTIVE_PLAYBACKS.release()
        }
    }

    private val JAVA_SOUND_SOURCE_HANDLE: VarHandle = run {
        val lookup = MethodHandles.privateLookupIn(JavaSoundAudioDevice::class.java, MethodHandles.lookup())

        lookup.findVarHandle(JavaSoundAudioDevice::class.java, "source", SourceDataLine::class.java)
    }

    /** Creates a custom audio device that applies volume adjustment */
    private fun createVolumeAdjustedAudioDevice(volumePercent: Float): JavaSoundAudioDevice {
        val loggedFailure = AtomicBoolean()

        return object : JavaSoundAudioDevice() {
            override fun writeImpl(samples: ShortArray, offs: Int, len: Int) {
                super.writeImpl(samples, offs, len)

                runCatching {
                    val sourceLine = JAVA_SOUND_SOURCE_HANDLE.get(this) as? SourceDataLine

                    if (sourceLine != null && sourceLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        val gain = sourceLine.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                        val targetGain = volumeToDecibels(volumePercent)

                        if (gain.value != targetGain) {
                            gain.value = targetGain
                        }
                    }
                }.onFailure {
                    if (loggedFailure.compareAndSet(false, true)) {
                        logger.error("[$MOD_ID] Failed to adjust audio volume", it)
                    }
                }
            }
        }
    }
}
