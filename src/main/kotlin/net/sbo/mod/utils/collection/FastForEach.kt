package net.sbo.mod.utils.collection

/**
 * Essentially .forEach, but without the extra overhead from allocating an Iterator object.
 *
 * JIT compiler can usually optimize the iteration, but it does not prevent the Iterator object allocation itself.
 * If any iterator allocations shows up in an allocation profiler report, switching to this can improve performance.
 *
 * Note that if the list is a LinkedList, you should keep using .forEach / for (item in list), as algorithmic complexity grows with indexed access.
 */
inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    for (i in 0..<size) {
        action(this[i])
    }
}
