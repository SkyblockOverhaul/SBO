package net.sbo.mod.utils.collection

import com.google.common.cache.RemovalCause
import java.util.HashMap
import kotlin.time.Duration

/**
 * A [MutableSet] implementation backed by a time-limited cache.
 *
 * Elements in this set will expire and be removed after a specified duration
 * since their last write (addition).
 *
 * @param T The type of elements in the set.
 * @param expireAfterWrite The duration after which an element should expire.
 * @param removalListener An optional listener that gets notified when an element is removed,
 *                        along with the cause of removal.
 *
 * Credits to this go fully to SkyHanni
 */
class TimeLimitedSet<T : Any>(
    expireAfterWrite: Duration,
    private val removalListener: ((T?, RemovalCause) -> Unit)? = null,
) : AbstractMutableSet<T>() {
    private val addedAt = HashMap<T, Long>()
    private val expireAfterWriteNanos = expireAfterWrite.inWholeNanoseconds

    override val size: Int
        get() {
            expireEntries()
            return addedAt.size
        }

    override fun add(element: T): Boolean {
        expireEntries()
        val wasPresent = addedAt.put(element, System.nanoTime()) != null
        if (wasPresent) removalListener?.invoke(element, RemovalCause.REPLACED)
        return !wasPresent
    }

    override fun contains(element: T): Boolean {
        expireEntries()
        return element in addedAt
    }

    override fun remove(element: T): Boolean {
        expireEntries()
        if (addedAt.remove(element) == null) return false
        removalListener?.invoke(element, RemovalCause.EXPLICIT)
        return true
    }

    override fun clear() {
        expireEntries()
        val iterator = addedAt.keys.iterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            iterator.remove()
            removalListener?.invoke(element, RemovalCause.EXPLICIT)
        }
    }

    override fun iterator(): MutableIterator<T> {
        expireEntries()
        val iterator = addedAt.keys.iterator()
        var current: T? = null
        var canRemove = false
        return object : MutableIterator<T> {
            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): T {
                val next = iterator.next()
                current = next
                canRemove = true
                return next
            }

            override fun remove() {
                check(canRemove) { "next() must be called before remove()" }
                iterator.remove()
                current?.let { removalListener?.invoke(it, RemovalCause.EXPLICIT) }
                current = null
                canRemove = false
            }
        }
    }

    private fun expireEntries() {
        val now = System.nanoTime()
        val expired = mutableListOf<T>()
        val iterator = addedAt.entries.iterator()
        while (iterator.hasNext()) {
            val (element, addedAtNanos) = iterator.next()
            if (now - addedAtNanos >= expireAfterWriteNanos) {
                iterator.remove()
                expired.add(element)
            }
        }
        expired.forEach { removalListener?.invoke(it, RemovalCause.EXPIRED) }
    }
}
