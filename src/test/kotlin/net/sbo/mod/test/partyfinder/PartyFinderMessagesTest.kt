package net.sbo.mod.test.partyfinder

import net.sbo.mod.partyfinder.JoinRequest
import net.sbo.mod.partyfinder.StatReporter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PartyFinderMessagesTest {
    private val uuid = "0123456789abcdef0123456789abcdef"

    @Test
    fun joinRequestRoundTrip() {
        val message = JoinRequest.message(uuid, "crowd_control")
        assertEquals("[SBO] join party request - id:$uuid role:crowd_control", message)
        assertEquals(JoinRequest(uuid, "crowd_control"), JoinRequest.parse(message.substringAfter("id:")))
        assertEquals(JoinRequest(uuid, null), JoinRequest.parse("$uuid§r"))
        assertEquals(JoinRequest(uuid, "dps"), JoinRequest.parse("${uuid.uppercase()} role:dps§r"))
    }

    @Test
    fun oldModsHaveNoUuid() {
        // Old mods send a random uuid with dashes
        assertEquals(JoinRequest(null, null), JoinRequest.parse("123e4567-e89b-12d3-a456-426614174000"))
        assertEquals(JoinRequest(null, null), JoinRequest.parse("not a uuid"))
    }

    @Test
    fun bphOnlyWhenPlausible() {
        assertNull(StatReporter.bphReport(400, 0.9, current = true))
        assertNull(StatReporter.bphReport(0, 3.0, current = true))
        assertNull(StatReporter.bphReport(4000, 2.0, current = true))
        val report = StatReporter.bphReport(1000, 1.5, current = false)!!
        assertEquals(666.67, report.value)
        assertEquals("lastEvent", report.scope)
        assertEquals("current", StatReporter.bphReport(600, 1.0, current = true)!!.scope)
    }
}
