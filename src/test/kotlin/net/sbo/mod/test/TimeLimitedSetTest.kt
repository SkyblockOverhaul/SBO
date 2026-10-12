package net.sbo.mod.test

import com.google.common.cache.RemovalCause
import net.sbo.mod.utils.collection.TimeLimitedSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.TreeMap
import kotlin.time.Duration.Companion.seconds

class TimeLimitedSetTest {
    @Test
    fun `iterator removal updates expiration index and notifies once`() {
        val removals = mutableListOf<Pair<String?, RemovalCause>>()
        val set = TimeLimitedSet<String>(60.seconds) { element, cause -> removals += element to cause }
        set.add("entry")

        val iterator = set.iterator()
        assertEquals("entry", iterator.next())
        iterator.remove()

        assertFalse(set.contains("entry"))
        assertTrue(expirationIndex(set).values.none { "entry" in it })
        assertEquals(listOf("entry" to RemovalCause.EXPLICIT), removals)
    }

    @Test
    fun `stale expiration bucket cannot remove a later insertion`() {
        val set = TimeLimitedSet<String>(60.seconds)
        set.add("entry")
        val iterator = set.iterator()
        iterator.next()
        iterator.remove()
        set.add("entry")

        val writeTimes = addedAt(set)
        val currentWriteTime = writeTimes.getValue("entry")
        val staleWriteTime = currentWriteTime - 60.seconds.inWholeNanoseconds - 1
        expirationIndex(set)[staleWriteTime] = mutableSetOf("entry")
        set.javaClass.getDeclaredMethod("expireEntries").apply { isAccessible = true }.invoke(set)

        assertTrue(set.contains("entry"))
        assertEquals(currentWriteTime, addedAt(set)["entry"])
    }

    @Test
    fun `iterator removal requires one next per remove`() {
        val set = TimeLimitedSet<String>(60.seconds)
        set.add("entry")
        val iterator = set.iterator()

        assertThrows(IllegalStateException::class.java) { iterator.remove() }
        iterator.next()
        iterator.remove()
        assertThrows(IllegalStateException::class.java) { iterator.remove() }
    }

    @Test
    fun `normal expiration notifies once`() {
        val removals = mutableListOf<Pair<String?, RemovalCause>>()
        val set = TimeLimitedSet<String>(kotlin.time.Duration.ZERO) { element, cause -> removals += element to cause }

        assertTrue(set.add("entry"))
        assertFalse(set.contains("entry"))
        assertEquals(listOf("entry" to RemovalCause.EXPIRED), removals)
    }

    @Suppress("UNCHECKED_CAST")
    private fun addedAt(set: TimeLimitedSet<String>): MutableMap<String, Long> =
        set.javaClass.getDeclaredField("addedAt").apply { isAccessible = true }.get(set) as MutableMap<String, Long>

    @Suppress("UNCHECKED_CAST")
    private fun expirationIndex(set: TimeLimitedSet<String>): TreeMap<Long, MutableSet<String>> =
        set.javaClass.getDeclaredField("elementsByWriteTime").apply { isAccessible = true }.get(set) as TreeMap<Long, MutableSet<String>>
}
