package net.sbo.mod.utils.version

import net.minecraft.Util
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.http.Http

object UpdateChecker {
    private const val MODRINTH_ID = "9lBqVbQF"
    var latestVersion: String? = null
    var latestId: String? = null
    var isUpdateAvailable = false

    private fun String.extractCoreVersion(): String {
        val regex = Regex("""(\d+\.\d+\.\d+|\d+\.\d+)""")
        return regex.find(this)?.value ?: this
    }

    fun check() {
        val mcVersion = SBOKotlin.mcVersion
        val url = "https://api.modrinth.com/v2/project/$MODRINTH_ID/version?game_versions=%5B%22$mcVersion%22%5D"

        SBOKotlin.logger.info("[SBO] Update-Check gestartet (MC: $mcVersion)")

        Http.sendGetRequest(url).toJson<List<ModrinthVersion>>(ignoreUnknownKeys = true) { versions ->
            val latestEntry = versions.firstOrNull() ?: return@toJson
            val rawLatest = latestEntry.versionNumber

            val coreLatest = rawLatest.extractCoreVersion()
            val coreLocal = SBOKotlin.version.extractCoreVersion()

            if (coreLatest != coreLocal) {
                latestVersion = rawLatest
                latestId = latestEntry.id
                isUpdateAvailable = true
                SBOKotlin.logger.info("[SBO] Update für $mcVersion gefunden: $rawLatest")
            }
        }.error {
            SBOKotlin.logger.error("[SBO] Update-Check fehlgeschlagen: ${it.message}")
        }
    }

    internal fun printUpdateMessage() {
        val breakLine = Chat.getChatBreak(" ", "§a§m")

        val versionUrl = "https://modrinth.com/mod/skyblock-overhaul/version/${latestId}"

        Chat.chat(breakLine)
        Chat.clickableChat(
            "§6[SBO] §eUpdate available: §a${latestVersion} §b[Click]",
            "§eOpen Version on Modrinth"
        ) { Util.getPlatform().openUri(versionUrl) }
        Chat.chat(breakLine)
    }
}