package net.sbo.mod.general

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.sbo.mod.SBOKotlin
import net.sbo.mod.mixinbase.MixinHealth
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.http.Http.getBoolean
import net.sbo.mod.utils.http.SboApi
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged
import java.io.File
import java.nio.file.Files

/**
 * Collects mod conflicts: SBO hooks another mod broke (see [MixinHealth]) and the mods that change the same game
 * classes. Only those mods go in, never the whole mod list, that would tell far more about the player than needed.
 * Saved to mod-conflicts.json every session; when a hook is broken the player is told once and can send the
 * report to the SBO Discord with a click, `/sboconflicts` shows and sends it by hand.
 */
object ModConflicts {
    private const val COMMAND = "sboconflicts"
    private const val FILE_NAME = "mod-conflicts.json"
    // The chat isn't ready the moment the first world loads
    private const val NOTICE_DELAY_MS = 5_000L
    // The backend's limits (routes/modConflicts.ts), a longer report would be refused as a whole
    private const val MAX_TEXT = 64
    private const val MAX_SUSPECTS = 50

    @Serializable
    data class ConflictMod(val id: String, val version: String)

    @Serializable
    data class FailedHook(val feature: String, val target: String, val handler: String)

    @Serializable
    data class Suspects(val target: String, val mods: List<ConflictMod>)

    @Serializable
    data class Report(
        val player: String? = null,
        val sboVersion: String,
        val mcVersion: String,
        val failed: List<FailedHook>,
        val suspects: List<Suspects>,
    )

    private val json = Json { prettyPrint = true }
    private val compactJson = Json { encodeDefaults = false }

    private var report: Report? = null
    private var checked = false
    private var sent = false

    fun init() {
        Register.command(COMMAND) { show(byHand = true) }
    }

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        // Once per session, also when it fails, so a broken mod file doesn't log on every world change
        if (checked) return
        checked = true
        val built = runCatching(::build).onFailure { SBOKotlin.logger.warn("[ModConflicts] could not check for mod conflicts", it) }.getOrNull() ?: return
        report = built
        save(built)
        if (built.failed.isNotEmpty()) Helper.sleep(NOTICE_DELAY_MS) { show(byHand = false) }
    }

    private fun build(): Report {
        val mixinOwners = mixinPackages()
        fun modOf(mixinClass: String): ConflictMod {
            val owner = mixinOwners.entries.filter { mixinClass.startsWith(it.key + ".") }.maxByOrNull { it.key.length }?.value
            return owner?.let(::modInfo) ?: ConflictMod("unknown", mixinClass.substringBeforeLast('.').take(MAX_TEXT))
        }
        // Other mods' mixins leave @MixinMerged on every method they add to a class
        val suspects = MixinHealth.watchedTargets().mapNotNull { target ->
            val type = runCatching { Class.forName(target) }.getOrNull() ?: return@mapNotNull null
            val mods = type.declaredMethods
                .mapNotNull { it.getAnnotation(MixinMerged::class.java)?.mixin }
                .filterNot { it.startsWith("net.sbo.mod.") }
                .map(::modOf)
                .distinct()
                .take(MAX_SUSPECTS)
            Suspects(target, mods)
        }
        val failed = MixinHealth.failedHooks().map { FailedHook(it.feature(), it.targetClass(), it.handler()) }
        // With a broken hook only the mods in its classes, the others have nothing to do with it
        val brokenTargets = failed.map { it.target }.toSet()
        val relevant = if (failed.isEmpty()) suspects else suspects.filter { it.target in brokenTargets }
        // The player is set at send time, the first world loads before it exists
        return Report(
            sboVersion = versionOf("sbo"),
            mcVersion = versionOf("minecraft"),
            failed = failed,
            suspects = relevant,
        )
    }

    // Mixin package of every mod, read from the mixin configs its fabric.mod.json lists
    private fun mixinPackages(): Map<String, ModContainer> = buildMap {
        for (mod in FabricLoader.getInstance().allMods) {
            val meta = mod.findPath("fabric.mod.json").orElse(null)?.let(::readJson) ?: continue
            val configs = meta.getAsJsonArray("mixins") ?: continue
            for (entry in configs) {
                val config = if (entry.isJsonPrimitive) entry.asString else entry.asJsonObject.get("config")?.asString ?: continue
                val pkg = mod.findPath(config).orElse(null)?.let(::readJson)?.get("package")?.asString ?: continue
                put(pkg, mod)
            }
        }
    }

    private fun readJson(path: java.nio.file.Path): JsonObject? =
        runCatching { JsonParser.parseString(Files.readString(path)).asJsonObject }.getOrNull()

    private fun modInfo(mod: ModContainer) = ConflictMod(mod.metadata.id.take(MAX_TEXT), mod.metadata.version.friendlyString.take(MAX_TEXT))

    private fun versionOf(id: String) = FabricLoader.getInstance().getModContainer(id).map { it.metadata.version.friendlyString }.orElse("?")

    private fun file() = File(File(FabricLoader.getInstance().configDir.toFile(), DataManager.dataDir), FILE_NAME)

    private fun save(report: Report) {
        runCatching { file().apply { parentFile.mkdirs() }.writeText(json.encodeToString(report)) }
            .onFailure { SBOKotlin.logger.warn("[ModConflicts] could not save $FILE_NAME", it) }
    }

    private fun show(byHand: Boolean) {
        val report = report ?: return Chat.chat("§6[SBO] §eJoin a world first, the check runs then.")
        if (report.failed.isEmpty()) {
            if (!byHand) return
            Chat.chat("§6[SBO] §aNo SBO feature is broken by another mod.")
        } else {
            val features = report.failed.map { it.feature }.distinct().joinToString("§c, §e")
            val likely = report.suspects.flatMap { it.mods }.distinct()
                .joinToString("§c, §e") { "${it.id} ${it.version}" }
                .ifEmpty { "unknown" }
            Chat.chat("§6[SBO] §cAnother mod breaks: §e$features§c. Likely: §e$likely§c.")
        }
        if (sent) return Chat.chat("§6[SBO] §7Report already sent this session, saved in config/${DataManager.dataDir}/$FILE_NAME.")
        Chat.clickableChat(
            "§6[SBO] §7Saved in config/${DataManager.dataDir}/$FILE_NAME. §e§l[Send report]",
            "Sends this report to the SBO Discord so we can fix it, only SBO's versions and the mods changing the same game code go along",
        ) { send() }
    }

    private fun send() {
        val report = report ?: return
        if (sent) return
        sent = true
        SboApi.modConflicts(compactJson.encodeToString(report.copy(player = Player.getName())))
            .toJsonObject {
                if (it.getBoolean("Success")) Chat.chat("§6[SBO] §aReport sent, thanks!")
                else {
                    sent = false
                    Chat.chat("§6[SBO] §cThe report could not be sent, try again later with /$COMMAND.")
                }
            }
            .error {
                sent = false
                Chat.chat("§6[SBO] §cThe report could not be sent, try again later with /$COMMAND.")
            }
    }
}
