package net.sbo.mod.utils

import net.fabricmc.loader.api.FabricLoader
import net.sbo.mod.SBOKotlin.MOD_ID
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.utils.data.SboDataObject
import java.io.File
import java.nio.file.Files
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
        soundDir = File(modConfigDir, "sounds").apply { mkdirs() }

        configDir = File("config/sbo", "sounds").apply { mkdirs() }
    }

    fun getAvailableSoundsList(): List<String> = availableSounds.sorted().toList()

    fun hasSound(soundName: String): Boolean = soundName.isNotEmpty() && availableSounds.contains(soundName.lowercase())

    fun init() {
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
        for (sound in sounds) {
            val file = File("config/sbo/sounds", "$sound.ogg")

            if (!file.exists()) continue

            val clip = AudioSystem.getClip()
            val stream = AudioSystem.getAudioInputStream(file)

            clip.open(stream)

            val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val volumePercent = volume.coerceIn(1f, 100f) / 100f
            gain.value = (20 * log10(volumePercent.toDouble())).toFloat()

            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    clip.close()
                    stream.close()
                }
            }

            clip.start()
        }
    }
}
