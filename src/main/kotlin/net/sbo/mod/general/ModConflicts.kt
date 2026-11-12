package net.sbo.mod.general

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.sbo.mod.SBOKotlin
import net.sbo.mod.mixinbase.MixinHealth
import net.sbo.mod.settings.Settings
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
 * The info report: mod conflicts, i.e. SBO hooks another mod broke (see [MixinHealth]) and the mods that change the same
 * game classes, plus everything the player can set in SBO's settings and windows, so support can find wrong settings too.
 * Only the conflicting mods go in, never the whole mod list, that would tell far more about the player than needed;
 * other players' names and keys are left out of the settings.
 * Saved to info-report.json; when a hook is broken the player is told once and can send the report to the SBO Discord
 * with a click, `/sboinforeport` shows and sends it by hand.
 */
object ModConflicts {
    private const val COMMAND = "sboinforeport"
    private const val FILE_NAME = "info-report.json"
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
        /** Taken when the report is shown or sent, not when the first world loads. */
        val settings: kotlinx.serialization.json.JsonObject? = null,
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
                .asSequence()
                .mapNotNull { it.getAnnotation(MixinMerged::class.java)?.mixin }
                .filterNot { it.startsWith("net.sbo.mod.") }
                .map(::modOf)
                .distinct()
                .take(MAX_SUSPECTS)
                .toList()
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
    internal fun mixinPackages(): Map<String, ModContainer> = buildMap {
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

    /** What the player set in the settings, the GUIs and their own badge; other players' names and keys are left out. */
    private fun settings(): JsonObject {
        val gson = Gson()
        val account = Player.accountUuid()
        val sboData = gson.toJsonTree(DataManager.sboData).asJsonObject.apply {
            remove("sboKey")
            remove("currentCoaUuid")
            add("partyBlacklist", JsonPrimitive(DataManager.sboData.partyBlacklist.size))
            // Per account; only this account's, without the sync hashes
            add("cloudSync", JsonObject().also { sync ->
                DataManager.sboData.cloudSync[account]?.let { state ->
                    sync.add("version", JsonPrimitive(state.version))
                    state.autoSync?.let { sync.add("autoSync", JsonPrimitive(it)) }
                    state.premium?.let { sync.add("premium", JsonPrimitive(it)) }
                }
            })
            remove("badgeAccess")
            DataManager.sboData.badgeAccess[account]?.let { add("badgeAccess", JsonPrimitive(it)) }
        }
        val partyFinder = gson.toJsonTree(DataManager.partyFinderConfigState).asJsonObject.apply {
            add("blockedPlayers", JsonPrimitive(DataManager.partyFinderConfigState.blockedPlayers.size))
            add("viewerFavorites", JsonPrimitive(DataManager.partyFinderConfigState.viewerFavorites.size))
        }
        return JsonObject().apply {
            add("settings", Settings.values())
            add("sboData", sboData)
            add("overlays", gson.toJsonTree(DataManager.overlayData))
            add("sounds", gson.toJsonTree(DataManager.soundSettingsData))
            add("partyFinder", partyFinder)
            add("alphaData", JsonPrimitive(DataManager.alphaActive))
            SboBadges.own()?.let { add("ownBadge", gson.toJsonTree(it)) }
        }
    }

    /** The report with the settings as they are right now; without them when reading them fails. */
    private fun withSettings(report: Report): Report {
        val settings = runCatching { Json.parseToJsonElement(settings().toString()).jsonObject }
            .onFailure { SBOKotlin.logger.warn("[ModConflicts] could not read the settings for the report", it) }
            .getOrNull()
        return report.copy(settings = settings)
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
        val report = report?.let(::withSettings) ?: return Chat.chat("§6[SBO] §eJoin a world first, the check runs then.")
        save(report)
        if (report.failed.isEmpty()) {
            if (!byHand) return
            Chat.chat("§6[SBO] §aNo SBO feature is broken by another mod.")
        } else {
            val features = report.failed.asSequence().map { it.feature }.distinct().joinToString("§c, §e")
            val likely = report.suspects.asSequence().flatMap { it.mods }.distinct()
                .joinToString("§c, §e") { "${it.id} ${it.version}" }
                .ifEmpty { "unknown" }
            Chat.chat("§6[SBO] §cAnother mod breaks: §e$features§c. Likely: §e$likely§c.")
        }
        if (sent) return Chat.chat("§6[SBO] §7Report already sent this session, saved in config/${DataManager.dataDir}/$FILE_NAME.")
        Chat.clickableChat(
            "§6[SBO] §7Saved in config/${DataManager.dataDir}/$FILE_NAME. §e§l[Send report]",
            "Sends this report to the SBO Discord so we can fix it: SBO's versions, your SBO settings and the mods changing the same game code",
        ) { send() }
    }

    private fun send() {
        val report = report?.let(::withSettings) ?: return
        if (sent) return
        sent = true
        SboApi.infoReport(compactJson.encodeToString(report.copy(player = Player.getName())))
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
