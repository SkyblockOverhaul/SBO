package net.sbo.mod.utils.collection

internal class EvictingQueue<T>(private val maxSize: Int) {
    private val queue = mutableListOf<T>()

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