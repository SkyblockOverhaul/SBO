package net.sbo.mod.utils.medal

import net.sbo.mod.SBOKotlin
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.SboDataObject.dianaTrackerMayor
import net.sbo.mod.utils.game.Mayor
import net.sbo.mod.utils.game.Mayor.sbYear
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture

object MedalIntegration {

    private const val PUBLIC_KEY = "pub_gm8PleBWBACgpFhYEZGohVHylz7WtPoN"
    private const val MEDAL_API_URL = "http://localhost:12665/api/v1/event/invoke"

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()

    private data class MedalEvent(
        val eventId: String,
        val eventName: String,
        val displayName: String,
        val durationSeconds: Int,
        val captureDelayMs: Int
    )

    private val chimeraEvent: MedalEvent
        get() {
            val dropNumber = dianaTrackerMayor.items.CHIMERA + dianaTrackerMayor.items.CHIMERA_LS + 1
            return MedalEvent(
                eventId = "1",
                eventName = clipTitle("Chimera Drop", dropNumber),
                displayName = displayTitle("Chimera Drop", dropNumber),
                durationSeconds = Diana.medalChimeraClipDurationSeconds,
                captureDelayMs = Diana.medalChimeraCaptureDelayMs
            )
        }

    private val woolEvent: MedalEvent
        get() {
            val dropNumber = dianaTrackerMayor.items.SHIMMERING_WOOL + dianaTrackerMayor.items.SHIMMERING_WOOL_LS + 1
            return MedalEvent(
                eventId = "2",
                eventName = clipTitle("Shimmering Wool Drop", dropNumber),
                displayName = displayTitle("Shimmering Wool Drop", dropNumber),
                durationSeconds = Diana.medalWoolClipDurationSeconds,
                captureDelayMs = Diana.medalWoolCaptureDelayMs
            )
        }

    private val brainFoodEvent: MedalEvent
        get() {
            val dropNumber = dianaTrackerMayor.items.BRAIN_FOOD + dianaTrackerMayor.items.BRAIN_FOOD_LS + 1
            return MedalEvent(
                eventId = "3",
                eventName = clipTitle("Brainfood Drop", dropNumber),
                displayName = displayTitle("Brainfood Drop", dropNumber),
                durationSeconds = Diana.medalBrainFoodClipDurationSeconds,
                captureDelayMs = Diana.medalBrainFoodCaptureDelayMs
            )
        }

    private val minosRelicEvent: MedalEvent
        get() {
            val dropNumber = dianaTrackerMayor.items.MINOS_RELIC + 1
            return MedalEvent(
                eventId = "4",
                eventName = clipTitle("Minos Relic Drop", dropNumber),
                displayName = displayTitle("Minos Relic Drop", dropNumber),
                durationSeconds = Diana.medalMinosRelicClipDurationSeconds,
                captureDelayMs = Diana.medalMinosRelicCaptureDelayMs
            )
        }

    fun saveChimeraClip() = triggerEvent(chimeraEvent)

    fun saveWoolClip() = triggerEvent(woolEvent)

    fun saveBrainFoodClip() = triggerEvent(brainFoodEvent)

    fun saveMinosRelicClip() = triggerEvent(minosRelicEvent)

    private fun clipTitle(dropName: String, dropNumber: Int): String {
        return "[${sbYear} YEAR] $dropName #$dropNumber"
    }

    private fun displayTitle(dropName: String, dropNumber: Int): String {
        return "$dropName #$dropNumber"
    }

    fun testClip(clip: String?) {
        if (!Diana.medalEnabled) {
            Chat.chat("§6[SBO] §cMedal Clips are disabled. Enable them before testing.")
            return
        }

        val events = when (clip?.lowercase()?.replace("-", "")?.replace("_", "")) {
            null, "", "all" -> listOf(chimeraEvent, woolEvent, brainFoodEvent, minosRelicEvent)
            "chim", "chimera" -> listOf(chimeraEvent)
            "wool", "shimmeringwool" -> listOf(woolEvent)
            "bf", "food", "brainfood", "brainfoods" -> listOf(brainFoodEvent)
            "relic", "minosrelic" -> listOf(minosRelicEvent)
            else -> {
                Chat.chat("§6[SBO] §cUsage: /sbotestmedalclip [chimera|wool|brainfood|relic|all]")
                return
            }
        }

        SBOKotlin.logger.info("Sending Medal test clip request${if (events.size > 1) "s" else ""}: ${events.joinToString { it.eventName }}")
        events.forEach { triggerEvent(it, showSuccess = true) }
    }

    private fun triggerEvent(event: MedalEvent, showSuccess: Boolean = false) {
        if (!Diana.medalEnabled) return

        val body = """
            {"eventId":"${event.eventId}","eventName":"${event.eventName}","triggerActions":["SaveClip"],"clipOptions":{"duration":${event.durationSeconds},"captureDelayMs":${event.captureDelayMs}}}
        """.trimIndent()

        CompletableFuture.runAsync {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(MEDAL_API_URL))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("publicKey", PUBLIC_KEY)
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build()

                val response = client.send(request, HttpResponse.BodyHandlers.discarding())
                if (response.statusCode() == 200) {
                    if (showSuccess) Chat.chat("§6[SBO] §aClipping ${event.displayName}.")
                } else {
                    SBOKotlin.logger.warn("Medal clip failed for ${event.eventName}: HTTP ${response.statusCode()}")
                    Chat.chat("§6[SBO] §cFailed to clip. Medal may not be running.")
                }
            } catch (error: Exception) {
                SBOKotlin.logger.warn("Medal clip failed for ${event.eventName}", error)
                Chat.chat("§6[SBO] §cFailed to clip. Medal may not be running.")
            }
        }
    }
}
