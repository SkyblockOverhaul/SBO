package net.sbo.mod.utils.collection

import com.google.common.cache.RemovalCause
import java.util.HashMap
import java.util.TreeMap
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
    private val elementsByWriteTime = TreeMap<Long, MutableSet<T>>()
    private val expireAfterWriteNanos = expireAfterWrite.inWholeNanoseconds

    override val size: Int
        get() {
            expireEntries()
            return addedAt.size
        }

    override fun add(element: T): Boolean {
        expireEntries()
        val now = System.nanoTime()
        val previousWriteTime = addedAt.put(element, now)
        if (previousWriteTime != null) {
            removeFromExpirationIndex(element, previousWriteTime)
        }
        elementsByWriteTime.getOrPut(now) { LinkedHashSet() }.add(element)
        val wasPresent = previousWriteTime != null
        if (wasPresent) removalListener?.invoke(element, RemovalCause.REPLACED)
        return !wasPresent
    }

    override fun contains(element: T): Boolean {
        expireEntries()
        return element in addedAt
    }

    override fun remove(element: T): Boolean {
        expireEntries()
        val writeTime = addedAt.remove(element) ?: return false
        removeFromExpirationIndex(element, writeTime)
        removalListener?.invoke(element, RemovalCause.EXPLICIT)
        return true
    }

    override fun clear() {
        expireEntries()
        val explicitlyRemoved = addedAt.keys.toList()
        addedAt.clear()
        elementsByWriteTime.clear()
        explicitlyRemoved.forEach { removalListener?.invoke(it, RemovalCause.EXPLICIT) }
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
                val element = checkNotNull(current)
                val writeTime = checkNotNull(addedAt[element])
                iterator.remove()
                removeFromExpirationIndex(element, writeTime)
                current = null
                canRemove = false
                removalListener?.invoke(element, RemovalCause.EXPLICIT)
            }
        }
    }

    private fun expireEntries() {
        val now = System.nanoTime()
        var expired: MutableList<T>? = null
        while (elementsByWriteTime.isNotEmpty()) {
            val (writeTime, elements) = elementsByWriteTime.firstEntry()
            if (now - writeTime < expireAfterWriteNanos) break

            elementsByWriteTime.pollFirstEntry()
            val expiredElements = expired ?: mutableListOf<T>().also { expired = it }
            for (element in elements) {
                if (addedAt[element] == writeTime) {
                    addedAt.remove(element)
                    expiredElements.add(element)
                }
            }
        }
        expired?.forEach { removalListener?.invoke(it, RemovalCause.EXPIRED) }
    }

    private fun removeFromExpirationIndex(element: T, writeTime: Long) {
        val elementsAtWriteTime = elementsByWriteTime[writeTime] ?: return
        elementsAtWriteTime.remove(element)
        if (elementsAtWriteTime.isEmpty()) elementsByWriteTime.remove(writeTime)
    }
}
