package net.sbo.mod.test

import net.sbo.mod.diana.RareMobKillTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RareMobKillTimeTest {
    @Test
    fun parsesPartyKillTimeMessageWithLiteralPrefixAndCapturesFields() {
        val parsed = RareMobKillTime.parseKillTimeMessage(
            "Party > Alice: [SBO] Took 3.2s to kill the Minos Inquisitor!"
        )

        assertEquals("Party > ", parsed?.prefix)
        assertEquals("Alice", parsed?.player)
        assertEquals("3.2s", parsed?.elapsed)
        assertEquals("Minos Inquisitor", parsed?.mob)
    }

    @Test
    fun parsesMessageWithoutOptionalPartySenderPrefix() {
        val parsed = RareMobKillTime.parseKillTimeMessage(
            "Alice: [SBO] Took 3.2s to kill the Minos Inquisitor!"
        )

        assertEquals(null, parsed?.prefix)
        assertEquals("Alice", parsed?.player)
    }

    @Test
    fun rejectsMalformedOrMissingSboPrefix() {
        assertNull(RareMobKillTime.parseKillTimeMessage("Alice: SBO Took 3.2s to kill the Minos Inquisitor!"))
        assertNull(RareMobKillTime.parseKillTimeMessage("Alice: [SB] Took 3.2s to kill the Minos Inquisitor!"))
    }

    @Test
    fun rejectsOtherMobsAndUnrelatedPartyMessages() {
        assertNull(RareMobKillTime.parseKillTimeMessage("Alice: [SBO] Took 3.2s to kill a Zombie!"))
        assertNull(RareMobKillTime.parseKillTimeMessage("Party > Alice: [SBO] Cocooned a Minos Inquisitor!"))
    }

    @Test
    fun retainsCocoonHatchSuffixFormat() {
        val parsed = RareMobKillTime.parseKillTimeMessage(
            "Alice: [SBO] Took 3.2s to kill the King Minos! (cocoon hatch: 1.5s)"
        )

        assertEquals("King Minos", parsed?.mob)
        assertEquals("3.2s", parsed?.elapsed)
    }
}
