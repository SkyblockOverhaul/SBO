package net.sbo.mod.utils.collection

internal class EvictingQueue<T>(internal val maxSize: Int) {
    internal val queue = mutableListOf<T>()

    fun add(item: T) {
        if (queue.size >= maxSize) {
            queue.removeFirst()
        }
        queue.add(item)
    }

    fun contains(item: T): Boolean {
        return queue.contains(item)
    }

    fun clear() {
        queue.clear()
    }
}