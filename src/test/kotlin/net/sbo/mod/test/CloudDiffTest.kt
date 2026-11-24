package net.sbo.mod.test

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.sbo.mod.utils.data.cloud.CloudComparison
import net.sbo.mod.utils.data.cloud.CloudDiff
import net.sbo.mod.utils.data.cloud.CloudSide
import net.sbo.mod.utils.data.cloud.CloudTimes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CloudDiffTest {
    private val pcConfig = """
        {
            // comment
            "rconfig:version": 0,
            "General": { "bobberOverlay": true, "legionOverlay": true, "fastAotv": false },
            "Diana": { "spadeGuess": true, "hideOwnWaypoints": ["NORMAL", "INQ"] },
            "Keybinds": { "guessWarpKey": "key.keyboard.g" }
        }
    """.trimIndent()
    private val cloudConfig = """{"rconfig:version":1,"General":{"bobberOverlay":false,"legionOverlay":true},"Diana":{"spadeGuess":true,"hideOwnWaypoints":["NORMAL"]}}"""

    private val pc = mapOf(
        CloudDiff.CONFIG to pcConfig,
        "SboData.json" to """{"mobsSinceInq":5,"b2bInq":false,"settingsVersion":"0.8.0","lastStatsProfile":""}""",
        "dianaTrackerTotal.json" to """{"items":{"CHIMERA":2,"COINS":100},"mobs":{"INQ":4}}""",
        "dianaTrackerSession.json" to """{"items":{"LOOTING_7":0},"mobs":{}}""",
        "overlayData.json" to """{"overlays":{}}""",
    )
    private val cloud = mapOf(
        CloudDiff.CONFIG to cloudConfig,
        "SboData.json" to """{"mobsSinceInq":9,"b2bInq":false,"settingsVersion":"0.7.2","lastStatsProfile":"Papaya","newField":"x"}""",
        "dianaTrackerTotal.json" to """{"items":{"CHIMERA":3,"COINS":100},"mobs":{"INQ":5}}""",
        "dianaTrackerSession.json" to """{"items":{},"mobs":{}}""",
        "overlayData.json" to """{"overlays":{}}""",
    )

    private fun json(text: String?): JsonObject = JsonParser.parseString(text).asJsonObject

    private fun mergeAll(pc: Map<String, String>, cloud: Map<String, String>, result: CloudComparison, choices: Map<String, CloudSide> = emptyMap()) =
        CloudDiff.merge(pc, cloud, result, choices)

    @Test
    fun showsOnlyWhatNeedsAChoice() {
        val result = CloudDiff.compare(pc, cloud)
        assertEquals(listOf("config", "SboData.json", CloudDiff.TRACKERS), result.areas.map { it.id })

        val settings = result.areas[0]
        assertTrue(settings.perField)
        // Comments, the config version and values only one side has don't count
        assertEquals(listOf("General › Bobber Overlay", "Diana › Hide Own Waypoints"), settings.fields.map { it.label })
        assertEquals("On", settings.fields[0].pc)
        assertEquals("Off", settings.fields[0].cloud)

        // Versions are hidden, newField only exists in the cloud, the empty stats profile was never set
        assertEquals(listOf("Mobs Since Inq"), result.areas[1].fields.map { it.label })

        // Without the last sync the counts cannot be added, Session only misses a value
        val tracker = result.areas[2]
        assertFalse(tracker.perField)
        assertEquals(listOf("Items › CHIMERA", "Mobs › INQ"), tracker.fields.map { it.label })
        assertEquals(listOf("Total", "Total"), tracker.fields.map { it.group })

        // fastAotv, Keybinds, lastStatsProfile, newField, LOOTING_7
        assertEquals(5, result.auto.size)
    }

    @Test
    fun guessesNewerWithoutLastSync() {
        val result = CloudDiff.compare(pc, cloud)
        // Settings: no guess
        assertTrue(result.areas[0].fields.all { it.newer == null })
        assertEquals(CloudSide.CLOUD, result.areas[1].fields.single().newer)
        // The cloud tracker counted more
        assertEquals(CloudSide.CLOUD, result.areas[2].newer)

        val suggested = CloudDiff.suggested(result.areas)
        assertEquals(CloudSide.CLOUD, suggested[CloudDiff.TRACKERS])
        assertEquals(CloudSide.PC, suggested[result.areas[0].fields[0].id])
    }

    @Test
    fun changesOnOneSideAreTakenOnTheirOwn() {
        val base = mapOf(
            CloudDiff.CONFIG to """{"General":{"bobberOverlay":true,"legionOverlay":true},"Diana":{"spadeGuess":true,"hideOwnWaypoints":["NORMAL"]}}""",
            "SboData.json" to """{"mobsSinceInq":5,"lastStatsProfile":"Old"}""",
            "dianaTrackerTotal.json" to """{"items":{"CHIMERA":2,"COINS":100},"mobs":{"INQ":4}}""",
        )
        val result = CloudDiff.compare(pc, cloud, base)
        // Only the stats profile was changed on both sides
        val profile = result.areas.single().fields.single()
        assertEquals("Last Stats Profile", profile.label)
        assertTrue(profile.bothChanged)

        val merged = mergeAll(pc, cloud, result)
        val config = json(merged[CloudDiff.CONFIG])
        assertFalse(config.getAsJsonObject("General").get("bobberOverlay").asBoolean)
        assertEquals(2, config.getAsJsonObject("Diana").getAsJsonArray("hideOwnWaypoints").size())
        assertEquals(9, json(merged["SboData.json"]).get("mobsSinceInq").asInt)
        assertEquals(3, json(merged["dianaTrackerTotal.json"]).getAsJsonObject("items").get("CHIMERA").asInt)
    }

    @Test
    fun countsOfBothSidesAreAddedOnce() {
        val base = mapOf("dianaTrackerTotal.json" to """{"items":{"CHIMERA":100,"CoA Time":-1},"mobs":{"INQ":10}}""")
        val pcFiles = mapOf("dianaTrackerTotal.json" to """{"items":{"CHIMERA":103,"CoA Time":50},"mobs":{"INQ":10}}""")
        val cloudFiles = mapOf("dianaTrackerTotal.json" to """{"items":{"CHIMERA":102,"CoA Time":20},"mobs":{"INQ":12}}""")
        val result = CloudDiff.compare(pcFiles, cloudFiles, base)
        assertTrue(result.areas.isEmpty())

        val total = json(mergeAll(pcFiles, cloudFiles, result)["dianaTrackerTotal.json"])
        assertEquals(105, total.getAsJsonObject("items").get("CHIMERA").asInt)
        // -1 means never counted
        assertEquals(70, total.getAsJsonObject("items").get("CoA Time").asInt)
        assertEquals(12, total.getAsJsonObject("mobs").get("INQ").asInt)
    }

    @Test
    fun resetOnOneSideNeedsAChoice() {
        val base = mapOf("dianaTrackerTotal.json" to """{"items":{"CHIMERA":100}}""")
        val pcFiles = mapOf("dianaTrackerTotal.json" to """{"items":{"CHIMERA":1}}""")
        val cloudFiles = mapOf("dianaTrackerTotal.json" to """{"items":{"CHIMERA":102}}""")
        val result = CloudDiff.compare(pcFiles, cloudFiles, base)
        assertEquals(listOf(CloudDiff.TRACKERS), result.areas.map { it.id })
    }

    @Test
    fun olderMayorTrackerLoses() {
        val pcFiles = mapOf("dianaTrackerMayor.json" to """{"year":527,"items":{"CHIMERA":1},"resetCount":3}""")
        val cloudFiles = mapOf("dianaTrackerMayor.json" to """{"year":526,"items":{"CHIMERA":5},"resetCount":6}""")
        val result = CloudDiff.compare(pcFiles, cloudFiles)
        assertTrue(result.areas.isEmpty())
        val merged = mergeAll(pcFiles, cloudFiles, result)
        assertEquals(pcFiles["dianaTrackerMayor.json"], merged["dianaTrackerMayor.json"])
        // The older event is not lost, it moves to the past events like at an election
        val past = json(merged["pastDianaEvents.json"]).getAsJsonArray("events")
        assertEquals(1, past.size())
        assertEquals(526, past[0].asJsonObject.get("year").asInt)
        assertEquals(5, past[0].asJsonObject.getAsJsonObject("items").get("CHIMERA").asInt)
    }

    @Test
    fun changedLastWins() {
        val pcFiles = mapOf(CloudDiff.CONFIG to """{"General":{"x":1}}""")
        val cloudFiles = mapOf(CloudDiff.CONFIG to """{"General":{"x":2}}""")
        val id = "config\u0000General\u0000x"

        val pcLater = CloudDiff.compare(pcFiles, cloudFiles, times = CloudTimes(mapOf(id to 200L), mapOf(id to 100L)))
        assertTrue(pcLater.areas.isEmpty())
        assertEquals(1, json(mergeAll(pcFiles, cloudFiles, pcLater)[CloudDiff.CONFIG]).getAsJsonObject("General").get("x").asInt)

        val cloudLater = CloudDiff.compare(pcFiles, cloudFiles, times = CloudTimes(mapOf(id to 100L), mapOf(id to 200L)))
        assertEquals(2, json(mergeAll(pcFiles, cloudFiles, cloudLater)[CloudDiff.CONFIG]).getAsJsonObject("General").get("x").asInt)

        // Unknown or same time: SBO cannot tell
        assertEquals(1, CloudDiff.compare(pcFiles, cloudFiles, times = CloudTimes(mapOf(id to 100L))).areas.size)
        assertEquals(1, CloudDiff.compare(pcFiles, cloudFiles, times = CloudTimes(mapOf(id to 100L), mapOf(id to 100L))).areas.size)
    }

    @Test
    fun achievementsFollowTheNewerEvent() {
        val pcFiles = mapOf("sbo_achievements.json" to """{"lastEventYear":527,"currentEventAchievements":{"3":true},"totalAchievements":{"1":2,"3":1}}""")
        val cloudFiles = mapOf("sbo_achievements.json" to """{"lastEventYear":525,"currentEventAchievements":{"1":true},"totalAchievements":{"1":2}}""")
        val result = CloudDiff.compare(pcFiles, cloudFiles)
        assertTrue(result.areas.isEmpty())
        val merged = json(mergeAll(pcFiles, cloudFiles, result)["sbo_achievements.json"])
        assertEquals(527, merged.get("lastEventYear").asInt)
        // The old event's unlock does not come back
        assertEquals(setOf("3"), merged.getAsJsonObject("currentEventAchievements").keySet())
        assertEquals(1, merged.getAsJsonObject("totalAchievements").get("3").asInt)
    }

    @Test
    fun achievementsOfTheSameEventAreCombined() {
        val base = mapOf("sbo_achievements.json" to """{"lastEventYear":527,"currentEventAchievements":{},"totalAchievements":{"1":1}}""")
        val pcFiles = mapOf("sbo_achievements.json" to """{"lastEventYear":527,"currentEventAchievements":{"3":true},"totalAchievements":{"1":2}}""")
        val cloudFiles = mapOf("sbo_achievements.json" to """{"lastEventYear":527,"currentEventAchievements":{"1":true},"totalAchievements":{"1":3}}""")
        val result = CloudDiff.compare(pcFiles, cloudFiles, base)
        assertTrue(result.areas.isEmpty())
        val merged = json(mergeAll(pcFiles, cloudFiles, result)["sbo_achievements.json"])
        assertEquals(setOf("1", "3"), merged.getAsJsonObject("currentEventAchievements").keySet())
        assertEquals(4, merged.getAsJsonObject("totalAchievements").get("1").asInt)

        // Without the last sync the totals cannot be added
        assertEquals(1, CloudDiff.compare(pcFiles, cloudFiles).areas.size)
    }

    @Test
    fun otherCoaResetsTheStartCoins() {
        val pcFiles = mapOf(
            "SboData.json" to """{"currentCoaUuid":"a"}""",
            "dianaTrackerTotal.json" to """{"items":{"COA Start Coins":100}}""",
        )
        val cloudFiles = mapOf(
            "SboData.json" to """{"currentCoaUuid":"b"}""",
            "dianaTrackerTotal.json" to """{"items":{"COA Start Coins":500}}""",
        )
        val result = CloudDiff.compare(pcFiles, cloudFiles)
        assertTrue(result.coaChanged)
        val merged = mergeAll(pcFiles, cloudFiles, result)
        assertEquals(-1, json(merged["dianaTrackerTotal.json"]).getAsJsonObject("items").get("COA Start Coins").asInt)

        // Same CoA: the start coins only grow
        val same = mapOf("SboData.json" to """{"currentCoaUuid":"a"}""", "dianaTrackerTotal.json" to """{"items":{"COA Start Coins":500}}""")
        val sameResult = CloudDiff.compare(pcFiles, same)
        assertFalse(sameResult.coaChanged)
        assertEquals(500, json(mergeAll(pcFiles, same, sameResult)["dianaTrackerTotal.json"]).getAsJsonObject("items").get("COA Start Coins").asInt)
    }

    @Test
    fun deletedSinceLastSyncStaysDeleted() {
        val pcFiles = mapOf("SboData.json" to """{"a":1}""")
        val cloudFiles = mapOf("SboData.json" to """{"a":1,"b":2}""")
        val base = mapOf("SboData.json" to """{"a":1,"b":2}""")
        val result = CloudDiff.compare(pcFiles, cloudFiles, base)
        assertTrue(result.areas.isEmpty())
        assertFalse(json(mergeAll(pcFiles, cloudFiles, result)["SboData.json"]).has("b"))
    }

    @Test
    fun mergesPickedValuesAndTheRest() {
        val result = CloudDiff.compare(pc, cloud)
        val bobber = result.areas[0].fields.first { it.label.endsWith("Bobber Overlay") }.id
        val merged = mergeAll(pc, cloud, result, mapOf(bobber to CloudSide.CLOUD, CloudDiff.TRACKERS to CloudSide.CLOUD))

        val config = json(merged[CloudDiff.CONFIG])
        assertFalse(config.getAsJsonObject("General").get("bobberOverlay").asBoolean)
        // Not picked: stays like on this PC
        assertEquals(2, config.getAsJsonObject("Diana").getAsJsonArray("hideOwnWaypoints").size())
        assertEquals("key.keyboard.g", config.getAsJsonObject("Keybinds").get("guessWarpKey").asString)

        val data = json(merged["SboData.json"])
        assertEquals(5, data.get("mobsSinceInq").asInt)
        assertEquals("0.8.0", data.get("settingsVersion").asString)
        assertEquals("x", data.get("newField").asString)
        assertEquals("Papaya", data.get("lastStatsProfile").asString)

        assertEquals(3, json(merged["dianaTrackerTotal.json"]).getAsJsonObject("items").get("CHIMERA").asInt)
        assertEquals(0, json(merged["dianaTrackerSession.json"]).getAsJsonObject("items").get("LOOTING_7").asInt)
        assertEquals(pc["overlayData.json"], merged["overlayData.json"])
    }

    @Test
    fun allFromPcOnlyAddsMissingValues() {
        val result = CloudDiff.compare(pc, cloud)
        val merged = mergeAll(pc, cloud, result)
        // Untouched files keep their text
        assertEquals(pc["dianaTrackerTotal.json"], merged["dianaTrackerTotal.json"])
        assertEquals(pc["dianaTrackerSession.json"], merged["dianaTrackerSession.json"])
        assertTrue(json(merged[CloudDiff.CONFIG]).getAsJsonObject("General").get("bobberOverlay").asBoolean)
        assertEquals("x", json(merged["SboData.json"]).get("newField").asString)
    }

    @Test
    fun pastEventsAreComparedByYear() {
        val pcFiles = mapOf("pastDianaEvents.json" to """{"events":[{"year":520,"mobs":{"INQ":1}},{"year":527,"mobs":{"INQ":2}}]}""")
        val cloudFiles = mapOf("pastDianaEvents.json" to """{"events":[{"year":520,"mobs":{"INQ":3}},{"year":525,"mobs":{"INQ":4}}]}""")
        val result = CloudDiff.compare(pcFiles, cloudFiles)
        // 527 and 525 are only on one side, only 520 is a choice
        assertEquals(listOf("Events › Year 520 › Mobs › INQ"), result.areas.single().fields.map { it.label })
        assertEquals(2, result.auto.size)

        val merged = mergeAll(pcFiles, cloudFiles, result)
        val events = json(merged["pastDianaEvents.json"]).getAsJsonArray("events").map { it.asJsonObject }
        assertEquals(listOf(520, 525, 527), events.map { it.get("year").asInt })
        assertEquals(1, events[0].getAsJsonObject("mobs").get("INQ").asInt)

        // With the last sync the counts of the same event are added
        val base = mapOf("pastDianaEvents.json" to """{"events":[{"year":520,"mobs":{"INQ":0}}]}""")
        val withBase = CloudDiff.compare(pcFiles, cloudFiles, base)
        assertTrue(withBase.areas.isEmpty())
        val added = json(mergeAll(pcFiles, cloudFiles, withBase)["pastDianaEvents.json"]).getAsJsonArray("events")
        assertEquals(4, added[0].asJsonObject.getAsJsonObject("mobs").get("INQ").asInt)

        // Same events: the file keeps its text
        assertEquals(pcFiles, mergeAll(pcFiles, pcFiles, CloudDiff.compare(pcFiles, pcFiles)))
    }

    @Test
    fun fingerprintsPerSettingAndFile() {
        val ids = CloudDiff.fingerprints(mapOf(
            CloudDiff.CONFIG to """{"rconfig:version":1,"General":{"x":1,"y":2}}""",
            "overlayData.json" to "{}",
        )).keys
        assertEquals(setOf("config\u0000General\u0000x", "config\u0000General\u0000y", "overlayData.json"), ids)
    }

    @Test
    fun fileOnlyInTheCloudIsTaken() {
        val pcFiles = mapOf("a.json" to "{}")
        val cloudFiles = mapOf("a.json" to "{}", "b.json" to """{"x":1}""")
        val result = CloudDiff.compare(pcFiles, cloudFiles)
        assertTrue(result.areas.isEmpty())
        assertEquals(1, result.auto.size)
        assertEquals("""{"x":1}""", mergeAll(pcFiles, cloudFiles, result)["b.json"])
    }

    @Test
    fun resetOnOneSideIsKept() {
        val base = mapOf(
            "sbo_achievements.json" to """{"lastEventYear":527,"currentEventAchievements":{"1":true},"totalAchievements":{"1":2}}""",
            "dianaTrackerMayor.json" to """{"year":527,"items":{"CHIMERA":5},"edited":true}""",
        )
        val pcFiles = mapOf(
            "sbo_achievements.json" to """{"lastEventYear":-1,"currentEventAchievements":{},"totalAchievements":{}}""",
            "dianaTrackerMayor.json" to """{"year":0,"items":{"CHIMERA":0},"edited":false}""",
        )
        val result = CloudDiff.compare(pcFiles, base, base)
        assertTrue(result.areas.isEmpty())
        val merged = mergeAll(pcFiles, base, result)
        val achievements = json(merged["sbo_achievements.json"])
        assertEquals(-1, achievements.get("lastEventYear").asInt)
        assertTrue(achievements.getAsJsonObject("totalAchievements").keySet().isEmpty())
        val mayor = json(merged["dianaTrackerMayor.json"])
        assertEquals(0, mayor.get("year").asInt)
        assertEquals(0, mayor.getAsJsonObject("items").get("CHIMERA").asInt)
        assertFalse(mayor.get("edited").asBoolean)
    }

    @Test
    fun resetWithoutLastSyncAsks() {
        val pcFiles = mapOf("sbo_achievements.json" to """{"lastEventYear":-1,"currentEventAchievements":{},"totalAchievements":{}}""")
        val cloudFiles = mapOf("sbo_achievements.json" to """{"lastEventYear":527,"currentEventAchievements":{"1":true},"totalAchievements":{"1":2}}""")
        assertEquals(listOf("sbo_achievements.json"), CloudDiff.compare(pcFiles, cloudFiles).areas.map { it.id })
    }

    @Test
    fun emptyTrackerWithoutLastSyncTakesTheOtherOne() {
        // Like a new PC: nothing counted yet, so the cloud's tracker is taken
        val pcFiles = mapOf(
            "dianaTrackerMayor.json" to """{"year":0,"items":{"CHIMERA":0,"CoA Time":-1}}""",
            "dianaTrackerSession.json" to """{"items":{"CHIMERA":0},"mobs":{}}""",
        )
        val cloudFiles = mapOf(
            "dianaTrackerMayor.json" to """{"year":527,"items":{"CHIMERA":5,"CoA Time":-1}}""",
            "dianaTrackerSession.json" to """{"items":{"CHIMERA":2},"mobs":{}}""",
        )
        val result = CloudDiff.compare(pcFiles, cloudFiles)
        assertTrue(result.areas.isEmpty())
        val merged = mergeAll(pcFiles, cloudFiles, result)
        assertEquals(json(cloudFiles["dianaTrackerMayor.json"]), json(merged["dianaTrackerMayor.json"]))
        assertEquals(json(cloudFiles["dianaTrackerSession.json"]), json(merged["dianaTrackerSession.json"]))

        // Emptied on this PC after the cloud last changed it: a reset, it stays empty
        val resetHere = CloudDiff.compare(pcFiles, cloudFiles, times = CloudTimes(mapOf(session to 300L), mapOf(session to 100L)))
        assertTrue(resetHere.areas.isEmpty())
        assertEquals(0, json(mergeAll(pcFiles, cloudFiles, resetHere)[session]).getAsJsonObject("items").get("CHIMERA").asInt)
    }

    private val session = "dianaTrackerSession.json"
    private val resetKey = CloudDiff.resetKey(session)
    private val sessionBase = mapOf(session to """{"items":{"CHIMERA":10,"time":5000},"mobs":{"INQ":40},"edited":false}""")
    private val sessionReset = mapOf(session to """{"items":{"CHIMERA":1,"time":300},"mobs":{"INQ":2},"edited":false}""")
    private val sessionPlayed = mapOf(session to """{"items":{"CHIMERA":12,"time":6000},"mobs":{"INQ":45},"edited":false}""")

    @Test
    fun sessionResetOnOnePcIsKept() {
        // Reset here, the cloud did not change: the reset stays, nothing comes back
        val result = CloudDiff.compare(sessionReset, sessionBase, sessionBase)
        assertTrue(result.areas.isEmpty())
        assertEquals(json(sessionReset[session]), json(mergeAll(sessionReset, sessionBase, result)[session]))

        // Reset on the other PC, this one did not change: the cloud's reset tracker is taken
        val other = CloudDiff.compare(sessionBase, sessionReset, sessionBase)
        assertTrue(other.areas.isEmpty())
        assertEquals(json(sessionReset[session]), json(mergeAll(sessionBase, sessionReset, other)[session]))
    }

    @Test
    fun deletedTrackerFileStaysReset() {
        // A deleted file comes back empty when SBO starts
        val deleted = mapOf(session to """{"items":{"CHIMERA":0,"time":0},"mobs":{"INQ":0},"edited":false}""")
        val result = CloudDiff.compare(deleted, sessionBase, sessionBase)
        assertTrue(result.areas.isEmpty())
        assertEquals(0, json(mergeAll(deleted, sessionBase, result)[session]).getAsJsonObject("items").get("CHIMERA").asInt)
    }

    @Test
    fun resetAfterTheOtherPcPlayedWins() {
        // The other PC's counts all came before the reset here, the reset clears them
        val times = CloudTimes(mapOf(resetKey to 2000L, session to 2500L), mapOf(session to 1500L))
        val result = CloudDiff.compare(sessionReset, sessionPlayed, sessionBase, times)
        assertTrue(result.areas.isEmpty())
        assertEquals(json(sessionReset[session]), json(mergeAll(sessionReset, sessionPlayed, result)[session]))

        // Same the other way round, reset on the other PC
        val cloudTimes = CloudTimes(mapOf(session to 1500L), mapOf(resetKey to 2000L, session to 2500L))
        val other = CloudDiff.compare(sessionPlayed, sessionReset, sessionBase, cloudTimes)
        assertTrue(other.areas.isEmpty())
        assertEquals(json(sessionReset[session]), json(mergeAll(sessionPlayed, sessionReset, other)[session]))
    }

    @Test
    fun playedAfterAResetElsewhereAsks() {
        // The other PC counted after the reset: adding brings old counts back, dropping loses new ones
        val times = CloudTimes(mapOf(resetKey to 2000L, session to 2500L), mapOf(session to 3000L))
        assertEquals(listOf(CloudDiff.TRACKERS), CloudDiff.compare(sessionReset, sessionPlayed, sessionBase, times).areas.map { it.id })
        // Reset time unknown, e.g. from an older SBO
        assertEquals(listOf(CloudDiff.TRACKERS), CloudDiff.compare(sessionReset, sessionPlayed, sessionBase).areas.map { it.id })

        // The pick takes the whole tracker from that side
        val result = CloudDiff.compare(sessionReset, sessionPlayed, sessionBase, times)
        val fromCloud = mergeAll(sessionReset, sessionPlayed, result, mapOf(CloudDiff.TRACKERS to CloudSide.CLOUD))
        assertEquals(json(sessionPlayed[session]), json(fromCloud[session]))
        val fromPc = mergeAll(sessionReset, sessionPlayed, result, mapOf(CloudDiff.TRACKERS to CloudSide.PC))
        assertEquals(json(sessionReset[session]), json(fromPc[session]))
    }

    @Test
    fun resetOnBothPcsKeepsTheLaterOne() {
        val pcFiles = mapOf(session to """{"items":{"CHIMERA":1,"time":300},"mobs":{"INQ":2}}""")
        val cloudFiles = mapOf(session to """{"items":{"CHIMERA":3,"time":900},"mobs":{"INQ":5}}""")
        // The cloud was reset after this PC last counted
        val times = CloudTimes(mapOf(resetKey to 1000L, session to 1200L), mapOf(resetKey to 1500L, session to 1800L))
        val result = CloudDiff.compare(pcFiles, cloudFiles, sessionBase, times)
        assertTrue(result.areas.isEmpty())
        assertEquals(json(cloudFiles[session]), json(mergeAll(pcFiles, cloudFiles, result)[session]))
    }

    private val mayor = "dianaTrackerMayor.json"
    private val past = "pastDianaEvents.json"
    private fun mayorOf(year: Int, chimeras: Int, time: Int, resets: Int = 4, at: Long = 0) =
        """{"year":$year,"items":{"CHIMERA":$chimeras,"time":$time,"COA Start Coins":-1},"mobs":{"INQ":${chimeras * 10}},"edited":false,"resetCount":$resets,"lastTrackedAt":$at}"""
    private fun pastOf(vararg events: String) = """{"events":[${events.joinToString(",")}]}"""
    private fun chimeras(tracker: JsonObject) = tracker.getAsJsonObject("items").get("CHIMERA").asInt
    private fun pastEvent(merged: Map<String, String>, year: Int) =
        json(merged[past]).getAsJsonArray("events").map { it.asJsonObject }.single { it.get("year").asInt == year }

    @Test
    fun newMayorOnBothPcsAddsBothEvents() {
        // At the last sync year 527 had 10 chimeras. Both PCs played on, then the election moved both to 528.
        val base = mapOf(mayor to mayorOf(527, 10, 1000, at = 100), past to pastOf())
        val pcFiles = mapOf(mayor to mayorOf(528, 2, 200, resets = 5, at = 900), past to pastOf(mayorOf(527, 12, 1200, resets = 0, at = 300)))
        val cloudFiles = mapOf(mayor to mayorOf(528, 3, 300, resets = 5, at = 800), past to pastOf(mayorOf(527, 15, 1500, resets = 0, at = 400)))
        val result = CloudDiff.compare(pcFiles, cloudFiles, base)
        assertTrue(result.areas.isEmpty())
        val merged = mergeAll(pcFiles, cloudFiles, result)

        val newMayor = json(merged[mayor])
        assertEquals(528, newMayor.get("year").asInt)
        // Both count the new year from zero
        assertEquals(5, chimeras(newMayor))
        assertEquals(500, newMayor.getAsJsonObject("items").get("time").asInt)
        assertEquals(900, newMayor.get("lastTrackedAt").asInt)
        // 10 from before the sync, 2 more here, 5 more on the other PC
        val old = pastEvent(merged, 527)
        assertEquals(17, chimeras(old))
        assertEquals(1700, old.getAsJsonObject("items").get("time").asInt)
        assertEquals(170, old.getAsJsonObject("mobs").get("INQ").asInt)
        assertEquals(1, json(merged[past]).getAsJsonArray("events").size())
    }

    @Test
    fun newMayorOnOnePcKeepsTheOtherPcsCounts() {
        val base = mapOf(mayor to mayorOf(527, 10, 1000), past to pastOf())
        val moved = mapOf(mayor to mayorOf(528, 2, 200, resets = 5), past to pastOf(mayorOf(527, 12, 1200, resets = 0)))
        val stayed = mapOf(mayor to mayorOf(527, 15, 1500), past to pastOf())

        // This PC is in the new year, the cloud still has the old one with 5 more chimeras
        val here = CloudDiff.compare(moved, stayed, base)
        assertTrue(here.areas.isEmpty())
        val mergedHere = mergeAll(moved, stayed, here)
        assertEquals(json(moved[mayor]), json(mergedHere[mayor]))
        assertEquals(17, chimeras(pastEvent(mergedHere, 527)))

        // The other way round: the old year is on this PC, its event moves to the past events here
        val there = CloudDiff.compare(stayed, moved, base)
        assertTrue(there.areas.isEmpty())
        val mergedThere = mergeAll(stayed, moved, there)
        assertEquals(json(moved[mayor]), json(mergedThere[mayor]))
        assertEquals(17, chimeras(pastEvent(mergedThere, 527)))
        assertEquals(1, json(mergedThere[past]).getAsJsonArray("events").size())
    }

    @Test
    fun newMayorWithoutLastSync() {
        // A new PC in the new year, the cloud still holds last year's event
        val fresh = mapOf(mayor to mayorOf(528, 0, 0, resets = 0), past to pastOf())
        val cloudFiles = mapOf(mayor to mayorOf(527, 15, 1500), past to pastOf(mayorOf(526, 4, 400, resets = 0)))
        val result = CloudDiff.compare(fresh, cloudFiles)
        assertTrue(result.areas.isEmpty())
        val merged = mergeAll(fresh, cloudFiles, result)
        assertEquals(528, json(merged[mayor]).get("year").asInt)
        assertEquals(15, chimeras(pastEvent(merged, 527)))
        assertEquals(4, chimeras(pastEvent(merged, 526)))
    }

    @Test
    fun sameMayorYearAddsCounts() {
        val base = mapOf(mayor to mayorOf(527, 10, 1000, at = 100))
        val pcFiles = mapOf(mayor to mayorOf(527, 12, 1200, resets = 5, at = 300))
        val cloudFiles = mapOf(mayor to mayorOf(527, 15, 1500, resets = 6, at = 200))
        val result = CloudDiff.compare(pcFiles, cloudFiles, base)
        assertTrue(result.areas.isEmpty())
        val merged = json(mergeAll(pcFiles, cloudFiles, result)[mayor])
        assertEquals(17, chimeras(merged))
        assertEquals(1700, merged.getAsJsonObject("items").get("time").asInt)
        assertEquals(6, merged.get("resetCount").asInt)
        assertEquals(300, merged.get("lastTrackedAt").asInt)
    }

    @Test
    fun trackerSumsNoticeResets() {
        val before = CloudDiff.trackerSums(sessionBase)
        val after = CloudDiff.trackerSums(sessionReset)
        assertTrue(after.getValue(session) < before.getValue(session))
        // Start coins of the CoA are no count, a new CoA is no reset
        val coa = mapOf(session to """{"items":{"CHIMERA":1,"COA Start Coins":900000},"mobs":{}}""")
        val newCoa = mapOf(session to """{"items":{"CHIMERA":1,"COA Start Coins":-1},"mobs":{}}""")
        assertEquals(CloudDiff.trackerSums(coa), CloudDiff.trackerSums(newCoa))
        assertTrue(CloudDiff.trackerSums(mapOf("SboData.json" to "{}")).isEmpty())
    }

    @Test
    fun eventThatEndedOnBothPcsIsNotAddedTwice() {
        // The 525 event ended on both PCs after the last sync, both copies hold counts from before it
        val base = mapOf("pastDianaEvents.json" to """{"events":[]}""")
        val pcFiles = mapOf("pastDianaEvents.json" to """{"events":[{"year":525,"mobs":{"INQ":103}}]}""")
        val cloudFiles = mapOf("pastDianaEvents.json" to """{"events":[{"year":525,"mobs":{"INQ":102}}]}""")
        assertEquals(1, CloudDiff.compare(pcFiles, cloudFiles, base).areas.size)

        // With the tracker of the last sync, the counts from before it are known and counted once
        val withMayor = base + ("dianaTrackerMayor.json" to """{"year":525,"mobs":{"INQ":100}}""")
        val pcNow = pcFiles + ("dianaTrackerMayor.json" to """{"year":526,"mobs":{"INQ":0}}""")
        val cloudNow = cloudFiles + ("dianaTrackerMayor.json" to """{"year":526,"mobs":{"INQ":0}}""")
        val result = CloudDiff.compare(pcNow, cloudNow, withMayor)
        assertTrue(result.areas.isEmpty())
        assertEquals(105, pastEvent(mergeAll(pcNow, cloudNow, result), 525).getAsJsonObject("mobs").get("INQ").asInt)
    }

    @Test
    fun humanizesKeys() {
        assertEquals("Mobs Since Inq", CloudDiff.humanize("mobsSinceInq"))
        assertEquals("Cloud Sync", CloudDiff.humanize("Cloud Sync"))
        assertEquals("B2b Inq", CloudDiff.humanize("b2bInq"))
    }
}
