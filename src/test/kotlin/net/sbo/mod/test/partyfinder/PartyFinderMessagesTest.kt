package net.sbo.mod.test.partyfinder

import net.sbo.mod.partyfinder.JoinRequest
import net.sbo.mod.partyfinder.StatReporter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PartyFinderMessagesTest {
    @Test
    fun joinRequestRoundTrip() {
        val message = JoinRequest.message("crowd_control", id = "123e4567-e89b-12d3-a456-426614174000")
        assertEquals("[SBO] join party request - id:123e4567-e89b-12d3-a456-426614174000 role:crowd_control", message)
        assertEquals(JoinRequest("crowd_control"), JoinRequest.parse(message.substringAfter("id:")))
        // A random id each time, Hypixel blocks the same message twice in a row
        assert(JoinRequest.message(null) != JoinRequest.message(null))
        assertEquals(JoinRequest("dps"), JoinRequest.parse("123e4567-e89b-12d3-a456-426614174000 role:dps\u00a7r"))
    }

    @Test
    fun joinCommandFitsTheChatLimit() {
        // Longest name (16) and the longest role the leader accepts (32); Minecraft allows 256 characters
        val command = "/msg ${"a".repeat(16)} " + JoinRequest.message("r".repeat(32))
        assert(command.length <= 256) { "${command.length} characters" }
    }

    @Test
    fun oldModRequestsHaveNoRole() {
        assertEquals(JoinRequest(null), JoinRequest.parse("123e4567-e89b-12d3-a456-426614174000"))
        assertEquals(JoinRequest(null), JoinRequest.parse("not a uuid"))
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
