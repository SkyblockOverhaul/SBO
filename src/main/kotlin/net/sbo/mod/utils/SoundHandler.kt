package net.sbo.mod.utils

import net.sbo.mod.utils.data.SboDataObject

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.SharedConstants
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.server.packs.PackType
import net.minecraft.sounds.SoundEvent
import net.minecraft.IdentifierException
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.MOD_ID
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.utils.chat.Chat
import java.io.File

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
        runCatching {
            val urls = SoundHandler::class.java.classLoader.getResources("assets/$MOD_ID/sounds")
            while (urls.hasMoreElements()) {
                val resource = urls.nextElement()
                val dir = File(resource.path)
                val files = dir.listFiles { f -> f.isFile && f.extension.equals("ogg", ignoreCase = true) } ?: emptyArray()
                files.forEach { file ->
                    val target = File(soundDir, "${safeName(file.nameWithoutExtension)}.ogg")
                    if (!target.exists()) runCatching { file.copyTo(target, overwrite = false) }
                }
            }
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
        if (sound.isEmpty()) return

        val packManager = mc.resourcePackRepository
        val packId = "file/SBO Custom Sounds Data Pack"

        val safeSound = safeName(sound)

        if (!availableSounds.contains(safeSound)) {
            logger.warn("[$MOD_ID] Sound '$sound' not found. Available: ${availableSounds.joinToString()}")
            return
        }

        val id = try {
            SBOKotlin.id(safeSound)
        } catch (invalidIdentifierError: IdentifierException) {
            Chat.chat("§6[SBO] §cInvalid sound name. Use letters, numbers, _ or -")
            logger.error("Invalid sound ID: $sound", invalidIdentifierError)
            return
        }

        val event = SoundEvent.createVariableRangeEvent(id)

        if (!packManager.selectedIds.contains(packId)) {
            Chat.chat("§6[SBO] §cSounds not playing? §aGo to Options > Resource Packs > move '§lSBO Custom Sounds Data Pack§a' to the right (Active)")
        }
        mc.soundManager.play(SimpleSoundInstance.forUI(event, pitch, volume))
    }
}
