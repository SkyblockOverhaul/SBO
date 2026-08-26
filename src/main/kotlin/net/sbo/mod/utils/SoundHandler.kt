package net.sbo.mod.utils

import javazoom.jl.player.JavaSoundAudioDevice
import javazoom.jl.player.Player
import net.fabricmc.loader.api.FabricLoader
import net.sbo.mod.SBOKotlin.MOD_ID
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.utils.chat.Chat
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.sound.sampled.*
import kotlin.math.log10

object SoundHandler {
    private val SUPPORTED_EXTENSIONS = setOf(".ogg", ".mp3", ".wav", ".au", ".aif", ".aiff")
    private const val SOUND_DIR_PATH = "config/sbo/sounds"
    private val availableSounds = mutableSetOf<String>()
    private val availableSoundsWithExt = mutableSetOf<String>()

    // Thread pool for audio processing - bounded and daemon threads
    private val AUDIO_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "sbo-audio-thread").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY + 1 // audio processing needs slightly more priority for less latency
        }
    }

    /**
     * Initializes sound system: extracts built-in sounds and scans for available sounds.
     */
    fun init() {
        File(SOUND_DIR_PATH).apply { mkdirs() }
        // Defer the expensive operations to avoid blocking game startup
        AUDIO_EXECUTOR.execute {
            extractBuiltInSounds()
            scanUserSounds()
        }
    }


    /**
     * Returns available sounds with their file extensions (e.g., "sound.mp3", "music.ogg")
     */
    fun getAvailableSoundsWithExt(): List<String> {
        scanUserSounds() // Update to avoid the user having to restart minecraft to see his added sound
        return availableSoundsWithExt.sorted().toList()
    }

    /**
     * Plays a custom sound.
     * @param sound Sound name (extension optional, .ogg assumed if missing)
     * @param volume Volume level (0-1), combined with master volume
     */
    fun playCustomSound(sound: String, volume: Float) {
        if (sound.isEmpty()) return

        // Combine per-sound volume (0-1) with global master volume
        val volumePercent = volume.coerceIn(0f, 1f) * Customization.masterVolume

        // Assume .ogg if no extension provided
        val soundFile = if (SUPPORTED_EXTENSIONS.none { sound.endsWith(it, ignoreCase = true) }) "$sound.ogg" else sound

        val file = File(SOUND_DIR_PATH, soundFile)
        if (!file.exists()) {
            logger.warn("[$MOD_ID] Sound file not found: ${file.absolutePath}")
            return
        }

        // Dispatch to appropriate player using thread pool
        if (soundFile.endsWith(".mp3", ignoreCase = true)) {
            AUDIO_EXECUTOR.execute {
                playMp3WithVolume(file, volumePercent)
            }
        } else {
            AUDIO_EXECUTOR.execute {
                playStandardAudio(file, volumePercent)
            }
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
                        availableSounds.add(sound.substringBeforeLast('.').lowercase())
                        availableSoundsWithExt.add(sound)
                    }
            }
        }
    }

    /** Scans the config directory for user-added sounds */
    fun scanUserSounds() {
        File(SOUND_DIR_PATH).listFiles()
            ?.filter { it.isFile }
            ?.filter { file -> SUPPORTED_EXTENSIONS.any { ext -> file.name.endsWith(ext, ignoreCase = true) } }
            ?.forEach { file ->
                availableSounds.add(file.name.substringBeforeLast('.').lowercase())
                availableSoundsWithExt.add(file.name)
            }
    }

    /** Converts linear volume (0-1) to decibels for audio control */
    private fun volumeToDecibels(volume: Float): Float {
        if (volume <= 0.0001f) return -96f // Avoid log10(0)
        return (20 * log10(volume.toDouble())).toFloat()
    }

    /** Plays standard audio formats (.ogg, .wav, .au, .aif, .aiff) */
    private fun playStandardAudio(file: File, volumePercent: Float) {
        val clip = AudioSystem.getClip()
        val inputStream = try {
            AudioSystem.getAudioInputStream(file)
        } catch (e: Exception) {
            logger.error("[$MOD_ID] Failed to read audio file: ${file.name}", e)
            Chat.chat("§c[SBO] Something went wrong while reading the file ${file.name} try using an another format and if the issue persist, please contact us")
            return
        }

        val stream = try {
            // Skip expensive format conversion if it's already PCM
            if (inputStream.format.encoding == AudioFormat.Encoding.PCM_SIGNED) {
                inputStream
            } else {
                // Only convert if necessary
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
        } catch (e: Exception) {
            logger.error("[$MOD_ID] Failed to process audio format: ${file.name}", e)
            runCatching { inputStream.close() }
            return
        }

        try {
            clip.open(stream)
        } catch (e: Exception) {
            logger.error("[$MOD_ID] Failed to open audio stream: ${file.name}", e)
            runCatching { clip.close() }
            runCatching { stream.close() }
            runCatching { inputStream.close() }
            return
        }

        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            gain.value = volumeToDecibels(volumePercent)
        }

        // Use separate function for cleanup to ensure all resources are closed
        cleanupOnAudioStop(clip, stream, inputStream)
        clip.start()
    }

    /**
     * Clean up audio resources when playback stops
     */
    private fun cleanupOnAudioStop(clip: Clip, stream: AudioInputStream, inputStream: InputStream) {
        clip.addLineListener { event ->
            if (event.type == LineEvent.Type.STOP) {
                try {
                    clip.close()
                    stream.close()
                    inputStream.close()
                } catch (e: Exception) {
                    logger.error("[$MOD_ID] Error closing audio resources", e)
                }
            }
        }
    }

    /** Plays MP3 with volume control via custom audio device */
    private fun playMp3WithVolume(file: File, volumePercent: Float) {
        FileInputStream(file).use { fileStream ->
            runCatching {
                val player = Player(fileStream, createVolumeAdjustedAudioDevice(volumePercent))
                player.play()
                player.close()
            }.onFailure {
                logger.error("[$MOD_ID] Failed to play MP3 sound: ${file.name}", it)
            }
        }
    }

    /** Creates a custom audio device that applies volume adjustment */
    private fun createVolumeAdjustedAudioDevice(volumePercent: Float): JavaSoundAudioDevice {
        return object : JavaSoundAudioDevice() {
            override fun writeImpl(samples: ShortArray, offs: Int, len: Int) {
                super.writeImpl(samples, offs, len)

                runCatching {
                    val sourceField = JavaSoundAudioDevice::class.java
                        .getDeclaredField("source").apply { isAccessible = true }
                    val sourceLine = sourceField.get(this) as? SourceDataLine

                    if (sourceLine != null && sourceLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        val gain = sourceLine.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                        val targetGain = volumeToDecibels(volumePercent)
                        if (gain.value != targetGain) {
                            gain.value = targetGain
                        }
                    }
                }
            }
        }
    }
}
