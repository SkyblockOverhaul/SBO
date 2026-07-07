package net.sbo.mod.utils

import net.fabricmc.loader.api.FabricLoader
import net.sbo.mod.SBOKotlin.MOD_ID
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.events.Register
import java.io.File
import java.nio.file.Files
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineEvent
import java.io.FileInputStream
import javazoom.jl.player.Player
import net.sbo.mod.settings.categories.Customization
import kotlin.math.log10

object SoundHandler {
    private val soundDir: File
    private val configDir: File

    private val availableSounds = mutableSetOf<String>()

    init {
        val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), SboDataObject.dataDir).apply { mkdirs() }
        soundDir = File(modConfigDir, "sounds").apply { mkdirs() } // resources/assets/sbo/sounds/

        configDir = File("config/sbo", "sounds").apply { mkdirs() } // /config/sbo/sounds/
    }

    fun getAvailableSoundsList(): List<String> = availableSounds.sorted().toList()

    fun hasSound(soundName: String): Boolean = soundName.isNotEmpty() && availableSounds.contains(soundName.lowercase())

    /**
     * Copies the default sounds to /config/sbo/sounds
     */
    fun init() {
        Register.command("playsoundtest") { args ->
            val volume = if (args.size > 1) args[1].toFloat() else 100f
            playCustomSound(args[0], volume = volume)
        }

        val modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).orElse(null)

        if (modContainer != null) {
            for (root in modContainer.rootPaths) {
                val sounds = root.resolve("assets/$MOD_ID/sounds")

                if (!Files.exists(sounds)) {
                    continue
                }

                Files.walk(sounds).use { stream ->
                    stream
                        .filter(Files::isRegularFile)
                        .filter { it.fileName.toString().endsWith(".ogg") }
                        .forEach { source ->
                            val sound = source.fileName.toString()
                            val target = soundDir.toPath().resolve(sound)
                            if (!Files.exists(target)) {
                                runCatching {
                                    Files.copy(source, target)
                                }.onFailure {
                                    logger.error("[$MOD_ID] Failed to extract the built-in sound $sound", it)
                                }
                            }
                        }
                }
            }
        } else {
            logger.warn("[$MOD_ID] Could not locate the mod container; built-in sounds will not be extracted.")
        }
    }

    /**
     * Plays a custom sound.
     * volume must be between 0 & 1
     */
    fun playCustomSound(vararg sounds: String, volume: Float) {
        var sound = sounds[0]
        val listOfExt = listOf(".ogg", ".mp3", ".wav", ".au", ".aif", ".aiff")
        val volumePercent = volume.coerceIn(1f, 100f) / 100f * Customization.masterVolume

        // Assume the file is a .ogg if not specified
        if (listOfExt.none { sound.endsWith(it, ignoreCase = true) }) sound += ".ogg"

        logger.info("Playing sound: $sound")

        val file = File("config/sbo/sounds", sound)
        if (!file.exists()) return

        // Annoying mp3 support
        if (sound.endsWith(".mp3", ignoreCase = true)) {
            Thread {
                runCatching {
                    val fileStream = FileInputStream(file)

                    // Override the device implementation directly using the verified decompiled signatures
                    val device = object : javazoom.jl.player.JavaSoundAudioDevice() {
                        override fun writeImpl(samples: ShortArray, offs: Int, len: Int) {
                            // Let the default logic initialize the 'source' field
                            super.writeImpl(samples, offs, len)

                            // Now adjust the volume once on the newly initialized stream
                            runCatching {
                                val sourceField = javazoom.jl.player.JavaSoundAudioDevice::class.java
                                    .getDeclaredField("source").apply { isAccessible = true }
                                val sourceLine = sourceField.get(this) as? javax.sound.sampled.SourceDataLine

                                if (sourceLine != null && sourceLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                                    val gain = sourceLine.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl

                                    // Calculate the volume target
                                    val targetGain = (20 * log10(volumePercent.toDouble())).toFloat()

                                    // Only update if it actually changed to reduce overhead across sequential frames
                                    if (gain.value != targetGain) {
                                        gain.value = targetGain
                                    }
                                }
                            }
                        }
                    }

                    val player = Player(fileStream, device)
                    player.play()
                    player.close()
                    fileStream.close()
                }.onFailure { e ->
                    logger.error("[$MOD_ID] Failed to play MP3 sound with volume: $sound", e)
                }
            }.start()
        } else {
            // Original stuff for .ogg, .wav, .au, .aif, .aiff
            val clip = AudioSystem.getClip()
            val inputStream = AudioSystem.getAudioInputStream(file)

            val decodedFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                inputStream.format.sampleRate,
                16,
                inputStream.format.channels,
                inputStream.format.channels * 2,
                inputStream.format.sampleRate,
                false
            )

            val stream = AudioSystem.getAudioInputStream(decodedFormat, inputStream)

            clip.open(stream)

            val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            gain.value = (20 * log10(volumePercent.toDouble())).toFloat()

            // Closes the clip & streams once the sound is done playing
            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    clip.close()
                    stream.close()
                    inputStream.close()
                }
            }

            clip.start()
        }
    }
}
