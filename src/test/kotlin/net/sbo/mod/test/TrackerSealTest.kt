package net.sbo.mod.test

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.sbo.mod.utils.data.TrackerSeal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TrackerSealTest {
    private fun tracker(burrows: Int, time: Long, edited: Boolean = false): JsonObject =
        JsonParser.parseString("""{"year":512,"items":{"Total Burrows":$burrows,"time":$time,"Chimera":3},"edited":$edited,"resetCount":2}""").asJsonObject

    // What a load sees: the file parsed again, with `seal` from the save
    private fun reread(tree: JsonObject): JsonObject = JsonParser.parseString(tree.toString()).asJsonObject

    @Test
    fun `a saved tracker gives back what was sealed`() {
        val saved = reread(TrackerSeal.sealed(tracker(1200, 3_600_000), "dianaTrackerMayor.json"))
        val original = TrackerSeal.original(saved, "dianaTrackerMayor.json")!!
        assertEquals(tracker(1200, 3_600_000), original)
        assertFalse(TrackerSeal.changed(saved, original))
        // The alpha copy of the same tracker is fine as well
        assertNotNull(TrackerSeal.original(saved, "alpha/dianaTrackerMayor.json"))
    }

    @Test
    fun `changed values are found and can be undone`() {
        val seal = TrackerSeal.sealed(tracker(1200, 3_600_000), "dianaTrackerMayor.json").get("seal")
        val chimeras = tracker(1200, 3_600_000).apply { getAsJsonObject("items").addProperty("Chimera", 9) }
        for (changed in listOf(tracker(5000, 3_600_000), tracker(1200, 1_800_000), tracker(1200, 3_600_000, edited = true), chimeras)) {
            changed.add("seal", seal)
            val original = TrackerSeal.original(reread(changed), "dianaTrackerMayor.json")!!
            assertTrue(TrackerSeal.changed(reread(changed), original))
            assertEquals(tracker(1200, 3_600_000), original)
        }
    }

    @Test
    fun `a changed seal or the seal of another tracker gives nothing back`() {
        val saved = TrackerSeal.sealed(tracker(1200, 3_600_000), "dianaTrackerMayor.json")
        assertNull(TrackerSeal.original(saved, "dianaTrackerTotal.json"))
        val seal = saved.get("seal").asString
        val flipped = (if (seal[20] == 'A') 'B' else 'A').toString()
        saved.addProperty("seal", seal.replaceRange(20, 21, flipped))
        assertNull(TrackerSeal.original(saved, "dianaTrackerMayor.json"))
        saved.addProperty("seal", "not a seal")
        assertNull(TrackerSeal.original(saved, "dianaTrackerMayor.json"))
    }

    @Test
    fun `a file without a seal is not sealed`() {
        assertFalse(TrackerSeal.isSealed(tracker(1200, 3_600_000)))
        assertTrue(TrackerSeal.isSealed(TrackerSeal.sealed(tracker(1200, 3_600_000), "dianaTrackerMayor.json")))
    }
}
