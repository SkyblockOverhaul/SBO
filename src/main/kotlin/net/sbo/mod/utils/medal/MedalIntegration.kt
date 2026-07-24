package net.sbo.mod.utils.medal

import net.sbo.mod.SBOKotlin
import net.sbo.mod.settings.categories.Medal
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.SboDataObject.dianaTrackerMayor
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
        val enabled: Boolean,
        val durationSeconds: Int,
        val captureDelayMs: Int
    )

    private val chimeraEvent: MedalEvent
        get() = event(
            eventId = "1",
            dropName = "Chimera Drop",
            dropNumber = dianaTrackerMayor.items.CHIMERA + dianaTrackerMayor.items.CHIMERA_LS + 1,
            enabled = Medal.medalChimeraEnabled
        )

    private val woolEvent: MedalEvent
        get() = event(
            eventId = "2",
            dropName = "Shimmering Wool Drop",
            dropNumber = dianaTrackerMayor.items.SHIMMERING_WOOL + dianaTrackerMayor.items.SHIMMERING_WOOL_LS + 1,
            enabled = Medal.medalWoolEnabled
        )

    private val brainFoodEvent: MedalEvent
        get() = event(
            eventId = "3",
            dropName = "Brainfood Drop",
            dropNumber = dianaTrackerMayor.items.BRAIN_FOOD + dianaTrackerMayor.items.BRAIN_FOOD_LS + 1,
            enabled = Medal.medalBrainFoodEnabled
        )

    private val minosRelicEvent: MedalEvent
        get() = event(
            eventId = "4",
            dropName = "Minos Relic Drop",
            dropNumber = dianaTrackerMayor.items.MINOS_RELIC + 1,
            enabled = Medal.medalMinosRelicEnabled
        )

    private val mantiCoreEvent: MedalEvent
        get() = event(
            eventId = "5",
            dropName = "Manti-core Drop",
            dropNumber = dianaTrackerMayor.items.MANTI_CORE + dianaTrackerMayor.items.MANTI_CORE_LS + 1,
            enabled = Medal.medalMantiCoreEnabled
        )

    private val fatefulStingerEvent: MedalEvent
        get() = event(
            eventId = "6",
            dropName = "Fateful Stinger Drop",
            dropNumber = dianaTrackerMayor.items.FATEFUL_STINGER + dianaTrackerMayor.items.FATEFUL_STINGER_LS + 1,
            enabled = Medal.medalFatefulStingerEnabled
        )

    private val daedalusStickEvent: MedalEvent
        get() = event(
            eventId = "7",
            dropName = "Daedalus Stick Drop",
            dropNumber = dianaTrackerMayor.items.DAEDALUS_STICK + 1,
            enabled = Medal.medalDaedalusStickEnabled
        )

    private val braidedFeatherEvent: MedalEvent
        get() = event(
            eventId = "8",
            dropName = "Braided Griffin Feather Drop",
            dropNumber = dianaTrackerMayor.items.BRAIDED_GRIFFIN_FEATHER + 1,
            enabled = Medal.medalBraidedGriffinFeatherEnabled
        )

    private val mythologicalDyeEvent: MedalEvent
        get() = event(
            eventId = "9",
            dropName = "Mythological Dye Drop",
            dropNumber = dianaTrackerMayor.items.MYTHOLOGICAL_DYE + 1,
            enabled = Medal.medalMythologicalDyeEnabled
        )

    private val mythTheFishEvent: MedalEvent
        get() = event(
            eventId = "10",
            dropName = "Myth the Fish Drop",
            dropNumber = dianaTrackerMayor.items.MYTH_THE_FISH + 1,
            enabled = Medal.medalMythTheFishEnabled
        )

    private val crownOfGreedEvent: MedalEvent
        get() = event(
            eventId = "11",
            dropName = "Crown of Greed Drop",
            dropNumber = dianaTrackerMayor.items.CROWN_OF_GREED + 1,
            enabled = Medal.medalCrownOfGreedEnabled
        )

    private val hiltOfRevelationsEvent: MedalEvent
        get() = event(
            eventId = "12",
            dropName = "Hilt of Revelations Drop",
            dropNumber = dianaTrackerMayor.items.HILT_OF_REVELATIONS + 1,
            enabled = Medal.medalHiltOfRevelationsEnabled
        )

    private val washedUpSouvenirEvent: MedalEvent
        get() = event(
            eventId = "13",
            dropName = "Washed-up Souvenir Drop",
            dropNumber = dianaTrackerMayor.items.WASHED_UP_SOUVENIR + 1,
            enabled = Medal.medalWashedUpSouvenirEnabled
        )

    private val dwarfTurtleShelmetEvent: MedalEvent
        get() = event(
            eventId = "14",
            dropName = "Dwarf Turtle Shelmet Drop",
            dropNumber = dianaTrackerMayor.items.DWARF_TURTLE_SHELMET + 1,
            enabled = Medal.medalDwarfTurtleShelmetEnabled
        )

    private val crochetTigerPlushieEvent: MedalEvent
        get() = event(
            eventId = "15",
            dropName = "Crochet Tiger Plushie Drop",
            dropNumber = dianaTrackerMayor.items.CROCHET_TIGER_PLUSHIE + 1,
            enabled = Medal.medalCrochetTigerPlushieEnabled
        )

    private val antiqueRemediesEvent: MedalEvent
        get() = event(
            eventId = "16",
            dropName = "Antique Remedies Drop",
            dropNumber = dianaTrackerMayor.items.ANTIQUE_REMEDIES + 1,
            enabled = Medal.medalAntiqueRemediesEnabled
        )

    private val cretanUrnEvent: MedalEvent
        get() = event(
            eventId = "17",
            dropName = "Cretan Urn Drop",
            dropNumber = dianaTrackerMayor.items.CRETAN_URN + 1,
            enabled = Medal.medalCretanUrnEnabled
        )

    private val allEvents: List<MedalEvent>
        get() = listOf(
            chimeraEvent,
            woolEvent,
            brainFoodEvent,
            minosRelicEvent,
            mantiCoreEvent,
            fatefulStingerEvent,
            daedalusStickEvent,
            braidedFeatherEvent,
            mythologicalDyeEvent,
            mythTheFishEvent,
            crownOfGreedEvent,
            hiltOfRevelationsEvent,
            washedUpSouvenirEvent,
            dwarfTurtleShelmetEvent,
            crochetTigerPlushieEvent,
            antiqueRemediesEvent,
            cretanUrnEvent
        )

    fun saveChimeraClip() = triggerEvent(chimeraEvent)

    fun saveWoolClip() = triggerEvent(woolEvent)

    fun saveBrainFoodClip() = triggerEvent(brainFoodEvent)

    fun saveMinosRelicClip() = triggerEvent(minosRelicEvent)

    fun saveMantiCoreClip() = triggerEvent(mantiCoreEvent)

    fun saveFatefulStingerClip() = triggerEvent(fatefulStingerEvent)

    fun saveDaedalusStickClip() = triggerEvent(daedalusStickEvent)

    fun saveBraidedFeatherClip() = triggerEvent(braidedFeatherEvent)

    fun saveMythologicalDyeClip() = triggerEvent(mythologicalDyeEvent)

    fun saveMythTheFishClip() = triggerEvent(mythTheFishEvent)

    fun saveCrownOfGreedClip() = triggerEvent(crownOfGreedEvent)

    fun saveHiltOfRevelationsClip() = triggerEvent(hiltOfRevelationsEvent)

    fun saveWashedUpSouvenirClip() = triggerEvent(washedUpSouvenirEvent)

    fun saveDwarfTurtleShelmetClip() = triggerEvent(dwarfTurtleShelmetEvent)

    fun saveCrochetTigerPlushieClip() = triggerEvent(crochetTigerPlushieEvent)

    fun saveAntiqueRemediesClip() = triggerEvent(antiqueRemediesEvent)

    fun saveCretanUrnClip() = triggerEvent(cretanUrnEvent)

    private fun event(
        eventId: String,
        dropName: String,
        dropNumber: Int,
        enabled: Boolean
    ): MedalEvent {
        return MedalEvent(
            eventId = eventId,
            eventName = clipTitle(dropName, dropNumber),
            displayName = displayTitle(dropName, dropNumber),
            enabled = enabled,
            durationSeconds = Medal.medalClipDurationSeconds,
            captureDelayMs = Medal.medalCaptureDelayMs
        )
    }

    private fun clipTitle(dropName: String, dropNumber: Int): String {
        return "[${sbYear} YEAR] $dropName #$dropNumber"
    }

    private fun displayTitle(dropName: String, dropNumber: Int): String {
        return "$dropName #$dropNumber"
    }

    fun testClip(clip: String?) {
        if (!Medal.medalEnabled) {
            Chat.chat("§6[SBO] §cMedal Clips are disabled. Enable them before testing.")
            return
        }

        val events = when (clip?.lowercase()?.replace("-", "")?.replace("_", "")) {
            null, "", "all" -> allEvents
            "chim", "chimera" -> listOf(chimeraEvent)
            "wool", "shimmeringwool" -> listOf(woolEvent)
            "bf", "food", "brainfood", "brainfoods" -> listOf(brainFoodEvent)
            "relic", "minosrelic" -> listOf(minosRelicEvent)
            "core", "manti", "manticore", "manticoredrop" -> listOf(mantiCoreEvent)
            "stinger", "fatefulstinger" -> listOf(fatefulStingerEvent)
            "stick", "daedalus", "daedalusstick" -> listOf(daedalusStickEvent)
            "braid", "braided", "feather", "braidedfeather", "braidedgriffinfeather" -> listOf(braidedFeatherEvent)
            "dye", "mythologicaldye" -> listOf(mythologicalDyeEvent)
            "fish", "myth", "myththefish" -> listOf(mythTheFishEvent)
            "cog", "crown", "crownofgreed" -> listOf(crownOfGreedEvent)
            "hilt", "hiltofrevelations" -> listOf(hiltOfRevelationsEvent)
            "souvenir", "washedupsouvenir" -> listOf(washedUpSouvenirEvent)
            "shelmet", "dwarfturtleshelmet" -> listOf(dwarfTurtleShelmetEvent)
            "plushie", "crochettigerplushie" -> listOf(crochetTigerPlushieEvent)
            "remedies", "antiqueremedies" -> listOf(antiqueRemediesEvent)
            "urn", "cretanurn" -> listOf(cretanUrnEvent)
            else -> {
                Chat.chat("§6[SBO] §cUsage: /sbotestmedalclip [chimera|wool|brainfood|relic|core|stinger|stick|braided|dye|fish|crown|hilt|souvenir|shelmet|plushie|remedies|urn|all]")
                return
            }
        }

        SBOKotlin.logger.info("Sending Medal test clip request${if (events.size > 1) "s" else ""}: ${events.joinToString { it.eventName }}")
        events.forEach { triggerEvent(it, showSuccess = true) }
    }

    private fun triggerEvent(event: MedalEvent, showSuccess: Boolean = false) {
        if (!Medal.medalEnabled) return
        if (!event.enabled) return


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
