package net.sbo.mod.utils.collection

internal class EvictingQueue<T>(private val maxSize: Int) {
    private val queue = mutableListOf<T>()

    fun add(item: T) {
        if (queue.size >= maxSize) {
            queue.removeFirst()
        }
        queue.add(item)
    }

    operator fun contains(item: T): Boolean = item in queue

    fun clear() {
        queue.clear()
    }
}