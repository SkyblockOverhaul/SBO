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

    fun playCustomSound(vararg sounds: String, volume: Float) {
        var sound = sounds[0]
        val listOfExt = listOf(".ogg", ".mp3", ".wav", ".au", ".aif", ".aiff")

        // Check if the sound doesn't end with any extension in the list
        if (listOfExt.none { sound.endsWith(it, ignoreCase = true) }) sound += ".ogg"

        val file = File("config/sbo/sounds", sound)

        if (!file.exists()) return

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
        val volumePercent = volume.coerceIn(1f, 100f) / 100f // test if it is dependant to volume set in configs (require testing normally)
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
