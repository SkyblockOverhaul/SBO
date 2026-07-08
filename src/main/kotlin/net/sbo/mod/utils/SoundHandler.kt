package net.sbo.mod.utils

import net.fabricmc.loader.api.FabricLoader
import net.sbo.mod.SBOKotlin.MOD_ID
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.settings.categories.Customization
import javazoom.jl.player.Player
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineEvent
import kotlin.math.log10

object SoundHandler {
    private val SUPPORTED_EXTENSIONS = setOf(".ogg", ".mp3", ".wav", ".au", ".aif", ".aiff")
    private val SOUND_DIR_PATH = "config/sbo/sounds"
    private val availableSounds = mutableSetOf<String>()
    private val availableSoundsWithExt = mutableSetOf<String>()

    /**
     * Initializes sound system: extracts built-in sounds and scans for available sounds.
     */
    fun init() {
        File(SOUND_DIR_PATH).apply { mkdirs() }
        extractBuiltInSounds()
        scanUserSounds()
    }

    fun getAvailableSoundsList(): List<String> = availableSounds.sorted().toList()

    /**
     * Returns available sounds with their file extensions (e.g., "sound.mp3", "music.ogg")
     */
    fun getAvailableSoundsWithExt(): List<String> = availableSoundsWithExt.sorted().toList()

    fun hasSound(soundName: String): Boolean = soundName.isNotEmpty() && availableSounds.contains(soundName.lowercase())

    /**
     * Plays a custom sound.
     * @param sounds Sound name (extension optional, .ogg assumed if missing)
     * @param volume Volume level (0-1), combined with master volume
     */
    fun playCustomSound(sound: String, volume: Float) {
        // Combine per-sound volume (0-1) with global master volume
        val volumePercent = volume.coerceIn(0f, 1f) * Customization.masterVolume

        // Assume .ogg if no extension provided
        var soundFile = if (SUPPORTED_EXTENSIONS.none { sound.endsWith(it, ignoreCase = true) }) sound + ".ogg" else sound

        val file = File(SOUND_DIR_PATH, soundFile)
        if (!file.exists()) {
            logger.warn("[$MOD_ID] Sound file not found: ${file.absolutePath}")
            return
        }

        // Dispatch to appropriate player
        if (soundFile.endsWith(".mp3", ignoreCase = true)) {
            playMp3WithVolume(file, volumePercent)
        } else {
            playStandardAudio(file, volumePercent)
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
    private fun scanUserSounds() {
        File(SOUND_DIR_PATH).listFiles()
            ?.filter { it.isFile }
            ?.filter { file -> SUPPORTED_EXTENSIONS.any { ext -> file.name.endsWith(ext, ignoreCase = true) } }
            ?.forEach { file ->
                availableSounds.add(file.name.substringBeforeLast('.').lowercase())
                availableSoundsWithExt.add(file.name)
            }
    }

    /** Converts linear volume (0-1) to decibels for audio control */
    private fun volumeToDecibels(volume: Float): Float = (20 * log10(volume.toDouble())).toFloat()

    /** Plays standard audio formats (.ogg, .wav, .au, .aif, .aiff) */
    private fun playStandardAudio(file: File, volumePercent: Float) {
        val clip = AudioSystem.getClip()
        val inputStream = try {
            AudioSystem.getAudioInputStream(file)
        } catch (e: Exception) {
            logger.error("[$MOD_ID] Failed to read audio file: ${file.name}", e)
            return
        }

        val decodedFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            inputStream.format.sampleRate,
            16,
            inputStream.format.channels,
            inputStream.format.channels * 2,
            inputStream.format.sampleRate,
            false
        )

        val stream = try {
            AudioSystem.getAudioInputStream(decodedFormat, inputStream)
        } catch (e: Exception) {
            logger.error("[$MOD_ID] Failed to convert audio format: ${file.name}", e)
            runCatching { inputStream.close() }
            return
        }

        try {
            clip.open(stream)
        } catch (e: Exception) {
            logger.error("[$MOD_ID] Failed to open audio stream: ${file.name}", e)
            runCatching { stream.close() }
            runCatching { inputStream.close() }
            return
        }

        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            gain.value = volumeToDecibels(volumePercent)
        }

        clip.addLineListener { event ->
            if (event.type == LineEvent.Type.STOP) {
                clip.close()
                stream.close()
                inputStream.close()
            }
        }

        clip.start()
    }

    /** Plays MP3 with volume control via custom audio device */
    private fun playMp3WithVolume(file: File, volumePercent: Float) {
        Thread {
            FileInputStream(file).use { fileStream ->
                runCatching {
                    val player = Player(fileStream, createVolumeAdjustedAudioDevice(volumePercent))
                    player.play()
                    player.close()
                }.onFailure {
                    logger.error("[$MOD_ID] Failed to play MP3 sound: ${file.name}", it)
                }
            }
        }.start()
    }

    /** Creates a custom audio device that applies volume adjustment */
    private fun createVolumeAdjustedAudioDevice(volumePercent: Float): javazoom.jl.player.JavaSoundAudioDevice {
        return object : javazoom.jl.player.JavaSoundAudioDevice() {
            override fun writeImpl(samples: ShortArray, offs: Int, len: Int) {
                super.writeImpl(samples, offs, len)

                runCatching {
                    val sourceField = javazoom.jl.player.JavaSoundAudioDevice::class.java
                        .getDeclaredField("source").apply { isAccessible = true }
                    val sourceLine = sourceField.get(this) as? javax.sound.sampled.SourceDataLine

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