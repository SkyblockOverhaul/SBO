package net.sbo.mod.test

import com.google.gson.JsonParser
import net.sbo.mod.utils.data.cloud.CloudDiff
import net.sbo.mod.utils.data.cloud.CloudSide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CloudDiffTest {
    private val pcConfig = """
        {
            // comment
            "rconfig:version": 0,
            "General": { "bobberOverlay": true, "legionOverlay": true },
            "Diana": { "spadeGuess": true, "hideOwnWaypoints": ["NORMAL", "INQ"] }
        }
    """.trimIndent()
    private val cloudConfig = """{"rconfig:version":1,"General":{"bobberOverlay":false,"legionOverlay":true},"Diana":{"spadeGuess":true,"hideOwnWaypoints":["NORMAL"]}}"""

    private val pc = mapOf(
        CloudDiff.CONFIG to pcConfig,
        "SboData.json" to """{"mobsSinceInq":5,"b2bInq":false}""",
        "dianaTrackerTotal.json" to """{"items":{"CHIMERA":2,"COINS":100},"mobs":{"INQ":4}}""",
        "overlayData.json" to """{"overlays":{}}""",
    )
    private val cloud = mapOf(
        CloudDiff.CONFIG to cloudConfig,
        "SboData.json" to """{"mobsSinceInq":9,"b2bInq":false,"newField":"x"}""",
        "dianaTrackerTotal.json" to """{"items":{"CHIMERA":3,"COINS":100},"mobs":{"INQ":5}}""",
        "overlayData.json" to """{"overlays":{}}""",
    )

    @Test
    fun comparesPerFieldAndWhole() {
        val areas = CloudDiff.compare(pc, cloud)
        assertEquals(listOf("config", "SboData.json", "dianaTrackerTotal.json"), areas.map { it.file })

        val settings = areas[0]
        assertTrue(settings.perField)
        // Comments and the config version don't count
        assertEquals(listOf("General › Bobber Overlay", "Diana › Hide Own Waypoints"), settings.fields.map { it.label })
        assertEquals("On", settings.fields[0].pc)
        assertEquals("Off", settings.fields[0].cloud)

        val data = areas[1]
        assertEquals(listOf("Mobs Since Inq", "New Field"), data.fields.map { it.label })
        assertEquals(null, data.fields[1].pc)

        val tracker = areas[2]
        assertFalse(tracker.perField)
        assertEquals(listOf("Items › CHIMERA", "Mobs › INQ"), tracker.fields.map { it.label })
    }

    @Test
    fun mergesPickedValues() {
        val areas = CloudDiff.compare(pc, cloud)
        val bobber = areas[0].fields.first { it.label.endsWith("Bobber Overlay") }.id
        val newField = areas[1].fields.first { it.label == "New Field" }.id
        val merged = CloudDiff.merge(pc, cloud, areas, mapOf(bobber to CloudSide.CLOUD, newField to CloudSide.CLOUD, "dianaTrackerTotal.json" to CloudSide.CLOUD))

        val config = JsonParser.parseString(merged.getValue(CloudDiff.CONFIG)).asJsonObject
        assertFalse(config.getAsJsonObject("General").get("bobberOverlay").asBoolean)
        // Not picked: stays like on this PC
        assertEquals(2, config.getAsJsonObject("Diana").getAsJsonArray("hideOwnWaypoints").size())

        val data = JsonParser.parseString(merged.getValue("SboData.json")).asJsonObject
        assertEquals(5, data.get("mobsSinceInq").asInt)
        assertEquals("x", data.get("newField").asString)

        assertEquals(cloud["dianaTrackerTotal.json"], merged["dianaTrackerTotal.json"])
        assertEquals(pc["overlayData.json"], merged["overlayData.json"])
    }

    @Test
    fun nothingPickedKeepsThisPc() {
        val areas = CloudDiff.compare(pc, cloud)
        assertEquals(pc, CloudDiff.merge(pc, cloud, areas, emptyMap()))
    }

    @Test
    fun cloudValueMissingRemovesField() {
        val areas = CloudDiff.compare(cloud, pc)
        val newField = areas.first { it.file == "SboData.json" }.fields.first { it.label == "New Field" }.id
        val merged = CloudDiff.merge(cloud, pc, areas, mapOf(newField to CloudSide.CLOUD))
        assertFalse(JsonParser.parseString(merged.getValue("SboData.json")).asJsonObject.has("newField"))
    }

    @Test
    fun humanizesKeys() {
        assertEquals("Mobs Since Inq", CloudDiff.humanize("mobsSinceInq"))
        assertEquals("Cloud Sync", CloudDiff.humanize("Cloud Sync"))
        assertEquals("B2b Inq", CloudDiff.humanize("b2bInq"))
    }
}
