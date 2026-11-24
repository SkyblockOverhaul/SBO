package net.sbo.mod.utils.data.cloud

import com.google.gson.Gson
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.data.LocalStore

// When each setting, value or file last changed on this PC, so Auto Merge knows which side changed something last
object CloudChanges {
    // Keeps the upload small, the oldest times go first
    private const val MAX_TIMES = 250

    // sums holds all counts of each tracker added up, so a reset can be noticed
    private class Saved(
        val hashes: Map<String, Int> = emptyMap(),
        val times: Map<String, Long> = emptyMap(),
        val sums: Map<String, Double> = emptyMap(),
    )

    // uuid -> Saved as json
    private val store = LocalStore("cloud-changes.json")
    private val gson = Gson()

    private fun load(uuid: String): Saved =
        runCatching { gson.fromJson(store[uuid] ?: return Saved(), Saved::class.java) }.getOrNull() ?: Saved()

    private fun write(uuid: String, saved: Saved) {
        runCatching { store[uuid] = gson.toJson(saved) }
            .onFailure { SBOKotlin.logger.warn("[CloudSync] could not remember the change times: ${it.message}") }
    }

    fun times(uuid: String): Map<String, Long> = load(uuid).times

    // Notes the time of everything that changed since the last call. Values that are new (e.g. after an update) get no time.
    // A tracker with fewer counts than before was reset (button, command or deleted file), that time is noted as well.
    fun track(uuid: String, files: Map<String, String>, now: Long = System.currentTimeMillis()) {
        val saved = load(uuid)
        val hashes = CloudDiff.fingerprints(files)
        val sums = CloudDiff.trackerSums(files)
        if (hashes == saved.hashes && sums == saved.sums) return
        val times = saved.times.toMutableMap()
        if (saved.hashes.isNotEmpty()) {
            hashes.forEach { (id, hash) ->
                val old = saved.hashes[id]
                if (old != null && old != hash) times[id] = now
            }
            sums.forEach { (file, sum) ->
                val old = saved.sums[file]
                if (old != null && sum < old) times[CloudDiff.resetKey(file)] = now
            }
        }
        write(uuid, Saved(hashes, newest(times), sums))
    }

    // After a download or merge: this PC has exactly these files now, changed at these times
    fun reset(uuid: String, files: Map<String, String>, times: Map<String, Long>) {
        write(uuid, Saved(CloudDiff.fingerprints(files), newest(times), CloudDiff.trackerSums(files)))
    }

    // The later time of both sides for every value
    fun latest(a: Map<String, Long>, b: Map<String, Long>): Map<String, Long> =
        (a.keys + b.keys).associateWith { maxOf(a[it] ?: 0L, b[it] ?: 0L) }

    private fun newest(times: Map<String, Long>): Map<String, Long> =
        if (times.size <= MAX_TIMES) times
        else times.entries.sortedByDescending { it.value }.take(MAX_TIMES).associate { it.key to it.value }
}
