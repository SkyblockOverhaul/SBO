package net.sbo.mod.utils.version

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.http.Http

object UpdateChecker {
    private const val RECHECK_INTERVAL_HOURS = 8
    private const val RECHECK_INTERVAL_TICKS = 20 * 60 * 60 * RECHECK_INTERVAL_HOURS
    private const val MODRINTH_ID = "9lBqVbQF"
    private val VERSION_REGEX = Regex("""(\d+\.\d+\.\d+|\d+\.\d+)""")

    private var latestVersion: String? = null
    private var latestId: String? = null
    var isUpdateAvailable = false

    private val PRE_RELEASE_REGEX = Regex("""(alpha|beta|rc|pre|snapshot)""", RegexOption.IGNORE_CASE)

    private fun String.extractCoreVersion(): String = VERSION_REGEX.find(this)?.value ?: this

    private fun String.isPreRelease(): Boolean = PRE_RELEASE_REGEX.containsMatchIn(this)

    /**
     * Compares two core versions like `0.8.1` part by part, missing parts count as 0.
     * Returns a negative number when [a] is older, 0 when equal and a positive number when newer.
     */
    private fun compareCore(a: String, b: String): Int {
        val pa = a.split('.').map { it.toIntOrNull() ?: 0 }
        val pb = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val diff = pa.getOrElse(i) { 0 } - pb.getOrElse(i) { 0 }
            if (diff != 0) return diff
        }
        return 0
    }

    /**
     * True only when [latest] is really newer than [local]. A local dev or beta build ahead of the
     * last release is not outdated. On the same core version a full release beats a local pre-release.
     */
    private fun isNewer(latest: String, local: String): Boolean {
        val cmp = compareCore(latest.extractCoreVersion(), local.extractCoreVersion())
        if (cmp != 0) return cmp > 0
        return local.isPreRelease() && !latest.isPreRelease()
    }

    fun init() {
        check()
        Register.onTick(RECHECK_INTERVAL_TICKS) { check() }
    }

    fun check() {
        val mcVersion = SBOKotlin.mcVersion
        val url = "https://api.modrinth.com/v2/project/$MODRINTH_ID/version?game_versions=%5B%22$mcVersion%22%5D"

        SBOKotlin.logger.info("Update check started (MC: $mcVersion)")

        Http.sendGetRequest(url).toJson<List<ModrinthVersion>>(ignoreUnknownKeys = true) { versions ->
            val latestEntry = versions.firstOrNull() ?: return@toJson
            val rawLatest = latestEntry.versionNumber

            if (isNewer(rawLatest, SBOKotlin.version)) {
                latestVersion = rawLatest
                latestId = latestEntry.id
                isUpdateAvailable = true
                SBOKotlin.logger.info("Update found for $mcVersion: $rawLatest")
            }
        }.error {
            SBOKotlin.logger.error("Update check failed: ${it.message}")
        }
    }

    internal fun printUpdateMessage() {
        val breakLine = Chat.getChatBreak(" ", "§a§m")

        val versionUrl = "https://modrinth.com/mod/skyblock-overhaul/version/$latestId"

        SBOKotlin.toast(
                Component.literal("SBO").setStyle(
                    Style.EMPTY.withColor(ChatFormatting.GOLD)
                ),
                Component.literal("Outdated version, please update!").setStyle(
                    Style.EMPTY.withColor(ChatFormatting.YELLOW)
                )
        )

        Chat.chat(breakLine)
        Chat.clickableChat(
            "§6[SBO] §eUpdate available: §a$latestVersion §b[Click]",
            "§eOpen Version on Modrinth"
        ) { SBOKotlin.openInBrowser(versionUrl) }
        Chat.chat(breakLine)
    }
}
