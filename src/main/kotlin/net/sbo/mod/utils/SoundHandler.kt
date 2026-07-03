package net.sbo.mod.utils

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.SharedConstants
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.server.packs.PackType
import net.minecraft.sounds.SoundEvent
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.MOD_ID
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.SboDataObject
import java.io.File
import java.nio.file.Files

object SoundHandler {
    private val soundDir: File
    private val generatedPackDir: File

    private val availableSounds = mutableSetOf<String>()

    init {
        val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), SboDataObject.dataDir).apply { mkdirs() }
        soundDir = File(modConfigDir, "sounds").apply { mkdirs() }

        val packsDir = File(FabricLoader.getInstance().gameDir.toFile(), "resourcepacks").apply { mkdirs() }
        generatedPackDir = File(packsDir, "SBO Custom Sounds Data Pack").apply { mkdirs() }
    }

    fun getAvailableSoundsList(): List<String> = availableSounds.sorted().toList()

    fun hasSound(soundName: String): Boolean = soundName.isNotEmpty() && availableSounds.contains(soundName.lowercase())

    private fun safeName(base: String): String {
        val b = base.lowercase()
        val sb = StringBuilder(b.length)
        for (ch in b) {
            when (ch) {
                in 'a'..'z', in '0'..'9', '_', '-', '.', '/' -> sb.append(ch)
                ' ' -> sb.append('_')
                else -> sb.append('_')
            }
        }
        return sb.toString()
    }

    private fun currentResourcePackFormat(): Int {
        return try {
            SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES).major()
        } catch (tw: Throwable) {
            logger.error("Error while determining current resource pack format, falling back to 48", tw)
            48
        }
    }

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

        val assetsSoundsDir = File(generatedPackDir, "assets/$MOD_ID/sounds").apply { mkdirs() }
        val oggFiles = soundDir.listFiles { f -> f.isFile && f.extension.equals("ogg", ignoreCase = true) }?.toList() ?: emptyList()
        val names = mutableListOf<String>()
        oggFiles.forEach { src ->
            val name = safeName(src.nameWithoutExtension)
            names += name
            availableSounds.add(name)
            src.copyTo(File(assetsSoundsDir, "$name.ogg"), overwrite = true)
        }

        val soundsJsonFile = File(generatedPackDir, "assets/$MOD_ID/sounds.json")
        val json = buildString {
            append("{\n")
            names.forEachIndexed { i, name ->
                append("  \"$name\": { \"sounds\": [ { \"name\": \"$MOD_ID:$name\", \"stream\": true } ] }")
                if (i < names.size - 1) append(",\n") else append("\n")
            }
            append("}\n")
        }
        soundsJsonFile.writeText(json)

        val packFormat = currentResourcePackFormat()
        val packMcmeta = File(generatedPackDir, "pack.mcmeta")
        packMcmeta.writeText(
            """
            {
              "pack": {
                "description": "SBO Custom Sounds",
                "pack_format": $packFormat,
                "min_format": 65,
                "max_format": 999
              }
            }
            """.trimIndent()
        )
        writePackIcon()

        logger.info("[$MOD_ID] Custom sounds ready. Found ${availableSounds.size} sounds. To enable: Options > Resource Packs > move 'SBO Custom Sounds Data Pack' to the right")
    }

    private fun writePackIcon() {
        val target = File(generatedPackDir, "pack.png")
        val resPath = "assets/$MOD_ID/icon.png"
        val stream = SoundHandler::class.java.classLoader.getResourceAsStream(resPath)
        if (stream != null) {
            runCatching {
                stream.use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }.onFailure {
                logger.warn("[$MOD_ID] Failed to write pack icon: ${it.message}")
            }
        } else {
            logger.info("[$MOD_ID] Pack icon not found at $resPath")
        }
    }

    fun playCustomSound(sound: String, volume: Float, pitch: Float = 1f) {
        // Playing sounds off the Render thread causes a ConcurrentModificationException at SoundEngine#tickInGameSound due to a HashMap iteration, so we need this
        mc.execute {
            playCustomSoundNonThreadSafe(sound, volume, pitch)
        }
    }

    private fun playCustomSoundNonThreadSafe(sound: String, volume: Float, pitch: Float) {
        if (sound.isEmpty()) return

        val packManager = mc.resourcePackRepository
        val packId = "file/SBO Custom Sounds Data Pack"

        val safeSound = safeName(sound)
        val packMissing = "§6[SBO] §cCustom sounds are inactive. §aGo to Options > Resource Packs > move '§lSBO Custom Sounds Data Pack§a' to the right (Active)"

        if (!availableSounds.contains(safeSound)) {
            val message = "Sound '$sound' not found. Available: ${availableSounds.joinToString()}"

            if (availableSounds.isEmpty()) {
                Chat.chat(packMissing)
            } else {
                Chat.chat("§6[SBO] §c$message")
            }

            logger.warn("[$MOD_ID] $message")
            return
        }

        val id = SBOKotlin.userSuppliedId(safeSound) { invalidIdentifierException ->
            Chat.chat("§6[SBO] §cInvalid sound name \"$sound\". Use letters, numbers, _ or -")
            logger.error("Invalid sound ID: $sound", invalidIdentifierException)
        }

        if (id == null) {
            Chat.chat("§6[SBO] §cUnknown error when creating Identifier for sound ID: $sound")
            logger.error("Unknown error when creating Identifier for sound ID: $sound")
            return
        }

        val event = SoundEvent.createVariableRangeEvent(id)

        if (!packManager.selectedIds.contains(packId)) {
            Chat.chat(packMissing)
        }

        mc.soundManager.play(SimpleSoundInstance.forUI(event, pitch, volume))
    }
}
