package net.sbo.mod.utils

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.SharedConstants
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.server.packs.PackType
import net.minecraft.sounds.SoundEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.ResourceLocationException
import net.sbo.mod.SBOKotlin.MOD_ID
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.settings.categories.Customization
import java.io.File

object SoundHandler {
    private val soundDir: File
    private val generatedPackDir: File

    init {
        val modConfigDir = File(FabricLoader.getInstance().configDir.toFile(), "SBO").apply { mkdirs() }
        soundDir = File(modConfigDir, "sounds").apply { mkdirs() }

        val packsDir = File(FabricLoader.getInstance().gameDir.toFile(), "resourcepacks").apply { mkdirs() }
        generatedPackDir = File(packsDir, "SBO Custom Sounds Data Pack").apply { mkdirs() }
    }

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
        } catch (_: Throwable) {
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
        println("[$MOD_ID] Generated dynamic soundpack at: ${generatedPackDir.absolutePath}")
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
                println("[$MOD_ID] Failed to write pack icon: ${it.message}")
            }
        } else {
            println("[$MOD_ID] Pack icon not found at $resPath")
        }
    }

    fun playCustomSound(sound: String, volume: Float, pitch: Float = 1f) {
        val packManager = mc.resourcePackRepository
        val packId = "file/SBO Custom Sounds Data Pack"

        val id = try {
            ResourceLocation.fromNamespaceAndPath(MOD_ID, sound.lowercase())
        } catch (invalidIdentifierError: ResourceLocationException) {
            Customization.resetSoundCustomizationToDefaults()

            Chat.chat("§6[SBO] §cYou had an error with your custom sound configuration, your custom sound settings will automatically reset to protect from crashes. More information might be available in your logs.")
            logger.error("Error with the user supplied custom sound ID", invalidIdentifierError) // print full error with stacktrace to logs

            return
        }

        val event = SoundEvent.createVariableRangeEvent(id)

        if (!packManager.selectedIds.contains(packId) && sound.isNotEmpty()) Chat.chat("§6[SBO] §cCustom sound pack is not enabled. Please enable it in the resource packs menu.")
        mc.soundManager.play(SimpleSoundInstance.forUI(event, pitch, volume))
    }
}
